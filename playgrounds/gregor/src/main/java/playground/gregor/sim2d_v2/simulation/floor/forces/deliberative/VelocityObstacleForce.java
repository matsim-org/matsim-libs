package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;


import com.vividsolutions.jts.algorithm.NonRobustLineIntersector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;


import playground.gregor.sim2d_v2.scenario.MyDataContainer;
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

	private static double SQRT2 = Math.sqrt(2);

	private final double quadUpdateInterval = 0.1;
	private double lastQuadUpdate = Double.NEGATIVE_INFINITY;

	private final PathAndDrivingAcceleration driver;

	private final double deltaT = 1/25.;

	private final double timeHorizont = 5;

	private final double w_i = 1;

	private final double tau = 0.5;

	private final Scenario sc;

	private QuadTree<Coordinate[]> quad;

	//Laemmel constant
	private static final double neighborhoodSensingRange = 5;

	public VelocityObstacleForce(PhysicalFloor floor, Scenario sc) {
		this.floor = floor;
		this.driver = new PathAndDrivingAcceleration(floor,sc);
		this.sc = sc;
	}


	@Override
	public void run(Agent2D agent, double time) {

		Force f = agent.getForce();
		double[] df = this.driver.getDesiredAccelerationForce(agent);
		if (agent.getEarliestUpdate() > time) {
			f.incrementX(df[0]);
			f.incrementY(df[1]);
			return;
		}


		CCWPolygon aGeo = agent.getGeometry();


		//		//DEBUG
		//		GeometryFactory geofac = new GeometryFactory();
		//		LineString lr = geofac.createLineString(aGeo.getCCWRing());
		//		GisDebugger.addGeometry(lr,"A");


		List<VelocityObstacle> VOs = new ArrayList<VelocityObstacle>();


		calcOtherAgentsVOs(VOs,agent,aGeo);
		calcEnvVOs(VOs,agent,aGeo);

		double dvx = agent.getVx()+(this.deltaT *df[0])/Agent2D.AGENT_WEIGHT; //desired velocity in the next time step
		double dvy = agent.getVy()+(this.deltaT *df[1])/Agent2D.AGENT_WEIGHT;
		Coordinate c0 = agent.getPosition();
		Coordinate c1 = new Coordinate(c0.x+dvx,c0.y + dvy);

		double ii = timeToCollision(VOs, c0, c1);
		//		//DEBUG
		//		LineString lsdvA = geofac.createLineString(new Coordinate []{c0,c1});
		//		GisDebugger.addGeometry(lsdvA,"dvA: " + ii);

		if (ii <= this.timeHorizont) {
			ii = chooseAlternativeAccelerationForce(ii,VOs,c0,df,dvx,dvy,agent);
		}

		f.incrementX(df[0]);
		f.incrementY(df[1]);

		double timeToUpdate = Math.min(2, ii);
		agent.setEarliestUpdate(time+timeToUpdate);
		//		GisDebugger.dump("/Users/laemmel/tmp/vis/minkowski.shp");

	}

	private void calcEnvVOs(List<VelocityObstacle> VOs, Agent2D agent,
			CCWPolygon aGeo) {
		QuadTree<CCWPolygon> q = this.sc.getScenarioElement(MyDataContainer.class).getSegmentsQuadTree();

		Coordinate pos = agent.getPosition();
		double dist = Math.max(4, this.timeHorizont*agent.getDesiredVelocity());
		Collection<CCWPolygon> coords = new HashSet<CCWPolygon>();
		q.get(pos.x-dist , pos.y-dist, pos.x+dist, pos.y+dist, coords);
		for (CCWPolygon c : coords) {
			Coordinate [] envObst = ConfigurationSpaceObstacle.getCObstacle(c, aGeo);
			int [] idx = Algorithms.getTangentIndices(agent.getPosition(),envObst);
			Coordinate [] tan = {new Coordinate(agent.getPosition()),new Coordinate(envObst[idx[0]]),new Coordinate(envObst[idx[1]])};
			VelocityObstacle info = new VelocityObstacle();
			info.cso = envObst;
			info.vo = tan;
			info.vBx = 0;
			info.vBy = 0;

			VOs.add(info);
		}


	}


	private void calcOtherAgentsVOs(List<VelocityObstacle> VOs, Agent2D agent, CCWPolygon aGeo) {
		double minX = agent.getPosition().x - neighborhoodSensingRange;
		double maxX = agent.getPosition().x + neighborhoodSensingRange;
		double minY = agent.getPosition().y - neighborhoodSensingRange;
		double maxY = agent.getPosition().y + neighborhoodSensingRange;
		Envelope e = new Envelope(minX, maxX, minY, maxY);
		@SuppressWarnings("unchecked")
		Collection<Agent2D> l = this.agentsQuad.query(e);
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


	}

	//TODO put this in an extra class
	private double chooseAlternativeAccelerationForce(double ii,
			List<VelocityObstacle> vOs, Coordinate c0, double[] df, double dvx, double dvy, Agent2D agent) {
		double minPenalty = this.w_i/ii;
		//		double deltaVx = this.deltaT*df[0]/Agent2D.AGENT_WEIGHT;
		//		double deltaVy = this.deltaT*df[1]/Agent2D.AGENT_WEIGHT;

		double retTime = 0;

		double v = Math.sqrt(dvx*dvx+dvy*dvy);

		//		vx /= v;
		//		vy /= v;

		double fx = df[0];
		double fy = df[1];

		//45 deg to the left
		double nvx = (dvx - dvy)/SQRT2;
		double nvy = (dvy + dvx)/SQRT2;
		Coordinate c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		double time = timeToCollision(vOs, c0, c1);
		double diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		double penalty = this.w_i / time + diff;
		if (penalty < minPenalty) {
			minPenalty = penalty;
			fx = (nvx - agent.getVx())*Agent2D.AGENT_WEIGHT/this.tau ;
			fy = (nvy - agent.getVy())*Agent2D.AGENT_WEIGHT/this.tau;
			retTime = time;
		}


		//90 deg to the left
		nvx = dvy;
		nvy = -dvx;
		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		time = timeToCollision(vOs, c0, c1);
		//		diff = 2*deltaVy;
		diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		penalty = this.w_i / time + diff;
		if (penalty < minPenalty) {
			minPenalty = penalty;
			fx = (nvx - agent.getVx())*Agent2D.AGENT_WEIGHT/this.tau ;
			fy = (nvy - agent.getVy())*Agent2D.AGENT_WEIGHT/this.tau;
			retTime =time;
		}
		//		//DEBUG
		//		GeometryFactory geofac = new GeometryFactory();
		//		LineString leftvA = geofac.createLineString(new Coordinate []{c0,c1});
		//		GisDebugger.addGeometry(leftvA,"left va: " + time);

		//45 deg to the right
		nvx = (dvx + dvy)/SQRT2;
		nvy = (dvy-dvx)/SQRT2;
		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		time = timeToCollision(vOs, c0, c1);
		//		diff = 2*deltaVy;
		diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		penalty = this.w_i / time + diff;
		if (penalty < minPenalty) {
			minPenalty = penalty;
			fx = (nvx - agent.getVx())*Agent2D.AGENT_WEIGHT/this.tau ;
			fy = (nvy - agent.getVy())*Agent2D.AGENT_WEIGHT/this.tau;
			retTime = time;
		}
		//90 deg to the right
		nvx = -dvy;
		nvy = dvx;
		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		time = timeToCollision(vOs, c0, c1);
		//		diff = 2*deltaVy;
		diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		penalty = this.w_i / time + diff;
		if (penalty < minPenalty) {
			minPenalty = penalty;
			fx = (nvx - agent.getVx())*Agent2D.AGENT_WEIGHT/this.tau ;
			fy = (nvy - agent.getVy())*Agent2D.AGENT_WEIGHT/this.tau;
			retTime = time;
		}
		//		//DEBUG
		//		LineString rightvA = geofac.createLineString(new Coordinate []{c0,c1});
		//		GisDebugger.addGeometry(rightvA,"right va: " + time);

		//accel
		nvx = dvx*1.25;
		nvy = dvy*1.25;
		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		time = timeToCollision(vOs, c0, c1);
		//		diff = 2*deltaVy;
		diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		penalty = this.w_i / time + diff;
		if (penalty < minPenalty) {
			minPenalty = penalty;
			fx = (nvx - agent.getVx())*Agent2D.AGENT_WEIGHT/this.tau ;
			fy = (nvy - agent.getVy())*Agent2D.AGENT_WEIGHT/this.tau;
			retTime = time;
		}

		//break
		nvx = 0.1*dvx;
		nvy = 0.1*dvy;
		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		time = timeToCollision(vOs, c0, c1);
		//		diff = 2*deltaVy;
		diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		penalty = this.w_i / time + diff;
		if (penalty < minPenalty) {
			minPenalty = penalty;
			fx = (nvx - agent.getVx())*Agent2D.AGENT_WEIGHT/this.tau ;
			fy = (nvy - agent.getVy())*Agent2D.AGENT_WEIGHT/this.tau;
			retTime = time;
		}


		//		//keep current
		//		nvx = vx;
		//		nvy = vy;
		//		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		//		time = timeToCollision(vOs, c0, c1);
		//		//		diff = 2*deltaVy;
		//		penalty = this.w_i / time + diff;
		//		if (penalty < minPenalty) {
		//			minPenalty = penalty;
		//			fx = df[0];
		//			fy = df[1];
		//		}

		df[0] =  fx;
		df[1] = fy;

		return retTime;

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

		for (int i = 0; i < info.cso.length-1; i++) {
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
