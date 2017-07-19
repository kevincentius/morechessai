package application.generic;

import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

public class HighlightPane {

	private StackPane node;
	private Rectangle highlight;
	private StackPane contentBox;

	private Color highlightColor = new Color(1, 1, 1, 0.8);
	
	public HighlightPane(Node content) {
		highlight = new Rectangle();
		contentBox = new StackPane();
		node = new StackPane(contentBox, highlight);
		
		highlight.widthProperty().bind(node.widthProperty());
		highlight.heightProperty().bind(node.heightProperty());
		highlight.setVisible(false);
		highlight.setFill(highlightColor);
		highlight.setManaged(false);
		
		setContent(content);
	}
	
	public Region getRegion() {
		return node;
	}
	
	public void setContent(Node node) {
		contentBox.getChildren().clear();
		contentBox.getChildren().add(node);
	}
	
	public void setBacklight(boolean backlight) {
		if (backlight) {
			highlight.toBack();
		} else {
			highlight.toFront();
		}
	}
	
	public void setHighlightOnHoverOnly() {
		node.setOnMouseEntered(e -> {
			highlight.setVisible(true);
		});
		node.setOnMouseExited(e -> {
			highlight.setVisible(false);
		});
	}

	public void setHighlight(boolean value) {
		highlight.setVisible(value);
	}
	
	public void setFill(Paint fill) {
		highlight.setFill(fill);
	}
	
}
