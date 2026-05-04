package com.iccuu.general_web_backend.infrastructure.monitor;

import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class HealthScoringService {

    /**
     * Calculate health score (0-100) for a cluster based on metrics.
     * Formula: uptime%*0.3 + playerRetention*0.2 + (1-crashRate)*0.3 + resourceHeadroom*0.2
     */
    public int calculateScore(Map<String, Object> metrics) {
        double uptime = toDouble(metrics.getOrDefault("uptimeRatio", 1.0));
        double retention = toDouble(metrics.getOrDefault("playerRetention", 1.0));
        double crashRate = Math.min(toDouble(metrics.getOrDefault("crashRate", 0.0)), 1.0);
        double headroom = toDouble(metrics.getOrDefault("resourceHeadroom", 1.0));

        double score = uptime * 30 + retention * 20 + (1 - crashRate) * 30 + headroom * 20;
        return Math.max(0, Math.min(100, (int) Math.round(score)));
    }

    public String getScoreLabel(int score) {
        if (score >= 80) return "healthy";
        if (score >= 50) return "degraded";
        return "critical";
    }

    public Map<String, Object> generateSuggestions(Map<String, Object> metrics, int score) {
        java.util.List<String> suggestions = new java.util.ArrayList<>();
        double crashRate = toDouble(metrics.getOrDefault("crashRate", 0.0));
        double headroom = toDouble(metrics.getOrDefault("resourceHeadroom", 1.0));
        long daysSinceBackup = toLong(metrics.getOrDefault("daysSinceBackup", 0));
        long daysSinceUpdate = toLong(metrics.getOrDefault("daysSinceUpdate", 0));
        long playerCount = toLong(metrics.getOrDefault("playerCount", 10));

        if (crashRate > 0.1) suggestions.add("High crash rate detected — check mod compatibility");
        if (headroom < 0.3) suggestions.add("Low resource headroom — consider adding more memory");
        if (daysSinceBackup > 7) suggestions.add("No backup in " + daysSinceBackup + " days — create a backup");
        if (daysSinceUpdate > 14) suggestions.add("DST version may be outdated — check for updates");
        if (playerCount == 0) suggestions.add("No players online — consider stopping to save resources");

        return Map.of("score", score, "label", getScoreLabel(score), "suggestions", suggestions);
    }

    private double toDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0.0; }
    }

    private long toLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; }
    }
}
