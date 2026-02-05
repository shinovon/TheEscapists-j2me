/*
Copyright (c) 2024-2026 Arman Jussupgaliyev
*/
import java.util.Random;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.lcdui.game.Sprite;

class NPC implements Constants {
	
	static final int ANIM_REGULAR = 0; // walking
	static final int ANIM_PUNCH = 1; // punching
	static final int ANIM_FOOD = 2; // holding tray
	static final int ANIM_STUNNED = 3; // unconscious
	static final int ANIM_TIED = 4; // tied
	static final int ANIM_LYING = 5; // on bed
	static final int ANIM_WEIGHT = 6; // training weight
	
	static final int DIR_RIGHT = 0;
	static final int DIR_UP = 1;
	static final int DIR_LEFT = 2;
	static final int DIR_DOWN = 3;

	static final int AI_RESET = -1;
	static final int AI_IDLE = 0;
	static final int AI_CHECK = 1;
	static final int AI_ROAM = 2;
	static final int AI_ATTACK = 3;
	static final int AI_WAYPOINT = 4;
	static final int AI_SLEEP = 5;
	static final int AI_MEAL = 6;
	static final int AI_GYM = 7;
	static final int AI_WORK = 8;

	static final int ACT_NONE = 0;
	static final int ACT_READING = 1;
	static final int ACT_CLEANING = 2;
	static final int ACT_SEARCHING = 3;
	static final int ACT_CHIPPING = 4;
	static final int ACT_DIGGING = 5;

	static Random rng = new Random();

	static int inmateRoamPos = rng.nextInt(10);
	static int guardRoamPos = rng.nextInt(10);
	static int rollcallPos = rng.nextInt(10);
	static int canteenServingPos;
	static int canteenSeatPos;
	static int showerPos = rng.nextInt(10);
	static int gymPos;
	
	private Sprite body;
	private Sprite outfit;
	
	Game map;
	int id;
	int typedId;
	boolean visible; // is visible on screen right now
	boolean hidden;
	
	int animation,
		direction;

	int bodyId,
		outfitId;

	int prevAnimation,
		prevDirection,
		prevBodyId,
		prevOutfitId;
	
	int[] animationSequence;
	
	int x, y;
	int prevX, prevY;
	int layer = LAYER_GROUND;
	
	int state;
	int animationTimer;

	int animateToX = -1, animateToY = -1;
	
	// items
	int[] inventory = new int[6];
	int outfitItem, weapon;
	
	String name;
	
	// stats
	int statStrength = 30, statSpeed = 30, statIntellect = 30, statRespect = 30;

	int health = 15;
	
	// ai
	int aiState = AI_RESET;
	NPC chaseTarget;
	boolean ai;
	boolean nextRoamPos = true;
	int aiWaitTimer = 0;
	int heatTimer = 0;
	int targetOffsetX, targetOffsetY;
	int bedX, bedY;
	boolean sitting;
	boolean training;
	int attackTimer;
	int wakeupTimer = -1;
	int angerTimer;
	int trainingFrame = 0;
	int aiWorkState;
	int animationFrame;

	int fighting;

	// path
	int pathX, pathY;
	short[] path = new short[400];
	boolean correctPath;
	int pathStep;
	int pathEndDir = -1;
	boolean targetReached;
	
	String dialog;
	int dialogTimer, nextDialogTimer;

	boolean guard, inmate, other;

	boolean carried;
	
	// collision
	boolean canClimb, climb, climbed;
	boolean doorSound;
	boolean wasTryingToMove;
	int lastDoor;
	int lastShower;

	float xFloat = -1, yFloat = -1;

	boolean inShower;

	int job;
	int jobQuota;

	int gymObject;
	int gymObjectIdx = -1;

	NPC(Game map) {
		animationSequence = new int[5];
		prevAnimation = -1;
		this.map = map;
		this.ai = true;
	}
	
	void load(int bodyId, int outfitId) {
		this.bodyId = prevBodyId = bodyId;
		this.outfitId = prevOutfitId = outfitId;
		guard = bodyId == Textures.GUARD;
		inmate = bodyId < Textures.GUARD;
		other = !guard && !inmate;
		for (int i = 0; i < 6; ++i) {
			inventory[i] = Items.ITEM_NULL;
		}
		outfitItem = (guard ? Items.GUARD_OUTFIT : Items.INMATE_OUTFIT) | Items.ITEM_DEFAULT_DURABILITY;
		weapon = Items.ITEM_NULL;

		(this.body = new Sprite(Game.sprites[bodyId], 16, 16))
		.setRefPixelPosition(8, 8);

		if (outfitId != -1) {
			(this.outfit = new Sprite(Game.sprites[outfitId], 16, 16))
			.setRefPixelPosition(8, 8);
		}
	}
	
	void paint(Graphics g, int x, int y) {
		if (hidden)
			return;
		if (bodyId == Textures.SNIPER && (map.time < 8 * 60 || map.time > 22 * 60))
			return;
		int frame = inmate && ai ? animationFrame : Game.animationFrame & 1;
		
		if (animation != prevAnimation || direction != prevDirection) {
			int[] animationSequence = this.animationSequence;
			switch (animation) {
			case ANIM_REGULAR:
			case ANIM_FOOD:
				int i;
				animationSequence[0] = i = animation * 8 + direction * 2;
				animationSequence[1] = i + 1;
				break;
			case ANIM_PUNCH:
				animationSequence[0] = animationSequence[1] = 8 + direction;
				break;
			case ANIM_STUNNED:
				animationSequence[0] = animationSequence[1] = 12;
				break;
			case ANIM_TIED:
				animationSequence[0] = 13;
				animationSequence[1] = 14;
				break;
			case ANIM_LYING:
				animationSequence[0] = animationSequence[1] = 15;
				break;
			case ANIM_WEIGHT:
				animationSequence[0] = 28;
				animationSequence[1] = 27;
				animationSequence[2] = 26;
				animationSequence[3] = 27;
				animationSequence[4] = 28;
				frame = ai ? trainingFrame >> 2 : (map.trainingTimer * 3) / 40;
				break;
			default:
				animationSequence[0] = direction * 2;
				animationSequence[1] = direction * 2 + 1;
				break;
			}
			body.setFrameSequence(animationSequence);
			if (outfit != null) outfit.setFrameSequence(animationSequence);
		}

		// body sprite won't change
//		if (bodyId != prevBodyId) {
//			outfit.setImage(Sprites.spritesheets[bodyId], 16, 16);
//			outfit.setFrameSequence(animationSequence);
//			outfit.setRefPixelPosition(8, 8);
//			prevOutfitId = outfitId;
//			prevBodyId = bodyId;
//		}
		
		body.setPosition(x, y);
		body.setFrame(frame);
		body.paint(g);
		
		if (outfitId != -1 && !inShower) {
			Sprite outfit = this.outfit;
			if (outfit == null) {
				outfit = this.outfit = new Sprite(Game.sprites[outfitId], 16, 16);
				prevOutfitId = -1;
			}
			if (prevOutfitId != outfitId) {
				outfit.setImage(Game.sprites[outfitId], 16, 16);
				outfit.setFrameSequence(animationSequence);
				outfit.setRefPixelPosition(8, 8);
				prevOutfitId = outfitId;
			}
			
			outfit.setPosition(x, y);
			outfit.setFrame(frame);
			outfit.paint(g);
		}
		
		if (carry != null) {
			carry.paint(g, x, y - 8);
		}
	}
	
	boolean isInZone(int zone) {
		return map.isInZone((int) x + 7, (int) y + 7, zone);
	}

	void tick(int tick) {
		if (animationTimer != 0 && --animationTimer == 0) {
			animation = state;
		}
		if (dialogTimer != 0 && --dialogTimer == 0) {
			dialog = null;
		}
		if (attackTimer != 0) {
			attackTimer--;
		}
		if (x != prevX || y != prevY) {
			// moved
			prevX = x;
			prevY = y;
			byte s1 = getCollision(0, 5, false);
			byte s2 = getCollision(15, 5, false);
			byte s3 = getCollision(0, 15, false);
			byte s4 = getCollision(15, 15, false);
			if (climbed
					&& s1 != COLL_TABLE && s1 != COLL_DESK
					&& s2 != COLL_TABLE && s2 != COLL_DESK
					&& s3 != COLL_TABLE && s3 != COLL_DESK
					&& s4 != COLL_TABLE && s4 != COLL_DESK) {
				// got down from table
				climbed = false;
			}

			if (s1 == COLL_DOOR || s2 == COLL_DOOR
					|| s3 == COLL_DOOR || s4 == COLL_DOOR
					|| s1 == COLL_DOOR_STAFF || s2 == COLL_DOOR_STAFF
					|| s3 == COLL_DOOR_STAFF || s4 == COLL_DOOR_STAFF) {
				// colliding with doors, play sound
				if (!doorSound && visible) {
					Sound.playEffect(Sound.SFX_DOOR);
				}
				doorSound = true;
			} else {
				doorSound = false;
				if (lastDoor != -1) {
					// uncheck hide flag
					map.objects[layer][lastDoor + 2] &= ~(1 << 12);
					lastDoor = -1;
				}
			}

			if (inmate) {
				if (!(s1 == COLL_SHOWER || s2 == COLL_SHOWER
						|| s3 == COLL_SHOWER || s4 == COLL_SHOWER)) {
					// hide shower when not colliding
					if (lastShower != -1) {
						map.objects[layer][lastShower + 2] |= 1 << 12;
						lastShower = -1;
					}
				}

				// don't render outfit when in shower room
				inShower = isInZone(ZONE_SHOWER);
			}
		}
		if (inmate) {
			if (animation == ANIM_FOOD &&
					((map.schedule != SC_EVENING_MEAL && map.schedule != SC_BREAKFAST)
							|| !isInZone(ZONE_CANTEEN) || climbed)) {
				// remove tray when left from canteen
				animation = ANIM_REGULAR;
			}
			if (map.schedule == SC_WORK_PERIOD && map.prevSchedule != SC_WORK_PERIOD) {
				jobQuota = 0;
			} else if (ai && map.prevSchedule == SC_WORK_PERIOD && map.schedule != SC_WORK_PERIOD && jobQuota < NPC_JOB_QUOTA_THRESHOLD) {
				map.jobs[job] &= ~JOB_OCCUPIED_BIT;
				job = 0;
			}
		}

		if (chaseTarget != null && health > 0) {
			// fight
			attack: {
				if (animationTimer != 0)
					break attack;

				int tx = chaseTarget.x, ty = chaseTarget.y;
				int dx = tx - x, dy = ty - y;
				if (!ai && (dx * dx + dy * dy > NPC_CHASE_LOSE_DISTANCE || chaseTarget.health <= 0)) {
					chaseTarget = null;
					break attack;
				}
				if (dx * dx + dy * dy > TILE_SIZE * TILE_SIZE + 10)
					break attack;

				correctPath = false;
				moveTowards(tx, ty, 0);
				if (attackTimer != 0)
					break attack;

				attackTimer = 37 - (statSpeed / 5);
				if (!ai) {
					map.action = ACT_NONE;
					map.progress = 0;
					if (map.fatigue >= 100) {
						Sound.playEffect(Sound.SFX_LOSE);
						dialog = "You are too fatigued";
						dialogTimer = TPS * 2;
						break attack;
					}
					map.fatigue += 2;
				}
				animation = ANIM_PUNCH;
				animationTimer = TPS >> 2;

				fighting = 2;

				if (visible) Sound.playEffect(Sound.SFX_ENHIT);

				int damage = statStrength / 14;

				if (weapon != Items.ITEM_NULL) {
					damage += Game.getItemAttack(weapon);
				}

				chaseTarget.damage(this, damage);

				if (chaseTarget.health <= 0) {
					chaseTarget = null;
					if (ai) {
						aiState = AI_RESET;
						correctPath = false;
						aiWaitTimer = TPS;
						attackTimer = 0;
					}
				}
			}
		}

		if (animateToX != -1 && animateToY != -1) {
			// smooth move to position
			moveTowards(animateToX, animateToY, ANIMATE_SPEED);
			if (animateToX == x && animateToY == y) {
				// animation done
				animateToX = -1;
				animateToY = -1;
				if (sitting || training) {
					direction = pathEndDir;
					if (!ai) {
						if (training && map.objects[LAYER_GROUND][gymObjectIdx + 1] == Objects.TRAINING_WEIGHT) {
							animation = ANIM_WEIGHT;
						}
						pathEndDir = -1;
					}
				}
				if (animatingInCabinet) {
					inCabinet = true;
					animatingInCabinet = false;
				}
			}
		}
	}

