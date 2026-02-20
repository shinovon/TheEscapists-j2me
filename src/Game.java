/*
Copyright (c) 2024-2026 Arman Jussupgaliyev
*/
import com.nokia.mid.ui.DirectGraphics;
import com.nokia.mid.ui.DirectUtils;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.lcdui.game.TiledLayer;
import javax.microedition.m3g.*;
import javax.microedition.rms.RecordStore;
import java.io.*;
import java.util.Vector;

public class Game extends GameCanvas implements Runnable, Constants {

// region Canvas

	static final String[] scheduleStrings = {
			"Lights out",
			"Morning Rollcall",
			"Breakfast",
			"Work Period",
			"Mid-day Rollcall",
			"Afternoon Free Time",
			"Evening Meal",
			"Exercise Period",
			"Shower Block",
			"Evening Free Time",
			"Evening Rollcall",
			"Lockdown"
	};

	static final String[] jobStrings = {
			"Unemployed",
			"Laundry",
			"Kitchen",
			"Woodshop",
			"Metalshop",
			"Tailorshop",
			"Deliveries",
			"Janitor",
			"Mailman",
			"Librarian",
	};

	Image buffer;
	Graphics bufferGraphics;
	int[] rgb;
	int[] scaleBuffer;
	int viewWidth, viewHeight;

	Graphics canvasGraphics;
	int canvasWidth, canvasHeight;

	int fps, tps;
	long globalCounter;

	boolean paused;
	boolean pausedOverlay;
	boolean mapLoaded;
	boolean wasPaused;
	boolean noTextures;

	float x, y;

	int screen;

	Image bgImg;
	float fadeIn, fadeOut;
	float ingameFadeIn, ingameFadeOut;

	// 0: init, 1: text, 2: menu, 3: game, 4: paused, 5: settings, 6: loading, 7: won
	int state = 0;
	boolean exiting;
	boolean mapError;

	boolean noScaling = true;
	boolean supportsAlpha;

	// player
	int heat;
	int fatigue;
	int money = 10;
	boolean firePressed, softPressed;
	int keyStates;
	int selectedInventory = -1;
	int lastSelectedInventory = -1;
	int trainingTimer = 0;
	int trainingLastKey;
	int trainingRepeats;
	boolean trainingBlocked;
	boolean playerSeenByGuards;
	int progress;
	boolean sendToSolitary;
	NPC interactNPC;
	int action = NPC.ACT_NONE;
	int actionTargetX, actionTargetY;

	// ui
	int containerOpen = -1;
	NPC inventoryOpen;
	int note = -1;
	boolean toilet;
	String[] noteText;
	int selectedSlot; // negative is player's inventory

	boolean debugFreecam;

	// settings
	int selectedSetting;
	static boolean use3D = false; //USE_M3G;
	static boolean enableShadows = DRAW_SHADOWS;
	static boolean altControls;

	Game() {
		super(false);
		if (LOGGING) Profiler.initLogs();
		setFullScreenMode(true);

		initBuffer();
	}

	void initBuffer() {
		if (!BUFFER_SCREEN) {
			viewWidth = getWidth();
			viewHeight = getHeight();
			return;
		}
		boolean album = getWidth() > getHeight();
		buffer = Image.createImage(viewWidth = album ? 320 : 240, viewHeight = album ? 240 : 320);
		bufferGraphics = buffer.getGraphics();
	}

	void drawGame() {
		Profiler.beginFrameSection(Profiler.FRAME_RENDER);
		Graphics g = BUFFER_SCREEN ? bufferGraphics : canvasGraphics;

		int w = viewWidth, h = viewHeight;

		if (state == 0) {
			g.setColor(0x231F20);
			g.fillRect(0, 0, viewWidth, viewHeight);
			if (noTextures) {
				String s = "Missing textures";
				drawText(g, s, (w - textWidth(s, FONT_REGULAR)) >> 1, h >> 1, FONT_REGULAR);
				return;
			}
			g.drawImage(bgImg, (viewWidth - bgImg.getWidth()) >> 1, (viewHeight - bgImg.getHeight()) >> 1, 0);
			return;
		}

		g.setColor(0);
		g.fillRect(0, 0, w, h);

		if (state == 1) {
			// logo
//			drawText(g, "asdfsdfas", 60, 20);
		} else if (state == 2) {
			// title screen
			g.drawImage(bgImg, (viewWidth - bgImg.getWidth()) >> 1, (viewHeight - bgImg.getHeight()) >> 2, 0);
			String s = "Press fire to start";
			drawText(g, s, (w - textWidth(s, FONT_BOLD)) >> 1, h - 60, FONT_BOLD);

			s = "Press left soft for settings";
			drawText(g, s, (w - textWidth(s, FONT_REGULAR)) >> 1, h - 40, FONT_REGULAR);
		} else if (state == 6) {
			// loading
			String s = mapError ? "Error loading map" : "Loading";
			drawText(g, s, (w - textWidth(s, FONT_REGULAR)) >> 1, h >> 1, FONT_REGULAR);
		} else if (state == 4) {
			// paused
			fontColor = FONT_COLOR_ORANGE;
			String s = "GAME PAUSED";
			drawText(g, s, (w - textWidth(s, FONT_BOLD)) >> 1, 40, FONT_BOLD);
			fontColor = FONT_COLOR_GREY_B4;

			s = "Press fire to resume";
			drawText(g, s, (w - textWidth(s, FONT_BOLD)) >> 1, h - 40, FONT_BOLD);
		} else if (state == 5) {
			// settings
			int i = 0;
			fontColor = FONT_COLOR_ORANGE;
			String s = "Settings";
			drawText(g, s, (w - textWidth(s, FONT_BOLD)) >> 1, 20, FONT_BOLD);

			if (!NO_SFX) {
				fontColor = selectedSetting == i ? FONT_COLOR_WHITE : FONT_COLOR_GREY_B4;
				drawText(g, "SFX volume: ".concat(Integer.toString(Sound.volumeSfx)), 40, 60 + i * 12, FONT_REGULAR);
				i++;
			}

			fontColor = selectedSetting == i ? FONT_COLOR_WHITE : FONT_COLOR_GREY_B4;
			drawText(g, "Music volume: ".concat(Integer.toString(Sound.volumeMusic)), 40, 60 + i * 12, FONT_REGULAR);
			i++;

			if (DRAW_SHADOWS && supportsAlpha) {
				fontColor = selectedSetting == i ? FONT_COLOR_WHITE : FONT_COLOR_GREY_B4;
				drawText(g, "Shadows: ".concat(enableShadows ? "On" : "Off"), 40, 60 + i * 12, FONT_REGULAR);
				i++;
			}

			if (USE_M3G) {
				fontColor = selectedSetting == i ? FONT_COLOR_WHITE : FONT_COLOR_GREY_B4;
				drawText(g, "Light effects: ".concat(use3D ? "On" : "Off"), 40, 60 + i * 12, FONT_REGULAR);
				i++;
			}

			fontColor = selectedSetting == i ? FONT_COLOR_WHITE : FONT_COLOR_GREY_B4;
			drawText(g, "Alternative controls: ".concat(altControls ? "On" : "Off"), 40, 60 + i * 12, FONT_REGULAR);
			i++;
		} else if (state == 7) {
			// escaped
			fontColor = FONT_COLOR_ORANGE;
			String s = "ESCAPED";
			drawText(g, s, (w - textWidth(s, FONT_BOLD)) >> 1, 40, FONT_BOLD);
			fontColor = FONT_COLOR_GREY_B4;

			s = "Press any key to exit";
			drawText(g, s, (w - textWidth(s, FONT_BOLD)) >> 1, h - 40, FONT_BOLD);
		} else if (mapLoaded && note != NOTE_SOLITARY) {
			// game
			int x = (int) this.x, y = (int) this.y;
			int layer = player.layer;
			if (layer == LAYER_ROOF) {
				paintMap(g, x, y, w, h, LAYER_GROUND);
			} else if (layer == LAYER_VENT) {
				paintMap(g, x, y, w, h, LAYER_GROUND);
			}
			paintMap(g, x, y, viewWidth, viewHeight, layer);
			if (player.climbed) {
				paintMap(g, x, y, w, h, LAYER_VENT);
			}
		}

		if (ingameFadeIn > 0) {
			g.setColor(0);
			g.fillRect(0, 0, (int) ingameFadeIn, h);
			g.fillRect(w - (int) ingameFadeIn, 0, (w >> 1), h);
		} else if (ingameFadeOut > 0) {
			g.setColor(0);
			g.fillRect(0, 0,  (w >> 1) - (int) ingameFadeOut, h);
			g.fillRect((w >> 1) + (int) ingameFadeOut, 0, w, h);
		}

		Profiler.beginFrameSection(Profiler.FRAME_HUD);

		if (state == 3 && mapLoaded) {
			// HUD
			{
				fontColor = FONT_COLOR_WHITE;
	//			drawText(g, "$ " + money, 0, 0, FONT_REGULAR);
	//			drawText(g, "HP " + player.health, 0, 11, FONT_REGULAR);
	//			drawText(g, "HEAT " + heat, 0, 22, FONT_REGULAR);
	//			drawText(g, "FATIGUE " + fatigue, 0, 33, FONT_REGULAR);
				int n;
				int x;
				NPC player = this.player;
				char[] s = charBuffer;

				// money
				g.drawRegion(hudSymbolsTexture, 90, 0, 9, 11, 0, 4, 2, 0);
				n = intToCharBuffer(money, 0);
				x = 13;
				for (int i = 0; i < n; i++) {
					g.drawRegion(hudSymbolsTexture, (s[i] - '0') * 9, 0, 9, 11, 0, x, 2, 0);
					x += 9;
				}

				// health
				g.drawRegion(hudSymbolsTexture, 90, 11, 9, 11, 0, 3, 14, 0);
				n = intToCharBuffer(player.health, 0);
				x = 13;
				for (int i = 0; i < n; i++) {
					g.drawRegion(hudSymbolsTexture, (s[i] - '0') * 9, 11, 9, 11, 0, x, 14, 0);
					x += 9;
				}

				// heat
				g.drawRegion(hudSymbolsTexture, 90, 22, 9, 11, 0, 3, 26, 0);
				n = intToCharBuffer(heat, 0);
				x = 13;
				for (int i = 0; i < n; i++) {
					g.drawRegion(hudSymbolsTexture, (s[i] - '0') * 9, 22, 9, 11, 0, x, 26, 0);
					x += 9;
				}
				g.drawRegion(hudSymbolsTexture, 100, 22, 9, 11, 0, x, 26, 0);

				// fatigue
				g.drawRegion(hudSymbolsTexture, 90, 33, 9, 11, 0, 3, 38, 0);
				n = intToCharBuffer(fatigue, 0);
				x = 13;
				for (int i = 0; i < n; i++) {
					g.drawRegion(hudSymbolsTexture, (s[i] - '0') * 9, 33, 9, 11, 0, x, 38, 0);
					x += 9;
				}
				g.drawRegion(hudSymbolsTexture, 100, 33, 9, 11, 0, x, 38, 0);

				// general debug
				fontColor = FONT_COLOR_WHITE;
				// fps
				n = intToCharBuffer(fps, 0);
				drawText(g, s, 0, 55, FONT_REGULAR);
				// tps
				intToCharBuffer(tps, 0);
				drawText(g, s, n * 7 + 4, 55, FONT_REGULAR);
				// used heap
				Runtime r = Runtime.getRuntime();
				intToCharBuffer((int) (r.totalMemory() - r.freeMemory()) / 1024, 0);
				drawText(g, s, 0, 66, FONT_REGULAR);

				// bottom

				{
					g.setColor(0x333333);
					g.fillRect(0, h - 17, w, 17);

	//				int bw = w - 22;
	//				int bw = w;
	//				g.setColor(0);
	//				g.fillRect(0, h - 17, bw, 17);
	//				g.setColor(0x333333);
	//				g.fillRect(2, h - 15, bw - 4, 13);
	//				g.setColor(0x535353);
	//				g.drawLine(1, h - 15, 1, h - 3);
	//				g.drawLine(2, h - 16, bw - 3, h - 16);
	//				g.setColor(0x1F1F1F);
	//				g.drawLine(bw - 2, h - 15, bw - 2, h - 3);
	//				g.drawLine(2, h - 2, bw - 3, h - 2);
				}


	//			g.setColor(0);
	//			g.drawLine(w - 23, 0, w - 23, h);

				// schedule
				StringBuffer stringBuffer = Game.stringBuffer;
				stringBuffer.setLength(0);

				putDigitsToCharBuffer(time / 60, 0);
				charBuffer[2] = ':';
				putDigitsToCharBuffer(time % 60, 3);
				charBuffer[5] = ' ';
				charBuffer[6] = '-';
				charBuffer[7] = ' ';

				stringBuffer.append(scheduleStrings[schedule])
						.append(" (Day ")
						.append(day + 1)
						.append(')');

				n = stringBuffer.length();
				stringBuffer.getChars(0, n, charBuffer, 8);
				s[n + 8] = 0;
				fontColor = FONT_COLOR_GREY_23;
				drawText(g, s, 5, h - 14, FONT_REGULAR);
				drawText(g, s, 5, h - 12, FONT_REGULAR);
				drawText(g, s, 4, h - 13, FONT_REGULAR);
				drawText(g, s, 6, h - 13, FONT_REGULAR);
				fontColor = FONT_COLOR_GREY_C0;
				drawText(g, s, 5, h - 13, FONT_REGULAR);

				progressbar:
				{
					int t;
					String a;
					if (player.training) {
						a = "Repeats: " + trainingRepeats;
						t = (trainingTimer * 50) / 40;
					} else if (action != NPC.ACT_NONE) {
						switch (action) {
						case NPC.ACT_READING:
							a = "Reading";
							break;
						case NPC.ACT_CLEANING:
							a = "Cleaning";
							break;
						case NPC.ACT_SEARCHING:
							a = "Searching";
							break;
						case NPC.ACT_CHIPPING:
							a = "Chipping";
							break;
						case NPC.ACT_DIGGING:
							a = "Digging";
							break;
						default:
							break progressbar;
						}
						t = (progress * 50) / (TPS * 2);
					} else if (schedule == SC_WORK_PERIOD && player.job != JOB_UNEMPLOYED) {
						a = "Job quota";
						t = (player.jobQuota * 50) / MAX_JOB_QUOTA;
					} else {
						break progressbar;
					}

					g.setColor(0x333333);
					g.fillRect(4, h - 42, 67, 23);
					g.setColor(0);
					g.drawRect(4, h - 42, 66, 22);
					g.setColor(0x4E4E4E);
					g.drawRect(11, h - 29, 51, 6);
					g.setColor(0x1D1D1D);
					g.fillRect(12, h - 28, 50, 5);

					fontColor = FONT_COLOR_GREY_C0;
					drawText(g, a, 11, h - 39, FONT_REGULAR);

					g.setColor(0x4172DC);
					if (t > 50) t = 50;
					if (t > 0) g.fillRect(12, h - 28, t, 5);
				}
				// side

//				g.setColor(0x0F0F0F);
//				g.fillRect(w - 22, 0, 22, h);

				// inventory items
				x = w - 21;
				for (int i = 0; i < 6; ++i) {
					int y = i * 22 + 1;
					int item = player.inventory[i] & Items.ITEM_ID_MASK;
//					g.setColor(0x434343);
//					g.fillRect(x + 1, y + 1, 18, 18);
//					g.setColor(selectedInventory == i ? 0x97479B : 0x1D1D1D);
//					g.drawRect(x, y, 19, 19);
					if (selectedInventory == i) {
						g.setColor(0x97479B);
						g.drawRect(x, y, 19, 19);
					}
					if (player.inventory[i] == Items.ITEM_NULL)
						continue;
					g.drawRegion(itemsTexture, (item % TILE_SIZE) * TILE_SIZE, (item / TILE_SIZE) * TILE_SIZE, TILE_SIZE, TILE_SIZE, 0, x + 3, y + 3, 0);
				}
			}

			// overlays
			if (note != -1) {
				pausedOverlay = true;

				int nw = Math.min(240, (w * 4) / 5);
				int nh = Math.min(180, (h * 4) / 5);
				int nx = (w - nw) >> 1;
				int ny = (h - nh) >> 1;

				g.setColor(0xEBEBEB);
				g.fillRect(nx, ny, nw, nh);
				g.setColor(0x000000);
				g.drawRect(nx, ny, nw - 1, nh - 1);
				g.setColor(0xFFFFFE);
				g.drawRect(nx + 1, ny + 1, nw - 3, nh - 3);

				fontColor = FONT_COLOR_DARKBLUE;

				// note messages
				// TODO scrolling on smaller screens
				String[] a = noteText;
				int tx = 9;
				int ty = 16;
				if (a == null) {
					String t;
					switch (note) {
					case NOTE_WELCOME:
						switch (map) {
						case MAP_PERKS:
							t = "Dear ".concat(player.name).concat("\n\nWelcome to Center Perks - the most comfortable low security prison in the county. On behalf of all the staff here we wish you a happy and relaxing visit!\n\nShould you get bored of the complimentary cable TV, we pride ourselves in many other engaging activities around the grounds.");
							break;
						case MAP_STALAGFLUCHT:
							t = "Sent me another one have they?..\n\nListen ".concat(player.name).concat(", I don't have to remind you that Stalag Flucht is famous for housing inmates with a record of escapism, so if you're planning on getting out of this one, think again!\n\nNow get yourself settled in, it's going to be a cold, long winter.");
							break;
						case MAP_SHANKTONSTATEPEN:
							t = "Welcome to Shankton State Pen, your new home for the foreseeable future.\n\nSince I've been warden we've had a few daring escapists among us, but they were promptly scooped back up and punished. No one escapes on my watch, so don't get any ideas!\n\nIf you forget any of the rules around here, the guards batons will be only too glad to remind you!";
							break;
						case MAP_JUNGLE:
							t = "Welcome to the jungle!\n\nSociety has declared you a menace, so we've put you far away from any trace of it.\n\nBefore you even entertain the thought of escaping, let me remind you that even if by some remote chance you make it past the fence, the wall, the perimeter jeeps and the guard checkpoint, there's no surviving out in the wild beyond...";
							break;
						case MAP_SANPANCHO:
							t = "This is the notorious San Pancho, the roughest, toughest and downright nastiest prison south of the border.\n\nThe blistering heat and claustrophobic conditions here turns our inmates angry and violent.\n\nEven the guards daren't enter!";
							break;
						case MAP_IRONGATE:
							t = "Listen here maggot.\n\nYou know why you're here so no point crying about it. Generally feared as the highest security prison ever, HMP Irongate is where you'll live out the rest of your meaningless existence.\n\nEscape you say? Don't make me laugh! The handful of idiots who tried met a watery demise trying to swim off the island. Still, chin up eh?";
							break;
						default:
							t = "";
							break;
						}
						break;
					case NOTE_JOB_LOST:
						t = "Due to your sheer incompetence and inability to reach the quotas we've set, we've taken away your job.\n\nOnce you pull yourself together and decide to try harder you may reapply at the job board.";
						break;
					case NOTE_SOLITARY:
						Sound.playEffect(Sound.SFX_RUMBLE);
						ingameFadeIn = Integer.MAX_VALUE;
						t = "Nice try ".concat(player.name).concat("\n\nAs punishment I'm placing you in solitary for a few days, hopefully it'll teach you a hard lesson!");
						break;
					case NOTE_RIOT:
						t = "Okay ".concat(player.name).concat(", you've made your point!\n\nThe main prison gate is now unlocked as requested, just please.. don't hurt anyone else!");
						break;
					default:
						t = "Error!";
						break;
					}
					a = noteText = getStringArray(t, nw - tx * 2, FONT_REGULAR);
				}
				int n = a.length;
				for (int i = 0; i < n; ++i) {
					drawText(g, a[i], nx + tx, ny + ty, FONT_REGULAR);
					ty += fontCharHeight[FONT_REGULAR];
				}

				fontColor = FONT_COLOR_GREY_B4;
				String t = "PRESS FIRE TO CONTINUE";
				drawText(g, t, (w - textWidth(t, FONT_REGULAR)) >> 1, ny + nh - 16, FONT_REGULAR);
			} else if (inventoryOpen != null) {
				// looting npc
				pausedOverlay = true;
			} else if (containerOpen != -1) {
				// desk open
				pausedOverlay = true;

				int idx = containerOpen;
				int[] containers = this.containers;
				Image itemsImg = itemsTexture;

				if (toilet) {
					// TODO toilet gui
				} else {
					int nw = 120;
					int nh = 110;
					int nx = (w - nw) >> 1;
					int ny = (h - nh) >> 1;

					g.setColor(0x333333);
					g.fillRect(nx, ny, nw, nh);
					g.setColor(0);
					g.drawRect(nx, ny, nw - 1, nh -1);

					fontColor = FONT_COLOR_GREY_7F;
					String s = "Desk";
					drawText(g, s, (w - textWidth(s, FONT_REGULAR)) >> 1, ny + 5, FONT_REGULAR);

					int y = ny + 18;
					for (int row = 0; row < 4; ++row) {
						for (int col = 0; col < 5; ++col) {
							int x = nx + 6 + 22 * col;
							int i = (col + row * 5);

							g.setColor(0x212121);
							g.fillRect(x + 1, y + 1, 18, 18);
							g.setColor(selectedSlot == i ? 0xFFFFFF : 0x0F0F0F);
							g.drawRect(x, y, 19, 19);

							int item = containers[idx + 3 + i];
							if (item != Items.ITEM_NULL) {
								item &= Items.ITEM_ID_MASK;
								g.drawRegion(itemsImg, (item % TILE_SIZE) * TILE_SIZE, (item / TILE_SIZE) * TILE_SIZE, TILE_SIZE, TILE_SIZE, 0, x + 2, y + 2, 0);


								if (selectedSlot == i) {
									String t = getItemName(item);
									int tw = textWidth(t, FONT_REGULAR);
									fontColor = isIllegal(item) ? FONT_COLOR_RED : FONT_COLOR_GREEN;
									g.setColor(0x212121);
									g.fillRect(x + 8 - (tw >> 1) - 3, y - 18, tw + 6, 15);
									g.setColor(0);
									g.drawRect(x + 8 - (tw >> 1) - 3, y - 18, tw + 6, 15);
									drawText(g, t, x + 8 - (tw >> 1), y - 15, FONT_REGULAR);
									fontColor = FONT_COLOR_WHITE;
								}
							}
						}
						y += 22;
					}
				}
			} else {
				pausedOverlay = false;
			}
		}

		if (PROFILER && state == 3) {
			g.setColor(0xABABAB);
			g.fillRect(0, h - 30, 176, 2);
			int t = (int) (Profiler.frameEndRes - Profiler.frameStartRes);

			if (t != 0) {
				int x = 0;
				for (int i = 0; i < Profiler.frameSectionsRes.length; ++i) {
					int sw = (((int) Profiler.frameSectionsRes[i] * 176) / t);
					g.setColor(Profiler.frameColors[i]);
					g.fillRect(x, h - 30, sw, 2);
					x += sw;
				}
			}

			g.setColor(0xABABAB);
			g.fillRect(0, h - 24, 176, 2);
			t = (int) (Profiler.renderEndRes - Profiler.renderStartRes);
			if (t != 0) {
				int x = 0;
				for (int i = 0; i < Profiler.renderSectionsRes.length; ++i) {
					int sw = (((int) Profiler.renderSectionsRes[i] * 176) / t);
					g.setColor(Profiler.renderColors[i]);
					g.fillRect(x, h - 24, sw, 2);
					x += sw;
				}
			}
		}

		if (LOGGING && SCREEN_LOGS) {
			fontColor = FONT_COLOR_WHITE;
			String[] logs = Profiler.logs;
			if (logs != null) {
				for (int i = 0; i < logs.length; ++i) {
					if (logs[i] == null) continue;
					drawText(g, logs[i], 4, i * 10, FONT_REGULAR);
				}
			}
		}

		if (fadeIn > 0) {
			g.setColor(0);
			g.fillRect(0, 0, (int) fadeIn, h);
			g.fillRect(w - (int) fadeIn, 0, (w >> 1), h);
		} else if (fadeOut > 0) {
			g.setColor(0);
			g.fillRect(0, 0,  (w >> 1) - (int) fadeOut, h);
			g.fillRect((w >> 1) + (int) fadeOut, 0, w, h);
		}
	}

