package application;

import java.util.function.Function;

import chess.instance.board.Game;
import chess.instance.board.GameState;
import chess.instance.piece.Piece;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import util.Vec2i;

public class BoardView {
	
	private GridPane grid = new GridPane();
	
	private ImageView[][] tiles;
	private Vec2i size = new Vec2i(0, 0);

	private double width = 750;
	private double height = 750;
	
	private Vec2i selectedTile = null;
	
	private Function<Vec2i[], Void> onMoveTargetted;
	
	public void setOnMoveTargetted(Function<Vec2i[], Void> onMoveTargetted) {
		this.onMoveTargetted = onMoveTargetted;
	}

	public BoardView() {
		grid.setPrefSize(width, height);
	}
	
	public void showBoard(Game game) {
		GameState gameState = game.getGameState();
		if (!size.equals(gameState.getSize())) {
			resize(gameState.getSize());
		}
		
		size.forEach(pos -> {
			Piece piece = gameState.getPiece(pos);
			Image image = piece == null ? null : game.getRuleSet().getPieceRule(piece.getId()).getImage(piece.getTeam());
			pos.in(tiles).setImage(image);
			
			pos.in(tiles).setPickOnBounds(true);
			pos.in(tiles).setOnMouseClicked(e -> {
				if (selectedTile == null) {
					selectTile(pos);
				} else {
					if (selectedTile.equals(pos)) {
						unselectTile();
					} else {
						if (onMoveTargetted != null) {
							onMoveTargetted.apply(new Vec2i[]{ selectedTile, pos });
						}
						unselectTile();
					}
				}
			});
		});
	}

	private void unselectTile() {
		selectedTile.in(tiles).setBlendMode(null);
		selectedTile = null;
	}

	private void selectTile(Vec2i pos) {
		selectedTile = pos;
		pos.in(tiles).setBlendMode(BlendMode.DIFFERENCE);
	}

	private void resize(Vec2i size) {
		this.size = size;
		grid.getChildren().clear();
		tiles = new ImageView[size.x][size.y];
		
		size.forEach(pos -> {
			ImageView imageView = new ImageView();
			imageView.setFitWidth(width / size.x);
			imageView.setFitHeight(height / size.y);
			tiles[pos.x][pos.y] = imageView;
			StackPane tilePane = new StackPane(imageView);
			tilePane.getStyleClass().add("tile");
			grid.add(tilePane, pos.x, pos.y);
		});
	}

	public Region getRegion() {
		return grid;
	}
	
}
