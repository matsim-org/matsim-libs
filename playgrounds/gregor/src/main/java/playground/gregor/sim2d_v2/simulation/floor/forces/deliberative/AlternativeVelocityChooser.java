package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.List;

import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.CircularVelocityObstacle;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.PolygonalVelocityObstacle;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.VelocityObstacle;

import com.vividsolutions.jts.geom.Coordinate;

public abstract class AlternativeVelocityChooser {
	protected final double timeHorizont = 2;

private static final double EPSILON = 0.000001;
	
	public abstract void chooseAlterantiveVelocity(List<? extends VelocityObstacle> vOs, Coordinate c0, Coordinate c1, double[] df, Agent2D agent);

	protected double timeToCollision(List<? extends VelocityObstacle> vOs, Coordinate c0,
			Coordinate c1) {
		double ret = Double.POSITIVE_INFINITY;
		for (VelocityObstacle  info : vOs) {
			double tmpTime = Double.POSITIVE_INFINITY;
			if (Algorithms.testForCollision(info, c1)) {
//				if (info.getCollTime() == 0) {
//					return 0;
//				}

				if (info instanceof PolygonalVelocityObstacle) {
					tmpTime = computeCollisionTime((PolygonalVelocityObstacle)info,c0,c1);
				} else {
					tmpTime = computeCollisionTime((CircularVelocityObstacle)info,c0,c1);
				}
			}
			if (tmpTime < ret) {
				ret = tmpTime;
			}
		}

		return ret;
	}

	private double computeCollisionTime(CircularVelocityObstacle info,
			Coordinate c0, Coordinate c1) {
		
//		//DEBUG
//		GisDebugger.addCircle(c0,PhysicalAgentRepresentation.AGENT_DIAMETER/2,"A");
//		Coordinate cc = info.getCsoC();
//		double r = PhysicalAgentRepresentation.AGENT_DIAMETER/2;
//		GisDebugger.addCircle(cc, r, "B");
//		
//		GisDebugger.addCircle(cc, info.getCsoR(), "CSO");
//		Coordinate [] vB = {info.getCsoC(), new Coordinate(cc.x + info.getvBx(), cc.y + info.getvBy())};
//		LineString ls1 = GisDebugger.geofac.createLineString(vB);
//		GisDebugger.addGeometry(ls1, "vB");
//		//DEBUG
		
		
		
		
		double relVx = ((c1.x - c0.x) - info.getvBx());
		double relVy = ((c1.y - c0.y) - info.getvBy());
		double dx = relVx*this.timeHorizont;
		double dy = relVy*this.timeHorizont;
		
//		//DEBUG
//		Coordinate[] relVA = {c0, new Coordinate(c0.x+relVx, c0.y + relVy)};
//		LineString ls2 = GisDebugger.geofac.createLineString(relVA);
//		GisDebugger.addGeometry(ls2, "relVA");
//		Coordinate[] vo = info.getVo();
//		Coordinate[] vvoo = {vo[1], vo[0], vo[2]};
//		LineString ls3 = GisDebugger.geofac.createLineString(vvoo);
//		GisDebugger.addGeometry(ls3, "VO");
//		
//		Coordinate[] projectedVx = {c0, new Coordinate(c0.x + dx, c0.y + dy)};
//		LineString ls4 = GisDebugger.geofac.createLineString(projectedVx);
//		GisDebugger.addGeometry(ls4, "projected");
//		//DEBUG
//		
		
//		Coordinate p2 = new Coordinate(c0.x+dx, c0.y + dy);
		
		double a = dx * dx + dy * dy; 
		double b = 2 * (dx * (c0.x - info.getCsoC().x) + dy * (c0.y - info.getCsoC().y));
		double c = info.getCsoC().x * info.getCsoC().x + info.getCsoC().y * info.getCsoC().y;
		c += c0.x * c0.x + c0.y * c0.y;
		c -= 2 * (info.getCsoC().x * c0.x + info.getCsoC().y * c0.y);
		c -= info.getCsoR() * info.getCsoR();
		double dd = b * b - 4 * a * c;
		
		if (Math.abs(a) < EPSILON || dd < 0) {
//			GisDebugger.dump("/Users/laemmel/tmp/tangents.shp");
//			throw new RuntimeException("no collision!!");
			return this.timeHorizont;
		}
		
		double dist = (-b + Math.sqrt(dd)) / (2 * a);
		double relV = Math.hypot(relVx, relVy);
		
		double time = dist / relV;
//		GisDebugger.dump("/Users/laemmel/tmp/tangents.shp");
//		if (time <2 && time > 1 && Math.hypot(info.getvBx(), info.getvBy()) > 1.3) {
//			System.out.println();
//		}
		return time;
	}

	private double computeCollisionTime(PolygonalVelocityObstacle info, Coordinate c0,
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
