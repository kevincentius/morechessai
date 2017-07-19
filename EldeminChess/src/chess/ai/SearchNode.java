package chess.ai;

import chess.rule.piece.move.Move;

public class SearchNode {
	
	private SearchNode bestChild;
	private Move move;
	private double score;
	
	public SearchNode(Move move) {
		super();
		this.move = move;
	}
	public SearchNode getBestChild() {
		return bestChild;
	}
	public void setBestChild(SearchNode bestChild) {
		this.bestChild = bestChild;
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
		StringBuilder sb = new StringBuilder();
		for (SearchNode p = this; p != null; p = p.bestChild) {
			sb.append(p.getMove() + " (" + p.getScore() + ") - ");
		}
		return sb.toString();
	}
	
}