	void tickAI(int tick) {
		if (!ai) return;
		if (bodyId == Textures.SNIPER
				|| ((bodyId == Textures.EMPLOYMENT_OFFICER || bodyId == Textures.DOCTOR)
				&& targetReached)) {
			if (tick % (TPS * 4) == 0) {
				direction = rng.nextInt(4);
			}
			if (bodyId == Textures.SNIPER) return;

			if (pathX == map.npcSpawnX && pathY == map.npcSpawnY) {
				hidden = true;
			} else {
				// TODO say
			}
		}

		if (inmate && ((tick + typedId) % ANIMATION_TICKS) == 0 && ++animationFrame > 1)
			animationFrame = 0;

		if (fighting != 0) {
			fighting--;
		}
		if (health <= 0) {
			// stunned
			health = 0;
			if (wakeupTimer == -1) {
				if (guard) {
					map.guardsDown++;
					if (++map.guardsDown >= 3 && !map.lockdown) {
						map.startLockdown();
					}
				}
				int x = this.x / TILE_SIZE;
				int y = (this.y + 5) / TILE_SIZE;
				if (map.solid[LAYER_GROUND][x + y * map.width] != 0) {
					for (int n = 0; n < 4; ++n) {
						int ox = Game.PATH_DIR_POSITIONS[(3 - n) << 1];
						int oy = Game.PATH_DIR_POSITIONS[((3 - n) << 1) + 1];
						if (map.solid[LAYER_GROUND][x + ox + (y + oy) * map.width] == 0) {
							x += ox;
							y += oy;
							break;
						}
					}
				}
				xFloat = this.x = x * TILE_SIZE;
				yFloat = this.y = y * TILE_SIZE;
				wakeupTimer = TPS * 20;
				angerTimer = TPS * 10 + rng.nextInt(TPS * 50);
			} else if (layer == LAYER_GROUND && --wakeupTimer == 0) {
				if (guard) map.guardsDown--;
				wakeupTimer = -1;
				animation = ANIM_REGULAR;
				chaseTarget = null;
				aiState = AI_RESET;
				aiWaitTimer = TPS;
				health = statStrength >> 1;
				carried = false;
				if (map.player.carry == this) {
					map.player.carry = null;
				}
				return;
			}
			animation = ANIM_STUNNED;
			return;
		}
		if (layer != LAYER_GROUND || carried) {
			// can't move
			animation = ANIM_TIED;
			return;
		}

		if (angerTimer != 0) {
			angerTimer--;
		}
		if (animationTimer != 0) {
			return;
		}

		if (inmate) {
			say: {
				int p, delay;
				if (aiState == AI_ROAM) {
					p = 20;
					delay = 5;
				} else if (aiState == AI_MEAL && sitting) {
					p = 80;
					delay = 10;
				} else if (aiState == AI_GYM && training) {
					p = 50;
					delay = 7;
				} else if (aiState == AI_WAYPOINT && targetReached) {
					if (map.schedule != SC_SHOWER_BLOCK) {
						break say;
					}
					p = 50;
					delay = 7;
				} else {
					break say;
				}
				if (dialogTimer == 0 && (nextDialogTimer == 0 || --nextDialogTimer == 0)) {
					if (rng.nextInt(p) != 0) {
						nextDialogTimer = TPS >> 1;
					} else {
						dialog = "Random text";
						dialogTimer = (TPS * 2);
						nextDialogTimer = TPS * delay;
					}
				}
			}
		}
		if (map.prevSchedule != map.schedule
				&& aiState != AI_ATTACK
				&& (inmate || (guard && (aiState != AI_ROAM || typedId < 3
				|| (map.schedule == SC_LIGHTSOUT && bedX != 0 && bedY != 0)))
				|| bodyId == Textures.EMPLOYMENT_OFFICER || bodyId == Textures.DOCTOR)) {
			aiState = AI_RESET;
		}
		if (aiState == AI_RESET) {
			if (gymObjectIdx != -1 && gymObject == Objects.TRAINING_WEIGHT) {
				int idx = gymObjectIdx;
				map.objects[LAYER_GROUND][idx + 2] = (short) (12 | (map.objects[LAYER_GROUND][idx + 2] & 0xFF00));
				gymObjectIdx = -1;
			}
			if (training) {
				int seat = map.getSeatIndex(map.gymPositions, (x + 7) / TILE_SIZE, (y + 7) / TILE_SIZE);
				if (seat != -1 && map.gymPositions[seat + 1] == id) {
					map.gymPositions[seat + 1] = -1;
				}
				training = false;
			}
			if (sitting) {
				int seat = map.getSeatIndex(map.canteenSeatsPositions, (x + 7) / TILE_SIZE, (y + 7) / TILE_SIZE);
				if (seat != -1 && map.canteenSeatsPositions[seat + 1] == id) {
					map.canteenSeatsPositions[seat + 1] = -1;
				}
				sitting = false;
			}
//			angerTimer = TPS * 10 + rng.nextInt(TPS * 50);
//			aiWaitTimer = 0;
			targetReached = false;
			pathEndDir = -1;

			// TODO outfit was stolen while unconcious, go change it
//			if (outfitItem == Items.ITEM_NULL) {
//				aiState = AI_WAYPOINT;
//				if (inmate) {
//					if (map.solid[0][bedX + 1 + bedY * map.width] != 0) {
//						pathX = bedX - 1;
//					} else {
//						pathX = bedX + 1;
//					}
//					pathY = bedY;
//					correctPath = false;
//				} else {
//					int idx = map.findObject(Objects.GUARD_BED, LAYER_GROUND, 0);
//					pathX = bedX;
//					pathY = bedY;
//					correctPath = false;
//				}
//			} else
			if (chaseTarget != null && chaseTarget.health > 0) {
				// start attacking
				aiState = AI_ATTACK;
			} else if (guard) {
				// guards schedule
				if (map.schedule == SC_LIGHTSOUT && bedX != 0 && bedY != 0) {
					aiState = AI_SLEEP;
					pathX = bedX;
					pathY = bedY;
					correctPath = false;
				} else if (typedId < 3) {
					// first three guards
					int pos = (typedId << 1) + 1;
					switch (map.schedule) {
					case SC_MORNING_ROLLCALL:
					case SC_MIDDAY_ROLLCALL:
					case SC_EVENING_ROLLCALL:
						// rollcall
						aiState = AI_WAYPOINT;
						pathX = map.guardRollcallPositions[pos];
						pathY = map.guardRollcallPositions[pos + 1];
						pathEndDir = DIR_UP; // TODO face inmates
						correctPath = false;
						break;
					case SC_BREAKFAST:
					case SC_EVENING_MEAL:
						// canteen
						aiState = AI_WAYPOINT;
						pathX = map.guardCanteenPositions[pos];
						pathY = map.guardCanteenPositions[pos + 1];
						correctPath = false;
						break;
					case SC_EXCERCISE_PERIOD:
						// gym
						aiState = AI_WAYPOINT;
						pathX = map.guardGymPositions[pos];
						pathY = map.guardGymPositions[pos + 1];
						correctPath = false;
						break;
					case SC_SHOWER_BLOCK:
						// shower
						aiState = AI_WAYPOINT;
						pathX = map.guardShowerPositions[pos];
						pathY = map.guardShowerPositions[pos + 1];
						correctPath = false;
						break;
					default:
						// roam in other cases
						aiState = AI_ROAM;
						nextRoamPos = true;
						correctPath = false;
						break;
					}
				} else {
					// other guards always patrol
					aiState = AI_ROAM;
					nextRoamPos = true;
					correctPath = false;
				}
				if (animation == ANIM_LYING) {
					// reset animation state
					animation = ANIM_REGULAR;
				}
			} else if (inmate) {
				// inmates schedule
				switch (map.schedule) {
				case SC_MORNING_ROLLCALL:
				case SC_MIDDAY_ROLLCALL:
				case SC_EVENING_ROLLCALL: {
					// rollcall
					aiState = AI_WAYPOINT;
//					if (animation == ANIM_LYING) aiWaitTimer = TPS;
					int pos;
					if ((pos = rollcallPos++) >= map.rollcallPositions[0]) {
						pos = rollcallPos = 0;
					}
					pos = (pos << 1) + 1;
					pathX = map.rollcallPositions[pos];
					pathY = map.rollcallPositions[pos + 1];
					pathEndDir = DIR_DOWN; // TODO face guards
					correctPath = false;
					break;
				}
				case SC_BREAKFAST:
				case SC_EVENING_MEAL: {
					// canteen
					aiState = AI_MEAL;
					correctPath = false;
					sitting = false;
					break;
				}
				case SC_EXCERCISE_PERIOD:
					// gym
					aiState = AI_GYM;
					correctPath = false;
					training = false;
					break;
				case SC_SHOWER_BLOCK:
					// shower
					aiState = AI_WAYPOINT;
					int pos;
					if ((pos = showerPos++) >= map.showerPositions[0]) {
						pos = showerPos = 0;
					}
					pos = (pos << 1) + 1;
					pathX = map.showerPositions[pos];
					pathY = map.showerPositions[pos + 1];
					correctPath = false;
					break;
				case SC_LIGHTSOUT:
					// sleep
					aiState = AI_SLEEP;
					pathX = bedX;
					pathY = bedY;
					correctPath = false;
					break;
				case SC_LOCKDOWN:
					// lockdown: go to cell, stay there
					aiState = AI_WAYPOINT;
					if (map.solid[0][bedX + 1 + bedY * map.width] != 0) {
						pathX = bedX - 1;
					} else {
						pathX = bedX + 1;
					}
					pathY = bedY;
					correctPath = false;
					break;
				case SC_WORK_PERIOD:
					if (job != JOB_UNEMPLOYED) {
						// go to job
						aiState = AI_WORK;
						aiWorkState = 0;
						correctPath = false;
						jobQuota = 0;
						break;
					}
				default:
					// roam in other cases
					aiState = AI_ROAM;
					nextRoamPos = true;
					correctPath = false;
					break;
				}
				if (animation == ANIM_LYING || animation == ANIM_FOOD || animation == ANIM_WEIGHT) {
					// reset animation state
					animation = ANIM_REGULAR;
				}
			} else if (bodyId == Textures.EMPLOYMENT_OFFICER || bodyId == Textures.DOCTOR) {
				if (map.schedule == SC_MORNING_ROLLCALL) {
					pathX = bedX;
					pathY = bedY;
					correctPath = false;
					xFloat = x = map.npcSpawnX * TILE_SIZE;
					yFloat = y = map.npcSpawnY * TILE_SIZE;
					hidden = false;
					aiState = AI_WAYPOINT;
				} else if (map.schedule == SC_EVENING_ROLLCALL) {
					pathX = map.npcSpawnX;
					pathY = map.npcSpawnY;
					correctPath = false;
					aiState = AI_WAYPOINT;
				} else if (aiState != AI_WAYPOINT) {
					aiState = AI_IDLE;
				}
			}
			// TODO other npcs: warden, visitor
		}

		// guard heat cooldown
		if (heatTimer != 0) --heatTimer;

		NPC player = map.player;
		vision: {
			if (((tick + id) & 1) != 0) break vision;
			if (!inmate && !guard) break vision;
			if (aiState == AI_ATTACK) break vision;

			if (player.layer == LAYER_GROUND && player.health > 0 && canSee(player)) {
				if (guard) {
					if (map.time < 8 * 60 && !map.lockdown && player.outfitId != Textures.OUTFIT_GUARD
							&& !player.isInZone(ZONE_PLAYER_CELL)) {
						// send player to solitary if they're not in cell during lights out
						map.note = NOTE_SOLITARY;
					}
					if (map.heat >= 90 || map.lockdown || player.fighting != 0
							|| (map.action == ACT_SEARCHING && player.job != JOB_MAILMAN && player.job != JOB_LIBRARY)) {
						// guard attack player if they saw them stealing from others pocket
						//  or is searching others desk, except their job is mailman,
						//  or if heat is over 90 or lockdown is in progress
						aiState = AI_ATTACK;
						chaseTarget = player;
						aiWaitTimer = 0;
						map.playerSeenByGuards = true;

						heat("Oi, " + player.name + '!', 30);
						if (map.lockdown && map.guardsDown < 3) {
							map.lockdown = false;
						}
					} else if (map.action == ACT_CHIPPING || map.action == ACT_DIGGING) {
						map.note = NOTE_SOLITARY;
					} else if (heatTimer == 0 && player.health > 0) {
						if (player.outfitId == Textures.OUTFIT_GUARD) {
							// increase heat a little, if seeing player in disguise
							map.heat += 2;
							heatTimer = TPS;
						} else if (player.climbed) {
							// guard warns player if they're standing on desk or table
							heat("Get off that table!", 10);
						} else if (player.carry != null) {
							// guard warns player if they're carrying stunned npc
							heat("Drop it!", 10);
						} else if (player.isInZone(ZONE_INMATE_CELL) && !player.isInZone(ZONE_PLAYER_CELL)
								&& !player.isInZone(ZONE_JANITOR) && !player.isInZone(ZONE_GARDENING)
								&& player.job != JOB_MAILMAN && player.job != JOB_LIBRARY) {
							// TODO this check still doesn't work well
							// guard warns player if they're in wrong cell
							heat("Get out of that cell " + player.name + '!', 10);
						} else if (map.time % 60 >= 25) {
							// hurry up the player to scheduled place
							onPlace: {
								switch (map.schedule) {
								case SC_MORNING_ROLLCALL:
								case SC_MIDDAY_ROLLCALL:
								case SC_EVENING_ROLLCALL:
									if (player.isInZone(ZONE_ROLLCALL))
										break onPlace;
									break;
								case SC_BREAKFAST:
								case SC_EVENING_MEAL:
									if (player.isInZone(ZONE_CANTEEN))
										break onPlace;
									break;
								case SC_EXCERCISE_PERIOD:
									if (player.isInZone(ZONE_GYM))
										break onPlace;
									break;
								case SC_SHOWER_BLOCK:
									if (player.isInZone(ZONE_SHOWER))
										break onPlace;
									break;
								default:
									break onPlace;
								}

								heat("Move it " + player.name, 10);
							}
						} else if (player.outfitItem == Items.ITEM_NULL) {
							heat("Get some clothes on " + player.name + '!', 10);
						} else if (map.schedule == SC_WORK_PERIOD && map.time % 60 > 25 /* TODO count hours */
								&& player.job != JOB_UNEMPLOYED && player.jobQuota < MAX_JOB_QUOTA
								&& player.job != JOB_JANITOR && player.job != JOB_GARDENING
								&& player.job != JOB_MAILMAN && player.job != JOB_LIBRARY) {
							// if player is employed, job quota is not completed, and not in work zone, hurry them up
							onJob: {
								switch (job) {
								case JOB_LAUNDRY:
									if (player.isInZone(ZONE_LAUNDRY))
										break onJob;
									break;
								case JOB_KITCHEN:
									if (player.isInZone(ZONE_KITCHEN))
										break onJob;
									break;
								case JOB_WOODSHOP:
									if (player.isInZone(ZONE_WOODSHOP))
										break onJob;
									break;
								case JOB_METALSHOP:
									if (player.isInZone(ZONE_METALSHOP))
										break onJob;
									break;
								case JOB_TAILOR:
									if (player.isInZone(ZONE_TAILORSHOP))
										break onJob;
									break;
								case JOB_DELIVERIES:
									if (player.isInZone(ZONE_DELIVERIES))
										break onJob;
									break;
								default:
									break onJob;
								}

								heat("Get to work  " + player.name + '!', 10);
							}
						}
					}
//				} else if (inmate) {
				}
			}

			NPC[] chars = map.chars;
			int n = chars.length;
			boolean attack = (targetReached || !correctPath || aiState == AI_ROAM)
					&& aiState != AI_WAYPOINT && aiState != AI_SLEEP
					&& angerTimer == 0 && rng.nextInt(20) == 0;
			if (attack && rng.nextInt(80) != 0) {
				angerTimer = TPS;
				attack = false;
			}

			boolean found = false;
			for (int i = 1; i < n; ++i) {
				NPC other = chars[i];
				if (other == null || other == this || other.guard || other.health <= 0) {
					continue;
				}

				if (!canSee(other)) continue;

				if (guard) {
					// attack if inmate is fighting
					if (other.fighting != 0) {
						aiState = AI_ATTACK;
						chaseTarget = other;
						aiWaitTimer = 0;
						heat("Oi! " + other.name, 0);
						break;
					}
				} else if (inmate && other.inmate) {
					// attack someone in random interval if not busy
					found = true;
					if (attack && rng.nextInt(30) == 0) {
						animation = ANIM_REGULAR;
						aiState = AI_RESET;
						chaseTarget = other;
						break;
					}
				}
			}
			if (attack && found) {
				angerTimer = TPS + rng.nextInt(TPS * 3);
			}
		}

		boolean targetReached = this.targetReached;
		if (!targetReached && correctPath) {
			int dx = pathX * TILE_SIZE - x, dy = pathY * TILE_SIZE - y + 5;
			targetReached = this.targetReached = dx * dx + dy * dy <= TILE_SIZE * 2 + rng.nextInt(10);
		}

		if (aiState == AI_ROAM) {
			// roam: walk around points on the map
			if (nextRoamPos) {
				int idx;
				short[] arr;
				if (guard) {
					arr = map.guardRoamPositions;
					if ((idx = guardRoamPos++) >= arr[0]) {
						idx = guardRoamPos = 0;
					}
				} else {
					arr = map.roamPositions;
					if ((idx = inmateRoamPos++) >= arr[0]) {
						idx = inmateRoamPos = 0;
					}
				}
				idx = (idx << 1) + 1;
				pathX = arr[idx];
				pathY = arr[idx + 1];
				nextRoamPos = false;
				if (map.pathfind(x / TILE_SIZE, (y + 5) / TILE_SIZE, direction, pathX, pathY, inmate, path)) {
					correctPath = true;
					pathStep = 0;
					// fallback
				} else if (map.pathfind(x / TILE_SIZE, (y + 5) / TILE_SIZE, direction, pathX, pathY, false, path)) {
					correctPath = true;
					pathStep = 0;
				} else {
					if (LOGGING) Profiler.log(debugName() + " cannot pathfind to roam");
					correctPath = false;
					nextRoamPos = true;
					this.targetReached = false;
				}
			} else if (targetReached) {
				aiWaitTimer = TPS * 2;
				nextRoamPos = true;
				this.targetReached = false;
				correctPath = false;
			}
		} else if (aiState == AI_WAYPOINT) {
			// waypoint: go to point, stay there
			if (targetReached) {
				if (pathEndDir != -1) {
					direction = pathEndDir;
				}
				correctPath = false;
			} else if (!correctPath) {
				if (map.pathfind(x / TILE_SIZE, (y + 5) / TILE_SIZE, direction, pathX, pathY, false, path)) {
					correctPath = true;
					pathStep = 0;
				} else if (LOGGING) {
					Profiler.log(debugName() + " cannot pathfind to waypoint");
				}
			}
		} else if (aiState == AI_SLEEP) {
			// sleep: go to own bed, sleep
			pathX = bedX;
			pathY = bedY;
			if (x / TILE_SIZE == pathX && (y + 5) / TILE_SIZE == pathY) {
				// stay at the point
				animation = ANIM_LYING;
				xFloat = x = bedX * TILE_SIZE;
				yFloat = y = bedY * TILE_SIZE + 2;
			} else if (!correctPath) {
				if (map.pathfind(x / TILE_SIZE, (y + 5) / TILE_SIZE, direction, pathX, pathY, false, path)) {
					correctPath = true;
					pathStep = 0;
				} else if (LOGGING) {
					Profiler.log(debugName() + " cannot pathfind to sleep");
				}
			}
		} else if (aiState == AI_MEAL) {
			// meal: go to serving table, take food, go to free seat
			if (animation == ANIM_FOOD) {
				if (!correctPath && !sitting) {
					// find free seat
					while (true) {
						int pos;
						if ((pos = canteenSeatPos++) >= map.canteenSeatsPositions[0]) {
							pos = canteenSeatPos = 0;
						}
						pos = (pos << 1) + 1;
						if (map.canteenSeatsPositions[pos + 1] != -1) {
							continue;
						}
						pos = map.canteenSeatsPositions[pos];
						pathX = pos & 0xFF;
						pathY = (pos >> 8) & 0xFF;
						break;
					}
					pathEndDir = map.solid[LAYER_GROUND][pathX + (pathY + 1) * map.width] == COLL_TABLE ? DIR_DOWN : DIR_UP;
					aiWaitTimer = TPS >> 2;
				}
				if (x / TILE_SIZE == pathX && (y + 5) / TILE_SIZE == pathY) {
					correctPath = false;

					if (!sitting) {
						int seat = map.getSeatIndex(map.canteenSeatsPositions, pathX, pathY);
						if (map.canteenSeatsPositions[seat + 1] == -1) {
							map.canteenSeatsPositions[seat + 1] = (short) id;
							animateToX = pathX * TILE_SIZE;
							animateToY = pathY * TILE_SIZE;
							sitting = true;
							direction = pathEndDir;
						}
					}
				} else if (!correctPath) {
					if (map.pathfind(x / TILE_SIZE, (y + 5) / TILE_SIZE, direction, pathX, pathY, false, path)) {
						correctPath = true;
						pathStep = 0;
					} else if (LOGGING) {
						Profiler.log(debugName() + " cannot pathfind to seat");
					}
				}
			} else {
				if (!correctPath) {
					int pos;
					if ((pos = canteenServingPos++) >= map.canteenServingPositions[0]) {
						pos = canteenServingPos = 0;
					}
					pos = (pos << 1) + 1;
					pathX = map.canteenServingPositions[pos];
					pathY = map.canteenServingPositions[pos + 1];
				}
				if (targetReached) {
					animation = ANIM_FOOD;
					correctPath = false;
				} else if (!correctPath) {
					if (map.pathfind(x / TILE_SIZE, (y + 5) / TILE_SIZE, direction, pathX, pathY, false, path)) {
						correctPath = true;
						pathStep = 0;
					} else if (LOGGING) {
						Profiler.log(debugName() + " cannot pathfind to serving");
					}
				}
			}
		} else if (aiState == AI_GYM) {
			// gym: go to free object and start training animation
			if (!correctPath && !training) {
				// find free seat
				while (true) {
					int pos;
					if ((pos = gymPos++) >= map.gymPositions[0]) {
						pos = gymPos = 0;
					}
					pos = (pos << 1) + 1;
					if (map.gymPositions[pos + 1] != -1) {
						continue;
					}
					pos = map.gymPositions[pos];
					pathX = pos & 0xFF;
					pathY = (pos >> 8) & 0xFF;
					break;
				}

				int idx = map.getObjectIdxAt(pathX, pathY, LAYER_GROUND);
				int obj = map.objects[LAYER_GROUND][idx + 1];
				gymObjectIdx = idx;
				gymObject = obj;
				if (obj == Objects.TRAINING_TREADMILL) {
					pathEndDir = DIR_RIGHT;
				} else {
					pathEndDir = DIR_DOWN;
				}
			}
			if (training && animation == ANIM_WEIGHT) {
				if (rng.nextInt(2) == 0 && ++trainingFrame == 20) {
					trainingFrame = 0;
				}
			} else if (x / TILE_SIZE == pathX && (y + 5) / TILE_SIZE == pathY) {
				correctPath = false;

				if (!training) {
					int seat = map.getSeatIndex(map.gymPositions, pathX, pathY);
					if (map.gymPositions[seat + 1] == -1) {
						map.gymPositions[seat + 1] = (short) id;
						animateToX = pathX * TILE_SIZE;
						animateToY = pathY * TILE_SIZE;
						if (pathEndDir == DIR_RIGHT) {
							// treadmill
							animateToX -= 4;
							animateToY -= 4;
						} else {
							animation = ANIM_WEIGHT;
							trainingFrame = 0;
						}
						if (gymObjectIdx != -1 && gymObject == Objects.TRAINING_WEIGHT) {
							int idx = gymObjectIdx;
							map.objects[LAYER_GROUND][idx + 2] = (short) (13 | (map.objects[LAYER_GROUND][idx + 2] & 0xFF00));
						}
						training = true;
						direction = pathEndDir;
					}
				}
			} else if (!correctPath && !training) {
				if (map.pathfind(x / TILE_SIZE, (y + 5) / TILE_SIZE, direction, pathX, pathY, false, path)) {
					correctPath = true;
					pathStep = 0;
				} else if (LOGGING) {
					Profiler.log(debugName() + " cannot pathfind to gym");
				}
			}
		} else if (aiState == AI_WORK) {
			work:
			{
				switch (job) {
				case JOB_JANITOR:
				case JOB_GARDENING: {
					if (nextRoamPos) {
						pathX = map.dirt[0];
						pathY = map.dirt[1];
						nextRoamPos = false;
						if (map.pathfind(x / TILE_SIZE, (y + 5) / TILE_SIZE, direction, pathX, pathY, inmate, path)) {
							correctPath = true;
							pathStep = 0;
							// fallback
						} else if (map.pathfind(x / TILE_SIZE, (y + 5) / TILE_SIZE, direction, pathX, pathY, false, path)) {
							correctPath = true;
							pathStep = 0;
						} else {
							if (LOGGING) Profiler.log(debugName() + " cannot pathfind to job " + Game.jobStrings[job]);
							correctPath = false;
							nextRoamPos = true;
							this.targetReached = false;
						}
					} else if (targetReached) {
						aiWaitTimer = TPS * 2;
						nextRoamPos = true;
						this.targetReached = false;
						correctPath = false;
						map.updateDirt(0);
						jobQuota++;
					}
					break;
				}
				case JOB_METALSHOP:
				case JOB_LAUNDRY:
				case JOB_KITCHEN: {
					int obj;
					int i = aiWorkState;
					if (!correctPath) {
						switch (job) {
						case JOB_METALSHOP:
							obj = i == 0 ? Objects.JOB_RAW_METAL : i == 1 ? Objects.JOB_METAL_TOOLS : Objects.JOB_PREPARED_METAL;
							break;
						case JOB_LAUNDRY:
							obj = i == 0 ? Objects.JOB_DIRTY_LAUNDRY : i == 1 ? Objects.WASHING_MACHINE : Objects.JOB_CLEAN_LAUNDRY;
							break;
						case JOB_KITCHEN:
							obj = i == 0 ? Objects.FREEZER : i == 1 ? Objects.OVEN : Objects.SERVING_TABLE;
							break;
						default:
							break work;
						}

						obj = map.findObject(obj, LAYER_GROUND, 0);
						if (obj == -1) break work;

						int x = map.objects[LAYER_GROUND][obj + 3];
						int y = map.objects[LAYER_GROUND][obj + 4];

						solid: {
							if (map.solid[LAYER_GROUND][x + y * map.width] != 0) {
								for (int n = 0; n < 4; ++n) {
									int ox = Game.PATH_DIR_POSITIONS[(3 - n) << 1];
									int oy = Game.PATH_DIR_POSITIONS[((3 - n) << 1) + 1];
									if (map.solid[LAYER_GROUND][x + ox + (y + oy) * map.width] == 0) {
										x += ox;
										y += oy;
										break solid;
									}
								}
							} else break solid;

							break work;
						}
						pathX = x;
						pathY = y;

						if (map.pathfind(this.x / TILE_SIZE, (this.y + 5) / TILE_SIZE, direction, pathX, pathY, false, path)) {
							correctPath = true;
							pathStep = 0;
						} else if (LOGGING) {
							Profiler.log(debugName() + " cannot pathfind to job " + Game.jobStrings[job]);
						}
					} else if (targetReached) {
						if (++aiWorkState == 3) {
							aiWorkState = 0;
							jobQuota++;
						}
						aiWaitTimer = TPS * 2;
						this.targetReached = false;
						correctPath = false;
					}
					break;
				}
				case JOB_WOODSHOP:
				case JOB_TAILOR: {
					int obj;
					int i = aiWorkState;
					if (!correctPath) {
						switch (job) {
						case JOB_WOODSHOP:
							obj = i == 0 ? Objects.JOB_RAW_WOOD : Objects.JOB_PREPARED_WOOD;
							break;
						case JOB_TAILOR:
							obj = i == 0 ? Objects.JOB_FABRIC_CHEST : Objects.JOB_CLOTHING_STORAGE;
							break;
						default:
							break work;
						}

						obj = map.findObject(obj, LAYER_GROUND, 0);
						if (obj == -1) break work;

						int x = map.objects[LAYER_GROUND][obj + 3];
						int y = map.objects[LAYER_GROUND][obj + 4];

						solid: {
							if (map.solid[LAYER_GROUND][x + y * map.width] != 0) {
								for (int n = 0; n < 4; ++n) {
									int ox = Game.PATH_DIR_POSITIONS[(3 - n) << 1];
									int oy = Game.PATH_DIR_POSITIONS[((3 - n) << 1) + 1];
									if (map.solid[LAYER_GROUND][x + ox + (y + oy) * map.width] == 0) {
										x += ox;
										y += oy;
										break solid;
									}
								}
							} else break solid;

							break work;
						}
						pathX = x;
						pathY = y;
						if (map.pathfind(this.x / TILE_SIZE, (this.y + 5) / TILE_SIZE, direction, pathX, pathY, false, path)) {
							correctPath = true;
							pathStep = 0;
						} else if (LOGGING) {
							Profiler.log(debugName() + " cannot pathfind to job " + Game.jobStrings[job]);
						}
					} else if (targetReached) {
						if (++aiWorkState == 2) {
							aiWorkState = 0;
							jobQuota++;
						}
						aiWaitTimer = TPS * 2;
						this.targetReached = false;
						correctPath = false;
					}
					break;
				}
				case JOB_MAILMAN:
				case JOB_LIBRARY: {
					// TODO
					break;
				}
				}
			}
		}

		if (chaseTarget != null) {
			// chase
			int tx = chaseTarget.x, ty = chaseTarget.y;
			int dx = tx - x, dy = ty - y;
			if (dx * dx + dy * dy > NPC_CHASE_LOSE_DISTANCE || chaseTarget.health <= 0) {
				// target lost
				aiState = AI_RESET;
				chaseTarget = null;
				correctPath = false;
				aiWaitTimer = TPS;
			} else {
				if (dx * dx + dy * dy > TILE_SIZE * TILE_SIZE) {
					// pick random offsets of position
					if (tick % TPS == 0) {
						targetOffsetX = rng.nextInt(30) - 15;
						targetOffsetY = rng.nextInt(30) - 15;
					}
					tx += targetOffsetX;
					ty += targetOffsetY;
					pathX = tx / TILE_SIZE;
					pathY = (ty + 5) / TILE_SIZE;
					if (map.solid[LAYER_GROUND][pathX + pathY * map.width] != 0) {
						// if offset is solid, pick original position
						tx = chaseTarget.x;
						ty = chaseTarget.y;
						pathX = tx / TILE_SIZE;
						pathY = (ty + 5) / TILE_SIZE;
					}
					if ((map.solid[LAYER_GROUND][pathX + pathY * map.width] & COLL_BIT_SOLID_AI) != 0) {
						// TODO handle case, when player is standing on table
						//  e.g, find closest free tile to pathfind
						correctPath = false;
					} else if (map.pathfind(x / TILE_SIZE, (y + 5) / TILE_SIZE, direction, pathX, pathY, false, path)) {
						correctPath = true;
						pathStep = 0;
					} else {
						if (LOGGING) Profiler.log(debugName() + " cannot pathfind to target " + chaseTarget.debugName());
						correctPath = false;
					}
				}
			}
		}

		// cooldown
		if (aiWaitTimer != 0 && --aiWaitTimer != 0) {
			return;
		}

		// follow path
		if (correctPath) {
			int n = path[0];
			if (n >= 4 && (pathStep << 1) + 4 < n) {
				n -= (pathStep << 1) + 4;
				int px = path[n];
				int py = path[n + 1];

				moveTowards(px * TILE_SIZE, py * TILE_SIZE, (other ? OTHER_NPC_SPEED : NPC_SPEED) * (0.8f + (rng.nextFloat() * 0.4f)));

				if (x / TILE_SIZE == px && ((y + 5) / TILE_SIZE) == py && n != 1) {
					++pathStep;
				}
			} else if (aiState == AI_ROAM) {
				nextRoamPos = true;
			}
		} else if (aiState == AI_ROAM) {
			nextRoamPos = true;
		}
	}

