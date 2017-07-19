package chess.rule.piece;

import java.util.List;

import chess.instance.board.GameState;
import chess.rule.piece.move.Move;
import chess.rule.set.RuleSet;
import javafx.scene.image.Image;
import util.Vec2i;

public interface PieceRule {
	
	public List<Move> getAllLegalMoves(GameState game, RuleSet ruleSet, Vec2i position);
	public Image getImage(int team);
	public boolean isBlocking();
	public int[] getPromotionList();
	
	public int[] getDeathTriggers();
	public String getName();
	public int getTransformTo();
	
}
