package rmicommunication;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIInterface extends Remote {

  void setLeader(String leader) throws RemoteException;

  void chooseNewLeader(String starterId, Integer winner) throws RemoteException;

  void answerAlive(String destinationID, String nodeIDReplying) throws RemoteException;

  void sendWinnerInfoToNodes(String nodeIDString) throws RemoteException;

  boolean isAlive() throws RemoteException;
}