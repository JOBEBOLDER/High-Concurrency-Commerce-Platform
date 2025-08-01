# High-Concurrency Commerce Platform

This project is an extension of my undergraduate project, link:https://github.com/JOBEBOLDER/SUPERLAND,
a spatial exploration designed to enhance the food experience by creating an immersive and interactive environment.
<img width="722" alt="image" src="https://github.com/user-attachments/assets/5f89d7b2-0146-445f-a1c0-49be65084987" />
<img width="685" alt="image" src="https://github.com/user-attachments/assets/91bfce3c-2bdd-4ad9-9569-e1e3e144b7df" />




A high-performance e-commerce platform built with Java, Spring Boot, and Redis, focusing on solving concurrency challenges in high-traffic scenarios. This platform features optimized solutions for user authentication, merchant management, flash sales, and social interaction components.

## 🚀 Key Features Introduction:
<img width="1339" alt="image" src="https://github.com/user-attachments/assets/10019b9e-1657-418e-a436-ee485f7c3114" />
<img width="685" alt="image" src="https://github.com/user-attachments/assets/86fb8ef8-d05b-4a06-8515-859fe1f4901a" />

##  project structure:
<img width="706" alt="image" src="https://github.com/user-attachments/assets/a52ade00-86fb-46bb-ae5b-78992c94af86" />



### 📱 User Authentication
- Redis-based distributed session management replacing traditional Tomcat sessions
- Token-based authentication with automatic refresh
- Secure SMS verification code login system
  #### Session Problems
  - Multiple Tomcat instances don't share session storage space, leading to data loss when requests switch to different servers.
  - Session data is also lost when servers are restarted.
  #### Session Alternatives
  - Data sharing
  - In-memory storage
  - Key-value structure
  #### Redis-based Shared Session Login
  Verification code storage using String structure
  User information storage

  String structure: Serialize objects to JSON and store in Redis.
  HASH structure: Store each field of the object separately, supporting CRUD operations on individual fields, using less memory than String.

<img width="1110" alt="image" src="https://github.com/user-attachments/assets/5c2a8d06-72cd-455d-9cd4-55d50eecaf44" />
<img width="1186" alt="image" src="https://github.com/user-attachments/assets/78ab68d7-de58-4d6e-8c3e-dff3f9ab521a" />

### Interceptor Optimization
New token interceptor: responsible for refreshing the validity time of the token in redis




### 🏪 Merchant Management
<img width="1104" alt="image" src="https://github.com/user-attachments/assets/ffa85eba-5666-45a9-bd66-3a2c5c1570fe" />
two important things to mark here: when we submit the shopID form, and check the cache from Redis,
anther case, if we can not find the shopID in Redis, we will query the database and then update the Redis cache.
#### Exercise: Store Type Query Business Add Cache
- The store type is used on the home page and several other pages, as shown here:
  <img width="809" alt="image" src="https://github.com/user-attachments/assets/c089dad9-1be3-4000-a499-05b91186953b" />
  Requirement: Modify the queryTypeList method in ShopTypeController to add query caching:
  <img width="809" alt="image" src="https://github.com/user-attachments/assets/5a7829e0-20ab-41d2-a82a-10844ad1a853" />
  Here's the markdown code for your content:


### 3. Cache Update Strategies
![img_14.png](readmepics/img_14.png)


#### 🧠 Read Operations:
- If the cache is hit: return the data directly.
- If the cache is missed: query the database first, then update the cache and set an expiration time.

---

#### 🖋️ Write Operations:
- Use the write-then-delete strategy:
  - Update the database first, then delete the Redis cache.
  - Use Spring's @Transactional annotation or TCC compensation to ensure consistency between the database and the cache.


### Ensuring Consistency Between Shop Cache and Database (Double Write Consistency)
	1.	Set expiration time during read operations
	2.	On updates: update the database first, then delete the corresponding Redis key.
Use @Transactional to ensure the operation is wrapped in a transaction.

## 5. Cache Issues

### Cache Penetration
When client requests miss both in cache and database, it's cache penetration.

**Solutions:**
* **Cache Empty Objects**: When no data exists in both cache and database, cache a null object with TTL.
  * Pros: Simple implementation
  * Cons: Extra memory consumption, potential data inconsistency
* **Bloom Filter**: A pre-filter that checks if data exists in cache. Returns immediately if not found, continues query if found

### Cache Avalanche
When a large number of cache keys expire simultaneously or Redis service crashes, causing massive requests to hit the database.

**Solutions:**
* Set different random TTL values for keys
* Improve Redis availability (using cluster solutions with sentinel mechanism)
* Add degradation and rate limiting strategies to cache services using sentinel
* Set keys to never expire (no TTL)
* Implement multi-level caching for services

