package com.vaultdesk.service;

import com.vaultdesk.model.Credential;
import com.vaultdesk.model.VaultItem;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class SecurityScoreService {

    private static final int OLD_PASSWORD_DAYS = 180;
    private static final int WEAK_THRESHOLD = 50;
    private static final int STRONG_THRESHOLD = 75;

    private final PasswordGenerator strengthChecker = new PasswordGenerator();

    public static class Report {
        public int score;
        public int strongCount;
        public int weakCount;
        public int reusedCount;
        public int oldCount;
        public int totalCredentials;
    }

    public Report analyze(Map<Integer, VaultItem> items, List<Credential> credentials) {
        Report report = new Report();
        report.totalCredentials = credentials.size();

        if (credentials.isEmpty()) {
            report.score = 100;
            return report;
        }

        Map<String, Integer> passwordFrequency = new HashMap<>();
        for (Credential c : credentials) {
            if (c.getPlainPassword() != null) {
                passwordFrequency.merge(c.getPlainPassword(), 1, Integer::sum);
            }
        }

        int runningScore = 0;
        int baselinePerItem = 100 / credentials.size();

        for (Credential c : credentials) {
            String pwd = c.getPlainPassword();
            if (pwd == null) continue;

            int strength = strengthChecker.estimateStrength(pwd);
            int itemScore = baselinePerItem;

            if (strength >= STRONG_THRESHOLD) {
                report.strongCount++;
                itemScore += 10;
            } else if (strength < WEAK_THRESHOLD) {
                report.weakCount++;
                itemScore -= 10;
            }

            if (passwordFrequency.getOrDefault(pwd, 1) > 1) {
                report.reusedCount++;
                itemScore -= 15;
            }

            VaultItem item = items.get(c.getItemId());
            if (item != null && item.getUpdatedAt() != null) {
                long daysOld = ChronoUnit.DAYS.between(item.getUpdatedAt(), LocalDateTime.now());
                if (daysOld > OLD_PASSWORD_DAYS) {
                    report.oldCount++;
                    itemScore -= 5;
                }
            }

            runningScore += itemScore;
        }

        report.score = Math.max(0, Math.min(100, runningScore / credentials.size() + 50));
        return report;
    }
}
