package chess.ai;

import java.util.ArrayList;
import java.util.List;

import chess.instance.board.GameState;
import chess.instance.piece.Piece;
import chess.rule.piece.move.Move;
import chess.rule.set.RuleSet;

public class AiTools {
	
	public static List<Move> getAllLegalMoves(RuleSet ruleSet, GameState gameState) {
		ArrayList<Move> moves = new ArrayList<Move>();
		moves.ensureCapacity(50);
		gameState.getSize().forEach(pos -> {
			Piece piece = gameState.getPiece(pos);
			if (piece != null && piece.getTeam() == gameState.getTurn()) {
				moves.addAll(ruleSet.getPieceRule(piece.getId()).getAllLegalMoves(gameState, ruleSet, pos));
			}
		});
		return moves;
	}

}
