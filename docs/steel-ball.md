# Steel Ball

Craft one ball using four iron ingots in a diamond around an iron nugget.
It is also available in the mod creative tab as `roundabout:steel_ball`.
Two balls fit in a stack, allowing alternating throws.

Hold Use for at least 0.4 seconds, then release to throw. Charging increases
speed and damage. At 1.5 seconds, a chime and action-bar prompt open a
half-second Golden Spin release window. Holding longer retains full ordinary
charge but loses the timing bonus. This is a timing technique, separate from
Tusk's Infinite Spin and horse-based progression.

Ordinary throws deal 4–8 damage and ricochet once; Golden Spin deals 10 damage
and ricochets twice. A hit slows the target briefly (1.5 seconds, or 3 seconds
for Golden Spin). Damage and status effects are decided on the server.

Balls return after a hit, exhausting their ricochets, or 1.2 seconds of flight.
Returning balls phase through terrain and cannot hit entities. Survival throws
consume one item until it returns. A full inventory causes a recoverable drop;
creative throws never duplicate items. If the owner dies or cannot be reached
within six seconds of active entity ticking, the ball drops at its location.
Unloaded chunks pause this timer. Flight state and the carried item are saved.

Effects include a rotating green ball, double spiral trails, golden impact
bursts, and a charge ring. Five original synthesized mono Vorbis sounds cover
charge, golden readiness, throw, impact, and catch, with English subtitles.
No anime audio samples are used.

## Validation

Run `bash tests/spin/run.sh` with a JDK for timing/damage boundary tests.
Run `bash gradlew :fabric:compileJava :forge:compileJava` with Java 17 for both
platforms. The Forge build passed with Java 17 using
`bash gradlew --no-daemon --configure-on-demand --max-workers=2 :forge:jar`,
including the required `reobfJar` step. All 14 timing/damage checks passed.
The final JAR passed ZIP integrity and steel-ball class/resource checks.
The Sponge downloader's direct-DNS issue was resolved in the cloud machine by
prefetching official Minecraft manifests, binaries, and mappings through the
session proxy into its cache, verifying the published artifact hashes.
Fabric compilation and in-game acceptance checks remain unverified.

In-game acceptance checks (still required on both platforms):

- Craft a ball; check item art, tooltip, and creative-tab entry.
- Cancel before 0.4 seconds: no item loss, throw sound, or projectile.
- Release before, during, and after the golden chime; compare damage on
  an unarmored target and ensure the bonus expires after the window.
- Hit a wall, floor, and ceiling; verify ricochets, then safe return.
- Hit a mob, a shield, and a player with PvP disabled; confirm normal
  Minecraft damage permissions, one hit per throw, and no return-flight damage.
- Throw two balls; catch with space and with a full inventory. Count items.
- Repeat in creative, after death/disconnect, and after saving/reloading
  mid-flight; verify no extra items are created.
- Have a second player observe trails and sounds; toggle subtitles and
  reduced particles, and verify the client works against a dedicated server.
- Confirm existing iron-ball crossbow shots retain their previous behavior.
