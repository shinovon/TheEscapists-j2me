import javax.microedition.io.CommConnection;
import javax.microedition.io.Connector;
import java.io.OutputStreamWriter;
import java.util.Date;

/*
Copyright (c) 2025 Arman Jussupgaliyev
*/
public class Profiler implements Constants {

	// frame sections
	static final int FRAME_TICK = 0;
	static final int FRAME_RENDER = 1;
	static final int FRAME_HUD = 2;
	static final int FRAME_BLIT = 3;
	static final int FRAME_FLUSH = 4;
	
	static final int[] frameColors = new int[] {
			0xFF0000, // red
			0xFFFF00, // yellow
			0x00FF00, // green
			0x00FFFF, // cyan
			0x0000FF, // blue
	};
	
	// render sections
	static final int RENDER_BG = 0;
	static final int RENDER_TILES = 1;
	static final int RENDER_OBJECTS = 2;
	static final int RENDER_CHARACTERS = 3;
	static final int RENDER_TOP_OBJECTS = 4;
	static final int RENDER_3D = 5;
	
	static final int[] renderColors = new int[] {
			0xFF0000, // red
			0xFFFF00, // yellow
			0x00FF00, // green
			0x0000FF, // blue
			0x00FFFF, // cyan
			0xFF00FF, // purple
	};
	
	private static long frameStart, frameEnd, lastFrameSection;
	private static long[] frameSections = new long[5];
	private static int frameSection;
	
	static long frameStartRes, frameEndRes;
	static long[] frameSectionsRes = new long[5];
	
	private static long renderStart, renderEnd, lastRenderSection;
	private static int renderSection;
	private static long[] renderSections = new long[6];
	
	static long renderStartRes, renderEndRes;
	static long[] renderSectionsRes = new long[6];
	
	static void beginFrame() {
		if (!PROFILER) return;
		frameStart = lastFrameSection = System.currentTimeMillis();
		frameSection = -1;
	}
	
	static void beginFrameSection(int i) {
		if (!PROFILER) return;
		long now = System.currentTimeMillis();
		if (frameSection != -1) {
			endFrameSection(now, frameSection);
		}
		if (i == FRAME_RENDER) {
			renderStart = now;
			renderSection = -1;
		}
		lastFrameSection = now;
		frameSection = i;
	}
	
	private static void endFrameSection(long now, int i) {
		if (!PROFILER) return;
		frameSections[i] = now - lastFrameSection;
		if (i == FRAME_RENDER) {
			if (renderSection != -1) {
				renderSections[renderSection] = now - lastRenderSection;
			}

			renderSection = -1;
			renderEnd = now;
		}
	}

	static void endFrame() {
		if (!PROFILER) return;
		long now = System.currentTimeMillis();
		endFrameSection(now, frameSection);
		frameEnd = lastFrameSection = now;
		frameSection = -1;

		// copy values to display vars

		frameStartRes = frameStart;
		frameEndRes = frameEnd;
		System.arraycopy(frameSections, 0, frameSectionsRes, 0, frameSections.length);
		
		renderStartRes = renderStart;
		renderEndRes = renderEnd;
		System.arraycopy(renderSections, 0, renderSectionsRes, 0, renderSections.length);
	}
	
	static void beginRenderSection(int i) {
		if (!PROFILER) return;
		long now = System.currentTimeMillis();
		if (renderSection != -1) {
			renderSections[renderSection] = now - lastRenderSection;
		}
		lastRenderSection = now;
		renderSection = i;
	}

	static OutputStreamWriter console;

	static void initLogs() {
		if (!LOGGING || !SERIAL_LOGS) return;

		try {
			CommConnection c = (CommConnection) Connector.open("comm:USB1;baudrate=9600");
			console = new OutputStreamWriter(c.openOutputStream());
			log("TE log started at " + new Date());
		} catch (Exception ignored) {}
	}

	static void log(String s) {
		if (!LOGGING) return;
		System.out.println(s);

		if (!SERIAL_LOGS || console == null) return;
		try {
			console.write(s.concat("\r\n"));
		} catch (Exception ignored) {}
	}

}
