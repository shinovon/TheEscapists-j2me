# Game controls

Keycodes layout is Nokia/SE.
<br>
Touch controls support is not planned, at least for now.



## Ingame

### Left soft key:
Drop selected item / Pickup item / Interact with object / Interact with nearby NPC
<br>
(ordered by priority starting from highest)

### Middle soft key/Fire:
Use selected item / Drop carrying NPC / Attack nearby NPC
<br>
(ordered by priority starting from highest)

### Right soft key:
Exit or pause

### Numpad:
1-6 correspond to inventory slots
<br>
7-9 currently do debug things or cheats
<br>
star, 0, and pound are reserved
<br>
(maybe *: journal, 0: crafting, #: profile)



## Ingame (Alternative controls enabled)
In this mode, game keys are respected. Soft keys are the same as above.

GAME_A (1) and GAME_B (3) scroll through inventory slots.



## Containers

### Left soft key:
Switch between inventory and container

### Middle soft key/Fire:
Move item

### Right soft key:
Exit

## Training
Smash 1 and 3, or A and B in alt. controls mode.



## Implementation notes

The problem is that target devices don't have the mouse or joystick.

Many variants of interaction controls were suggested, such as cursor moving on grid while game is paused, like in strategy game. With option to disable pausing to more dynamic play, popup context actions, etc etc.

But I chose the simplest one to avoid writing tons of code: pressing one key to interact with object player is currently standing close and facing to. It isn't most convenient thing to do on dpad, but it does work.

No attack mode switching, attack target will be set as player hits someone and will be unset when target is defeated or too far away.

Alternative controls mode is introduced for devices that do not have full alphanumeric keypad. Such as Symbian^3 with virtual keyboard, PSPKVM.

Some inspiration on controls were taken from console/mobile port.
