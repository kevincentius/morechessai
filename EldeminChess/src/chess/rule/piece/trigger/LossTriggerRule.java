package chess.rule.piece.trigger;

import chess.instance.board.GameState;
import chess.instance.piece.Piece;
import util.Vec2i;

public class LossTriggerRule implements TriggerRule {

	private int prevWinner;
	
	@Override
	public void make(GameState state, Piece activator, Vec2i pos) {
		prevWinner = state.getWinner();
		if (state.getWinner() == -1) {
			state.setWinner(1 - activator.getTeam());
		}
	}

	@Override
	public void unmake(GameState state, Piece activator, Vec2i pos) {
		state.setWinner(prevWinner);
	}
	
		
	
}
