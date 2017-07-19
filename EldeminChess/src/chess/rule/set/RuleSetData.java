package chess.rule.set;

import chess.rule.piece.PieceRuleData;

public class RuleSetData {

	public int turn;
	public Integer[][] startingPosition; // transposed
	public Integer[][] startingTeam; // transposed
	public PieceRuleData[] pieceData;
	
}
