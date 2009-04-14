package playground.gregor.otf;

public class BGRequest implements Comparable<BGRequest>{

	private final BackgroundFromStreamDrawer bgs;

	public BGRequest(BackgroundFromStreamDrawer backgroundFromStreamDrawer) {
		this.bgs = backgroundFromStreamDrawer;
	}
	
	public BackgroundFromStreamDrawer getBGS() {
		return this.bgs;
	}

	public int compareTo(BGRequest o) {
		if (this.bgs.getDist() < o.getBGS().getDist()) {
			return -1;
		}
		
		if (this.bgs.getDist() > o.getBGS().getDist()) {
			return 1;
		}
		return 0;
	}


}
