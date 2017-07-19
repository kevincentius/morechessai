package chess.ai;

import java.util.ArrayDeque;

import chess.rule.piece.move.Move;

public class SearchNode_v1 {
	
	private SearchNode_v1 parent;
	private Move move;
	private double score;
	public SearchNode_v1(SearchNode_v1 parent, Move move) {
		super();
		this.parent = parent;
		this.move = move;
	}
	public SearchNode_v1 getParent() {
		return parent;
	}
	public Move getMove() {
		return move;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	@Override
	public String toString() {
		ArrayDeque<SearchNode_v1> nodes = new ArrayDeque<>();
		for (SearchNode_v1 p = this; p != null; p = p.parent) {
			nodes.push(p);
		}
		
		StringBuilder sb = new StringBuilder();
		for (SearchNode_v1 node : nodes) {
			sb.append(node.getMove() + " (" + node.getScore() + ") - ");
		}
		return sb.toString();
	}
	
}
