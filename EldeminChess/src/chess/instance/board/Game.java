package chess.instance.board;

import chess.rule.set.RuleSet;

public class Game {
	
	private RuleSet ruleSet;
	private GameState gameState;
	
	public Game(RuleSet ruleSet) {
		this.ruleSet = ruleSet;
		gameState = ruleSet.getInitialState();
	}
	public RuleSet getRuleSet() {
		return ruleSet;
	}
	public void setRuleSet(RuleSet ruleSet) {
		this.ruleSet = ruleSet;
	}
	public GameState getGameState() {
		return gameState;
	}
	public void setGameState(GameState gameState) {
		this.gameState = gameState;
	}
	
}
