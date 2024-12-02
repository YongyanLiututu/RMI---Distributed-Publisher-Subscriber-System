import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SubPrint extends Remote {
    void sendMessageDeleted(String topicId, String topicName) throws RemoteException;
    String getSubscriberName() throws RemoteException;
    void printMessage(String topicId, String topicName, String publisherName, String message) throws RemoteException;
}