import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface DirectoryInterface extends Remote {
    void registerBroker(String brokerId, String brokerAddress) throws RemoteException;
    // Method to get broker for publisher based on hash-based load balancing
    String getBrokerForPublisher(String publisherName) throws RemoteException;
    String BrokerToSub(String subscriberName) throws RemoteException;
    List<String> getBrokers() throws RemoteException;
}

