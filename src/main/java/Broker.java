import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Broker implements BrokerInterface {

    private Map<String, List<SubPrint>> topicSubs = new HashMap<>();
    private final Lock changeLock = new ReentrantLock();
    private Map<String, String> publishers = new HashMap<>();
    private Map<String, SubPrint> subCallbacks = new HashMap<>();
    private List<BrokerInterface> DirBrokers = new ArrayList<>();
    private String brokerID;
    private Map<String, PubPrint> publisherCallbacks = new HashMap<>();
    private Map<String, Topic> StringsTopics = new HashMap<>();

    private Set<String> actSub = new HashSet<>();
    // Maximum number of publishers
    private static final int maxPub = 5;
    // Keep track of publishers who are currently online
    private Set<String> acPub = new HashSet<>();
    // Heartbeat dependent: Stores the last heartbeat time of publishers and subscribers
    private Map<String, Long> pubCon = new ConcurrentHashMap<>();
    private Map<String, Long> subCon = new ConcurrentHashMap<>();
    private static final long outTime = 3000;

    // A recurring task to detect a heartbeat timeout
    private ScheduledExecutorService connection_checker = Executors.newScheduledThreadPool(1);
    private Map<String, List<String>> broker_published_only_topic = new HashMap<>();
    // Store the addresses of all proxies
    private List<String> allBrokerAddresses = new ArrayList<>();

    // A scheduled task thread to check if a new agent has joined
    private ScheduledExecutorService get_newly_connected_broker = Executors.newScheduledThreadPool(1);


    public Broker(String brokerId, DirectoryInterface directoryService) {
        this.brokerID = brokerId;
        // Check every once in a while for publishers and subscribers whose heartbeats out are timed out
        connection_checker.scheduleAtFixedRate(() -> {
            try {
                checkOutPubSub();
            } catch (RemoteException e) {
                System.err.println("[Error] during heartbeat check: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 2, TimeUnit.SECONDS);

        get_newly_connected_broker.scheduleAtFixedRate(() -> {
            try {
                get_newly_connected_NewBrokers(directoryService);
            } catch (RemoteException e) {
                System.err.println("[Error] while checking for new brokers: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 2, TimeUnit.SECONDS);


    }

    private void get_newly_connected_NewBrokers(DirectoryInterface directoryService) throws RemoteException {
        changeLock.lock();
        try {
            List<String> this_current_address = directoryService.getBrokers();

            // Find out the broker addresses that don't currently exist and connect to these new broker
            List<String> newBrokerAddresses = new ArrayList<>();
            for (String address : this_current_address) {
                if (!allBrokerAddresses.contains(address)) {
                    newBrokerAddresses.add(address);
                }
            }

            // Update the existing broker address list
            if (!newBrokerAddresses.isEmpty()) {
                allBrokerAddresses.addAll(newBrokerAddresses);
                findOtherBrokerList(newBrokerAddresses);
            }
        } catch (Exception e){
            System.out.println("no connection " );
        }
        finally {
            changeLock.unlock();
        }
    }

    @Override
    public void send_pub_info(String publisherId) throws RemoteException {
        pubCon.put(publisherId, System.currentTimeMillis());
    }

    @Override
    public void sendSubInfo(String subscriberId) throws RemoteException {
        subCon.put(subscriberId, System.currentTimeMillis());
    }

    private void checkOutPubSub() throws RemoteException {

        long TimeStart = System.currentTimeMillis();
        for (String publisherId : new HashSet<>(pubCon.keySet())) {
            if (TimeStart - pubCon.get(publisherId) > outTime) {
                System.out.println("publisherHeartbeats " +
                        "has expired!");
                pubOut(publisherId);
            }
        }

        for (String subscriberId : new HashSet<>(subCon.keySet())) {
            if (TimeStart - subCon.get(subscriberId) > outTime) {
                System.out.println("subscriberHeartbeats has expired!");
                subOut(subscriberId);
            }
        }
    }

    private void pubOut(String publisherId) throws RemoteException {
        changeLock.lock();
        try {


            // Get all the topics published by that publisher
            List<String> topicsToDelete = broker_published_only_topic.get(publisherId);
            if (topicsToDelete != null) {

                for (String topicId : topicsToDelete) {
                    System.out.println("deleting topic");
                    deleteTopicCrash(publisherId, topicId);
                }
            }

            // Removes information about the publisher from the data structure
            pubCon.remove(publisherId);
            publishers.remove(publisherId);
            publisherCallbacks.remove(publisherId);
            broker_published_only_topic.remove(publisherId);

            System.out.println("Publisher " + publisherId + " has been removed due to heartbeat timeout.");
        } finally {
            changeLock.unlock();
        }
    }


    private void subOut(String subscriberId) throws RemoteException {
        changeLock.lock();
        try {
            // Remove heartbeats and callbacks
            subCon.remove(subscriberId);
            subCallbacks.remove(subscriberId);

            List<String> topicsToUnsubscribe = subscriberTopics.get(subscriberId);
            if (topicsToUnsubscribe != null) {
                for (String topicId : topicsToUnsubscribe) {
                    removesubCrashed(subscriberId, topicId);
                    Topic removedTopic = StringsTopics.remove(topicId);

                    if (removedTopic != null) {
                        System.out.println("Successfully removed topic for subscriber: " + subscriberId);
                    } else {
                        System.out.println("No topic found for subscriber: " + subscriberId);
                    }
                }
                subscriberTopics.remove(subscriberId);

            }

            System.out.println("Subscriber " + subscriberId + " has been removed due to heartbeat timeout.");
        } finally {
            changeLock.unlock();
        }
    }



    public class Topic {
        private String name;
        private String publisherName;

        public Topic(String name, String publisherName) {
            this.name = name;
            this.publisherName = publisherName;
        }

        public String getName() {
            return name;
        }

        public String getPublisherName() {
            return publisherName;
        }
    }

    private Map<String, List<String>> subscriberTopics = new HashMap<>();


    @Override
    public void registerPublisher(String publisherName) throws RemoteException {
        changeLock.lock();
        try {
            if (acPub.size() >= maxPub) {
                throw new RemoteException("Maximum number of publishers reached.");
            }

            if (acPub.contains(publisherName)) {
                throw new RemoteException("Publisher already registered.");
            }

            acPub.add(publisherName);
            publishers.put(publisherName, publisherName);
            System.out.println("Publisher " + publisherName + " registered to broker " + brokerID);
        } finally {
            changeLock.unlock();
        }
    }

    @Override
    public void connectSub(String subscriberName, SubPrint callback) throws RemoteException {
        connectSub(subscriberName, callback, new HashSet<>());

    }


    private Set<SubPrint> registeredCallbacks = new HashSet<>();

    @Override
    public void connectSub(String subscriberName, SubPrint callback, Set<String> visitedBrokers) throws RemoteException {

        if (!registeredCallbacks.contains(callback)) {
            subCallbacks.put(subscriberName, callback);
            registeredCallbacks.add(callback);
            System.out.println("Subscriber " + subscriberName + " registered successfully.");
        } else {
            System.out.println("Callback is already registered. Skipping addition.");
        }

        visitedBrokers.add(brokerID);

        // Forward the message to another broker
        for (BrokerInterface broker : DirBrokers) {
            try {
                if (!visitedBrokers.contains(broker.getBrokerId())) {
                    broker.connectSub(subscriberName, callback, visitedBrokers);
                    System.out.println("");
                    System.out.println("notifying boker " + broker.getBrokerId()+ " a new subscriber " + subscriberName);
                }
            } catch (RemoteException e) {
                System.err.println("Failed to notify broker here: " );
            }
        }

    }

    private static void notifyOthers(String topicId, List<SubPrint> subscriberCallbacks, String topicName) throws RemoteException {
        if (subscriberCallbacks != null && !subscriberCallbacks.isEmpty()) {
            for (SubPrint callback : subscriberCallbacks) {
                callback.sendMessageDeleted(topicId, topicName);
            }
        } else {
            System.out.println("No subscribers to notify for topic: " + topicId);
        }
    }

    @Override
    public void createNewTopic(String publisherName, String topicId, String topicName) throws RemoteException {
        createNewTopic(publisherName, topicId, topicName, new HashSet<>());
    }

    @Override
    public List<String> listSubNumber(String subscriberName) throws RemoteException {
        //If a subscriber exists, a list of topics to which they subscribed is returned
        List<String> subscriptions = subscriberTopics.getOrDefault(subscriberName, new ArrayList<>());
        List<String> result = new ArrayList<>();

        createTOP(subscriptions, result);

        return result;
    }


    public List<String> getPublisherTopics(String publisherName) {
        return broker_published_only_topic.getOrDefault(publisherName, new ArrayList<>());
    }


    @Override
    public void createNewTopic(String publisherName, String topicId, String topicName, Set<String> visitedBrokers) throws
            RemoteException {
        refreshTops(publisherName, topicId, topicName);

        visitedBrokers.add(brokerID);

        for (BrokerInterface broker : DirBrokers) {
            try {
                if (!visitedBrokers.contains(broker.getBrokerId())) {
                    System.out.println("Notifying other broker about new topic: " + topicId);
                    broker.createNewTopic(publisherName, topicId, topicName, visitedBrokers);
                }
            } catch (RemoteException e) {
                System.err.println("Failed to notify broker: " + e.getMessage());
            }
        }
        System.out.println("Topic created successfully: " + topicId);
    }

    private void refreshTops(String publisherName, String topicId, String topicName) {
        System.out.println("Creating topic: " + topicId + " " + topicName);

        StringsTopics.put(topicId, new Topic(topicName, publisherName));
        topicSubs.put(topicId, new ArrayList<>());
        broker_published_only_topic.putIfAbsent(publisherName, new ArrayList<>());
        broker_published_only_topic.get(publisherName).add(topicId);
    }

    private String getPublisherName(String topicId) {
        Topic topic = StringsTopics.get(topicId);
        if (topic != null) {
            return topic.getPublisherName();
        }
        return "Unknown Publisher";
    }


    @Override
    public void publishMessage(String publisherName, String topicId, String message) throws RemoteException {
        publishMessage(publisherName, topicId, message, new HashSet<>());
    }


    private void createTOP(List<String> subscriptions, List<String> result) {
        for (String topicId : subscriptions) {

            Topic topic = StringsTopics.get(topicId);
            if (topic != null) {
                result.add(topicId + " " + topic.getName() + " " + topic.getPublisherName());
            } else {
                System.out.println("Topic not found for ID: " + topicId);
            }
        }
    }

    @Override
    public void publishMessage(String publisherName, String topicId, String message, Set<String> visitedBrokers) throws RemoteException {

        if (!broker_published_only_topic.containsKey(publisherName) || !broker_published_only_topic.get(publisherName).contains(topicId)) {
            throw new RemoteException("Publisher " + publisherName + " is not authorized to publish to topic " + topicId);
        }
        changeLock.lock();
        try {
            CheckTopics2(topicId);

            publishOther(publisherName, topicId, message, visitedBrokers);


        } finally {
            changeLock.unlock();
        }
    }


    private void CheckTopics2(String topicId) throws RemoteException {
        if (!StringsTopics.containsKey(topicId)) {
            throw new RemoteException("Topic not found: " + topicId);
        }
    }

    private List<SubPrint> getSubscribers(String topicId) {
        // Send messages to local subscribers
        List<SubPrint> subscribers = topicSubs.get(topicId);
        Set<SubPrint> uniqueSubscribers = new HashSet<>(subscribers);
        subscribers = new ArrayList<>(uniqueSubscribers);
        return subscribers;
    }

    private void checkSub1(String publisherName, String topicId, String message, List<SubPrint> subscribers) throws RemoteException {
        if (subscribers == null || subscribers.isEmpty()) {
            System.out.println("No subscribers for topic: " + topicId);
        } else {
            for (SubPrint callback : subscribers) {
                if (callback != null) {
                    Topic topic = StringsTopics.get(topicId);
                    if (topic != null) {
                        callback.printMessage(topicId, topic.getName(), publisherName, message);
                    }
                }
            }
        }
    }


    @Override
    public void syncSubscribers(String topicId, List<SubPrint> subscribers) throws RemoteException {
        topicSubs.put(topicId, subscribers);
    }

    @Override
    public void syncTopics(String topicId, String topicName) throws RemoteException {
        if (!StringsTopics.containsKey(topicId)) {
            StringsTopics.put(topicId, new Topic(topicName, "Unknown Publisher"));
        }
    }


    public void removesubCrashed(String subscriberName, String topicId) throws RemoteException {
        // Call an existing three-parameter unsubscribeTopic method and pass in an empty Set to maintain compatibility
        removesubCrashed(subscriberName, topicId, new HashSet<>());
    }

    public void removesubCrashed(String subscriberName, String topicId, Set<String> visitedBrokers) throws
            RemoteException {
        changeLock.lock();
        try {

            doRemove(subscriberName, topicId);

            // Mark visited proxy topicSubs
            notifyOthers(subscriberName, topicId, visitedBrokers);

            // Update the publisher's subscriber count
            updateCount(topicId);
        } finally {
            changeLock.unlock();
        }
    }


    @Override
    public void unsubTopic(String subscriberName, String topicId) throws RemoteException {
        // Call an existing three-parameter unsubscribeTopic method and pass in an empty Set to maintain compatibility
        unsubTopic(subscriberName, topicId, new HashSet<>());
    }

    private void subTopics(String subscriberName, String topicId) {
        SubPrint callback = subCallbacks.get(subscriberName);
        if (callback != null) {
            topicSubs.computeIfAbsent(topicId, k -> new ArrayList<>()).add(callback);

            // Print a list of subscribers
            System.out.println("Topic '" + topicId + "' current subscribers after addition:");
            List<SubPrint> currentSubscribers = topicSubs.get(topicId);
            for (SubPrint sub : currentSubscribers) {
                try {
                    System.out.println(" - Subscriber ID: " + sub.getSubscriberName());
                } catch (RemoteException e) {
                    System.err.println("Error while accessing subscriber ID: " + e.getMessage());
                }
            }
        } else {
            // If the callback does not exist, an error message is printed
            System.err.println("Warning: Subscriber callback for '" + subscriberName + "' is null. ");
        }
    }

    @Override
    public void unsubTopic(String subscriberName, String topicId, Set<String> visitedBrokers) throws
            RemoteException {
        changeLock.lock();
        try {
            doRemove(subscriberName, topicId);
            notifyOthers(subscriberName, topicId, visitedBrokers);
            updateCount(topicId);
        } finally {
            changeLock.unlock();
        }
    }

    private void doRemove(String subscriberName, String topicId) throws RemoteException {
        System.out.println("test");

        if (!topicSubs.containsKey(topicId)) {
            throw new RemoteException("Topic '" + topicId + "' does not exist. No action taken for unsubscribing subscriber '" + subscriberName + "'.");

        }

        if (!StringsTopics.containsKey(topicId)) {
            throw new RemoteException("Topic not found.");
        }

        removeTopSubs(subscriberName, topicId);
        removeThisTop(subscriberName, topicId);
    }

    private void removeTopSubs(String subscriberName, String topicId) {

        List<SubPrint> subscribers = topicSubs.get(topicId);
        if (subscribers != null) {
            SubPrint callback = subCallbacks.get(subscriberName);
            if (callback != null) {
                subscribers.remove(callback);
                System.out.println("Removed subscriber callback for topic: " + topicId);
            }
        }
    }


    private void notifyOthers(String subscriberName, String topicId, Set<String> visitedBrokers) {
        visitedBrokers.add(brokerID);

        for (BrokerInterface broker : DirBrokers) {
            try {
                if (!visitedBrokers.contains(broker.getBrokerId())) {
                    broker.unsubTopic(subscriberName, topicId, visitedBrokers);
                }
            } catch (RemoteException e) {
                System.err.println("Failed to notify broker: " + e.getMessage());
            }
        }
    }

    private void publishOther(String publisherName, String topicId, String message, Set<String> visitedBrokers) throws RemoteException {
        List<SubPrint> subscribers = getSubscribers(topicId);
        checkSub1(publisherName, topicId, message, subscribers);
        // Prevent round-robin forwarding - Immediately add agents to the visited list here
        visitedBrokers.add(brokerID);
        System.out.println("Broker '" + brokerID + "' added to visited brokers.");
    }

    @Override
    public void subscribeTopic(String subscriberName, String topicId, Set<String> visitedBrokers) throws
            RemoteException {
        System.out.println("subscribing topic ");
        CheckTopics2(topicId);

        List<String> subscriptions = subscriberTopics.computeIfAbsent(subscriberName, k -> new ArrayList<>());

        // Prevent duplicate subscriptions
        if (subscriptions.contains(topicId)) {
            throw new RemoteException("Already subscribed to this topic: " + topicId);
        }

        subscriptions.add(topicId);
        subTopics(subscriberName, topicId);
        System.out.println("subscribing topic and notifying other broker!");
        visitedBrokers.add(brokerID);

        // Notify other agents to synchronize subscriber information
        for (BrokerInterface broker : DirBrokers) {
            try {
                if (!visitedBrokers.contains(broker.getBrokerId())) {
                    broker.subscribeTopic(subscriberName, topicId, visitedBrokers);
                }
            } catch (RemoteException e) {
                System.err.println("Failed to notify broker: " + e.getMessage());
            }
        }
    }


    private final ReentrantReadWriteLock totalLocker = new ReentrantReadWriteLock();
    private final Lock RLocker = totalLocker.readLock();
    private final Lock wLocker = totalLocker.writeLock();

    private void removeThisTop(String subscriberName, String topicId) {
        // Removed from the subscriber's list of topics
        List<String> topicsForSubscriber = subscriberTopics.get(subscriberName);
        if (topicsForSubscriber != null) {
            topicsForSubscriber.remove(topicId);
            System.out.println("Removed topic from subscriber's list: " + topicId);
        }
    }

    private void updateCount(String topicId) throws RemoteException {
        RLocker.lock();
        try {
            List<SubPrint> currentSubscribers = topicSubs.get(topicId);
            int subscriberCount = (currentSubscribers != null) ? currentSubscribers.size() : 0;

            String publisherName = getPublisherName(topicId);
            if (publisherName != null && !publisherName.equals("Unknown Publisher")) {
                // Get the publisher's callback object and notify it that the number of subscribers is updated

                PubPrint publisherCallback = publisherCallbacks.get(publisherName);
                if (publisherCallback != null) {
                    publisherCallback.updateSubscriberCount(topicId, subscriberCount);
                    System.out.println("Updated subscriber count for publisher: " + publisherName);
                } else {
                    System.out.println("No callback found for publisher: " + publisherName);
                }
            } else {
                System.out.println("No publisher found for topic: " + topicId);
            }
        } finally {
            RLocker.unlock();
        }
    }

    private void getCallbacks(String topicId) throws RemoteException {
        List<SubPrint> subscribers = topicSubs.remove(topicId);
        if (subscribers != null) {
            for (SubPrint callback : subscribers) {
                List<String> topicsForSubscriber = subscriberTopics.get(callback.getSubscriberName());
                if (topicsForSubscriber != null) {
                    topicsForSubscriber.remove(topicId);
                }
            }
        }
    }

    @Override
    public void syncTopicInfo(String topicId, String topicName, List<SubPrint> subscribers) throws
            RemoteException {
        StringsTopics.put(topicId, new Topic(topicName, getPublisherName(topicId)));
        topicSubs.put(topicId, subscribers);
    }

    public void deleteTopicCrash(String publisherName, String topicId) throws RemoteException {
        deleteTopicCrash(publisherName, topicId, new HashSet<>());
    }

    @Override
    public void deleteTopicCrash(String publisherName, String topicId, Set<String> visitedBrokers) throws RemoteException {
        System.out.println("deleteTopicCrash called with parameters:");
        System.out.println("Publisher Name: " + publisherName);
        System.out.println("Topic ID: " + topicId);
        System.out.println("Visited Brokers: " + visitedBrokers);
        System.out.println("notifying subscriber");
        notifySub(topicId, "The topic '" + topicId + "' you subscribed to has been deleted.");
        StringsTopics.remove(topicId);
        System.out.println("Visited Brokers2: " + visitedBrokers);
        getCallbacks(topicId);

    }


    public void deleteTopic(String publisherName, String topicId) throws RemoteException {
        deleteTopic(publisherName, topicId, new HashSet<>());
    }

    @Override
    public void deleteTopic2(String publisherName, String topicId, Set<String> visitedBrokers) throws RemoteException {
//        System.out.println("222222222");
        checkDelete(publisherName, topicId);
//        notifySub(topicId, "The topic '" + topicId + "' you subscribed to has been deleted.");

        StringsTopics.remove(topicId);

        getCallbacks(topicId);

        visitedBrokers.add(brokerID);

        for (BrokerInterface broker : DirBrokers) {
            try {
                if (!visitedBrokers.contains(broker.getBrokerId())) {
                    System.out.println("Notifying other broker about deleted topic: " + topicId);
                    broker.deleteTopic2(publisherName, topicId, visitedBrokers);
                }
            } catch (RemoteException e) {
                System.err.println("Failed to notify broker: " + e.getMessage());
            }
        }


    }



    @Override
    public void deleteTopic(String publisherName, String topicId, Set<String> visitedBrokers) throws RemoteException {
        System.out.println("111111111111");
        checkDelete(publisherName, topicId);

        // Notify subscribers that the topic has been deleted
        notifySub(topicId, "The topic '" + topicId + "' you subscribed to has been deleted.");

        // Delete topic and subscriber information
        StringsTopics.remove(topicId);

        getCallbacks(topicId);

        visitedBrokers.add(brokerID);

        for (BrokerInterface broker : DirBrokers) {
            try {
                if (!visitedBrokers.contains(broker.getBrokerId())) {
                    System.out.println("Notifying other broker about deleted topic: " + topicId);
                    broker.deleteTopic2(publisherName, topicId, visitedBrokers);
                }
            } catch (RemoteException e) {
                System.err.println("Failed to notify broker: " + e.getMessage());
            }
        }


    }


    private void checkDelete(String publisherName, String topicId) throws RemoteException {
        if (!broker_published_only_topic.containsKey(publisherName) || !broker_published_only_topic.get(publisherName).contains(topicId)) {
            throw new RemoteException("Publisher " + publisherName + " is not authorized to publish to topic " + topicId);
        }

        if (!StringsTopics.containsKey(topicId)) {
            throw new RemoteException("Topic not found.");
        }
    }


    @Override
    public List<String[]> listTopics() throws RemoteException {
        List<String[]> topicList = new ArrayList<>();
        for (Map.Entry<String, Topic> entry : StringsTopics.entrySet()) {
            String topicId = entry.getKey();
            Topic topic = entry.getValue();
            String topicName = topic.getName();
            String publisherName = topic.getPublisherName();
            topicList.add(new String[]{topicId, topicName, publisherName});
        }
        return topicList;
    }

    @Override
    public void subscribeTopic(String subscriberName, String topicId) throws RemoteException {
        subscribeTopic(subscriberName, topicId, new HashSet<>());
    }


    @Override
    public boolean haveTopics(String topicId) throws RemoteException {
        return StringsTopics.containsKey(topicId);
    }


    @Override
    public List<String> calCount(String publisherName) throws RemoteException {

        List<String> subscriberCountList = new ArrayList<>();

        checkPublishTopic(publisherName, subscriberCountList);

        return subscriberCountList;
    }

    private void checkPublishTopic(String publisherName, List<String> subscriberCountList) {
        // Iterate through all topics to check if they are relevant to that publisher
        for (Map.Entry<String, Topic> entry : StringsTopics.entrySet()) {
            String topicId = entry.getKey();
            Topic topic = entry.getValue();
            if (topic.getPublisherName().equals(publisherName)) {
                List<SubPrint> currentSubscribers = topicSubs.get(topicId);
                int subscriberCount = (currentSubscribers == null) ? 0 : currentSubscribers.size();
                subscriberCountList.add(topicId + " " + topic.getName() + " " + subscriberCount);
            }
        }
    }


    @Override
    public void notifySub(String topicId, String message) throws RemoteException {
        List<SubPrint> subscriberCallbacks = topicSubs.get(topicId);
        String namedeleted = StringsTopics.get(topicId).getName();
        if (subscriberCallbacks != null && !subscriberCallbacks.isEmpty()) {
            System.out.println("Subscribers for topic '" + topicId + "':");
            for (SubPrint callback : subscriberCallbacks) {
                callback.sendMessageDeleted(topicId, namedeleted);
                                    System.out.println("- Subscriber Name: " + callback.getSubscriberName());
            }
        } else {
            System.out.println("No subscribers found for topic '" + topicId + "'.");
        }

        String topicName = StringsTopics.get(topicId).getName();


//        notifyOthers(topicId, subscriberCallbacks, topicName);


    }


    private void sendNotification(String subscriber, String message) {
        System.out.println("Notification sent to subscriber: " + subscriber + " - " + message);
    }


    private void findOtherBrokerList(List<String> brokerAddresses) {
        for (String address : brokerAddresses) {
            String[] parts = address.split(":");
            String ip = parts[0];
            int port = Integer.parseInt(parts[1]);

            try {
                Registry registry = LocateRegistry.getRegistry(ip, port);
                BrokerInterface broker = (BrokerInterface) registry.lookup("Broker");


                if (!DirBrokers.contains(broker)) {
                    if (!broker.getBrokerId().equals(this.brokerID)) {
                        DirBrokers.add(broker);
                        System.out.println();

                    }
                }

            } catch (Exception e) {
                System.err.println("Failed to connect to broker at " + address + ": " + e.getMessage());
            }
        }
    }


    @Override
    public String getBrokerId() throws RemoteException {
        return brokerID;
    }

    public static void main(String[] args) {
        try {
            if (args.length != 4) {
                System.out.println("Usage: java Broker <port> <directory_service_ip> <directory_service_port> <broker_id>");
                return;
            }

            int port = Integer.parseInt(args[0]);
            String directoryServiceIp = args[1];
            int directoryServicePort = Integer.parseInt(args[2]);
            String brokerId = args[3];

            DirectoryInterface directoryService = null;
            try {
                Registry directoryRegistry = LocateRegistry.getRegistry(directoryServiceIp, directoryServicePort);
                directoryService = (DirectoryInterface) directoryRegistry.lookup("DirectoryService");
            } catch (RemoteException e) {
                System.out.println("no directory service, start first.");
                System.exit(1);
            } catch (NotBoundException e) {
                System.out.println("no directory service, start first ");
                System.exit(1);
            }


            Broker broker = new Broker(brokerId, directoryService);
            BrokerInterface stub = (BrokerInterface) UnicastRemoteObject.exportObject(broker, 0);

            try {
                Registry registry = LocateRegistry.createRegistry(port);
                if (registry == null) {
                    System.out.println("Failed to create registry on port " + port + ". Registry is null.");
                } else {
                    registry.bind("Broker", stub);
                    System.out.println("Broker ready on port " + port);
                }
            } catch (Exception e) {
                System.out.println("no directory service, start first ");

            }


            String brokerAddress = "127.0.0.1:" + port;
            directoryService.registerBroker(brokerId, brokerAddress);

            List<String> brokerAddresses = directoryService.getBrokers();
            broker.findOtherBrokerList(brokerAddresses);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
