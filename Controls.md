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

## Containers, other menus

nothing here yet

## Implementation notes

The problem is that target devices don't have the mouse or joystick.

Many variants of interaction controls were suggested, such as cursor moving on grid while game is paused, like in strategy game. With option to disable pausing to more dynamic play, popup context actions, etc etc.

But I chose the simplest one to avoid writing tons of code: pressing one key to interact with object player is currently standing close and facing to. It isn't most convenient thing to do on dpad, but it does work.

No attack mode switching, attack target will be set as player hits someone and will be unset when target is defeated or too far away.

Also, unlike in other j2me games, 2 4 5 6 8 numbers don't duplicate d-pad, because I thought it would be more convenient to jump to inventory slots, rather than scrolling through each.

Some inspiration on controls were taken from console/mobile port.
