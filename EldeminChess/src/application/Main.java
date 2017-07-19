package application;
	
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {
	
	@Override
	public void start(Stage primaryStage) {
		try {
			Scene scene = new Scene(new TestView().getRegion());
			/*try (Scanner s = new Scanner(getClass().getResourceAsStream("/application.css"))) {
				s.useDelimiter("\\A");
				String result = s.next();
			}*/
			scene.getStylesheets().add("/application.css");				
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
