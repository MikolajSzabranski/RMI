package rmicommunication;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;
import rmicommunication.RMIServer;


public class ServerController {
  private String serverIP;
  private int serverPort;
  RMIServer server = new RMIServer();
  @FXML
  private Button Start;
  private PrintStream consolePrintStream;

  @FXML
  private ResourceBundle resources;

  @FXML
  private URL location;

  @FXML
  private TextArea Console;

  @FXML
  private TextField HostPort;
  @FXML
  private Button Stop;
  @FXML
  private TextField HostIP;


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
      Console.appendText("\nServer Port: " + serverPort);
    } else {
      Console.appendText("Błędny numer portu");
      return;
    }
    runServer();
    Start.setDisable(true);
    Stop.setDisable(false);
  }

  @FXML
  void terminate(ActionEvent event) {
    terminateServer();
    Stop.setDisable(true);
    Start.setDisable(false);
  }

  private void runServer() {
    server.startServer(serverIP, serverPort);
  }

  private void terminateServer() {
    server.stopServer();
  }

  @FXML
  void initialize() {
    assert Console != null : "fx:id=\"Console\" was not injected: check your FXML file 'SRServer.fxml'.";

    Font consoleFont = new Font("Arial", 14);
    Console.setFont(consoleFont);

    consolePrintStream = new PrintStream(new ConsoleOutputStream(Console));

    System.setOut(consolePrintStream);

    assert HostIP != null : "fx:id=\"HostIP\" was not injected: check your FXML file 'SRServer.fxml'.";
    assert HostPort != null : "fx:id=\"HostPort\" was not injected: check your FXML file 'SRServer.fxml'.";
    assert Start != null : "fx:id=\"Start\" was not injected: check your FXML file 'SRServer.fxml'.";
    assert Stop != null : "fx:id=\"Stop\" was not injected: check your FXML file 'SRServer.fxml'.";
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