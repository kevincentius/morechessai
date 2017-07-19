package chess.ai;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import chess.ai.ChessTranspositionTable.EntryType;
import chess.ai.ChessTranspositionTable.TpEntry;
import chess.ai.evaluator.DoubleUtil;
import chess.ai.evaluator.MaterialAndMobilityEvaluator;
import chess.instance.board.GameState;
import chess.rule.piece.move.Move;
import chess.rule.set.RuleSet;
import util.MurmurHash3.LongPair;

public class ChessAICurrent implements ChessAI {
	
	private double[] values = { 999999, 100, 325, 350, 500, 900, 525, 400 };
	
	private Thread thread;
	private RuleSet ruleSet;
	private GameState gameState;
	
	private boolean terminateSearch = false;

	private Function<SearchNode, Void> onNewBestNode;
	
	private Runnable onEvaluationFinished;
	private Function<GameState, Void> onDebug;
	
	private MaterialAndMobilityEvaluator evaluator = new MaterialAndMobilityEvaluator(values, 0.5);

	private long timeLimit;
	
	private ChessTranspositionTable transpositionTable = new ChessTranspositionTable();
	
	private int minDepthForTranspositionTable = 1;
	private double quiesceDepth = 8;
	
	
	private int quiesceCount = 0;
	private int negamaxCount = 0;
	
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
		this.gameState = gameState.copy();
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
		GameState state = gameState;
		
		try {
			for (int depth = 1; true; depth++) {
				negamaxCount = 0;
				quiesceCount = 0;

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
					System.out.println("Depth " + depth + ", " + negamaxCount + "+" + quiesceCount + " nodes, " + ms + " ms: " + rootNode);
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
		negamaxCount++;
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
			return setMatingScore(state, node, numMoves);
		} else if (depth <= 0) {
			return quiesce(state, node, quiesceDepth, alpha, beta, numMoves);
		}
		
		// Analyze node in depth
		List<Move> moves = getSortedLegalMoves(ruleSet, state);
		double bestValue = -Double.MAX_VALUE;
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

		node.setScore(bestValue);
		return bestValue;
	}

	private double quiesce(GameState state, SearchNode node, double depth, double alpha, double beta, int numMoves) {
		quiesceCount++;
		double color = 1 - state.getTurn() * 2;
		double score = color * evaluator.evaluate(state, ruleSet);
		if (score >= beta) {
			return beta;
		}
		alpha = Math.max(alpha, score);
		
		// TODO: Note1 = more efficient way, e.g. getSortedTacticalMoves() ?
		List<Move> moves = getSortedLegalMoves(ruleSet, state);
		for (Iterator<Move> it = moves.iterator(); it.hasNext(); ) {
			Move move = it.next();
			//if (move.getTacticalValue(state, values) <= 0) {
			if (move.getTacticalValue(state, values) <= 0 || alpha + move.getTacticalValue(state, values) <= -200) {
				it.remove();
			}
		}

		// Check for terminal / quiet node
		if (state.getWinner() != -1) {
			return setMatingScore(state, node, numMoves);
		} else if (depth <= 0 || moves.isEmpty()) {
			node.setScore(color * score);
			return score;
		}
		
		// Analyze tactical moves
		double bestValue = score; // Double.NEGATIVE_INFINITY;
		for (Move move : moves) {
			SearchNode childNode = new SearchNode(move);
			
			move.make(state, ruleSet);
			double v = -quiesce(state, childNode, depth - 1, -beta, -alpha, numMoves + 1);
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

		node.setScore(color * bestValue);
		return bestValue;
	}

	private double setMatingScore(GameState state, SearchNode node, int numMoves) {
		if (state.getTurn() == state.getWinner()) {
			node.setScore(DoubleUtil.getNthMaxDouble(numMoves));
			return DoubleUtil.getNthMaxDouble(numMoves);
		} else {
			node.setScore(-DoubleUtil.getNthMaxDouble(numMoves));
			return -DoubleUtil.getNthMaxDouble(numMoves);
		}
	}

	private List<Move> getSortedLegalMoves(RuleSet ruleSet, GameState state) {
		List<Move> moveList = AiTools.getAllLegalMoves(ruleSet, state);
		Comparator<Move> moveComparator = new Comparator<Move>() {
			@Override
			public int compare(Move a, Move b) {
				return -Double.compare(a.getTacticalValue(state, values), b.getTacticalValue(state, values));
			}
		};
		Collections.sort(moveList, moveComparator);
		return moveList;
	}

	@Override
	public void setOnDebug(Function<GameState, Void> onDebug) {
		this.onDebug = onDebug;
	}

}
