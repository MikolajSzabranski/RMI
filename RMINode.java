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
//    node.startAlgorithm("127.0.0.1", 5696, "7");
    node.startAlgorithm("25.31.77.86", 5696, "1");
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
    thread = new Thread(() -> runAlgorithm(host, port, id));
    running = true;
    thread.start();
    System.out.println("Started algorithm");
  }

  public void runAlgorithm(String host, Integer port, String id) {
    try {
      System.out.println("Algorithm running");
      REGISTRY_HOSTNAME = host;
      REGISTRY_PORT = port;
      THIS_NODE_ID = id;

      registry = LocateRegistry.getRegistry(REGISTRY_HOSTNAME, REGISTRY_PORT);
      RMIServerInterface serverStub = (RMIServerInterface) registry.lookup("0");

      nodeStub = (RMIInterface) UnicastRemoteObject.exportObject(this,
          Integer.parseInt(THIS_NODE_ID) + REGISTRY_PORT);

      serverStub.registerNode(THIS_NODE_ID, nodeStub);

      RMIInterface stub = (RMIInterface) registry.lookup(Arrays.stream(registry.list()).findFirst().get());

      nodeStub.electionMessage(THIS_NODE_ID, Integer.valueOf(stub.getLeader()), true);
      // Schedule the coordinator check task
      EXECUTOR = Executors.newScheduledThreadPool(1);
//      executor.scheduleAtFixedRate(this::checkCoordinatorStatus, 0, 10, TimeUnit.SECONDS);
      System.out.println("Algorithm running properly?");
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
  public void electionMessage(String starterId, Integer winner, boolean firstLoop) throws RemoteException {
    if (!Objects.equals(starterId, THIS_NODE_ID) && !firstLoop) {
      if (winner < Integer.parseInt(THIS_NODE_ID)) {
        winner = Integer.valueOf(THIS_NODE_ID);
      }
      boolean ifCurrent = false;
      System.out.println("LIST: " + Arrays.toString(registry.list()));
      for (String temp : registry.list()) {
        System.out.println("test: " + temp + "  " + ifCurrent);
        if (ifCurrent) {
          System.out.println("NEXT: " + temp);
          try {
            RMIInterface stub = (RMIInterface) registry.lookup(temp);
            System.out.println("Call election method in next node: " + temp);
            stub.electionMessage(starterId, winner, false);
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
            System.out.println("Informing node " + winnerNode + " of this node's victory");
            stub.victoryMessage(winner.toString());
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
//      answerAlive(nodeIDString, thisNodeIDString);
    }
  }

  @Override
  public void victoryMessage(String winner) throws RemoteException {
    LEADER = winner;
    System.out.println("Leader send message to other nodes about his win");
    Arrays.stream(registry.list()).forEach(node -> {
          RMIInterface stub;
          if (!node.equals(THIS_NODE_ID) && !node.equals("0")) {
            try {
              stub = (RMIInterface) registry.lookup(node);
              System.out.println("Node " + node + " is informed about new leader");
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
        electionMessage(THIS_NODE_ID, Integer.valueOf(THIS_NODE_ID), false);
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

  @Override
  public String getLeader(){
    return LEADER;
  }
}
