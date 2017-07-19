package chess.ai;

import java.util.function.Function;

import chess.instance.board.GameState;
import chess.rule.set.RuleSet;

public interface ChessAI {
	
	public void start(RuleSet ruleSet, GameState gameState, Function<SearchNode, Void> onNewBestNode, long maxTimeMs);
	public void setOnNewBestNode(Function<SearchNode, Void> onNewBestNode);
	public void setOnEvaluationFinished(Runnable onTimeLimitReached);
	public void stop();
	public void setOnDebug(Function<GameState, Void> onDebug);
	
}
