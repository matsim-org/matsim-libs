package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.List;

import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.VelocityObstacle;

import com.vividsolutions.jts.geom.Coordinate;

public abstract class AlternativeVelocityChooser {
	protected final double timeHorizont = 10;


	public abstract void chooseAlterantiveVelocity(List<VelocityObstacle> vOs, Coordinate c0, Coordinate c1, double[] df, Agent2D agent);

	protected double timeToCollision(List<VelocityObstacle> vOs, Coordinate c0,
			Coordinate c1) {
		double ret = Double.POSITIVE_INFINITY;
		for (VelocityObstacle  info : vOs) {
			double tmpTime = Double.POSITIVE_INFINITY;
			if (Algorithms.testForCollision(info, c1)) {
				if (info.getCollTime() == 0) {
					return 0;
				}
				tmpTime = computeCollisionTime(info,c0,c1);
			}
			if (tmpTime < ret) {
				ret = tmpTime;
			}
		}

		return ret;
	}

	private double computeCollisionTime(VelocityObstacle info, Coordinate c0,
			Coordinate c1) {
		double relVx = ((c1.x - c0.x) - info.getvBx());
		double relVy = ((c1.y - c0.y) - info.getvBy());
		double dx = relVx*this.timeHorizont;
		double dy = relVy*this.timeHorizont;
		Coordinate tmp = new Coordinate(c0.x+dx, c0.y + dy);
		
		
//		DEBUG
//				GisDebugger.addGeometry(ls, "ray");

		double ret = this.timeHorizont;

		Coordinate intersectionCoordinate = new Coordinate();
		for (int i = 0; i < info.getCso().length-1; i++) {
			//			this.li.computeIntersection(c0, tmp, info.cso[i], info.cso[i+1]);

			if (Algorithms.computeLineIntersection(c0, tmp, info.getCso()[i], info.getCso()[i+1], intersectionCoordinate)) {
				double dist = intersectionCoordinate.distance(c0);
				double time = dist /(Math.sqrt(relVx*relVx+relVy*relVy));
				if (time < ret) {
					ret = time;
				}
//				//DEBUG
//								LineString ls2 = geofac.createLineString(new Coordinate[]{c0,new Coordinate(intersectionCoordinate)});
//								GisDebugger.addGeometry(ls2, "ray:" + time);
			}
		}

		return ret;
	}

}
