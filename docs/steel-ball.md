# Steel Ball and Spin Fate

## Choosing Spin

Craft a **Spin Manual** with a book, a gold ingot, and an iron nugget
(shapeless). Use it while human to choose the Spin Fate. It does not consume the
manual or replace another non-human Fate. Use it again to print your training
status, or sneak-use it to return to human while retaining earned training.
The manual is also in the mod creative tab as `roundabout:spin_manual`.

Open the existing Roundabout power inventory and select the **Fate** tab
(book icon). It shows the Spin training path with locked/unlocked techniques.
Use your remappable **Skill 1–4** keys from Minecraft Controls → Roundabout.
Desummon your Stand before using Fate skills, as with the other Fates.

| Skill | Unlock | Technique | Rotation cost | Cooldown |
| --- | --- | --- | --- | --- |
| 1 | Rank 1, 0 practice | Spin Shot: instant 80%-charged throw | 0 | 0.8s |
| 2 | Rank 2, 20 practice | Disrupting Spin: full shot, Slowness II + Weakness for 4s | 20 | 3s |
| 3 | Rank 3, 60 practice | Spin Step: forward dash while grounded and unmounted | 15 | 4s |
| 4 | Rank 4, 120 practice | Golden Rotation: golden throws for 6s | 100 | 20s |

Skills 1 and 2 require a Steel Ball in either hand and consume one on a
successful throw until it returns. Failed skill use consumes no ball or
Rotation. Successful hits on mobs/players award 4 practice, or 6 for golden
hits, and restore 12 Rotation. Holding a Steel Ball restores 1 Rotation per
second outside empowerment. Misses, invulnerable targets, and armor stands
do not award training. Rotation caps at 100.

Training is saved, replicated to the client, and retained on respawn.
Temporary Rotation, horse rhythm, and empowerment reset on rejoining,
respawning, or switching Fates. The existing lose-Fate-on-death gamerule and
respawn-as-human choice still apply; learned training remains available if
you choose Spin again.

## Horse mastery and Infinite Rotation

At rank 4, ride a **tamed, saddled horse** as its controlling rider. Keep
moving on the ground for 10 seconds to build horse rhythm to 100%. Stopping,
dismounting, jumping, or the horse being hurt decays rhythm; lower ranks
cannot build it.

With full rhythm and 100 Rotation, activate **Skill 4 while still riding
and moving**. It activates Infinite Rotation instead of ordinary Golden
Rotation. For 10 seconds, your steel-ball throws deal 14 damage and inflict
Slowness III and Weakness for 4 seconds. Rhythm and Rotation are spent;
the 20-second cooldown remains. This is a bounded combat ability, not an
unlimited-damage or permanent-debuff effect.

Operator commands support Spin:

- `/roundaboutSetFate @s spin 1 0`: select Spin at rank 1.
- `/roundaboutMaxFateSkills @s`: unlock all four techniques.
- `/roundaboutClearFateSkills @s`: clear training and temporary empowerment.
- Existing Fate level-up/down commands also adjust Spin rank.

## Throwing manually

Craft one ball using four iron ingots in a diamond around an iron nugget.
It is also available in the mod creative tab as `roundabout:steel_ball`.
Two balls fit in a stack, allowing alternating throws.

Hold Use for at least 0.4 seconds, then release to throw. Charging increases
speed and damage. At 1.5 seconds, a chime and action-bar prompt open a
half-second Golden Spin release window. Holding longer retains full ordinary
charge but loses the timing bonus. During Golden/Infinite Rotation,
throws automatically receive the golden bonus. Manual throws remain available
without choosing a Fate.

Ordinary throws deal 4–8 damage and ricochet once; Golden Spin deals 10 damage
and ricochets twice. A hit slows the target briefly (1.5 seconds, or 3 seconds
for Golden Spin). Damage and status effects are decided on the server.

Balls return after a hit, exhausting their ricochets, or 1.2 seconds of flight.
Returning balls phase through terrain and cannot hit entities. Survival throws
consume one item until it returns. A full inventory causes a recoverable drop;
creative throws never duplicate items. If the owner dies or cannot be reached
within six seconds of active entity ticking, the ball drops at its location.
Unloaded chunks pause this timer. Flight state and the carried item are saved.

Effects include a rotating green ball, double spiral trails, and golden impact
bursts. **The charge ring and all charge particles near the aiming point have
been removed**; charge timing uses sound and a brief action-bar message.
Five original synthesized mono Vorbis sounds cover
charge, golden readiness, throw, impact, and catch, with English subtitles.
No anime audio samples are used.

## Validation

Run `bash tests/spin/run.sh` with a JDK for timing/damage boundary tests.
Run `bash gradlew :fabric:compileJava :forge:compileJava` with Java 17 for both
platforms. The Forge build passed with Java 17 using
`bash gradlew --no-daemon --configure-on-demand --max-workers=2 :forge:jar`,
including the required `reobfJar` step. All 47 timing/damage/progression/resource/
horse-rhythm checks passed.
The final JAR passed ZIP integrity and steel-ball class/resource checks.
The Sponge downloader's direct-DNS issue was resolved in the cloud machine by
prefetching official Minecraft manifests, binaries, and mappings through the
session proxy into its cache, verifying the published artifact hashes.
Fabric compilation and in-game acceptance checks remain unverified.

In-game acceptance checks (still required on both platforms):

- Learn Spin with the manual as human; ensure another Fate is not overwritten.
- Verify Skill 2–4 stay locked below their practice thresholds and unlock from
  successful combat hits. Check both hands, costs, cooldown overlays, and the Fate menu.
- Reconnect and respawn; verify training persists and temporary resources reset.
- Verify skill packets cannot bypass locks/costs/cooldowns or act while
  sleeping, dazed, time-stopped, spectating, using an item, or a Stand is active.
- At mastery, verify a stationary/untamed/unsaddled horse cannot charge rhythm.
  Ride for 10 seconds; activate Skill 4 with 100 Rotation and check empowerment
  expires, resources are spent, and returning balls cannot damage anything.
- Confirm there are no charge particles at or near the crosshair.
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
