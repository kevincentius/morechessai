package chess.rule.piece.move;

import chess.instance.board.GameState;
import chess.rule.set.RuleSet;
import util.Vec2i;

public interface Move {

	public void make(GameState gameState, RuleSet ruleSet);
	public void unmake(GameState gameState, RuleSet ruleSet);
	
	public double getTacticalValue(GameState gameState, double[] pieceValues);
	
	public Vec2i getFrom();
	public Vec2i getTo();
	String getActionName(RuleSet ruleSet);
	
}
