package rmicommunication;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

public interface RMIServerInterface extends Remote {
    void registerNode(String nodeID, RMIInterface node) throws RemoteException;
    void unregisterNode(String nodeID, Registry reg) throws RemoteException;
}
