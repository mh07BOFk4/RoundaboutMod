package net.hydra.jojomod.fates.powers;

/** Rules shared by server gameplay, replicated UI, and dependency-free tests. */
public final class SpinProgression {
    public static final int MAX_EXPERIENCE = 120;
    public static final int MAX_ROTATION = 100;
    public static final int HORSE_TICKS = 200;
    private SpinProgression() {}

    public static int experience(int value) { return Math.max(0, Math.min(MAX_EXPERIENCE, value)); }
    public static int rotation(int value) { return Math.max(0, Math.min(MAX_ROTATION, value)); }
    public static int rank(int experience) {
        return experience >= 120 ? 4 : experience >= 60 ? 3 : experience >= 20 ? 2 : 1;
    }
    public static int requiredExperience(int rank) {
        return rank >= 4 ? 120 : rank == 3 ? 60 : rank == 2 ? 20 : 0;
    }
    public static int horseCharge(int current, boolean movingHorse, int rank) {
        if (rank < 4) return 0;
        return Math.max(0, Math.min(HORSE_TICKS, current + (movingHorse ? 1 : -4)));
    }
    public static int cost(int slot) { return slot == 2 ? 20 : slot == 3 ? 15 : slot == 4 ? 100 : 0; }
    public static int cooldown(int slot) { return slot == 1 ? 16 : slot == 2 ? 60 : slot == 3 ? 80 : 400; }
    public static boolean canUse(int slot, int experience, int rotation) {
        return slot >= 1 && slot <= 4 && rank(experience) >= slot && rotation >= cost(slot);
    }
}
