/*
Copyright (c) 2024-2026 Arman Jussupgaliyev

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
import java.util.Vector;

import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;

public class Sound implements Runnable, PlayerListener, Constants {
	
	static final Player[] music = new Player[COUNT_MUSIC],
			effects = new Player[COUNT_EFFECTS];
	
	private static final Vector queue = new Vector();
	private static final Object queueLock = new Object();
	
	private static final Object EVENT_STOP_MUSIC = new Object(),
			EVENT_STOP_EFFECT = new Object(),
			EVENT_START_MUSIC = new Object(),
			EVENT_START_EFFECT = new Object();
	
	static Player currentMusicPlayer,
			currentEffectPlayer,
			lastMusicPlayer,
			lastEffectPlayer;
	
	static int volumeMusic = 40;
	static int volumeSfx = 60;
	static int lastMusic = -1;
	static int lastEffect = -1;

	static boolean musicPrefetched;
	static boolean sfxPrefetched;
	
	private static Sound inst;
	
	static void load() {
		inst = new Sound();

		// prefetching sounds causes out of memory exceptions on n96,
		// and then crashes the jvm when continuing to create more players.
		// so the fallback to S40 method was introduced.
		prefetch: {
			if (PREFETCH_MUSIC) {
				try {
					loadMusic(MUSIC_THEME, "/theme.mid");
					loadMusic(MUSIC_LIGHTSOUT, "/lights.mid");
					loadMusic(MUSIC_GENERIC, "/generic.mid");
					loadMusic(MUSIC_CANTEEN, "/canteen.mid");
					loadMusic(MUSIC_ROLLCALL, "/rollcall.mid");
					loadMusic(MUSIC_SHOWER, "/shower.mid");
					loadMusic(MUSIC_WORK, "/work.mid");
					loadMusic(MUSIC_WORKOUT, "/workout.mid");
					loadMusic(MUSIC_LOCKDOWN, "/lockdown.mid");
					loadMusic(MUSIC_ESCAPED, "/escaped.mid");
					musicPrefetched = true;
				} catch (Exception e) {
					unloadMusic();
					break prefetch;
				}
			}
	
			if (!NO_SFX && PREFETCH_SFX) {
				try {
					loadEffect(SFX_ACCOLADE, "/accolade.wav");
					loadEffect(SFX_BELL, "/bell.wav");
					loadEffect(SFX_BUY, "/buy.wav");
					loadEffect(SFX_CLOSE, "/close.wav");
					loadEffect(SFX_DOOR, "/door.wav");
					loadEffect(SFX_ENHIT, "/en_hit.wav");
					loadEffect(SFX_OPEN, "/open.wav");
					loadEffect(SFX_PICKUP, "/pickup.wav");
					loadEffect(SFX_PLIP, "/plip.wav");
					loadEffect(SFX_RUMBLE, "/rumble.wav");
					loadEffect(SFX_LOSE, "/lose.wav");
					loadEffect(SFX_THROW, "/throw.wav");
					loadEffect(SFX_HP, "/hp.wav");
					sfxPrefetched = true;
				} catch (Exception e) {
					unloadEffects();
				}
			}
		}
		
		new Thread(inst, "Music").start();
	}
	
	public void run() {
		try {
			while (true) {
				synchronized (queueLock) {
					queueLock.wait();
				}
				while (!queue.isEmpty()) {
					Object o;
					synchronized (queue) {
						o = queue.elementAt(0);
						queue.removeElementAt(0);
					}
					if (o == EVENT_STOP_MUSIC || o == EVENT_START_MUSIC) {
						if (lastMusicPlayer != null && lastMusicPlayer.getState() >= Player.CLOSED) {
							lastMusicPlayer.stop();
							if (!PREFETCH_MUSIC || !musicPrefetched) {
								lastMusicPlayer.deallocate();
								lastMusicPlayer.close();
							}
							lastMusicPlayer = null;
						}
						if (PREFETCH_MUSIC && musicPrefetched) {
							for (int i = 0; i < COUNT_MUSIC; ++i) {
								Player p = music[i];
								if (p != null && p.getState() >= Player.STARTED) {
									p.stop();
									break;
								}
							}
						}
					} else if (!NO_SFX && (o == EVENT_STOP_EFFECT || o == EVENT_START_EFFECT)) {
						if (lastEffectPlayer != null && lastEffectPlayer.getState() >= Player.CLOSED) {
							lastEffectPlayer.stop();
							if (!PREFETCH_SFX || !sfxPrefetched) {
								lastEffectPlayer.deallocate();
								lastEffectPlayer.close();
							}
							lastEffectPlayer = null;
						}
						if (PREFETCH_SFX && sfxPrefetched) {
							for (int i = 0; i < COUNT_EFFECTS; ++i) {
								Player p = effects[i];
								if (p != null && p.getState() >= Player.STARTED) {
									p.stop();
									break;
								}
							}
						}
					}
					if (o == EVENT_START_MUSIC) {
						Player p = currentMusicPlayer;
						if (!PREFETCH_MUSIC || !musicPrefetched) {
							String res;
							String type = "audio/midi";
							int loopCount = -1;
							switch (lastMusic) {
							case MUSIC_THEME:
								res = "/theme.mid";
								break;
							case MUSIC_LIGHTSOUT:
								res = "/lights.mid";
								break;
							case MUSIC_GENERIC:
								res = "/generic.mid";
								break;
							case MUSIC_CANTEEN:
								res = "/canteen.mid";
								break;
							case MUSIC_ROLLCALL:
								res = "/rollcall.mid";
								break;
							case MUSIC_SHOWER:
								res = "/shower.mid";
								break;
							case MUSIC_WORK:
								res = "/work.mid";
								break;
							case MUSIC_WORKOUT:
								res = "/workout.mid";
								break;
							case MUSIC_LOCKDOWN:
								res = "/lockdown.mid";
								//type = "audio/mpeg";
								break;
							case MUSIC_ESCAPED:
								res = "/escaped.mid";
								loopCount = 0;
								break;
							default:
								continue;
							}
							try {
								p = currentMusicPlayer = Manager.createPlayer("".getClass().getResourceAsStream(res), type);
								p.realize();
								if (loopCount != 0) p.setLoopCount(loopCount);
								p.addPlayerListener(inst);
								setVolume(p, volumeMusic);
							} catch (Throwable e) {
								p = currentMusicPlayer = null;
							}
						}
						if (p != null) {
							try {
								p.setMediaTime(0);
							} catch (Exception ignored) {}
							p.start();
						}
						lastMusicPlayer = p;
						continue;
					}
					if (!NO_SFX && o == EVENT_START_EFFECT) {
						Player p = currentEffectPlayer;

						if (!PREFETCH_SFX || !sfxPrefetched) {
							String res;
							switch (lastEffect) {
							case SFX_ACCOLADE:
								res = "/accolade.wav";
								break;
							case SFX_BELL:
								res = "/bell.wav";
								break;
							case SFX_BUY:
								res = "/buy.wav";
								break;
							case SFX_CLOSE:
								res = "/close.wav";
								break;
							case SFX_DOOR:
								res = "/door.wav";
								break;
							case SFX_ENHIT:
								res = "/en_hit.wav";
								break;
							case SFX_OPEN:
								res = "/open.wav";
								break;
							case SFX_PICKUP:
								res = "/pickup.wav";
								break;
							case SFX_PLIP:
								res = "/plip.wav";
								break;
							case SFX_RUMBLE:
								res = "/rumble.wav";
								break;
							case SFX_LOSE:
								res = "/lose.wav";
								break;
							case SFX_THROW:
								res = "/throw.wav";
								break;
							case SFX_HP:
								res = "/hp.wav";
								break;
							default:
								continue;
							}
							try {
								p = currentEffectPlayer = Manager.createPlayer("".getClass().getResourceAsStream(res), "audio/wav");
								p.realize();
								p.addPlayerListener(inst);
								setVolume(p, volumeSfx);
							} catch (Throwable e) {
								p = currentEffectPlayer = null;
							}
						}
						if (p != null) {
							try {
								p.setMediaTime(0);
							} catch (Exception ignored) {}
							try {
								p.start();
							} catch (Exception e) {
								volumeSfx = 0;
								p = null;
							}
						}
						lastEffectPlayer = p;
						continue;
					}
				}
			}
		} catch (Exception e) {
			if (LOGGING) {
				Profiler.log("Sound thread died");
				Profiler.log(e.toString());
			}
			e.printStackTrace();
		}
	}

	public void playerUpdate(Player player, String event, Object eventData) {
		if (PlayerListener.END_OF_MEDIA.equals(event) && player == currentEffectPlayer) {
			currentEffectPlayer = null;
		}
	}
	
	private static void loadMusic(int id, String res) throws Exception {
		try {
			Player p = music[id] = Manager.createPlayer("".getClass().getResourceAsStream(res), "audio/midi");
			p.realize();
			p.prefetch();
			p.setLoopCount(-1);
			p.addPlayerListener(inst);
			setVolume(p, volumeMusic);
		} catch (Exception e) {
			if (LOGGING) {
				Profiler.log("loadMusic failed");
				Profiler.log(res);
				Profiler.log(e.toString());
			}
			e.printStackTrace();
			throw e;
		}
	}
	
	private static void loadEffect(int id, String res) throws Exception {
		try {
			Player p = effects[id] = Manager.createPlayer("".getClass().getResourceAsStream(res), "audio/wav");
			p.realize();
			p.prefetch();
			p.addPlayerListener(inst);
			setVolume(p, volumeSfx);
		} catch (Exception e) {
			if (LOGGING) {
				Profiler.log("loadEffect failed");
				Profiler.log(res);
				Profiler.log(e.toString());
			}
			e.printStackTrace();
			throw e;
		}
	}

	private static void unloadMusic() {
		for (int i = 0; i < music.length; i++) {
			if (music[i] == null) {
				continue;
			}
			try {
				music[i].close();
			} catch (Exception ignored) {}
			music[i] = null;
		}
	}

	private static void unloadEffects() {
		for (int i = 0; i < effects.length; i++) {
			if (effects[i] == null) {
				continue;
			}
			try {
				effects[i].close();
			} catch (Exception ignored) {}
			effects[i] = null;
		}
	}
	
	private static void setVolume(Player p, int v) {
		if (p == null) return;
		try {
			VolumeControl c = (VolumeControl) p.getControl("VolumeControl");
			if (c == null) return;
			if (v <= 0) {
				c.setMute(true);
				return;
			}
			c.setMute(false);
			c.setLevel(v);
		} catch (Exception ignored) {}
	}
	
	static void setMusicVolume(int v) {
		volumeMusic = v;
		if (PREFETCH_MUSIC && musicPrefetched) {
			for (int i = 0; i < COUNT_MUSIC; ++i) {
				setVolume(music[i], v);
			}
		} else if (currentMusicPlayer != null) {
			setVolume(currentMusicPlayer, v);
		}
		if (v != 0) {
			if (currentMusicPlayer == null && lastMusic != -1) {
				playMusic(lastMusic);
			}
		} else {
			stopMusic();
		}
	}
	
	static void setEffectVolume(int v) {
		volumeSfx = v;
		if (PREFETCH_SFX && sfxPrefetched) {
			for (int i = 0; i < COUNT_MUSIC; ++i) {
				setVolume(effects[i], v);
			}
		}
	}
	
	synchronized static void stopMusic() {
		if (PREFETCH_MUSIC && musicPrefetched) {
			if (currentMusicPlayer == null) return;
			currentMusicPlayer = null;
		}
		queue.addElement(EVENT_STOP_MUSIC);
		synchronized (queueLock) {
			queueLock.notify();
		}
	}
	
	synchronized static void stopEffect() {
		if (NO_SFX || currentEffectPlayer == null) return;
		currentEffectPlayer = null;
		queue.addElement(EVENT_STOP_EFFECT);
		synchronized (queueLock) {
			queueLock.notify();
		}
	}
	
	synchronized static void playMusic(int id) {
		lastMusic = id;
		if (volumeMusic <= 0) {
			return;
		}
		if (PREFETCH_MUSIC && musicPrefetched) {
			Player p = music[id];
			if (p == null) {
				queue.addElement(EVENT_STOP_MUSIC);
				currentMusicPlayer = null;
				return;
			}
			if (currentMusicPlayer == p) return;
			currentMusicPlayer = p;
		}
		queue.addElement(EVENT_START_MUSIC);
		synchronized (queueLock) {
			queueLock.notify();
		}
	}
	
	synchronized static void playEffect(int id) {
		if (NO_SFX || volumeSfx <= 0) return;
		if (!PREFETCH_SFX || !sfxPrefetched) {
			if (lastEffect == id && currentEffectPlayer != null) return;
			lastEffect = id;
		} else {
			Player p = effects[id];
			if (p == null || currentEffectPlayer == p) return;
			currentEffectPlayer = p;
		}
		queue.addElement(EVENT_START_EFFECT);
		synchronized (queueLock) {
			queueLock.notify();
		}
	}
	
	synchronized static void resumeMusic() {
		if (volumeMusic <= 0 || lastMusic == -1) return;
		if (PREFETCH_MUSIC && musicPrefetched) {
			if (currentMusicPlayer != null) return;
			Player p = music[lastMusic];
			if (p == null) return;
			currentMusicPlayer = p;
		}
		queue.addElement(EVENT_START_MUSIC);
		synchronized (queueLock) {
			queueLock.notify();
		}
	}
	
	private Sound() {}

}
