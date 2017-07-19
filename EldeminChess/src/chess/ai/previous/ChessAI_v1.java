package chess.ai.previous;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import chess.ai.SearchNode_v1;
import chess.instance.board.GameState;
import chess.instance.piece.Piece;
import chess.rule.piece.move.Move;
import chess.rule.set.RuleSet;

public class ChessAI_v1 {
	
	private int[] searchSpan = { 100, 1 };
	private int bruteDepth = 3;
	
	private Thread thread;
	private RuleSet ruleSet;
	private GameState gameState;
	
	private boolean terminateSearch = false;

	private Function<SearchNode_v1, Void> onNewBestNode;
	private Function<GameState, Void> onEvaluate;
	
	public void setOnNewBestNode(Function<SearchNode_v1, Void> onNewBestNode) {
		this.onNewBestNode = onNewBestNode;
	}

	public void setOnEvaluate(Function<GameState, Void> onEvaluate) {
		this.onEvaluate = onEvaluate;
	}

	public void start(RuleSet ruleSet, GameState gameState) {
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
		terminateSearch = true;
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		thread = null;
	}
	
	private Runnable mainLoop = () -> {
		int i = 0;
		
		GameState state = gameState.copy();
		
		List<SearchNode_v1> topNodes = new ArrayList<>();
		topNodes.add(null);
		while (!terminateSearch) {
			System.out.println(topNodes.size() + " child nodes");
			int childrenPerNode = i < searchSpan.length ? searchSpan[i] : searchSpan[searchSpan.length - 1];
			System.out.println(childrenPerNode + " children per node");
			topNodes = findTopChildNodes(topNodes, childrenPerNode, state);
			i++;
			
			SearchNode_v1 newBestNode = null;
			for (SearchNode_v1 node : topNodes) {
				System.out.println("c - " + node);
				if (newBestNode == null || node.getScore() > newBestNode.getScore()) {
					newBestNode = node;
					if (onNewBestNode != null) {
						onNewBestNode.apply(node);
					}
				}
			}
			
			if (i > 5) {
				break;
			}
		}
	};

	private List<SearchNode_v1> findAllChildNodes(SearchNode_v1 parent, GameState stateAfterParent) {
		List<SearchNode_v1> topNodes = new ArrayList<>();
		for (Move move : getAllLegalMoves(ruleSet, stateAfterParent)) {
			topNodes.add(new SearchNode_v1(parent, move));
		}
		return topNodes;
	}
	
	private List<Move> getAllLegalMoves(RuleSet ruleSet, GameState gameState) {
		List<Move> moves = new ArrayList<Move>();
		gameState.getSize().forEach(pos -> {
			Piece piece = gameState.getPiece(pos);
			if (piece != null && piece.getTeam() == gameState.getTurn()) {
				moves.addAll(ruleSet.getPieceRule(piece.getId()).getAllLegalMoves(gameState, ruleSet, pos));
			}
		});
		return moves;
	}

	private List<SearchNode_v1> findTopChildNodes(List<SearchNode_v1> parentNodes, int childrenPerNode, GameState state) {
		System.out.println("Choosing " + childrenPerNode + " in each of the " + parentNodes.size() + " nodes...");
		List<SearchNode_v1> ans = new ArrayList<>();
		
		for (SearchNode_v1 parentNode : parentNodes) {
			executeAllMoves(parentNode, state);
			List<SearchNode_v1> childNodes = findAllChildNodes(parentNode, state);
			List<SearchNode_v1> bestChildNodes = new ArrayList<>();
			
			for (SearchNode_v1 childNode : childNodes) {
				childNode.setScore(bruteEvaluate(state, bruteDepth));
				insertToSortedListOfBestNodes(childrenPerNode, childNode, bestChildNodes);
			}
			
			undoAllMoves(parentNode, state);
			
			ans.addAll(bestChildNodes);
		}
		
		return ans;
	}

	private void undoAllMoves(SearchNode_v1 node, GameState state) {
		SearchNode_v1 p = node;
		while (p != null) {
			p.getMove().unmake(state, ruleSet);
			p = p.getParent();
		}
	}

	private void executeAllMoves(SearchNode_v1 node, GameState state) {
		ArrayDeque<Move> moves = new ArrayDeque<>();
		SearchNode_v1 p = node;
		while (p != null) {
			moves.push(p.getMove());
			p = p.getParent();
		}
		
		while (!moves.isEmpty()) {
			moves.pop().make(state, ruleSet);
		}
	}

	private double bruteEvaluate(GameState state, int bruteDepth) {
		if (bruteDepth == 0 || state.getWinner() != -1) {
			return evaluate(state);
		} else {
			double bestScore = -Double.MAX_VALUE;
			for (Move move : getAllLegalMoves(ruleSet, state)) {
				move.make(state, ruleSet);
				double moveScore = bruteEvaluate(state, bruteDepth - 1);
				move.unmake(state, ruleSet);
				
				if (moveScore > bestScore) {
					bestScore = moveScore;
				}
			}
			return -bestScore;
		}
	}

	/**
	 * If it's white's move, evaluate for black (positive score = black is better)
	 * @param state
	 * @return
	 */
	private double evaluate(GameState state) {
		// TODO: improve. Right now just the number of pieces
		if (state.getWinner() == state.getTurn()) {
			return -Double.MAX_VALUE;
		} else if (state.getWinner() == 1 - state.getTurn()) {
			return Double.MAX_VALUE;
		}
		
		double[] score = { 0 };
		state.getSize().forEach(pos -> {
			Piece piece = state.getPiece(pos);
			if (piece != null) {
				if (piece.getTeam() == state.getTurn()) {
					score[0]--;
				} else {
					score[0]++;
				}
			}
		});
		return score[0];
	}

	private void insertToSortedListOfBestNodes(int childrenPerNode, SearchNode_v1 child,
			List<SearchNode_v1> topChildren) {
		if (topChildren.size() < childrenPerNode) {
			topChildren.add(child);
		} else if (topChildren.get(childrenPerNode - 1).getScore() < child.getScore()) {
			for (int i = 0; i < topChildren.size(); i++) {
				if (topChildren.get(i).getScore() < child.getScore()) {
					topChildren.remove(childrenPerNode - 1);
					topChildren.add(i, child);
					break;
				}
			}
		}
	}
	
}
