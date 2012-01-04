package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle;

import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

public class VelocityObstacle {
	private double collTime = Double.POSITIVE_INFINITY;
	private double vBx;
	private double vBy;
	private Coordinate[] cso;
	private Coordinate[] vo;
	public double getCollTime() {
		return this.collTime;
	}
	public void setCollTime(double collTime) {
		this.collTime = collTime;
	}
	public double getvBx() {
		return this.vBx;
	}
	public void setvBx(double vBx) {
		this.vBx = vBx;
	}
	public double getvBy() {
		return this.vBy;
	}
	public void setvBy(double vBy) {
		this.vBy = vBy;
	}
	public Coordinate[] getCso() {
		return this.cso;
	}
	public void setCso(Coordinate[] cso) {
		this.cso = cso;
	}
	public Coordinate[] getVo() {
		return this.vo;
	}
	public void setVo(Coordinate[] vo) {
		this.vo = vo;
	}


}
