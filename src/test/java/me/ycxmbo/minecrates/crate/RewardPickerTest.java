package me.ycxmbo.minecrates.crate;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RewardPickerTest {

    private static Reward reward(String id, double weight) {
        // No ItemStack/commands so the Reward can be built without a running server.
        return new Reward(id, id, weight, null, List.of(), List.of(), false, null, 0D, 0);
    }

    @Test
    void weightedDistributionIsRoughlyProportional() {
        Reward common = reward("common", 3.0);
        Reward rare = reward("rare", 1.0);
        RewardPicker picker = new RewardPicker(List.of(common, rare));

        int trials = 200_000;
        int commonHits = 0;
        for (int i = 0; i < trials; i++) {
            if (picker.pick() == common) commonHits++;
        }
        double commonRatio = (double) commonHits / trials;
        // Expected 0.75; allow a small tolerance for randomness.
        assertTrue(Math.abs(commonRatio - 0.75) < 0.02,
                "common ratio was " + commonRatio + ", expected ~0.75");
    }

    @Test
    void weightPercentSumsToOne() {
        Reward a = reward("a", 2.0);
        Reward b = reward("b", 2.0);
        RewardPicker picker = new RewardPicker(List.of(a, b));
        assertEquals(1.0, picker.weightPercent(a) + picker.weightPercent(b), 1e-9);
    }

    @Test
    void singleRewardAlwaysWins() {
        Reward only = reward("only", 1.0);
        RewardPicker picker = new RewardPicker(List.of(only));
        for (int i = 0; i < 1_000; i++) {
            assertEquals(only, picker.pick());
        }
    }

    @Test
    void zeroWeightDoesNotBreakPicker() {
        Reward zero = reward("zero", 0.0);
        Reward normal = reward("normal", 5.0);
        RewardPicker picker = new RewardPicker(List.of(zero, normal));
        Reward picked = picker.pick();
        assertNotNull(picked);
    }
}
