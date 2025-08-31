package me.ycxmbo.minecrates.crate;

import java.util.*;

public final class RewardPicker {
    private final List<Reward> rewards;
    private final double total;

    public RewardPicker(List<Reward> rewards) {
        this.rewards = new ArrayList<>(rewards);
        double t = 0;
        for (Reward r : rewards) t += r.weight();
        this.total = Math.max(0.00001, t);
    }

    public Reward pick() {
        double roll = ThreadLocalRandom.current().nextDouble() * total;
        double c = 0;
        for (Reward r : rewards) {
            c += r.weight();
            if (roll <= c) return r;
        }
        return rewards.get(rewards.size() - 1);
    }

    public double weightPercent(Reward r) {
        return r.weight() / total;
    }
}
