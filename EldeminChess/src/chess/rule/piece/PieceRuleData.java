package chess.rule.piece;

import chess.rule.piece.SlideRule.SlideRuleData;

public class PieceRuleData {
	
	public String name;
	public String image;
	public boolean blocking = true;
	public int[] promotionList;
	public int transformTo = -1; // after first move, e.g. chess pawn cannot move two spaces anymore
	
	public SlideRuleData[] slideMove;
	public SlideRuleData[] slideCapture;
	public SlideRuleData[] slideSwap;
	
	public Boolean[][] jumpMove;
	public Boolean[][] jumpCapture;
	public Boolean[][] jumpSwap;
	
	public int[] deathTriggers;
	
	public double value;
	
}
