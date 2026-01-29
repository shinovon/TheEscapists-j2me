/*
Copyright (c) 2026 Arman Jussupgaliyev
*/
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import javax.imageio.ImageIO;

public class ResourceBuilder implements Constants {
	
	static Path decompiledDir;
	static Path[] unsortedImagesDirs;
	static Path gameDir;
	static Path resDir;
	
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Parameters: <output res dir> <decompiled dir> <game dir> <map>");
			return;
		}

		resDir = Paths.get(args[0]);
		decompiledDir = Paths.get(args[1]);
		gameDir = Paths.get(args.length >= 3 ? args[2] : "C:\\Program Files (x86)\\Steam\\steamapps\\common\\The Escapists");
		unsortedImagesDirs = new Path[] {
				Paths.get("Sorted Images", "[6] game", "[UNSORTED]"),
				Paths.get("Sorted Images", "[11] tutorial", "[UNSORTED]"),
		};
		
		String map = args.length >= 4 ? args[3] : "shanktonstatepen";
		
		try {
			items();
			objects();
			light();
			huds();
			title();
			icon();
			ground("ground_" + map + ".gif");
			tiles("tiles_" + map + ".gif", "tiles_ea.png");
			character("Inmate 4", "inmate4.png", 4);
			character("Guard", "guard.png", 2);
			character("Outfit - Inmate", "outfit0.png", 4);
			character("Outfit - Guard", "outfit1.png", 4);
			character("Extra NPCs_0", "warden.png", 1);
			character("Extra NPCs_1", "jobstaff.png", 1);
			character("Extra NPCs_2", "doctor.png", 1);
			character("Tower Guard_0", "sniper.png", 1);
			map(map + ".map");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static void items() throws IOException {
		final String[] names = {
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
		for (int i = 0; i < names.length; ++i) {
			Path path = getImagePath(names[i]);
			if (path == null) {
				continue;
			}
			BufferedImage tile = ImageIO.read(path.toFile());
			g.drawImage(tile, (i % 16) * TILE_SIZE, (i / 16) * TILE_SIZE, null);
		}
		
		ImageIO.write(img, "png", resDir.resolve("items.png").toFile());
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
				"Truck_0-16_0.png", 4, 12, // 3x2
				"Truck_0-0_0.png", 7, 12, // 3x2
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
		
		ImageIO.write(img, "png", resDir.resolve("objects.png").toFile());
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
				tile = tile.getSubimage(0, 16, 16, 16);
			} else if (h == 18) {
				tile = tile.getSubimage(0, 2, 16, 16);
			} else if (h < 16) {
				x = 3;
				y = 7;
			}
			g.drawImage(tile, (i % 8) * TILE_SIZE + x, (i / 8) * TILE_SIZE + y, null);
		}
		
		ImageIO.write(img, "png", resDir.resolve(out).toFile());
	}
	
	static void ground(String name) throws IOException {
		Path dir = gameDir.resolve("Data").resolve("images");

		Path soilPath = dir.resolve("soil.gif");
		if (!Files.exists(soilPath)) {
			throw new IOException(soilPath.toString());
		}
		BufferedImage soil = ImageIO.read(soilPath.toFile());

		Path groundPath = dir.resolve(name);
		if (!Files.exists(groundPath)) {
			throw new IOException(groundPath.toString());
		}
		BufferedImage ground = ImageIO.read(groundPath.toFile());
		
		BufferedImage img = new BufferedImage(TILE_SIZE * 6, TILE_SIZE * 3, BufferedImage.TYPE_INT_RGB);
		Graphics g = img.getGraphics();
		g.drawImage(ground.getSubimage(0, 0, 48, 48), 0, 0, null);
		g.drawImage(soil.getSubimage(0, 0, 48, 48), TILE_SIZE * 3, 0, null);
		
		ImageIO.write(img, "png", resDir.resolve("ground.png").toFile());
	}
	
	static void tiles(String name, String out) throws IOException {
		Path dir = gameDir.resolve("Data").resolve("images");
		
		Path path = dir.resolve(name);
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
		ImageIO.write(img, "png", resDir.resolve(out).toFile());
	}
	
	static void light() throws IOException {
		BufferedImage img = new BufferedImage(TILE_SIZE * 4, TILE_SIZE * 4, BufferedImage.TYPE_INT_ARGB);
		img.getGraphics().drawImage(ImageIO.read(getImagePath("Light_0-0_0.png").toFile()), 2, 2, null);
		ImageIO.write(img, "png", resDir.resolve("light.png").toFile());
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
		
		ImageIO.write(img, "png", resDir.resolve("huds.png").toFile());
	}
	
	static void icon() throws IOException {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
		Graphics g = img.getGraphics();
		g.setColor(new Color(0x808080));
		g.fillRect(0, 0, 16, 16);
		g.drawImage(ImageIO.read(getImagePath("Inmate 4_0-24_0.png").toFile()), 0, 0, null);
		g.drawImage(ImageIO.read(getImagePath("Outfit - Inmate_0-24_0.png").toFile()), 0, 0, null);
		
		ImageIO.write(img, "png", resDir.resolve("icon.png").toFile());
	}
	
	static void title() throws IOException {
		Path titlePath = decompiledDir.resolve(Paths.get("Sorted Images", "[2] title_screen", "[UNSORTED]", "Active_0-0_0.png"));
		Files.copy(titlePath, resDir.resolve("title.png"), StandardCopyOption.REPLACE_EXISTING);
	}
	
	static void map(String name) throws IOException {
		Path path = gameDir.resolve("Data").resolve("Maps").resolve(name);
		Path temp = Files.createTempFile(null, null);
		try {
			Files.write(temp, BlowfishCompatEncryption.decrypt(Files.readAllBytes(path)));
			MapCompiler.main(new String[] { temp.toString(), resDir.resolve("map").toString()});
		} finally {
			Files.deleteIfExists(temp);
		}
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
		
		System.out.println(name + " missing!");
		return null;
	}

}
