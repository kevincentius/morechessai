package application;

import java.util.function.Function;

import chess.instance.board.Game;
import chess.instance.board.GameState;
import chess.instance.piece.Piece;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import util.Vec2i;

public class BoardView {
	
	private GridPane grid = new GridPane();
	
	private TileView[][] tiles;
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
			
			pos.in(tiles).getRegion().setOnMouseClicked(e -> {
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
		selectedTile.in(tiles).unselect();
		selectedTile = null;
	}

	private void selectTile(Vec2i pos) {
		selectedTile = pos;
		selectedTile.in(tiles).select();
	}

	private void resize(Vec2i size) {
		this.size = size;
		grid.getChildren().clear();
		tiles = new TileView[size.x][size.y];
		
		size.forEach(pos -> {
			TileView tileView = new TileView(width / size.x, height / size.y);
			tiles[pos.x][pos.y] = tileView;
			grid.add(tileView.getRegion(), pos.x, pos.y);
		});
	}

	public Region getRegion() {
		return grid;
	}
	
}