	// move towards point smoothly
	private void moveTowards(int tx, int ty, float speed) {
		if (xFloat == -1 || yFloat == -1) {
			xFloat = x;
			yFloat = y;
		}

		float dx = tx - xFloat;
		float dy = ty - yFloat;
		float distance = (float) Math.sqrt(dx * dx + dy * dy);

		if (distance <= speed) {
			xFloat = tx;
			yFloat = ty;
		} else {
			xFloat += (dx / distance) * speed;
			yFloat += (dy / distance) * speed;
		}

		this.x = (int) xFloat;
		this.y = (int) yFloat;

		if (Math.abs(dx) > Math.abs(dy)) {
			direction = dx > 0 ? DIR_RIGHT : DIR_LEFT;
		} else {
			direction = dy > 0 ? DIR_DOWN : DIR_UP;
		}
	}

	void updateSprite() {
		body.setImage(Game.sprites[bodyId], 16, 16);
		if (outfit != null) {
			outfit.setImage(Game.sprites[outfitId], 16, 16);
		}
	}

	// check if player is free to go on position offset
	boolean checkCollision(int x, int y) {
		x = ((int) this.x + x) / TILE_SIZE;
		y = ((int) this.y + y) / TILE_SIZE;
		if (x < 0 || y < 0 || x >= map.width || y >= map.height) {
			return false;
		}
		byte s = map.solid[layer][x + y * map.width];
		if (s == COLL_NONE || s == COLL_NOT_SOLID_INTERACT || s == COLL_DIGGED_WALL) {
			return false;
		}
		if (s == COLL_SOLID || s == COLL_SOLID_TRANSPARENT || s == COLL_SOLID_INTERACT) {
			canClimb = false;
			return true;
		}
		if (this.ai) {
			return (s & COLL_BIT_SOLID_AI) != 0;
		}
		if (climbed && (s == COLL_DESK || s == COLL_TABLE)) {
			return false;
		}
		if (s == COLL_GYM) {
			return true;
		}
		if (s == COLL_DOOR || s == COLL_DOOR_STAFF) {
			canClimb = false;
			int idx = map.getObjectIdxAt(x, y, layer);
			int obj = map.objects[layer][idx + 1];

			// TODO make sure player wouldn't get stuck in closed doors
			if (lastDoor == idx) {
				return false;
			}
			if (obj == Objects.DOOR_CELL) {
				return map.cellsClosed && !hasItem(Items.CELL_KEY);
			}
			if (obj == Objects.DOOR_OUTSIDE) {
				return (map.time < 10 * 60 || map.time >= 22 * 60)
						&& !hasItem(Items.ENTRANCE_KEY) && !hasItem(Items.PLASTIC_ENTRANCE_KEY);
			}
			if (obj == Objects.DOOR_PRISON_ENTRANCE) {
				return !map.entranceOpen;
			}
			if (obj == Objects.DOOR_STAFF) {
				return !hasItem(Items.STAFF_KEY) && !hasItem(Items.PLASTIC_STAFF_KEY);
			}
			if (obj == Objects.DOOR_UTILITY || obj == Objects.DOOR_UTILITY_VENT) {
				return !hasItem(Items.UTILITY_KEY) && !hasItem(Items.PLASTIC_UTILITY_KEY);
			}
			// job doors
			switch (job) {
			case JOB_LAUNDRY:
				if (obj == Objects.DOOR_LAUNDRY)
					return false;
				break;
			case JOB_KITCHEN:
				if (obj == Objects.DOOR_KITCHEN)
					return false;
				break;
			case JOB_WOODSHOP:
				if (obj == Objects.DOOR_WOODSHOP)
					return false;
				break;
			case JOB_METALSHOP:
				if (obj == Objects.DOOR_METALSHOP)
					return false;
				break;
			case JOB_TAILOR:
				if (obj == Objects.DOOR_TAILORSHOP)
					return false;
				break;
			case JOB_DELIVERIES:
				if (obj == Objects.DOOR_DELIVERIES)
					return false;
				break;
			case JOB_JANITOR:
				if (obj == Objects.DOOR_JANITOR)
					return false;
				break;
			case JOB_GARDENING:
				if (obj == Objects.DOOR_GARDENING)
					return false;
				break;
			case JOB_MAILMAN:
				if (obj == Objects.DOOR_MAILROOM)
					return false;
				break;
			}
			return !hasItem(Items.WORK_KEY) && !hasItem(Items.PLASTIC_WORK_KEY);
		}
		if (!climbed && (s == COLL_DESK || s == COLL_TABLE)) {
			if (!wasTryingToMove && canClimb) {
				climb = true;
				return false;
			}
			return true;
		}
		if ((s & COLL_BIT_SOLID_AI) != 0) {
			canClimb = false;
			return true;
		}
		canClimb = false;
		return false;
	}

