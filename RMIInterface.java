package rmicommunication;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIInterface extends Remote {

  void setLeader(String leader) throws RemoteException;

//  void electionMessage(String starterId, Integer winner) throws RemoteException;

  void answerAlive(String destinationID, String nodeIDReplying) throws RemoteException;

  void electionMessage(String starterId, Integer winner, boolean firstLoop) throws RemoteException;

  void victoryMessage(String nodeIDString) throws RemoteException;

  boolean isAlive() throws RemoteException;

  String getLeader();
}