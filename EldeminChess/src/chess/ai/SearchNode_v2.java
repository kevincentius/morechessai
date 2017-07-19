package chess.ai;

import java.util.ArrayDeque;

import chess.rule.piece.move.Move;

public class SearchNode_v2 {
	
	private SearchNode_v2 parent;
	private Move move;
	private double score;
	private int side;
	
	public SearchNode_v2(SearchNode_v2 parent, Move move, int side) {
		super();
		this.parent = parent;
		this.move = move;
		this.side = side;
	}
	public SearchNode_v2 getParent() {
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
	public int getSide() {
		return side;
	}
	@Override
	public String toString() {
		ArrayDeque<SearchNode_v2> nodes = new ArrayDeque<>();
		for (SearchNode_v2 p = this; p != null; p = p.parent) {
			nodes.push(p);
		}
		
		StringBuilder sb = new StringBuilder();
		for (SearchNode_v2 node : nodes) {
			sb.append(node.getMove() + " (" + node.getScore() + ") - ");
		}
		return sb.toString();
	}
	
}
