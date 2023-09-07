package rmicommunication;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;

import java.util.regex.Pattern;

import javafx.event.ActionEvent;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.text.Font;
import rmicommunication.RMINode;

import java.io.OutputStream;
import java.io.PrintStream;

import javafx.application.Platform;


public class Controller {
  private String serverIP;
  private int serverPort;
  private int numberNode;
  RMINode node = new RMINode();

  private PrintStream consolePrintStream;

  @FXML // ResourceBundle that was given to the FXMLLoader
  private ResourceBundle resources;

  @FXML // URL location of the FXML file that was given to the FXMLLoader
  private URL location;

  @FXML // fx:id="Console"
  private TextArea Console; // Value injected by FXMLLoader

  @FXML // fx:id="HostIP"
  private TextField HostIP; // Value injected by FXMLLoader

  @FXML // fx:id="NodeID"
  private Spinner<Integer> NodeID; // Value injected by FXMLLoader

  @FXML // fx:id="HostPort"
  private TextField HostPort; // Value injected by FXMLLoader

  @FXML
  private Button Start;

  @FXML
  private Button Stop;

  private boolean isValidIPAddress(String ipAddress) {
    String ipPattern = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    return Pattern.matches(ipPattern, ipAddress);
  }

  private boolean isValidPortNumber(String portNumber) {
    try {
      int port = Integer.parseInt(portNumber);
      return port >= 5000 && port <= 6000;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  @FXML
  void run(ActionEvent event) {
    if (isValidIPAddress(HostIP.getText())) {
      serverIP = HostIP.getText();
      Console.appendText("Server IP: " + serverIP);
    } else {
      Console.appendText("Błędny adres IP");
      return;
    }

    if (isValidPortNumber(HostPort.getText())) {
      serverPort = Integer.parseInt(HostPort.getText());
      Console.appendText(" Server Port: " + serverPort);
    } else {
      Console.appendText("Błędny numer portu");
      return;
    }

    numberNode = NodeID.getValue();
    Console.appendText(" Node ID: " + numberNode + "\n");

    startNode();
    Start.setDisable(true);
    Stop.setDisable(false);
  }

  @FXML
  void terminate(ActionEvent event) {
    terminateNode();
    Stop.setDisable(true);
    Start.setDisable(false);
  }

  private void startNode() {
    Console.appendText("Start\n");
    node.startAlgorithm(serverIP, serverPort, Integer.toString(numberNode));
  }

  private void terminateNode() {
    Console.appendText("Stop\n");
    node.stopAlgorithm();
  }

  @FXML // This method is called by the FXMLLoader when initialization is complete
  void initialize() {
    assert Console != null : "fx:id=\"Console\" was not injected: check your FXML file 'SR.fxml'.";
    Font consoleFont = new Font("Arial", 14);
    Console.setFont(consoleFont);

    consolePrintStream = new PrintStream(new ConsoleOutputStream(Console));
    System.setOut(consolePrintStream);

    assert HostIP != null : "fx:id=\"HostIP\" was not injected: check your FXML file 'SR.fxml'.";
    assert NodeID != null : "fx:id=\"NodeID\" was not injected: check your FXML file 'SR.fxml'.";

    // Utwórz fabrykę wartości dla Spinnera
    SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1);

    Font font = new Font("Arial", 18);
    NodeID.setStyle("-fx-font: " + font.getSize() + " \"" + font.getFamily() + "\";");

    NodeID.setValueFactory(valueFactory);

    assert HostPort != null : "fx:id=\"HostPort\" was not injected: check your FXML file 'SR.fxml'.";
    assert Start != null : "fx:id=\"Start\" was not injected: check your FXML file 'SR.fxml'.";
    assert Stop != null : "fx:id=\"Stop\" was not injected: check your FXML file 'SR.fxml'.";

  }

  private static class ConsoleOutputStream extends OutputStream {
    private final TextArea console;

    public ConsoleOutputStream(TextArea console) {
      this.console = console;
    }

    @Override
    public void write(int b) {
      console.appendText(String.valueOf((char) b));
    }

    @Override
    public void write(byte[] b, int off, int len) {
      String text = new String(b, off, len);
      Platform.runLater(() -> console.appendText(text));
    }

    @Override
    public void write(byte[] b) {
      write(b, 0, b.length);
    }
  }

}
