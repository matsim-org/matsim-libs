package playground.gregor.otf;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureData;

public class Tile implements Comparable<Tile> {

	public static final int LENGTH = 256;
	
	int zoomLevel;
	TextureData tx;
	Texture tex;
	
	public float tX;
	public float tY;
//	public float bX;
//	public float bY;
//	public float cX;
//	public float cY;

	public float sX;

	public float sY;

	double zoom;

	double time;
	public String id;

	public int x;

	public int y;

	public double key;

	public int compareTo(Tile o) {
		if (this.time > o.time) {
			return -1;
		} else if (this.time < o.time){
			return 1;
		}
		return 0;
	}
	
}