	void drawScreen() {
		if (BUFFER_SCREEN && bufferGraphics == null) return;

		Graphics g = canvasGraphics;
		int w = canvasWidth, h = canvasHeight;
		int vw = viewWidth, vh = viewHeight;
		if (g == null || w == 0 || h == 0) {
			canvasGraphics = g = getGraphics();
			canvasWidth = w = getWidth();
			canvasHeight = h = getHeight();
			if (!BUFFER_SCREEN) {
				viewWidth = w;
				viewHeight = h;
			}
		}


		if (!BUFFER_SCREEN) {
			drawGame();
		} else if (w == vw && h == vh || noScaling) {
			Profiler.beginFrameSection(Profiler.FRAME_BLIT);
			g.setColor(0);
			g.fillRect(0, 0, w, h);
			g.drawImage(buffer, (w - vw) >> 1, (h - vh) >> 1, 0);
		} else {
			Profiler.beginFrameSection(Profiler.FRAME_BLIT);
			// scaling
			int[] rgb = this.rgb;
			int[] scaleBuffer = this.scaleBuffer;
			if (rgb == null || rgb.length < w * h) {
				this.rgb = rgb = new int[w * h];
			}
			if (scaleBuffer == null || scaleBuffer.length < vw * vh) {
				this.scaleBuffer = scaleBuffer = new int[vw * vh];
			}

			buffer.getRGB(scaleBuffer, 0, vw, 0, 0, vw, vh);
			int YD = (vh / h) * vw - vw;
			int YR = vh % h;
			int XD = vw / w;
			int XR = vw % w;
			int outOffset = 0;
			int inOffset = 0;

			for (int y = h, YE = 0; y > 0; y--) {
				for (int x = w, XE = 0; x > 0; x--) {
					rgb[outOffset++] = scaleBuffer[inOffset];
					inOffset += XD;
					XE += XR;
					if (XE >= w) {
						XE -= w;
						inOffset++;
					}
				}
				inOffset += YD;
				YE += YR;
				if (YE >= h) {
					YE -= h;
					inOffset += vw;
				}
			}
			g.drawRGB(rgb, 0, w, 0, 0, w, h, false);
		}

		Profiler.beginFrameSection(Profiler.FRAME_FLUSH);
		flushGraphics();
	}

	public void keyPressed(int key) {
		super.keyPressed(key);
		try {
			int gameAction = 0;
			try {
				gameAction = getGameAction(key);
			} catch (Exception ignored) {}
			if (mapLoaded && state == 3 && !paused) {
				if (note != -1) {
					if (key == -5 || key == -6 || key == -7 || gameAction == FIRE) {
						// close note
						if (note == NOTE_SOLITARY) {
							note = -1;
							sendToSolitary = true;
						} else {
							Sound.playEffect(SFX_CLOSE);
							note = -1;
						}
						noteText = null;
					}
				} else if (inventoryOpen != null) {
					// TODO
					if (key == -7) {
						inventoryOpen = null;
					}
				} else if (containerOpen != -1) {
					int slots = toilet ? 3 : 20;
					if (key == -7) {
						// exit
						containerOpen = -1;
						selectedSlot = 0;
						selectedInventory = lastSelectedInventory;
					} else if (key == -6) {
						// switch between inventory and container
						if (selectedSlot == -1) {
							selectedSlot = 0;
							selectedInventory = -1;
						} else {
							selectedSlot = -1;
							selectedInventory = 0;
						}
					} else if (!altControls && key >= '1' && key <= '6') {
						// select inventory
						int slot = key - '1';
						if (player.inventory[slot] != Items.ITEM_NULL) {
							selectedSlot = -1;
							selectedInventory = slot;
						} else {
							selectedInventory = -1;
						}
					} else {
						// container navigation
						switch (gameAction) {
						case UP:
							if (selectedSlot == -1) {
								if (selectedInventory-- == 0) {
									selectedInventory = 5;
								}
								break;
							}
							if (toilet) {
								if (selectedSlot == -2) selectedSlot = 0;
								break;
							}
							if ((selectedSlot = selectedSlot - 5) < 0) {
								selectedSlot += slots;
							}
							break;
						case DOWN:
							if (selectedSlot == -1) {
								if (++selectedInventory == 6) {
									selectedInventory = 0;
								}
								break;
							}
							if (toilet) {
								selectedSlot = -2;
								break;
							}
							selectedSlot = (selectedSlot + 5) % slots;
							break;
						case LEFT:
							if (selectedSlot == -1) {
								if (selectedInventory-- == 0) {
									selectedInventory = 5;
								}
								break;
							}
							if (selectedSlot == -2) break;
							if (selectedSlot-- == 0) {
								selectedSlot = slots - 1;
							}
							break;
						case RIGHT:
							if (selectedSlot == -1) {
								if (++selectedInventory == 6) {
									selectedInventory = 0;
								}
								break;
							}
							if (selectedSlot == -2) break;
							if (++selectedSlot == slots) {
								selectedSlot = 0;
							}
							break;
						case FIRE:
							if (selectedSlot == -2 && toilet) {
								// flush
								break;
							}
							if (selectedSlot == -1) {
								// if (selectedInventory != -1)
								putItem(containerOpen, selectedInventory);
								break;
							}
							takeItem(containerOpen, selectedSlot);
							break;
						}
					}
				} else if (!pausedOverlay) {
					if (key == -6) {
						softPressed = true;
					} else if (key == -7) {
						paused = true;
						state = 4;
//						action = NPC.ACT_NONE;
//						progress = 0;
					} else if (altControls) {
						try {
							switch (gameAction) {
							case UP:
								keyStates |= UP_PRESSED;
								break;
							case DOWN:
								keyStates |= DOWN_PRESSED;
								break;
							case LEFT:
								keyStates |= LEFT_PRESSED;
								break;
							case RIGHT:
								keyStates |= RIGHT_PRESSED;
								break;
							case FIRE:
								firePressed = true;
								keyStates |= FIRE_PRESSED;
								break;
							case GAME_A:
							case GAME_B:
								if (player.training) {
									if (fatigue >= 100) {
										Sound.playEffect(Sound.SFX_LOSE);
										player.dialog = "You are too fatigued";
										player.dialogTimer = TPS * 2;
									} else if (!trainingBlocked) {
										if (key != trainingLastKey) {
											trainingTimer += player.gymObject == Objects.TRAINING_TREADMILL ? 4 : 8;
										}
										trainingLastKey = key;
									}
								// scroll through inventory slots
								} else if (gameAction == GAME_A) {
									if (selectedInventory == -1) {
										selectedInventory = lastSelectedInventory != -1 ? lastSelectedInventory : 5;
									} else if (selectedInventory-- == 0) {
										selectedInventory = 5;
									}
								} else /*if (gameAction == GAME_B)*/ {
									if (selectedInventory == -1) {
										selectedInventory = lastSelectedInventory != -1 ? lastSelectedInventory : 0;
									} else if (++selectedInventory == 6) {
										selectedInventory = 0;
									}
								}
								break;
							}
						} catch (Exception ignored) {}
					} else if (key == '*') {
					} else if (key == '#') {
					} else if (key == '0') {
						// crafting TODO
					} else if (player.training && (key == '1' || key == '3')) {
						if (fatigue >= 100) {
							Sound.playEffect(Sound.SFX_LOSE);
							player.dialog = "You are too fatigued";
							player.dialogTimer = TPS * 2;
						} else if (!trainingBlocked) {
							if (key != trainingLastKey) {
								trainingTimer += player.gymObject == Objects.TRAINING_TREADMILL ? 4 : 8;
							}
							trainingLastKey = key;
						}
					} else if (key >= '1' && key <= '6') {
						// select inventory
						int slot = key - '1';
						if (player.inventory[slot] != Items.ITEM_NULL) {
							selectedInventory = slot;
						} else {
							selectedInventory = -1;
						}
					} else if (!(key >= '0' && key <= '9')) {
						// dpad
						switch (gameAction) {
						case UP:
							keyStates |= UP_PRESSED;
							break;
						case DOWN:
							keyStates |= DOWN_PRESSED;
							break;
						case LEFT:
							keyStates |= LEFT_PRESSED;
							break;
						case RIGHT:
							keyStates |= RIGHT_PRESSED;
							break;
						case FIRE:
							firePressed = true;
							keyStates |= FIRE_PRESSED;
							break;
						}
					}
				}
			} else if (state == 2) {
				if (gameAction == FIRE) {
					state = 6;
					mapError = false;
				} else if (key == -6) {
					state = 5;
				} else if (key == -7) {
					TE.midlet.notifyDestroyed();
				}
			} else if (state == 5) {
				int numSettings = 0;
				if (!NO_SFX) numSettings++;
				numSettings++; // music volume
				if (DRAW_SHADOWS && supportsAlpha) numSettings++;
				if (USE_M3G) numSettings++;
				numSettings++; // alt controls

				if (gameAction == UP) {
					if (selectedSetting-- == 0)
						selectedSetting = numSettings - 1;
				} else if (gameAction == DOWN) {
					if (++selectedSetting == numSettings)
						selectedSetting = 0;
				} else if (gameAction == FIRE || gameAction == LEFT || gameAction == RIGHT) {
					set: {
						int i = selectedSetting;
						if (!NO_SFX) {
							if (i == 0) {
								int v = Sound.volumeSfx;
								if (gameAction == LEFT) {
									if ((v -= 5) < 0) v = 0;
								} else if (gameAction == RIGHT) {
									if ((v += 5) > 100) v = 100;
								}
								Sound.setEffectVolume(v);
								Sound.stopEffect();
								Sound.playEffect(SFX_BUY);
								break set;
							}
							i--;
						}

						if (i == 0) {
							int v = Sound.volumeMusic;
							if (gameAction == LEFT) {
								if ((v -= 5) < 0) v = 0;
							} else if (gameAction == RIGHT) {
								if ((v += 5) > 100) v = 100;
							}
							Sound.setMusicVolume(v);
							break set;
						}
						i--;

						if (DRAW_SHADOWS && supportsAlpha) {
							if (i == 0) {
								enableShadows = !enableShadows;
								break set;
							}
							i--;
						}

						if (USE_M3G) {
							if (i == 0) {
								use3D = !use3D;
								break set;
							}
							i--;
						}

						if (i == 0) {
							altControls = !altControls;
							break set;
						}
						i--;
					}
				} else if (key == -6 || key == -7) {
					writeConfig();
					state = mapLoaded ? 4 : 2;
				}
			} else if (state == 4) {
				if (gameAction == FIRE) {
					paused = false;
					state = 3;
				} else if (key == -6) {
					state = 5;
				}
			} else if (state == 7) {
				TE.midlet.notifyDestroyed();
			}
			if (!altControls) {
				if (key == '9' && mapLoaded) {
					// debug time skip
					time = ((time / 60 + 1) * 60) - 1;
					playerWasOnRollcall = true;
					playerWasOnMeal = true;
					playerWasOnExcercise = true;
					playerWasOnShowers = true;
				}
				if (key == '8' && mapLoaded) {
					// cheat
					fatigue = 0;
					money += 50;
					player.health = 50;
					heat = 0;

					player.statSpeed = 100;
					player.statStrength = 100;
					player.statIntellect = 100;

					player.outfitItem = Items.PADDED_INMATE_OUTFIT | Items.ITEM_DEFAULT_DURABILITY;
					player.weapon = Items.NUNCHUKS | Items.ITEM_DEFAULT_DURABILITY;

					player.inventory[0] = Items.MULTITOOL | Items.ITEM_DEFAULT_DURABILITY;
					player.inventory[1] = Items.UTILITY_KEY | Items.ITEM_DEFAULT_DURABILITY;
					player.inventory[2] = Items.WORK_KEY | Items.ITEM_DEFAULT_DURABILITY;
					player.inventory[3] = Items.STAFF_KEY | Items.ITEM_DEFAULT_DURABILITY;
					player.inventory[4] = Items.ENTRANCE_KEY | Items.ITEM_DEFAULT_DURABILITY;
					player.inventory[5] = Items.CELL_KEY | Items.ITEM_DEFAULT_DURABILITY;
				}
				if (key == '7' && mapLoaded) {
					debugFreecam = !debugFreecam;
//				note = NOTE_SOLITARY;
				}
			}
		} catch (Exception ignored) {}
	}

	public void keyRepeated(int key) {
		super.keyRepeated(key);
	}

	public void keyReleased(int key) {
		super.keyReleased(key);
		if (altControls) {
			try {
				int game = getGameAction(key);
				switch (game) {
				case UP:
					keyStates &= ~UP_PRESSED;
					break;
				case DOWN:
					keyStates &= ~DOWN_PRESSED;
					break;
				case LEFT:
					keyStates &= ~LEFT_PRESSED;
					break;
				case RIGHT:
					keyStates &= ~RIGHT_PRESSED;
					break;
				case FIRE:
					keyStates &= ~FIRE_PRESSED;
					break;
				}
			} catch (Exception ignored) {}
		} else {
			switch (key) {
			case -1:
				keyStates &= ~UP_PRESSED;
				break;
			case -2:
				keyStates &= ~DOWN_PRESSED;
				break;
			case -3:
				keyStates &= ~LEFT_PRESSED;
				break;
			case -4:
				keyStates &= ~RIGHT_PRESSED;
				break;
			case -5:
				keyStates &= ~FIRE_PRESSED;
				break;
			}
		}
	}

	public void pointerPressed(int x, int y) {

	}

	public void pointerDragged(int x, int y) {

	}

	public void pointerReleased(int x, int y) {

	}

	public void sizeChanged(int w, int h) {
		canvasWidth = 0;
		canvasHeight = 0;
	}

