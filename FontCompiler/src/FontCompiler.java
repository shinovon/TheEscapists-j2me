/*
Copyright (c) 2022-2025 Arman Jussupgaliyev
*/
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

public class FontCompiler {

	public static void main(String[] args) {
		File chars = new File("./font.txt");
		int cols = 16;
		int rows = 6;
		int ver = 1;
		convert(new File("./font.png"), chars, new File("../res/fontr"), 7, 10, cols, rows, ver, "TE-R");
		convert(new File("./fontbold.png"), chars, new File("../res/fontb"), 8, 10, cols, rows, ver, "TE-B");

	}

	private static void convert(File bitmap, File chars, File out, int cw, int ch, int cols, int rows, int ver, String name) {
		try {
			if(out.exists())
				out.delete();
			DataOutputStream d = new DataOutputStream(new FileOutputStream(out));
			//magic
			d.writeInt(0xDE11CFAB);
			//name
			writeName(d, name);
			//version
			d.writeByte(ver);
			//char width
			d.writeByte(cw);
			//char height
			d.writeByte(ch);
			//columns
			d.writeByte(cols);
			//rows
			d.writeByte(rows);
			//chars read
			int cl = (int)chars.length();
			FileInputStream cis = new FileInputStream(chars);
			byte[] cb = new byte[cl];
			cis.read(cb);
			cis.close();
			//chars length
			d.writeInt(cl);
			//chars bytes
			d.write(cb);
			cis.close();
			//bitmap read
			int bl = (int)bitmap.length();
			FileInputStream bis = new FileInputStream(bitmap);
			byte[] bb = new byte[bl];
			bis.read(bb);
			bis.close();
//			//bitmap length
//			d.writeInt(bl);
//			//bitmap bytes
//			d.write(bb);


			//calc widths
			BufferedImage bmp = ImageIO.read(bitmap);

			// write 1-bit map
			for (int y = 0; y < rows; y++) {
				for (int x = 0; x < cols; x++) {
					for (int i = 0; i < ch; i++) {
						for (int j = 0; j < cw; j++) {
							int r = bmp.getRGB(j+x*cw, i+y*ch);
							d.writeByte((byte) ((r >> 24) & 0xFF));
						}
					}
				}
			}

			int[][] widths = new int[cols][rows];
			for(int y = 0; y<rows; y++) {
				for(int x = 0; x<cols; x++) {
					int iw = 0;
					for(int i = 0; i < ch; i++) {
						for(int j = 0; j < cw; j++) {
							int r = bmp.getRGB(j+x*cw, i+y*ch);
							if((r |= 0x00FFFFFF) != 0x00FFFFFF && j > iw) {
								iw = j;
							}
						}
					}
					widths[x][y] = iw+1;
				}
			}
			//widths arr
			for(int j = 0; j < rows; j++) {
				for(int i = 0; i < cols; i++) {
					d.writeByte(widths[i][j]);
				}
			}
			// end
			d.flush();
			d.close();

			System.out.println("Wrote " + out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeName(DataOutputStream d, String s) throws IOException {
		if(s.length() > 8) s = s.substring(0, 8);
		char[] c = new char[8];
		for(int i = 0; i < s.length(); i++) c[i] = s.charAt(i);
		for(int i = 0; i < 8; i++) {
			int j = c[i];
			if(j < 0 || j > 255) j = 0;
			d.writeByte(j);
		}
	}

}
