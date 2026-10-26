package net.hydra.jojomod.entity.projectile;

/** Flight budgets remain absolute; steering never resets the safety timeout. */
public final class SpinProjectileRules {
    public static final int OUTBOUND_TICKS = 32;
    public static final int MAX_OUTBOUND_TICKS = 72;
    public static final int LIFETIME_TICKS = 200;
    private SpinProjectileRules() {}
    public static boolean canRedirect(boolean owner, boolean returning, int used, int limit, int age) {
        return owner && !returning && used >= 0 && used < Math.min(2, limit)
                && age >= 0 && age < MAX_OUTBOUND_TICKS;
    }
    public static int returnDeadline(int age, int previous) {
        return Math.min(MAX_OUTBOUND_TICKS, Math.max(previous, age + 24));
    }
    public static boolean hitsLegs(double relativeHeight, double targetHeight) {
        return targetHeight > 0 && relativeHeight < targetHeight * 0.45;
    }
}
