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
        Reward r1 = new Reward("a", 1.0, "COMMON", List.of(), List.of(), false, null, 0D);
        Reward r2 = new Reward("b", 3.0, "RARE", List.of(), List.of(), false, null, 0D);
        RewardPicker picker = new RewardPicker(List.of(r1, r2));
        int a=0,b=0;
        for (int i=0;i<10000;i++) {
            Reward r = picker.pick();
            if (r == r1) a++; else b++;
        }
        System.out.println("Distribution a/b = " + a + "/" + b);
    }
}
