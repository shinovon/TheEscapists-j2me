/*
Copyright (c) 2024-2025 Arman Jussupgaliyev
*/
import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

public class TE extends MIDlet {

	static TE midlet;
	Game canvas;
	Display display;
	private static boolean started;
	static boolean paused;

	public TE() {
	}

	protected void destroyApp(boolean b) {

	}

	protected void pauseApp() {
		paused = true;
	}

	protected void startApp() {
		paused = false;
		if (started) return;
		started = true;
		midlet = this;
		
		canvas = new Game();
		(display = Display.getDisplay(this)).setCurrent(canvas);
		
		new Thread(canvas).start();
	}

}
