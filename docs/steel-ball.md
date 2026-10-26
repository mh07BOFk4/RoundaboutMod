# Spin Fate: active steel-ball combat

All four base techniques work as soon as you learn Spin. There is no Rotation
meter, passive resource regeneration, resource cost, or horse-charge timer.
Short cooldowns and recovering your two steel balls set the combat rhythm.
These mechanics are gameplay adaptations of Gyro's Spin, muscle manipulation,
medical applications, trajectory control, and the horse's Golden Rectangle.

## Starting and controls

Craft a Spin Manual from a book, a gold ingot, and an iron nugget (shapeless).
Use it as a human to learn Spin. It does not overwrite another non-human Fate.
Sneak-use the manual to become human again without losing practice.
Use it normally as a Spin user to print your rank and practice.

Craft a Steel Ball from four iron ingots arranged above/below/left/right of an
iron nugget. Two balls fit in a stack. Hold them in either hand for throwing.
Both items are in the mod creative tab.

Press **Y** (Power Inventory) and select the Fate/book tab for the technique
menu. Default Skill 1–4 keys are **Z, X, C, V**; remap them under Minecraft
Controls → Roundabout Controls. Desummon your Stand to use Fate techniques.

| Input | Technique | Cooldown |
| --- | --- | --- |
| Z | Instantly throw a fully spinning Steel Ball | 0.5s |
| Sneak + Z | Muscle Spin: a precision throw that affects the muscles you hit | 0.5s |
| X | Aim at a new point and redirect an airborne ball toward it | 0.4s |
| Sneak + X | Recall your airborne balls; return flight cannot deal damage | 0.4s, separate from steering |
| C | Body Spin: harden your muscles and time a defensive counter | 4s |
| Sneak + C | First Aid Spin: heal and restore blood flow | 8s, shared with hardening |
| V | Instant Golden Rectangle shot | 3s; 8s for mastered mounted shots |
| Sneak + V | Opposite Rotation: cancel Lingering Rotation on yourself | 4s, separate from shooting |

Failed actions consume no item and start no cooldown. Steering/recalling,
first aid, and reversing rotation do not require a ball in your hand.
Throwing still requires one, and consumes one until it returns.

## Combos and target feedback

- **Z → aim → X:** curve your existing ball onto a new trajectory. It still
  collides with terrain; this is steering, not teleportation or wall penetration.
- **Bank shot:** bounce a ball off terrain into a target for +2 impact damage.
- **Sneak + Z:** aim at the lower 45% of a target's body for Slowness III,
  or its upper body for Weakness II. Both last 3 seconds. Contacts are derived
  from the projectile trajectory, rather than the arrow hit result's default
  target-feet position.
- **C just before impact → Z:** the first 0.4 seconds of hardening reduces
  eligible physical damage by 75% and grants a golden counterthrow for 2 seconds.
  Later hardening reduces it by 40%. The counter is spent only on a successful
  ordinary throw; an already-golden throw preserves it until the timer expires.
- **Sneak + C:** heals one heart, stops the mod's bleeding, and removes
  Slowness/Weakness. At rank 3 it heals two hearts. Full-health, unaffected
  players cannot waste its cooldown. It does not remove Lingering Rotation.
- **Sneak + V while affected:** reverse the lingering rotation wound and remove
  its slowing effect. This does not remove Tusk's separate Infinite Spin effect.

Hardening does not reduce fall, fire, poison, magic, armor-bypassing damage,
or damage that bypasses invulnerability. Normal damage permissions remain in
force; denied damage cannot apply debuffs or award practice.

## Mastery improves the moves

The existing practice save is preserved. Standard hits award 4 practice;
golden hits award 6. Bank shots and redirected hits add 2. Perfect physical
blocks award 2. Misses, denied damage, and armor stands do not award practice.

| Practice | Upgrade |
| --- | --- |
| 0 | All base techniques and one redirect per ball |
| 20 | Two redirects per ball |
| 60 | One extra ricochet, 2-second hardening, two-heart first aid |
| 120 | Natural horse movement enables Infinite Rotation shots |

