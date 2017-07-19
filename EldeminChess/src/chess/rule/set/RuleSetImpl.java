package chess.rule.set;

import chess.instance.board.GameState;
import chess.instance.piece.Piece;
import chess.rule.piece.PieceRule;
import chess.rule.piece.PieceRuleData;
import chess.rule.piece.PieceRuleImpl;
import chess.rule.piece.trigger.LossTriggerRule;
import chess.rule.piece.trigger.TriggerRule;
import util.Vec2i;

public class RuleSetImpl implements RuleSet {

	private int turn;
	private Piece[][] startPos;
	private PieceRule[] pieceRules;
	private TriggerRule[] triggerRules;
	
	public RuleSetImpl(RuleSetData ruleSetData) {
		super();
		turn = ruleSetData.turn;
		initStartPos(ruleSetData);
		initPieceRules(ruleSetData.pieceData);
		initTriggerRules();
	}

	private void initStartPos(RuleSetData ruleSetData) {
		startPos = new Piece[ruleSetData.startingPosition[0].length][ruleSetData.startingPosition.length];
		Vec2i.forEach(startPos, pos -> {
			if (pos.in(ruleSetData.startingPosition) != -1) {
				startPos[pos.y][pos.x] = new Piece(pos.in(ruleSetData.startingPosition), pos.in(ruleSetData.startingTeam));				
			}
		});
	}

	private void initPieceRules(PieceRuleData[] pieceData) {
		pieceRules = new PieceRule[pieceData.length];
		for (int i = 0; i < pieceData.length; i++) {
			pieceRules[i] = new PieceRuleImpl(pieceData[i]);
		}
	}

	private void initTriggerRules() {
		triggerRules = new TriggerRule[1];
		triggerRules[0] = new LossTriggerRule();
	}

	@Override
	public GameState getInitialState() {
		GameState state = new GameState(new Vec2i(startPos.length, startPos[0].length));
		state.setTurn(turn);
		Vec2i.forEach(startPos, pos -> {
			if (pos.in(startPos) != null) {
				state.setPiece(pos, pos.in(startPos).copy());
			}
		});
		
		return state;
	}

	@Override
	public PieceRule getPieceRule(int pieceId) {
		return pieceRules[pieceId];
	}

	@Override
	public TriggerRule getTriggerRule(int triggerId) {
		return triggerRules[triggerId];
	}
	
}
