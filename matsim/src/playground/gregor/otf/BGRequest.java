package playground.gregor.otf;

import java.awt.geom.Rectangle2D.Float;

import playground.gregor.otf.BackgroundFromStreamDrawer.ZoomLevel;

import com.sun.opengl.util.texture.TextureData;

public class BGRequest implements Comparable<BGRequest>{

	private final BackgroundFromStreamDrawer bgs;

	private boolean locked = false;

	private Float koords;

	private TextureData t;
	
	public enum State { open, processed, obsolete };
	private State state;

	private final ZoomLevel z;
	
	public BGRequest(BackgroundFromStreamDrawer backgroundFromStreamDrawer, ZoomLevel z) {
		this.bgs = backgroundFromStreamDrawer;
		this.z = z;
	}
	
	public BackgroundFromStreamDrawer getBGS() {
		return this.bgs;
	}

	public synchronized boolean getLock() {
		if (!this.locked) {
			this.locked = true;
			return true;
		}
		return false;
		
	}
	
	public void unLock() {
		this.locked = false;
	}
	
	public int compareTo(BGRequest o) {
		if (this.bgs.getDist()*this.bgs.getHight() < o.getBGS().getDist()*o.getBGS().getHight()) {
			return -1;
		}
		
		if (this.bgs.getDist()*this.bgs.getHight() > o.getBGS().getDist()*o.getBGS().getHight()) {
			return 1;
		}
		return 0;
	}

	public void response(TextureData t, Float koords) {
		this.t = t;
		this.koords = koords;
		
	}
	
	public State getState() {
		return this.state;
	}
	
	public void setState(State state) {
		this.state = state;
	}

	public ZoomLevel getZoomLevel() {
		return this.z;
	}

	public TextureData getTxData() {
		return this.t;
	}
	
	public Float getKoords() {
		return this.koords;
	}

}
