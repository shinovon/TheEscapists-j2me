/*
Copyright (c) 2025-2026 Arman Jussupgaliyev
*/
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;

public class MapCompiler implements Constants {

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Parameters: <input path> <output path>");
			return;
		}

		try {
			process(Paths.get(args[0]), Paths.get(args[1]));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void process(Path inputPath, Path outputPath) throws Exception {
		int width = 96;
		int height = 93;
		
		int inmates = 0;
		int guards = 0;
		
		int startJob = JOB_UNEMPLOYED;
		
		Vector<int[]> zones = new Vector<>();
		
		Vector<int[]>[] objects = new Vector[4];
		for (int i = 0; i < 4; ++i) objects[i] = new Vector<>();
		
		Vector<Integer> topObjects = new Vector<>();
		
		Vector<int[]> lights = new Vector<>();
		
		Vector<int[]> roamPositions = new Vector<>();
		Vector<int[]> rollcallPositions = new Vector<>();
		Vector<int[]> canteenServingPositions = new Vector<>();
		Vector<int[]> gymPositions = new Vector<>();
		Vector<int[]> showerPositions = new Vector<>();
		
		Vector<int[]> guardRoamPositions = new Vector<>();
		Vector<int[]> guardRollcallPositions = new Vector<>();
		Vector<int[]> guardCanteenPositions = new Vector<>();
		Vector<int[]> guardGymPositions = new Vector<>();
		Vector<int[]> guardShowerPositions = new Vector<>();
		Vector<int[]> guardBeds = new Vector<>();
		
		Vector<int[]> npc = new Vector<>();

		Vector<int[]> containers = new Vector<>();
		
		byte[][] tiles = new byte[4][width * height];
		
		int[] jobs = new int[COUNT_JOBS];
		
		int npcSpawnX = 0;
		int npcSpawnY = 0;
		
		int beds = 0;

		// read
		byte[] decrypted = BlowfishCompatEncryption.decrypt(Files.readAllBytes(inputPath));
		try (InputStreamReader r = new InputStreamReader(new ByteArrayInputStream(decrypted))) {
			int c;
			StringBuffer sb = new StringBuffer();
			boolean sectionLine = false;
			boolean value = false;
			boolean tilesSection = false;
			boolean objectsSection = false;
			boolean zonesSection = false;
			boolean infoSection = false;
			boolean jobsSection = false;
			int layer = -1;
			int col = 0;
			int row = 0;
			int[] object = new int[4];
			int objectP = 0;
			int[] zone = new int[4];
			int zoneP = 0;
			String section;
			String key = null;
			while ((c = r.read()) != -1) {
				if (c == '\n' || c == '\r') {
					if (value && objectsSection) {
						if (sb.length() != 0) {
							object[objectP] = Integer.parseInt(sb.toString());
						}
						int objectId = object[2];
						int sprite, w = 1, h = 1, x = object[0], y = object[1];
						layer = object[3] - 1;

						object:
						{
							if (objectId == 0) break object;
							switch (objectId) {
							case Objects.PLAYER_DESK:
								checkLayer("player desk", objectId, x, y, layer, LAYER_GROUND);
								containers.add(new int[] { objects[LAYER_GROUND].size(), objectId, 20 });
								sprite = 0;
								break;
							case Objects.DESK:
								checkLayer("desk", objectId, x, y, layer, LAYER_GROUND);
								containers.add(new int[] { objects[LAYER_GROUND].size(), objectId, 20 });
								sprite = 1;
								break;
							case Objects.SERVING_TABLE:
								checkLayer("serving table", objectId, x, y, layer, LAYER_GROUND);
								sprite = 2;
								break;
							case Objects.DINING_TABLE:
								checkLayer("dining table", objectId, x, y, layer, LAYER_GROUND);
								sprite = 8;
								break;
							case Objects.CUTLERY_TABLE:
								checkLayer("cutlery table", objectId, x, y, layer, LAYER_GROUND);
								containers.add(new int[] { objects[LAYER_GROUND].size(), objectId, 20 });
								sprite = 9;
								break;
							case Objects.TRAINING_INTERNET:
								checkLayer("internet", objectId, x, y, layer, LAYER_GROUND);
								sprite = 10;
								break;
							case Objects.TOILET:
								checkLayer("toilet", objectId, x, y, layer, LAYER_GROUND);
								containers.add(new int[] { objects[LAYER_GROUND].size(), objectId, 3 });
								// rotate toilet depending on walls placement
								sprite = isSolidTile(tiles[layer][x + 1 + y * width]) != COLL_NONE ? 17 : 16;
								break;
							case Objects.CABLE_TV:
								checkLayer("cable tv", objectId, x, y, layer, LAYER_GROUND);
								sprite = 63;
								break;
							case Objects.PAYPHONE:
								checkLayer("payphone", objectId, x, y, layer, LAYER_GROUND);
								sprite = 25;
								break;
							case Objects.CHAIR:
							case Objects.VISITATION_GUEST_SEAT:
							case Objects.VISITATION_PLAYER_SEAT:
								checkLayer("chair", objectId, x, y, layer, LAYER_GROUND);
								sprite = 26;
								break;
							case Objects.MEDICAL_SUPPLIES:
								checkLayer("medical", objectId, x, y, layer, LAYER_GROUND);
								sprite = 21;
								break;
							case Objects.JOB_CLEANING_SUPPLIES:
								checkLayer("cleaning", objectId, x, y, layer, LAYER_GROUND);
								containers.add(new int[] { objects[LAYER_GROUND].size(), objectId, 20 });

								sprite = 20;
								break;
							case Objects.JOB_BOOK_CHEST:
								checkLayer("book chest", objectId, x, y, layer, LAYER_GROUND);
								sprite = 22;
								break;
							case Objects.JOB_DELIVERIES_RED_BOX:
								checkLayer("deliveries red", objectId, x, y, layer, LAYER_GROUND);
								sprite = 24;
								break;
							case Objects.JOB_DELIVERIES_BLUE_BOX:
								checkLayer("deliveries blue", objectId, x, y, layer, LAYER_GROUND);
								sprite = 23;
								break;
							case Objects.TRAINING_PUNCHBAG: // replace until implemented in game
							case Objects.TRAINING_PRESSUPS:
							case Objects.TRAINING_CHINUP:
							case Objects.TRAINING_WEIGHT: {
								checkLayer("training", objectId, x, y, layer, LAYER_GROUND);
								sprite = 12;

								gymPositions.add(new int[] { objectId, x, y });
								break;
							}
							case Objects.TRAINING_SPEEDBAG:
							case Objects.TRAINING_JOGGING:
							case Objects.TRAINING_SKIPPING:
							case Objects.TRAINING_TREADMILL: {
								checkLayer("training", objectId, x, y, layer, LAYER_GROUND);
								objectId = Objects.TRAINING_TREADMILL;
								sprite = 11;

								gymPositions.add(new int[] { objectId, x, y });
								break;
							}

							case Objects.DOOR_OUTSIDE:
								checkLayer("door", objectId, x, y, layer, LAYER_GROUND);
								sprite = 36;
								break;
							case Objects.DOOR_CELL:
								checkLayer("door", objectId, x, y, layer, LAYER_GROUND);
								sprite = 33;
								break;
							case Objects.DOOR_UTILITY:
							case Objects.DOOR_UTILITY_VENT:
//								checkLayer("door", objectId, x, y, layer, LAYER_GROUND);
								sprite = 38;
								break;
							case Objects.DOOR_KITCHEN:
							case Objects.DOOR_LAUNDRY:
							case Objects.DOOR_JANITOR:
							case Objects.DOOR_METALSHOP:
							case Objects.DOOR_LIBRARIAN:
							case Objects.DOOR_WOODSHOP:
							case Objects.DOOR_DELIVERIES:
							case Objects.DOOR_MAILROOM:
							case Objects.DOOR_GARDENING:
							case Objects.DOOR_TAILORSHOP:
								checkLayer("door", objectId, x, y, layer, LAYER_GROUND);
								sprite = 34;
								break;
							case Objects.DOOR_PRISON_ENTRANCE:
								checkLayer("door", objectId, x, y, layer, LAYER_GROUND);
								sprite = 35;
								break;
							case Objects.DOOR_STAFF:
								checkLayer("door", objectId, x, y, layer, LAYER_GROUND);
								sprite = 37;
								break;

							case Objects.VENT:
								checkLayer("vent", objectId, x, y, layer, LAYER_VENT);
								sprite = 80;
								break;
							case Objects.STASH:
								sprite = 39;
								break;
							case Objects.JOB_SELECTION:
								checkLayer("job selection", objectId, x, y, layer, LAYER_GROUND);
								sprite = 94;
								break;
							case Objects.CABINET:
								checkLayer("cabinet", objectId, x, y, layer, LAYER_GROUND);
								sprite = 108;
								h++;
								break;
							case Objects.VENT_SLATS:
								checkLayer("vent slats", objectId, x, y, layer, LAYER_VENT);
								// rotate vent depending on walls placement
								if (isSolidTile(tiles[layer][x + 1 + y * width]) != COLL_NONE) {
									sprite = 83;
								} else {
									sprite = 109;
									h++;
								}
								break;
							case Objects.PLAYER_BED:
								checkLayer("player bed", objectId, x, y, layer, LAYER_GROUND);
								sprite = 160;

								npc.add(new int[] { objectId, x, y });

								h++;
								y++;
								break;
							case Objects.BED:
								checkLayer("bed", objectId, x, y, layer, LAYER_GROUND);
								sprite = 163;
								h++;
								y++;


								if (beds < inmates - 1) {
									// TODO
									npc.add(new int[] { objectId, x, y });
									beds++;
								}

								break;
							case Objects.MEDICAL_BED:
								checkLayer("medical bed", objectId, x, y, layer, LAYER_GROUND);
								sprite = 166;
								h++;
								y++;
								break;
							case Objects.GUARD_BED: {
								checkLayer("guard bed", objectId, x, y, layer, LAYER_GROUND);
								guardBeds.add(new int[] { x, y });

								sprite = 168;
								h++;
								y++;
								break;
							}
							case Objects.SOLITARY_BED:
								checkLayer("solitary bed", objectId, x, y, layer, LAYER_GROUND);
								sprite = 167;
								h++;
								y++;
								break;
							case Objects.FREEZER:
								checkLayer("freezer", objectId, x, y, layer, LAYER_GROUND);
								sprite = 102;
								h++;
								y++;
								break;
							case Objects.SINK:
								checkLayer("sink", objectId, x, y, layer, LAYER_GROUND);
								sprite = 105;
								h++;
								y++;
								break;
							case Objects.OVEN:
								checkLayer("oven", objectId, x, y, layer, LAYER_GROUND);
								sprite = 128;
								h++;
								break;
							case Objects.JOB_DIRTY_LAUNDRY:
								checkLayer("dirty laundry", objectId, x, y, layer, LAYER_GROUND);
								sprite = 96;
								h++;
								y++;
								break;
							case Objects.JOB_CLEAN_LAUNDRY:
								checkLayer("clean laundry", objectId, x, y, layer, LAYER_GROUND);
								sprite = 97;
								h++;
								y++;
								break;
							case Objects.JOB_METAL_TOOLS:
								checkLayer("metal tools", objectId, x, y, layer, LAYER_GROUND);
								sprite = 140;
								h++;
								break;
							case Objects.WASHING_MACHINE:
								checkLayer("washing machine", objectId, x, y, layer, LAYER_GROUND);
								sprite = 133;
								h++;
								break;
							case Objects.JOB_RAW_METAL:
								checkLayer("raw metal", objectId, x, y, layer, LAYER_GROUND);
								sprite = 103;
								h++;
								y++;
								break;
							case Objects.JOB_PREPARED_METAL:
								checkLayer("prepared metal", objectId, x, y, layer, LAYER_GROUND);
								sprite = 104;
								h++;
								y++;
								break;
							case Objects.JOB_RAW_WOOD:
								checkLayer("raw wood", objectId, x, y, layer, LAYER_GROUND);
								sprite = 100;
								h++;
								y++;
								break;
							case Objects.JOB_PREPARED_WOOD:
								checkLayer("prepared wood", objectId, x, y, layer, LAYER_GROUND);
								sprite = 101;
								h++;
								y++;
								break;
							case Objects.JOB_FABRIC_CHEST:
								checkLayer("fabric chest", objectId, x, y, layer, LAYER_GROUND);
								sprite = 99;
								h++;
								y++;
								break;
							case Objects.JOB_CLOTHING_STORAGE:
								checkLayer("clothing storage", objectId, x, y, layer, LAYER_GROUND);
								sprite = 98;
								h++;
								y++;
								break;
							case Objects.JOB_GARDENING_TOOLS:
								checkLayer("gardening", objectId, x, y, layer, LAYER_GROUND);
								containers.add(new int[] { objects[LAYER_GROUND].size(), objectId, 20 });
								sprite = 192;
								w++;
								break;
							case Objects.GENERATOR:
								checkNotLayer("generator", objectId, x, y, layer, LAYER_VENT);
								checkNotLayer("generator", objectId, x, y, layer, LAYER_UNDERGROUND);
								sprite = 228;
								w++;
								h++;
								y++;
								break;
							case Objects.JOB_MAILROOM_FILE:
								checkLayer("mailroom file", objectId, x, y, layer, LAYER_GROUND);
								sprite = 224;
								w++;
								h++;
								y++;
								break;
							case Objects.TRAINING_BOOKSHELF:
								checkLayer("bookshelf", objectId, x, y, layer, LAYER_GROUND);
								sprite = 226;
								w++;
								h++;
								break;
							case Objects.SHOWER: {
								checkLayer("shower", objectId, x, y, layer, LAYER_GROUND);
								sprite = 208 | (1 << 12);

								showerPositions.add(new int[] {  x, y });
								break;
							}
							case Objects.CONTRABAND_DETECTOR: {
								checkLayer("detector", objectId, x, y, layer, LAYER_GROUND);
								// rotate metal detector depending on walls placement
								if (isSolidTile(tiles[layer][x + (y - 1) * width]) != COLL_NONE) {
									sprite = 106;
									h++;
								} else {
									sprite = 14;
								}

								topObjects.add(objects[LAYER_GROUND].size());
								break;
							}
							case Objects.SECURITY_CAMERA: {
								checkLayer("camera", objectId, x, y, layer, LAYER_GROUND);
								sprite = 48;

								topObjects.add(objects[LAYER_GROUND].size());
								break;
							}
							case Objects.LIGHT: {
								checkLayer("light", objectId, x, y, layer, LAYER_GROUND);
								lights.add(new int[] {x, y});
								break object;
							}

							// ai waypoints

							case Objects.AI_WP_PRISONER_GENERAL: {
								checkLayer("ai wp", objectId, x, y, layer, LAYER_GROUND);
								roamPositions.add(new int[] { x, y });
								break object;
							}
							case Objects.AI_WP_PRISONER_ROLLCALL:{
								checkLayer("ai wp", objectId, x, y, layer, LAYER_GROUND);
								rollcallPositions.add(new int[] { x, y });
								break object;
							}
							case Objects.AI_WP_PRISONER_MEALS:{
								checkLayer("ai wp", objectId, x, y, layer, LAYER_GROUND);
								canteenServingPositions.add(new int[] { x, y });
								break object;
							}

							case Objects.AI_WP_GUARD_GENERAL: {
								checkLayer("ai wp", objectId, x, y, layer, LAYER_GROUND);
								guardRoamPositions.add(new int[] { x, y });
								npc.add(new int[] { objectId, x, y });
								break object;
							}
							case Objects.AI_WP_GUARD_ROLLCALL:{
								checkLayer("ai wp", objectId, x, y, layer, LAYER_GROUND);
								guardRollcallPositions.add(new int[] { x, y });
								break object;
							}
							case Objects.AI_WP_GUARD_MEALS: {
								checkLayer("ai wp", objectId, x, y, layer, LAYER_GROUND);
								guardCanteenPositions.add(new int[] { x, y });
								break object;
							}
							case Objects.AI_WP_GUARD_EXERCISE: {
								checkLayer("ai wp", objectId, x, y, layer, LAYER_GROUND);
								guardGymPositions.add(new int[] { x, y });
								break object;
							}
							case Objects.AI_WP_GUARD_SHOWERS: {
								checkLayer("ai wp", objectId, x, y, layer, LAYER_GROUND);
								guardShowerPositions.add(new int[] { x, y });
								break object;
							}
							case Objects.AI_WP_DOCTOR_WORK: {
								checkLayer("ai wp", objectId, x, y, layer, LAYER_GROUND);
								// spawn doctor npc
								npc.add(new int[] { objectId, x, y });
								break object;
							}
							case Objects.AI_WP_EMPLOYMENT_OFFICER: {
								checkLayer("ai wp", objectId, x, y, layer, LAYER_GROUND);
								// spawn employment officer npc
								npc.add(new int[] { objectId, x, y });
								break object;
							}
							case Objects.AI_NPC_SPAWN: {
								checkLayer("ai npc spawn", objectId, x, y, layer, LAYER_GROUND);
								npcSpawnX = x;
								npcSpawnY = y;
								break object;
							}
							case Objects.PRISON_SNIPER: {
								// spawn sniper
								npc.add(new int[] { objectId, x, y });
								break object;
							}
							case Objects.LADDER_UP: {
								// TODO
								break object;
							}
							case Objects.LADDER_DOWN: {
								// TODO
								break object;
							}
							case Objects.ROOF_SPOTLIGHTS: {
								// TODO
								break object;
							}
							default:
								// unrecognized object id
								System.out.println("Unrecognized object: " + objectId + " at " + x + ' ' + y);
								break object;
							}

							objects[layer].add(new int[] { objectId, sprite, w, h, x, y });
						}

					}
					if (value && zonesSection && zone[0] != -1) {
						if (sb.length() != 0) {
							zone[zoneP] = Integer.parseInt(sb.toString());
						}
						zone: {
							int type;
							if ("Rollcall".equals(key)) {
								type = ZONE_ROLLCALL;
							} else if ("YourCell".equals(key)) {
								type = ZONE_PLAYER_CELL;
							} else if ("Cells1".equals(key)
									|| "Cells2".equals(key)
									|| "Cells3".equals(key)
									|| "Cells4".equals(key)
									|| "Cells5".equals(key)) {
								type = ZONE_INMATE_CELL;
							} else if ("Canteen".equals(key)) {
								type = ZONE_CANTEEN;
							} else if ("Gym".equals(key)) {
								type = ZONE_GYM;
							} else if ("Showers".equals(key)) {
								type = ZONE_SHOWER;
							} else if ("Laundry".equals(key)) {
								type = ZONE_LAUNDRY;
							} else if ("Kitchen".equals(key)) {
								type = ZONE_KITCHEN;
							} else if ("Woodshop".equals(key)) {
								type = ZONE_WOODSHOP;
							} else if ("Metalshop".equals(key)) {
								type = ZONE_METALSHOP;
							} else if ("Tailorshop".equals(key)) {
								type = ZONE_TAILORSHOP;
							} else if ("Deliveries".equals(key)) {
								type = ZONE_DELIVERIES;
							} else if ("Janitor".equals(key)) {
								type = ZONE_JANITOR;
							} else if ("Gardening".equals(key)) {
								type = ZONE_GARDENING;
							} else {
								break zone;
							}

							zones.add(new int[] { zone[0], zone[1], zone[2], zone[3], type });
						}
					}
					if (value && infoSection) {
						if ("Guards".equals(key)) {
							guards = Integer.parseInt(sb.toString());
						} else if ("Inmates".equals(key)) {
							inmates = Integer.parseInt(sb.toString());
						}
					}
					if (value && jobsSection) {
						if ("StartingJob".equals(key)) {
							startJob = getJob(sb.toString());
						} else if ("1".equals(sb.toString())) {
							jobs[0]++;
							jobs[getJob(key)] = 1;
						}
					}
					sb.setLength(0);
					col = 0;
					value = false;
					objectP = 0;
					zoneP = 0;
					zone[0] = -1;
					zone[1] = -1;
					zone[2] = -1;
					zone[3] = -1;
					key = null;
					continue;
				}
				if (!sectionLine && !value && c == '[') {
					sectionLine = true;
					continue;
				}
				if (sectionLine && c == ']') {
					sectionLine = false;
					tilesSection = false;
					objectsSection = false;
					zonesSection = false;
					infoSection = false;
					jobsSection = false;
					section = sb.toString();
					switch (section) {
					case "Tiles":
						layer = LAYER_GROUND;
						tilesSection = true;
						break;
					case "Vents":
						layer = LAYER_VENT;
						tilesSection = true;
						break;
					case "Roof":
						layer = LAYER_ROOF;
						tilesSection = true;
						break;
					case "Underground":
						layer = LAYER_UNDERGROUND;
						tilesSection = true;
						break;
					case "Objects":
						objectsSection = true;
						break;
					case "Zones":
						zonesSection = true;
						break;
					case "Info":
						infoSection = true;
						break;
					case "Jobs":
						jobsSection = true;
						break;
					}
					continue;
				}
				if (c == '=' && !value) {
					if (tilesSection) {
						row = Integer.parseInt(sb.toString());
					} else {
						key = sb.toString();
					}
					sb.setLength(0);
					value = true;
					continue;
				}
				if (value && tilesSection) {
					if (c == '_') {
						int t = Byte.parseByte(sb.toString());
						if (t != 0) {
							t--;
							int x = t / 25;
							int y = t - (x * 25);
							int nt = (x + y * 4) + 1;

							tiles[layer][col + row * width] = (byte) nt;
						}
						sb.setLength(0);
						col++;
						continue;
					}
				}
				if (value && objectsSection && c == 'x') {
					object[objectP++] = Integer.parseInt(sb.toString());
					sb.setLength(0);
					continue;
				}
				if (value && zonesSection && c == '_') {
					zone[zoneP++] = Integer.parseInt(sb.toString());
					sb.setLength(0);
					continue;
				}
				sb.append((char)c);
			}
		}

		// checks

		if (width == 0 || height == 0) {
			throw new Exception("no size");
		}
		if (inmates == 0) {
			throw new Exception("no inmates count");
		}
		if (inmates < 2) {
			throw new Exception("no inmates");
		}
		if (guards == 0) {
			throw new Exception("no guards count");
		}
		if (objects[0].size() == 0) {
			throw new Exception("no objects");
		}
		if (zones.size() == 0) {
			throw new Exception("no zones");
		}
		if (jobs[0] == 0) {
			throw new Exception("no jobs");
		}
		if (npcSpawnX == 0 || npcSpawnY == 0) {
			throw new Exception("no npc spawn");
		}
		if (guardRoamPositions.size() < guards) {
			throw new Exception("not enough guard waypoints");
		}
		if (guardRollcallPositions.size() < 3) {
			throw new Exception("not enough guard rollcall positions");
		}
		if (guardCanteenPositions.size() < 3) {
			throw new Exception("not enough guard canteen positions");
		}
		if (guardGymPositions.size() < 3) {
			throw new Exception("not enough guard gym positions");
		}
		if (guardShowerPositions.size() < 3) {
			throw new Exception("not enough guard shower positions");
		}
		if (gymPositions.size() < inmates) {
			throw new Exception("not enough gym objects");
		}
		if (showerPositions.size() < inmates) {
			throw new Exception("not enough shower positions");
		}
		if (containers.size() < inmates) {
			throw new Exception("not enough containers");
		}

		// write

		try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(outputPath))) {
			// version
			out.writeInt(1);

			out.writeByte(width);
			out.writeByte(height);

			out.writeByte(inmates);
			out.writeByte(guards);

			out.writeByte(npcSpawnX);
			out.writeByte(npcSpawnY);

			// jobs
			out.writeByte(startJob);
			System.out.println(jobs[0] + " jobs");
			for (int i = 0; i < COUNT_JOBS; ++i) {
				out.writeByte(jobs[i]);
			}

			// zones
			System.out.println(zones.size() + " zones");
			out.writeByte(zones.size());
			for (int[] zone : zones) {
				out.writeShort(zone[0]);
				out.writeShort(zone[1]);
				out.writeShort(zone[2]);
				out.writeShort(zone[3]);
				out.writeByte(zone[4]);
			}

			// tiles
			for (int layer = 0; layer < 4; ++layer) {
				int n = 0;
				for (int i = tiles[layer].length - 1; i >= 0; --i) {
					if (tiles[layer][i] != 0) {
						n = i + 1;
						break;
					}
				}
				out.writeShort(n);
				for (int i = 0; i < n; ++i) {
					out.writeByte(tiles[layer][i]);
				}
			}

			// objects
			for (int layer = 0; layer < 4; ++layer) {
				System.out.println(objects[layer].size() + " objects on " + layerStrings[layer] + " layer");
				out.writeShort(objects[layer].size());
				for (int[] object : objects[layer]) {
					out.writeByte(object[0]);
					out.writeShort(((object[1]) | ((object[2] & 0x3) << 8) | ((object[3] & 0x3) << 10)));
					out.writeByte(object[4]);
					out.writeByte(object[5]);
				}
			}

			// lights
			out.writeShort(lights.size());
			for (int[] arr : lights) {
				out.writeByte(arr[0]);
				out.writeByte(arr[1]);
			}

			// topObjects
			out.writeShort(topObjects.size());
			for (Integer idx : topObjects) {
				out.writeShort((int) idx << 2);
			}

			// waypoints

			out.writeShort(roamPositions.size());
			for (int[] pos : roamPositions) {
				out.writeByte(pos[0]);
				out.writeByte(pos[1]);
			}

			out.writeShort(rollcallPositions.size());
			for (int[] pos : rollcallPositions) {
				out.writeByte(pos[0]);
				out.writeByte(pos[1]);
			}

			out.writeShort(canteenServingPositions.size());
			for (int[] pos : canteenServingPositions) {
				out.writeByte(pos[0]);
				out.writeByte(pos[1]);
			}

			out.writeShort(showerPositions.size());
			for (int[] pos : showerPositions) {
				out.writeByte(pos[0]);
				out.writeByte(pos[1]);
			}



			out.writeShort(guardRoamPositions.size());
			for (int[] pos : guardRoamPositions) {
				out.writeByte(pos[0]);
				out.writeByte(pos[1]);
			}

			out.writeShort(guardRollcallPositions.size());
			for (int[] pos : guardRollcallPositions) {
				out.writeByte(pos[0]);
				out.writeByte(pos[1]);
			}

			out.writeShort(guardCanteenPositions.size());
			for (int[] pos : guardCanteenPositions) {
				out.writeByte(pos[0]);
				out.writeByte(pos[1]);
			}

			out.writeShort(guardGymPositions.size());
			for (int[] pos : guardGymPositions) {
				out.writeByte(pos[0]);
				out.writeByte(pos[1]);
			}

			out.writeShort(guardShowerPositions.size());
			for (int[] pos : guardShowerPositions) {
				out.writeByte(pos[0]);
				out.writeByte(pos[1]);
			}



			out.writeShort(guardBeds.size());
			for (int[] pos : guardBeds) {
				out.writeByte(pos[0]);
				out.writeByte(pos[1]);
			}



			out.writeShort(gymPositions.size());
			for (int[] pos : gymPositions) {
				out.writeByte(pos[0]);
				out.writeByte(pos[1]);
				out.writeByte(pos[2]);
			}



			// npc
			System.out.println(npc.size() + " npc spawn points");
			out.writeShort(npc.size());
			for (int[] arr : npc) {
				out.writeByte(arr[0]);
				out.writeByte(arr[1]);
				out.writeByte(arr[2]);
			}

			System.out.println(containers.size() + " containers");
			out.writeShort(containers.size());
			for (int[] arr : containers) {
				out.writeShort(arr[0]);
				out.writeByte(arr[1]);
				out.writeByte(arr[2]);
			}

			out.writeShort(0xFFEF);
		}
	}
	
	private static final String[] layerStrings = {
			"ground",
			"vent",
			"roof",
			"underground",
	};
	
	private static void checkLayer(String object, int id, int x, int y, int layer, int requiredLayer) throws Exception {
		if (layer != requiredLayer) {
			throw new Exception(object + " (" + id + ") not on " + layerStrings[requiredLayer] + " layer at " + x + " " + y + " " + layerStrings[layer]);
		}
	}
	
	private static void checkNotLayer(String object, int id, int x, int y, int layer, int requiredLayer) throws Exception {
		if (layer == requiredLayer) {
			throw new Exception(object + " (" + id + ") not on " + layerStrings[requiredLayer] + " layer at " + x + " " + y + " " + layerStrings[layer]);
		}
	}
	
	private static int getJob(String s) {
		if ("Laundry".equals(s)) {
			return JOB_LAUNDRY;
		}
		if ("Gardening".equals(s)) {
			return JOB_GARDENING;
		}
		if ("Janitor".equals(s)) {
			return JOB_JANITOR;
		}
		if ("Woodshop".equals(s)) {
			return JOB_WOODSHOP;
		}
		if ("Metalshop".equals(s)) {
			return JOB_METALSHOP;
		}
		if ("Kitchen".equals(s)) {
			return JOB_KITCHEN;
		}
		if ("Deliveries".equals(s)) {
			return JOB_DELIVERIES;
		}
		if ("Tailor".equals(s)) {
			return JOB_TAILOR;
		}
		if ("Mailman".equals(s)) {
			return JOB_MAILMAN;
		}
		if ("Library".equals(s)) {
			return JOB_LIBRARY;
		}
		
		return JOB_UNEMPLOYED;
	}
	
	

	static byte isSolidTile(byte tile) {
		switch (tile) {
		case 4:
		case 6:
		case 7:
		case 8:
		case 10:
		case 11:
		case 12:
		case 14:
		case 15:
		case 16:
		case 18:
		case 19:
		case 20:
		case 21:
		case 22:
		case 25:
		case 26:
		case 27:
		case 28:
		case 29:
		case 30:
		case 31:
		case 32:
		case 33:
		case 36:
		case 37:
		case 38:
		case 40:
		case 41:
		case 42:
		case 45:
		case 46:
		case 49:
		case 50:
		case 52:
		case 53:
		case 54:
		case 55:
		case 56:
		case 57:
		case 58:
		case 60:
		case 61:
		case 62:
		case 64:
		case 65:
		case 66:
		case 68:
		case 77:
		case 79:
		case 81:
		case 83:
		case 85:
		case 87:
		case 89:
		case 91:
		case 92:
		case 93:
		case 95:
		case 96:
		case 97:
		case 99:
		case 100:
			return COLL_SOLID;
		case 23:
		case 34:
			return COLL_SOLID_TRANSPARENT;
		}
		return COLL_NONE;
	}
	
}
