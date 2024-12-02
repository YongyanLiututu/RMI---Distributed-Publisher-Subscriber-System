import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class Subscriber {

    private Timer timerToMyBroker;
    private void newThreadInfo(BrokerInterface broker, String subscriberName) {
        timerToMyBroker = new Timer(true);

        TimerTask sendInfoTask = new TimerTask() {
            @Override
            public void run() {


                try {
                    broker.sendSubInfo(subscriberName);
//                    System.out.println("Heartbeat sent by publisher: " + publisherId);
                } catch (RemoteException e) {
                    System.out.println("no connection " );
                }

            }
        };

        timerToMyBroker.scheduleAtFixedRate(sendInfoTask, 0, 1000);
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Invalid number of arguments.");
            System.out.println("Usage: java Subscriber <subscriberName> <directory_service_ip> <directory_service_port>");
            return;
        }

        try {
            String subscriberName = args[0];
            String directoryServiceIP = args[1];
            int directoryServicePort = Integer.parseInt(args[2]);

            Registry registry = LocateRegistry.getRegistry(directoryServiceIP, directoryServicePort);
            DirectoryInterface directoryService = (DirectoryInterface) registry.lookup("DirectoryService");

            String brokerAddress = directoryService.BrokerToSub(subscriberName);

            if (brokerAddress == null || brokerAddress.isEmpty()) {
                System.out.println("Could not retrieve broker address for subscriber: " + subscriberName);
                return;
            }

            String[] parts = brokerAddress.split(":");
            if (parts.length != 2) {
                System.out.println("Invalid broker address format.");
                return;
            }

            String brokerIP = parts[0];
            int brokerPort;
            try {
                brokerPort = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid broker port number: " + parts[1]);
                return;
            }

            Registry brokerRegistry = LocateRegistry.getRegistry(brokerIP, brokerPort);
            BrokerInterface broker = (BrokerInterface) brokerRegistry.lookup("Broker");

            SubprintImpl callback = new SubprintImpl();
            broker.connectSub(subscriberName, callback);

            Subscriber subscriber = new Subscriber();
            subscriber.newThreadInfo(broker,subscriberName);

            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("Please select a command:");
                System.out.println("1. list - List all available topics.");
                System.out.println("2. sub - Subscribe to a topic.");
                System.out.println("3. current - Show current subscriptions.");
                System.out.println("4. unsub - Unsubscribe from a topic.");
                System.out.print("Please choose a number: ");
                String command = scanner.nextLine();
                String[] commandParts = command.split(" ");

                switch (commandParts[0]) {
                    case "1":
                        List<String[]> topics = broker.listTopics();
                        if (topics.isEmpty()) {
                            System.out.println("No topics available.");
                        } else {
                            System.out.println("Available Topics:");
                            for (String[] topic : topics) {
                                System.out.printf("ID: %s | Name: %s | Publisher: %s%n", topic[0], topic[1], topic[2]);
                            }
                        }
                        break;

                    case "2":

                        System.out.print("Enter topic ID to subscribe: ");
                        String topicId = scanner.nextLine().trim();
                        try {
                            broker.subscribeTopic(subscriberName, topicId);
                            System.out.println("Successfully subscribed to topic: " + topicId);
                        } catch (RemoteException e) {
                            System.out.println("Failed to subscribe: can not subscribe this topics!" );
                        }
                        break;

                    case "3":
                        List<String> currentSubscriptions = broker.listSubNumber(subscriberName);
                        if (currentSubscriptions.isEmpty()) {
                            System.out.println("No current subscriptions.");
                        } else {
                            System.out.println("Current Subscriptions:");
                            for (String subscription : currentSubscriptions) {
                                System.out.println("Debug: Current Subscription Raw Data: " + subscription); // 调试输出
                                String[] subscriptionParts = subscription.split(" ");

                                checkLen(subscription, subscriptionParts);
                            }
                        }
                        break;

                    case "4":

                        System.out.print("Enter topic ID to unsubscribe: ");
                        String topicToUnsub = scanner.nextLine().trim();
                        try {
                            broker.unsubTopic(subscriberName, topicToUnsub);
                            System.out.println("Unsubscribed from topic: " + topicToUnsub);
                        } catch (RemoteException e) {
                            System.out.println("[Error] Failed to unsubscribe from topic: you do not sub this topic" );
                        }
                        break;

                    default:
                        System.out.println("Invalid command. Please enter a number between 1 and 4.");
                        break;

                }
            }

        } catch (NumberFormatException e) {
            System.out.println("Invalid port number format.");
        } catch (RemoteException e) {
            System.out.println("Failed to communicate with the remote service: no brokers" );
        } catch (NotBoundException e) {
            System.out.println("Service not found: " + e.getMessage());
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Invalid input. Please provide enough arguments for the command.");
        } catch (Exception e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
        }
    }

    private static void checkLen(String subscription, String[] subscriptionParts) {
        //Check the formatting
        if (subscriptionParts.length >= 3) {
            String currentTopicId = subscriptionParts[0];
            String topicName = subscriptionParts[1];
            String publisherName = subscriptionParts[2];
            System.out.printf("ID: %s | Name: %s | Publisher: %s%n", currentTopicId, topicName, publisherName);
        } else {
            System.out.println("Invalid subscription format for: " + subscription);
        }
    }
}
