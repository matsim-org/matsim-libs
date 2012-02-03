package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle;

import com.vividsolutions.jts.geom.Coordinate;

public interface VelocityObstacle {

	public abstract Coordinate[] getVo();

	public abstract double getCollTime();
	

}