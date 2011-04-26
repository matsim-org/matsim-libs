package playground.gregor.sim2d_v2.calibration.simulation;


import playground.gregor.sim2d_v2.simulation.Agent2D;

public class PhantomAgent2D extends Agent2D {

	private double lastUpdate;

	public PhantomAgent2D() {
		super(null,null);
	}

	public void setUpdateTime(double time) {
		this.lastUpdate = time;
	}
	public double getLastUpdate() {
		return this.lastUpdate;
	}
}
