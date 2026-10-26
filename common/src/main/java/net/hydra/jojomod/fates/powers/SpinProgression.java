package net.hydra.jojomod.fates.powers;

/** Rules shared by server gameplay, replicated UI, and dependency-free tests. */
public final class SpinProgression {
    public static final int MAX_EXPERIENCE = 120;
    private SpinProgression() {}

    public static int experience(int value) { return Math.max(0, Math.min(MAX_EXPERIENCE, value)); }
    public static int rank(int experience) {
        return experience >= 120 ? 4 : experience >= 60 ? 3 : experience >= 20 ? 2 : 1;
    }
    public static int requiredExperience(int rank) {
        return rank >= 4 ? 120 : rank == 3 ? 60 : rank == 2 ? 20 : 0;
    }
    public static boolean canUse(int slot) { return slot >= 1 && slot <= 4; }
    public static int cooldown(int slot) { return slot == 1 ? 10 : slot == 2 ? 8 : slot == 3 ? 80 : 60; }
    public static int redirects(int rank) { return rank >= 2 ? 2 : 1; }
    public static int guardDuration(int rank) { return rank >= 3 ? 40 : 30; }
    public static boolean perfectGuard(int remaining, int rank) {
        return remaining > guardDuration(rank) - 8 && remaining <= guardDuration(rank);
    }
    public static float guardReduction(int remaining, int rank, boolean physical) {
        if (remaining <= 0 || !physical) return 0;
        return perfectGuard(remaining, rank) ? 0.75F : 0.4F;
    }
    public static boolean infiniteAvailable(int rank, boolean movingHorse) { return rank >= 4 && movingHorse; }
    public static int practice(boolean golden, boolean trickShot) { return (golden ? 6 : 4) + (trickShot ? 2 : 0); }
}
