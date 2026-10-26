import net.hydra.jojomod.fates.powers.SpinProgression;

public class SpinProgressionTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(SpinProgression.rank(0) == 1, "New players start with Spin Shot");
        check(SpinProgression.rank(19) == 1, "Disrupting Spin must stay locked before its threshold");
        check(SpinProgression.rank(20) == 2, "Disrupting Spin unlocks at 20");
        check(SpinProgression.rank(59) == 2, "Spin Step stays locked before 60");
        check(SpinProgression.rank(60) == 3, "Spin Step unlocks at 60");
        check(SpinProgression.rank(119) == 3, "Ultimate stays locked before mastery");
        check(SpinProgression.rank(120) == 4, "Golden Rotation unlocks at mastery");
        check(SpinProgression.experience(-5) == 0, "Negative saved practice is clamped");
        check(SpinProgression.experience(Integer.MAX_VALUE) == 120, "Practice caps at mastery");
        check(!SpinProgression.canUse(0, 120, 100), "Reject unknown packet slot zero");
        check(!SpinProgression.canUse(5, 120, 100), "Reject unknown packet slot five");
        check(SpinProgression.canUse(1, 0, 0), "Basic technique remains usable at zero Rotation");
        check(!SpinProgression.canUse(2, 0, 100), "Energy cannot bypass a locked skill");
        for (int slot = 2; slot <= 4; slot++) {
            int xp = SpinProgression.requiredExperience(slot);
            int cost = SpinProgression.cost(slot);
            check(!SpinProgression.canUse(slot, xp, cost - 1), "Reject insufficient Rotation");
            check(SpinProgression.canUse(slot, xp, cost), "Allow exact Rotation cost");
            check(SpinProgression.cooldown(slot) > 0, "Skills have nonzero server cooldowns");
        }
        check(SpinProgression.rotation(-50) == 0, "No negative Rotation");
        check(SpinProgression.rotation(130) == 100, "No excess Rotation");
        check(SpinProgression.horseCharge(199, true, 3) == 0, "Mounted movement cannot bypass mastery");
        check(SpinProgression.horseCharge(0, false, 4) == 0, "Standing still cannot build rhythm");
        int horse = 0;
        for (int t = 0; t < 199; t++) horse = SpinProgression.horseCharge(horse, true, 4);
        check(horse == 199, "Infinite Rotation is not ready one tick early");
        horse = SpinProgression.horseCharge(horse, true, 4);
        check(horse == 200, "Ten seconds of movement reaches the required rhythm");
        check(SpinProgression.horseCharge(horse, true, 4) == 200, "Horse rhythm is capped");
        check(SpinProgression.horseCharge(horse, false, 4) == 196, "Losing horse movement decays rhythm");
        check(SpinProgression.horseCharge(2, false, 4) == 0, "Decay cannot go negative");
        int xp = 0, rotation = 0;
        for (int hit = 0; hit < 30; hit++) {
            xp = SpinProgression.experience(xp + 4);
            rotation = SpinProgression.rotation(rotation + 12);
        }
        check(SpinProgression.canUse(4, xp, rotation), "Successful practice can reach mastery and fund the ultimate");
        rotation -= SpinProgression.cost(4);
        check(!SpinProgression.canUse(4, xp, rotation), "A spent meter cannot activate the ultimate twice");
        System.out.println(checks + " Spin progression, resource, and horse-rhythm checks passed");
    }
}
