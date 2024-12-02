import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PubPrint extends Remote {

    void updateSubscriberCount(String topicId, int count) throws RemoteException;
}
