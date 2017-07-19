package chess.rule.piece.trigger;

import chess.instance.piece.Piece;
import util.Vec2i;

public class Trigger {
	
	public int id;
	public Piece activator;
	public Vec2i pos;
	
	public Trigger(int id, Piece activator, Vec2i pos) {
		super();
		this.id = id;
		this.activator = activator;
		this.pos = pos;
	}
	
}
