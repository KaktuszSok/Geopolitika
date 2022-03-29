package kaktusz.geopolitika.util;

public class ColourUtils {
	public static int colourAsInt(int r, int g, int b) {
		r = (r << 16) & 0x00FF0000;
		g = (g << 8) & 0x0000FF00;
		b = b & 0x000000FF;

		return 0xFF000000 | r | g | b;
	}

	public static int colourAsInt(int r, int g, int b, int a) {
		a = (a << 24) & 0xFF000000;
		r = (r << 16) & 0x00FF0000;
		g = (g << 8) & 0x0000FF00;
		b = b & 0x000000FF;

		return a | r | g | b;
	}
}