	byte getCollision(int x, int y, boolean act) {
		x = ((int) this.x + x) / TILE_SIZE;
		y = ((int) this.y + y) / TILE_SIZE;
		if (x < 0 || y < 0 || x >= map.width || y >= map.height) {
			return COLL_NONE;
		}
		byte b = map.solid[layer][x + y * map.width];

		if (!act) {
			if (b == COLL_DOOR || b == COLL_DOOR_STAFF) {
				int idx = map.getObjectIdxAt(x, y, layer);
				if (lastDoor != -1 && lastDoor != idx) {
					map.objects[layer][lastDoor + 2] &= ~(1 << 12);
				}
				map.objects[layer][idx + 2] |= 1 << 12;
				lastDoor = idx;
			}
			if (b == COLL_SHOWER) {
				int idx = map.getObjectIdxAt(x, y, layer);

				if (lastShower != -1 && lastShower != idx) {
					map.objects[layer][lastShower + 2] |= 1 << 12;
				}
				map.objects[layer][idx + 2] &= ~(1 << 12);
				lastShower = idx;
			}
		}

		return b;
	}

	boolean canSee(NPC other) {
		if (layer != LAYER_GROUND || other.inCabinet) {
			// player can't be seen if on other map layer or hiding in cabinet
			return false;
		}
		int dx = this.x - other.x;
		int dy = this.y - other.y;
		if (dx * dx + dy * dy > NPC_VIEW_DISTANCE) {
			// too far
			return false;
		}

		int x0, y0;
		int x1 = x0 = this.x / TILE_SIZE, y1 = y0 = (this.y + 5) / TILE_SIZE;
		int x2 = other.x / TILE_SIZE, y2 = (other.y + 5) / TILE_SIZE;
		int w = map.width;
		int h = map.height;
		byte[] solid = map.solid[layer];

		dx = Math.abs(x2 - x0);
		dy = Math.abs(y2 - y0);

		int sx = (x0 < x2) ? 1 : -1;
		int sy = (y0 < y2) ? 1 : -1;

		int e = dx - dy;

		while (true) {
			if (x1 == x2 && y1 == y2) {
				return true;
			}
			if ((x1 != x0 || y1 != y0)
					&& x1 >= 0 && y1 >= 0 && x1 < w && y1 < h) {
				byte s = solid[x1 + y1 * w];
				if (s == COLL_SOLID) {
					return false;
				}
			}

			int e2 = e * 2;
			if (e2 > -dy) {
				e -= dy;
				x1 += sx;
			}
			if (e2 < dx) {
				e += dx;
				y1 += sy;
			}
		}
	}

