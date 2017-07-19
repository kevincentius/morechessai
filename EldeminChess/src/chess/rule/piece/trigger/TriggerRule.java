package chess.rule.piece.trigger;

import chess.instance.board.GameState;
import chess.instance.piece.Piece;
import util.Vec2i;

public interface TriggerRule {
	
	void make(GameState state, Piece activator, Vec2i pos);
	void unmake(GameState state, Piece activator, Vec2i pos);
	
}
