package rmicommunication;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.net.*;
import java.util.Enumeration;

public class RMINode implements RMIInterface {
  private static ScheduledExecutorService EXECUTOR;
  private static String REGISTRY_HOSTNAME;
  private static Integer REGISTRY_PORT;
  private static String THIS_NODE_ID;
  private static String LEADER;
  private boolean running;
  static RMIInterface nodeStub;
  private Thread thread;
  Registry registry;

  @Override
  public void setLeader(String leader) throws RemoteException {
    RMINode.LEADER = leader;
  }

  public RMINode() {
    running = false;
  }

  public static void main(String[] args) {
    RMINode node = new RMINode();
    node.startAlgorithm("25.31.77.86", 5696, "11");
  }

  private String getLocalIPAddress() {
    try {
      String last_address = "127.0.0.1";
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface networkInterface = interfaces.nextElement();

        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
        while (addresses.hasMoreElements()) {
          InetAddress address = addresses.nextElement();
          if (networkInterface.getDisplayName().equals("LogMeIn Hamachi Virtual Ethernet Adapter")) {
            last_address = address.getHostAddress();
            return last_address;
          }
          if (!address.isLoopbackAddress() && !address.isLinkLocalAddress() && address.isSiteLocalAddress()) {
            last_address = address.getHostAddress();
          }
        }
      }
      return last_address;
    } catch (SocketException e) {
      e.printStackTrace();
    }

    return null;
  }

  public void startAlgorithm(String host, Integer port, String id) {
    String localIPAddress = getLocalIPAddress();
    if (localIPAddress != null) {
      System.setProperty("java.rmi.server.hostname", localIPAddress);
      System.out.println("RMI IP set to: " + localIPAddress);
    } else {
      System.err.println("Failed to set IP address.");
    }

    System.out.println("Starting algorithm");
    thread = new Thread(() -> runAlgorithm(host, port, id));
    running = true;
    thread.start();
  }

  public void runAlgorithm(String host, Integer port, String id) {
    try {
      System.out.println("Start algorithm");
      REGISTRY_HOSTNAME = host;
      REGISTRY_PORT = port;
      THIS_NODE_ID = id;

      registry = LocateRegistry.getRegistry(REGISTRY_HOSTNAME, REGISTRY_PORT);
      RMIServerInterface serverStub = (RMIServerInterface) registry.lookup("0");

      nodeStub = (RMIInterface) UnicastRemoteObject.exportObject(this,
          Integer.parseInt(THIS_NODE_ID) + REGISTRY_PORT);

      serverStub.registerNode(THIS_NODE_ID, nodeStub);

      nodeStub.chooseNewLeader(THIS_NODE_ID, Integer.valueOf(THIS_NODE_ID));
      // Coordinator check task
      EXECUTOR = Executors.newScheduledThreadPool(1);
//      executor.scheduleAtFixedRate(this::checkCoordinatorStatus, 0, 10, TimeUnit.SECONDS);
      while (running) {
        try {
          Thread.sleep(7000);
          System.out.println("LEADER: " + LEADER);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      if (EXECUTOR != null) {
        EXECUTOR.shutdownNow();
      }
      if (nodeStub != null) {
        serverStub.unregisterNode(THIS_NODE_ID, registry);
        System.out.println("Unbound and unregistered");
        UnicastRemoteObject.unexportObject(this, true);
        System.out.println("Unexported");
      }
      thread.interrupt();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void chooseNewLeader(String starterId, Integer winner) throws RemoteException {
    if (winner == null || winner < Integer.parseInt(THIS_NODE_ID)) {
      winner = Integer.valueOf(THIS_NODE_ID);
    }
    if (!Arrays.stream(registry.list()).skip(registry.list().length - 1).findFirst().get().equals(THIS_NODE_ID)) {
//    if (!Objects.equals(starterId, THIS_NODE_ID) || (winner == null && Objects.equals(starterId, THIS_NODE_ID))) {
      boolean ifCurrent = false;
      System.out.println("LIST: " + Arrays.toString(registry.list()));
      for (String temp : registry.list()) {
        if (ifCurrent) {
          try {
            RMIInterface stub = (RMIInterface) registry.lookup(temp);
            System.out.println("Call election method in next node: " + temp);
            stub.chooseNewLeader(starterId, winner);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        ifCurrent = Objects.equals(temp, THIS_NODE_ID);
      }
    } else {
      for (String winnerNode : registry.list()) {
        if (winner.toString().equals(winnerNode)) {
          try {
            RMIInterface stub = (RMIInterface) registry.lookup(winnerNode);
            System.out.println("Send victory info");
            stub.sendWinnerInfoToNodes(winner.toString());
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
//      answerAlive(nodeIDString, thisNodeIDString);
    }
  }

  @Override
  public void sendWinnerInfoToNodes(String winner) throws RemoteException {
    LEADER = winner;
    System.out.println("Send message to other nodes about his win");
    Arrays.stream(registry.list()).forEach(node -> {
          RMIInterface stub;
          if (!node.equals(THIS_NODE_ID) && !node.equals("0")) {
            try {
              stub = (RMIInterface) registry.lookup(node);
              System.out.println("Send info about new leader to " + node);
              stub.setLeader(winner);
              System.out.println("LEADER: " + LEADER);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
    );
  }

  @Override
  public void answerAlive(String destinationIDString, String nodeIDReplying) throws RemoteException {
    if (!THIS_NODE_ID.equals(destinationIDString)) {
      try {
        RMIInterface stub = (RMIInterface) registry.lookup(destinationIDString);
        System.out.println("Sending alive message to " + destinationIDString);
        stub.answerAlive(destinationIDString, THIS_NODE_ID);
        // start election after sending OK
        chooseNewLeader(THIS_NODE_ID, Integer.valueOf(THIS_NODE_ID));
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      // receive OK
      System.out.println(nodeIDReplying + " is Alive");
    }
  }

  public boolean isAlive() throws RemoteException {
    return true;
  }

//
//  private void checkCoordinatorStatus() {
//    try {
//      System.out.println("Coordinator check");
//      RMIInterface coordinatorStub = (RMIInterface) registry.lookup(leader);
//      coordinatorStub.isAlive();
//    } catch (RemoteException e) {
//      System.out.println("Exception, inactive coordinator ");
//      coordinatorCrashed();
//    } catch (Exception e) {
//      System.out.println("Exception, inactive coordinator");
//      coordinatorCrashed();
//    }
//  }

  //  private static void coordinatorCrashed() {
//    System.out.println("Coordinator inactive, starting elections");
//    try {
//      nodeStub.electionMessage(thisNodeIDString, Integer.valueOf(thisNodeIDString));
//    } catch (RemoteException e) {
//      e.printStackTrace();
//    }
//  }
  public void stopAlgorithm() {
    running = false;
  }
}
