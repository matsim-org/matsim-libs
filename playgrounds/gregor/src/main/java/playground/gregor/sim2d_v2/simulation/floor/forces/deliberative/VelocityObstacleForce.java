package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


import com.vividsolutions.jts.algorithm.NonRobustLineIntersector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.quadtree.Quadtree;


//import playground.gregor.sim2d_v2.helper.gisdebug.GisDebugger;
import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalFloor;
import playground.gregor.sim2d_v2.simulation.floor.forces.DynamicForceModule;
import playground.gregor.sim2d_v2.simulation.floor.forces.Force;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.CCWPolygon;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.ConfigurationSpaceObstacle;

public class VelocityObstacleForce implements DynamicForceModule{

	NonRobustLineIntersector li = new NonRobustLineIntersector();

	private Quadtree agentsQuad;
	private final PhysicalFloor floor;

	private final double quadUpdateInterval = 0.1;
	private double lastQuadUpdate = Double.NEGATIVE_INFINITY;

	private final PathAndDrivingAcceleration driver;

	private final double deltaT = 1/25.;

	private final double timeHorizont = 10;

	//Laemmel constant
	private static final double neighborhoodSensingRange = 5;

	public VelocityObstacleForce(PhysicalFloor floor) {
		this.floor = floor;
		this.driver = new PathAndDrivingAcceleration(floor);
	}


	@Override
	public void run(Agent2D agent) {

		double minX = agent.getPosition().x - neighborhoodSensingRange;
		double maxX = agent.getPosition().x + neighborhoodSensingRange;
		double minY = agent.getPosition().y - neighborhoodSensingRange;
		double maxY = agent.getPosition().y + neighborhoodSensingRange;
		Envelope e = new Envelope(minX, maxX, minY, maxY);
		@SuppressWarnings("unchecked")
		Collection<Agent2D> l = this.agentsQuad.query(e);


		CCWPolygon aGeo = agent.getGeometry();


		//		//DEBUG
		//		GeometryFactory geofac = new GeometryFactory();
		//		LineString lr = geofac.createLineString(aGeo.getCCWRing());
		//		GisDebugger.addGeometry(lr,"A");


		List<VelocityObstacle> VOs = new ArrayList<VelocityObstacle>();
		for (Agent2D other : l) {
			if (other == agent) {
				continue;
			}
			CCWPolygon oGeo = other.getGeometry();

			//			//DEBUG
			//			LineString lrO = geofac.createLineString(oGeo.getCCWRing());
			//			GisDebugger.addGeometry(lrO,"B");
			//			Coordinate vb0 = other.getPosition();
			//			Coordinate vb1 = new Coordinate(vb0.x+other.getVx(), vb0.y+other.getVy());
			//			LineString lsvb = geofac.createLineString(new Coordinate[]{vb0, vb1});
			//			GisDebugger.addGeometry(lsvb, "vB");



			Coordinate[] cso = ConfigurationSpaceObstacle.getCObstacle(oGeo, aGeo);

			//			//DEBUG
			//			LineString lrCso = geofac.createLineString(cso);
			//			GisDebugger.addGeometry(lrCso,"A(+)-B");

			int [] idx = Algorithms.getTangentIndices(agent.getPosition(),cso);
			Coordinate [] tan = {new Coordinate(agent.getPosition()),new Coordinate(cso[idx[0]]),new Coordinate(cso[idx[1]])};
			Algorithms.translate(other.getVx(),other.getVy(), tan);
			VelocityObstacle info = new VelocityObstacle();
			info.cso = cso;
			info.vo = tan;
			info.vBx = other.getVx();
			info.vBy = other.getVy();

			VOs.add(info);



			//			LineString ranR = geofac.createLineString(new Coordinate []{tan[1],tan[0],tan[2]});
			//			GisDebugger.addGeometry(ranR,"VO");
		}

		Force f = agent.getForce();
		f.reset();
		double[] df = this.driver.getDesiredAccelerationForce(agent);
		double dvx = agent.getVx()+(this.deltaT *df[0])/Agent2D.AGENT_WEIGHT; //desired velocity in the next time step
		double dvy = agent.getVy()+(this.deltaT *df[1])/Agent2D.AGENT_WEIGHT;
		Coordinate c0 = agent.getPosition();
		Coordinate c1 = new Coordinate(c0.x+dvx,c0.y + dvy);

		//		//DEBUG
		//		LineString lsdvA = geofac.createLineString(new Coordinate []{c0,c1});
		//		GisDebugger.addGeometry(lsdvA,"dvA");
		//		Coordinate va = new Coordinate(c0.x+agent.getVx(),c0.y + agent.getVy());
		//		LineString lsvA = geofac.createLineString(new Coordinate[]{c0,va});
		//		GisDebugger.addGeometry(lsvA, "vA");

		//		boolean intersects = intersetcsVo(VOs,c0,c1);
		double ii = timeToCollision(VOs, c0, c1);
		if (ii <= this.timeHorizont) {
			f.setVx(0);
			f.setVy(0);
		} else {
			f.incrementX(df[0]);
			f.incrementY(df[1]);
		}

		//		GisDebugger.dump("/Users/laemmel/tmp/vis/minkowski.shp");

	}

