package rmicommunication;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class RMIServer implements RMIServerInterface {

  private static final long NODE_CHECK_TIME = 4000;
  private static String REGISTRY_HOSTNAME;
  private static Integer REGISTRY_PORT;
  private static Registry REGISTRY;
  private static RMIServerInterface stub;
  private List<String> registeredNodes;
  private boolean isServerRunning;
  private Thread serverThread;

  public RMIServer() {
    super();
    registeredNodes = new ArrayList<>();
  }

  public static void main(String[] args) {
    try {
//      REGISTRY_HOSTNAME = "127.0.0.1";
      REGISTRY_HOSTNAME = "25.31.77.86";
      REGISTRY_PORT = 5696;
      System.setProperty("java.rmi.server.hostname", REGISTRY_HOSTNAME);
      RMIServer server = new RMIServer();
      stub = (RMIServerInterface) UnicastRemoteObject.exportObject(server, (REGISTRY_PORT - 1));

      REGISTRY = LocateRegistry.createRegistry(REGISTRY_PORT);
      REGISTRY.bind("0", stub);
      System.out.println("RMI stub is registered. RMIServer is online");

      // verify node liveness
      Timer timer = new Timer();
      timer.scheduleAtFixedRate(new NodeLivenessCheckTask(server), 0, NODE_CHECK_TIME);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void startServer(String host, Integer port) {
    try {
      REGISTRY_HOSTNAME = host;
      REGISTRY_PORT = port;
      System.setProperty("java.rmi.server.hostname", REGISTRY_HOSTNAME);

      System.out.println("Starting server");
      isServerRunning = true;
      serverThread = new Thread(this::runServer);
      serverThread.start();
      System.out.println("Started server");


    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void runServer() {
    try {
      RMIServerInterface stub = (RMIServerInterface) UnicastRemoteObject.exportObject(this, (REGISTRY_PORT - 1));
      // Register the stub with the RMI registry
      REGISTRY = LocateRegistry.createRegistry(REGISTRY_PORT);
      REGISTRY.bind("0", stub);
    } catch (Exception e) {
      System.err.println("Error running server: " + e);
    }
    System.out.println("RMIServer is ready.");
    // Schedule the node liveness check task
    Timer timer = new Timer();
    timer.scheduleAtFixedRate(new NodeLivenessCheckTask(this), 0, NODE_CHECK_TIME);

    while (isServerRunning) {
      try {
        Thread.sleep(20000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    timer.cancel();
    try {
      for (String node : REGISTRY.list()) {
        if (!node.equals("0")) {
          this.unregisterNode(node, REGISTRY);
        }
      }
      REGISTRY.unbind("0");
      UnicastRemoteObject.unexportObject(this, true);
      UnicastRemoteObject.unexportObject(REGISTRY, true);
      System.out.println("Server is offline");
    } catch (Exception e) {
      System.err.println("Error when shutting down server: " + e);
    }
    serverThread.interrupt();
  }

  @Override
  public void registerNode(String nodeID, RMIInterface node) throws RemoteException {
    try {
      REGISTRY.bind(nodeID, node);
      registeredNodes.add(nodeID);
      System.out.println("Node " + nodeID + " registered.");
    } catch (Exception e) {
      System.err.println("Binding exception: " + e);
      e.printStackTrace();
    }
  }

  @Override
  public void unregisterNode(String nodeID, Registry reg) throws RemoteException {
    try {
      registeredNodes.remove(nodeID);
      reg.unbind(nodeID);
      System.out.println("Node " + nodeID + " unregistered.");
    } catch (Exception e) {
      System.err.println("Error occurred when unregistering node: " + e);
      e.printStackTrace();
    }
  }

  private void checkNodeLiveness() {
    System.out.println("Checking Node liveliness");
    List<String> nodesCopy;
    synchronized (registeredNodes) {
      nodesCopy = new ArrayList<>(registeredNodes);
    }
    try {
      for (String nodeID : nodesCopy) {
        try {
          RMIInterface nodeStub = (RMIInterface) REGISTRY.lookup(nodeID);
          nodeStub.isAlive();
        } catch (Exception e) {
          System.err.println("Node " + nodeID + " inaccessible, unregistering.");
          unregisterNode(nodeID, REGISTRY); // Node is inactive, remove it from the registry
        }
      }
    } catch (RemoteException e) {
      System.err.println("Error occurred when accessing the RMI registry: " + e);
      e.printStackTrace();
    }
  }

  private static class NodeLivenessCheckTask extends TimerTask {
    private RMIServer server;

    public NodeLivenessCheckTask(RMIServer server) {
      this.server = server;
    }

    @Override
    public void run() {
      server.checkNodeLiveness();
    }
  }

  public void stopServer() {
    isServerRunning = false;
  }
}