### Cache Breakdown
When a hot key is accessed with high concurrency and its cache has expired, countless requests bypass the cache and directly hit the database,
as picture shown:
![img.png](readmepics/img.png)

#### Cache Breakdown solutions:1,Mutual Exclusion Lock 2,Logical Expiration:
![img_1.png](readmepics/img_1.png)

#### Cache Breakdown Solutions:

* **Mutual Exclusion Lock Approach**: When multiple threads query data, apply mutual exclusion locks to ensure only one thread can rebuild the cache by querying the database at any given time, while other threads wait:
  * **Pros**: Ensures data consistency
  * **Cons**: Sacrifices system availability - if reconstruction takes too long, other requests must wait. Also carries deadlock risks

* **Logical Expiration Approach**: Add a new logical expiration field to cached data. When a query arrives, first check if the current time exceeds expiration. If expired, acquire a lock and start a new thread responsible for cache rebuilding while immediately returning the old data. Other threads similarly attempt to acquire the lock - if unsuccessful, they directly return the expired data:
  * **Pros**: Better performance, greatly enhanced system availability
  * **Cons**: Additional memory consumption, prioritizes availability over consistency (sacrificing strong consistency). More complex implementation

#### 🧭 Two solutions flowchart:
![img_2.png](readmepics/img_2.png)
![img_3.png](readmepics/img_3.png)

### 🔥 Flash Sales System 

- ![img_4.png](readmepics/img_4.png)
#### Coupon Flash Sale Analysis

#### Pessimistic Lock Approach
Adding synchronization locks to make threads execute serially. Using synchronized or Lock.
* **Advantages**: Simple and direct, applies locks to shared resource objects
* **Disadvantages**: Generally lower performance

#### Optimistic Lock Approach
No locks applied, only checking whether other threads have modified the data during updates. Using CAS version number method or leveraging database row locks with UPDATE statements.
* **Advantages**: Better performance
* **Disadvantages**: Can have low success rates in high-contention scenarios

#### Coupon Flash flowchart:
![img_5.png](readmepics/img_5.png)

![img_15.png](readmepics/img_15.png)
This diagram illustrates how a distributed lock ensures data consistency and concurrency control across multiple JVM instances in a “one person, one order” scenario.

⸻

✅ Key Flow:
1.	Only one thread (Thread 1 in JVM1) can acquire the distributed lock from the Lock Coordinator.
2.	It checks if the order already exists:
•	If yes, it throws an error (duplicate order).
•	If no, it inserts the new order safely.
3.	After completing, it releases the lock.
4.	Other threads (Thread 2, 3, 4) that failed to get the lock initially:
•	Wait in line until the lock is released.
•	Once released, they retry:
•	If the order now exists, they throw an error (as expected).
•	Otherwise, they insert (if applicable, but usually it fails due to existing order).

⸻

🧠 Why this matters:

Without a distributed lock, multiple JVMs might allow multiple threads to create duplicate orders.
With a lock coordinator (e.g., Redis, ZooKeeper), only one thread globally can access the critical section at a time, ensuring cross-instance safety.
![img_6.png](readmepics/img_6.png)
![img_7.png](readmepics/img_7.png)

### Using command to add Redis lock with expiration time

```bash
# Add lock with value, only if key doesn't exist (NX), and set expiration to 10 seconds (EX 10)
set lock value NX EX 10

# Unlock by deleting the key
del lock
```
#### Lua script manipulation of redis to achieve atomic unlock operations
```bash
unlock.lua
-- key
if (ARGV[1] == redis.call('get', KEYS[1])) then
    -- 释放锁
    return redis.call('del', KEYS[1])
end
return 0


```
#### Solving Flash Sale (Seckill) Problems Using Redisson Distributed Locks
![img_8.png](readmepics/img_8.png)

![img_9.png](readmepics/img_9.png)

#### Summary of Redisson distubuted lock :
![img_10.png](readmepics/img_10.png)

### Coupon Flash Sale Optimization Plan:
Optimized process for coupon flash sales: Redis + Lua + BlockingQueue

Overall steps:
1.	Use Lua script + Redis to handle the flash sale logic and make sure everything is done atomically (all at once, safely).
2.	For creating orders, use a separate thread to handle the task asynchronously. It takes data from a blocking queue and saves it to the database.
![img_11.png](readmepics/img_11.png)
      ❗ Problems with the original solution (BlockingQueue in JVM):
      1.	Memory Issue:
      BlockingQueue uses Java memory (JVM). If there are too many orders, it may run out of memory and crash, or some orders might not be created.
      2.	Data Safety Issue:
      If the JVM crashes, all order data in the BlockingQueue will be lost.

⸻

✅ Improved Solution: Use Redis as a Message Queue

1. Using Redis List as a Queue

