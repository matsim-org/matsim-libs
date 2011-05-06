package playground.gregor.sim2d_v2.calibration_v2;


import org.matsim.api.core.v01.Id;

import playground.gregor.sim2d_v2.simulation.floor.Agent2D;

public class PhantomAgent2D extends Agent2D {

	private double lastUpdate;

	private Id currentLinkId = null;

	private final Id id;
	public PhantomAgent2D(Id id) {
		super(null);
		this.id = id;
	}

	public void setUpdateTime(double time) {
		this.lastUpdate = time;
	}
	public double getLastUpdate() {
		return this.lastUpdate;
	}

	@Override
	public Id getCurrentLinkId() {
		return this.currentLinkId;
	}

	public void setCurrentLinkId(Id id) {
		this.currentLinkId = id;
	}

	@Override
	public Id getId() {
		return this.id;
	}


}
