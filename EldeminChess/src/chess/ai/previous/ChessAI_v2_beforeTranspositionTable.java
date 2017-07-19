package chess.ai.previous;

import java.util.HashMap;
import java.util.function.Function;

import chess.ai.AiTools;
import chess.ai.SearchNode;
import chess.ai.evaluator.PieceCountEvaluator;
import chess.instance.board.GameState;
import chess.rule.piece.move.Move;
import chess.rule.set.RuleSet;

public class ChessAI_v2_beforeTranspositionTable {
	
	private Thread thread;
	private RuleSet ruleSet;
	private GameState gameState;
	
	private boolean terminateSearch = false;

	private Function<SearchNode, Void> onNewBestNode;
	
	private Runnable onEvaluationFinished;
	
	private PieceCountEvaluator evaluator = new PieceCountEvaluator();

	private long timeLimit;
	
	private HashMap<Double, Double> transpositionTable;
	
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

	private SearchNode alphaBetaSearch(GameState state, SearchNode node, int depth, double alpha, double beta) throws InterruptedException {
		if (System.currentTimeMillis() >= timeLimit) {
			terminateSearch = true;
		}
		if (terminateSearch) {
			throw new InterruptedException();
		}
		
		if (depth == 0 || state.getWinner() != -1) {
			node.setScore(evaluator.evaluate(state));
			
			//System.out.println("eval " + node + " = " + node.getScore());
			return node;
		} else if (state.getTurn() == 0) {
			SearchNode bestNode = new SearchNode(node, null);
			bestNode.setScore(-Double.MAX_VALUE);
			for (Move move : AiTools.getSortedLegalMoves(ruleSet, state)) {
				move.make(state, ruleSet);
				SearchNode childNode = alphaBetaSearch(state, new SearchNode(node, move), depth - 1, alpha, beta);
				move.unmake(state, ruleSet);
				if (childNode.getScore() > bestNode.getScore()) {
					bestNode = childNode;					
					alpha = Math.max(alpha, bestNode.getScore());
					if (alpha >= beta) {
						break;
					}
				}
			}
			node.setScore(bestNode.getScore()); // k - set score to something inf instead?
			return bestNode;
		} else {
			SearchNode bestNode = null;
			for (Move move : AiTools.getSortedLegalMoves(ruleSet, state)) {
				move.make(state, ruleSet);
				SearchNode childNode = alphaBetaSearch(state, new SearchNode(node, move), depth - 1, alpha, beta);
				move.unmake(state, ruleSet);
				if (bestNode == null || childNode.getScore() < bestNode.getScore()) {
					bestNode = childNode;
					beta = Math.min(beta, bestNode.getScore());
					if (alpha >= beta) {
						break;
					}					
				}
			}
			node.setScore(bestNode.getScore()); // k - set score to something inf instead?
			return bestNode;
		}
	}

}