	// give player a warn
	private void heat(String dialog, int amount) {
		map.heat += amount;
		heatTimer = TPS * 3;
		this.dialog = dialog;
		dialogTimer = TPS * 2;
		if (amount > 0 || visible) {
			Sound.playEffect(Sound.SFX_LOSE);
		}
	}
	
	void damage(NPC source, int n) {
		// defense
		switch (outfitItem & Items.ITEM_ID_MASK) {
		case Items.GUARD_OUTFIT:
		case Items.CUSHIONED_INMATE_OUTFIT:
		case Items.CUSHIONED_POW_OUTFIT:
			n -= 1;
		case Items.PADDED_INMATE_OUTFIT:
		case Items.PADDED_POW_OUTFIT:
			n -= 2;
			break;
		case Items.PLATED_INMATE_OUTFIT:
		case Items.PLATED_POW_OUTFIT:
			n -= 3;
			break;
		}
		if (n < 1) n = 1;

		health -= n;
		if (health < 0) {
			health = 0;
			attackTimer = 0;
			return;
		}
		if (!ai) {
			if (chaseTarget == null && !training && !sitting
					&& (animation == ANIM_REGULAR || animation == ANIM_FOOD)) {
				chaseTarget = source;
			}
			return;
		}
		if (source == null || aiState == AI_ATTACK)
			return;

		if (inmate) {
			dialog = "LOL!";
			dialogTimer = (TPS * 2);
		} else if (guard) {
			if (visible) {
				Sound.playEffect(Sound.SFX_LOSE);
			}
			if (!source.ai) {
				map.heat += 10;
			}
		}
		aiState = AI_RESET;
		chaseTarget = source;
		correctPath = false;
		aiWaitTimer = 0;
	}

