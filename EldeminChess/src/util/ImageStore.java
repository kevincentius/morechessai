package util;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;

public class ImageStore {
	
	private static ImageStore instance;
	private ImageStore() {}
	public static ImageStore getInstance(){
		if(null == instance){
			instance = new ImageStore();
		}
		return instance;
	}
	
	private Map<String, Image[]> pieceImagesMap = new HashMap<>();
	private int numTeams = 2;
	
	public Image[] getPieceImages(String name) {
		Image[] images = pieceImagesMap.get(name);
		if (images == null) {
			images = new Image[numTeams];
			for (int i = 0; i < numTeams; i++) {				
				images[i] = new Image("file:data/img/piece/" + name + i + ".png");
			}
			pieceImagesMap.put(name, images);
		}
		return images;
	}
	
}
