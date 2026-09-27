package com.pedidai.api.security;

import com.pedidai.api.exceptions.TooManyRequestsException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límit d'ús en memòria per finestra lliscant (l'API s'executa en una sola instància).
 * Protegeix el login contra força bruta, el registre contra abusos i limita el cost de la IA.
 */
@Component
public class RateLimiter {

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    /** Registra un intent i llança 429 si la clau ja ha arribat al màxim dins la finestra. */
    public void check(String key, int max, Duration window, String messageKey) {
        if (!tryAcquire(key, max, window)) {
            throw new TooManyRequestsException(messageKey);
        }
    }

    public boolean tryAcquire(String key, int max, Duration window) {
        long now = System.currentTimeMillis();
        long from = now - window.toMillis();
        Deque<Long> deque = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst() < from) {
                deque.pollFirst();
            }
            if (deque.size() >= max) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }

    /** Oblida els intents d'una clau (p. ex. després d'un login correcte). */
    public void reset(String key) {
        hits.remove(key);
    }

    /** Neteja periòdica de claus sense activitat recent per no acumular memòria. */
    @Scheduled(fixedDelay = 3_600_000)
    public void purge() {
        long from = System.currentTimeMillis() - Duration.ofDays(1).toMillis();
        hits.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                return e.getValue().isEmpty() || e.getValue().peekLast() < from;
            }
        });
    }
}
