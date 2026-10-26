import net.hydra.jojomod.fates.powers.SpinProgression;

public class SpinProgressionTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(SpinProgression.rank(0) == 1, "Fresh training starts at rank 1");
        check(SpinProgression.rank(19) == 1, "Second redirect needs its practice threshold");
        check(SpinProgression.rank(20) == 2, "Second redirect unlocks at 20");
        check(SpinProgression.rank(59) == 2, "Defense upgrade threshold");
        check(SpinProgression.rank(60) == 3, "Upgraded body Spin at 60");
        check(SpinProgression.rank(119) == 3, "Mounted mastery threshold");
        check(SpinProgression.rank(120) == 4, "Mounted mastery at 120");
        check(SpinProgression.experience(-5) == 0, "Negative saved practice clamps");
        check(SpinProgression.experience(Integer.MAX_VALUE) == 120, "Practice caps at mastery");
        check(!SpinProgression.canUse(0), "Reject invalid slot zero");
        check(!SpinProgression.canUse(5), "Reject invalid slot five");
        for (int slot = 1; slot <= 4; slot++) {
            check(SpinProgression.canUse(slot), "All four base moves work immediately");
            check(SpinProgression.cooldown(slot) > 0, "Server cooldown prevents packet spam");
        }
        check(SpinProgression.redirects(1) == 1, "Initial steering limit");
        check(SpinProgression.redirects(2) == 2, "Mastered steering limit");
        check(SpinProgression.redirects(4) == 2, "Mastery does not remove steering budget");
        check(SpinProgression.guardDuration(1) == 30, "Initial hardening lasts 1.5 seconds");
        check(SpinProgression.guardDuration(3) == 40, "Upgraded hardening lasts 2 seconds");
        check(SpinProgression.perfectGuard(30, 1), "Perfect defense begins immediately");
        check(SpinProgression.perfectGuard(23, 1), "Eighth perfect-defense tick is included");
        check(!SpinProgression.perfectGuard(22, 1), "Perfect window expires after eight ticks");
        check(!SpinProgression.perfectGuard(31, 1), "Invalid remaining time is not a perfect defense");
        check(SpinProgression.perfectGuard(33, 3), "Upgraded guard keeps the same eight-tick timing window");
        check(!SpinProgression.perfectGuard(32, 3), "Upgraded perfect window expires on schedule");
        check(SpinProgression.guardReduction(30, 1, true) == .75F, "Well-timed physical defense");
        check(SpinProgression.guardReduction(22, 1, true) == .4F, "Late physical defense");
        check(SpinProgression.guardReduction(0, 1, true) == 0, "Expired defense cannot reduce damage");
        check(SpinProgression.guardReduction(30, 1, false) == 0, "Environmental and magical damage stays unaffected");
        check(!SpinProgression.infiniteAvailable(3, true), "Horse alone cannot bypass mastery");
        check(!SpinProgression.infiniteAvailable(4, false), "Mastery alone cannot bypass natural horse movement");
        check(SpinProgression.infiniteAvailable(4, true), "Mastery plus movement is immediately ready without a timer");
        check(SpinProgression.practice(false, true) > SpinProgression.practice(false, false), "Trick shots train faster");
        check(SpinProgression.practice(true, true) > SpinProgression.practice(true, false), "Golden trick shots retain their bonus");
        int xp = 0;
        for (int hit = 0; hit < 20; hit++) xp = SpinProgression.experience(xp + SpinProgression.practice(false, true));
        check(SpinProgression.infiniteAvailable(SpinProgression.rank(xp), true), "Active trick-shot play reaches mounted mastery");
        System.out.println(checks + " Spin combat, defense, progression, and mounted-mastery checks passed");
    }
}
