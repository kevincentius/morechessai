package chess.rule.piece.move;

import java.util.ArrayList;
import java.util.List;

import chess.instance.board.GameState;
import chess.instance.piece.Piece;
import chess.rule.piece.trigger.Trigger;
import chess.rule.set.RuleSet;
import util.Vec2i;

public class MoveImpl implements Move {

	private Vec2i from;
	private Vec2i to;
	private boolean swap;
	private int promoteTo;
	
	
	
	// ----- data that are only available after the move is made -----
	private Piece piece; // for promotion
	private Piece targetPiece;
	private int turn;
	private List<Trigger> triggers;
	// ---------------------------------------------------------------

	
	
	public MoveImpl(Vec2i from, Vec2i to, boolean swap, int promoteTo) {
		this.from = from;
		this.to = to;
		this.swap = swap;
		this.promoteTo = promoteTo;
	}

	@Override
	public void make(GameState state, RuleSet ruleSet) {
		piece = state.getPiece(from);
		targetPiece = state.getPiece(to);
		turn = state.getTurn();

		Piece resultingFromPiece = null;
		Piece resultingToPiece = promoteTo == -1 ? piece : new Piece(promoteTo, turn);
		if (swap) {
			int[] promotionList = ruleSet.getPieceRule(targetPiece.getId()).getPromotionList();
			int opponentTeam = 1 - turn;
			if (promotionList != null && state.isPromotingSquare(from, opponentTeam)) {
				resultingFromPiece = new Piece(promotionList[0], opponentTeam);
			} else {
				resultingFromPiece = targetPiece;
			}
		}
		
		if (!swap
				&& targetPiece != null
				&& ruleSet.getPieceRule(targetPiece.getId()).getDeathTriggers() != null) {
			triggers = new ArrayList<>();
			for (int triggerId : ruleSet.getPieceRule(targetPiece.getId()).getDeathTriggers()) {
				Trigger trigger = new Trigger(triggerId, targetPiece, to);
				triggers.add(trigger);
				
				ruleSet.getTriggerRule(triggerId).make(state, targetPiece, to);
			}
		}
		
		state.setPiece(from, resultingFromPiece);
		state.setPiece(to, resultingToPiece);
		state.nextTurn();
	}
	
	@Override
	public void unmake(GameState state, RuleSet ruleSet) {
		if (triggers != null) {
			for (int i = triggers.size() - 1; i >= 0; i--) {
				Trigger trigger = triggers.get(i);
				
				ruleSet.getTriggerRule(trigger.id).unmake(state, trigger.activator, trigger.pos);
			}
		}
		
		state.setPiece(from, piece);
		state.setPiece(to, targetPiece);
		state.setTurn(turn);
	}

	@Override
	public String toString() {
		return from.x + ", " + from.y + " -> " + to.x + ", " + to.y;
	}

	@Override
	public double getTacticalValue(GameState state, double[] pieceValues) {
		if (!swap) {
			Piece capturedPiece = state.getPiece(to);
			if (capturedPiece != null) {
				return pieceValues[capturedPiece.getId()];
			}
		}
		return 0;
		/*&& targetPiece.getTeam() != state.getTurn()) {
			return pieceValues[targetPiece.getId()];
		} else {
			return 0;
		}*/
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + promoteTo;
		result = prime * result + (swap ? 1231 : 1237);
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MoveImpl other = (MoveImpl) obj;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		if (promoteTo != other.promoteTo)
			return false;
		if (swap != other.swap)
			return false;
		if (to == null) {
			if (other.to != null)
				return false;
		} else if (!to.equals(other.to))
			return false;
		return true;
	}

}
