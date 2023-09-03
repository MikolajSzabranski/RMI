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
  private static String thisNodeID;
  private static String leader;
  private static ScheduledExecutorService executor;
  private static String registryHostname;
  private static Integer registryPort;
  static RMIInterface nodeStub;
  private boolean isAlgorithmRunning;
  private Thread algorithmThread;
  Registry registry;

  @Override
  public void setLeader(String leader) throws RemoteException {
    RMINode.leader = leader;
  }

  public RMINode() {
    isAlgorithmRunning = false;
  }

  public static void main(String[] args) {
    RMINode node = new RMINode();
    node.startAlgorithm("127.0.0.1", 5696, "7");
//    node.startAlgorithm("10.0.2.6", 5696, "7");
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
      System.out.println("Local RMI IP set to: " + localIPAddress);
    } else {
      System.err.println("Failed to determine the local IP address.");
    }

    System.out.println("Starting algorithm");
    isAlgorithmRunning = true;
    algorithmThread = new Thread(() -> runAlgorithm(host, port, id));
    algorithmThread.start();
    System.out.println("Started algorithm");
  }

  public void runAlgorithm(String host, Integer port, String id) {
    try {
      System.out.println("Algorithm running");
      registryHostname = host;
      registryPort = port;
      thisNodeID = id;

      registry = LocateRegistry.getRegistry(registryHostname, registryPort);
      RMIServerInterface serverStub = (RMIServerInterface) registry.lookup("0");

      nodeStub = (RMIInterface) UnicastRemoteObject.exportObject(this,
          Integer.parseInt(thisNodeID) + registryPort);

      serverStub.registerNode(thisNodeID, nodeStub);

      nodeStub.electionMessage(thisNodeID, null);
      // Schedule the coordinator check task
      executor = Executors.newScheduledThreadPool(1);
//      executor.scheduleAtFixedRate(this::checkCoordinatorStatus, 0, 10, TimeUnit.SECONDS);
      System.out.println("Algorithm running properly?");
      while (isAlgorithmRunning) {
        try {
          Thread.sleep(7000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      if (executor != null) {
        executor.shutdownNow();
      }
      if (nodeStub != null) {
        serverStub.unregisterNode(thisNodeID, registry);
        System.out.println("Unbound and unregistered");
        UnicastRemoteObject.unexportObject(this, true);
        System.out.println("Unexported");
      }
      algorithmThread.interrupt();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void stopAlgorithm() {
    isAlgorithmRunning = false;
  }

  @Override
  public void electionMessage(String starterId, Integer winner) throws RemoteException {
    if (!Objects.equals(starterId, thisNodeID)) {
      if (winner < Integer.parseInt(thisNodeID)) {
        winner = Integer.parseInt(thisNodeID);
      }
      boolean ifCurrent = false;
      for (String temp : registry.list()) {
        if (ifCurrent) {
          try {
            RMIInterface stub = (RMIInterface) registry.lookup(temp);
            System.out.println("Call election method in next node: " + temp);
            stub.electionMessage(starterId, winner);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        ifCurrent = Objects.equals(temp, thisNodeID);
      }
    } else {
      for (String winnerNode : registry.list()) {
        try {
          RMIInterface stub = (RMIInterface) registry.lookup(winnerNode);
          System.out.println("Informing node " + winnerNode + " of this node's victory");
          stub.victoryMessage(winner.toString());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
//      answerAlive(nodeIDString, thisNodeIDString);
    }
  }

  @Override
  public void victoryMessage(String winner) throws RemoteException {
    leader = winner;
//    if (winner.equals(thisNodeIDString)) {
    System.out.println("Leader send message to other nodes about his win");
    Arrays.stream(registry.list()).forEach(node -> {
          RMIInterface stub;
          if (!node.equals(thisNodeID) && !node.equals("0")) {
            try {
              stub = (RMIInterface) registry.lookup(node);
              System.out.println("Node " + node + " is informed about new leader");
              stub.setLeader(winner);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
    );
  }

  @Override
  public void answerAlive(String destinationIDString, String nodeIDReplying) throws RemoteException {
    if (!thisNodeID.equals(destinationIDString)) {
      try {
        RMIInterface stub = (RMIInterface) registry.lookup(destinationIDString);
        System.out.println("Sending alive message to " + destinationIDString);
        stub.answerAlive(destinationIDString, thisNodeID);
        // start election after sending OK
        electionMessage(thisNodeID, Integer.valueOf(thisNodeID));
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
}
