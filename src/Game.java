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
import java.io.*;

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
	boolean searching;
	int keyStates;
	int selectedInventory = -1;
	int trainingTimer = 0;
	int trainingLastKey;
	int trainingRepeats;
	boolean trainingBlocked;
	boolean playerSeenByGuards;
	int progress;

	boolean debugFreecam;

	// settings
	int selectedSetting;
	boolean use3D = USE_M3G;
	boolean enableShadows = DRAW_SHADOWS;

	Game() {
		super(false);
		if (PROFILER && SERIAL_LOGS) Profiler.initLogs();
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
		} else if (state == 7) {
			// escaped
			fontColor = FONT_COLOR_ORANGE;
			String s = "ESCAPED";
			drawText(g, s, (w - textWidth(s, FONT_BOLD)) >> 1, 40, FONT_BOLD);
			fontColor = FONT_COLOR_GREY_B4;

			s = "Press any key to exit";
			drawText(g, s, (w - textWidth(s, FONT_BOLD)) >> 1, h - 40, FONT_BOLD);
		} else if (mapLoaded) {
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
			fontColor =  FONT_COLOR_WHITE;
//			drawText(g, "$ " + money, 0, 0, FONT_REGULAR);
//			drawText(g, "HP " + player.health, 0, 11, FONT_REGULAR);
//			drawText(g, "HEAT " + heat, 0, 22, FONT_REGULAR);
//			drawText(g, "FATIGUE " + fatigue, 0, 33, FONT_REGULAR);
			char[] chars;
			int x;
			NPC player = this.player;

			// money
			g.drawRegion(hudSymbolsTexture, 90, 0, 9, 11, 0, 4, 2, 0);
			chars = Integer.toString(money).toCharArray();
			x = 13;
			for (int i = 0; i < chars.length; i++) {
				g.drawRegion(hudSymbolsTexture, (chars[i] - '0') * 9, 0, 9, 11, 0, x, 2, 0);
				x += 9;
			}

			// health
			g.drawRegion(hudSymbolsTexture, 90, 11, 9, 11, 0, 3, 14, 0);
			chars = Integer.toString(player.health).toCharArray();
			x = 13;
			for (int i = 0; i < chars.length; i++) {
				g.drawRegion(hudSymbolsTexture, (chars[i] - '0') * 9, 11, 9, 11, 0, x, 14, 0);
				x += 9;
			}

			// heat
			g.drawRegion(hudSymbolsTexture, 90, 22, 9, 11, 0, 3, 26, 0);
			chars = Integer.toString(heat).toCharArray();
			x = 13;
			for (int i = 0; i < chars.length; i++) {
				g.drawRegion(hudSymbolsTexture, (chars[i] - '0') * 9, 22, 9, 11, 0, x, 26, 0);
				x += 9;
			}
			g.drawRegion(hudSymbolsTexture, 100, 22, 9, 11, 0, x, 26, 0);

			// fatigue
			g.drawRegion(hudSymbolsTexture, 90, 33, 9, 11, 0, 3, 38, 0);
			chars = Integer.toString(fatigue).toCharArray();
			x = 13;
			for (int i = 0; i < chars.length; i++) {
				g.drawRegion(hudSymbolsTexture, (chars[i] - '0') * 9, 33, 9, 11, 0, x, 38, 0);
				x += 9;
			}
			g.drawRegion(hudSymbolsTexture, 100, 33, 9, 11, 0, x, 38, 0);

			// general debug
			drawText(g, fps + " fps " + tps + " tps", 0, 55, FONT_REGULAR);
//			Runtime r = Runtime.getRuntime();
//			drawText(g, ((r.totalMemory() - r.freeMemory()) / 1024) + "k used", 0, 66, FONT_REGULAR);

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
			char[] s = (n(time / 60) + ':' + n(time % 60) + " - " + scheduleStrings[schedule] + " (Day " + (day + 1) + ')').toCharArray();
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
				} else if (player.action != NPC.ACT_NONE) {
					switch (player.action) {
					case NPC.ACT_READING:
						a = "Reading";
						break;
					case NPC.ACT_CLEANING:
						a = "Cleaning";
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

//			g.setColor(0x0F0F0F);
//			g.fillRect(w - 22, 0, 22, h);

			// inventory items
			x = w - 21;
			for (int i = 0; i < 6; ++i) {
				int y = i * 22 + 1;
				int item = player.inventory[i] & Items.ITEM_ID_MASK;
//				g.setColor(0x434343);
//				g.fillRect(x + 1, y + 1, 18, 18);
//				g.setColor(selectedInventory == i ? 0x97479B : 0x1D1D1D);
//				g.drawRect(x, y, 19, 19);
				if (selectedInventory == i) {
					g.setColor(0x97479B);
					g.drawRect(x, y, 19, 19);
				}
				if (player.inventory[i] == Items.ITEM_NULL)
					continue;
				g.drawRegion(itemsTexture, (item % TILE_SIZE) * TILE_SIZE, (item / TILE_SIZE) * TILE_SIZE, TILE_SIZE, TILE_SIZE, 0, x + 3, y + 3, 0);

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

	static String n(int n) {
		String s = Integer.toString(n);
		if (n < 10) return "0".concat(s);
		return s;
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
				if (key == -6) {
					softPressed = true;
				} else if (key == -7) {
					paused = true;
					state = 4;
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
			} else if (state == 2) {
				if (gameAction == FIRE){
					state = 6;
					mapError = false;
				} else if (key == -6 || key == -21) {
					state = 5;
				} else if (key == -7 || key == -22) {
					TE.midlet.notifyDestroyed();
				}
			} else if (state == 5) {
				if (gameAction == UP) {
					if (selectedSetting-- == 0)
						selectedSetting = 0;
				} else if (gameAction == DOWN) {
					if (selectedSetting++ == 3)
						selectedSetting = 0;
				} else if (gameAction == FIRE || gameAction == LEFT || gameAction == RIGHT) {
					set: {
						int i = selectedSetting;
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
					}
				} else if (key == -6 || key == -7) {
					state = mapLoaded ? 4 : 2;
				}
			} else if (state == 4) {
				if (gameAction == FIRE) {
					paused = false;
					state = 3;
				} else if (key == -6 || key == -21) {
					state = 5;
				}
			} else if (state == 7) {
				TE.midlet.notifyDestroyed();
			}
			// debug time skip
			if (key == '9' && mapLoaded) {
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
				player.weapon = Items.NUNCHUCKS | Items.ITEM_DEFAULT_DURABILITY;

				player.inventory[0] = Items.MULTITOOL | Items.ITEM_DEFAULT_DURABILITY;
				player.inventory[1] = Items.UTILITY_KEY | Items.ITEM_DEFAULT_DURABILITY;
				player.inventory[2] = Items.WORK_KEY | Items.ITEM_DEFAULT_DURABILITY;
				player.inventory[3] = Items.STAFF_KEY | Items.ITEM_DEFAULT_DURABILITY;
				player.inventory[4] = Items.ENTRANCE_KEY | Items.ITEM_DEFAULT_DURABILITY;
				player.inventory[5] = Items.CELL_KEY | Items.ITEM_DEFAULT_DURABILITY;
			}
			if (key == '7' && mapLoaded) {
				debugFreecam = !debugFreecam;
			}
		} catch (Exception ignored) {}
	}

	public void keyRepeated(int key) {
		super.keyRepeated(key);
	}

	public void keyReleased(int key) {
		super.keyReleased(key);
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
						if (state == 3) paused = false;
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
						if ((globalCounter % ANIMATION_TICKS) == 0 && ++animationFrame > 1)
							animationFrame = 0;
						if (!paused) {
							ticksC++;
							tickMap();
						}
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
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	int guardsDown;
	int guards;
	int inmates;
	int npcNum;

	int[] jobs; // 0 is count
	int npcSpawnX, npcSpawnY;

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

				// TODO randomize stash spawn

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

					if (objects[idx + 1] == Objects.CHAIR && isInZone(x * TILE_SIZE, y * TILE_SIZE, ZONE_CANTEEN)) {
						int p = ((canteenSeatsPositions[0]++) << 1) + 1;
						canteenSeatsPositions[p] = (short) ((x & 0xFF) | ((y & 0xFF) << 8));
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
				npc.statStrength = 30 + NPC.rng.nextInt(30);
				npc.statSpeed = 30 + NPC.rng.nextInt(30);
				npc.statRespect = 30 + NPC.rng.nextInt(30);
				npc.statIntellect = 30 + NPC.rng.nextInt(30);
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

	NPC getClosestNPC(NPC self) {
		NPC res = null;
		int dist = 0;
		int n = npcNum;
		for (int i = 0; i < n; ++i) {
			NPC npc = chars[i];
			if (self == npc || npc == null) continue;

			int dx = npc.x - self.x;
			int dy = npc.y - self.y;
			int d = dx * dx + dy * dy;
			if (res == null || d < dist) {
				dist = d;
				res = npc;
			}
		}

		return res;
	}

	void tickMap() {
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
							// fire player TODO
							jobs[player.job] &= ~JOB_OCCUPIED_BIT;
							player.job = 0;
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
						entranceOpen = true;
					} else if (guardsDown <= 1 && playerSeenByGuards) {
						lockdown = false;
						entranceOpen = false;
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
				// TODO to solitary
//				return;
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
			if (player.interactNPC == npc && npc.name != null) {
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
		if (droppedItems[layer][pos] != Items.ITEM_NULL)
			return -1;
		droppedItems[layer][pos] = item & Items.ITEM_MASK;
		return 0;
	}

	int pickItem(int x, int y, int layer) {
		int item = droppedItems[layer][x + y * width];
		if (item == 0) {
			return -1;
		}
		droppedItems[layer][x + y * width] = Items.ITEM_NULL;
		return item & Items.ITEM_MASK;
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
	short[] topObjects; // {count, [objectIdx, speed], ...}
	short[] lights; // {count, [x, y], ...}
	short[] dirt; // {[x, y, idx], ...}

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
				if (time % 60 == 0) {
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
			return "Cell key";
		case Items.STAFF_KEY:
			return "Staff key";
		case Items.PACK_OF_MINTS:
			return "Pack of Mints";
		case Items.GUARD_OUTFIT:
			return "Guard Outfit";
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
		case Items.TIMBED_BRACE:
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
		case Items.NUNCHUCKS:
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
		case Items.DURABLE_CONTRABAND_POUCNH:
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
		case Items.NUNCHUCKS:
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
			tilesTexture = loadTiles("/tiles_ea.png");
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
			0xFF0000FF,
			0xFFC0C0C0,
			0xFFB4B4B4,
			0xFF7F7F7F,
			0xFF696969,
			0xFF232323,
			0xFFFF8000,
			0xFF7BA7FF,
			0xFF9BC4F3,
			0xFFFFFF00,
	};

	static int[] fontCharWidth;
	static int[] fontCharHeight;
	static int fontColor = FONT_COLOR_WHITE;
	private static int[][] fontWidths;
	private static byte[][][] fontData;

	private static int[][] fontCacheChars;
	private static Image[][] fontCacheImages;
	private static int[] fontCacheIdx;

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

	static int drawChar(Graphics g, char c, int x, int y, int font) {
		int charWidth = fontCharWidth[font];
		if (c == ' ') {
			// space
			return x + charWidth / 2;
		}
		if (c < ' ' || c > '~') return x;
		c -= '!';

		int w = fontWidths[font][c];

		Image img;
		img: {
			int id = (c & 0xFFFF) | (fontColor << 16);
			int[] cacheChars = fontCacheChars[font];

			// try to get from cache
			for (int i = 0; i < FONT_CACHE_SIZE; ++i) {
				if (cacheChars[i] == id) {
					img = fontCacheImages[font][i];
					break img;
				}
			}

			// create image
			int color = FONT_COLORS[fontColor];
			int[] rgb = Game.intBuffer;

			// save some pixels by storing only effective width of chars
			int h = fontCharHeight[font];
			byte[] charsData = fontData[font][c];
			for (int cy = 0; cy < h; ++cy) {
				for (int cx = 0; cx < w; ++cx) {
					rgb[cx + cy * w] = charsData[cx + cy * charWidth] != 0 ? color : 0;
				}
			}

//			int n = charWidth * charHeight;
//			for (int i = 0; i < n; ++i) {
//				rgb[i] = charsData[c][i] != 0 ? color : 0;
//			}
			img = Image.createRGBImage(rgb, /*charWidth*/ w, h, true);

			// put to cache
			int idx = fontCacheIdx[font];
			cacheChars[idx] = id;
			fontCacheImages[font][idx] = img;
			fontCacheIdx[font] = (idx + 1) % FONT_CACHE_SIZE;
		}

		g.drawImage(img, x, y, 0);

		return x + w + 1;
	}

	int textWidth(String text, int font) {
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
		char[] chars = text.toCharArray();
		int i = 0;
		while (i < chars.length) {
			x = drawChar(g, chars[i++], x, y, font);
		}
		// return new x position
		return x;
	}

	static int drawText(Graphics g, char[] chars, int x, int y, int font) {
		int i = 0;
		while (i < chars.length) {
			x = drawChar(g, chars[i++], x, y, font);
		}
		// return new x position
		return x;
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