At rank 4, ride a **moving, tamed, saddled horse** as its controlling rider,
with the horse grounded and not currently hurt. The HUD shows when the Golden
Rectangle is aligned. Press V to fire a mastered shot immediately. No meter
or ten-second riding timer is required. A stationary, untamed, unsaddled,
airborne, or hurt horse does not qualify; V fires an ordinary golden shot.

A mastered mounted shot deals 12 initial damage, inflicts Slowness II for
3 seconds, and transmits **Lingering Rotation** for 3.5 seconds. That wound
adds small periodic magical damage and a low golden spiral around the target.
It has a fixed duration and no exponential scaling. Opposite Rotation or milk
can clear it. Existing Tusk mechanics remain separate.

## Manual throwing, effects, and recovery

Right-click charging is optional: hold Use for at least 0.4 seconds and release
to throw. Charge raises damage/speed. At 1.5 seconds, a chime and short text
prompt open a half-second golden-release window. Z and V throw instantly.

Ordinary full-charge impacts deal 8 damage, golden impacts deal 10, and a bank
shot adds 2. Mounted mastered shots have a fixed 12-damage impact before their
lingering effect. Defensive golden counters and timed releases share the normal
golden damage and ricochet rules.

Each ball can be redirected once (twice from rank 2). Steering extends its
outbound time within an absolute 3.6-second cap; it never resets its total
lifetime. Default outbound time is 1.6 seconds. Balls return after a target hit,
exhausting their ricochets, recall, or the outbound timer. Return flight phases
through terrain and cannot hit entities. If an owner is dead or cannot be
reached within 10 seconds of active entity ticking, the item drops. Unloaded
chunks pause entity ticking. A full inventory also results in a drop; creative
throws cannot duplicate items. Save/reload preserves flight/steering/recovery state.

The aiming-point charge particles remain removed. Feedback comes from the
rotating ball, double spiral trails, impact sparks, low wound spirals, timing
text, original synthesized throw/impact/catch sounds, steering audio, and
perfect-defense/mounted-shot chimes.

Training persists on reconnect and respawn. Temporary defense/counter windows
reset on reconnect, respawn, and changing Fate. The existing lose-Fate-on-death
rule and respawn-as-human option still apply; learned training remains available
when choosing Spin again.

## Operator commands and validation

- `/roundaboutSetFate @s spin 1 0`: choose Spin at rank 1.
- `/roundaboutMaxFateSkills @s`: grant mastery upgrades.
- `/roundaboutClearFateSkills @s`: reset practice and temporary defense/counter windows.
- Existing Fate level-up/down commands adjust Spin rank.

Run `bash tests/spin/run.sh` with a JDK. It covers 79 timing, damage, progression,
defense-window, mounted-mastery, ownership, steering, contact-height, and lifetime
checks. Build Forge with Java 17 using
`bash gradlew --no-daemon --configure-on-demand --max-workers=2 :forge:jar`;
include the required `reobfJar` step. Check final ZIP integrity, class/resource
presence, and that the SteelBallItem class contains no charge-particle calls.

In-game acceptance checks remain required:

- Start at zero practice; exercise all eight inputs, both hands, cooldowns, and
  the Fate menu. Confirm no Rotation meter or passive waiting requirement.
- Steer before and after a bank shot; check rank-1/rank-2 budgets and that
  another player's balls and harmless return flight cannot be redirected.
- Hit legs/upper body, shielded targets, PvP-disabled players, and mobs. Check
  bonuses, permissions, practice awards, and lack of repeated return damage.
- Time a body-spin defense; compare early/late/environmental hits, consume a
  golden counter, miss its window, and test first aid and opposite rotation.
- Test mounted shots with mastery and each valid/invalid horse condition;
  confirm the lingering effect expires without increasing its damage.
- Test full-inventory recovery, creative mode, death/disconnect, and save/reload
  while a ball is airborne or returning. Count items for duplication/loss.
- On a dedicated server, observe trajectories, effects, cooldowns, and sounds
  from a second player. Verify no charge particles obstruct aiming.

Forge compilation, rule tests, and package checks are verified separately from
in-game behavior. Fabric registration is maintained; its build and gameplay
checks remain unverified.
