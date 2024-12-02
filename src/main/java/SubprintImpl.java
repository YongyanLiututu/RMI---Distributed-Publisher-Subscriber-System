import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SubprintImpl extends UnicastRemoteObject implements SubPrint {
    private String subscriberName;
    protected SubprintImpl() throws RemoteException {
        super();
    }

    @Override
    public String getSubscriberName() throws RemoteException {
        return subscriberName;
    }

    @Override
    public void sendMessageDeleted(String topicId, String topicName) throws RemoteException {
        System.out.println();
        System.out.println("The topic '" + topicName + "' (" + topicId + ") has been deleted.");
    }

    @Override
    public void printMessage(String topicId, String topicName, String publisherName, String message) throws RemoteException {
        System.out.println();
//        System.out.println("New message from publisher '" + publisherName + "' on topic '" + topicName + "' (" + topicId + "): " + message);

        String timestamp = new java.text.SimpleDateFormat("dd/MM HH:mm:ss").format(new java.util.Date());

        System.out.println();
        System.out.println( timestamp + " " + topicId + ":" + topicName + ": " + message);
    }

    private String formatMessage(String topicId, String topicName, String message) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM HH:mm:ss");
        String timestamp = dateFormat.format(new Date());
        return String.format("[%s] %s: %s", timestamp, topicName, message);
    }
}
