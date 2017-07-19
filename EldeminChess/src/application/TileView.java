package application;

import application.generic.HighlightPane;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

public class TileView {

	private ImageView imageView = new ImageView();
	HighlightPane node = new HighlightPane(imageView);
	
	public TileView(double width, double height) {
		imageView.setFitWidth(width);
		imageView.setFitHeight(height);
		node.getRegion().getStyleClass().add("tile");
		node.setFill(new Color(0, 0, 0, 0.5));
	}
	
	public void setImage(Image image) {
		imageView.setImage(image);
	}

	public Node getRegion() {
		return node.getRegion();
	}

	public void unselect() {
	node.setHighlight(false);
	}

	public void select() {
		node.setHighlight(true);
	}
	
}
