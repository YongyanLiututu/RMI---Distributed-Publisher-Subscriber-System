import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.util.Timer;
import java.util.TimerTask;
public class Publisher {
    private static int topTotal = 1;
    private Timer subTimerBroker;

    private void sendLiveTime(BrokerInterface broker, String publisherId) {
        subTimerBroker = new Timer(true);
        // Set as a daemon thread to ensure that the program is not prevented from terminating when the JVM exits

        TimerTask pubSendInfoTask = new TimerTask() {
            @Override
            public void run() {

                try {
                    broker.send_pub_info(publisherId);
//                    System.out.println("Heartbeat sent by publisher: " + publisherId);
                } catch (RemoteException e) {
                    System.out.println("no connection. " );
                }

            }
        };

        subTimerBroker.scheduleAtFixedRate(pubSendInfoTask, 0, 1000);
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java -jar publisher.jar <username> <directory_service_ip> <directory_service_port>");
            return;
        }

        String username = args[0];
        String directoryServiceIP = args[1];
        int directoryServicePort = Integer.parseInt(args[2]);

        try {
            Registry directoryRegistry = LocateRegistry.getRegistry(directoryServiceIP, directoryServicePort);
            DirectoryInterface directoryService = (DirectoryInterface) directoryRegistry.lookup("DirectoryService");
            // Obtain the broker assigned to the publisher through a round-robin mechanism

            String brokerAddress = directoryService.getBrokerForPublisher(username);

            if (brokerAddress == null || brokerAddress.isEmpty()) {
                System.out.println("No broker assigned.");
                return;
            }

            String[] brokerInfo = brokerAddress.split(":");
            String brokerIP = brokerInfo[0];


            int brokerPort = Integer.parseInt(brokerInfo[1]);

            BrokerInterface broker = null;
            try {
                Registry registry = LocateRegistry.getRegistry(brokerIP, brokerPort);
                broker = (BrokerInterface) registry.lookup("Broker");
                broker.registerPublisher(username);

                Publisher publisher = new Publisher();
                publisher.sendLiveTime(broker,username);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            } catch (NotBoundException e) {
                throw new RuntimeException(e);
            }
            Scanner scanner = new Scanner(System.in);
            while (true) {

                System.out.println("Please select a command: create, publish, show, delete.");
                String command = scanner.nextLine().trim();

                String[] parts = command.split(" ", 3);

                if (parts.length < 1) {
                    System.out.println("[error] Invalid command.");
                    continue;
                }

                switch (parts[0]) {
                    case "create":
                        if (checkCeateargs(parts)) break;

                        String topicId = parts[1].trim();
                        String topicName = parts[2].trim();

                        try {
                            if (broker.haveTopics(topicId)) {
                                System.out.println("[error] Topic ID already exists. Please choose a different ID.");
                                break;
                            }
                            broker.createNewTopic(username, topicId, topicName);
                            System.out.println("[success] Topic created: " + topicId);
                        } catch (Exception e) {
                            System.err.println("[error] " + e.getMessage());
                            System.out.println("Usage: create {topic_id} {topic_name}");
                            System.out.println("Example: create Weather123 WeatherUpdates");
                        }
                        break;


                    case "publish":
                        checkargs result = getCheckargs(parts);
                        if (result == null) break;
                        try {
                            broker.publishMessage(username, result.topicIdToPublish, result.inputMessage);
                            System.out.println("[success] Message published to topic: " + result.topicIdToPublish);
                        } catch (java.rmi.RemoteException e) {
                            System.out.println("[error] this publisher is not authorized to publish this topic");
                        } catch (Exception e) {
                            System.out.println("[error] this publisher is not authorized to publish to this topic");
                        }
                        break;

                    case "show":
                        try {
                            List<String> subscriberCount = broker.calCount(username);
                            if (subscriberCount.isEmpty()) {
                                System.out.println("[info] No subscribers");
                            } else {
                                System.out.println("Current Subscribers:");
                                for (String count : subscriberCount) {
                                    System.out.println(count);
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("[error] " + e.getMessage());
                        }
                        break;


                    case "delete":
                        System.out.print("Enter {topic_id}: ");
                        String topicIdToDelete = scanner.nextLine().trim();
                        try {
                            broker.deleteTopic(username, topicIdToDelete);
                            System.out.println("[success] Topic deleted: " + topicIdToDelete);
                        } catch (Exception e) {
                            System.out.println("[error] you are not authorized to delete this topic." );
                        }
                        break;

                    default:
                        System.out.println("[error] Invalid command.");
                        break;
                }
            }

        } catch (Exception e) {
//            e.printStackTrace();
            System.out.println("[error] no broker." );
        }
    }

    private static checkargs getCheckargs(String[] parts) {
        if (parts.length < 3) {
            System.out.println("[error] Invalid publish command. Usage: publish {topic_id} {message}");
            return null;
        }
        String topicIdToPublish = parts[1].trim();
        String inputMessage = parts[2].trim();
        checkargs result = new checkargs(topicIdToPublish, inputMessage);
        return result;
    }

    private static class checkargs {
        public final String topicIdToPublish;
        public final String inputMessage;

        public checkargs(String topicIdToPublish, String inputMessage) {
            this.topicIdToPublish = topicIdToPublish;
            this.inputMessage = inputMessage;
        }
    }

    private static boolean checkCeateargs(String[] parts) {
        // Make sure that the command enter starts with 'create' and that there are no illegal characters
        if (!parts[0].equals("create")) {
            System.out.println("[error] Invalid command.");
            System.out.println("Did you mean: create {topic_id} {topic_name}?");
            System.out.println("Example: create Weather123 WeatherUpdates");
            return true;
        }

        if (parts.length < 3) {
            System.out.println("[error] Invalid create command. Not enough arguments.");
            System.out.println("Usage: create {topic_id} {topic_name}");
            System.out.println("Example: create Weather123 WeatherUpdates");
            return true;
        }
        return false;
    }
}
