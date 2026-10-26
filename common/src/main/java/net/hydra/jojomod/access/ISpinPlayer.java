package net.hydra.jojomod.access;

/** Server-owned values replicated through the player's entity data. */
public interface ISpinPlayer {
    int rdbt$getSpinExperience();
    void rdbt$setSpinExperience(int value);
    int rdbt$getSpinRotation();
    void rdbt$setSpinRotation(int value);
    int rdbt$getSpinHorseCharge();
    void rdbt$setSpinHorseCharge(int value);
    int rdbt$getSpinEmpoweredTicks();
    void rdbt$setSpinEmpoweredTicks(int value);
    boolean rdbt$isInfiniteSpin();
    void rdbt$setInfiniteSpin(boolean value);
}
