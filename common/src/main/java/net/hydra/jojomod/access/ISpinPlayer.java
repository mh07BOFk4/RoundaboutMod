package net.hydra.jojomod.access;

/** Server-owned values replicated through the player's entity data. */
public interface ISpinPlayer {
    int rdbt$getSpinExperience();
    void rdbt$setSpinExperience(int value);
    int rdbt$getSpinGuardTicks();
    void rdbt$setSpinGuardTicks(int value);
    int rdbt$getSpinCounterTicks();
    void rdbt$setSpinCounterTicks(int value);
}
