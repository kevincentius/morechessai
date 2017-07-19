package chess.rule.piece.move;

import chess.instance.board.GameState;
import chess.rule.set.RuleSet;

public interface Move {

	public void make(GameState gameState, RuleSet ruleSet);
	public void unmake(GameState gameState, RuleSet ruleSet);
	
	public double getTacticalValue(GameState gameState, double[] pieceValues);
	
}
