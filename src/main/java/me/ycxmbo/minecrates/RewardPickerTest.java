package me.ycxmbo.minecrates;

import me.ycxmbo.minecrates.crate.Reward;
import me.ycxmbo.minecrates.crate.RewardPicker;

import java.util.List;

/**
 * Dev-only quick check. Not a JUnit test.
 * Move a proper test to src/test/java later.
 */
public final class RewardPickerTest {
    public static void devCheck() {
        Reward r1 = new Reward("a", "a", 1.0, Reward.Rarity.COMMON, List.of(), List.of(), false, null, 0D, 0);
        Reward r2 = new Reward("b", "b", 3.0, Reward.Rarity.RARE, List.of(), List.of(), false, null, 0D, 0);

        RewardPicker picker = new RewardPicker(List.of(r1, r2));
        int a=0,b=0;
        for (int i=0;i<10000;i++) {
            Reward r = picker.pick();
            if (r == r1) a++; else b++;
        }
        System.out.println("Distribution a/b = " + a + "/" + b);
    }
}