	boolean addItem(int item, boolean sound) {
		for (int i = 0; i < 6; ++i) {
			if (inventory[i] == Items.ITEM_NULL) {
				inventory[i] = item;
				if (sound && !ai && !map.pausedOverlay) {
					Sound.playEffect(Sound.SFX_PICKUP);
				}
				return true;
			}
		}
		if (sound && !ai) {
			Sound.playEffect(Sound.SFX_LOSE);
		}
		return false;
	}

	boolean hasItem(int item) {
		for (int i = 0; i < 6; ++i) {
			if (inventory[i] != Items.ITEM_NULL
					&& (inventory[i] & Items.ITEM_ID_MASK) == item) {
				return true;
			}
		}
		return false;
	}

	private String debugName() {
		if (!LOGGING) return null;
		return (!ai ? "player" : guard ? "guard" : inmate ? "inmate" : "other") + typedId + '(' + id + ')';
	}

// region Player

	NPC carry;

	boolean inCabinet, animatingInCabinet;

	void tickPlayer(int tick) {
		if (statStrength < 10) {
			statStrength = 10;
		}
		if (fighting != 0) {
			fighting--;
		}
		if (map.lockdown) {
			map.heat = 100;
		} else if (map.heat != 0) {
			// lose heat
			if (map.heat > 100) map.heat = 100;
			if (tick % (TPS * 3) == 0) {
				map.heat--;
			}
		}
		int maxHealth = statStrength >> 1;
		if (health <= 0 && animation != ANIM_STUNNED) {
			// defeated
			map.heat = 0;
			animation = ANIM_STUNNED;
			chaseTarget = null;
			map.action = ACT_NONE;
			map.progress = 0;

			// fadeout
			animationTimer = TPS * 3;
			map.ingameFadeOut = map.viewWidth;

			if (map.lockdown) {
				map.lockdown = false;
				map.note = NOTE_SOLITARY;
			}
			if (sitting) {
				int seat = map.getSeatIndex(map.canteenSeatsPositions, (x + 7) / TILE_SIZE, (y + 7) / TILE_SIZE);
				if (seat != -1) {
					map.canteenSeatsPositions[seat + 1] = -1;
				}
				sitting = false;
			}
			if (gymObjectIdx != -1 && gymObject == Objects.TRAINING_WEIGHT) {
				int idx = gymObjectIdx;
				map.objects[LAYER_GROUND][idx + 2] = (short) (12 | (map.objects[LAYER_GROUND][idx + 2] & 0xFF00));
				gymObjectIdx = -1;
			}
			if (training) {
				int seat = map.getSeatIndex(map.gymPositions, (x + 7) / TILE_SIZE, (y + 7) / TILE_SIZE);
				if (seat != -1) {
					map.gymPositions[seat + 1] = -1;
				}
				training = false;
			}
		} else if (health > maxHealth) {
			// limit health
			health = maxHealth;
		} else if (health < maxHealth) {
			// restore health
			if ((animation == ANIM_LYING && tick % TPS == 0)
					|| (tick % (TPS * 3) == 0)) {
				health++;
			}
		}
		if (map.fatigue != 0) {
			// restore fatigue
			if (tick % TPS == 0) {
				if (sitting) {
					map.fatigue -= animation == ANIM_FOOD ? 6 : (3 - rng.nextInt(2));
				} else if (lastShower != -1) {
					map.fatigue -= 4;
				} else if (animation == ANIM_LYING) {
					map.fatigue -= 2;
				}
				if (tick % (TPS * 5) == 0) {
					map.fatigue--;
				}
				if (map.fatigue < 0) map.fatigue = 0;
			}
		}

		if (chaseTarget != null && chaseTarget.health <= 0) {
			chaseTarget = null;
		}

		// limit stats
		if (statStrength > 100)
			statStrength = 100;
		if (statSpeed > 100)
			statSpeed = 100;
		if (statIntellect > 100)
			statIntellect = 100;

		if (map.heat < 99 && getCollision(8, 8, true) == COLL_DETECTOR) {
			boolean hasContraband = false;
			for (int i = 0; i < 6; ++i) {
				if (inventory[i] != Items.ITEM_NULL && Game.isIllegal(inventory[i] & Items.ITEM_ID_MASK)) {
					hasContraband = true;
					break;
				}
			}
			if (hasContraband) {
				// TODO animate detector object
				map.heat = 100;
				Sound.playEffect(Sound.SFX_BUY); // TODO replace effect
			}
		}

		if (layer == LAYER_GROUND) {
			int schedule = map.schedule;
			switch (schedule) {
			case SC_MORNING_ROLLCALL:
			case SC_MIDDAY_ROLLCALL:
			case SC_EVENING_ROLLCALL:
				if (!map.playerWasOnRollcall && isInZone(ZONE_ROLLCALL)) {
					map.playerWasOnRollcall = true;
				}
				break;
			case SC_BREAKFAST:
			case SC_EVENING_MEAL:
				if (!map.playerWasOnMeal && isInZone(ZONE_CANTEEN)) {
					map.playerWasOnMeal = true;
				}
				break;
			case SC_EXCERCISE_PERIOD:
				if (!map.playerWasOnExcercise && isInZone(ZONE_GYM)) {
					map.playerWasOnExcercise = true;
				}
				break;
			case SC_SHOWER_BLOCK:
				if (!map.playerWasOnShowers && isInZone(ZONE_SHOWER)) {
					map.playerWasOnShowers = true;
				}
				break;
			}
		}

		if (animateToX == -1 || animateToY == -1) {
			int actions = map.debugFreecam ? 0 : map.keyStates;
			if (inCabinet) {
				// leave cabinet
				if ((actions & GameCanvas.DOWN_PRESSED) != 0) {
					inCabinet = false;
					animateToX = x;
					animateToY = y + TILE_SIZE;
				}
			} else if (sitting) {
				canClimb = false;
				if ((actions & GameCanvas.UP_PRESSED) != 0 && !checkCollision(5, -1)) {
					sitting = false;
					animateToX = x;
					animateToY = y - TILE_SIZE;
					direction = NPC.DIR_UP;
				} else if ((actions & GameCanvas.DOWN_PRESSED) != 0 && !checkCollision(5, 16)) {
					sitting = false;
					animateToX = x;
					animateToY = y + TILE_SIZE;
					direction = NPC.DIR_DOWN;
				} else if ((actions & GameCanvas.LEFT_PRESSED) != 0 && !checkCollision(-1, 5)) {
					sitting = false;
					animateToX = x - TILE_SIZE;
					animateToY = y;
					direction = NPC.DIR_LEFT;
				} else if ((actions & GameCanvas.RIGHT_PRESSED) != 0 && !checkCollision(16, 5)) {
					sitting = false;
					animateToX = x + TILE_SIZE;
					animateToY = y;
					direction = NPC.DIR_RIGHT;
				}
				if (!sitting) {
					// leave seat
					int seat = map.getSeatIndex(map.canteenSeatsPositions, (x + 7) / TILE_SIZE, (y + 7) / TILE_SIZE);
					if (seat != -1) {
						map.canteenSeatsPositions[seat + 1] = -1;
					}
				}
			} else if (training) {
				canClimb = false;
				boolean treadmill = direction == DIR_RIGHT;
				if ((actions & GameCanvas.UP_PRESSED) != 0 && !checkCollision(5, -1)) {
					training = false;
					animateToX = x;
					animateToY = y - TILE_SIZE;
					direction = NPC.DIR_UP;
				} else if ((actions & GameCanvas.DOWN_PRESSED) != 0 && !checkCollision(5, 16)) {
					training = false;
					animateToX = x;
					animateToY = y + TILE_SIZE;
					direction = NPC.DIR_DOWN;
				} else if ((actions & GameCanvas.LEFT_PRESSED) != 0 && !checkCollision(-1, 5)) {
					training = false;
					animateToX = x - TILE_SIZE;
					animateToY = y;
					direction = NPC.DIR_LEFT;
				} else if ((actions & GameCanvas.RIGHT_PRESSED) != 0 && !checkCollision(16, 5)) {
					training = false;
					animateToX = x + TILE_SIZE;
					animateToY = y;
					direction = NPC.DIR_RIGHT;
				}
				if (!training) {
					// leave training
					if (gymObjectIdx != -1 && gymObject == Objects.TRAINING_WEIGHT) {
						int idx = gymObjectIdx;
						map.objects[LAYER_GROUND][idx + 2] = (short) (12 | (map.objects[LAYER_GROUND][idx + 2] & 0xFF00));
						gymObjectIdx = -1;
					}
					int seat = map.getSeatIndex(map.gymPositions, (x + 7) / TILE_SIZE, (y + 7) / TILE_SIZE);
					if (seat != -1) {
						map.gymPositions[seat + 1] = -1;
					}
					if (treadmill) {
						// treadmill
						animateToX += 4;
						animateToY += 4;
					}
					animation = ANIM_REGULAR;
				} else if (map.trainingTimer != 0) {
					if (gymObject == Objects.TRAINING_WEIGHT) {
						if (map.trainingTimer >= 40 && !map.trainingBlocked) {
							map.trainingBlocked = true;
							map.fatigue += 5;
							if (++map.trainingRepeats % 2 == 0) {
								statStrength++;
							}
							Sound.playEffect(SFX_ENHIT);
						}
					} else if (gymObject == Objects.TRAINING_TREADMILL) {
						// TODO
					}
					if (map.trainingTimer > 40) {
						map.trainingTimer = 40;
					}
					map.trainingTimer--;
				} else {
					map.trainingBlocked = false;
				}
			} else if (map.action != ACT_NONE) {
				if ((actions & (GameCanvas.UP_PRESSED | GameCanvas.DOWN_PRESSED | GameCanvas.LEFT_PRESSED | GameCanvas.RIGHT_PRESSED)) != 0) {
					// cancel
					map.action = ACT_NONE;
					map.progress = 0;
				} else if (++map.progress == TPS * 2) {
					// finished
					switch (map.action) {
					case ACT_READING:
						Sound.playEffect(Constants.SFX_CLOSE);
						statIntellect++;
						break;
					case ACT_CLEANING: {
						int idx = map.getObjectIdxAt(map.actionTargetX, map.actionTargetY, layer);
						map.updateDirt(idx == map.dirt[2] ? 0 : 1);
						map.fatigue += 5;
						if (map.schedule == SC_WORK_PERIOD && (job == JOB_GARDENING || job == JOB_JANITOR)) {
							if (jobQuota < MAX_JOB_QUOTA) {
								if ((jobQuota += (MAX_JOB_QUOTA / 5)) >= MAX_JOB_QUOTA) {
									// TODO
									Sound.playEffect(Sound.SFX_BUY);
									jobQuota = MAX_JOB_QUOTA;
									map.money += 20;
								}
							}
						}
						break;
					}
					case ACT_SEARCHING: {
						map.openContainer(map.getObjectIdxAt(map.actionTargetX, map.actionTargetY, layer));
						break;
					}
					}

					map.action = ACT_NONE;
					map.progress = 0;
				} else if (map.action != ACT_READING && map.action != ACT_SEARCHING && map.progress % (TPS / 2) == 1) {
					Sound.playEffect(Constants.SFX_OPEN);
				}
			} else if (animation == NPC.ANIM_REGULAR || animation == NPC.ANIM_FOOD) {
				// movement
				canClimb = true;
				climb = false;

				if ((actions & GameCanvas.UP_PRESSED) != 0
						&& !checkCollision(0, 4) && !checkCollision(15, 4)) {
					if (checkCollision(0, 5 - PLAYER_SPEED) || checkCollision(15, 5 - PLAYER_SPEED)) {
						y -= Math.min(PLAYER_SPEED, (y + 5) % TILE_SIZE);
					} else {
						y -= PLAYER_SPEED;
					}
					direction = NPC.DIR_UP;
				} else if ((actions & GameCanvas.DOWN_PRESSED) != 0) {
					if (!checkCollision(0, 16) && !checkCollision(15, 16)) {
						if (checkCollision(0, 16 + PLAYER_SPEED) || checkCollision(15, 16 + PLAYER_SPEED)) {
							y += Math.min(PLAYER_SPEED, TILE_SIZE - (y % TILE_SIZE));
						} else {
							y += PLAYER_SPEED;
						}
					}
					direction = NPC.DIR_DOWN;
				} else if ((actions & GameCanvas.UP_PRESSED) != 0) {
					direction = NPC.DIR_UP;
				}

				if ((actions & GameCanvas.LEFT_PRESSED) != 0
						&& !checkCollision(-1, 5) && !checkCollision(-1, 15)) {
					if (checkCollision(-1 - PLAYER_SPEED, 5) || checkCollision(-1, 15)) {
						x -= Math.min(PLAYER_SPEED, x % TILE_SIZE);
					} else {
						x -= PLAYER_SPEED;
					}
					direction = NPC.DIR_LEFT;
				} else if ((actions & GameCanvas.RIGHT_PRESSED) != 0) {
					if (!checkCollision(16, 5) && !checkCollision(16, 15)) {
						if (checkCollision(16 + PLAYER_SPEED, 5) || checkCollision(16 + PLAYER_SPEED, 15)) {
							x += Math.min(PLAYER_SPEED, TILE_SIZE - (x % TILE_SIZE));
						} else {
							x += PLAYER_SPEED;
						}
					}
					direction = NPC.DIR_RIGHT;
				} else if ((actions & GameCanvas.LEFT_PRESSED) != 0) {
					direction = NPC.DIR_LEFT;
				}
				if (climb && canClimb && !climbed) {
					if (direction != NPC.DIR_DOWN) {
						xFloat = y -= 4;
					}
					climbed = true;
				}

				xFloat = x;
				yFloat = y;

				if (map.firePressed) {
					if (animationTimer == 0 && !climbed) {
						hit: {
							int item = map.selectedInventory != -1 && inventory[map.selectedInventory] != Items.ITEM_NULL ?
									inventory[map.selectedInventory] & Items.ITEM_ID_MASK : -1;

							if (item == Items.HOE || item == Items.MOP || item == Items.BROOM) {
								map.selectedInventory = -1;
								for (int i = -1; i < 4; ++i) {
									int x = this.x / TILE_SIZE;
									int y = (this.y + 5) / TILE_SIZE;
									if (i != -1) {
										x += Game.PATH_DIR_POSITIONS[i << 1];
										y += Game.PATH_DIR_POSITIONS[(i << 1) + 1];
									}
									int idx = map.getObjectIdxAt(x, y, layer);
									int obj = map.objects[layer][idx + 1];
									if ((obj == Objects.OUTSIDE_DIRT && item == Items.HOE)
											|| (obj == Objects.FLOOR_DIRT && item != Items.HOE)) {
										if (map.fatigue >= 100) {
											Sound.playEffect(Sound.SFX_LOSE);
											dialog = "You are too fatigued";
											dialogTimer = TPS * 2;
											break hit;
										}
										moveTowards(x * TILE_SIZE, y * TILE_SIZE, 0);
										map.action = ACT_CLEANING;
										map.actionTargetX = x;
										map.actionTargetY = y;
										map.progress = 0;
										break hit;
									}
								}
								break hit;
							}
							if (item != -1) {
								int slot = map.selectedInventory;
								map.selectedInventory = -1;
								int x, y;
								switch (direction) {
								case DIR_RIGHT:
									x = 17;
									y = 8;
									break;
								case DIR_UP:
									x = 8;
									y = 3;
									break;
								case DIR_LEFT:
									x = -2;
									y = 8;
									break;
								case DIR_DOWN:
									x = 8;
									y = 17;
									break;
								default:
									break hit;
								}


								byte b = getCollision(x, y, true);
								if (b == COLL_SOLID) {
									if (item == Items.COMB) {
										inventory[slot] = Items.COMB_SHIV | Items.ITEM_DEFAULT_DURABILITY;
										break hit;
									}
									if (item == Items.TOOTHBRUSH) {
										inventory[slot] = Items.TOOTHBRUSH_SHIV | Items.ITEM_DEFAULT_DURABILITY;
										break hit;
									}
									if (item == Items.TUBE_OF_TOOTHPASTE
											|| item == Items.SHAVING_CREAM
											|| item == Items.ROLL_OF_DUCT_TAPE) {
										// TODO check for camera
										break hit;
									}
								}

								if (Game.getItemAttack(item) != 0 && weapon == Items.ITEM_NULL) {
									// equip
									weapon = inventory[slot];
									inventory[slot] = Items.ITEM_NULL;
									break hit;
								}

								break hit;
							}
							if (carry != null) {
								int x = this.x / TILE_SIZE;
								int y = (this.y + 5) / TILE_SIZE;
								if (map.solid[layer][x + y * map.width] != 0) {
									Sound.playEffect(Sound.SFX_LOSE);
								} else {
									Sound.playEffect(Sound.SFX_ENHIT); // TODO replace effect
									carry.xFloat = carry.x = x * TILE_SIZE;
									carry.yFloat = carry.y = y * TILE_SIZE;
									carry.carried = false;
									carry = null;
								}
								break hit;
							}
							if (map.interactNPC != null) {
								NPC npc = map.interactNPC;

								int dx = npc.x - this.x;
								int dy = npc.y - this.y;
								int d = dx * dx + dy * dy;
								if (d < TILE_SIZE * TILE_SIZE) {
									if (npc.health == 0) {
										Sound.playEffect(Sound.SFX_PICKUP);
										carry = npc;
										npc.carried = true;
									} else if (npc.inmate || npc.guard) {
										chaseTarget = npc;
									}
								}
							}
						}
					}
				}
				if (map.softPressed) {
					map.softPressed = false;
					interact:
					{
						if (climbed) break interact;

						// drop selected item
						if (map.selectedInventory != -1 && inventory[map.selectedInventory] != Items.ITEM_NULL) {
							int r = map.dropItem((x + 7) / TILE_SIZE, (y + 7) / TILE_SIZE, inventory[map.selectedInventory], layer);
							if (r == 0) {
								Sound.playEffect(Sound.SFX_PLIP);
								inventory[map.selectedInventory] = Items.ITEM_NULL;
							} else {
								Sound.playEffect(Sound.SFX_LOSE);
							}
							map.selectedInventory = -1;
							break interact;
						}

						int x, y;
						switch (direction) {
						case DIR_RIGHT:
							x = 17;
							y = 8;
							break;
						case DIR_UP:
							x = 8;
							y = 3;
							break;
						case DIR_LEFT:
							x = -2;
							y = 8;
							break;
						case DIR_DOWN:
							x = 8;
							y = 17;
							break;
						default:
							break interact;
						}

						byte b = getCollision(x, y, true);
						x = (x + this.x) / TILE_SIZE;
						y = (y + this.y) / TILE_SIZE;

						if (b == 0) {
							// pickup item
							int item = map.peekItem(x, y, layer);
							if (item != -1 && item != Items.ITEM_NULL) {
								if (addItem(item, true)) {
									map.pickItem(x, y, layer);
								} else {
									dialog = "Inventory full";
									dialogTimer = TPS * 2;
								}
								break interact;
							}
						}

						if (b != 0) {
							int idx = map.getObjectIdxAt(x, y, layer);
							int obj = idx == -1 ? -1 : map.objects[layer][idx + 1];

							if (b == COLL_DESK) {
								if (obj == Objects.PLAYER_DESK) {
									map.openContainer(idx);
									break interact;
								}
								Sound.playEffect(Sound.SFX_OPEN);
								map.action = ACT_SEARCHING;
								map.actionTargetX = x;
								map.actionTargetY = y;
								map.progress = 0;
								break interact;
							}
							if (b == COLL_TABLE) {
								if (obj == Objects.SERVING_TABLE
										// if there is food left
										&& (map.objects[layer][idx + 2] & 0xFF) != 2) {
									// pick food
									animation = ANIM_FOOD;
									Sound.playEffect(Sound.SFX_PICKUP);
									break interact;
								}
								if (obj == Objects.CUTLERY_TABLE) {
									map.openContainer(idx);
									break interact;
								}
								if (obj == Objects.TRAINING_INTERNET) {
									// learn
									if (map.fatigue >= 100) {
										Sound.playEffect(Sound.SFX_LOSE);
										dialog = "You are too fatigued";
										dialogTimer = TPS * 2;
										break interact;
									}
									Sound.playEffect(Sound.SFX_OPEN);
									map.action = ACT_READING;
									map.progress = 0;
									map.fatigue += 5;
									break interact;
								}
							}
							if (b == COLL_SOLID_INTERACT) {
								if (obj == Objects.TRAINING_BOOKSHELF) {
									// learn
									if (map.fatigue >= 100) {
										Sound.playEffect(Sound.SFX_LOSE);
										dialog = "You are too fatigued";
										dialogTimer = TPS * 2;
										break interact;
									}
									Sound.playEffect(Sound.SFX_OPEN);
									map.action = ACT_READING;
									map.progress = 0;
									map.fatigue += 5;
									break interact;
								}
								if (obj == Objects.CABINET) {
									// hide
									animateToX = x * TILE_SIZE;
									animateToY = y * TILE_SIZE;
									animatingInCabinet = true;
									break interact;
								}
								if (obj == Objects.PLAYER_BED) {
									xFloat = this.x = bedX * TILE_SIZE;
									yFloat = this.y = bedY * TILE_SIZE + 2;
									animation = ANIM_LYING;
									break interact;
								}
								if (obj == Objects.MEDICAL_BED) {
									xFloat = this.x = map.objects[layer][idx + 3] * TILE_SIZE;
									yFloat = this.y = (map.objects[layer][idx + 4] - 1) * TILE_SIZE + 2;
									animation = ANIM_LYING;
									break interact;
								}
								if (obj == Objects.CHAIR) {
									// sit
									animateToX = x * TILE_SIZE;
									animateToY = y * TILE_SIZE;
									direction = pathEndDir = map.solid[0][x + (y + 1) * map.width] == COLL_TABLE ? DIR_DOWN : DIR_UP;
									int seat = map.getSeatIndex(map.canteenSeatsPositions, x, y);
									if (seat != -1) {
										int tx = x * TILE_SIZE;
										int ty = y * TILE_SIZE;
										if (map.canteenSeatsPositions[seat + 1] != -1) {
											// push npc from their seat
											Sound.playEffect(Sound.SFX_ENHIT);
											int n = map.npcNum;
											for (int i = 1; i < n; ++i) {
												NPC npc = map.chars[i];
												if (npc != null && npc.sitting
														&& npc.x == tx&& npc.y == ty) {
													// TODO
													npc.aiState = AI_RESET;
													npc.aiWaitTimer = TPS;
													break;
												}
											}
										}
										map.canteenSeatsPositions[seat + 1] = (short) id;
									}
									sitting = true;
									break interact;
								}
								if (obj == Objects.JOB_DIRTY_LAUNDRY) {
									// take laundry
									addItem((rng.nextInt(2) == 0 ? Items.DIRTY_GUARD_OUTFIT : Items.DIRTY_INMATE_OUTFIT)
											| Items.ITEM_DEFAULT_DURABILITY, true);
									break interact;
								}
								if (obj == Objects.JOB_RAW_METAL) {
									// take metal
									addItem(Items.SHEET_OF_METAL | Items.ITEM_DEFAULT_DURABILITY, true);
									break interact;
								}
								if (obj == Objects.JOB_RAW_WOOD) {
									// take wood
									addItem(Items.TIMBER | Items.ITEM_DEFAULT_DURABILITY, true);
									break interact;
								}
								if (obj == Objects.JOB_CLEANING_SUPPLIES
										|| obj == Objects.JOB_GARDENING_TOOLS
										|| obj == Objects.TOILET) {
									map.openContainer(idx);
									break interact;
								}
//								if (obj == Objects.JOB_CLEANING_SUPPLIES) {
//									// take mop
//									addItem((rng.nextInt(2) == 0 ? Items.MOP : Items.BROOM)
//											| Items.ITEM_DEFAULT_DURABILITY, true);
//									break interact;
//								}
//								if (obj == Objects.JOB_GARDENING_TOOLS) {
//									// take hoe
//									addItem(Items.HOE | Items.ITEM_DEFAULT_DURABILITY, true);
//									break interact;
//								}

								if (obj == Objects.JOB_CLEAN_LAUNDRY) {
									// put clean laundry
									int i = map.selectedInventory;
									if (i != -1
											&& ((inventory[i] & Items.ITEM_ID_MASK) == Items.INMATE_OUTFIT
											|| (inventory[i] & Items.ITEM_ID_MASK) == Items.GUARD_OUTFIT)) {
										inventory[i] = Items.ITEM_NULL;
										map.selectedInventory = -1;
										if (jobQuota < MAX_JOB_QUOTA) {
											if ((jobQuota += (MAX_JOB_QUOTA / 10)) >= MAX_JOB_QUOTA) {
												// TODO
												Sound.playEffect(Sound.SFX_BUY);
												jobQuota = MAX_JOB_QUOTA;
												map.money += 20;
											}
										}
									}
									break interact;
								}

								if (obj == Objects.STASH) {
									// open stash TODO

									break interact;
								}
							}
							if (b == COLL_GYM) {
								if (map.fatigue >= 100) {
									Sound.playEffect(Sound.SFX_LOSE);
									dialog = "You are too fatigued";
									dialogTimer = TPS * 2;
									break interact;
								}
								// sit
								animateToX = x * TILE_SIZE;
								animateToY = y * TILE_SIZE;
								gymObjectIdx = idx;
								gymObject = obj;
								if (obj == Objects.TRAINING_TREADMILL) {
									animateToX -= 4;
									animateToY -= 4;
									direction = pathEndDir = DIR_RIGHT;
								} else {
									direction = pathEndDir = DIR_DOWN;
									if (obj == Objects.TRAINING_WEIGHT) {
										map.objects[LAYER_GROUND][idx + 2] = (short) (13 | (map.objects[LAYER_GROUND][idx + 2] & 0xFF00));
									}
								}
								int seat = map.getSeatIndex(map.gymPositions, x, y);
								if (seat != -1) {
									if (map.gymPositions[seat + 1] != -1) {
										// push npc from their seat
										Sound.playEffect(Sound.SFX_ENHIT);
										int n = map.npcNum;
										for (int i = 1; i < n; ++i) {
											NPC npc = map.chars[i];
											if (npc != null && npc.training
													&& npc.x == animateToX && npc.y == animateToY) {
												npc.aiState = AI_RESET;
												npc.aiWaitTimer = TPS;
												break;
											}
										}
									}
									map.gymPositions[seat + 1] = (short) id;
								}
								map.trainingRepeats = 0;
								map.trainingTimer = 0;
								map.trainingLastKey = 0;
								map.trainingBlocked = false;
								training = true;
								break interact;
							}
						}

						if (map.interactNPC != null) {
							NPC npc = map.interactNPC;

							if (npc.health <= 0) {
								// TODO loot
							} else {
								// TODO open menu
							}
						}
					}
				}
			} else if (animation == ANIM_LYING) {
				// get up from bed
				if ((actions & GameCanvas.LEFT_PRESSED) != 0) {
					if (!checkCollision(-1, 5)) {
						animation = ANIM_REGULAR;
						animateToX = x - TILE_SIZE;
						animateToY = y;
						direction = NPC.DIR_LEFT;
					}
				} else if ((actions & GameCanvas.RIGHT_PRESSED) != 0) {
					if (!checkCollision(16, 5)) {
						animation = ANIM_REGULAR;
						animateToX = x + TILE_SIZE;
						animateToY = y;
						direction = NPC.DIR_RIGHT;
					}
				}
			}
			wasTryingToMove = (actions & (GameCanvas.UP_PRESSED | GameCanvas.DOWN_PRESSED | GameCanvas.LEFT_PRESSED | GameCanvas.RIGHT_PRESSED)) != 0;
		}

		if (climbed || map.selectedInventory != -1) {
			map.interactNPC = null;
		} else if ((tick & 4) == 0 || wasTryingToMove) {
			map.interactNPC = getInteractNPC();
		}

		if (x < 0 || y < 0 || x > map.width * TILE_SIZE - TILE_SIZE || y > map.height * TILE_SIZE - TILE_SIZE) {
			map.paused = true;
			Sound.stopEffect();
			Sound.stopMusic();
			map.fadeOut = map.viewWidth >> 1;
		}

		x = Math.min(Math.max(x, 0), map.width * TILE_SIZE - TILE_SIZE);
		y = Math.min(Math.max(y, 0), map.height * TILE_SIZE - TILE_SIZE);

		if (carry != null) {
			carry.xFloat = carry.x = x;
			carry.yFloat = carry.y = y;
		}

		if (map.firePressed) {
			map.firePressed = false;
		}
		if (map.softPressed) {
			map.softPressed = false;
		}
	}

