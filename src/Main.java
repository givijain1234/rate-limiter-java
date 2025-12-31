package com.rate.limiter;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// --- ENUMS ---
enum UserTier {
    FREE(2, 1), PREMIUM(5, 3), ENTERPRISE(10, 5);
    final int capacity;
    final int refillRate;
    UserTier(int cap, int rate) { this.capacity = cap; this.refillRate = rate; }
}

// --- MODELS ---
class TokenBucket {
    private volatile UserTier tier;
    private final AtomicLong currentTokens;

    public TokenBucket(UserTier tier) {
        this.tier = tier;
        this.currentTokens = new AtomicLong(tier.capacity);
    }

    public void updateTier(UserTier newTier) {
        this.tier = newTier;
        this.currentTokens.set(newTier.capacity);
    }

    public boolean tryConsume() {
        while (true) {
            long tokens = currentTokens.get();
            if (tokens <= 0) return false;
            if (currentTokens.compareAndSet(tokens, tokens - 1)) return true;
        }
    }

    public void refill() {
        long current = currentTokens.get();
        long next = Math.min(tier.capacity, current + tier.refillRate);
        currentTokens.set(next);
    }

    public long getAvailable() { return currentTokens.get(); }
}

// --- SERVICE ---
class RateLimiterService {
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, UserTier> userTiers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public RateLimiterService() {
        // Automatically refill every 5 seconds
        scheduler.scheduleAtFixedRate(() -> {
            buckets.values().forEach(TokenBucket::refill);
        }, 5, 5, TimeUnit.SECONDS);
    }

    public void registerOrUpgrade(String userId, UserTier tier) {
        userTiers.put(userId, tier);
        if (buckets.containsKey(userId)) {
            buckets.get(userId).updateTier(tier);
        } else {
            buckets.put(userId, new TokenBucket(tier));
        }
    }

    public void hitApi(String userId) {
        TokenBucket bucket = buckets.get(userId);
        if (bucket == null) {
            System.out.println("❌ User not found. Register them first.");
            return;
        }
        if (bucket.tryConsume()) {
            System.out.println("✅ [200 OK] Request processed for " + userId + " (Tokens left: " + bucket.getAvailable() + ")");
        } else {
            System.out.println("❌ [429 Too Many Requests] " + userId + " is rate limited!");
        }
    }

    public void showStatus() {
        System.out.println("\n--- LIVE BUCKET STATUS ---");
        buckets.forEach((id, bucket) ->
                System.out.println("User: " + id + " | Tokens: " + bucket.getAvailable()));
    }
}

// --- MAIN RUNNER ---
public class Main {
    public static void main(String[] args) {
        RateLimiterService service = new RateLimiterService();
        Scanner scanner = new Scanner(System.in);

        System.out.println("🛡️ ADVANCED RATE LIMITER SYSTEM LOADED");

        while (true) {
            try {
                System.out.println("\n1. Register/Upgrade User | 2. Send Request | 3. Check All Status | 4. Quit");
                System.out.print("Action: ");
                int choice = Integer.parseInt(scanner.nextLine());

                if (choice == 4) break;

                switch (choice) {
                    case 1 -> {
                        System.out.print("Enter User ID: ");
                        String id = scanner.nextLine();
                        System.out.print("Select Tier (1: FREE, 2: PREMIUM, 3: ENTERPRISE): ");
                        int t = Integer.parseInt(scanner.nextLine());
                        UserTier tier = switch (t) {
                            case 2 -> UserTier.PREMIUM;
                            case 3 -> UserTier.ENTERPRISE;
                            default -> UserTier.FREE;
                        };
                        service.registerOrUpgrade(id, tier);
                        System.out.println("👤 User " + id + " set to " + tier);
                    }
                    case 2 -> {
                        System.out.print("Enter User ID to send request: ");
                        service.hitApi(scanner.nextLine());
                    }
                    case 3 -> service.showStatus();
                    default -> System.out.println("⚠️ Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println("❌ Error: Please enter a valid number.");
            }
        }
        System.out.println("Shutting down... Goodbye!");
        System.exit(0);
    }
}