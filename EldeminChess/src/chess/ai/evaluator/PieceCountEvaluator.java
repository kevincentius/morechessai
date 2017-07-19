package chess.ai.evaluator;

import chess.instance.board.GameState;
import chess.instance.piece.Piece;

public class PieceCountEvaluator {
	
	public double evaluate(GameState state) {
		// TODO: improve. Right now just the number of pieces
		switch (state.getWinner()) {
		case 0:
			return Double.MAX_VALUE;
		case 1:
			return -Double.MAX_VALUE;
		default:
			double[] score = { 0 };
			state.getSize().forEach(pos -> {
				Piece piece = state.getPiece(pos);
				if (piece != null) {
					if (piece.getTeam() == 0) {
						score[0]++;
					} else {
						score[0]--;
					}
				}
			});
			return score[0];
		}
	}
	
}
