package chess.ai.previous;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import chess.ai.AiTools;
import chess.ai.SearchNode;
import chess.ai.TranspositionTable;
import chess.ai.TranspositionTable.AnalysedPosition;
import chess.ai.evaluator.MaterialAndMobilityEvaluator;
import chess.data.ChessMove;
import chess.data.PromotionMove;
import chess.instance.board.GameState;
import chess.instance.piece.Piece;
import chess.rule.piece.move.Move;
import chess.rule.set.RuleSet;
import util.MurmurHash3.LongPair;

public class ChessAI_v6_beforeDestryoed {
	
	//private double[] values = { 300, 300, 500, 650, 0, 100 };
	private double[] values = { 300, 300, 500, 900, 0, 100 };
	
	private Thread thread;
	private RuleSet ruleSet;
	private GameState gameState;
	
	private boolean terminateSearch = false;

	private Function<SearchNode, Void> onNewBestNode;
	
	private Runnable onEvaluationFinished;
	
	private MaterialAndMobilityEvaluator evaluator = new MaterialAndMobilityEvaluator(values, 5);

	private long timeLimit;
	
	private TranspositionTable transpositionTable = new TranspositionTable();
	
	private int minDepthForTranspositionTable = 0;
	
	public void setOnEvaluationFinished(Runnable onTimeLimitReached) {
		this.onEvaluationFinished = onTimeLimitReached;
	}

	public void setOnNewBestNode(Function<SearchNode, Void> onNewBestNode) {
		this.onNewBestNode = onNewBestNode;
	}

	public void start(RuleSet ruleSet, GameState gameState, Function<SearchNode, Void> onNewBestNode, long maxTimeMs) {
		this.onNewBestNode = onNewBestNode;
		this.timeLimit = System.currentTimeMillis() + maxTimeMs;
		
		if (thread != null) {
			throw new RuntimeException("AI already running!");
		}
		this.ruleSet = ruleSet;
		this.gameState = gameState;
		thread = new Thread(mainLoop);
		
		terminateSearch = false;
		thread.start();
	}

	public void stop() {
		if (thread != null) {
			terminateSearch = true;
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			thread = null;
		}
	}
	
	private Runnable mainLoop = () -> {
		// TODO: sync problem on copying state from gameState
		GameState state = gameState.copy();
		
		try {
			for (int depth = 1; true; depth++) {
				if (depth == 2) {
					System.out.println("DEBUG START");
				}
				transpositionTable = new TranspositionTable();
				long ms = System.currentTimeMillis();
				
				SearchNode rootNode = alphaBetaSearch(state, new SearchNode(null), depth, -Double.MAX_VALUE, Double.MAX_VALUE);
				ms = System.currentTimeMillis() - ms;
				if (terminateSearch) {
					break;
				}

				if (rootNode != null) {
					onNewBestNode.apply(rootNode);
					System.out.println("Depth " + depth + ", " + ms + " ms: " + rootNode);
					System.out.println("Transposition: " + transpositionTable.getHitCount() + " / " + transpositionTable.getCheckCount() + " hits, " + transpositionTable.getPosCount() + " positions, " + transpositionTable.getDelCount() + "@" + transpositionTable.getDelOnUpdateCount() + " thrown away, " + transpositionTable.getAvgBucketSize() + " avg, " + transpositionTable.getMaxBucketSize() + " biggest bucket");
				}
			}
		} catch (InterruptedException e) {
			System.out.println("AI Thread interrupted");
			if (onEvaluationFinished != null) {
				onEvaluationFinished.run();
			}
			thread = null;
		}
	};

	private SearchNode alphaBetaSearch(GameState state, SearchNode node, double depth, double alpha, double beta) throws InterruptedException {
		List<Move> legalMoves;
		LongPair hash = state.get128Hash();
		AnalysedPosition previousAnalysis = transpositionTable.checkPosition(hash.val1, hash.val2);
		if (previousAnalysis != null && previousAnalysis.depthLeft >= depth) {
			node.setScore(previousAnalysis.score);
			return node;
		} else {
			transpositionTable.savePosition(hash.val1, hash.val2, 0, depth);
		}
		
		if (System.currentTimeMillis() >= timeLimit) {
			//terminateSearch = true;
		}
		if (terminateSearch) {
			throw new InterruptedException();
		}
		
		if (depth <= 0 || state.getWinner() != -1) {
			double score = evaluator.evaluate(state, ruleSet);
			node.setScore(score);
			
			if (state.getWinner() == -1) {
				transpositionTable.updatePosition(hash.val1, hash.val2, score, depth);
			}
			return node;
		} else {
			legalMoves = getSortedLegalMoves(ruleSet, state);
			if (legalMoves.isEmpty()) {
				node.setScore(0);
				return node;
			}
		}
		
		SearchNode bestNode = null;
		for (Move move : legalMoves) {
			move.make(state, ruleSet);
			double nextDepth = depth - (move.isTactical() ? 0.5 : 1d);
			SearchNode childNode = alphaBetaSearch(state, new SearchNode(move), nextDepth, -beta, -alpha);
			childNode.setScore(-childNode.getScore());
			move.unmake(state, ruleSet);
			if (bestNode == null || childNode.getScore() > bestNode.getScore()) {
				bestNode = childNode;
				alpha = Math.max(alpha, bestNode.getScore());
				if (alpha >= beta) {
					break;
				}
			}
		}

		
		if (bestNode != null) {
			node.setBestChild(bestNode);
			node.setScore(-bestNode.getScore());
			//System.out.println("NB " + node);
		} else {			
			System.out.println("no legal moves after " + node);
			node.setScore(Double.NEGATIVE_INFINITY);
		}
		transpositionTable.updatePosition(hash.val1, hash.val2, node.getScore(), depth);
		return node;
	}

	private List<Move> getSortedLegalMoves(RuleSet ruleSet, GameState state) {
		List<Move> moveList = AiTools.getAllLegalMoves(ruleSet, state);
		Comparator<Move> moveComparator = new Comparator<Move>() {
			@Override
			public int compare(Move a, Move b) {
				return -Double.compare(getValue(a), getValue(b));
			}

			private double getValue(Move a) {
				if (a instanceof ChessMove) {
					Piece capturedPieceA = state.getPiece(((ChessMove)a).getTo());
					return capturedPieceA == null ? 0 : values[capturedPieceA.getId()];
				} else if (a instanceof PromotionMove) {
					Piece capturedPieceA = state.getPiece(((PromotionMove)a).getTo());
					return capturedPieceA == null ? 0 : values[capturedPieceA.getId()];
				} else {
					return 0;
				}
			}
		};
		Collections.sort(moveList, moveComparator);
		return moveList;
	}

}
