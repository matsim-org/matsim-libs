package playground.mmoyo.PTRouter;

import playground.mmoyo.PTRouter.PTLine;

public class PTVehicle {
	
	private PTLine ptline;
	private int capacity;
	private boolean hasDedicatedTracks;
	
	/**
	 * TODO: check class vehicle of MATSIM
	 */
	public PTVehicle() {
		super();
	}

	public PTLine getPtline() {
		return ptline;
	}

	public void setPtline(PTLine ptline) {
		this.ptline = ptline;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public boolean isHasDedicatedTracks() {
		return hasDedicatedTracks;
	}

	public void setHasDedicatedTracks(boolean hasDedicatedTracks) {
		this.hasDedicatedTracks = hasDedicatedTracks;
	}
}
