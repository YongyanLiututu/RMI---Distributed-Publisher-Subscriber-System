import java.rmi.RemoteException;

public class PubPrintImpl implements PubPrint {
    @Override
    public void updateSubscriberCount(String topicId, int count) throws RemoteException {
        System.out.println("Subscriber count for topic " + topicId + " is now: " + count);
    }
}
