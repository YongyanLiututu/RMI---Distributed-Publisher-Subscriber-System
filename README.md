# Distributed Publisher-Subscriber System

This project is an implementation of a **Distributed Publisher-Subscriber (Pub-Sub) System**. The system is designed to demonstrate real-time message distribution in a decentralized architecture using **RMI**. It includes three major components: **Brokers**, **Publishers**, and **Subscribers**, all communicating seamlessly to manage topics and deliver messages.

---

## Features

1. **Brokers**:
    - Three interconnected broker nodes handle topic management, subscription, and message distribution.
    - Ensure seamless communication and synchronization across the network.
    - Efficient routing of messages between publishers and subscribers.

2. **Publishers**:
    - Create new topics and publish messages.
    - View subscriber counts for each topic.
    - Delete topics, with automated unsubscription for all associated subscribers.

3. **Subscribers**:
    - Subscribe to multiple topics and receive real-time messages.
    - List all available topics and view active subscriptions.
    - Unsubscribe from topics with notifications.

4. **Communication**:
    - Reliable message delivery through inter-broker synchronization.
    - Real-time notifications for subscription changes and deleted topics.

5. **Fault Tolerance**:
    - Handles crashes of publishers and subscribers gracefully.
    - Automatic topic and subscription management on failures.

---

## Design Challenges

Developing this system required addressing several technical challenges:

1. **Concurrency**:
    - Brokers handle multiple simultaneous connections from publishers and subscribers.
    - Proper synchronization of shared resources to prevent race conditions.

2. **Network Communication**:
    - Designing a robust protocol for inter-broker communication to ensure message consistency.
    - Handling message routing dynamically across multiple brokers.

3. **Decentralization**:
    - Ensuring all components operate independently yet cohesively in a distributed environment.

4. **Real-Time Operations**:
    - Ensuring minimal latency in delivering messages to subscribers.

---

## Implementation Details

### Technologies Used
### Technologies Used

- **Java Socket Programming / RMI**:  
  Implemented efficient distributed communication across nodes.
    - **Sample Code**:
      ```java
      ServerSocket serverSocket = new ServerSocket(port);
      while (true) {
          Socket clientSocket = serverSocket.accept();
          new Thread(new ClientHandler(clientSocket)).start();
      }
      ```

- **Multithreading and Synchronization**:  
  Supported concurrent client connections using thread pools and synchronization mechanisms to protect shared resources.
    - **Sample Code**:
      ```java
      ExecutorService threadPool = Executors.newFixedThreadPool(10);
      threadPool.submit(() -> handleClient(socket));
      ```

- **Custom Communication Protocol**:  
  Designed a simple protocol format (e.g., Message Type + Topic ID + Content) for parsing transmitted messages.
    - **Sample Protocol Structure**:
      ```
      [PUBLISH] [topic_id] [message_content]
      ```
    - **Sample Code**:
      ```java
      String message = "PUBLISH topic1 Hello World";
      String[] parts = message.split(" ");
      ```

- **Optimized Data Structures**:  
  Used `ConcurrentHashMap` to manage topics and subscribers, ensuring thread-safe and efficient read/write operations.
    - **Sample Code**:
      ```java
      ConcurrentHashMap<String, List<String>> topicSubscribers = new ConcurrentHashMap<>();
      topicSubscribers.putIfAbsent("topic1", new ArrayList<>());
      ```

- **Failure Detection and Recovery**:  
  Implemented heartbeat detection to promptly remove inactive clients and clean up associated resources.
    - **Sample Code**:
      ```java
      ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
      scheduler.scheduleAtFixedRate(() -> checkHeartbeats(), 0, 5, TimeUnit.SECONDS);
      ```

- **Modular Architecture Design**:  
  Separated the functionalities of brokers, publishers, and subscribers to facilitate independent scalability and feature enhancement.
    - **Sample Module Design**:
      ```
      Broker.java
      Publisher.java
      Subscriber.java
      ```

- **Console-Based User Interface**:  
  Provided a simple console interaction for common operations.
    - **Sample Code**:
      ```java
      Scanner scanner = new Scanner(System.in);
      System.out.println("Enter command: ");
      String command = scanner.nextLine();
      ```

- **Real-Time Message Synchronization**:  
  Used non-blocking I/O for fast synchronization of messages across brokers.
    - **Sample Code**:
      ```java
      Selector selector = Selector.open();
      channel.register(selector, SelectionKey.OP_READ);
      ```

- **Network Fault Tolerance**:  
  Automatically retried failed connections and tracked undelivered messages for later processing.
    - **Sample Code**:
      ```java
      for (int i = 0; i < MAX_RETRIES; i++) {
          try {
              sendMessage(socket, message);
              break;
          } catch (IOException e) {
              Thread.sleep(RETRY_INTERVAL);
          }
      }
      ```

