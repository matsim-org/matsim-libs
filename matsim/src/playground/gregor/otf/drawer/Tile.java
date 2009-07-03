package playground.gregor.otf.drawer;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureData;

public class Tile implements Comparable<Tile> {

	public static final int LENGTH = 256;
	
//	int zoomLevel;
	private TextureData tx;
	private Texture tex;
	
	public float tX;
	public float tY;

	public float sX;

	public float sY;

	double zoom;

	private double time;
	public String id;

	public int x;

	public int y;

	public double key;

	public int compareTo(Tile o) {
		if (this.getTime() > o.getTime()) {
			return -1;
		} else if (this.getTime() < o.getTime()){
			return 1;
		}
		return 0;
	}

	public void setTx(TextureData tx) {
		this.tx = tx;
	}

	public TextureData getTx() {
		return tx;
	}

	public void setTex(Texture tex) {
		this.tex = tex;
	}

	public Texture getTex() {
		return tex;
	}

	public void setTime(double time) {
		this.time = time;
	}

	public double getTime() {
		return time;
	}
	
}
