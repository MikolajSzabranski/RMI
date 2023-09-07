package rmicommunication;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class RMIAppServer extends Application {

  @Override
  public void start(Stage primaryStage) throws Exception {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("SRServer.fxml"));
    Parent root = loader.load();
//    ServerController controller = loader.getController();
    Scene scene = new Scene(root);

    primaryStage.setTitle("Tanenbaum");
    primaryStage.setScene(scene);
    primaryStage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}