- **Logging and Monitoring**:  
  Used logging tools to record key events and errors for debugging and monitoring purposes.
    - **Sample Code**:
      ```java
      Logger logger = LoggerFactory.getLogger(Broker.class);
      logger.info("Message received: {}", message);
      ```


### Deployment
- Each broker is launched as a separate process with a predefined port.
- Publishers and subscribers connect dynamically to brokers via command-line arguments.

### Commands
#### Publisher Commands
- `create {topic_id} {topic_name}`: Create a new topic.
- `publish {topic_id} {message}`: Publish a message to a topic.
- `show {topic_id}`: Display subscriber count for a topic.
- `delete {topic_id}`: Delete a topic and notify subscribers.

#### Subscriber Commands
- `list {all}`: List all available topics.
- `sub {topic_id}`: Subscribe to a topic.
- `current`: View current subscriptions.
- `unsub {topic_id}`: Unsubscribe from a topic.

---

## Simplicity in Use

While the backend involves complex distributed systems logic, the user interface is straightforward:
- **Publishers** and **Subscribers** interact through simple console commands.
- The system ensures real-time feedback and status updates for all operations.

---

## Getting Started

### Prerequisites
- **Java JDK 11+**
- A basic understanding of console-based interactions.

### Running the System
1. Start the brokers:
   ```bash
   java -jar broker.jar <port> -b [broker_ip1:port1 broker_ip2:port2]
    ```

## Testing & Validation

- **Concurrent Operations**:  
  The system has been tested for concurrent operations, ensuring real-time message delivery to all subscribers.

- **Failure Scenarios**:  
  Various failure scenarios, such as publisher and subscriber crashes, were simulated to validate fault tolerance and ensure automatic topic and subscription management.

- **Inter-Broker Communication**:  
  Inter-broker communication ensures that subscribers receive messages regardless of their connected broker, maintaining message consistency across the network.

---

## Challenges Addressed

- **Distributed Synchronization**:  
  Ensuring that distributed brokers remain synchronized without message loss or duplication.

- **Concurrent Client Management**:  
  Managing multiple simultaneous connections from publishers and subscribers effectively.

- **Simplification**:  
  Simplifying a complex distributed system architecture for ease of implementation and usability.

---

## Future Enhancements

- **Dynamic Broker Discovery**:  
  Implement a directory service to enable brokers, publishers, and subscribers to dynamically discover active brokers and manage connections more flexibly.

- **Advanced Fault Tolerance**:  
  Enhance the system to handle broker crashes with automatic recovery mechanisms and message rerouting to unaffected brokers.

---

This project demonstrates the complexity of building a distributed system while delivering a user-friendly and robust interface suitable for real-world applications.


#中文版

# 分布式发布-订阅系统

该项目实现了一个**分布式发布-订阅（Pub-Sub）系统**。该系统通过**Java Socket编程**或**RMI**演示了去中心化架构中的实时消息分发。系统包含三个主要组件：**Broker（代理）**、**Publisher（发布者）**和**Subscriber（订阅者）**，它们无缝通信以管理主题并分发消息。

---

## 功能特点

1. **Broker（代理）**：
    - 三个相互连接的代理节点负责主题管理、订阅和消息分发。
    - 确保网络中的无缝通信和同步。
    - 高效路由消息到发布者和订阅者之间。

2. **Publisher（发布者）**：
    - 创建新主题并发布消息。
    - 查看每个主题的订阅者数量。
    - 删除主题，并自动取消所有关联订阅者的订阅。

3. **Subscriber（订阅者）**：
    - 订阅多个主题并接收实时消息。
    - 列出所有可用主题并查看活动订阅。
    - 从主题中取消订阅，并收到通知。

4. **通信功能**：
    - 通过代理同步实现可靠的消息传递。
    - 实时通知订阅更改和主题删除。

5. **容错性**：
    - 优雅地处理发布者和订阅者的崩溃。
    - 自动管理主题和订阅。

---

## 设计挑战

开发该系统需要解决多个技术挑战：

1. **并发性**：
    - Broker能够处理来自多个发布者和订阅者的并发连接。
    - 正确同步共享资源以防止竞争条件。

2. **网络通信**：
    - 设计一个可靠的协议以确保代理之间的消息一致性。
    - 动态处理消息在多个代理之间的路由。

3. **去中心化**：
    - 确保所有组件独立运行，同时保持系统的协同性。

4. **实时操作**：
    - 确保消息分发给订阅者时的延迟最小化。

---

## 实现细节

### 使用的技术
### 使用的技术

- **Java Socket 编程 / RMI**：  
  实现跨节点的高效分布式通信。
   - **示例代码**：
     ```java
     ServerSocket serverSocket = new ServerSocket(port);
     while (true) {
         Socket clientSocket = serverSocket.accept();
         new Thread(new ClientHandler(clientSocket)).start();
     }
     ```

