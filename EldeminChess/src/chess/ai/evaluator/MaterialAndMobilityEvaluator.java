package chess.ai.evaluator;

import chess.ai.AiTools;
import chess.instance.board.GameState;
import chess.instance.piece.Piece;
import chess.rule.set.RuleSet;

public class MaterialAndMobilityEvaluator {

	int evalCount = 0;
	long totalMs = 0;
	
	private double[] values;
	private double mobilityValue;
	
	public MaterialAndMobilityEvaluator(double[] values, double mobilityValue) {
		this.values = values;
		this.mobilityValue = mobilityValue;
	}
	
	public double evaluate(GameState state, RuleSet ruleSet) {
		switch (state.getWinner()) {
		case 0:
			return Double.MAX_VALUE;
		case 1:
			return -Double.MAX_VALUE;
		default:
			long ms = System.currentTimeMillis();
			double[] materialScore = { 0 };
			state.getSize().forEach(pos -> {
				Piece piece = state.getPiece(pos);
				if (piece != null) {
					if (piece.getTeam() == 0) {
						materialScore[0] += values[piece.getId()];
					} else {
						materialScore[0] -= values[piece.getId()];
					}
				}
			});

			int turn = state.getTurn();
			state.setTurn(0);
			double mobilityScore = AiTools.getAllLegalMoves(ruleSet, state).size();
			state.setTurn(1);
			mobilityScore -= AiTools.getAllLegalMoves(ruleSet, state).size();
			state.setTurn(turn);
			mobilityScore *= mobilityValue;
			totalMs += System.currentTimeMillis() - ms;
			evalCount++;
			return materialScore[0] + mobilityScore;
		}
	}

	public void printDebugAndReset() {
		System.out.println(evalCount + " positions evaluated in " + totalMs + " ms");
		evalCount = 0;
		totalMs = 0;
	}
	
}