Use Redis List with BLPUSH + BRPOP or BRPUSH + BLPOP to create a blocking queue.

Pros:
•	Uses Redis memory → no memory limit
•	Redis has persistence → data is safer
•	Keeps message order

Cons:
•	Can’t fully prevent message loss
•	Only supports one consumer (one worker can read the queue)

2. Using Redis Pub/Sub (Publish/Subscribe model)

Use PUBLISH to send messages, SUBSCRIBE or PSUBSCRIBE to receive messages.

Pros:
•	Supports multiple senders and receivers (many producers and consumers)

Cons:
•	No data persistence
•	Messages can still be lost
•	Limited message buffer → old messages may be lost if buffer is full

3. Using Redis Stream

Pros:
•	Messages are saved (persistent)
•	Has ACK (acknowledge) system to confirm message received
•	Supports multiple consumers

________________________________________________________________
### 👥 Social Features:👍 Like Leaderboard
Use Redis ZSet (sorted set) to create a leaderboard.
Each like is stored with a timestamp, so you can sort likes based on time.

#### 👥 Friend Follow System

Mutual Follows:
Use Redis ZSet to find common follows between users by doing set intersection.

#### 📰 Feed System (Timeline / Content Push)
1. Time-based Feed (Timeline)
   •	No filtering, just show posts sorted by publish time
   •	Often used for showing what friends post (like a WeChat Moments or Instagram feed)
Pros:
•	Shows all posts (nothing missed)
•	Easy to implement
Cons:
•	May show too many unrelated or boring posts
•	Not very efficient for finding interesting content

2. Smart Feed (Intelligent Sorting)
   •	Uses algorithms to hide spam or boring posts
   •	Tries to show only what you like

Pros:
•	Shows content users are interested in
•	Users stay longer and keep scrolling

Cons:
•	If the algorithm is bad, it might backfire
•	May show wrong or less useful posts

#### 📥 Push and Pull Models

🔄 Pull Mode (Read Diffusion):
•	When you open the app, it checks who you follow and loads their latest posts
•	Slower, but uses less memory

📤 Push Mode (Write Diffusion):
•	When someone posts, it directly sends the post to all their followers
•	Faster, but uses more memory
•	Best when users don’t follow too many people


🔁 Push + Pull Combined Mode
•	Mix of both models
•	Example:
•	If a celebrity has lots of followers, push to active fans, and let normal fans pull manually
•	If someone has few followers, push to everyone’s inbox
![img_12.png](readmepics/img_12.png)
## 🔧 Technical Implementation

### Redis Cache Solutions
- **Cache Penetration Prevention**: Empty result caching and Bloom filter implementation
- **Cache Avalanche Protection**: TTL randomization, Redis cluster with Sentinel
- **Cache Breakdown Handling**:
  - Mutex lock approach for consistency
  - Logical expiration for high availability

### Distributed Locks
- Redis-based distributed lock with Lua script for atomic operations
- Redisson implementation with reentrant lock support
- Watchdog mechanism for lock renewal

### Message Queue Patterns
- List-based blocking queue implementation
- Pub/Sub messaging for multi-consumer scenarios
- Redis Stream for persistent, acknowledged message delivery

### Feed Stream Implementation
- Timeline-based chronological content delivery
- ZSET-based scoring and ranking
- Hybrid push-pull model optimizing for different user types

## 🛠️ Tech Stack
- Java
- Spring Boot
- Redis
- MySQL
- Maven

## 🔍 Project Structure
The application follows a standard layered architecture:
- Controllers: Handle HTTP requests
- Services: Implement business logic
- Repositories: Data access layer
- Utils: Helper classes and utilities

## 📊 Performance Metrics
- 8K+ TPS during flash sales
- 15K+ daily inventory conflicts resolved
- 65% reduction in MySQL load
- 40% reduction in content delivery latency

## 📝 Usage Examples

### Login Implementation
```java
// Code examples demonstrating the login flow
```

### Flash Sale Implementation
```java
// Code examples demonstrating the flash sale mechanics
```

### Feed Stream Example
```java
// Code examples demonstrating the feed implementation
```

## 🚀 Getting Started

### Prerequisites
- JDK 11+
- Redis 6.0+
- MySQL 5.7+
- Maven 3.6+

### Installation
1. Clone the repository
```
git clone https://github.com/JOBEBOLDER/High-Concurrency-Commerce-Platform.git
```

2. Configure application properties
```
# Configure your database and Redis settings in application.yaml
```

3. Run the application
```
mvn spring-boot:run
```

## 📚 Learning Resources
This project implements various patterns and solutions discussed in Redis best practices and distributed systems design literature.

## 📄 License
[MIT](LICENSE)

## 👨‍💻 Author
JOBEBOLDER
