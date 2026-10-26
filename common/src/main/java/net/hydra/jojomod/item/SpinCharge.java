package net.hydra.jojomod.item;

/** Shared timing rules, in game ticks (20 ticks = one second). */
public final class SpinCharge {
    public static final int MIN_TICKS = 8;
    public static final int FULL_TICKS = 30;
    public static final int GOLDEN_END = 40;

    private SpinCharge() {}

    public static float progress(int ticks) {
        return Math.max(0, Math.min(1, ticks / (float) FULL_TICKS));
    }

    public static boolean isGolden(int ticks) {
        return ticks >= FULL_TICKS && ticks <= GOLDEN_END;
    }

    public static float damage(float charge, boolean golden) {
        return 4 + 4 * Math.max(0, Math.min(1, charge)) + (golden ? 2 : 0);
    }
}
