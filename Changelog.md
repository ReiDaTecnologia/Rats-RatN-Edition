## This Changelog Follows the Semantic Versions system

### 3.2.20
- Hopefully Fixed Memory Leak on Rat Hats models ([#1](https://github.com/Rebirth-of-the-Night/Rats-RatN-Edition/issues/1))
- Updated requirements string format

### 3.2.19
- Added Config option to turn off plague doctor trasnformation when struck by a lightning

### 3.2.18
- Plague effect can't infect enemies through shields anymore
- Added some subtitles to rat sounds

### 3.2.17
- You can now configure which plague stage the player will get after they respawn
    - New available options are:
        - Assign the same plague level the player had before dying (old behaviour) {default}
        - No plague effect reassigned at all
        - Always assign a certain level of plague effect

### 3.2.16
- Fixed a logic flaw that caused the plague effect to finish on Stage 3 if the player died during it
- Max health debuff now defaults to 4 health points (2 hearts for each death)

### 3.2.15
- The Plague effect is now divided into stages, from I to IV.
- Stage 1: Healing rate and effectiveness reduced by 50%
- Stage 2: All forms of HP regen blocked
- Stage 3: Player occasionally takes damage over time
- Stage 4: Consistent damage until death. Plague effect ends. Player maximum HP is reduced by X% until <y item> is consumed.
- Configurability for max health restore item, plague effect duration, stage duration, max health debuff amount, stage 1 healing multiplier, stage 3 damage and frequency, and stage 4 damage.