	void respawnPlayer() {
		layer = LAYER_GROUND;
		animation = ANIM_LYING;
		health = statStrength / 3;
		map.fatigue = 0;
		map.heat = 0;
		if (map.schedule == SC_LIGHTSOUT) {
			// send to cell bed
			xFloat = x = bedX * TILE_SIZE;
			yFloat = y = bedY * TILE_SIZE + 2;
		} else {
			// send to medical bed
			int obj = map.findObject(Objects.MEDICAL_BED, LAYER_GROUND, 0);
			xFloat = x = map.objects[0][obj + 3] * TILE_SIZE;
			yFloat = y = (map.objects[0][obj + 4] - 1) * TILE_SIZE + 2;
		}

		// reset outfit and weapon, remove contraband items from inventory
		outfitItem = Items.INMATE_OUTFIT | Items.ITEM_DEFAULT_DURABILITY;
		weapon = Items.ITEM_NULL;
		for (int i = 0; i < 6; ++i) {
			if (Game.isIllegal(inventory[i] & Items.ITEM_ID_MASK)) {
				inventory[i] = Items.ITEM_NULL;
			}
		}
	}

	NPC getInteractNPC() {
		NPC[] chars = map.chars;
		int n = chars.length;
		NPC bestFacing = null;
		int bestFacingDist = Integer.MAX_VALUE;

		NPC bestOther = null;
		int bestOtherDist = Integer.MAX_VALUE;

		for (int i = 1; i < n; ++i) {
			NPC other = chars[i];
			if (other == null || other == this || !canSee(other)) continue;

			int dx = other.x - x;
			int dy = other.y - y;
			int dist = dx * dx + dy * dy;

			if (dist > NPC_INTERACT_DISTANCE) continue;

			boolean facing = false;
			switch (this.direction) {
			case DIR_RIGHT:
				if (dx > 0 && Math.abs(dx) >= Math.abs(dy)) facing = true;
				break;
			case DIR_LEFT:
				if (dx < 0 && Math.abs(dx) >= Math.abs(dy)) facing = true;
				break;
			case DIR_UP:
				if (dy < 0 && Math.abs(dy) >= Math.abs(dx)) facing = true;
				break;
			case DIR_DOWN:
				if (dy > 0 && Math.abs(dy) >= Math.abs(dx)) facing = true;
				break;
			}

			if (facing) {
				if (dist < bestFacingDist) {
					bestFacing = other;
					bestFacingDist = dist;
				}
			} else if (dist < bestOtherDist) {
				bestOther = other;
				bestOtherDist = dist;
			}
		}

		return bestFacing != null ? bestFacing : bestOther;
	}

//endregion Player

}