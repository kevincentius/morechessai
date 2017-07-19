package chess.rule.set;

import chess.instance.board.GameState;
import chess.rule.piece.PieceRule;
import chess.rule.piece.trigger.TriggerRule;

public interface RuleSet {

	GameState getInitialState();
	PieceRule getPieceRule(int pieceId);
	TriggerRule getTriggerRule(int triggerId);

}
