import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Directory implements DirectoryInterface {

    private List<String> allBrokers = new ArrayList<>();
    private Map<String, String> subToBroker = new HashMap<>();

    private int nextPub = 0;
    private int nextSub = 0;


    @Override
    public synchronized List<String> getBrokers() throws RemoteException {
        return new ArrayList<>(allBrokers);
    }


    @Override
    public synchronized void registerBroker(String brokerId, String brokerAddress) throws RemoteException {
        if (!allBrokers.contains(brokerAddress)) {
            allBrokers.add(brokerAddress);
            System.out.println("Registered broker: " + brokerId + " at " + brokerAddress);
            System.out.println("Total registered brokers: " + allBrokers.size());
        } else {
            System.out.println("Broker already registered: " + brokerAddress);
        }
    }


    @Override
    public synchronized String BrokerToSub(String subscriberName) throws RemoteException {
        // If the subscriber has already assigned a broker, the assigned broker is returned directly
        if (subToBroker.containsKey(subscriberName)) {
            return subToBroker.get(subscriberName);
        } else {
            if (allBrokers == null || allBrokers.isEmpty()) {
                throw new RemoteException("No brokers available.");
            }

            System.out.println("Available brokers: " + allBrokers);

            String assignedBroker = getOne(subscriberName);

            // Prints the debugging information, showing the broker address assigned to the subscriber
            System.out.println("Assigned broker for subscriber '" + subscriberName + "': " + assignedBroker);

            return assignedBroker;
        }
    }

    private String getOne(String subscriberName) {
        //Load balancing based on subscribers' hashCodes
        int brokerIndex = Math.abs(subscriberName.hashCode()) % allBrokers.size();
        String assignedBroker = allBrokers.get(brokerIndex);

        subToBroker.put(subscriberName, assignedBroker);
        return assignedBroker;
    }

    private void getNextPub(String publisherName, String assignedBroker) {
        nextPub = (nextPub + 1) % allBrokers.size();
        System.out.println("Assigned broker for publisher '" + publisherName + "': " + assignedBroker);
        System.out.println("After Round Robin, next index = " + nextPub);
    }

    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                System.out.println("Usage: java DirectoryService <port>");
                return;
            }

            int port = Integer.parseInt(args[0]);

            Directory directoryService = new Directory();

            DirectoryInterface stub = (DirectoryInterface) UnicastRemoteObject.exportObject(directoryService, 0);

            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind("DirectoryService", stub);

            System.out.println("DirectoryService ready on port " + port);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized String getBrokerForPublisher(String publisherName) throws RemoteException {
        if (allBrokers == null || allBrokers.isEmpty()) {
            throw new RemoteException("No brokers available.");
        }

        String assignedBroker = allBrokers.get(nextPub);

        System.out.println("Assigning broker to publisher '" + publisherName + "' : " + assignedBroker);

        getNextPub(publisherName, assignedBroker);

        return assignedBroker;
    }




}