	private double timeToCollision(List<VelocityObstacle> vOs, Coordinate c0,
			Coordinate c1) {

		double ret = Double.POSITIVE_INFINITY;
		for (VelocityObstacle  info : vOs) {
			Coordinate[] vo = info.vo;
			double tmpTime = Double.POSITIVE_INFINITY;

			boolean leftOfLeft = Algorithms.isLeftOfLine(c1, vo[0], vo[1]) > 0;
			boolean rightOfRight = Algorithms.isLeftOfLine(c1, vo[0], vo[2]) < 0;
			if (leftOfLeft && rightOfRight) {
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
		double relVx = ((c1.x - c0.x) - info.vBx);
		double relVy = ((c1.y - c0.y) - info.vBy);
		double dx = relVx*this.timeHorizont;
		double dy = relVy*this.timeHorizont;
		Coordinate tmp = new Coordinate(c0.x+dx, c0.y + dy);

		//DEBUG
		//		GeometryFactory geofac = new GeometryFactory();
		//		LineString ls = geofac.createLineString(new Coordinate [] {c0, tmp});
		//		GisDebugger.addGeometry(ls, "ray");

		double ret = Double.POSITIVE_INFINITY;

		for (int i = 0; i < info.cso.length-2; i++) {
			this.li.computeIntersection(c0, tmp, info.cso[i], info.cso[i+1]);
			if (this.li.hasIntersection()) {
				Coordinate it = this.li.getIntersection(0);

				double time = it.distance(c0)/(Math.sqrt(relVx*relVx+relVy*relVy));
				if (time < ret) {
					ret = time;
				}
				//DEBUG
				//				LineString ls2 = geofac.createLineString(new Coordinate[]{c0,new Coordinate(it)});
				//				GisDebugger.addGeometry(ls2, "ray");
			}
		}


		return ret;
	}




	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public void update(double time) {
		if (time >= this.lastQuadUpdate + this.quadUpdateInterval) {

			updateAgentQuadtree();

			this.lastQuadUpdate = time;
		}

	}

	@Override
	public void forceUpdate() {
		// TODO Auto-generated method stub

	}


	protected void updateAgentQuadtree() {
		this.agentsQuad = new Quadtree();
		for (Agent2D agent : this.floor.getAgents()) {
			Envelope e = new Envelope(agent.getPosition());
			this.agentsQuad.insert(e, agent);
		}

	}

	private static final class VelocityObstacle {
		double vBx;
		double vBy;
		Coordinate[] cso;
		Coordinate[] vo;

	}

}