	public void run() {
		try {
			supportsAlpha = TE.midlet.display.numAlphaLevels() > 2;
			// TODO detect pspkvm
			altControls = hasPointerEvents() && !"None".equals(System.getProperty("com.nokia.keyboard.type"));
			if (!"true".equals(System.getProperty("supports.mixing"))) {
				Sound.volumeSfx = 0;
			}
			loadFonts();
			bgImg = Image.createImage("/logo.png");
			Thread.sleep(100);

			if (BUFFER_SCREEN) drawGame();
			drawScreen();

			loadTextures();

			if (tilesTexture == null) {
				noTextures = true;
				if (BUFFER_SCREEN) drawGame();
				drawScreen();
				return;
			}

			// read settings
			try {
				DataInputStream d;
				{
					RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORD_NAME, false);
					byte[] data = r.getRecord(1);
					r.closeRecordStore();
					ByteArrayInputStream bais = new ByteArrayInputStream(data);
					d = new DataInputStream(bais);
				}
				int i;
				boolean b;

				Sound.volumeMusic = d.readInt();
				i = d.readInt();
				if (!NO_SFX) Sound.volumeSfx = i;
				b = d.readBoolean();
				if (USE_M3G) use3D = b;
				b = d.readBoolean();
				if (DRAW_SHADOWS) enableShadows = b;
				b = d.readBoolean();
				altControls = b;
			} catch (Exception ignored) {}

			Sound.load();
			Thread.sleep(100);

			if (System.getProperty("nomusic") != null) {
				Sound.volumeMusic = 0;
			}
			if (System.getProperty("nosfx") != null) {
				Sound.volumeSfx = 0;
			}

			Sound.playMusic(MUSIC_THEME);

			state = 1;
			paused = true;
			if (BUFFER_SCREEN) drawGame();
			drawScreen();

			long lastFrameTime = System.currentTimeMillis();
			long lastCounterTime = lastFrameTime;
			float passedTime = 0;
			int frames = 0, ticksC = 0;
			while (true) {
				if (TE.paused || !isShown()) {
					if (!wasPaused) {
						Sound.stopMusic();
						wasPaused = true;
					}
					if (state == 3) state = 4;
					paused = true;
					Thread.sleep(200);
					continue;
				}
				if (wasPaused) {
					Sound.resumeMusic();
					wasPaused = false;
				}
				long now = System.currentTimeMillis();
				long passed = now - lastFrameTime;
				lastFrameTime = now;
				if (passed < 0L) passed = 0;
				if (passed > 1000L) {
					passed = 1000L;

					// huge lag, reset fps counter
					lastCounterTime = now;
					frames = 0;
				}

				Profiler.beginFrame();

				Profiler.beginFrameSection(Profiler.FRAME_TICK);

				float deltaTime = passed * TPS / 1000F;
				float animDeltaTime = deltaTime > 2 ? 2 : deltaTime;
				if (fadeIn > 0) {
					fadeIn -= FADE_SPEED * animDeltaTime;
					if (fadeIn <= 0) {
						fadeIn = 0;
						if (state == 3) {
							paused = false;
						}
					}
				} else if (fadeOut > 0) {
					paused = true;
					fadeOut -= FADE_SPEED * animDeltaTime;
					if (fadeOut <= 0) {
						fadeOut = 0;
						if (exiting) {
							TE.midlet.notifyDestroyed();
						} else {
							Sound.playMusic(MUSIC_ESCAPED);
							state = 7;
						}
					}
				} else if (mapLoaded) {
					if (!paused) {
						if (ingameFadeIn > 0) {
							ingameFadeIn -= FADE_SPEED * animDeltaTime;
							if (ingameFadeIn <= 0) {
								ingameFadeIn = 0;
								player.animationTimer = 0;
							}
						} else if (ingameFadeOut > 0) {
							ingameFadeOut -= FADE_SPEED * animDeltaTime;
							if (ingameFadeOut <= 0) {
								ingameFadeOut = 0;
								ingameFadeIn = viewWidth >> 1;
								player.respawnPlayer();
							}
						}
					}
					passedTime += deltaTime;
					int ticks = (int) passedTime;
					passedTime -= (float) ticks;

					if (ticks > 10) ticks = 10;
					for (int i = 0; i < ticks; ++i) {
						// tick
						if (!paused && !pausedOverlay) {
							if ((globalCounter % ANIMATION_TICKS) == 0 && ++animationFrame > 1)
								animationFrame = 0;
							tickMap();
						}
						ticksC++;
						globalCounter++;
					}

					if (debugFreecam) {
						int actions = keyStates;
						float speed = 6 * deltaTime;
						if ((actions & GameCanvas.UP_PRESSED) != 0) {
							y -= speed;
						}
						if ((actions & GameCanvas.DOWN_PRESSED) != 0) {
							y += speed;
						}
						if ((actions & GameCanvas.LEFT_PRESSED) != 0) {
							x -= speed;
						}
						if ((actions & GameCanvas.RIGHT_PRESSED) != 0) {
							x += speed;
						}
						x = Math.min(Math.max(x, 0), width * TILE_SIZE - viewWidth);
						y = Math.min(Math.max(y, 0), height * TILE_SIZE - viewHeight);
					} else {
						x = Math.min(Math.max(player.x - (viewWidth >> 1) + (TILE_SIZE / 2), 0), width * TILE_SIZE - viewWidth);
						y = Math.min(Math.max(player.y - (viewHeight >> 1) + (TILE_SIZE / 2), 0), height * TILE_SIZE - viewHeight);
					}
				}

				if (BUFFER_SCREEN) drawGame();
				drawScreen();

				if (state == 6 && !mapError) {
					// start game
					Sound.stopMusic();
					Sound.playEffect(Sound.SFX_RUMBLE);

					try {
						loadMap();

						mapLoaded = true;

						NPC player = this.player;
						x = Math.min(Math.max(player.x - (viewWidth >> 1) + (TILE_SIZE / 2), 0), width * TILE_SIZE - viewWidth);
						y = Math.min(Math.max(player.y - (viewHeight >> 1) + (TILE_SIZE / 2), 0), height * TILE_SIZE - viewHeight);

						bgImg = null;

						Sound.playMusic(Sound.MUSIC_LIGHTSOUT);
						try {
							Thread.sleep(1000);
						} catch (Exception ignored) {}
						paused = true;
						state = 3;
						fadeIn = viewWidth >> 1;
					} catch (Exception e) {
						e.printStackTrace();
						mapError = true;
					}
				} else if (state == 1) {
					Sound.playEffect(Sound.SFX_RUMBLE);
					bgImg = Image.createImage("/title.png");
					fadeIn = viewWidth >> 1;
					state = 2;
				}

				// count FPS
				++frames;
				if (now >= lastCounterTime + 1000L) {
					fps = frames;
					tps = ticksC;
					lastCounterTime += 1000L;
					frames = 0;
					ticksC = 0;
				}

				Profiler.endFrame();

				// limit FPS
				long delay = (1000L / FPS_LIMIT) - (System.currentTimeMillis() - now);
				if (delay > 0) Thread.sleep(delay);
				else Thread.yield();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void writeConfig() {
		try {
			RecordStore.deleteRecordStore(SETTINGS_RECORD_NAME);
		} catch (Exception ignored) {}


		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream d = new DataOutputStream(baos);
			d.writeInt(Sound.volumeMusic);
			d.writeInt(NO_SFX ? 0 : Sound.volumeSfx);
			d.writeBoolean(USE_M3G && use3D);
			d.writeBoolean(DRAW_SHADOWS && enableShadows);
			d.writeBoolean(altControls);

			byte[] b = baos.toByteArray();
			RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORD_NAME, true);
			r.addRecord(b, 0, b.length);
			r.closeRecordStore();
		} catch (Exception ignored) {}
	}

// endregion Canvas

// region Map

	String file = "/map";

	NPC player;
	NPC[] chars, renderChars;
	int width, height;

	byte[][] tiles;
	byte[][] solid;

	int time = 7*60 + 50, day; // day count starts from 0
	int schedule, prevSchedule;
	int tickCounter;

	boolean cellsClosed = true;
	boolean entranceOpen = false;
	boolean lockdown;
	boolean willLockdown;
	boolean playerWasOnRollcall;
	boolean playerWasOnMeal;
	boolean playerWasOnExcercise;
	boolean playerWasOnShowers;
	int lockdownTimer;
	boolean rollcallFace; // true if guards are above inmates

	int guardsDown;
	int guards;
	int inmates;
	int npcNum;

	int[] jobs; // 0 is count
	int npcSpawnX, npcSpawnY;

	byte map;
	byte npcLevel;
	byte fightFreq;

	TiledLayer[] tiledLayer;

	void loadMap() throws Exception {
		player = new NPC(this);
		player.name = "Player";
		player.ai = false;
		player.load(Textures.INMATE4, Textures.OUTFIT_INMATE);
		npcNum = 1;

		{
			InputStream stream = null;
			if (file.charAt(0) == '/') {
				stream = getClass().getResourceAsStream(file);
			} else {
				// TODO i/o
			}
			if (stream == null) throw new Exception();
			DataInputStream in = new DataInputStream(stream);
			try {
				int version = in.readInt();

				map = in.readByte();
				npcLevel = in.readByte();
				fightFreq = in.readByte();

				int width = this.width = in.readByte() & 0xFF;
				int height = this.height = in.readByte() & 0xFF;

				tiles = new byte[4][width * height];
				objects = new short[4][];
				droppedItems = new int[4][width * height];
				solid = new byte[4][width * height];

				inmates = in.readByte() & 0xFF;
				guards = in.readByte() & 0xFF;

				npcSpawnX = in.readByte() & 0xFF;
				npcSpawnY = in.readByte() & 0xFF;

				// jobs
				jobs = new int[COUNT_JOBS];
				player.job = in.readByte();
				jobs[0] = in.readByte();
				for (int i = 1; i < COUNT_JOBS; ++i) {
					if (in.readByte() != 0) {
						jobs[i] |= JOB_EXISTING_BIT;
					}
				}

				// zones
				{
					int numZones = in.readByte();
					int[] zones = this.zones = new int[1 + (numZones * 5)];
					zones[0] = numZones;
					for (int i = 0; i < numZones; ++i) {
						zones[i * 5 + 1] = in.readShort();
						zones[i * 5 + 2] = in.readShort();
						zones[i * 5 + 3] = in.readShort();
						zones[i * 5 + 4] = in.readShort();
						zones[i * 5 + 5] = in.readByte();
					}
				}

				// tiles
				{
					for (int layer = 0; layer < 4; ++layer) {
						int n = in.readShort();
						byte[] tiles = this.tiles[layer];
						for (int i = 0; i < n; ++i) {
							tiles[i] = in.readByte();
						}
					}
				}

				// objects
				for (int layer = 0; layer < 4; ++layer) {
					short numObjects = in.readShort();
					short[] objects = this.objects[layer] = new short[1 + ((numObjects + 4) << 2)];
					objects[0] = numObjects;
					for (int i = 0; i < numObjects; ++i) {
						objects[(i << 2) + 1] = (short) (in.readByte() & 0xFF);
						objects[(i << 2) + 2] = in.readShort();
						objects[(i << 2) + 3] = (short) (in.readByte() & 0xFF);
						objects[(i << 2) + 4] = (short) (in.readByte() & 0xFF);
					}
				}

				// lights
				this.lights = readPositions(in);

				// topObjects
				{
					short numObjects = in.readShort();
					short[] topObjects = this.topObjects = new short[1 + (numObjects << 1)];
					topObjects[0] = numObjects;
					for (int i = 0; i < numObjects; ++i) {
						short idx = in.readShort();
						topObjects[(i << 1) + 1] = idx;
						topObjects[(i << 1) + 2] = (short) (objects[0][idx + 1] == Objects.SECURITY_CAMERA ? TPS : 0);
					}
				}

				// waypoints

				this.roamPositions = readPositions(in);
				this.rollcallPositions = readPositions(in);
				this.canteenServingPositions = readPositions(in);
				this.showerPositions = readPositions(in);

				this.guardRoamPositions = readPositions(in);
				this.guardRollcallPositions = readPositions(in);
				this.guardCanteenPositions = readPositions(in);
				this.guardGymPositions = readPositions(in);
				this.guardShowerPositions = readPositions(in);

				this.guardBeds = readPositions(in);

				canteenSeatsPositions = new short[1 + ((inmates + 20) << 1)];

				{
					short num = in.readShort();
					short[] gymPositions = this.gymPositions = new short[1 + (num * 3)];
					gymPositions[0] = num;
					for (int i = 0; i < num; ++i) {
						in.readByte(); // object id is not used yet
						int x = in.readByte() & 0xFF;
						int y = in.readByte() & 0xFF;
						gymPositions[(i << 1) + 1] = (short) (x | (y << 8));
						gymPositions[(i << 1) + 2] = -1;
					}
				}

				// npc
				{
					chars = new NPC[inmates + guards + 3 + 4 + 2];
					renderChars = new NPC[chars.length];

					int addedInmates = 0;
					int addedGuards = 0;

					short num = in.readShort();
					for (int i = 0; i < num; ++i) {
						int obj = in.readByte() & 0xFF;
						int x = in.readByte() & 0xFF;
						int y = in.readByte() & 0xFF;

						switch (obj) {
						case Objects.PLAYER_BED: {
							chars[0] = player;
							player.x = (player.bedX = x) * TILE_SIZE;
							player.y = (player.bedY = y) * TILE_SIZE + 2;
							player.animation = NPC.ANIM_LYING;
							break;
						}
						case Objects.BED: {
							if (addedInmates < inmates - 1) {
								NPC npc = chars[npcNum] = new NPC(this);
								npc.id = npcNum++;
								npc.typedId = addedInmates++;
								npc.name = "Inmate" + addedInmates;
								npc.x = (npc.bedX = x) * TILE_SIZE;
								npc.y = (npc.bedY = (y - 1)) * TILE_SIZE + 2;
								npc.animation = NPC.ANIM_LYING;
								npc.load(NPC.rng.nextInt(Textures.INMATE4 + 1), Textures.OUTFIT_INMATE);
							}
							break;
						}
						case Objects.AI_WP_GUARD_GENERAL: {
							// TODO randomize
							if (addedGuards < guards) {
								NPC npc = chars[npcNum] = new NPC(this);
								npc.id = npcNum++;
								npc.typedId = addedGuards++;
								npc.name = "Officer " + addedGuards;
								npc.outfitItem = Items.GUARD_OUTFIT | Items.ITEM_DEFAULT_DURABILITY;
								npc.weapon = Items.BATON | Items.ITEM_DEFAULT_DURABILITY;
								npc.x = x * TILE_SIZE;
								npc.y = y * TILE_SIZE;
								npc.load(Textures.GUARD, Textures.OUTFIT_GUARD);
							}
							break;
						}
						case Objects.AI_WP_DOCTOR_WORK: {
							NPC npc = chars[npcNum] = new NPC(this);
							npc.id = npcNum++;
							npc.bedX = x;
							npc.bedY = y;
							npc.x = npcSpawnX * TILE_SIZE;
							npc.y = npcSpawnY * TILE_SIZE;
							npc.load(Textures.DOCTOR, -1);
							break;
						}
						case Objects.AI_WP_EMPLOYMENT_OFFICER: {
							NPC npc = chars[npcNum] = new NPC(this);
							npc.id = npcNum++;
							npc.bedX = x;
							npc.bedY = y;
							npc.x = npcSpawnX * TILE_SIZE;
							npc.y = npcSpawnY * TILE_SIZE;
							npc.load(Textures.EMPLOYMENT_OFFICER, -1);
							break;
						}
						case Objects.PRISON_SNIPER: {
							NPC npc = chars[npcNum] = new NPC(this);
							npc.id = npcNum++;
							npc.x = x * TILE_SIZE;
							npc.y = y * TILE_SIZE;
							npc.load(Textures.SNIPER, -1);
							break;
						}
						}
					}
				}

				// containers
				{
					short num = in.readShort();
					int[] containers = this.containers = new int[1 + num * (3 + 20)];
					containers[0] = num;

					int idx = 1;
					for (int i = 0; i < num; ++i) {
						containers[idx++] = in.readShort() << 2;
						int obj = in.readByte() & 0xFF;
						if (obj == Objects.PLAYER_DESK) {
							obj = 0;
						} else {
							obj = -obj;
						}
						containers[idx++] = obj;
						idx += (containers[idx] = in.readByte() & 0xFF) + 1;
					}
				}

				if ((in.readShort() & 0xFFFF) != 0xFFEF) {
					throw new Exception();
				}

				boolean gardening = false;
				if ((jobs[JOB_JANITOR] & JOB_EXISTING_BIT) != 0
						|| (gardening = (jobs[JOB_GARDENING] & JOB_EXISTING_BIT) != 0)) {
					dirt = new short[6];
					dirt[2] = (short) (objects[0][0] << 2);
					addObject(gardening ? Objects.OUTSIDE_DIRT : Objects.FLOOR_DIRT, gardening ? 43 : 47, 1, 1, 0, 0, LAYER_GROUND);
					dirt[5] = (short) (objects[0][0] << 2);
					addObject(gardening ? Objects.OUTSIDE_DIRT : Objects.FLOOR_DIRT, gardening ? 43 : 47, 1, 1, 0, 0, LAYER_GROUND);
				}

			} finally {
				in.close();
			}
		}

		rollcallFace = guardRollcallPositions[2] < rollcallPositions[2];

		allocatePathfind();

		if (USE_TILED_LAYER) {
			tiledLayer = new TiledLayer[4];
			for (int i = 0; i < 4; ++i) {
				tiledLayer[i] = new TiledLayer(width, height, tilesTexture, TILE_SIZE, TILE_SIZE);
			}
		}


		// fill collision lookup
		for (int l = 0; l < 4; ++l) {
			byte[] solid = this.solid[l];
			for (int i = 0; i < width * height; ++i) {
				solid[i] = l == LAYER_UNDERGROUND ? COLL_SOLID : isSolidTile(tiles[l][i]);
			}

			short[] objects = this.objects[l];
			if (objects != null) {
				int n = objects[0];
				for (int i = 0; i < n; ++i) {
					int idx = i << 2;
					byte s;
					if ((s = isSolidObject(objects[idx + 1])) == COLL_NONE) {
						continue;
					}

					short x = objects[idx + 3], y = objects[idx + 4];

					// TODO randomize stash spawn
//					if (objects[idx + 1] == Objects.STASH && day == 0) {
//
//					} else
					if (objects[idx + 1] == Objects.CHAIR && isInZone(x * TILE_SIZE, y * TILE_SIZE, ZONE_CANTEEN)) {
						int p = ((canteenSeatsPositions[0]++) << 1) + 1;
						canteenSeatsPositions[p] = (short) ((x & 0xFF) | ((y & 0xFF) << 8));
						canteenSeatsPositions[p + 1] = -1;
					}

					short sprite = objects[idx + 2];
					for (int y2 = y - ((sprite & (3 << 10)) >> 10); y > y2; --y) {
						for (int x2 = 0; x2 < ((sprite & (3 << 8)) >> 8); ++x2) {
							int p = x + x2 + y * width;
							if (solid[p] == COLL_SOLID && (s == COLL_SOLID_INTERACT || s == COLL_DETECTOR)) continue;
							solid[p] = s;
						}
					}
				}
			}
		}

		initMap();
		System.gc();
	}

	private short[] readPositions(DataInputStream in) throws Exception {
		short num = in.readShort();
		short[] res = new short[1 + (num << 1)];
		res[0] = num;
		for (int i = 0; i < num; ++i) {
			res[(i << 1) + 1] = (short) (in.readByte() & 0xFF);
			res[(i << 1) + 2] = (short) (in.readByte() & 0XFF);
		}
		return res;
	}

