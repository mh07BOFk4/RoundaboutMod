import net.hydra.jojomod.item.SpinCharge;

/** Dependency-free regression tests: run tests/spin/run.sh. */
public class SpinChargeTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check(SpinCharge.progress(-1) == 0, "Negative charge clamps to zero");
        check(SpinCharge.progress(0) == 0, "Initial charge");
        check(SpinCharge.progress(15) == 0.5F, "Half charge");
        check(SpinCharge.progress(30) == 1, "Full charge");
        check(SpinCharge.progress(72000) == 1, "Holding never exceeds full charge");
        check(!SpinCharge.isGolden(29), "Early release is not golden");
        check(SpinCharge.isGolden(30), "Golden window opens on the chime");
        check(SpinCharge.isGolden(40), "Last golden tick is included");
        check(!SpinCharge.isGolden(41), "Overholding loses the timing bonus");
        check(SpinCharge.damage(0, false) == 4, "Minimum damage");
        check(SpinCharge.damage(1, false) == 8, "Full ordinary throw");
        check(SpinCharge.damage(1, true) == 10, "Golden bonus");
        check(SpinCharge.damage(10, true) == 10, "Malformed charge cannot increase damage");
        check(SpinCharge.damage(-10, false) == 4, "Malformed charge cannot make damage negative");
        System.out.println(checks + " Spin timing and damage checks passed");
    }
}