- **多线程并发处理与锁机制**：  
  支持多个客户端的并发连接，使用线程池和同步机制保护共享资源。
   - **示例代码**：
     ```java
     ExecutorService threadPool = Executors.newFixedThreadPool(10);
     threadPool.submit(() -> handleClient(socket));
     ```

- **自定义通信协议**：  
  通过简单的协议规范（如消息类型+主题ID+内容）解析消息传输。
   - **示例协议结构**：
     ```
     [PUBLISH] [topic_id] [message_content]
     ```
   - **示例代码**：
     ```java
     String message = "PUBLISH topic1 Hello World";
     String[] parts = message.split(" ");
     ```

- **数据结构优化**：  
  使用 `ConcurrentHashMap` 管理主题和订阅者，提高并发读写效率。
   - **示例代码**：
     ```java
     ConcurrentHashMap<String, List<String>> topicSubscribers = new ConcurrentHashMap<>();
     topicSubscribers.putIfAbsent("topic1", new ArrayList<>());
     ```

- **故障检测与恢复机制**：  
  通过心跳检测及时移除失效的客户端，并清理相关资源。
   - **示例代码**：
     ```java
     ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
     scheduler.scheduleAtFixedRate(() -> checkHeartbeats(), 0, 5, TimeUnit.SECONDS);
     ```

- **模块化架构设计**：  
  分离代理、发布者和订阅者功能模块，便于独立扩展。
   - **模块划分示例**：
     ```
     Broker.java
     Publisher.java
     Subscriber.java
     ```

- **基于控制台的用户界面**：  
  通过简单的控制台交互实现常用操作。
   - **示例代码**：
     ```java
     Scanner scanner = new Scanner(System.in);
     System.out.println("Enter command: ");
     String command = scanner.nextLine();
     ```

- **实时消息同步**：  
  使用非阻塞 I/O 快速同步代理间的消息。
   - **示例代码**：
     ```java
     Selector selector = Selector.open();
     channel.register(selector, SelectionKey.OP_READ);
     ```

- **网络故障容错**：  
  在连接失败时自动重试并记录未送达的消息。
   - **示例代码**：
     ```java
     for (int i = 0; i < MAX_RETRIES; i++) {
         try {
             sendMessage(socket, message);
             break;
         } catch (IOException e) {
             Thread.sleep(RETRY_INTERVAL);
         }
     }
     ```

- **日志与监控**：  
  使用日志工具记录关键事件和错误，便于调试和监控。
   - **示例代码**：
     ```java
     Logger logger = LoggerFactory.getLogger(Broker.class);
     logger.info("Message received: {}", message);
     ```


### 部署
- 每个代理作为一个单独的进程运行，并使用预定义端口。
- 发布者和订阅者通过命令行参数动态连接到代理。

### 命令
#### 发布者命令
- `create {topic_id} {topic_name}`：创建一个新主题。
- `publish {topic_id} {message}`：向主题发布消息。
- `show {topic_id}`：显示主题的订阅者数量。
- `delete {topic_id}`：删除主题并通知订阅者。

#### 订阅者命令
- `list {all}`：列出所有可用主题。
- `sub {topic_id}`：订阅一个主题。
- `current`：查看当前订阅。
- `unsub {topic_id}`：取消订阅一个主题。

---

## 使用简便性

虽然后端涉及复杂的分布式系统逻辑，但用户界面非常简洁：
- **发布者**和**订阅者**通过简单的控制台命令交互。
- 系统确保为所有操作提供实时反馈和状态更新。

---

## 快速上手

### 前置条件
- **Java JDK 11+**
- 基本的控制台操作知识。

### 系统运行
1. 启动代理：
   ```bash
   java -jar broker.jar <port> -b [broker_ip1:port1 broker_ip2:port2]
   ```


## 测试与验证

- **并发操作**：  
  系统已针对并发操作进行了测试，确保实时将消息分发给所有订阅者。

- **故障场景**：  
  模拟了发布者和订阅者崩溃等多种故障场景，以验证系统的容错能力，并确保自动管理主题和订阅。

- **代理间通信**：  
  代理之间的通信确保无论订阅者连接到哪个代理，都能接收到消息，维持消息在网络中的一致性。

---

## 解决的挑战

- **分布式同步**：  
  确保分布式代理之间的同步，无消息丢失或重复。

- **并发客户端管理**：  
  高效管理来自发布者和订阅者的多个并发连接。

- **简化**：  
  简化复杂的分布式系统架构，以便实现和使用。

---

## 未来改进

- **动态代理发现**：  
  实现一个目录服务，使代理、发布者和订阅者能够动态发现活动代理并更灵活地管理连接。

- **高级容错**：  
  增强系统以处理代理崩溃，通过自动恢复机制和消息重路由确保系统稳定性。
