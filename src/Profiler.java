/*
Copyright (c) 2025 Arman Jussupgaliyev

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
import javax.microedition.io.CommConnection;
import javax.microedition.io.Connector;
import java.io.OutputStreamWriter;
import java.util.Date;

public class Profiler implements Constants {

	// frame sections
	static final int FRAME_TICK = 0;
	static final int FRAME_RENDER = 1;
	static final int FRAME_HUD = 2;
	static final int FRAME_BLIT = 3;
	static final int FRAME_FLUSH = 4;
	
	static final int[] frameColors = new int[] {
			0xFF0000, // red - tick
			0xFFFF00, // yellow - render
			0x00FF00, // green - hud
			0x00FFFF, // cyan - blit
			0x0000FF, // blue - flush
	};
	
	// render sections
	static final int RENDER_BG = 0;
	static final int RENDER_TILES = 1;
	static final int RENDER_OBJECTS = 2;
	static final int RENDER_CHARACTERS = 3;
	static final int RENDER_TOP_OBJECTS = 4;
	static final int RENDER_3D = 5;
	
	static final int[] renderColors = new int[] {
			0xFF0000, // red - bg
			0xFFFF00, // yellow - tiles
			0x00FF00, // green - objects
			0x0000FF, // blue - characters
			0x00FFFF, // cyan - top objects
			0xFF00FF, // purple - 3d
	};
	
	private static long frameStart, lastFrameSection;
	private static final long[] frameSections = new long[5];
	private static int frameSection;
	
	static long frameStartRes, frameEndRes;
	static long[] frameSectionsRes = new long[5];
	
	private static long renderStart, renderEnd, lastRenderSection;
	private static int renderSection;
	private static final long[] renderSections = new long[6];
	
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
		frameEndRes = lastFrameSection = now;
		frameSection = -1;

		// copy values to display vars

		frameStartRes = frameStart;
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
	static String[] logs;

	static void initLogs() {
		if (!LOGGING) return;

		if (SERIAL_LOGS) {
			try {
				CommConnection c = (CommConnection) Connector.open("comm:USB1;baudrate=9600");
				console = new OutputStreamWriter(c.openOutputStream());
				log("TE log started at " + new Date());
			} catch (Exception ignored) {}
		}
		
		if (SCREEN_LOGS) {
			logs = new String[20];
		}
	}

	static void log(String s) {
		if (!LOGGING) return;
		System.out.println(s);
		
		if (SCREEN_LOGS) {
			System.arraycopy(logs, 0, logs, 1, logs.length - 1);
			logs[0] = s;
		}

		if (!SERIAL_LOGS || console == null) return;
		try {
			console.write(s.concat("\r\n"));
		} catch (Exception ignored) {}
	}

}
