/*
Copyright (c) 2026 Arman Jussupgaliyev

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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.imageio.ImageIO;

public class ResourceBuilder implements Constants {

	static final String[] maps = {
			"perks",
			"stalagflucht",
			"shanktonstatepen",
			"jungle",
			"sanpancho",
			"irongate",
	};
	static final HashMap<String, String> map = new HashMap<>();
	
	static Path decompiledDir;
	static Path[] unsortedImagesDirs;
	static Path gameDir;
	static Path resDir;
	static boolean force;
	
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("The Escapists J2ME resource builder");
			System.out.println("Parameters: <output res dir> <decompiled dir> <game dir>");
			return;
		}

		// TODO better arguments parsing

		resDir = Paths.get(args[0]);
		decompiledDir = Paths.get(args[1]);
		gameDir = Paths.get(args.length >= 3 ? args[2] : "C:\\Program Files (x86)\\Steam\\steamapps\\common\\The Escapists");
		unsortedImagesDirs = new Path[] {
				Paths.get("Sorted Images", "[6] game", "[UNSORTED]"),
				Paths.get("Sorted Images", "[11] tutorial", "[UNSORTED]"),
		};

		if (args.length >= 4 && "-force".equals(args[3])) force = true;
		
		try {
			initMap();
			items(); // + fx + arrow
			objects();
			light();
			huds();
			markers();
			title();
			icon();
			character("Inmate", "inmate.png", 4);
			character("Inmate 2", "inmate2.png", 4);
			character("Inmate 3", "inmate3.png", 4);
			character("Inmate 4", "inmate4.png", 4);
			character("Guard", "guard.png", 2);
			character("Outfit - Inmate", "outfit0.png", 4);
			character("Outfit - Guard", "outfit1.png", 4);
			character("Extra NPCs_0", "warden.png", 1);
			character("Extra NPCs_1", "jobstaff.png", 1);
			character("Extra NPCs_2", "doctor.png", 1);
			character("Tower Guard_0", "sniper.png", 1);
			sounds();

			for (String map: maps) {
				map(map);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static void items() throws IOException {
		final String[] items = {
				"Items_0-0_0.png",
				"Items_0-1_0.png",
				"Items_0-2_0.png",
				"Items_0-3_0.png",
				"Items_0-4_0.png",
				"Items_0-5_0.png",
				"Items_0-6_0.png",
				"Items_0-7_0.png",
				"Items_0-8_0.png",
				"Items_0-9_0.png",
				"Items_0-10_0.png",
				"Items_0-11_0.png",
				"Items_0-12_0.png",
				"Items_0-13_0.png",
				"Items_0-14_0.png",
				"Items_0-15_0.png",
				"Items_0-16_0.png",
				"Items_0-17_0.png",
				"Items_0-18_0.png",
				"Items_0-19_0.png",
				"Items_0-20_0.png",
				"Items_0-21_0.png",
				"Items_0-22_0.png",
				"Items_0-23_0.png",
				"Items_0-24_0.png",
				"Items_0-25_0.png",
				"Items_0-26_0.png",
				"Items_0-27_0.png",
				"Items_0-28_0.png",
				"Items_0-29_0.png",
				"Items_0-30_0.png",
				"Items_0-31_0.png",
				"Items_1-0_0.png",
				"Items_1-1_0.png",
				"Items_1-2_0.png",
				"Items_1-3_0.png",
				"Items_1-4_0.png",
				"Items_1-5_0.png",
				"Items_1-6_0.png",
				"Items_1-7_0.png",
				"Items_1-8_0.png",
				"Items_1-9_0.png",
				"Items_1-10_0.png",
				"Items_1-11_0.png",
				"Items_1-12_0.png",
				"Items_1-13_0.png",
				"Items_1-14_0.png",
				"Items_1-15_0.png",
				"Items_1-16_0.png",
				"Items_1-17_0.png",
				"Items_1-18_0.png",
				"Items_1-19_0.png",
				"Items_1-20_0.png",
				"Items_1-21_0.png",
				"Items_1-22_0.png",
				"Items_1-23_0.png",
				"Items_1-24_0.png",
				"Items_1-25_0.png",
				"Items_1-26_0.png",
				"Items_1-27_0.png",
				"Items_1-28_0.png",
				"Items_1-29_0.png",
				"Items_1-30_0.png",
				"Items_1-31_0.png",
				"Items_2-0_0.png",
				"Items_2-1_0.png",
				"Items_2-2_0.png",
				"Items_2-3_0.png",
				"Items_2-4_0.png",
				"Items_2-5_0.png",
				"Items_2-6_0.png",
				"Items_2-7_0.png",
				"Items_2-8_0.png",
				"Items_2-9_0.png",
				"Items_2-10_0.png",
				"Items_2-11_0.png",
				"Items_2-12_0.png",
				"Items_2-13_0.png",
				"Items_2-14_0.png",
				"Items_2-15_0.png",
				"Items_2-16_0.png",
				"Items_2-17_0.png",
				"Items_2-18_0.png",
				"Items_2-19_0.png",
				"Items_2-20_0.png",
				"Items_2-21_0.png",
				"Items_2-22_0.png",
				"Items_2-23_0.png",
				"Items_2-24_0.png",
				"Items_2-25_0.png",
				"Items_2-26_0.png",
				"Items_2-27_0.png",
				"Items_2-28_0.png",
				"Items_2-29_0.png",
				"Items_2-30_0.png",
				"Items_2-31_0.png",
				"Items_5-0_0.png",
				"Items_5-1_0.png",
				"Items_5-2_0.png",
				"Items_5-3_0.png",
				"Items_5-4_0.png",
				"Items_5-5_0.png",
				"Items_5-6_0.png",
				"Items_5-7_0.png",
				"Items_5-8_0.png",
				"Items_5-9_0.png",
				"Items_5-10_0.png",
				"Items_5-11_0.png",
				"Items_5-12_0.png",
				"Items_5-13_0.png",
				"Items_5-14_0.png",
				"Items_5-15_0.png",
				"Items_5-16_0.png",
				"Items_5-17_0.png",
				"Items_5-18_0.png",
				"Items_5-19_0.png",
				"Items_5-20_0.png",
				"Items_5-21_0.png",
				"Items_5-22_0.png",
				"Items_5-23_0.png",
				"Items_5-24_0.png",
				"Items_5-25_0.png",
				"Items_5-26_0.png",
				"Items_5-27_0.png",
				"Items_5-28_0.png",
				"Items_5-29_0.png",
				"Items_5-30_0.png",
				"Items_5-31_0.png",
				"Items_6-0_0.png",
				"Items_6-1_0.png",
				"Items_6-2_0.png",
				"Items_6-3_0.png",
				"Items_6-4_0.png",
				"Items_6-5_0.png",
				"Items_6-6_0.png",
				"Items_6-7_0.png",
				"Items_6-8_0.png",
				"Items_6-9_0.png",
				"Items_6-10_0.png",
				"Items_6-11_0.png",
				"Items_6-12_0.png",
				"Items_6-13_0.png",
				"Items_6-14_0.png",
				"Items_6-15_0.png",
				"Items_6-16_0.png",
				"Items_6-17_0.png",
				"Items_6-18_0.png",
				"Items_6-19_0.png",
				"Items_6-20_0.png",
				"Items_6-21_0.png",
				"Items_6-22_0.png",
				"Items_6-23_0.png",
				"Items_6-24_0.png",
				"Items_6-25_0.png",
				"Items_6-26_0.png",
				"Items_6-27_0.png",
				"Items_6-28_0.png",
				"Items_6-29_0.png",
				"Items_6-30_0.png",
				"Items_6-31_0.png",
				"Items_7-0_0.png",
				"Items_7-1_0.png",
				"Items_7-2_0.png",
				"Items_7-3_0.png",
				"Items_7-4_0.png",
				"Items_7-5_0.png",
				"Items_7-6_0.png",
				"Items_7-7_0.png",
				"Items_7-8_0.png",
				"Items_7-9_0.png",
				"Items_7-10_0.png",
				"Items_7-11_0.png",
				"Items_7-12_0.png",
				"Items_7-13_0.png",
				"Items_7-14_0.png",
				"Items_7-15_0.png",
				"Items_7-16_0.png",
				"Items_7-17_0.png",
				"Items_7-18_0.png",
				"Items_7-19_0.png",
				"Items_7-20_0.png",
				"Items_7-21_0.png",
				"Items_7-22_0.png",
				"Items_7-23_0.png",
				"Items_7-24_0.png",
				"Items_7-25_0.png",
				"Items_7-26_0.png",
				"Items_7-27_0.png",
				"Items_7-28_0.png",
				"Items_7-29_0.png",
		};
		
		BufferedImage img = new BufferedImage(TILE_SIZE * 16, TILE_SIZE * 16, BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		for (int i = 0; i < items.length; ++i) {
			Path path = getImagePath(items[i]);
			if (path == null) {
				continue;
			}
			BufferedImage tile = ImageIO.read(path.toFile());
			g.drawImage(tile, (i % 16) * TILE_SIZE, (i / 16) * TILE_SIZE, null);
		}

		// effects

		final String[] fx = {
				"fx_4-0_0.png",
				"fx_4-0_1.png",
				"fx_4-0_2.png",
				"fx_4-3_0.png",
				"fx_4-3_1.png",
				"fx_4-3_2.png",
				"fx_4-3_3.png",
				"fx_4-3_4.png",
				null,
				"dust_4-0_0.png",
				"dust_4-0_1.png",
				"dust_4-0_2.png",
				"dust_4-0_3.png",
				"dust_4-0_4.png",
				"dust_4-0_5.png",
				"dust_4-0_6.png",
				"dust_4-0_7.png",
				null,
				"dust_4-16_0.png",
				"dust_4-16_1.png",
				"dust_4-16_2.png",
				"dust_4-16_3.png",
				"dust_4-16_4.png",
				null,
				"fx_5-0_0.png",
				"fx_5-0_1.png",
				"fx_5-0_2.png",
				"fx_5-0_3.png",
				"fx_5-0_4.png",
		};

		int x = 0;
		int y = TILE_SIZE * 12;
		for (int i = 0; i < fx.length; ++i) {
			if (fx[i] == null) {
				x = 0;
				y += TILE_SIZE;
				continue;
			}
			Path path = getImagePath(fx[i]);
			if (path != null) {
				BufferedImage tile = ImageIO.read(path.toFile());
				g.drawImage(tile, x, y, null);
			}
			x += TILE_SIZE;
		}

		// arrow
		g.drawImage(arrow(), TILE_SIZE * 8, TILE_SIZE * 12, null);
		
		writePng(img, resDir.resolve("items.png"));
	}
	
	static void objects() throws IOException {
		final Object[] objects = {
				"Desks_0-8_0.png", 0, 0,
				"Desks_0-24_0.png", 1, 0,
				"Canteen - Serving_0-0_0.png", 2, 0,
				"Canteen - Serving_0-0_1.png", 3, 0,
				"Canteen - Serving_0-0_2.png", 4, 0,
				"Canteen - Serving_0-0_3.png", 5, 0,
				"Canteen - Serving_0-0_4.png", 6, 0,
				"Canteen - Serving_0-0_5.png", 7, 0,
				"Table 2_0-0_0.png", 8, 0,
				"Container - Canteen_0-0_0.png", 9, 0,
				"Interwebs_0-0_0.png", 10, 0,
				"Gym 3_0-0_0.png", 11, 0,
				"Gym 4_0-0_0.png", 12, 0,
				"Gym 4_1-0_0.png", 13, 0,
				"Metal Detector_0-24_0.png", 14, 0,
				"Metal Detector_5-24_1.png", 15, 0,
				"Toilet 2_0-0_0.png", 0, 1,
				"Toilet 2_0-16_0.png", 1, 1,
				"Ladders_0-8_0.png", 2, 1,
				"Ladders_0-24_0.png", 3, 1,
				"Container - Janitor_0-0_0.png", 4, 1,
				"Container - MEDICAL_0-0_0.png", 5, 1,
				"Container - Book Chest_0-0_0.png", 6, 1,
				"Container - Deliveries 2_0-0_0.png", 7, 1,
				"Container - Deliveries_0-0_0.png", 8, 1,
				"Payphone_0-0_0.png", 9, 1,
				"Canteen chair_0-24_0.png", 10, 1,
				"Chinup Bar_0-16_0.png", 11, 1,
				"Pushup Mat_0-16_0.png", 12, 1,
				"speed bag_0-16_0.png", 13, 1,
				"Skipping Mat_0-16_0.png", 14, 1,
				"Jogging Mat_0-0_0.png", 15, 1,
				"Door - Cell_0-16_0.png", 0, 2,
				"Door - Cell_0-0_0.png", 1, 2,
				"Door - Woodshop_0-0_0.png", 2, 2,
				"Door - Master_0-0_0.png", 3, 2,
				"Door - Outside_0-0_0.png", 4, 2,
				"Door - Staff_0-0_0.png", 5, 2,
				"Door - Utility_0-0_0.png", 6, 2,
				"Stash_0-0_0.png", 7, 2,
				"Stash_0-0_1.png", 8, 2,
				"Stash_0-0_2.png", 9, 2,
				"Stash_0-0_3.png", 10, 2,
				"Dirt_1-0_0.png", 11, 2,
				"Dirt_1-0_1.png", 12, 2,
				"Dirt_1-0_2.png", 13, 2,
				"Dirt_1-0_3.png", 14, 2,
				"Dirt_0-0_0.png", 15, 2,
				"CAMERA_0-18_0.png", 0, 3,
				"CAMERA_0-21_0.png", 1, 3,
				"CAMERA_0-24_0.png", 2, 3,
				"CAMERA_0-27_0.png", 3, 3,
				"CAMERA_0-30_0.png", 4, 3,
				"CAMERA_2-18_1.png", 5, 3,
				"CAMERA_2-21_1.png", 6, 3,
				"CAMERA_2-24_1.png", 7, 3,
				"CAMERA_2-27_1.png", 8, 3,
				"CAMERA_2-30_1.png", 9, 3,
				"CAMERA_5-18_0.png", 10, 3,
				"CAMERA_5-21_0.png", 11, 3,
				"CAMERA_5-24_0.png", 12, 3,
				"CAMERA_5-27_0.png", 13, 3,
				"CAMERA_5-30_0.png", 14, 3,
				"TV_0-0_0.png", 15, 3,
				"soil_0-0_0.png", 0, 4,
				"Active 10_0-0_0.png", 1, 4,
				"Active 10_2-0_0.png", 2, 4,
				"Dig Hole_0-0_0.png", 3, 4,
				"Dig Hole_0-1_0.png", 4, 4,
				"Dig Hole_0-2_0.png", 5, 4,
				"Dig Hole_0-3_0.png", 6, 4,
				"Dig Hole_0-4_0.png", 7, 4,
				"Dig Hole_0-16_0.png", 8, 4,
				"Dig Hole_0-17_0.png", 9, 4,
				"punch bag 2_0-16_0.png", 10, 4,
				"punch bag 2_1-0_1.png", 11, 4,
				"punch bag 2_1-0_0.png", 12, 4,
				"punch bag 3_0-16_0.png", 13, 4,
				"punch bag 3_1-0_1.png", 14, 4,
				"punch bag 3_1-0_0.png", 15, 4,
				"Vent Cover_0-0_0.png", 0, 5,
				"Vent Cover_1-0_0.png", 1, 5,
				"Vent Cover_2-0_0.png", 2, 5,
				"Fan_0-24_0.png", 3, 5,
				"Sheets + Posters_1-0_0.png", 4, 5,
				"Sheets + Posters_0-0_0.png", 5, 5,
				"Active 9_0-0_0.png", 6, 5,
				"soil_0-0_0.png", 7, 5, // hack
				"Brace_0-0_0.png", 7, 5,
				"Toilet 2_1-0_2.png", 8, 5,
				"Toilet 2_1-16_2.png", 9, 5,
				"Toilet 2_5-0_0.png", 10, 5,
				"Toilet 2_5-16_0.png", 11, 5,
				"Stash_4-0_0.png", 12, 5,
				"Dropped Sheet_4-0_0.png", 13, 5,
				"Job Board_0-0_0.png", 14, 5,
				"Job Board_0-0_1.png", 15, 5,
				"Container - Laundry - Dirty_0-0_0.png", 0, 6, // 1x2
				"Container - Laundry - Clean_0-0_0.png", 1, 6, // 1x2
				"Container - Tailor IN_0-0_0.png", 2, 6, // 1x2
				"Container - Tailor OUT_0-0_0.png", 3, 6, // 1x2
				"Container - Timber 2_0-0_0.png", 4, 6, // 1x2
				"Container - Timber_0-0_0.png", 5, 6, // 1x2
				"Container - Kitchen_0-0_0.png", 6, 6, // 1x2
				"Metalshop 2_0-0_0.png", 7, 6, // 1x2
				"Metalshop_0-0_0.png", 8, 6, // 1x2
				"Kitchen - Sink_0-0_0.png", 9, 6, // 1x2
				"Metal Detector_0-0_0.png", 10, 6, // 1x2
				"Metal Detector_5-0_1.png", 11, 6, // 1x2
				"Hiding_0-8_0.png", 12, 6, // 1x2
				"Fan_0-0_0.png", 13, 6,
				"Truck_0-8_0.png", 14, 6, // 2x3
				"Oven 2_0-0_0.png", 0, 8, // 1x2
				"Oven 2_1-0_0.png", 1, 8, // 1x2
				"Oven 2_1-0_1.png", 2, 8, // 1x2
				"Oven 2_5-0_0.png", 3, 8, // 1x2
				"Oven 2_5-0_1.png", 4, 8, // 1x2
				"Washing 2_0-0_0.png", 5, 8, // 1x2
				"Washing 2_1-0_0.png", 6, 8, // 1x2
				"Washing 2_1-0_1.png", 7, 8, // 1x2
				"Washing 2_1-0_2.png", 8, 8, // 1x2
				"Washing 2_1-0_3.png", 9, 8, // 1x2
				"Washing 2_5-0_1.png", 10, 8, // 1x2
				"Washing 2_5-0_0.png", 11, 8, // 1x2
				"License Prints_0-0_0.png", 12, 8, // 1x2
				"License Prints_1-0_0.png", 13, 8, // 1x2
				"Bed - Player_0-8_0.png", 0, 10, // 1x2
				"Bed - Player_1-8_0.png", 1, 10, // 1x2
				"Bed - Player_2-8_0.png", 2, 10, // 1x2
				"Bed - NPC_0-24_0.png", 3, 10, // 1x2
				"Bed - NPC_1-24_0.png", 4, 10, // 1x2
				"Bed - NPC_2-24_0.png", 5, 10, // 1x2
				"Bed - Medical_0-24_0.png", 6, 10, // 1x2
				"Bed - Solitary_0-24_0.png", 7, 10, // 1x2
				"Bed -guard_0-24_0.png", 8, 10, // 1x2
				"Dummies_0-0_0.png", 9, 10, // 1x2
				"Truck_0-24_0.png", 10, 10, // 2x3
				"Container - Gardening_0-0_0.png", 0, 12, // 2x1
				"Checkpoint_0-0_0.png", 2, 12,
				"Bed - Lounger_0-24_0.png", 3, 12, // 1x2
				"Truck_0-16_0.png", 4, 12, // 3x2
				"Truck_0-0_0.png", 7, 12, // 3x2
				"Shower_0-0_0.png", 0, 13,
				"Shower_0-0_1.png", 1, 13,
				"Shower_0-0_2.png", 2, 13,
				"Checkpoint - Guard_0-8_0.png", 10, 12, // 1x2
				"Checkpoint - Guard_0-8_1.png", 11, 12, // 1x2
				"container - mail_0-0_0.png", 0, 14, // 2x2
				"Library 2_0-0_0.png", 2, 14, // 2x2
				"POWER GEN_0-0_0.png", 4, 14, // 2x2
				"POWER GEN_0-0_1.png", 6, 14, // 2x2
				"POWER GEN_2-0_0.png", 8, 14, // 2x2
				"POWER GEN_2-0_1.png", 10, 14, // 2x2
		};

		BufferedImage img = new BufferedImage(TILE_SIZE * 16, TILE_SIZE * 16, BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		
		for (int i = 0; i < objects.length; i += 3) {
			String name = (String) objects[i];
			int x = (int) objects[i + 1];
			int y = (int) objects[i + 2];
			
			Path path = getImagePath(name);
			if (path == null) {
				continue;
			}
			BufferedImage tile = ImageIO.read(path.toFile());
			g.drawImage(tile, x * TILE_SIZE, y * TILE_SIZE, null);
		}
		
		writePng(img, resDir.resolve("objects.png"));
	}
	
	static void character(String name, String out, int rows) throws IOException {
		BufferedImage img = new BufferedImage(TILE_SIZE * 8, TILE_SIZE * rows, BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		
		String[] names;
		
		if (rows == 1) {
			names = new String[] {
					"-0_0.png",
					"-0_1.png",
					"-8_0.png",
					"-8_1.png",
					"-16_0.png",
					"-16_1.png",
					"-24_0.png",
					"-24_1.png",
			};
		} else {
			names = new String[] {
					"_0-0_0.png",
					"_0-0_1.png",
					"_0-8_0.png",
					"_0-8_1.png",
					"_0-16_0.png",
					"_0-16_1.png",
					"_0-24_0.png",
					"_0-24_1.png",
					"_6-0_0.png",
					"_6-8_0.png",
					"_6-16_0.png",
					"_6-24_0.png",
					"_8-0_0.png",
					"_8-16_0.png",
					"_8-16_1.png",
					"_8-24_1.png",
					"_11-0_0.png",
					"_11-0_1.png",
					"_11-8_0.png",
					"_11-8_1.png",
					"_11-16_0.png",
					"_11-16_1.png",
					"_11-24_0.png",
					"_11-24_1.png",
					"_9-0_0.png",
					"_9-0_1.png",
					"_10-0_0.png",
					"_10-0_1.png",
					"_10-0_2.png",
			};
			if ("Inmate 4".equals(name)) {
				names[2] = "_0-8_1.png";
				names[3] = "_0-8_0.png";
			} else if ("Outfit - Inmate".equals(name)) {
				names[26] = "_10-0_2.png";
				names[28] ="_10-0_0.png";
			}
		}
		
		for (int i = 0; i < names.length; ++i) {
			if (i / 8 >= rows) {
				break;
			}
			Path path = getImagePath(name + names[i]);
			if (path == null) {
				continue;
			}
			BufferedImage tile = ImageIO.read(path.toFile());
			int x = 0, y = 0;
			int w = tile.getWidth(), h = tile.getHeight();
			if (h == 32) {
				if (i == 12) {
					tile = tile.getSubimage(0, 17, 16, 15);
					y = 1;
				} else {
					tile = tile.getSubimage(0, 16, 16, 16);
				}
			} else if (h == 18) {
				tile = tile.getSubimage(0, 2, 16, 16);
			} else if (h < 16) {
				x = 3;
				y = 8;
			}
			g.drawImage(tile, (i % 8) * TILE_SIZE + x, (i / 8) * TILE_SIZE + y, null);
		}
		
		writePng(img, resDir.resolve(out));
	}
	
	static void ground(String in, String out) throws IOException {
		Path dir = gameDir.resolve("Data").resolve("images");

		Path soilPath = dir.resolve("soil.gif");
		if (!Files.exists(soilPath)) {
			throw new IOException(soilPath.toString());
		}
		BufferedImage soil = ImageIO.read(soilPath.toFile());

		Path groundPath = dir.resolve(in);
		if (!Files.exists(groundPath)) {
			throw new IOException(groundPath.toString());
		}
		BufferedImage ground = ImageIO.read(groundPath.toFile());
		
		BufferedImage img = new BufferedImage(TILE_SIZE * 6, TILE_SIZE * 3, BufferedImage.TYPE_INT_RGB);
		Graphics g = img.getGraphics();
		g.drawImage(ground.getSubimage(0, 0, 48, 48), 0, 0, null);
		g.drawImage(soil.getSubimage(0, 0, 48, 48), TILE_SIZE * 3, 0, null);
		
		writePng(img, resDir.resolve(out));
	}
	
	static void tiles(String in, String out) throws IOException {
		Path dir = gameDir.resolve("Data").resolve("images");
		
		Path path = dir.resolve(in);
		if (!Files.exists(path)) {
			throw new IOException(path.toString());
		}
		BufferedImage tiles = ImageIO.read(new ByteArrayInputStream(BlowfishCompatEncryption.decrypt(Files.readAllBytes(path))));
		int w = tiles.getWidth();
		int h = tiles.getHeight();
		for (int x = 0; x < w; ++x) {
			for (int y = 0; y < h; ++y) {
				if (tiles.getRGB(x, y) == 0xFFFFFFFF) {
					tiles.setRGB(x, y, 0);
				}
			}
		}
		BufferedImage img = new BufferedImage(TILE_SIZE * 4, TILE_SIZE * 25, BufferedImage.TYPE_INT_ARGB);
		img.getGraphics().drawImage(tiles, 0, 0, null);
		writePng(img, resDir.resolve(out));
	}
	
	static void light() throws IOException {
		BufferedImage img = new BufferedImage(TILE_SIZE * 4, TILE_SIZE * 4, BufferedImage.TYPE_INT_ARGB);
		img.getGraphics().drawImage(ImageIO.read(getImagePath("Light_0-0_0.png").toFile()), 2, 2, null);
		writePng(img, resDir.resolve("light.png"));

		img = ImageIO.read(getImagePath("Underground_Lighting_0-0_0.png").toFile());
		img = img.getSubimage((img.getWidth() - 64) / 2, (img.getHeight() - 64) / 2, 64, 64);
		for (int x = 0; x < 64; ++x) {
			for (int y = 0; y < 64; ++y) {
				img.setRGB(x, y, ~(img.getRGB(x, y) & 0xFFFFFF) | 0xFF000000);
			}
		}
		writePng(img, resDir.resolve("underground.png"));
	}
	
	static void huds() throws IOException {
		Path healthPath = getImagePath("30.png");
		Path heatPath = getImagePath("55.png");
		Path fatiguePath = getImagePath("111.png");
		Path moneyPath = getImagePath("473.png");
		Path crewPath = getImagePath("1439.png");
		if (healthPath == null || heatPath == null || fatiguePath == null || moneyPath == null || crewPath == null) {
			throw new IOException();
		}
		
		BufferedImage health = ImageIO.read(healthPath.toFile());
		BufferedImage heat = ImageIO.read(heatPath.toFile());
		BufferedImage fatigue = ImageIO.read(fatiguePath.toFile());
		BufferedImage money = ImageIO.read(moneyPath.toFile());
		BufferedImage crew = ImageIO.read(crewPath.toFile());
		
		BufferedImage img = new BufferedImage(110, 55, BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();

		g.drawImage(money, 0, 0, 90, 11, 145, 2, 145 + 90, 2 + 11, null);
		g.drawImage(money, 90, 0, 90 + 9, 11, 37, 2, 37 + 9, 2 + 11, null);
		
		g.drawImage(health, 0, 11, 90, 11 + 11, 145, 2, 145 + 90, 2 + 11, null);
		g.drawImage(health, 90, 11, 90 + 9, 11 + 11, 99, 2, 99 + 9, 2 + 11, null);
		
		g.drawImage(heat, 0, 22, 90, 22 + 11, 145, 2, 145 + 90, 2 + 11, null);
		g.drawImage(heat, 90, 22, 90 + 9, 22 + 11, 99, 2, 99 + 9, 2 + 11, null);
		g.drawImage(heat, 100, 22, 100 + 9, 22 + 11, 45, 2, 45 + 9, 2 + 11, null);
		
		g.drawImage(fatigue, 0, 33, 90, 33 + 11, 145, 2, 145 + 90, 2 + 11, null);
		g.drawImage(fatigue, 90, 33, 90 + 9, 33 + 11, 99, 2, 99 + 9, 2 + 11, null);
		g.drawImage(fatigue, 100, 33, 100 + 9, 33 + 11, 45, 2, 45 + 9, 2 + 11, null);
		
		g.drawImage(crew, 0, 44, 90, 44 + 11, 145, 2, 145 + 90, 2 + 11, null);
		g.drawImage(crew, 90, 44, 90 + 9, 44 + 11, 100, 2, 100 + 9, 2 + 11, null);
		g.drawImage(crew, 100, 44, 100 + 9, 44 + 11, 217, 28, 217 + 9, 28 + 11, null);
		
		writePng(img, resDir.resolve("huds.png"));
	}

	static void markers() throws IOException {
		BufferedImage img = new BufferedImage(50, 30, BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();

		g.drawImage(ImageIO.read(getImagePath("Marker - Vendor_0-0_0.png").toFile()), 0, 0, null);
		g.drawImage(ImageIO.read(getImagePath("Marker - Vendor_5-0_0.png").toFile()).getSubimage(2, 0, 4, 9), 7, 0, null);

		BufferedImage font = ImageIO.read(getImagePath("2178.png").toFile());
		g.drawImage(font.getSubimage(1, 9, 5, 7), 11, 0, null);
		g.drawImage(font.getSubimage(45, 2, 6, 6), 16, 0, null);
		g.drawImage(font.getSubimage(10, 9, 5, 7), 22, 0, null);

//		g.drawImage(font.getSubimage(30, 17, 5, 7), 0, 23, null);
//		g.drawImage(font.getSubimage(5, 17, 5, 7), 5, 23, null);
//		g.drawImage(font.getSubimage(100, 17, 4, 7), 10, 23, null);

		g.drawImage(font.getSubimage(95, 9, 5, 7), 0, 23, null);
		g.drawImage(font.getSubimage(101, 9, 4, 7), 5, 23, null);
		g.drawImage(font.getSubimage(90, 9, 5, 7), 9, 23, null);

		g.drawImage(font.getSubimage(46, 9, 4, 7), 15, 23, null);
		g.drawImage(font.getSubimage(70, 9, 5, 7), 19, 23, null);
		g.drawImage(font.getSubimage(101, 9, 4, 7), 24, 23, null);

		g.drawImage(font.getSubimage(95, 9, 5, 7), 30, 23, null);
		g.drawImage(font.getSubimage(80, 9, 5, 7), 35, 23, null);
		g.drawImage(font.getSubimage(20, 9, 5, 7), 40, 23, null);

		g.drawImage(ImageIO.read(getImagePath("2327.png").toFile()), 27, 0, null);
		g.drawImage(ImageIO.read(getImagePath("1456.png").toFile()), 34, 0, null);
		g.drawImage(ImageIO.read(getImagePath("660.png").toFile()), 41, 0, null);

		font = ImageIO.read(getImagePath("587.png").toFile());
		g.drawImage(font.getSubimage(80, 1, 50, 7), 0, 9, null);
		g.drawImage(font.getSubimage(65, 4, 4,2), 45, 23, null);

		font = ImageIO.read(getImagePath("94.png").toFile());
		g.drawImage(font.getSubimage(80, 1, 50, 7), 0, 16, null);
		g.drawImage(font.getSubimage(65, 4, 4,2), 45, 25, null);

		writePng(img, resDir.resolve("markers.png"));
	}

	static BufferedImage arrow() throws IOException {
		int w = 16;
		int h = 16;
		int angles = 5;
		BufferedImage res = new BufferedImage(w * angles, h * 4, BufferedImage.TYPE_INT_ARGB);
		Graphics resGraphics = res.getGraphics();

		BufferedImage orig0 = ImageIO.read(getImagePath("Dest Arrow_0-0_0.png").toFile());
		BufferedImage orig1 = ImageIO.read(getImagePath("Dest Arrow_0-0_1.png").toFile());
		BufferedImage orig2 = ImageIO.read(getImagePath("Dest Arrow_0-0_2.png").toFile());
		BufferedImage orig3 = ImageIO.read(getImagePath("Dest Arrow_0-0_3.png").toFile());

		BufferedImage t;
		BufferedImage rotated;
		Graphics2D g;

		t = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		g = (Graphics2D) t.getGraphics();
		g.drawImage(orig0, -2, 3, null);
		resGraphics.drawImage(t, 0, 0, null);

		for (int i = 1; i < angles; i++) {
			rotated = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			g = (Graphics2D) rotated.getGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.rotate(-Math.toRadians(i * (90d / angles)), w / 2d, h / 2d);
			g.drawImage(t, 0, 0, null);
			resGraphics.drawImage(rotated, i * w, 0, null);
		}

		t = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		g = (Graphics2D) t.getGraphics();
		g.drawImage(orig1, -1, 3, null);

		resGraphics.drawImage(t, 0, h, null);
		for (int i = 1; i < angles; i++) {
			rotated = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			g = (Graphics2D) rotated.getGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.rotate(-Math.toRadians(i * (90d / angles)), w / 2d, h / 2d);
			g.drawImage(t, 0, 0, null);
			resGraphics.drawImage(rotated, i * w, h, null);
		}

		t = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		g = (Graphics2D) t.getGraphics();
		g.drawImage(orig2, 0, 3, null);

		resGraphics.drawImage(t, 0, 2 * h, null);
		for (int i = 1; i < angles; i++) {
			rotated = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			g = (Graphics2D) rotated.getGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.rotate(-Math.toRadians(i * (90d / angles)), w / 2d, h / 2d);
			g.drawImage(t, 0, 0, null);
			resGraphics.drawImage(rotated, i * w, 2 * h, null);
		}

		t = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		g = (Graphics2D) t.getGraphics();
		g.drawImage(orig3, -1, 3, null);
		resGraphics.drawImage(t, 0, 3 * h, null);

		for (int i = 1; i < angles; i++) {
			rotated = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			g = (Graphics2D) rotated.getGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.rotate(-Math.toRadians(i * (90d / angles)), w / 2d, h / 2d);
			g.drawImage(t, 0, 0, null);
			resGraphics.drawImage(rotated, i * w, 3 * h, null);
		}

		w = res.getWidth();
		h = res.getHeight();
		for (int x = 0; x < w; ++x) {
			for (int y = 0; y < h; ++y) {
				int rgb = res.getRGB(x, y);
				res.setRGB(x, y, ((rgb >> 24) & 0xFF) >= 0x80 ? 0xFF000000 | rgb : 0);
			}
		}

		return res;
	}

	static void icon() throws IOException {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
		Graphics g = img.getGraphics();
		g.setColor(new Color(0x808080));
		g.fillRect(0, 0, 16, 16);
		g.drawImage(ImageIO.read(getImagePath("Inmate 4_0-24_0.png").toFile()), 0, 0, null);
		g.drawImage(ImageIO.read(getImagePath("Outfit - Inmate_0-24_0.png").toFile()), 0, 0, null);

		writePng(img, resDir.resolve("icon.png"));
	}
	
	static void title() throws IOException {
		Path titlePath = decompiledDir.resolve(Paths.get("Images", "61.png"));//decompiledDir.resolve(Paths.get("Sorted Images", "[2] title_screen", "[UNSORTED]", "Active_0-0_0.png"));
		Files.copy(titlePath, resDir.resolve("title.png"), StandardCopyOption.REPLACE_EXISTING);
	}
	
	static void map(String map) throws Exception {
		System.out.println();
		System.out.println("Map: " + map);

		tiles("tiles_" + map + ".gif", "tiles_" + map + ".png");
		ground("ground_" + map + ".gif", "ground_" + map + ".png");

		MapCompiler.process(gameDir.resolve("Data").resolve("Maps").resolve(map + ".map"), resDir.resolve("map_" + map));
	}

	static void sounds() throws Exception {
		final String[] sounds = {
				"accolade.wav", "44100", null,
				"bell.wav", "32000", null,
				"buy.wav", "32000", null,
				"close.wav", "32000", null,
				"door.wav", "44100", null,
				"en_hit.wav", "32000", null,
				"open.wav", "32000", null,
				"pickup.wav", "44100", null,
				"plip.wav", "32000", "adelay=3:all=1",
				"rumble.wav", "32000", null,
				"lose.wav", "44100", null,
				"throw.wav", "32000", null,
				"hp.wav", "32000", null,
				"shoot.wav", "32000", null,
				"hit1.wav", "32000", null,
		};
		for (int i = 0; i < sounds.length; i += 3) {
			String file = sounds[i];
			Path path = decompiledDir.resolve(Paths.get("Sounds", file));
			Path resPath = resDir.resolve(file);
			if (!force && Files.exists(resPath)) {
				continue;
			}
			ArrayList<String> cmd = new ArrayList<>();
			cmd.add("ffmpeg");
			cmd.add("-i");
			cmd.add(path.toString());
			cmd.add("-ac");
			cmd.add("1");
			cmd.add("-filter:a");
			cmd.add("dynaudnorm=p=0.9:s=5" + (sounds[i + 2] != null ? "," + sounds[i + 2] : ""));
			cmd.add("-ar");
			cmd.add(sounds[i + 1]);
			cmd.add("-acodec");
			cmd.add("pcm_u8");
			cmd.add("-y");
			cmd.add(resPath.toString());
			if (run(cmd.toArray(new String[0])) != 0) {
				throw new Exception(file);
			}
		}
	}

	static void initMap() {
		map.put("Items_5-0_0.png", "812.png");
		map.put("Items_7-15_0.png", "1484.png");
		map.put("Extra NPCs_1-24_0.png", "1339.png");
		map.put("CAMERA_0-27_0.png", "854.png");
		map.put("Extra NPCs_2-16_1.png", "128.png");
		map.put("Gym 3_0-0_0.png", "882.png");
		map.put("Outfit - Inmate_8-16_0.png", "42.png");
		map.put("Items_1-25_0.png", "2236.png");
		map.put("dust_4-0_0.png", "422.png");
		map.put("Inmate 3_0-8_0.png", "552.png");
		map.put("Items_5-14_0.png", "943.png");
		map.put("Inmate 4_8-16_1.png", "744.png");
		map.put("Inmate 3_0-24_1.png", "551.png");
		map.put("Items_1-12_0.png", "486.png");
		map.put("Outfit - Inmate_6-0_0.png", "787.png");
		map.put("Inmate_0-24_1.png", "540.png");
		map.put("Fan_0-24_0.png", "96.png");
		map.put("Bed - NPC_0-24_0.png", "856.png");
		map.put("Inmate 2_9-0_0.png", "1505.png");
		map.put("Pushup Mat_0-16_0.png", "1489.png");
		map.put("Items_1-3_0.png", "122.png");
		map.put("dust_4-16_2.png", "433.png");
		map.put("Items_6-4_0.png", "91.png");
		map.put("dust_4-0_7.png", "429.png");
		map.put("Dest Arrow_0-0_2.png", "1007.png");
		map.put("Outfit - Guard_6-0_0.png", "1416.png");
		map.put("Items_2-23_0.png", "366.png");
		map.put("Items_6-17_0.png", "28.png");
		map.put("Door - Cell_0-0_0.png", "1226.png");
		map.put("Items_0-14_0.png", "74.png");
		map.put("Items_0-22_0.png", "12.png");
		map.put("fx_4-3_4.png", "952.png");
		map.put("Stash_0-0_0.png", "1574.png");
		map.put("Items_2-15_0.png", "19.png");
		map.put("Canteen - Serving_0-0_5.png", "907.png");
		map.put("Items_5-19_0.png", "324.png");
		map.put("Items_5-22_0.png", "398.png");
		map.put("Outfit - Inmate_10-0_0.png", "765.png");
		map.put("Items_6-31_0.png", "1448.png");
		map.put("Items_1-20_0.png", "536.png");
		map.put("Items_2-7_0.png", "658.png");
		map.put("Inmate 3_0-16_0.png", "573.png");
		map.put("Items_0-4_0.png", "287.png");
		map.put("Extra NPCs_1-8_1.png", "1336.png");
		map.put("Items_1-17_0.png", "534.png");
		map.put("Inmate_0-16_0.png", "571.png");
		map.put("Oven 2_1-0_1.png", "771.png");
		map.put("Inmate 4_10-0_1.png", "493.png");
		map.put("CAMERA_5-27_0.png", "1077.png");
		map.put("Outfit - Inmate_8-24_1.png", "712.png");
		map.put("Metalshop 2_0-0_0.png", "925.png");
		map.put("Guard_0-16_0.png", "505.png");
		map.put("Items_0-19_0.png", "3.png");
		map.put("Outfit - Inmate_9-0_1.png", "1493.png");
		map.put("Items_1-8_0.png", "401.png");
		map.put("Sheets + Posters_1-0_0.png", "727.png");
		map.put("Door - Cell_0-16_0.png", "1227.png");
		map.put("fx_5-0_3.png", "254.png");
		map.put("Guard_0-24_1.png", "508.png");
		map.put("Tower Guard_0-0_0.png", "991.png");
		map.put("Items_2-0_0.png", "722.png");
		map.put("Guard_0-8_1.png", "509.png");
		map.put("Washing 2_1-0_1.png", "919.png");
		map.put("Extra NPCs_2-0_1.png", "126.png");
		map.put("punch bag 2_1-0_0.png", "2296.png");
		map.put("Container - Book Chest_0-0_0.png", "931.png");
		map.put("Container - Janitor_0-0_0.png", "929.png");
		map.put("Inmate 2_6-24_0.png", "788.png");
		map.put("Items_2-2_0.png", "709.png");
		map.put("Outfit - Guard_0-0_0.png", "1415.png");
		map.put("Outfit - Inmate_0-0_0.png", "564.png");
		map.put("Items_0-16_0.png", "284.png");
		map.put("Container - Canteen_0-0_0.png", "892.png");
		map.put("Inmate_0-24_0.png", "541.png");
		map.put("Inmate 2_6-16_0.png", "258.png");
		map.put("Items_7-3_0.png", "1539.png");
		map.put("Checkpoint - Guard_0-8_1.png", "2122.png");
		map.put("Items_0-9_0.png", "327.png");
		map.put("dust_4-16_3.png", "434.png");
		map.put("Inmate 3_0-8_1.png", "553.png");
		map.put("Outfit - Inmate_8-16_1.png", "46.png");
		map.put("Items_0-6_0.png", "325.png");
		map.put("Inmate 4_6-0_0.png", "1072.png");
		map.put("Dirt_1-0_3.png", "603.png");
		map.put("Vent Cover_2-0_0.png", "418.png");
		map.put("Job Board_0-0_1.png", "1285.png");
		map.put("License Prints_0-0_0.png", "924.png");
		map.put("Outfit - Guard_11-16_1.png", "1406.png");
		map.put("Tower Guard_0-16_0.png", "1299.png");
		map.put("Marker - Vendor_5-0_0.png", "49.png");
		map.put("Items_2-31_0.png", "831.png");
		map.put("Items_6-10_0.png", "26.png");
		map.put("Items_1-1_0.png", "110.png");
		map.put("Items_5-7_0.png", "875.png");
		map.put("Items_6-20_0.png", "402.png");
		map.put("Extra NPCs_1-24_1.png", "1340.png");
		map.put("Gym 4_1-0_0.png", "881.png");
		map.put("Inmate 4_9-0_0.png", "1490.png");
		map.put("Outfit - Guard_0-16_1.png", "498.png");
		map.put("Fan_0-0_0.png", "159.png");
		map.put("55.png", "55.png");
		map.put("CAMERA_5-30_0.png", "1078.png");
		map.put("Inmate_8-0_0.png", "757.png");
		map.put("Canteen - Serving_0-0_4.png", "906.png");
		map.put("Inmate 4_11-0_0.png", "535.png");
		map.put("Stash_0-0_1.png", "1576.png");
		map.put("Items_1-31_0.png", "69.png");
		map.put("Items_2-5_0.png", "345.png");
		map.put("Items_5-25_0.png", "956.png");
		map.put("Items_5-30_0.png", "127.png");
		map.put("Inmate 3_0-16_1.png", "576.png");
		map.put("dust_4-0_6.png", "428.png");
		map.put("Inmate 2_11-0_1.png", "969.png");
		map.put("Items_7-23_0.png", "679.png");
		map.put("Shower_0-0_2.png", "900.png");
		map.put("Dest Arrow_0-0_3.png", "1081.png");
		map.put("Underground_Lighting_0-0_0.png", "983.png");
		map.put("Chinup Bar_0-16_0.png", "2402.png");
		map.put("fx_5-0_2.png", "253.png");
		map.put("Dummies_0-0_0.png", "2375.png");
		map.put("Inmate 4_10-0_2.png", "596.png");
		map.put("Items_1-23_0.png", "153.png");
		map.put("2327.png", "2327.png");
		map.put("Inmate 2_11-0_0.png", "676.png");
		map.put("Outfit - Inmate_10-0_1.png", "376.png");
		map.put("Items_5-5_0.png", "908.png");
		map.put("Oven 2_5-0_0.png", "877.png");
		map.put("Items_7-17_0.png", "1564.png");
		map.put("Extra NPCs_1-16_0.png", "1332.png");
		map.put("Container - Timber 2_0-0_0.png", "478.png");
		map.put("Guard_0-8_0.png", "506.png");
		map.put("94.png", "94.png");
		map.put("Inmate 2_10-0_2.png", "379.png");
		map.put("Tower Guard_0-24_0.png", "21.png");
		map.put("Items_0-20_0.png", "9.png");
		map.put("Outfit - Inmate_9-0_0.png", "1492.png");
		map.put("Items_2-28_0.png", "809.png");
		map.put("Toilet 2_0-0_0.png", "93.png");
		map.put("Outfit - Guard_8-16_0.png", "967.png");
		map.put("Items_1-19_0.png", "469.png");
		map.put("Items_6-12_0.png", "1427.png");
		map.put("Inmate 3_11-24_0.png", "599.png");
		map.put("Items_7-21_0.png", "2170.png");
		map.put("Items_6-9_0.png", "1048.png");
		map.put("Metal Detector_5-0_1.png", "814.png");
		map.put("Inmate 3_6-8_0.png", "1066.png");
		map.put("Outfit - Guard_8-24_1.png", "704.png");
		map.put("Items_2-10_0.png", "866.png");
		map.put("Guard_0-16_1.png", "504.png");
		map.put("Washing 2_1-0_2.png", "918.png");
		map.put("Inmate 3_11-8_1.png", "589.png");
		map.put("Items_5-27_0.png", "770.png");
		map.put("dust_4-0_1.png", "423.png");
		map.put("Inmate 3_10-0_0.png", "579.png");
		map.put("Container - Laundry - Clean_0-0_0.png", "894.png");
		map.put("Items_7-1_0.png", "1537.png");
		map.put("Washing 2_5-0_0.png", "861.png");
		map.put("Job Board_0-0_0.png", "408.png");
		map.put("Outfit - Inmate_11-0_0.png", "358.png");
		map.put("Items_7-25_0.png", "2172.png");
		map.put("Inmate 4_0-0_0.png", "556.png");
		map.put("Dest Arrow_0-0_0.png", "1007.png");
		map.put("Items_5-24_0.png", "956.png");
		map.put("Active 10_2-0_0.png", "87.png");
		map.put("Extra NPCs_0-8_1.png", "1333.png");
		map.put("Inmate_9-0_0.png", "1497.png");
		map.put("fx_4-3_2.png", "659.png");
		map.put("Inmate 2_11-8_0.png", "1089.png");
		map.put("Inmate 4_11-24_1.png", "711.png");
		map.put("Extra NPCs_2-8_0.png", "130.png");
		map.put("Items_2-9_0.png", "873.png");
		map.put("Items_5-11_0.png", "938.png");
		map.put("Items_5-31_0.png", "1230.png");
		map.put("Dirt_1-0_2.png", "810.png");
		map.put("CAMERA_0-24_0.png", "819.png");
		map.put("Inmate 4_11-0_1.png", "537.png");
		map.put("Canteen chair_0-24_0.png", "858.png");
		map.put("Inmate 2_8-24_1.png", "696.png");
		map.put("Outfit - Inmate_11-16_1.png", "475.png");
		map.put("Stash_4-0_0.png", "1578.png");
		map.put("Outfit - Guard_11-0_0.png", "1404.png");
		map.put("Items_6-1_0.png", "1240.png");
		map.put("CAMERA_5-24_0.png", "1076.png");
		map.put("Items_2-18_0.png", "658.png");
		map.put("Inmate 4_6-16_0.png", "1074.png");
		map.put("Items_1-0_0.png", "109.png");
		map.put("Items_2-20_0.png", "326.png");
		map.put("speed bag_0-16_0.png", "2306.png");
		map.put("Items_5-17_0.png", "945.png");
		map.put("Items_0-2_0.png", "1269.png");
		map.put("Inmate 4_9-0_1.png", "1491.png");
		map.put("Toilet 2_1-16_2.png", "35.png");
		map.put("Guard_8-16_0.png", "492.png");
		map.put("Items_5-3_0.png", "775.png");
		map.put("Items_7-18_0.png", "1572.png");
		map.put("Items_2-19_0.png", "658.png");
		map.put("Items_0-11_0.png", "2130.png");
		map.put("Items_7-5_0.png", "1541.png");
		map.put("Outfit - Guard_6-8_0.png", "521.png");
		map.put("Items_2-12_0.png", "857.png");
		map.put("dust_4-16_0.png", "431.png");
		map.put("TV_0-0_0.png", "683.png");
		map.put("Outfit - Inmate_6-8_0.png", "789.png");
		map.put("Items_6-0_0.png", "1242.png");
		map.put("Inmate 2_0-0_0.png", "414.png");
		map.put("Extra NPCs_1-16_1.png", "1335.png");
		map.put("Items_1-27_0.png", "658.png");
		map.put("fx_5-0_1.png", "272.png");
		map.put("Items_2-26_0.png", "641.png");
		map.put("Items_5-2_0.png", "852.png");
		map.put("Outfit - Guard_8-16_1.png", "1271.png");
		map.put("Bed - Player_2-8_0.png", "466.png");
		map.put("Items_6-7_0.png", "611.png");
		map.put("Vent Cover_1-0_0.png", "141.png");
		map.put("Extra NPCs_0-0_0.png", "1325.png");
		map.put("punch bag 3_0-16_0.png", "2332.png");
		map.put("Inmate_11-8_0.png", "584.png");
		map.put("Items_7-24_0.png", "2174.png");
		map.put("Inmate 4_6-24_0.png", "1016.png");
		map.put("Kitchen - Sink_0-0_0.png", "445.png");
		map.put("Inmate 4_11-16_0.png", "539.png");
		map.put("Inmate_0-8_0.png", "543.png");
		map.put("Items_2-13_0.png", "927.png");
		map.put("Items_0-3_0.png", "279.png");
		map.put("Items_5-23_0.png", "398.png");
		map.put("Desks_0-8_0.png", "162.png");
		map.put("Extra NPCs_0-24_1.png", "1327.png");
		map.put("Items_6-6_0.png", "118.png");
		map.put("Extra NPCs_0-16_0.png", "1328.png");
		map.put("Items_0-12_0.png", "330.png");
		map.put("Inmate 3_10-0_1.png", "580.png");
		map.put("Washing 2_5-0_1.png", "871.png");
		map.put("Inmate 2_8-16_1.png", "655.png");
		map.put("Inmate_10-0_1.png", "542.png");
		map.put("Items_7-4_0.png", "1550.png");
		map.put("fx_4-0_0.png", "364.png");
		map.put("Outfit - Inmate_11-24_1.png", "195.png");
		map.put("Items_5-18_0.png", "324.png");
		map.put("Items_1-28_0.png", "685.png");
		map.put("Bed - NPC_1-24_0.png", "896.png");
		map.put("Container - MEDICAL_0-0_0.png", "930.png");
		map.put("Inmate 2_10-0_1.png", "382.png");
		map.put("Container - Gardening_0-0_0.png", "1441.png");
		map.put("587.png", "587.png");
		map.put("Outfit - Inmate_10-0_2.png", "249.png");
		map.put("Items_5-12_0.png", "95.png");
		map.put("Inmate 4_0-8_1.png", "563.png");
		map.put("Items_2-25_0.png", "658.png");
		map.put("Inmate 2_11-8_1.png", "1091.png");
		map.put("CAMERA_0-30_0.png", "891.png");
		map.put("Items_6-5_0.png", "337.png");
		map.put("Inmate_9-0_1.png", "1499.png");
		map.put("Items_2-14_0.png", "455.png");
		map.put("Inmate 2_10-0_0.png", "378.png");
		map.put("Items_1-4_0.png", "192.png");
		map.put("Dirt_1-0_1.png", "603.png");
		map.put("fx_4-3_3.png", "928.png");
		map.put("Items_2-17_0.png", "54.png");
		map.put("Items_7-29_0.png", "1251.png");
		map.put("Items_2-21_0.png", "338.png");
		map.put("Items_2-24_0.png", "490.png");
		map.put("Inmate 3_0-0_1.png", "575.png");
		map.put("Dest Arrow_0-0_1.png", "1081.png");
		map.put("1439.png", "1439.png");
		map.put("Inmate_8-24_1.png", "209.png");
		map.put("Items_6-2_0.png", "306.png");
		map.put("fx_4-0_1.png", "339.png");
		map.put("Items_5-13_0.png", "936.png");
		map.put("Container - Timber_0-0_0.png", "874.png");
		map.put("Items_2-8_0.png", "869.png");
		map.put("Items_0-10_0.png", "288.png");
		map.put("Sheets + Posters_0-0_0.png", "41.png");
		map.put("Inmate_0-16_1.png", "572.png");
		map.put("Inmate 3_11-0_1.png", "1218.png");
		map.put("Items_1-29_0.png", "956.png");
		map.put("Items_7-26_0.png", "2177.png");
		map.put("Inmate 3_6-0_0.png", "803.png");
		map.put("dust_4-16_1.png", "432.png");
		map.put("Items_7-9_0.png", "1555.png");
		map.put("Items_1-7_0.png", "398.png");
		map.put("Outfit - Inmate_11-0_1.png", "385.png");
		map.put("Items_7-6_0.png", "1552.png");
		map.put("Washing 2_1-0_0.png", "912.png");
		map.put("Items_2-22_0.png", "341.png");
		map.put("Door - Utility_0-0_0.png", "1223.png");
		map.put("Truck_0-16_0.png", "1429.png");
		map.put("Inmate_0-8_1.png", "544.png");
		map.put("Truck_0-8_0.png", "1432.png");
		map.put("Items_7-8_0.png", "1554.png");
		map.put("Inmate 2_6-0_0.png", "167.png");
		map.put("Items_2-16_0.png", "31.png");
		map.put("Guard_8-0_0.png", "516.png");
		map.put("Items_0-1_0.png", "238.png");
		map.put("Items_7-27_0.png", "2246.png");
		map.put("Extra NPCs_0-24_0.png", "1326.png");
		map.put("Outfit - Guard_11-0_1.png", "1405.png");
		map.put("Dig Hole_0-16_0.png", "973.png");
		map.put("Outfit - Guard_0-8_0.png", "500.png");
		map.put("Extra NPCs_1-8_0.png", "1337.png");
		map.put("Inmate_8-16_0.png", "650.png");
		map.put("Inmate 2_8-16_0.png", "643.png");
		map.put("Items_1-6_0.png", "397.png");
		map.put("Items_5-16_0.png", "941.png");
		map.put("Inmate_10-0_0.png", "548.png");
		map.put("Inmate 3_0-0_0.png", "574.png");
		map.put("Dig Hole_0-17_0.png", "968.png");
		map.put("Inmate_11-8_1.png", "585.png");
		map.put("Items_0-0_0.png", "237.png");
		map.put("Inmate 4_11-24_0.png", "165.png");
		map.put("Items_5-20_0.png", "954.png");
		map.put("Items_1-5_0.png", "10.png");
		map.put("Items_6-3_0.png", "285.png");
		map.put("Items_5-15_0.png", "942.png");
		map.put("CAMERA_0-18_0.png", "816.png");
		map.put("Outfit - Inmate_11-24_0.png", "195.png");
		map.put("Inmate 2_0-0_1.png", "609.png");
		map.put("Items_7-28_0.png", "2302.png");
		map.put("Inmate_6-8_0.png", "1059.png");
		map.put("CAMERA_5-21_0.png", "1051.png");
		map.put("Inmate 2_0-16_1.png", "622.png");
		map.put("fx_4-0_2.png", "368.png");
		map.put("Table 2_0-0_0.png", "902.png");
		map.put("Items_7-22_0.png", "2165.png");
		map.put("Items_2-6_0.png", "455.png");
		map.put("Inmate 3_10-0_2.png", "581.png");
		map.put("Items_7-7_0.png", "1551.png");
		map.put("Items_5-21_0.png", "958.png");
		map.put("Washing 2_0-0_0.png", "910.png");
		map.put("Vent Cover_0-0_0.png", "131.png");
		map.put("Items_5-6_0.png", "933.png");
		map.put("Inmate 2_0-8_0.png", "638.png");
		map.put("Inmate_11-0_0.png", "1012.png");
		map.put("Inmate 4_0-16_0.png", "559.png");
		map.put("Inmate_0-0_1.png", "566.png");
		map.put("CAMERA_0-21_0.png", "818.png");
		map.put("fx_5-0_0.png", "273.png");
		map.put("Items_0-5_0.png", "324.png");
		map.put("Inmate 4_0-24_0.png", "560.png");
		map.put("Tower Guard_0-8_1.png", "472.png");
		map.put("Items_1-18_0.png", "654.png");
		map.put("Inmate_11-16_1.png", "1098.png");
		map.put("Outfit - Guard_6-16_0.png", "1418.png");
		map.put("Truck_0-24_0.png", "1433.png");
		map.put("Outfit - Inmate_0-8_1.png", "525.png");
		map.put("Dirt_1-0_0.png", "774.png");
		map.put("Inmate 3_11-0_0.png", "1188.png");
		map.put("Guard_0-0_0.png", "496.png");
		map.put("Items_1-9_0.png", "116.png");
		map.put("Items_1-2_0.png", "117.png");
		map.put("Inmate 2_0-24_1.png", "624.png");
		map.put("Inmate 4_6-8_0.png", "1061.png");
		map.put("Canteen - Serving_0-0_2.png", "904.png");
		map.put("Guard_8-16_1.png", "644.png");
		map.put("soil_0-0_0.png", "235.png");
		map.put("Marker - Vendor_0-0_0.png", "259.png");
		map.put("Outfit - Guard_0-8_1.png", "501.png");
		map.put("Dig Hole_0-0_0.png", "16.png");
		map.put("dust_4-0_4.png", "426.png");
		map.put("Outfit - Guard_11-8_0.png", "1402.png");
		map.put("Inmate_8-16_1.png", "651.png");
		map.put("POWER GEN_0-0_0.png", "978.png");
		map.put("Items_7-14_0.png", "1585.png");
		map.put("Items_2-1_0.png", "699.png");
		map.put("Guard_6-0_0.png", "510.png");
		map.put("fx_4-3_1.png", "951.png");
		map.put("Gym 4_0-0_0.png", "880.png");
		map.put("Toilet 2_5-0_0.png", "762.png");
		map.put("Outfit - Inmate_0-24_0.png", "555.png");
		map.put("Inmate 4_11-8_0.png", "742.png");
		map.put("Items_5-1_0.png", "843.png");
		map.put("Dropped Sheet_4-0_0.png", "2336.png");
		map.put("Outfit - Inmate_11-16_0.png", "462.png");
		map.put("Outfit - Inmate_6-16_0.png", "790.png");
		map.put("Desks_0-24_0.png", "879.png");
		map.put("Toilet 2_1-0_2.png", "33.png");
		map.put("Inmate 3_9-0_0.png", "1019.png");
		map.put("Items_2-30_0.png", "784.png");
		map.put("Inmate 3_8-16_1.png", "653.png");
		map.put("CAMERA_2-24_1.png", "1055.png");
		map.put("Oven 2_0-0_0.png", "909.png");
		map.put("Bed - Solitary_0-24_0.png", "674.png");
		map.put("Guard_8-24_1.png", "703.png");
		map.put("Container - Tailor OUT_0-0_0.png", "1475.png");
		map.put("CAMERA_5-18_0.png", "1075.png");
		map.put("Extra NPCs_0-8_0.png", "1331.png");
		map.put("Items_2-27_0.png", "785.png");
		map.put("Items_7-0_0.png", "1533.png");
		map.put("Items_6-13_0.png", "1479.png");
		map.put("Container - Tailor IN_0-0_0.png", "1476.png");
		map.put("Metal Detector_0-24_0.png", "1049.png");
		map.put("Inmate 3_6-24_0.png", "1071.png");
		map.put("Inmate_0-0_0.png", "570.png");
		map.put("Items_7-19_0.png", "1573.png");
		map.put("Outfit - Guard_6-24_0.png", "518.png");
		map.put("Items_5-4_0.png", "876.png");
		map.put("Inmate 2_0-16_0.png", "363.png");
		map.put("Items_2-11_0.png", "922.png");
		map.put("Skipping Mat_0-16_0.png", "2187.png");
		map.put("Bed - Player_1-8_0.png", "154.png");
		map.put("Extra NPCs_1-0_0.png", "1320.png");
		map.put("CAMERA_2-30_1.png", "1057.png");
		map.put("Items_0-23_0.png", "937.png");
		map.put("Jogging Mat_0-0_0.png", "1293.png");
		map.put("Items_0-13_0.png", "405.png");
		map.put("Toilet 2_0-16_0.png", "85.png");
		map.put("Outfit - Guard_8-0_0.png", "480.png");
		map.put("Metalshop_0-0_0.png", "923.png");
		map.put("Outfit - Inmate_0-8_0.png", "526.png");
		map.put("Extra NPCs_2-8_1.png", "142.png");
		map.put("Items_5-28_0.png", "1003.png");
		map.put("Items_5-10_0.png", "939.png");
		map.put("Inmate_11-0_1.png", "483.png");
		map.put("Inmate_11-24_1.png", "251.png");
		map.put("Items_6-8_0.png", "766.png");
		map.put("Inmate 2_6-8_0.png", "793.png");
		map.put("Inmate 4_0-0_1.png", "558.png");
		map.put("Inmate 2_0-8_1.png", "639.png");
		map.put("Items_1-26_0.png", "1235.png");
		map.put("Inmate_11-16_0.png", "1017.png");
		map.put("Tower Guard_0-8_0.png", "1000.png");
		map.put("111.png", "111.png");
		map.put("Items_6-29_0.png", "1222.png");
		map.put("Inmate 4_0-24_1.png", "562.png");
		map.put("Items_6-11_0.png", "863.png");
		map.put("Items_2-29_0.png", "917.png");
		map.put("dust_4-0_5.png", "427.png");
		map.put("POWER GEN_0-0_1.png", "1042.png");
		map.put("473.png", "473.png");
		map.put("Items_1-30_0.png", "692.png");
		map.put("Inmate 3_9-0_1.png", "1488.png");
		map.put("Items_0-21_0.png", "4.png");
		map.put("Inmate 4_11-16_1.png", "538.png");
		map.put("Items_7-2_0.png", "1536.png");
		map.put("Inmate 2_11-16_1.png", "1082.png");
		map.put("Items_2-4_0.png", "256.png");
		map.put("30.png", "30.png");
		map.put("Door - Outside_0-0_0.png", "1225.png");
		map.put("Bed -guard_0-24_0.png", "137.png");
		map.put("Outfit - Guard_11-8_1.png", "1401.png");
		map.put("Canteen - Serving_0-0_3.png", "905.png");
		map.put("Items_5-26_0.png", "828.png");
		map.put("Library 2_0-0_0.png", "878.png");
		map.put("Inmate 4_0-8_0.png", "561.png");
		map.put("Extra NPCs_0-0_1.png", "1329.png");
		map.put("CAMERA_2-27_1.png", "1056.png");
		map.put("Items_7-16_0.png", "1571.png");
		map.put("Payphone_0-0_0.png", "1388.png");
		map.put("Items_0-15_0.png", "246.png");
		map.put("Items_1-24_0.png", "2374.png");
		map.put("Items_2-3_0.png", "139.png");
		map.put("Inmate 3_8-16_0.png", "652.png");
		map.put("Inmate 3_8-24_1.png", "224.png");
		map.put("Inmate_6-16_0.png", "1060.png");
		map.put("Inmate 2_0-24_0.png", "637.png");
		map.put("Active 9_0-0_0.png", "964.png");
		map.put("fx_4-3_0.png", "289.png");
		map.put("Metal Detector_0-0_0.png", "44.png");
		map.put("Inmate_10-0_2.png", "577.png");
		map.put("Extra NPCs_0-16_1.png", "1330.png");
		map.put("Outfit - Inmate_6-24_0.png", "530.png");
		map.put("Outfit - Inmate_0-0_1.png", "567.png");
		map.put("Items_0-7_0.png", "252.png");
		map.put("Ladders_0-24_0.png", "184.png");
		map.put("Items_1-15_0.png", "533.png");
		map.put("Dig Hole_0-2_0.png", "17.png");
		map.put("dust_4-16_4.png", "435.png");
		map.put("Checkpoint - Guard_0-8_0.png", "2113.png");
		map.put("CAMERA_2-21_1.png", "1053.png");
		map.put("Tower Guard_0-0_1.png", "1044.png");
		map.put("punch bag 2_0-16_0.png", "1424.png");
		map.put("Items_7-12_0.png", "1553.png");
		map.put("Outfit - Inmate_0-24_1.png", "549.png");
		map.put("Inmate 3_11-24_1.png", "104.png");
		map.put("Guard_6-8_0.png", "513.png");
		map.put("Items_1-22_0.png", "329.png");
		map.put("Items_0-17_0.png", "400.png");
		map.put("container - mail_0-0_0.png", "1481.png");
		map.put("Outfit - Guard_11-16_0.png", "1403.png");
		map.put("Shower_0-0_1.png", "899.png");
		map.put("Inmate_11-24_0.png", "320.png");
		map.put("Inmate_6-0_0.png", "1058.png");
		map.put("Outfit - Guard_0-24_1.png", "783.png");
		map.put("Items_0-8_0.png", "328.png");
		map.put("Items_0-25_0.png", "14.png");
		map.put("Outfit - Guard_11-24_1.png", "1400.png");
		map.put("Extra NPCs_1-0_1.png", "1324.png");
		map.put("Items_6-22_0.png", "2392.png");
		map.put("punch bag 3_1-0_1.png", "2334.png");
		map.put("Items_6-14_0.png", "1239.png");
		map.put("Outfit - Guard_0-0_1.png", "499.png");
		map.put("Outfit - Guard_0-16_0.png", "497.png");
		map.put("Extra NPCs_2-24_0.png", "133.png");
		map.put("Tower Guard_0-16_1.png", "1300.png");
		map.put("Items_7-20_0.png", "1589.png");
		map.put("Inmate 3_8-0_0.png", "705.png");
		map.put("Items_5-29_0.png", "1080.png");
		map.put("Items_6-21_0.png", "27.png");
		map.put("POWER GEN_2-0_1.png", "1006.png");
		map.put("Outfit - Inmate_0-16_1.png", "569.png");
		map.put("Items_6-28_0.png", "1443.png");
		map.put("Brace_0-0_0.png", "932.png");
		map.put("Inmate 3_11-8_0.png", "588.png");
		map.put("Outfit - Inmate_11-8_1.png", "739.png");
		map.put("Items_1-10_0.png", "403.png");
		map.put("Guard_0-0_1.png", "503.png");
		map.put("Stash_0-0_2.png", "1577.png");
		map.put("Inmate 2_8-0_0.png", "778.png");
		map.put("Items_5-9_0.png", "934.png");
		map.put("Tower Guard_0-24_1.png", "11.png");
		map.put("Items_0-18_0.png", "221.png");
		map.put("Items_6-15_0.png", "1231.png");
		map.put("1456.png", "1456.png");
		map.put("Inmate 2_11-16_0.png", "248.png");
		map.put("Inmate 3_11-16_0.png", "1219.png");
		map.put("Washing 2_1-0_3.png", "920.png");
		map.put("Bed - Medical_0-24_0.png", "897.png");
		map.put("Truck_0-0_0.png", "1431.png");
		map.put("Inmate 2_11-24_1.png", "732.png");
		map.put("Dig Hole_0-1_0.png", "2.png");
		map.put("Oven 2_5-0_1.png", "872.png");
		map.put("Items_6-27_0.png", "1094.png");
		map.put("Door - Staff_0-0_0.png", "1224.png");
		map.put("Inmate 2_9-0_1.png", "1504.png");
		map.put("Items_1-21_0.png", "657.png");
		map.put("Canteen - Serving_0-0_0.png", "855.png");
		map.put("Items_7-13_0.png", "1558.png");
		map.put("dust_4-0_2.png", "424.png");
		map.put("Dirt_0-0_0.png", "78.png");
		map.put("Items_0-24_0.png", "13.png");
		map.put("Inmate_6-24_0.png", "1062.png");
		map.put("Items_5-8_0.png", "875.png");
		map.put("Checkpoint_0-0_0.png", "1794.png");
		map.put("Items_1-16_0.png", "514.png");
		map.put("Items_6-26_0.png", "1097.png");
		map.put("Items_0-26_0.png", "29.png");
		map.put("Items_6-16_0.png", "734.png");
		map.put("Container - Deliveries 2_0-0_0.png", "1478.png");
		map.put("2178.png", "2178.png");
		map.put("Items_6-23_0.png", "914.png");
		map.put("Shower_0-0_0.png", "898.png");
		map.put("Active 10_0-0_0.png", "352.png");
		map.put("CAMERA_2-18_1.png", "1038.png");
		map.put("Inmate 3_0-24_0.png", "550.png");
		map.put("Outfit - Inmate_8-0_0.png", "782.png");
		map.put("Items_0-30_0.png", "108.png");
		map.put("Guard_6-24_0.png", "512.png");
		map.put("Items_7-11_0.png", "1557.png");
		map.put("Inmate 4_0-16_1.png", "557.png");
		map.put("Container - Laundry - Dirty_0-0_0.png", "893.png");
		map.put("Items_1-14_0.png", "515.png");
		map.put("Interwebs_0-0_0.png", "1290.png");
		map.put("Extra NPCs_2-16_0.png", "7.png");
		map.put("Items_0-29_0.png", "105.png");
		map.put("Toilet 2_5-16_0.png", "759.png");
		map.put("Items_1-11_0.png", "1233.png");
		map.put("Outfit - Guard_0-24_0.png", "502.png");
		map.put("Extra NPCs_2-24_1.png", "129.png");
		map.put("Light_0-0_0.png", "944.png");
		map.put("Outfit - Guard_11-24_0.png", "1399.png");
		map.put("Items_6-18_0.png", "305.png");
		map.put("Items_7-10_0.png", "1556.png");
		map.put("Oven 2_1-0_0.png", "911.png");
		map.put("Ladders_0-8_0.png", "647.png");
		map.put("Bed - NPC_2-24_0.png", "726.png");
		map.put("Items_0-31_0.png", "1079.png");
		map.put("Stash_0-0_3.png", "1575.png");
		map.put("Dig Hole_0-4_0.png", "23.png");
		map.put("Outfit - Inmate_0-16_0.png", "568.png");
		map.put("Door - Woodshop_0-0_0.png", "1221.png");
		map.put("Inmate 3_11-16_1.png", "1220.png");
		map.put("Items_0-28_0.png", "947.png");
		map.put("Items_6-25_0.png", "1096.png");
		map.put("Bed - Player_0-8_0.png", "144.png");
		map.put("660.png", "660.png");
		map.put("Outfit - Inmate_11-8_0.png", "741.png");
		map.put("POWER GEN_2-0_0.png", "799.png");
		map.put("punch bag 3_1-0_0.png", "2333.png");
		map.put("License Prints_1-0_0.png", "926.png");
		map.put("Inmate 3_6-16_0.png", "804.png");
		map.put("Extra NPCs_2-0_0.png", "45.png");
		map.put("Items_6-30_0.png", "1511.png");
		map.put("Bed - Lounger_0-24_0.png", "1480.png");
		map.put("Guard_0-24_0.png", "507.png");
		map.put("Items_0-27_0.png", "948.png");
		map.put("Container - Kitchen_0-0_0.png", "890.png");
		map.put("Metal Detector_5-24_1.png", "1050.png");
		map.put("dust_4-0_3.png", "425.png");
		map.put("Inmate 4_8-0_0.png", "614.png");
		map.put("Dig Hole_0-3_0.png", "20.png");
		map.put("Inmate 4_11-8_1.png", "743.png");
		map.put("Guard_6-16_0.png", "511.png");
		map.put("fx_5-0_4.png", "260.png");
		map.put("Door - Master_0-0_0.png", "1267.png");
		map.put("Items_6-24_0.png", "640.png");
		map.put("punch bag 2_1-0_1.png", "2326.png");
		map.put("Items_1-13_0.png", "477.png");
		map.put("Inmate 4_10-0_0.png", "595.png");
		map.put("Inmate 2_11-24_0.png", "605.png");
		map.put("Items_6-19_0.png", "365.png");
		map.put("Hiding_0-8_0.png", "868.png");
		map.put("Canteen - Serving_0-0_1.png", "903.png");
		map.put("Container - Deliveries_0-0_0.png", "1430.png");
		map.put("Inmate 4_8-24_1.png", "202.png");
		map.put("Inmate 4_8-16_0.png", "608.png");
	}

	// utils

	static void writePng(BufferedImage img, Path path) throws IOException {
		if (!force && Files.exists(path)) {
			return;
		}
		// TODO optimize size
		ImageIO.write(img, "png", path.toFile());
	}
	
	static Path getImagePath(String name) {
		for (Path dir : unsortedImagesDirs) {
			Path path = decompiledDir.resolve(dir).resolve(name);
			if (Files.exists(path)) {
				return path;
			}
		}

		Path path = decompiledDir.resolve("Images").resolve(name);
		if (Files.exists(path)) {
			return path;
		}

		if (map.containsKey(name)) {
			path = decompiledDir.resolve("Images").resolve(map.get(name));
			if (Files.exists(path)) {
				return path;
			}
		}
		
		System.out.println(name + " missing!");
		return null;
	}

	static int run(String[] cmd) throws Exception {
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.inheritIO();
		return pb.start().waitFor();
	}

}
