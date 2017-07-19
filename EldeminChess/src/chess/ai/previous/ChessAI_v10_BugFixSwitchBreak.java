package chess.ai.previous;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import chess.ai.AiTools;
import chess.ai.ChessAI;
import chess.ai.ChessTranspositionTable;
import chess.ai.SearchNode;
import chess.ai.ChessTranspositionTable.EntryType;
import chess.ai.ChessTranspositionTable.TpEntry;
import chess.ai.evaluator.DoubleUtil;
import chess.ai.evaluator.MaterialAndMobilityEvaluator;
import chess.data.ChessMove;
import chess.data.PromotionMove;
import chess.instance.board.GameState;
import chess.instance.piece.Piece;
import chess.rule.piece.move.Move;
import chess.rule.set.RuleSet;
import util.MurmurHash3.LongPair;

public class ChessAI_v10_BugFixSwitchBreak implements ChessAI {
	
	//private double[] values = { 300, 300, 500, 650, 0, 100 };
	private double[] values = { 300, 300, 500, 900, 0, 100 };
	
	private Thread thread;
	private RuleSet ruleSet;
	private GameState gameState;
	
	private boolean terminateSearch = false;

	private Function<SearchNode, Void> onNewBestNode;
	
	private Runnable onEvaluationFinished;
	
	private MaterialAndMobilityEvaluator evaluator = new MaterialAndMobilityEvaluator(values, 2);

	private long timeLimit;
	
	private ChessTranspositionTable transpositionTable = new ChessTranspositionTable();
	
	private int minDepthForTranspositionTable = 1;
	private int quisceDepth = 1;
	
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
				transpositionTable = new ChessTranspositionTable();
				long ms = System.currentTimeMillis();
				
				SearchNode rootNode = new SearchNode(null);
				negamax(state, rootNode, depth, -Double.MAX_VALUE, Double.MAX_VALUE, 0);
				ms = System.currentTimeMillis() - ms;
				if (terminateSearch) {
					break;
				}

				if (rootNode != null) {
					onNewBestNode.apply(rootNode);
					System.out.println("Depth " + depth + ", " + ms + " ms: " + rootNode);
					System.out.println("Transposition: " + transpositionTable.getHitCount() + " / " + transpositionTable.getCheckCount() + " hits, " + transpositionTable.getDelCount() + " thrown away, " + transpositionTable.getAvgBucketSize() + " filled");
				}
				
				evaluator.printDebugAndReset();
			}
		} catch (InterruptedException e) {
			System.out.println("AI Thread interrupted");
			if (onEvaluationFinished != null) {
				onEvaluationFinished.run();
			}
			thread = null;
		}
	};

	private double negamax(GameState state, SearchNode node, double depth, double alpha, double beta, int numMoves) throws InterruptedException {
		// Termination check
		if (System.currentTimeMillis() >= timeLimit) {
			terminateSearch = true;
		}
		if (terminateSearch) {
			throw new InterruptedException();
		}
		
		double alphaOrig = alpha;
		LongPair hash = state.get128Hash();
		
		// Transposition Table Lookup
		{
			TpEntry entry = transpositionTable.checkPosition(hash.val1, hash.val2);
			if (entry != null && entry.depth >= depth) {
				switch (entry.type) {
				case EXACT:
					node.setScore(entry.score);
					return entry.score;
				case LOWERBOUND:
					alpha = Math.max(alpha, entry.score);
					break;
				case UPPERBOUND:
					beta = Math.min(beta, entry.score);
					break;
				}
				if (alpha >= beta) {
					node.setScore(entry.score);
					return entry.score;
				}
			}
		}
		
		// Check for terminal node
		if (state.getWinner() != -1) {
			if (state.getTurn() == state.getWinner()) {
				node.setScore(DoubleUtil.getNthMaxDouble(numMoves));
				return DoubleUtil.getNthMaxDouble(numMoves);
			} else {
				node.setScore(-DoubleUtil.getNthMaxDouble(numMoves));
				return -DoubleUtil.getNthMaxDouble(numMoves);
			}
		} else if (depth <= 0) {
			double color = 1 - state.getTurn() * 2;
			node.setScore(color * evaluator.evaluate(state, ruleSet));
			return color * evaluator.evaluate(state, ruleSet);
		}
		
		// Analyze node in depth
		List<Move> moves = getSortedLegalMoves(ruleSet, state);
		double bestValue = Double.NEGATIVE_INFINITY;
		for (Move move : moves) {
			SearchNode childNode = new SearchNode(move);
			
			move.make(state, ruleSet);
			double v = -negamax(state, childNode, depth - 1, -beta, -alpha, numMoves + 1);
			if (DoubleUtil.isMaxDouble(v)) {
				v = DoubleUtil.decrementMaxDouble(v);
			} else if (DoubleUtil.isMaxDouble(-v)) {
				v = DoubleUtil.incrementMaxDouble(v);
			}
			move.unmake(state, ruleSet);
			
			if (v > bestValue) {
				bestValue = v;
				node.setBestChild(childNode);
				alpha = Math.max(alpha, v);
				if (alpha >= beta) {
					break;
				}
			}
		}
		
		// Transposition Table Storage
		/*if (depth <= minDepthForTranspositionTable)*/ {
			EntryType type;
			if (bestValue <= alphaOrig) {
				type = EntryType.UPPERBOUND;
			} else if (bestValue >= beta) {
				type = EntryType.LOWERBOUND;
			} else {
				type = EntryType.EXACT;
			}
			
			TpEntry entry = new TpEntry(hash.val1, hash.val2, depth, bestValue, type);
			transpositionTable.savePosition(entry);
		}

		if (depth == 4) {
			System.out.print("");
		}
		node.setScore(bestValue);
		return bestValue;
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
