# High-Performance Concurrent Rate Limiter (Token Bucket)

### 🚩 Problem Statement
In modern distributed systems, APIs can be overwhelmed by a high volume of requests (intentional or accidental), leading to server crashes or degraded performance. A mechanism is required to:
- Control the rate of traffic sent or received by a network interface or service.
- Support different service levels (Tiers) for different users.
- Handle thousands of simultaneous requests without the overhead of heavy thread locking.

### 🛠️ Tech Stack
- **Language:** Java 17+
- **Concurrency Utilities:** `java.util.concurrent` (ConcurrentHashMap, ScheduledExecutorService)
- **Atomic Operations:** `java.util.concurrent.atomic.AtomicLong` for lock-free synchronisation.

### 💡 Solution: The Token Bucket Algorithm
The system uses the **Token Bucket** algorithm, where tokens are added to a "bucket" at a fixed rate. Each request must "consume" a token to proceed. 
- **Efficiency:** By using `AtomicLong` and `compareAndSet` (CAS), the system achieves thread-safety without using `synchronised` blocks, significantly reducing latency.
- **Background Refill:** A dedicated scheduler replenishes tokens independently of incoming requests, ensuring the "Security Guard" (Rate Limiter) never slows down the "Customer" (Request).



### ✨ Key Features
- **Dynamic Tier Management:** Supports **FREE**, **PREMIUM**, and **ENTERPRISE** tiers with different burst capacities and refill rates.
- **Non-Blocking Logic:** High-concurrency support using lock-free data structures.
- **Dynamic Upgrades:** Ability to upgrade a user's tier in real-time without system downtime or data loss.
- **Interactive Simulation:** A CLI interface to simulate "burst" traffic and witness rate-limiting (HTTP 429) in action.

### 🧪 Use Case (Example Flow)
1. **Registration:** Register user `Dev_Givi` under the **FREE** tier (Capacity: 2).
2. **Burst Request:** Send 3 rapid requests.
   - *Result:* Request 1 & 2: ✅ **Success**. Request 3: ❌ **Limited** (Bucket empty).
3. **Upgrade:** Dynamically upgrade `Dev_Givi` to **ENTERPRISE**.
4. **Retry:** Send 5 rapid requests.
   - *Result:* All 5 requests: ✅ **Success** (Increased capacity).

### 🛠️ How to Run
1. Clone the repository: `git clone https://github.com/givijain1234/rate-limiter-java.git`
2. Run `Main.java` in any Java IDE.
3. Use the menu to register users and simulate API hits.
