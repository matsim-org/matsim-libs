package playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.velocityobstacle;

import com.vividsolutions.jts.geom.Coordinate;

public class CircularVelocityObstacle implements VelocityObstacle {
	private double vBx;
	private double vBy;
	private Coordinate csoC;
	private double csoR;
	private Coordinate[] vo;
	private double collTime = Double.POSITIVE_INFINITY; //TODO replace this by a boolean that tells if two object have collided
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
	public Coordinate getCsoC() {
		return this.csoC;
	}
	public double getCsoR(){
		return this.csoR;
	}
	public void setCso(Coordinate csoC, double csoR) {
		this.csoC = csoC;
		this.csoR = csoR;
	}
	@Override
	public Coordinate[] getVo() {
		return this.vo;
	}
	public void setVo(Coordinate[] vo) {
		this.vo = vo;
	}
	
	public void setCollTime(double collTime) {
		this.collTime = collTime;
	}
	
	@Override
	public double getCollTime() {
		return this.collTime;
	}
}