	void initMap() {
		// fill tiled layer
		if (USE_TILED_LAYER) {
			int width = this.width;
			int height = this.height;
			for (int layer = 0; layer < 4; ++layer) {
				for (int y = 0; y < height; ++y) {
					for (int x = 0; x < width; ++x) {
						tiledLayer[layer].setCell(x, y, tiles[layer][x + y * width]);
					}
				}
			}
		}

		System.arraycopy(chars, 0, renderChars, 0, chars.length);
		updateDoors();

		if ((jobs[JOB_JANITOR] & JOB_EXISTING_BIT) != 0 || (jobs[JOB_GARDENING] & JOB_EXISTING_BIT) != 0) {
			nextDirtPos = NPC.rng.nextInt(roamPositions[0]);
			updateDirt(0);
			updateDirt(1);
		}

		fatigue = 20;

		int n = npcNum;
		int j = 0;
		for (int i = 1; i < n; ++i) {
			NPC npc = chars[i];
			if (npc == null || !npc.guard) continue;

			// TODO
			if (npc.typedId > 3 && npc.typedId - 3 < guardBeds[0]) {
				npc.xFloat = npc.x = (npc.bedX = guardBeds[(j << 1) + 1]) * TILE_SIZE;
				npc.yFloat = npc.y = (npc.bedY = guardBeds[(j << 1) + 2]) * TILE_SIZE;
				j++;
			}
		}

		if (day == 0) {
			// first day

			// assign jobs
			int jobsLeft = jobs[0];
			if (player.job != JOB_UNEMPLOYED) {
				jobs[player.job] |= JOB_OCCUPIED_BIT;
				jobsLeft -= 1;
			}

			for (int i = 1; i < n; ++i) {
				NPC npc = chars[i];
				if (npc == null || (!npc.inmate && !npc.guard)) continue;
				// TODO npcLevel
				int b = npcLevel * 15;
				npc.statStrength = Math.max(5, Math.min(90, b + NPC.rng.nextInt(30)));
				npc.statSpeed = Math.max(5, Math.min(90, b + NPC.rng.nextInt(30)));
				npc.statIntellect = Math.max(5, Math.min(90, b + NPC.rng.nextInt(30)));
				npc.statRespect = Math.max(5, Math.min(90, 60 - b + NPC.rng.nextInt(30)));
				npc.health = npc.statStrength >> 1;

				if (npc.inmate) {
					if (jobsLeft > 0) {
						for (int k = 1; k < COUNT_JOBS; ++k) {
							if ((jobs[k] & JOB_EXISTING_BIT) == 0 || (jobs[k] & JOB_OCCUPIED_BIT) != 0)
								continue;
							npc.job = k;
							jobs[k] |= JOB_OCCUPIED_BIT;
							jobsLeft--;
							break;
						}
					}
				}
			}

			// init containers
			int[] containers = this.containers;
			if (containers == null) return;
			n = containers[0];
			int idx = 1;
			for (int i = 0; i < n; ++i) {
				int objIdx = containers[idx++];
				int owner = containers[idx++];
				if (owner == -Objects.TOILET) {
				} else if (owner == -Objects.JOB_GARDENING_TOOLS) {
					for (j = 0; j < 20; ++j) {
						containers[idx + 1 + j] = Items.HOE | Items.ITEM_DEFAULT_DURABILITY;
					}
				} else if (owner == -Objects.JOB_CLEANING_SUPPLIES) {
					for (j = 0; j < 20; ++j) {
						int item;
						switch (NPC.rng.nextInt(6)) {
						case 0:
						case 1:
							item = Items.MOP | Items.ITEM_DEFAULT_DURABILITY;
							break;
						case 2:
						case 3:
							item = Items.BROOM | Items.ITEM_DEFAULT_DURABILITY;
							break;
						case 4:
							item = Items.BROOM | Items.ITEM_DEFAULT_DURABILITY;
							break;
						case 5:
							item = Items.PLUNGER | Items.ITEM_DEFAULT_DURABILITY;
							break;
						default:
							continue;
						}
						containers[idx + 1 + j] = item;
					}
				} else {
					if (owner == -Objects.DESK) {
						short[] objects = this.objects[LAYER_GROUND];
						byte[] solid = this.solid[LAYER_GROUND];
						int w = width;
						int h = height;

						int x = objects[objIdx + 3];
						int y = objects[objIdx + 4];
						NPC res = null;
						for (int k = 1; k < npcNum; ++k) {
							NPC npc = chars[k];
							if (npc == null || !npc.inmate) continue;
							int x0, y0;
							int x1 = x0 = x, y1 = y0 = y;
							int x2 = npc.bedX, y2 = npc.bedY;
							int dx = x - x2;
							int dy = y - y2;
							if (dx * dx + dy * dy > 5 * 5) {
								// too far
								continue;
							}

							dx = Math.abs(x2 - x0);
							dy = Math.abs(y2 - y0);

							int sx = (x0 < x2) ? 1 : -1;
							int sy = (y0 < y2) ? 1 : -1;

							int e = dx - dy;

							while (true) {
								if (x1 == x2 && y1 == y2) {
									res = npc;
									break;
								}
								if ((x1 != x0 || y1 != y0)
										&& x1 >= 0 && y1 >= 0 && x1 < w && y1 < h) {
									byte s = solid[x1 + y1 * w];
									if (s == COLL_SOLID || s == COLL_SOLID_TRANSPARENT || s == COLL_DOOR) {
										break;
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
							if (res != null) {
								containers[idx - 1] = res.id;
								break;
							}
						}

						int numItems = NPC.rng.nextInt(5);
						for (int k = 0; k < numItems; ++k) {
							int[] items = NPC.rng.nextInt(3) == 0 ? DESK2 : DESK1;
							int item = items[NPC.rng.nextInt(items.length)];
							containers[idx + 5 + k] = item | Items.ITEM_DEFAULT_DURABILITY;
						}
					}
					containers[idx + 1] = Items.COMB | Items.ITEM_DEFAULT_DURABILITY;
					containers[idx + 2] = Items.TUBE_OF_TOOTHPASTE | Items.ITEM_DEFAULT_DURABILITY;
					containers[idx + 3] = Items.ROLL_OF_TOILET_PAPER | Items.ITEM_DEFAULT_DURABILITY;
					containers[idx + 4] = Items.SOAP | Items.ITEM_DEFAULT_DURABILITY;
				}

				idx += containers[idx] + 1;
			}
		}
	}

	void startLockdown() {
		if (lockdown || willLockdown) return;
		lockdownTimer = 99;
		heat = 100;
		willLockdown = true;
		playerSeenByGuards = false;
		Sound.stopEffect();
		Sound.stopMusic();
		Sound.playEffect(Sound.SFX_BELL);
		Sound.playMusic(MUSIC_LOCKDOWN);
	}

	void tickMap() {
		if (sendToSolitary) {
			sendToSolitary = false;
			time = 7*60 + 50;
			day += 3;
			lockdown = false;
			schedule = SC_LIGHTSOUT;
			cellsClosed = true;
			entranceOpen = false;
			Sound.playMusic(Constants.MUSIC_LIGHTSOUT);

			// reset npcs
			int n = npcNum;
			int roamPos;
			short[] arr = guardRoamPositions;
			for (int i = 1; i < n; ++i) {
				NPC npc = chars[i];
				if (npc == null) continue;
				if (npc.guard) {
					if ((roamPos = NPC.guardRoamPos++) >= arr[0]) {
						roamPos = NPC.guardRoamPos = 0;
					}
					npc.correctPath = false;
					npc.xFloat = npc.x = guardRoamPositions[(roamPos << 1) + 1] * TILE_SIZE;
					npc.yFloat = npc.y = guardRoamPositions[(roamPos << 1) + 2] * TILE_SIZE;
					npc.aiState = NPC.AI_RESET;
				} else if (npc.inmate) {
					npc.correctPath = false;
					npc.xFloat = npc.x = npc.bedX * TILE_SIZE;
					npc.yFloat = npc.y = npc.bedY * TILE_SIZE + 2;
					npc.aiState = NPC.AI_SLEEP;
				} else if (npc.bodyId != Textures.SNIPER) {
					npc.correctPath = false;
					npc.xFloat = npc.x = npcSpawnX * TILE_SIZE;
					npc.yFloat = npc.y = npcSpawnY * TILE_SIZE;
					npc.aiState = NPC.AI_RESET;
				}
			}

			// TODO reset containers
			removeIllegalItems(getContainerByOwner(0));

			// reset player
			NPC player = this.player;
			int obj = findObject(Objects.SOLITARY_BED, LAYER_GROUND, 0);
			player.xFloat = player.x = objects[0][obj + 3] * TILE_SIZE;
			player.yFloat = player.y = (objects[0][obj + 4] - 1) * TILE_SIZE + 2;
			ingameFadeIn = viewWidth >> 1;
			player.animation = NPC.ANIM_LYING;
			for (int i = 0; i < 6; ++i) {
				player.inventory[i] = Items.ITEM_NULL;
			}
			player.outfitItem = Items.INMATE_OUTFIT | Items.ITEM_DEFAULT_DURABILITY;
			player.weapon = Items.ITEM_NULL;
			heat = 0;
			fatigue = 20;
			player.job = JOB_UNEMPLOYED;

			initMap();
		}

		prevSchedule = schedule;
		if ((tickCounter++ % TIME_TICKS) == 0) {
			if (++time == 24 * 60) {
				time = 0;
				++day;
			}
			if (!lockdown && (time % 60 == 0 || schedule == SC_LOCKDOWN)) {
				boolean wasLockdown = schedule == SC_LOCKDOWN;
				m: {
					int hour = time / 60;
					int music;
					playerSeenByGuards = false;
					switch (hour) {
					case 8:
						music = Sound.MUSIC_ROLLCALL;
						schedule = SC_MORNING_ROLLCALL;
						cellsClosed = false;
						playerWasOnRollcall = false;
						updateDoors();
						break;
					case 9:
						playerWasOnMeal = false;
						if (!playerWasOnRollcall) {
							playerWasOnRollcall = true;
							startLockdown();
							break m;
						}
						music = Sound.MUSIC_CANTEEN;
						schedule = SC_BREAKFAST;
						updateDoors();
						break;
					case 10:
						if (!playerWasOnMeal) {
							playerWasOnMeal = true;
							heat += 30;
						}
						music = player.job != 0 ? Sound.MUSIC_WORK : Sound.MUSIC_GENERIC;
						schedule = SC_WORK_PERIOD;
						updateDoors();
						break;
					case 11:
					case 12:
						music = player.job != 0 ? Sound.MUSIC_WORK : Sound.MUSIC_GENERIC;
						schedule = SC_WORK_PERIOD;
						if (!wasLockdown) break m;
						break;
					case 13:
						if (player.jobQuota < MAX_JOB_QUOTA) {
							// fire player
							jobs[player.job] &= ~JOB_OCCUPIED_BIT;
							player.job = 0;
							note = NOTE_JOB_LOST;
//							Sound.playEffect(Sound.SFX_OPEN);
						}
						music = Sound.MUSIC_ROLLCALL;
						schedule = SC_MIDDAY_ROLLCALL;
						playerWasOnRollcall = false;
						break;
					case 14:
						if (!playerWasOnRollcall) {
							playerWasOnRollcall = true;
							startLockdown();
							break m;
						}
						music = Sound.MUSIC_GENERIC;
						schedule = SC_AFTERNOON_FREETIME;
						break;
					case 15:
						music = Sound.MUSIC_GENERIC;
						schedule = SC_AFTERNOON_FREETIME;
						if (!wasLockdown) break m;
						break;
					case 16:
						playerWasOnMeal = false;
						music = Sound.MUSIC_CANTEEN;
						schedule = SC_EVENING_MEAL;
						updateDoors();
						break;
					case 17:
						playerWasOnExcercise = false;
						if (!playerWasOnMeal) {
							playerWasOnMeal = true;
							heat += 30;
						}
						music = Sound.MUSIC_WORKOUT;
						schedule = SC_EXCERCISE_PERIOD;
						updateDoors();
						break;
					case 18:
						playerWasOnShowers = false;
						if (!playerWasOnExcercise) {
							playerWasOnExcercise = true;
							heat += 30;
						}
						music = Sound.MUSIC_SHOWER;
						schedule = SC_SHOWER_BLOCK;
						break;
					case 19:
						if (!playerWasOnShowers) {
							playerWasOnShowers = true;
							heat += 30;
						}
						music = Sound.MUSIC_GENERIC;
						schedule = SC_EVENING_FREETIME;
						break;
					case 20:
					case 21:
						music = Sound.MUSIC_GENERIC;
						schedule = SC_EVENING_FREETIME;
						if (!wasLockdown) break m;
						break;
					case 22:
						music = Sound.MUSIC_ROLLCALL;
						schedule = SC_EVENING_ROLLCALL;
						updateDoors();
						playerWasOnRollcall = false;
						break;
					case 23:
						if (!playerWasOnRollcall) {
							playerWasOnRollcall = true;
							startLockdown();
							break m;
						}
						music = Sound.MUSIC_LIGHTSOUT;
						schedule = SC_LIGHTSOUT;
						break;
					case 0:
					case 1:
					case 2:
					case 3:
					case 4:
					case 5:
					case 6:
					case 7:
						music = Sound.MUSIC_LIGHTSOUT;
						schedule = SC_LIGHTSOUT;
						if (!wasLockdown) break m;
						break;
					default:
						break m;
					}

					Sound.stopEffect();
					Sound.playEffect(Sound.SFX_BELL);
					Sound.playMusic(music);
				}
				if (willLockdown) {
					schedule = SC_LOCKDOWN;
					willLockdown = false;
					lockdown = true;
				}
			} else {
				if (!cellsClosed && (time == 1 || (time >= 23 * 60 + 20 && player.isInZone(ZONE_PLAYER_CELL)))) {
					cellsClosed = true;
					updateDoors();
					Sound.playEffect(Sound.SFX_RUMBLE);
				}
				if (lockdown) {
					if (guardsDown >= 4) {
						if (!entranceOpen) {
							entranceOpen = true;
							updateDoors();
							note = NOTE_RIOT;
							Sound.playEffect(SFX_OPEN);
						}
					} else if (guardsDown <= 1 && playerSeenByGuards) {
						lockdown = false;
						entranceOpen = false;
						updateDoors();
					}
				}
			}

			if (USE_M3G && time >= 7 * 60 && time <= 21 * 60 + 128 && globalVertexBuffer != null) {
				update3DLightingColor();
			}
		}

		// tick characters

		int tick = tickCounter;
		player.tickPlayer(tick);

		NPC[] chars = this.chars;
		int n = chars.length;
		for (int i = 0; i < n; ++i) {
			if (chars[i] != null) {
				if (i != 0) chars[i].tickAI(tick);
				chars[i].tick(tick);
			}
		}

		// sort characters for rendering, so that southern will be rendered on top

		if ((tick & 3) == 0) {
			NPC[] sortedChars = this.renderChars;

			for (int i = 0; i < n - 1; ++i) {
				for (int j = 0; j < n - i - 1; ++j) {
					if (sortedChars[j] != null && sortedChars[j + 1] != null
							&& ((sortedChars[j].y - (sortedChars[j].animation == NPC.ANIM_STUNNED ? 10000 : 0)) >
							(sortedChars[j + 1].y - (sortedChars[j + 1].animation == NPC.ANIM_STUNNED ? 10000 : 0)))) {
						NPC t = sortedChars[j];
						sortedChars[j] = sortedChars[j + 1];
						sortedChars[j + 1] = t;
					}
				}
			}
		}

		if (willLockdown) {
			schedule = SC_LOCKDOWN;
			willLockdown = false;
			lockdown = true;
		}

		if (lockdown && tick % TPS == 0) {
			if (lockdownTimer == 0) {
				note = NOTE_SOLITARY;
				return;
			}
			--lockdownTimer;
		}

		// TODO items decay
//		final int size = width * height;
//		for (int i = 0; i < size; ++i) {
//			int item = droppedItems[i];
//			if (item != Items.ITEM_NULL) {
//
//			}
//		}

		if (day == 0 && tick == TPS) {
			// welcome note TODO
			note = NOTE_WELCOME;
			Sound.playEffect(Sound.SFX_OPEN);
		}
	}

	void paintMap(Graphics g, int viewX, int viewY, int viewWidth, int viewHeight, int layer) {
		int viewCols = (viewWidth / TILE_SIZE) + 2, viewRows = (viewHeight / TILE_SIZE) + 2;
		int width = this.width, height = this.height;

		Profiler.beginRenderSection(Profiler.RENDER_BG);

		{
			int xr = viewX % TILE_SIZE, yr = viewY % TILE_SIZE;
			int viewx = viewX - xr;
			int viewy = viewY - yr;
			int xoff = viewx / TILE_SIZE, yoff = viewy / TILE_SIZE;

			Image tilesImg = tilesTexture;
			Image itemsImg = itemsTexture;

			// background
			if (layer == LAYER_GROUND || layer == LAYER_UNDERGROUND) {
				for (int x = viewWidth / (TILE_SIZE * 3) + 1; x >= 0; --x) {
					for (int y = viewHeight / (TILE_SIZE * 3) + 1; y >= 0; --y) {
						g.drawRegion(groundTexture, layer == LAYER_UNDERGROUND ? (TILE_SIZE * 3) : 0, 0,
								TILE_SIZE * 3, TILE_SIZE * 3, 0, -xr + x * TILE_SIZE * 3, -yr + y * 48, 0);
					}
				}
			}

			Profiler.beginRenderSection(Profiler.RENDER_TILES);

			g.setColor(0);

			final boolean drawShadows = DRAW_SHADOWS && enableShadows && supportsAlpha && layer == LAYER_GROUND;
			byte[] solid = this.solid[layer];

			Image shadowImg = null;
			if (drawShadows) {
				if (NOKIAUI_SHADOWS) {
					DirectGraphics dg;
					if (g instanceof DirectGraphics) {
						dg = (DirectGraphics) g;
					} else {
						dg = DirectUtils.getDirectGraphics(g);
					}
					if (dg != null) {
						dg.setARGBColor(SHADOW_COLOR);
					}
				} else {
					shadowImg = shadowsTexture;
				}
			}

			if (USE_TILED_LAYER) {
				tiledLayer[layer].setPosition(-viewX, -viewY);
				tiledLayer[layer].paint(g);
			}

			// tiles
			int y = -yr;
			byte[] tiles = this.tiles[layer];
			int[] items = this.droppedItems[layer];
			for (int i = 0; i < viewRows; ++i) {
				int x = -xr;
				y: {
					if (i + yoff < 0 || i + yoff >= height) break y;
					for (int j = 0; j < viewCols; ++j) {
						int pos = j + xoff + (i + yoff) * width;
						x: {
							if (j + xoff < 0 || j + xoff >= width) break x;

//							if (layer == LAYER_VENT) {
//								g.drawRegion(shadowsTexture, 0, 0, TILE_SIZE, TILE_SIZE, 0, x, y, 0);
//							}

							if (!USE_TILED_LAYER) {
								byte tile = tiles[pos];
								if (tile != 0) {
									--tile;
									g.drawRegion(tilesImg, (tile % 4) * TILE_SIZE, (tile / 4) * TILE_SIZE, TILE_SIZE, TILE_SIZE, 0, x, y, 0);
								}
							}

							// wall shadows
							if (drawShadows && i + yoff > 1 && j + xoff > 1) {
								byte s = solid[pos];
								if (s != COLL_SOLID && s != COLL_SOLID_TRANSPARENT) {
									byte t = solid[j + xoff + (i + yoff - 1) * width];
									byte l = solid[j + xoff - 1 + (i + yoff) * width];
									byte tl = solid[j + xoff - 1 + (i + yoff - 1) * width];
									if (NOKIAUI_SHADOWS) {
										if ((t & COLL_BIT_CAST_SHADOW) != 0) {
											if ((tl & COLL_BIT_CAST_SHADOW) == 0) {
												g.fillTriangle(x, y, x + TILE_SIZE, y, x + TILE_SIZE, y + TILE_SIZE);
											} else {
												g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
											}
										} else if ((l & COLL_BIT_CAST_SHADOW) != 0) {
											if ((tl & COLL_BIT_CAST_SHADOW) == 0) {
												g.fillTriangle(x, y, x, y + TILE_SIZE, x + TILE_SIZE, y + TILE_SIZE);
											} else {
												g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
											}
										} else if ((tl & COLL_BIT_CAST_SHADOW) != 0) {
											g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
										}
									} else {
										if ((t & COLL_BIT_CAST_SHADOW) != 0) {
											if ((tl & COLL_BIT_CAST_SHADOW) == 0) {
												g.drawRegion(shadowImg, TILE_SIZE * 2, 0, TILE_SIZE, TILE_SIZE, 0, x, y, 0);
											} else {
												g.drawRegion(shadowImg, 0, 0, TILE_SIZE, TILE_SIZE, 0, x, y, 0);
											}
										} else if ((l & COLL_BIT_CAST_SHADOW) != 0) {
											if ((tl & COLL_BIT_CAST_SHADOW) == 0) {
												g.drawRegion(shadowImg, TILE_SIZE, 0, TILE_SIZE, TILE_SIZE, 0, x, y, 0);
											} else {
												g.drawRegion(shadowImg, 0, 0, TILE_SIZE, TILE_SIZE, 0, x, y, 0);
											}
										} else if ((tl & COLL_BIT_CAST_SHADOW) != 0) {
											g.drawRegion(shadowImg, 0, 0, TILE_SIZE, TILE_SIZE, 0, x, y, 0);
										}
									}
								}
							}

							int item = items[pos];
							if (item != Items.ITEM_NULL) {
								item = item & Items.ITEM_ID_MASK;
								g.drawRegion(itemsImg, (item % TILE_SIZE) * TILE_SIZE, (item / TILE_SIZE) * TILE_SIZE, TILE_SIZE, TILE_SIZE, 0, x, y, 0);
							}

							// collision debug
//							if (solid[pos] != 0) {
//								g.setColor(0x00FF00);
//								g.drawRect(x, y, TILE_SIZE - 1, TILE_SIZE - 1);
//							}
							// tiles id debug
//							if (layer == 0) {
//								g.setColor(0xFFFFFF);
//								g.drawString(Integer.toString(tiles[layer][pos]), x, y, 0);
//							}
						}

						x += TILE_SIZE;
					}
				}
				y += TILE_SIZE;
			}
		}

		Profiler.beginRenderSection(Profiler.RENDER_OBJECTS);

		// objects
		Image objectsImg = objectsTexture;
		int ticks = tickCounter;
		short[] objects = this.objects[layer];
		if (objects != null) {
			int n = objects[0];
			for (int i = 0; i < n; ++i) {
				int idx = i << 2;
				int obj = objects[idx + 1] & 0xFF;
				if (obj == Objects.CONTRABAND_DETECTOR || obj == Objects.SECURITY_CAMERA) {
					// rendered on top
					continue;
				}
				int x = objects[idx + 3] * TILE_SIZE - viewX, y = objects[idx + 4] * TILE_SIZE - viewY;
				// off-screen culling
				if (x < -TILE_SIZE * 3 || y < -TILE_SIZE * 3 || x >= viewWidth + TILE_SIZE * 3 || y >= viewHeight + TILE_SIZE * 3) {
					continue;
				}
				short sprite = objects[idx + 2];
				if ((sprite & (1 << 12)) != 0) {
					continue; // hidden
				}
				// animate
				if (obj == Objects.JOB_SELECTION && (ticks / (TPS >> 1) & 1) == 1) {
					sprite++;
				} else if (obj == Objects.STASH) {
					// stash sparkles
					int a = ((ticks / (TPS >> 2)) % 8);
					if (a < 4) sprite += (short) a;
					else sprite += 3;
				} else if (obj == Objects.SHOWER) {
					sprite += (short) ((ticks / (TPS >> 3)) % 3);
				} else if (obj == Objects.OUTSIDE_DIRT) {
					sprite += (short) ((ticks / (TPS >> 1)) % 4);
				}
				int w = (sprite & (3 << 8)) >> 4, h = (sprite & (3 << 10)) >> 6;
				sprite &= 0xFF;
				g.drawRegion(objectsImg, ((sprite & 0xFF) % TILE_SIZE) * TILE_SIZE, (sprite / TILE_SIZE) * TILE_SIZE, w, h, 0, x, y - h + TILE_SIZE, 0);
			}
		}

		Profiler.beginRenderSection(Profiler.RENDER_CHARACTERS);

		// characters
		NPC[] renderChars = this.renderChars;
		for (int i = 0; i < renderChars.length; ++i) {
			NPC npc = renderChars[i];
			if (npc == null || npc.layer != layer || npc.carried || npc.inCabinet) continue;
			npc.visible = false;
			int x = (int) npc.x - viewX, y = (int) npc.y - viewY;

			// pathfind debug
//			if (npc.correctPath /*&& npc.guard && npc.typedId < 3*/) {
//				g.setColor(0xFF0000);
//				int n = npc.path[0];
//				for (int t = 1; t < n; t += 2) {
//					g.drawRect(npc.path[t] * TILE_SIZE - viewX, npc.path[t + 1] * TILE_SIZE - viewY, 15, 15);
//				}
//			}

			if (x < -TILE_SIZE * 2 || y < -TILE_SIZE * 2 || x >= viewWidth || y >= viewHeight) {
				continue;
			}
			npc.visible = true;
			npc.paint(g, x, y);
		}

		Profiler.beginRenderSection(Profiler.RENDER_TOP_OBJECTS);

		int px = player.x;
		int py = (player.y + 5) / TILE_SIZE;

		// objects above character
		if (layer == LAYER_GROUND && objects != null && topObjects != null) {
			short[] topObjects = this.topObjects;
			int n = topObjects[0];
			for (int i = 0; i < n; ++i) {
				int idx = i << 1;

				int objIdx = topObjects[idx + 1];
				int x = objects[objIdx + 3] * TILE_SIZE - viewX, y = objects[objIdx + 4] * TILE_SIZE - viewY;
				// off-screen culling
				if (x < -TILE_SIZE * 3 || y < -TILE_SIZE * 3 || x >= viewWidth + TILE_SIZE * 3 || y >= viewHeight + TILE_SIZE * 3) {
					continue;
				}
				short sprite = objects[objIdx + 2];
				if ((sprite & (1 << 12)) != 0) {
					continue; // hidden
				}
				int w = (sprite & (3 << 8)) >> 4, h = (sprite & (3 << 10)) >> 6;
				sprite &= 0xFF;

				// animate
				int speed = topObjects[idx + 2];
				if (speed != 0) {
					int obj = objects[objIdx + 1] & 0xFF;
					if (obj == Objects.SECURITY_CAMERA) {
						sprite += (short) Math.abs(((ticks / speed) % 8) - 4);

						// camera vision TODO
//						if (player.layer == 0) { // TODO camera update cooldown?
//							int dx = x - px, dy = y - py;
//							if (dx * dx + dy * dy < NPC_VIEW_DISTANCE && canSee(x, y, px, py, 0)) {
//								if (player.searching || player.carry != null || player.climbed || lockdown
//										|| (player.isInZone(Map.ZONE_INMATE_CELL) && !player.isInZone(Map.ZONE_PLAYER_CELL))) {
//									// TODO alarm nearby guard to come here
//									// TODO heat timeout
////									player.heat += 10;
//								}
//							}
//						}
					} else {
						int numSprites;
						if (obj == Objects.CONTRABAND_DETECTOR) {
							numSprites = 2;
						} else {
							continue;
						}
						sprite += (short) ((ticks / speed) % numSprites);
					}
				}

				g.drawRegion(objectsImg, ((sprite & 0xFF) % TILE_SIZE) * TILE_SIZE, (sprite / TILE_SIZE) * TILE_SIZE, w, h, 0, x, y - h + TILE_SIZE, 0);
			}
		}

		Profiler.beginRenderSection(Profiler.RENDER_3D);

		// lights

		if (USE_M3G) {
			setup3D(g, viewWidth, viewHeight);
			if (use3D) {
				if (DRAW_LIGHTS && layer == LAYER_GROUND && lights != null) {
					Transform t = transform;
					short[] lights = this.lights;
					int n = lights[0];
					for (int i = 0; i < n; ++i) {
						int idx = i << 1;

						int x = lights[idx + 1] * TILE_SIZE - viewX, y = lights[idx + 2] * TILE_SIZE - viewY;
						// off-screen culling
						if (x < -TILE_SIZE * 3 || y < -TILE_SIZE * 3 || x >= viewWidth + TILE_SIZE * 3 || y >= viewHeight + TILE_SIZE * 3) {
							continue;
						}

						t.setIdentity();
						t.postTranslate(x + TILE_SIZE - viewWidth / 2f, viewHeight / 2f - y, 5);
						graphics3D.render(lightVertexBuffer, lightStrip, lightAppearance, t);
					}
				}

				transform.setIdentity();

				// vent tint
				if (layer == LAYER_GROUND && (player.climbed || player.layer == LAYER_VENT)) {
					globalVertexBuffer.setDefaultColor(0x7F7F7F);
					graphics3D.render(globalVertexBuffer, globalStrip, globalAppearance, transform);
				}

				// global lighting
				if ((time < 7 * 60 + 128 || time > 21 * 60)
						&& (player.climbed ? layer == LAYER_VENT : layer == player.layer)) {
					globalVertexBuffer.setDefaultColor(globalLightColor);
					graphics3D.render(globalVertexBuffer, globalStrip, globalAppearance, transform);
				}
				release3D();
			}
		}

		// characters dialogs
		for (int i = 0; i < renderChars.length; ++i) {
			NPC npc = renderChars[i];
			if (npc == null || npc.layer != layer || !npc.visible) continue;

			int x = (int) npc.x - viewX, y = (int) npc.y - viewY;
			if (npc.dialog != null) {
				String s = npc.dialog;
				int w = textWidth(s, FONT_REGULAR);
				fontColor = FONT_COLOR_BLACK;
				g.setColor(!npc.ai ? 0xFFFF57 : npc.bodyId == Textures.GUARD ? 0xA9E6FC : 0xFFFFFF);
				g.fillRect(x + 8 - (w >> 1) - 3, y - 18, w + 6, 15);
				g.setColor(0);
				g.drawRect(x + 8 - (w >> 1) - 3, y - 18, w + 6, 15);
				drawText(g, s, x + 8 - (w >> 1), y - 15, FONT_REGULAR);
				fontColor = FONT_COLOR_WHITE;
			}
			if (interactNPC == npc && npc.name != null) {
				String s = npc.name;
				fontColor = npc.bodyId == Textures.GUARD ? FONT_COLOR_LIGHTBLUE : FONT_COLOR_YELLOW;
				int w = textWidth(s, FONT_REGULAR);
				drawText(g, s, x + 8 - (w >> 1), y + 15, FONT_REGULAR);
				fontColor = FONT_COLOR_WHITE;
			}

			if (player.chaseTarget == npc) {
				// hp bar
				g.setColor(0xFF0000);
				g.drawRect(x - 1, y - 1, TILE_SIZE + 2, TILE_SIZE + 2);
				g.drawRect(x + (TILE_SIZE >> 1) - 12, y + TILE_SIZE + 3, 23, 6);
				int w = (npc.health * 20) / (npc.statStrength >> 1);
				if (w > 0) {
					g.fillRect(x + 8 - 10, y + 21, w, 3);
				}
			}
		}

//		// zones debug
//		int[] zones = this.zones;
//		int n = zones[0];
//		for (int i = 0; i < n; ++i) {
//			int idx = i * 5 + 1;
//			g.setColor(0xFFFFFF);
//			g.drawString(Integer.toString(zones[idx + 4]), zones[idx] - viewX + 2, zones[idx + 1] - viewY + 2, 0);
//			g.setColor(0x00FF00);
//			g.drawRect(zones[idx] - viewX, zones[idx + 1] - viewY, zones[idx + 2] - zones[idx], zones[idx + 3] - zones[idx + 1]);
//		}
	}

// endregion Map

// region 3D
	Graphics3D graphics3D;
	Transform cameraTransform;
	Camera camera;

	Transform transform;

	VertexBuffer lightVertexBuffer;
	TriangleStripArray lightStrip;
	Appearance lightAppearance;

	VertexBuffer globalVertexBuffer;
	TriangleStripArray globalStrip;
	Appearance globalAppearance;

	int globalLightColor;

	private void setup3D(Graphics g, int viewWidth, int viewHeight) {
		if (!use3D) return;

		try {
			if (graphics3D == null) {
				graphics3D = Graphics3D.getInstance();
			}

			// init camera
			if (camera == null) {
				cameraTransform = new Transform();
				cameraTransform.postTranslate(0, 0, 10.0f);
				camera = new Camera();
				camera.setParallel(viewHeight, (float) viewWidth / (float) viewHeight, 0.1f, 100.0f);

				transform = new Transform();
			}

			// init light sprite
			if (DRAW_LIGHTS && lightVertexBuffer == null) {
				short[] vertices = {
						(short) -TILE_SIZE * 2, (short) -TILE_SIZE * 2, 0,
						(short) TILE_SIZE * 2,  (short) -TILE_SIZE * 2, 0,
						(short) TILE_SIZE * 2,  (short) TILE_SIZE * 2,  0,
						(short) -TILE_SIZE * 2, (short) TILE_SIZE * 2,  0
				};

				int[] strips = { 0, 1, 3, 2 };
				int[] stripLengths = { 4 };

				short[] texCoords = {
						0, 255,
						255, 255,
						255, 0,
						0, 0
				};

				VertexArray vertArray = new VertexArray(vertices.length / 3, 3, 2);
				vertArray.set(0, vertices.length / 3, vertices);

				VertexArray texArray = new VertexArray(texCoords.length / 2, 2, 2);
				texArray.set(0, texCoords.length / 2, texCoords);

				lightVertexBuffer = new VertexBuffer();
				lightVertexBuffer.setPositions(vertArray, 1f, null);
				lightVertexBuffer.setTexCoords(0, texArray, 1.0f/255.0f, null);
				lightVertexBuffer.setDefaultColor(0xFFFFFFFF);

				lightStrip = new TriangleStripArray(strips, stripLengths);

				Texture2D tex = new Texture2D(light3dTexture);
				tex.setBlending(Texture2D.FUNC_REPLACE);

				lightAppearance = new Appearance();
				lightAppearance.setTexture(0, tex);

				CompositingMode cm = new CompositingMode();
				cm.setBlending(CompositingMode.ALPHA_ADD);
				cm.setAlphaThreshold(0.5f);
				lightAppearance.setCompositingMode(cm);

				PolygonMode pm = new PolygonMode();
				pm.setShading(PolygonMode.SHADE_FLAT);
				lightAppearance.setPolygonMode(pm);
			}

			// init global lighting quad
			if (globalStrip == null) {
				short[] vertices = {
						(short) -viewWidth, (short) -viewHeight, 0,
						(short) viewWidth,  (short) -viewHeight, 0,
						(short) viewWidth,  (short) viewHeight, 0,
						(short) -viewWidth, (short)  viewHeight, 0
				};

				int[] strips = { 0, 1, 3, 2 };
				int[] stripLengths = { 4 };

				VertexArray vertArray = new VertexArray(vertices.length / 3, 3, 2);
				vertArray.set(0, vertices.length / 3, vertices);

				globalVertexBuffer = new VertexBuffer();
				globalVertexBuffer.setPositions(vertArray, 0.5f, null);

				globalStrip = new TriangleStripArray(strips, stripLengths);

				globalAppearance = new Appearance();

				CompositingMode cm = new CompositingMode();
				cm.setBlending(CompositingMode.MODULATE);
				globalAppearance.setCompositingMode(cm);

				PolygonMode pm = new PolygonMode();
				pm.setShading(PolygonMode.SHADE_FLAT);
				globalAppearance.setPolygonMode(pm);

				update3DLightingColor();
			}

			graphics3D.bindTarget(g, false, 0);
			graphics3D.setCamera(camera, cameraTransform);
		} catch (Throwable e) {
			e.printStackTrace();
			use3D = false;
		}
	}

	private void release3D() {
		if (graphics3D == null) return;
		graphics3D.releaseTarget();
	}

	private void update3DLightingColor() {
		int r = 255, g = 255, b = 255;
		int add = 0;
		if (time > 7 * 60 && time < 7 * 60 + 128) {
			add = 128 - time + 7 * 60;
		} else if (time > 21 * 60 && time < 21 * 60 + 128) {
			add = time - 21 * 60;
		} else if (time <= 7 * 60 || time >= 23 * 60) {
			add = 128;
		}
		r -= ((r * add * 390) >> 16);
		g -= ((g * add * 385) >> 16);
		b -= ((b * add * 260) >> 16);
		globalLightColor = (r << 16) | (g << 8) | b;
	}

// endregion 3D

// region Map items

	int[][] droppedItems; // TODO dynamic array

	int dropItem(int x, int y, int item, int layer) throws IllegalStateException {
		final int pos = x + y * width;
		// TODO check object collision
		if (solid[layer][pos] != COLL_NONE)
			return -2;
//		if (getObjectIdxAt(x, y, layer) != -1)
//			return -2;
		if (droppedItems[layer][pos] != Items.ITEM_NULL)
			return -1;
		droppedItems[layer][pos] = item & Items.ITEM_MASK;
		return 0;
	}

	int peekItem(int x, int y, int layer) {
		int item = droppedItems[layer][x + y * width];
		if (item == Items.ITEM_NULL) {
			return item;
		}
		return item & Items.ITEM_MASK;
	}

	int pickItem(int x, int y, int layer) {
		int item = peekItem(x, y, layer);
		if (item != Items.ITEM_NULL) {
			droppedItems[layer][x + y * width] = Items.ITEM_NULL;
		}
		return item;
	}

// endregion Map items

// region Map zones

	int[] zones; // int[5] {count, [x,y,x1,x2,type], ...}

	boolean isInZone(int x, int y, int zone) {
		int[] zones = this.zones;
		int n = zones[0];
		for (int i = 0; i < n; ++i) {
			int idx = i * 5 + 1;
			if (zones[idx + 4] != zone) continue;
			if (x >= zones[idx] && x <= zones[idx + 2] &&
					y >= zones[idx + 1] && y <= zones[idx + 3]) {
				return true;
			}
		}
		return false;
	}

// endregion Map zones

// region Map objects

	short[][] objects; // {count, [object, sprite, x, y], ...} for each layer
	short[] topObjects; // {count, [objectIdx, animation], ...}
	short[] lights; // {count, [x, y], ...}
	short[] dirt; // {[x, y, idx], ...}

	int[] containers; // {count, [objectIdx, owner/type, itemsCount, items...], ... }

	// inmate waypoints
	short[] roamPositions;
	short[] rollcallPositions;
	short[] canteenServingPositions;
	short[] canteenSeatsPositions;
	short[] showerPositions;
	short[] gymPositions;

	// guard waypoints
	short[] guardRoamPositions;
	short[] guardRollcallPositions;
	short[] guardCanteenPositions;
	short[] guardGymPositions;
	short[] guardShowerPositions;
	short[] guardBeds;

	int nextDirtPos;

	void updateDoors() {
		short[] objects = this.objects[LAYER_GROUND];
		if (objects == null) {
			return;
		}

		short cell = cellsClosed ? (short) 33 : (short) 32;
		short purple = time >= 10 * 60 && time < 22 * 60 ? (short) 32 : (short) 36;

		int n = objects[0];
		for (int i = 0; i < n; ++i) {
			int idx = i << 2;
			int obj = objects[idx + 1];
			if (obj == Objects.DOOR_CELL) {
				objects[idx + 2] = (short) (cell | (objects[idx + 2] & 0xFF00));
				continue;
			}
			if (obj == Objects.DOOR_OUTSIDE) {
				objects[idx + 2] = (short) (purple | (objects[idx + 2] & 0xFF00));
				continue;
			}
			if (obj == Objects.DOOR_PRISON_ENTRANCE) {
				objects[idx + 2] = (short) ((entranceOpen ? (short) 32 : (short) 35) | (objects[idx + 2] & 0xFF00));
				continue;
			}
			// update serving tables on meal time
			if (obj == Objects.SERVING_TABLE) {
				if (schedule != SC_BREAKFAST && schedule != SC_EVENING_MEAL) {
					objects[idx + 2] = (short) (2 | (objects[idx + 2] & 0xFF00));
					continue;
				}
				if (time % 60 == 0 || prevSchedule == SC_LOCKDOWN) {
					objects[idx + 2] = (short) (7 | (objects[idx + 2] & 0xFF00));
				}
				continue;
			}
		}
	}

	void updateDirt(int n) {
		int objIdx = dirt[n * 3 + 2];

		while (true) {
			int pos = nextDirtPos++;
			if (pos >= roamPositions[0]) {
				pos = nextDirtPos = 0;
			}
			int x = roamPositions[(pos << 1) + 1];
			int y = roamPositions[(pos << 1) + 2];

			if (isFloor(tiles[0][x + y * width]) == (objects[0][objIdx + 1] == Objects.OUTSIDE_DIRT)) {
				continue;
			}

			objects[0][objIdx + 3] = dirt[n * 3] = (short) x;
			objects[0][objIdx + 4] = dirt[n * 3 + 1] = (short) y;
			break;
		}
	}

	boolean addObject(int object, int sprite, int w, int h, int x, int y, int layer) {
		short[] objects = this.objects[layer];
		int idx = (objects[0]++) << 2;

		objects[idx + 1] = (short) object;
		objects[idx + 2] = (short) ((sprite) | ((w & 0x3) << 8) | ((h & 0x3) << 10));
		objects[idx + 3] = (short) x;
		objects[idx + 4] = (short) y;

		return true;
	}

	int getObjectIdxAt(int x, int y, int layer) {
		short[] objects = this.objects[layer];
		if (objects != null) {
			int n = objects[0] << 2;
			for (int idx = 0; idx < n; idx += 4) {
				int ox = objects[idx + 3], oy = objects[idx + 4];
				short sprite = objects[idx + 2];
				int w = (sprite & (3 << 8)) >> 8, h = (sprite & (3 << 10)) >> 10;
				if ((x == ox && y == oy)
						// check dimensions of non-single tile objects
						|| (x >= ox && x < ox + w && y > oy - h && y <= oy)) {
					return idx;
				}
			}
		}
		return -1;
	}

	int findObject(int obj, int layer, int startIdx) {
		short[] objects = this.objects[layer];
		if (objects != null) {
			int n = objects[0] << 2;
			for (int idx = startIdx; idx < n; idx += 4) {
				if ((objects[idx + 1] & 0xFF) == obj) {
					return idx;
				}
			}
		}
		return -1;
	}

	int getSeatIndex(short[] positions, int x, int y) {
		int n = positions[0];
		for (int i = 0; i < n; ++i) {
			int idx = (i << 1) + 1;
			int pos = positions[idx];
			if ((pos & 0xFF) == x && (pos >> 8 & 0xFF) == y) {
				return idx;
			}
		}
		return -1;
	}

// endregion Map objects

// region Containers

	int getContainer(int objIdx) {
		int[] containers = this.containers;
		if (containers == null) return - 1;
		int n = containers[0];
		int idx = 1;
		for (int i = 0; i < n; ++i) {
			if (containers[idx] == objIdx) {
				return idx;
			}
			idx += containers[idx + 2] + 3;
		}
		return -1;
	}

	int getContainerByOwner(int npcId) {
		int[] containers = this.containers;
		if (containers == null) return - 1;
		int n = containers[0];
		int idx = 1;
		for (int i = 0; i < n; ++i) {
			if (containers[idx + 1] == npcId) {
				return idx;
			}
			idx += containers[idx + 2] + 3;
		}
		return -1;
	}

	void openContainer(int objIdx) {
		int idx = getContainer(objIdx);
		if (idx == -1) return;
		if (containers[idx + 1] == -Objects.TOILET) {
			toilet = true;
		} else {
			toilet = false;
		}
		lastSelectedInventory = selectedInventory;
		selectedInventory = -1;
		containerOpen = idx;
		selectedSlot = 0;
		Sound.playEffect(Sound.SFX_OPEN);
	}

	// from inventory to container
	boolean putItem(int idx, int slot) {
		if (slot == -1) return false;
		int[] containers = this.containers;
		int slots = containers[idx + 2];
		int item = player.inventory[slot];
		if (item == Items.ITEM_NULL) return false;

		for (int i = 0; i < slots; ++i) {
			if (containers[idx + 3 + i] == Items.ITEM_NULL) {
				containers[idx + 3 + i] = item;
				player.inventory[slot] = Items.ITEM_NULL;
				return true;
			}
		}
		Sound.playEffect(Sound.SFX_LOSE);
		return false;
	}

	// from container to inventory
	boolean takeItem(int idx, int slot) {
		if (slot == -1) return false;
		int item = containers[idx + 3 + slot];
		if (player.addItem(item, true)) {
			containers[idx + 3 + slot] = Items.ITEM_NULL;
			return true;
		}
		return false;
	}

	void removeIllegalItems(int idx) {
		int slots = containers[idx + 2];
		for (int i = 0; i < slots; ++i) {
			if (isIllegal(containers[idx + 3 + i] & Items.ITEM_ID_MASK)) {
				containers[idx + 3 + i] = Items.ITEM_NULL;
			}
		}
	}

// endregion Container

// region Solid

	// tiles

	static boolean isFloor(byte tile) {
		switch (tile) {
		// TODO
		case 1:
		case 2:
		case 3:
		case 5:
		case 9:
			return true;
		}
		return false;
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
		case 43:
		case 45:
		case 46:
		case 47:
		case 49:
		case 50:
		case 51:
		case 52:
		case 53:
		case 55:
		case 56:
		case 57:
		case 60:
		case 61:
		case 62:
		case 64:
		case 65:
		case 66:
		case 68:
		case 77:
		case 81:
		case 85:
		case 89:
		case 92:
		case 93:
		case 96:
		case 97:
		case 99:
		case 100:
			return COLL_SOLID;
		case 23:
		case 34:
			return COLL_SOLID_TRANSPARENT;
		case 24:
		case 94:
		// water
		case 54:
		case 58:
		case 79:
		case 83:
		case 87:
		case 90:
		case 91:
		case 95:
			return COLL_SOLID_NO_SHADOW;
		}
		return COLL_NONE;
	}

	// objects

	static byte isSolidObject(int i) {
		// TODO
		switch (i) {
		case Objects.TOILET:
		case Objects.CHAIR:
		case Objects.CABINET:
		case Objects.FREEZER:
		case Objects.OVEN:
		case Objects.BED:
		case Objects.SIDEWAYS_PRISONER_BED:
		case Objects.PLAYER_BED:
		case Objects.PLAYER_SIDEWAYS_BED:
		case Objects.SOLITARY_BED:
		case Objects.MEDICAL_BED:
		case Objects.PAYPHONE:
		case Objects.CABLE_TV:
		case Objects.JOB_DIRTY_LAUNDRY:
		case Objects.JOB_CLEAN_LAUNDRY:
		case Objects.WASHING_MACHINE:
		case Objects.STASH:
		case Objects.JOB_RAW_METAL:
		case Objects.JOB_PREPARED_METAL:
		case Objects.JOB_RAW_WOOD:
		case Objects.JOB_PREPARED_WOOD:
		case Objects.JOB_SELECTION:
		case Objects.SINK:
		case Objects.GUARD_BED:
		case Objects.VENT_SLATS:
		case Objects.TRAINING_BOOKSHELF:
		case Objects.VISITATION_GUEST_SEAT:
		case Objects.VISITATION_PLAYER_SEAT:
		case Objects.JOB_GARDENING_TOOLS:
		case Objects.JOB_CLEANING_SUPPLIES:
		case Objects.MEDICAL_SUPPLIES:
		case Objects.JOB_MAILROOM_FILE:
		case Objects.JOB_METAL_TOOLS:
		case Objects.JOB_FABRIC_CHEST:
		case Objects.JOB_CLOTHING_STORAGE:
		case Objects.JOB_DELIVERIES_BLUE_BOX:
		case Objects.JOB_DELIVERIES_RED_BOX:
		case Objects.JOB_DELIVERIES_TRUCK:
		case Objects.JOB_BOOK_CHEST:
		case Objects.GENERATOR:
		case Objects.SUN_LOUNGER:
			return Game.COLL_SOLID_INTERACT;
		case Objects.DINING_TABLE:
		case Objects.SERVING_TABLE:
		case Objects.CUTLERY_TABLE:
		case Objects.TRAINING_INTERNET:
			return Game.COLL_TABLE;
		case Objects.DESK:
		case Objects.PLAYER_DESK:
			return Game.COLL_DESK;
		case Objects.TRAINING_TREADMILL:
		case Objects.TRAINING_WEIGHT:
			return Game.COLL_GYM;
		case Objects.DOOR_CELL:
		case Objects.DOOR_OUTSIDE:
			return Game.COLL_DOOR;
		case Objects.DOOR_UTILITY:
		case Objects.DOOR_STAFF:
		case Objects.DOOR_KITCHEN:
		case Objects.DOOR_LAUNDRY:
		case Objects.DOOR_JANITOR:
		case Objects.DOOR_METALSHOP:
		case Objects.DOOR_LIBRARIAN:
		case Objects.DOOR_WOODSHOP:
		case Objects.DOOR_PRISON_ENTRANCE:
		case Objects.DOOR_DELIVERIES:
		case Objects.DOOR_MAILROOM:
		case Objects.DOOR_GARDENING:
		case Objects.DOOR_TAILORSHOP:
		case Objects.DOOR_JUNGLE_ENTRANCE:
		case Objects.DOOR_UTILITY_VENT:
			return Game.COLL_DOOR_STAFF;
		case Objects.SHOWER:
			return Game.COLL_SHOWER;
		case Objects.CONTRABAND_DETECTOR:
			return Game.COLL_DETECTOR;
		case Objects.VENT:
			return Game.COLL_NOT_SOLID_INTERACT;
		}
		return Game.COLL_NONE;
	}

// endregion Solid

// region Items

	static final int[] DESK1 = {
			Items.PACK_OF_MINTS,
			Items.LIGHTER,
			Items.WATCH,
			Items.BOTTLE_OF_MEDICINE,
			Items.SHAVING_CREAM,
			Items.MAGAZINE,
			Items.COMB,
			Items.PLASTIC_SPOON,
			Items.PLASTIC_KNIFE,
			Items.PLASTIC_FORK,
			Items.JAR_OF_INK,
			Items.TUB_OF_BLEACH,
			Items.TUBE_OF_TOOTHPASTE,
			Items.BAR_OF_CHOCOLATE,
			Items.ROLL_OF_TOILET_PAPER,
			Items.SOAP,
			Items.PACK_OF_PLAYING_CARDS,
			Items.BOOK,
			Items.TUBE_OF_SUPER_GLUE,
			Items.TUB_OF_TALCUM_POWDER,
			Items.DIRTY_INMATE_OUTFIT,
			Items.BATTERY,
			Items.SOCK,
			Items.PLUNGER,
			Items.CUP,
			Items.PAPER_CLIP,
			Items.RAZOR_BLADE,
			Items.TOOTHBRUSH,
			Items.WIRE,
			Items.DENTAL_FLOSS,
			Items.TV_REMOTE,
			Items.NAILS,
//			// perks
//			Items.SPONGE,
//			Items.DVD,
//			Items.COOKIE,
//			Items.MUFFIN,
//			Items.SILK_HANDKERCHIEF,
//			Items.DELUXE_TOILET_ROLL,
//			Items.TEDDY_BEAR,
//			Items.HAND_CREAM,
//			Items.POSTCARD,
//			Items.PEDICURE_KIT,
//			Items.HAND_FAN,
//			// stalagflucht
//			Items.POCKET_WATCH,
//			Items.FAMILY_PHOTO,
//			Items.SERVICE_MEDAL,
//			Items.DOG_TAG,
//			// jungle
//			Items.BANANAS,
//			Items.GREEN_HERB,
//			Items.VINES,
//			Items.COCONUT,
//			Items.MANGO,
//			Items.TRIBAL_DRUM,
//			// sanpancho
//			Items.RED_CHILI,
//			Items.SAND,
//			Items.SOMBRERO,
//			Items.PONCHO,
//			Items.BURRITO,
	};

	static final int[] DESK2 = {
			Items.TIMBER,
			Items.ROLL_OF_DUCT_TAPE,
			Items.DIRT,
			Items.SPATULA,
			Items.FLASHLIGHT,
			Items.FILE,
			Items.COOKED_FOOD,
			Items.SHEET_OF_METAL,
			Items.FOIL,
			Items.COMB_SHIV,
			Items.CRAFTING_NOTE,
			Items.COMB_BLADE,
			Items.MEDIKIT,
//			// jungle
//			Items.RED_HERB,
	};

	static final int[] NPC_CARRY = {
			Items.PACK_OF_MINTS,
			Items.LIGHTER,
			Items.WATCH,
			Items.BOTTLE_OF_MEDICINE,
			Items.TIMBER,
			Items.ROLL_OF_DUCT_TAPE,
			Items.SHAVING_CREAM,
			Items.MAGAZINE,
			Items.BOTTLE_OF_SLEEPING_PILLS,
			Items.COMB,
			Items.GLASS_SHARD,
			Items.SPATULA,
			Items.BROOM,
			Items.MOP,
			Items.JAR_OF_INK,
			Items.TUB_OF_BLEACH,
			Items.TROWEL,
			Items.BAR_OF_CHOCOLATE,
			Items.ROLL_OF_TOILET_PAPER,
			Items.SOAP,
			Items.PACK_OF_PLAYING_CARDS,
			Items.BOOK,
			Items.TUBE_OF_SUPER_GLUE,
			Items.TUB_OF_TALCUM_POWDER,
			Items.FILE,
			Items.COOKED_FOOD,
			Items.BED_SHEET,
			Items.SOCK,
			Items.PLUNGER,
			Items.RAZOR_BLADE,
			Items.TOOTHBRUSH,
			Items.WIRE,
			Items.CRAFTING_NOTE,
			Items.DENTAL_FLOSS,
			Items.NAILS,
//			// perks
//			Items.TV_REMOTE,
//			Items.SPONGE,
//			Items.DVD,
//			Items.COOKIE,
//			Items.MUFFIN,
//			Items.SILK_HANDKERCHIEF,
//			Items.DELUXE_TOILET_ROLL,
//			Items.TEDDY_BEAR,
//			Items.HAND_CREAM,
//			Items.POSTCARD,
//			Items.PEDICURE_KIT,
//			Items.HAND_FAN,
//			// stalagflucht
//			Items.POCKET_WATCH,
//			Items.FAMILY_PHOTO,
//			Items.SERVICE_MEDAL,
//			Items.DOG_TAG,
//			// jungle
//			Items.BANANAS,
//			Items.GREEN_HERB,
//			Items.RED_HERB,
//			Items.VINES,
//			Items.COCONUT,
//			Items.MANGO,
//			Items.TRIBAL_DRUM,
//			// sanpancho
//			Items.RED_CHILI,
//			Items.SAND,
//			Items.SOMBRERO,
//			Items.PONCHO,
//			Items.BURRITO,

	};

	static final int[] BUY = {
			Items.GUARD_OUTFIT, 50,
			Items.LIGHTER, 25,
			Items.WATCH, 25,
			Items.BOTTLE_OF_MEDICINE, 20,
			Items.TIMBER, 10,
			Items.ROLL_OF_DUCT_TAPE, 30,
			Items.SHAVING_CREAM, 25,
			Items.MAGAZINE, 15,
			Items.BOTTLE_OF_SLEEPING_PILLS, 25,
			Items.SCREWDRIVER, 75,
			Items.CROWBAR, 75,
			Items.BATON, 60,
			Items.HAMMER, 70,
			Items.JAR_OF_INK, 25,
			Items.LENGTH_OF_ROPE, 60,
			Items.BAR_OF_CHOCOLATE, 15,
			Items.TUBE_OF_SUPER_GLUE, 30,
			Items.TUB_OF_TALCUM_POWDER, 15,
//			Items.BALSA_WOOD, 50, // irongate
			Items.FLASHLIGHT, 30,
			Items.FILE, 50,
			Items.STEPLADDER, 60,
			Items.SHEET_OF_METAL, 25,
			Items.RAZOR_BLADE, 20,
			Items.WIRE, 25,
			Items.FOIL, 30,
			Items.CRAFTING_NOTE, 25,
			Items.MEDIKIT, 30,
//			Items.EXOTIC_FEATHER, 30, // jungle
//			Items.GREEN_HERB, 15,
//			Items.RED_HERB, 25,
			Items.NAILS, 40,
	};

	static int getItemDecay(int id) {
		switch (id & Items.ITEM_ID_MASK) {
		// TODO
		}
		return 0;
	}

	static String getItemName(int id) {
		switch (id & Items.ITEM_ID_MASK) {
		// TODO
		case Items.CELL_KEY:
			return "Cell Key";
		case Items.STAFF_KEY:
			return "Staff Key";
		case Items.PACK_OF_MINTS:
			return "Pack of Mints";
		case Items.GUARD_OUTFIT:
			return "Guard Outfit";
		case Items.INMATE_OUTFIT:
			return "Inmate Outfit";
		case Items.STURDY_SHOVEL:
			return "Sturdy Shovel";
		case Items.ENTRANCE_KEY:
			return "Entrance Key";
		case Items.UTILITY_KEY:
			return "Utility Key";
		case Items.LIGHTER:
			return "Lighter";
		case Items.WATCH:
			return "Watch";
		case Items.BOTTLE_OF_MEDICINE:
			return "Bottle of Medicine";
		case Items.STUN_ROD:
			return "Stun Rod";
		case Items.TIMBER:
			return "Timber";
		case Items.ROLL_OF_DUCT_TAPE:
			return "Roll of Duct Tape";
		case Items.SHAVING_CREAM:
			return "Shaving Cream";
		case Items.MAGAZINE:
			return "Magazine";
		case Items.BOTTLE_OF_SLEEPING_PILLS:
			return "Bottle of Sleeping Pills";
		case Items.COMB:
			return "Comb";
		case Items.GLASS_SHARD:
			return "Glass Shard";
		case Items.GLASS_SHANK:
			return "Glass Shank";
		case Items.DEAD_RAT:
			return "Dead Rat";
		case Items.FIRE_EXTINGUISHER:
			return "Fire Extinguisher";
		case Items.RADIO_RECEIVER:
			return "Radio Receiver";
		case Items.PLASTIC_SPOON:
			return "Plastic Spoon";
		case Items.SCREWDRIVER:
			return "Screwdriver";
		case Items.CROWBAR:
			return "Crowbar";
		case Items.DIRT:
			return "Dirt";
		case Items.PLASTIC_KNIFE:
			return "Plastic Knife";
		case Items.PLASTIC_FORK:
			return "Plastic Fork";
		case Items.BATON:
			return "Baton";
		case Items.SPATULA:
			return "Spatula";
		case Items.HOE:
			return "Hoe";
		case Items.BROOM:
			return "Broom";
		case Items.SHEARS:
			return "Shears";
		case Items.HAMMER:
			return "Hammer";
		case Items.MOP:
			return "Mop";
		case Items.JAR_OF_INK:
			return "Jar of Ink";
		case Items.TUB_OF_BLEACH:
			return "Tub of Bleach";
		case Items.INFIRMARY_OVERALLS:
			return "Infirmary Overalls";
		case Items.STURDY_PICKAXE:
			return "Sturdy Pickaxe";
		case Items.TROWEL:
			return "Trowel";
		case Items.WALL_BLOCK:
			return "Wall Block";
		case Items.CONCRETE:
			return "Concrete";
		case Items.WORK_KEY:
			return "Work Key";
		case Items.LENGTH_OF_ROPE:
			return "Length of Rope";
		case Items.TUBE_OF_TOOTHPASTE:
			return "Tube of Toothpaste";
		case Items.BAR_OF_CHOCOLATE:
			return "Bar of Chocolate";
		case Items.ROLL_OF_TOILET_PAPER:
			return "Roll of Toiler Paper";
		case Items.SOAP:
			return "Soap";
		case Items.PACK_OF_PLAYING_CARDS:
			return "Pack of Playing Cards";
		case Items.BOOK:
			return "Book";
		case Items.TUBE_OF_SUPER_GLUE:
			return "Tube of Super Glue";
		case Items.TUB_OF_TALCUM_POWDER:
			return "Tub of Talcum Powder";
		case Items.WAD_OF_PUTTY:
			return "Wad of Putty";
		case Items.CANDLE:
			return "Candle";
		case Items.BALSA_WOOD:
			return "Balsa Wood";
		case Items.SAIL:
			return "Sail";
		case Items.PLASTIC_WORK_KEY:
			return "Plastic Work Key";
		case Items.WORK_KEY_MOLD:
			return "Work Key Mold";
		case Items.FLASHLIGHT:
			return "Flashlight";
		case Items.STURDY_CUTTERS:
			return "Strudy Cutters";
		case Items.FILE:
			return "File";
		case Items.DIRTY_GUARD_OUTFIT:
			return "Dirty Guard Outfit";
		case Items.DIRTY_INMATE_OUTFIT:
			return "Dirty Inmate Outfit";
		case Items.UNCOOKED_FOOD:
			return "Uncooked Food";
		case Items.COOKED_FOOD:
			return "Cooked Food";
		case Items.VENT_COVER:
			return "Vent Cover";
		case Items.STEPLADDER:
			return "Stepladder";
		case Items.BED_SHEET:
			return "Bed Sheet";
		case Items.WOODSHOP_KEY:
			return "Woodshop Key";
		case Items.STAFF_KEY_MOLD:
			return "Staff Key Mod";
		case Items.TIMBER_BRACE:
			return "Timber Brace";
		case Items.BATTERY:
			return "Battery";
		case Items.SOCK:
			return "Sock";
		case Items.UNVARNISHED_CHAIR:
			return "Unvarnished Chair";
		case Items.SHEET_OF_METAL:
			return "Sheet of Metal";
		case Items.LICENSE_PLATE:
			return "License Plate";
		case Items.METALSHOP_KEY:
			return "Metalshop Key";
		case Items.BAG_OF_CEMENT:
			return "Bag of Cement";
		case Items.PLUNGER:
			return "Plunger";
		case Items.SHEET_ROPE:
			return "Sheet Rope";
		case Items.UTILITY_KEY_MOLD:
			return "Utility Key Mold";
		case Items.CELL_KEY_MOLD:
			return "Cell Key Mold";
		case Items.MOLTEN_PLASTIC:
			return "Molten Plastic";
		case Items.PLASTIC_UTILITY_KEY:
			return "Plastic Utility Key";
		case Items.PLASTIC_STAFF_KEY:
			return "Plastic Staff Key";
		case Items.PLASTIC_CELL_KEY:
			return "Plastic Cell Key";
		case Items.PLASTIC_ENTRANCE_KEY:
			return "Plastic Entrance Key";
		case Items.ENTRANCE_KEY_MOLD:
			return "Entrance Key Mold";
		case Items.CUP:
			return "Cup";
		case Items.SMALL_SPEAKER:
			return "Small Speaker";
		case Items.CIRCUIT_BOARD:
			return "Circuit Board";
		case Items.PAPER_CLIP:
			return "Paper Clip";
		case Items.RAZOR_BLADE:
			return "Razor Blade";
		case Items.TOOTHBRUSH:
			return "Toothbrush";
		case Items.TOOTHBRUSH_SHIV:
			return "Toothbrush Shiv";
		case Items.WIRE:
			return "Wire";
		case Items.POSTER:
			return "Poster";
		case Items.FOIL:
			return "Foil";
		case Items.PAPER_MACHE:
			return "Paper Mache";
		case Items.DIY_TATOO_KIT:
			return "DIY Tatoo Kit";
		case Items.SOCK_MACE:
			return "Sock Mace";
		case Items.SUPER_SOCK_MACE:
			return "Super Sock Mace";
		case Items.CUP_OF_MOLTEN_CHOCOLATE:
			return "Cup of Molten Chocolate";
		case Items.GAME_SET:
			return "Game Set";
		case Items.DIE:
			return "Die";
		case Items.NUNCHUKS:
			return "Nunchuks";
		case Items.COMB_SHIV:
			return "Comb Shiv";
		case Items.WHIP:
			return "Whip";
		case Items.KNUCKLE_DUSTER:
			return "Knuckle Duster";
		case Items.GRAPPLE_HEAD:
			return "Grapple Head";
		case Items.GRAPPLING_HOOK:
			return "Grappling Hook";
		case Items.LIGHTWEIGHT_SHOVEL:
			return "Lightweight Shovel";
		case Items.FLIMSY_SHOVEL:
			return "Flimsy Shovel";
		case Items.FAKE_WALL_BLOCK:
			return "Fake Wall Block";
		case Items.FAKE_VENT_COVER:
			return "Fake Vent Cover";
		case Items.FLIMSY_PICKAXE:
			return "Flimsy Pickaxe";
		case Items.LIGHTWEIGHT_PICKAXE:
			return "Lightweight Pickaxe";
		case Items.FLIMSY_CUTTERS:
			return "Flimsy Cutters";
		case Items.LIGHTWEIGHT_CUTTERS:
			return "Lightweight Cutters";
		case Items.TOOL_HANDLE:
			return "Tool Handle";
		case Items.CRAFTING_NOTE:
			return "Crafting Note";
		case Items.COMB_BLADE:
			return "Comb Blade";
		case Items.CONTRABAND_POUCH:
			return "Contraband Pouch";
		case Items.BED_DUMMY:
			return "Bed Dummy";
		case Items.POW_OUTFIT:
			return "POW Outfit";
		case Items.CUSHIONED_INMATE_OUTFIT:
			return "Cushioned Inmate Outfit";
		case Items.PADDED_INMATE_OUTFIT:
			return "Padded Inmate Outfit";
		case Items.PLATED_INMATE_OUTFIT:
			return "Plated Inmate Outfit";
		case Items.MEDIKIT:
			return "Medikit";
		case Items.HAT:
			return "Hat";
		case Items.VEST:
			return "Vest";
		case Items.UNDERPANTS:
			return "Underpants";
		case Items.SHORTS:
			return "Shorts";
		case Items.NEEDLE_THREAD:
			return "Needle & Thread";
		case Items.FABRIC:
			return "Fabric";
		case Items.PACKAGE_A:
			return "Package";
		case Items.PACKAGE_B:
			return "Package";
		case Items.PACKAGE_C:
			return "Package";
		case Items.LETTER:
			return "Letter";
		case Items.CUSHIONED_POW_OUTFIT:
			return "Cushioned POW Outfit";
		case Items.PADDED_POW_OUTFIT:
			return "Padded POW Outfit";
		case Items.PLATED_POW_OUTFIT:
			return "Plated POW Outfit";
		case Items.TV_REMOTE:
			return "TV Remove";
		case Items.SPONGE:
			return "Sponge";
		case Items.DVD:
			return "DVD";
		case Items.COOKIE:
			return "Cookie";
		case Items.MUFFIN:
			return "Muffin";
		case Items.EXOTIC_FEATHER:
			return "Exotic Feather";
		case Items.BANANAS:
			return "Bananas";
		case Items.UNSIGNED_ID_PAPERS:
			return "Unsigned ID Papers";
		case Items.ID_PAPERS:
			return "ID Papers";
		case Items.GREEN_HERB:
			return "Green Herb";
		case Items.RED_HERB:
			return "Red Herb";
		case Items.SILK_HANDKERCHIEF:
			return "Silk Handkerchief";
		case Items.DENTAL_FLOSS:
			return "Dental Floss";
		case Items.TEDDY_BEAR:
			return "Teddy Bear";
		case Items.HAND_CREAM:
			return "Hand Cream";
		case Items.POSTCARD:
			return "Postcard";
		case Items.PEDICURE_KIT:
			return "Pedicure Kit";
		case Items.HAND_FAN:
			return "Hand Fan";
		case Items.POCKET_WATCH:
			return "Pocket Watch";
		case Items.FAMILY_PHOTO:
			return "Family Photo";
		case Items.SERVICE_MEDAL:
			return "Service Medal";
		case Items.DOG_TAG:
			return "Dog Tag";
		case Items.VINES:
			return "Vines";
		case Items.COCONUT:
			return "Coconut";
		case Items.MANGO:
			return "Mango";
		case Items.RED_CHILI:
			return "Red Chili";
		case Items.SAND:
			return "Sand";
		case Items.SOMBRERO:
			return "Sombrero";
		case Items.FAKE_FENCE:
			return "Fake Fence";
		case Items.PONCHO:
			return "Poncho";
		case Items.UNCOOKED_BURRITO:
			return "Uncooked Burrito";
		case Items.BURRITO:
			return "Burrito";
		case Items.TRIBAL_DRUM:
			return "Tribal Drum";
		case Items.NAILS:
			return "Nails";
		case Items.STINGER_STRIP:
			return "Stinger Strip";
		case Items.MULTITOOL:
			return "Multitool";
		case Items.WOODEN_BAT:
			return "Wooden Bat";
		case Items.SPIKED_BAT:
			return "Spiked Bat";
		case Items.DURABLE_CONTRABAND_POUCH:
			return "Durable Contraband Pouch";
		case Items.CUTTING_FLOSS:
			return "Cutting Floss";
		case Items.POWERED_SCREWDRIVER:
			return "Powered Screwdriver";
		case Items.RAFT_BASE:
			return "Raft Base";
		case Items.MAKESHIFT_RAFT:
			return "Makeshift Raft";
		case Items.ZIPLINE_HOOK:
			return "Zipline Hook";
		case Items.DODO_DONUT:
			return "DoDo Donut";
		case Items.PENARIUM_BARREL:
			return "Penarium Barrel";
		}
		return null;
	}

	static boolean isIllegal(int id) {
		switch (id & Items.ITEM_ID_MASK) {
		case Items.CELL_KEY:
		case Items.STAFF_KEY:
		case Items.GUARD_OUTFIT:
		case Items.STURDY_SHOVEL:
		case Items.ENTRANCE_KEY:
		case Items.UTILITY_KEY:
		case Items.STUN_ROD:
		case Items.TIMBER:
		case Items.ROLL_OF_DUCT_TAPE:
		case Items.BOTTLE_OF_SLEEPING_PILLS:
		case Items.GLASS_SHARD:
		case Items.GLASS_SHANK:
		case Items.FIRE_EXTINGUISHER:
		case Items.RADIO_RECEIVER:
		case Items.SCREWDRIVER:
		case Items.CROWBAR:
		case Items.DIRT:
		case Items.BATON:
		case Items.SPATULA:
		case Items.SHEARS:
		case Items.HAMMER:
		case Items.INFIRMARY_OVERALLS:
		case Items.STURDY_PICKAXE:
		case Items.TROWEL:
		case Items.WALL_BLOCK:
		case Items.WORK_KEY:
		case Items.LENGTH_OF_ROPE:
		case Items.WAD_OF_PUTTY:
		case Items.BALSA_WOOD:
		case Items.SAIL:
		case Items.PLASTIC_WORK_KEY:
		case Items.WORK_KEY_MOLD:
		case Items.STURDY_CUTTERS:
		case Items.FILE:
		case Items.DIRTY_GUARD_OUTFIT:
		case Items.VENT_COVER:
		case Items.STEPLADDER:
		case Items.STAFF_KEY_MOLD:
		case Items.TIMBER_BRACE:
		case Items.SHEET_OF_METAL:
		case Items.LICENSE_PLATE:
		case Items.SHEET_ROPE:
		case Items.UTILITY_KEY_MOLD:
		case Items.CELL_KEY_MOLD:
		case Items.MOLTEN_PLASTIC:
		case Items.PLASTIC_UTILITY_KEY:
		case Items.PLASTIC_STAFF_KEY:
		case Items.PLASTIC_CELL_KEY:
		case Items.PLASTIC_ENTRANCE_KEY:
		case Items.ENTRANCE_KEY_MOLD:
		case Items.SMALL_SPEAKER:
		case Items.CIRCUIT_BOARD:
		case Items.TOOTHBRUSH_SHIV:
		case Items.FOIL:
		case Items.PAPER_MACHE:
		case Items.SOCK_MACE:
		case Items.SUPER_SOCK_MACE:
		case Items.NUNCHUKS:
		case Items.COMB_SHIV:
		case Items.WHIP:
		case Items.KNUCKLE_DUSTER:
		case Items.GRAPPLE_HEAD:
		case Items.GRAPPLING_HOOK:
		case Items.LIGHTWEIGHT_SHOVEL:
		case Items.FLIMSY_SHOVEL:
		case Items.FAKE_WALL_BLOCK:
		case Items.FAKE_VENT_COVER:
		case Items.FLIMSY_PICKAXE:
		case Items.LIGHTWEIGHT_PICKAXE:
		case Items.FLIMSY_CUTTERS:
		case Items.LIGHTWEIGHT_CUTTERS:
		case Items.TOOL_HANDLE:
		case Items.CRAFTING_NOTE:
		case Items.COMB_BLADE:
		case Items.CONTRABAND_POUCH:
		case Items.CUSHIONED_INMATE_OUTFIT:
		case Items.PADDED_INMATE_OUTFIT:
		case Items.PLATED_INMATE_OUTFIT:
		case Items.CUSHIONED_POW_OUTFIT:
		case Items.PADDED_POW_OUTFIT:
		case Items.PLATED_POW_OUTFIT:
		case Items.UNSIGNED_ID_PAPERS:
		case Items.ID_PAPERS:
		case Items.SAND:
		case Items.FAKE_FENCE:
		case Items.STINGER_STRIP:
		case Items.MULTITOOL:
		case Items.WOODEN_BAT:
		case Items.SPIKED_BAT:
		case Items.DURABLE_CONTRABAND_POUCH:
		case Items.CUTTING_FLOSS:
		case Items.POWERED_SCREWDRIVER:
		case Items.RAFT_BASE:
		case Items.MAKESHIFT_RAFT:
		case Items.ZIPLINE_HOOK:
			return true;
		}
		return false;
	}

	static int getItemAttack(int id) {
		switch (id & Items.ITEM_ID_MASK) {
		case Items.PLASTIC_KNIFE:
		case Items.PILLOW:
		case Items.PLASTIC_FORK:
			return 1;
		case Items.MOP:
		case Items.COMB_SHIV:
		case Items.COMB_BLADE:
		case Items.TOOTHBRUSH_SHIV:
			return 2;
		case Items.CROWBAR:
		case Items.SOCK_MACE:
		case Items.SUPER_SOCK_MACE:
		case Items.WOODEN_BAT:
		case Items.HAMMER:
		case Items.BATON:
			return 3;
		case Items.GLASS_SHANK:
		case Items.KNUCKLE_DUSTER:
		case Items.SPIKED_BAT:
			return 4;
		case Items.NUNCHUKS:
		case Items.WHIP:
			return 5;
//		case Items.SHOE_KNIFE:
//			return 3;
//		case Items.METAL_RIMMED_HAT:
//			return 4;
//		case Items.STUN_PEN:
//			return 3;
//		case Items.BASEBALL_BAT:
//			return 4;
		}
		return 0;
	}

	static int getItemHeal(int id) {
		switch (id & Items.ITEM_ID_MASK) {
		case Items.BOTTLE_OF_MEDICINE:
		case Items.GREEN_HERB:
			return 5;
		case Items.RED_HERB:
			return 10;
		case Items.MEDIKIT:
			return 15;
		default:
			return 0;
		}
	}

	static int getItemEnergy(int id) {
		switch (id & Items.ITEM_ID_MASK) {
		case Items.PACK_OF_MINTS:
			return 5;
		case Items.COOKED_FOOD:
		case Items.BANANAS:
		case Items.COCONUT:
		case Items.MANGO:
		case Items.BURRITO:
			return 10;
		case Items.BAR_OF_CHOCOLATE:
		case Items.COOKIE:
		case Items.MUFFIN:
			return 15;
		default:
			return 0;
		}
	}

	static boolean hasDurability(int id) {
		switch (id & Items.ITEM_ID_MASK) {
		// TODO
		case Items.STURDY_SHOVEL:
		case Items.SHAVING_CREAM:
		case Items.SCREWDRIVER:
		case Items.PLASTIC_KNIFE:
		case Items.PLASTIC_FORK:
		case Items.STURDY_PICKAXE:
		case Items.TROWEL:
		case Items.TUBE_OF_TOOTHPASTE:
		case Items.PLASTIC_WORK_KEY:
		case Items.STURDY_CUTTERS:
		case Items.FILE:
		case Items.PLASTIC_UTILITY_KEY:
		case Items.PLASTIC_STAFF_KEY:
		case Items.PLASTIC_CELL_KEY:
		case Items.PLASTIC_ENTRANCE_KEY:
		case Items.LIGHTWEIGHT_SHOVEL:
		case Items.FLIMSY_SHOVEL:
		case Items.FLIMSY_PICKAXE:
		case Items.LIGHTWEIGHT_PICKAXE:
		case Items.FLIMSY_CUTTERS:
		case Items.LIGHTWEIGHT_CUTTERS:
			return true;
		default:
			return false;
		}
	}

// endregion Items

// region Textures

	static Image[] sprites = new Image[18];

	static Image tilesTexture;
	static Image itemsTexture;
	static Image objectsTexture;
	static Image groundTexture;
	static Image shadowsTexture;
	static Image2D light3dTexture;
	static Image hudSymbolsTexture;

	static int[] intBuffer = new int[256];

	static int animationFrame;

	static void loadTextures() {
		try {
			tilesTexture = loadTiles("/tiles.png");
			itemsTexture = loadTiles("/items.png");
			objectsTexture = loadTiles("/objects.png");
			groundTexture = loadTiles("/ground.png");
			if (DRAW_SHADOWS) shadowsTexture = loadTiles("/shadow.png");
			if (DRAW_LIGHTS && USE_M3G) {
				light3dTexture = (Image2D) Loader.load("/light.png")[0];
			}

			loadSpritesheet(Textures.INMATE4, "/inmate4.png");
			if (MORE_INMATES) {
				loadSpritesheet(Textures.INMATE1, "/inmate.png");
				loadSpritesheet(Textures.INMATE2, "/inmate2.png");
				loadSpritesheet(Textures.INMATE3, "/inmate3.png");
			} else {
				sprites[Textures.INMATE1] = sprites[Textures.INMATE4];
				sprites[Textures.INMATE2] = sprites[Textures.INMATE4];
				sprites[Textures.INMATE3] = sprites[Textures.INMATE4];
			}
			sprites[Textures.INMATE5] = sprites[Textures.INMATE4];
			sprites[Textures.INMATE6] = sprites[Textures.INMATE4];
			sprites[Textures.INMATE7] = sprites[Textures.INMATE4];
			sprites[Textures.INMATE8] = sprites[Textures.INMATE4];
			sprites[Textures.INMATE9] = sprites[Textures.INMATE4];
			sprites[Textures.INMATE10] = sprites[Textures.INMATE4];
			sprites[Textures.INMATE11] = sprites[Textures.INMATE4];
			loadSpritesheet(Textures.GUARD, "/guard.png");
			loadSpritesheet(Textures.SNIPER, "/sniper.png");
			loadSpritesheet(Textures.EMPLOYMENT_OFFICER, "/jobstaff.png");
			loadSpritesheet(Textures.DOCTOR, "/doctor.png");
			loadSpritesheet(Textures.WARDEN, "/warden.png");
			loadSpritesheet(Textures.OUTFIT_INMATE, "/outfit0.png");
			loadSpritesheet(Textures.OUTFIT_GUARD, "/outfit1.png");
			hudSymbolsTexture = loadTiles("/huds.png");
		} catch (Exception e) {
			if (LOGGING) {
				Profiler.log("loadTextures failed");
				Profiler.log(e.toString());
			}
			e.printStackTrace();
		}
	}

	private static Image loadTiles(String res) {
		try {
			return Image.createImage(res);
		} catch (Exception e) {
			if (LOGGING) {
				Profiler.log("load texture failed");
				Profiler.log(res);
				Profiler.log(e.toString());
			}
			e.printStackTrace();
		}
		return null;
	}

	private static void loadSpritesheet(int id, String res) {
		try {
			sprites[id] = Image.createImage(res);
		} catch (Exception e) {
			if (LOGGING) {
				Profiler.log("load sprite failed");
				Profiler.log(res);
				Profiler.log(e.toString());
			}
			e.printStackTrace();
		}
	}

// endregion Textures

// region Font

	static int[] FONT_COLORS = new int[] {
			0xFFFFFFFF,
			0xFF000000,
			0xFFFF0000,
			0xFFC0C0C0,
			0xFFB4B4B4,
			0xFF7F7F7F,
			0xFF696969,
			0xFF232323,
			0xFFFF8000,
			0xFF7BA7FF,
			0xFF9BC4F3,
			0xFFFFFF00,
			0xFF003D80,
			0xFF00FF00,
	};

	static int[] fontCharWidth;
	static int[] fontCharHeight;
	static int fontColor = FONT_COLOR_WHITE;
	private static int[][] fontWidths;
	private static byte[][][] fontData;

	private static int[][] fontCacheChars;
	private static Image[][] fontCacheImages;
	private static int[] fontCacheIdx;

	static char[] charBuffer = new char[100];
	static StringBuffer stringBuffer = new StringBuffer();

	static void loadFonts() {
		fontCharWidth = new int[FONTS_COUNT];
		fontCharHeight = new int[FONTS_COUNT];
		fontWidths = new int[FONTS_COUNT][];
		fontData = new byte[FONTS_COUNT][][];
		fontCacheChars = new int[FONT_CACHE_SIZE][];
		fontCacheImages = new Image[FONT_CACHE_SIZE][];
		fontCacheIdx = new int[FONT_CACHE_SIZE];

		if (!loadFont(FONT_REGULAR, FONT_REGULAR_RES)) {
			throw new Error();
		}
		if (!loadFont(FONT_BOLD, FONT_BOLD_RES)) {
			loadFont(FONT_BOLD, FONT_REGULAR_RES);
		}
	}

	static boolean loadFont(int idx, String res) {
		try {
			DataInputStream d = new DataInputStream("".getClass().getResourceAsStream(res));
			try {
				// check magic header
				if (d.readInt() != 0xDE11CFAB) {
					return false;
				}
				// skip font name
				d.skip(9);
				int charWidth = fontCharWidth[idx] = d.read();
				int charHeight = fontCharHeight[idx] = d.read();
				int cols = d.read();
				int rows = d.read();

				// reading char map is unnecessary since it's offset of ascii
//				Object j;
//				d.read((byte[])(j = new byte[d.readInt()]));
//				j = new String((byte[])j).toCharArray();

//				char[][] chars = new char[cols][rows];
//				int i = 0;
//				int x = 0;
//				int y = 0;
//				while (i < ((char[])j).length) {
//					char c = ((char[])j)[i];
//					if (c == '\n') {
//						y++;
//						x = 0;
//					} else if (c != '\r' && c != 0) {
//						chars[x++][y] = c;
//					}
//					i++;
//				}

				// so it's skipped
				d.skip(d.readInt());

				int i;

				// read pixels
				int charsNum = cols * rows, charSize = charWidth * charHeight;
				byte[][] bitmap = fontData[idx] = new byte[charsNum][charSize];
				for (int c = 0; c < charsNum; ++c) {
					for (i = 0; i < charSize; ++i) {
						bitmap[c][i] = d.readByte();
					}
				}

				// read widths
				int[] widths = fontWidths[idx] = new int[charsNum];
				for (i = 0; i < charsNum; ++i) {
					widths[i] = d.readByte();
				}
			} finally {
				d.close();
			}

			fontCacheChars[idx] = new int[FONT_CACHE_SIZE];
			fontCacheImages[idx] = new Image[FONT_CACHE_SIZE];
			return true;
		} catch (Exception e) {
			if (LOGGING) {
				Profiler.log("loadFont failed");
				Profiler.log(res);
				Profiler.log(e.toString());
			}
			e.printStackTrace();
		}
		return false;
	}

	static int textWidth(String text, int font) {
		char[] chars = text.toCharArray();
		int i = 0, x = 0;
		while (i < chars.length) {
			int c = chars[i++];
			if (c == ' ') {
				x += fontCharWidth[font] / 2;
				continue;
			}
			if (c < ' ' || c > '~') continue;
			c -= '!';
			x += fontWidths[font][c] + 1;
		}
		return x;
	}

	static int drawText(Graphics g, String text, int x, int y, int font) {
		return drawText(g, text.toCharArray(), x, y, font);
	}

	static int drawText(Graphics g, char[] chars, int x, int y, int font) {
		int i = 0;

		int charWidth = fontCharWidth[font];
		int charHeight = fontCharHeight[font];

		int fontColor = Game.fontColor;
		int color = FONT_COLORS[fontColor];

		int[] rgb = Game.intBuffer;

		int[] fontWidths = Game.fontWidths[font];
		byte[][] fontData = Game.fontData[font];

		int[] cacheChars = fontCacheChars[font];
		Image[] fontCacheImages = Game.fontCacheImages[font];
		int[] fontCacheIdx = Game.fontCacheIdx;

		while (i < chars.length && chars[i] != 0) {
			// x = drawChar(g, chars[idx++], x, y, font);
			char c = chars[i++];
			if (c == ' ') {
				// space
				x += charWidth / 2;
				continue;
			}
			if (c < ' ' || c > '~') return x;
			c -= '!';

			int w = fontWidths[c];

			Image img;
			img: {
				int id = (c & 0xFFFF) | (fontColor << 16);

				// try to get from cache
				for (int j = 0; j < FONT_CACHE_SIZE; ++j) {
					if (cacheChars[j] == id) {
						img = fontCacheImages[j];
						break img;
					}
				}
				// not in cache, create image

				// save some pixels by storing only effective width of chars
				byte[] charsData = fontData[c];
				for (int cy = 0; cy < charHeight; ++cy) {
					for (int cx = 0; cx < w; ++cx) {
						rgb[cx + cy * w] = charsData[cx + cy * charWidth] != 0 ? color : 0;
					}
				}

//				int n = charWidth * charHeight;
//				for (int i = 0; i < n; ++i) {
//					rgb[i] = charsData[c][i] != 0 ? color : 0;
//				}
				img = Image.createRGBImage(rgb, /*charWidth*/ w, charHeight, true);

				// put to cache
				int idx = fontCacheIdx[font];
				cacheChars[idx] = id;
				fontCacheImages[idx] = img;
				fontCacheIdx[font] = (idx + 1) % FONT_CACHE_SIZE;
			}

			g.drawImage(img, x, y, 0);
			x += w + 1;
		}
		// return new x position
		return x;
	}

	static int intToCharBuffer(int n, int i) {
		int start = i;
		char[] chars = charBuffer;
		if (n == 0) {
			chars[0] = '0';
			chars[1] = 0;
			return 1;
		}
		while (n != 0) {
			chars[i++] = (char) ((n % 10) + '0');
			n /= 10;
		}
		int k = i - 1;
		for (; start < k; ++start, --k) {
			char t = chars[start];
			chars[start] = chars[k];
			chars[k] = t;
		}
		chars[i] = 0;
		return i;
	}

	static void putDigitsToCharBuffer(int n, int i) {
		charBuffer[i] = (char) ('0' + (n >= 10 ? n / 10 : 0));
		charBuffer[i + 1] = (char) ('0' + (n % 10));
	}

	static String[] getStringArray(String text, int maxWidth, int font) {
		if (text == null || text.length() == 0 || text.equals(" ")) {
			return new String[0];
		}
		Vector v = new Vector(3);
		char[] chars = text.toCharArray();
		if (text.indexOf('\n') > -1) {
			int j = 0;
			for (int i = 0; i < text.length(); i++) {
				if (chars[i] == '\n') {
					v.addElement(text.substring(j, i));
					j = i + 1;
				}
			}
			v.addElement(text.substring(j, text.length()));
		} else {
			v.addElement(text);
		}
		for (int i = 0; i < v.size(); i++) {
			String s = (String) v.elementAt(i);
			if (textWidth(s, font) >= maxWidth) {
				int i1 = 0;
				for (int i2 = 0; i2 < s.length(); i2++) {
					if (textWidth(s.substring(i1, i2+1), font) >= maxWidth) {
						boolean space = false;
						for (int j = i2; j > i1; j--) {
							char c = s.charAt(j);
							if (c == ' ' || (c >= ',' && c <= '/')) {
								space = true;
								v.setElementAt(s.substring(i1, j + 1), i);
								v.insertElementAt(s.substring(j + 1), i + 1);
								i += 1;
								i2 = i1 = j + 1;
								break;
							}
						}
						if (!space) {
							i2 = i2 - 2;
							v.setElementAt(s.substring(i1, i2), i);
							v.insertElementAt(s.substring(i2), i + 1);
							i2 = i1 = i2 + 1;
							i += 1;
						}
					}
				}
			}
		}
		String[] arr = new String[v.size()];
		v.copyInto(arr);
		return arr;
	}

// endregion

// region Pathfinding

	static final int[] PATH_DIR_POSITIONS = new int[] {
			1, 0, // right
			0, -1, // up
			-1, 0, // left
			0, 1, // down
			1, -1, // right-up
			1, 1, // right-down
			-1, -1, // left-up
			-1, 1 // left-down
	};

	short[] nodeParent;
	short[] nodeG, nodeH;

	// linked list of open nodes
	short[] openNodePrev;
	short[] openNodeNext;
	int sortedNode;

	void allocatePathfind() {
		int size = width * height;

		nodeParent = new short[size];
		nodeG = new short[size];
		nodeH = new short[size];

		openNodePrev = new short[size];
		openNodeNext = new short[size];
	}

	boolean pathfind(int startX, int startY, int startDir, int targetX, int targetY, boolean noStaffDoors, short[] res) {
		int width = this.width;
		int height = this.height;

		short[] nodeParent = this.nodeParent;
		short[] nodeG = this.nodeG;
		short[] nodeH = this.nodeH;
		short[] openNodePrev = this.openNodePrev;
		short[] openNodeNext = this.openNodeNext;
		byte[] solid = this.solid[LAYER_GROUND]; // npc can only move on ground layer

		int size = width * height;

		// initialize nodes
		for (int i = 0; i < size; ++i) {
			nodeParent[i] = -1;
			nodeG[i] = 0;
			nodeH[i] = 0;

			openNodePrev[i] = -1;
			openNodeNext[i] = -1;
		}

		sortedNode = -1;

		int cur = startX + startY * width;

		int nx, ny;
		int g, h, dx, dy;
		int parent;
		int dir = startDir;

		while (cur != -1) {
			openRemove(cur);

			short gPrev = nodeG[cur];

			// close current node
			nodeG[cur] = -1;
			nodeH[cur] = -1;

			nx = cur % width;
			ny = cur / width;

			// finish
			if (nx == targetX && ny == targetY)
				break;

			for (int i = 0; i < 8; ++i) { // for each direction
				// new position
				int x = nx + PATH_DIR_POSITIONS[i << 1];
				int y = ny + PATH_DIR_POSITIONS[(i << 1) + 1];

				// oob check
				if (x < 0 || x >= width || y < 0 || y >= height)
					continue;

				if (i >= 4) { // check collision for both ways in diagonal dir
					if (i == 4) { // right up
						if ((solid[nx + (ny - 1) * width] & COLL_BIT_SOLID_AI) != 0)
							continue;
						if ((solid[nx + 1 + ny * width] & COLL_BIT_SOLID_AI) != 0)
							continue;
					} else if (i == 5) { // right down
						if ((solid[nx + (ny + 1) * width] & COLL_BIT_SOLID_AI) != 0)
							continue;
						if ((solid[nx + 1 + ny * width] & COLL_BIT_SOLID_AI) != 0)
							continue;
					} else if (i == 6) { // left up
						if ((solid[nx + (ny - 1) * width] & COLL_BIT_SOLID_AI) != 0)
							continue;
						if ((solid[nx - 1 + ny * width] & COLL_BIT_SOLID_AI) != 0)
							continue;
					} else { // left down
						if ((solid[nx + (ny + 1) * width] & COLL_BIT_SOLID_AI) != 0)
							continue;
						if ((solid[nx - 1 + ny * width] & COLL_BIT_SOLID_AI) != 0)
							continue;
					}
				}

				int n = x + y * width;
				byte s;

				// check if node is not closed and not solid
				if (nodeG[n] == -1 || ((s = solid[n]) & COLL_BIT_SOLID_AI) != 0)
					continue;
				if (noStaffDoors && s == COLL_DOOR_STAFF)
					continue;

				// add g cost
				g = gPrev + 10;
				// try to avoid objects
				if ((s & COLL_BIT_AVOID_AI) != 0) g += 15;
				// avoid diagonal move when unnecessary
				if (i >= 4) g += 5;
				// prefer keeping current facing
				if (i != dir) g += 10;
				if (i != startDir) g += 5;

				// h cost is euclidean distance
				dx = Math.abs(x - targetX);
				dy = Math.abs(y - targetY);
				h = dx*dx + dy*dy;

				// check if node is not already open
				if (openNodePrev[n] == -1 && openNodeNext[n] == -1 && sortedNode != n) {
					nodeParent[n] = (short) cur;
					nodeG[n] = (short) g;
					nodeH[n] = (short) h;
					openAdd(n);
				} else if (nodeG[n] > g) { // or shorter
					nodeParent[n] = (short) cur;
					nodeG[n] = (short) g;

					// re-sort
					openRemove(n);
					openAdd(n);
				}
			}

			cur = sortedNode;

			// update direction
			if (cur != -1) {
				parent = nodeParent[cur];

				nx = cur % width;
				ny = cur / width;

				int px = parent % width, py = parent / width;

				for (int i = 0; i < 8; ++i) {
					if (nx - px == PATH_DIR_POSITIONS[i << 1] && ny - py == PATH_DIR_POSITIONS[(i << 1) + 1]) {
						dir = i;
						break;
					}
				}
			}
		}

		if (cur == -1) {
			// no path
			if (LOGGING) Profiler.log("NO PATH!");
			return false;
		}

		if (res == null)
			return true; // just check if path exists

		// traceback
		int i = 1;
		while (cur != -1) {
			// oob check
			if (i >= res.length - 1) return false;

			res[i++] = (short) (cur % width);
			res[i++] = (short) (cur / width);
			cur = nodeParent[cur];
		}
		// set result length
		res[0] = (short) i;

		return true;
	}

	// sorted linked list functions

	void openAdd(int n) {
		int i = sortedNode;
		if (i == -1) {
			// first node
			sortedNode = n;
			return;
		}

		int f1 = nodeG[n] + nodeH[n];
		while (i != -1) {
			int f2 = nodeG[i] + nodeH[i];
			if (f1 < f2) {
				// insert to next
				if (openNodePrev[i] == -1) {
					// last
					sortedNode = n;
				} else {
					openNodeNext[openNodePrev[i]] = (short) n;
				}

				openNodePrev[n] = openNodePrev[i];
				openNodeNext[n] = (short) i;
				openNodePrev[i] = (short) n;
				return;
			} else if (openNodeNext[i] == -1) {
				// insert to previous
				openNodeNext[i] = (short) n;
				openNodePrev[n] = (short) i;
				return;
			}
			// continue
			i = openNodeNext[i];
		}
	}

	void openRemove(int n) {
		if (openNodeNext[n] != -1) {
			openNodePrev[openNodeNext[n]] = openNodePrev[n];
		}

		if (sortedNode == n) {
			sortedNode = openNodeNext[n];
		} else if (openNodePrev[n] != -1) {
			openNodeNext[openNodePrev[n]] = openNodeNext[n];
		}

		openNodePrev[n] = -1;
		openNodeNext[n] = -1;
	}

// endregion Pathfinding
}
