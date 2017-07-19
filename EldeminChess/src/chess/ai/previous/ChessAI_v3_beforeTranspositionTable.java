package chess.ai.previous;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import chess.ai.AiTools;
import chess.ai.SearchNode;
import chess.ai.TranspositionTable;
import chess.ai.evaluator.MaterialAndMobilityEvaluator;
import chess.data.ChessMove;
import chess.data.PromotionMove;
import chess.instance.board.GameState;
import chess.instance.piece.Piece;
import chess.rule.piece.move.Move;
import chess.rule.set.RuleSet;

public class ChessAI_v3_beforeTranspositionTable {
	
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
		transpositionTable = new TranspositionTable();
		
		SearchNode bestNode = null;
		try {
			for (int depth = 1; true; depth++) {
				long ms = System.currentTimeMillis();
				SearchNode bestNodeThisDepth = alphaBetaSearch(state, new SearchNode(null, null), depth, -Double.MAX_VALUE, Double.MAX_VALUE);
				if (bestNodeThisDepth.getMove() != null) {
					bestNode = bestNodeThisDepth;
				}
				ms = System.currentTimeMillis() - ms;
				if (terminateSearch) {
					break;
				}

				if (bestNode != null) {
					onNewBestNode.apply(bestNode);
					System.out.println("Depth " + depth + ", " + ms + " ms: " + bestNode);
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
		
		if (System.currentTimeMillis() >= timeLimit) {
			terminateSearch = true;
		}
		if (terminateSearch) {
			throw new InterruptedException();
		}
		
		if (depth <= 0 || state.getWinner() != -1) {
			node.setScore(evaluator.evaluate(state, ruleSet));
			//System.out.println("eval " + node + " = " + node.getScore());
			return node;
		} else {
			legalMoves = getSortedLegalMoves(ruleSet, state);
			if (legalMoves.isEmpty()) {
				node.setScore(0);
				return node;
			}
		}
		
		if (state.getTurn() == 0) {
			SearchNode bestNode = null;
			for (Move move : legalMoves) {
				move.make(state, ruleSet);
				double nextDepth = depth - (move.isTactical() ? 0.5 : 1d);
				SearchNode childNode = alphaBetaSearch(state, new SearchNode(node, move), nextDepth, alpha, beta);
				move.unmake(state, ruleSet);
				if (bestNode == null || childNode.getScore() > bestNode.getScore()) {
					bestNode = childNode;
					alpha = Math.max(alpha, bestNode.getScore());
					if (alpha >= beta) {
						break;
					}
				}
			}
			if (bestNode == null) {
				System.out.println("no legal moves after " + node);
				node.setScore(Double.POSITIVE_INFINITY);
			} else {
				node.setScore(bestNode.getScore());
			}
			return bestNode;
		} else {
			SearchNode bestNode = null;
			for (Move move : legalMoves) {
				move.make(state, ruleSet);
				double nextDepth = depth - (move.isTactical() ? 0.5 : 1d);
				SearchNode childNode = alphaBetaSearch(state, new SearchNode(node, move), nextDepth, alpha, beta);
				move.unmake(state, ruleSet);
				if (bestNode == null || childNode.getScore() < bestNode.getScore()) {
					bestNode = childNode;
					beta = Math.min(beta, bestNode.getScore());
					if (alpha >= beta) {
						break;
					}					
				}
			}
			if (bestNode == null) {
				System.out.println("no legal moves after " + node);
				node.setScore(Double.NEGATIVE_INFINITY);
			} else {
				node.setScore(bestNode.getScore());
			}
			return bestNode;
		}
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
