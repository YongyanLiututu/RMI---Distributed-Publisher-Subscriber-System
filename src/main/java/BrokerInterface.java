import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

public interface BrokerInterface extends Remote {
    void registerPublisher(String publisherName) throws RemoteException;

    void notifySub(String topicId, String message) throws RemoteException;

    void connectSub(String subscriberName, SubPrint callback) throws RemoteException;

    boolean haveTopics(String topicId) throws RemoteException;

    void createNewTopic(String publisherName, String topicId, String topicName) throws RemoteException;


    List<String> listSubNumber(String subscriberName) throws RemoteException;

    List<String[]> listTopics() throws RemoteException;


    void connectSub(String subscriberName, SubPrint callback, Set<String> visitedBrokers) throws RemoteException;

    void createNewTopic(String publisherName, String topicId, String topicName, Set<String> visitedBrokers) throws RemoteException;

    void publishMessage(String publisherName, String topicId, String message) throws RemoteException;

    void publishMessage(String publisherName, String topicId, String message, Set<String> visitedBrokers) throws RemoteException;

    void deleteTopic(String publisherName, String topicId) throws RemoteException;

    void deleteTopic(String publisherName, String topicId, Set<String> visitedBrokers) throws RemoteException;

    void deleteTopic2(String publisherName, String topicId, Set<String> visitedBrokers) throws RemoteException;
    void unsubTopic(String subscriberName, String topicId) throws RemoteException;

    void syncTopics(String topicId, String topicName) throws RemoteException;

    void syncSubscribers(String topicId, List<SubPrint> subscribers) throws RemoteException;

    void unsubTopic(String subscriberName, String topicId, Set<String> visitedBrokers) throws RemoteException;

    void syncTopicInfo(String topicId, String topicName, List<SubPrint> subscribers) throws RemoteException;

    void subscribeTopic(String subscriberName, String topicId) throws RemoteException;


    void subscribeTopic(String subscriberName, String topicId, Set<String> visitedBrokers) throws RemoteException;
    void send_pub_info(String publisherId) throws RemoteException;
    void sendSubInfo(String subscriberId) throws RemoteException;

    void deleteTopicCrash(String publisherName, String topicId, Set<String> visitedBrokers) throws RemoteException;

    List<String> calCount(String publisherName) throws RemoteException;



    String getBrokerId() throws RemoteException;
}
