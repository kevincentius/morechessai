package application;

import java.util.List;

import chess.rule.piece.move.Move;
import chess.rule.set.RuleSet;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class MoveListView {

	public static interface MoveSelectListener {
		void onMoveSelected(Move move);
	}
	
	private VBox node = new VBox();
	
	private MoveSelectListener listener;
	
	public MoveListView() {
		node.setPrefWidth(200);
		node.setSpacing(50);
	}
	
	public Region getRegion() {
		return node;
	}
	
	public void setMoves(List<Move> moves, RuleSet ruleSet) {
		clearMoves();
		for (Move move : moves) {
			Label label = new Label(move.getActionName(ruleSet));
			label.setOnMouseClicked(e -> {
				if (listener != null) {
					listener.onMoveSelected(move);
				}
			});
			label.getStyleClass().add("moveName");
			node.getChildren().add(label);
		}
	}
	
	public void setOnMoveSelected(MoveSelectListener listener) {
		this.listener = listener;
	}

	public void clearMoves() {
		node.getChildren().clear();
	}
	
}
