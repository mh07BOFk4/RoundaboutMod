import net.hydra.jojomod.entity.projectile.SpinProjectileRules;

public class SpinProjectileRulesTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(SpinProjectileRules.canRedirect(true, false, 0, 1, 12), "Owner can redirect a fresh airborne ball");
        check(!SpinProjectileRules.canRedirect(false, false, 0, 1, 12), "Cannot steer another player's ball");
        check(!SpinProjectileRules.canRedirect(true, true, 0, 1, 12), "Return flight cannot be weaponized");
        check(!SpinProjectileRules.canRedirect(true, false, 1, 1, 12), "Fresh Fate gets one redirect");
        check(SpinProjectileRules.canRedirect(true, false, 1, 2, 12), "Mastery permits a second redirect");
        check(!SpinProjectileRules.canRedirect(true, false, 2, 2, 12), "No third redirect");
        check(!SpinProjectileRules.canRedirect(true, false, 2, 99, 12), "A large requested limit cannot bypass the hard cap");
        check(!SpinProjectileRules.canRedirect(true, false, -1, 2, 12), "Negative counts are invalid");
        check(!SpinProjectileRules.canRedirect(true, false, 0, 2, -1), "Negative age is invalid");
        check(SpinProjectileRules.canRedirect(true, false, 0, 2, 71), "Last eligible flight tick");
        check(!SpinProjectileRules.canRedirect(true, false, 0, 2, 72), "Absolute flight cutoff");
        int deadline = SpinProjectileRules.returnDeadline(8, SpinProjectileRules.OUTBOUND_TICKS);
        check(deadline >= SpinProjectileRules.OUTBOUND_TICKS, "Steering never shortens the original flight budget");
        deadline = SpinProjectileRules.returnDeadline(30, deadline);
        check(deadline == 54, "Steering grants time to complete the new trajectory");
        deadline = SpinProjectileRules.returnDeadline(53, deadline);
        check(deadline == 72, "Extension respects the absolute outbound cap");
        check(SpinProjectileRules.returnDeadline(70, deadline) == 72, "Repeated extensions cannot keep a ball airborne forever");
        check(SpinProjectileRules.LIFETIME_TICKS > SpinProjectileRules.MAX_OUTBOUND_TICKS + 64,
                "Recall retains a separate recovery budget after the maximum outbound flight");
        check(SpinProjectileRules.hitsLegs(0.4, 2), "Low contact selects leg muscles");
        check(!SpinProjectileRules.hitsLegs(1.2, 2), "High contact selects upper-body muscles");
        check(!SpinProjectileRules.hitsLegs(0.9, 2), "The exact threshold belongs to the upper body");
        check(!SpinProjectileRules.hitsLegs(0, 0), "Zero-height entities are not leg targets");
        System.out.println(checks + " Spin ownership, redirect, and lifetime checks passed");
    }
}
