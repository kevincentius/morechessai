package chess.rule.piece;

import chess.rule.piece.SlideRule.SlideRuleData;

public class PieceRuleData {
	
	public String image;
	public boolean blocking = true;
	public int[] promotionList;
	
	public SlideRuleData[] slideMove;
	public SlideRuleData[] slideCapture;
	public SlideRuleData[] slideSwap;
	
	public Boolean[][] jumpMove;
	public Boolean[][] jumpCapture;
	public Boolean[][] jumpSwap;
	
	public int[] deathTriggers;
	
}
