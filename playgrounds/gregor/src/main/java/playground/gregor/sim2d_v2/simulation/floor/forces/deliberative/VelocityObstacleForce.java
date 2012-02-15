package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d_v2.scenario.MyDataContainer;
import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalAgentRepresentation;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalFloor;
import playground.gregor.sim2d_v2.simulation.floor.VelocityDependentEllipse;
import playground.gregor.sim2d_v2.simulation.floor.forces.DynamicForceModule;
import playground.gregor.sim2d_v2.simulation.floor.forces.Force;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.CCWPolygon;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.ConfigurationSpaceObstacle;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.PolygonalVelocityObstacle;

import com.vividsolutions.jts.algorithm.NonRobustLineIntersector;
import com.vividsolutions.jts.geom.Coordinate;

public class VelocityObstacleForce implements DynamicForceModule{

	NonRobustLineIntersector li = new NonRobustLineIntersector();

	private final QuadTree<Agent2D> agentsQuad;
	private final PhysicalFloor floor;



	private final double quadUpdateInterval = 0.1;
	private double lastQuadUpdate = Double.NEGATIVE_INFINITY;

	private final PathAndDrivingAcceleration driver;


	private final double timeHorizont = 5;

	private final double tau = 0.5;

	private final Scenario sc;

	private final AlternativeVelocityChooser velocityChooser;


	//Laemmel constant
	@Deprecated
	private static final double neighborhoodSensingRange = 5;

	public VelocityObstacleForce(PhysicalFloor floor, Scenario sc) {
		this.floor = floor;
		this.driver = new PathAndDrivingAcceleration(floor,sc);
		this.sc = sc;
		double maxX = 1000*this.sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMaxEasting();
		double minX = -1000 + this.sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMinEasting();
		double maxY = 1000*this.sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMaxNorthing();
		double minY = -1000 + this.sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMinNorthing();
		this.agentsQuad = new QuadTree<Agent2D>(minX, minY, maxX, maxY);

		this.velocityChooser = new RandomAlternativeVelocityChooser();
//		this.velocityChooser = new PenaltyBasedAlternativeVelocityChooser();
	}


	@Override
	public void run(Agent2D agent, double time) {

		Force f = agent.getForce();
		double[] df = this.driver.getDesiredVelocity(agent);
		//		if (agent.getEarliestUpdate() > time) {
		//			double fx = Agent2D.AGENT_WEIGHT*(df[0] - agent.getVx())/this.tau;
		//			double fy = Agent2D.AGENT_WEIGHT*(df[1] - agent.getVy())/this.tau;
		//			f.incrementX(fx);
		//			f.incrementY(fy);
		//			return;
		//		}


		CCWPolygon aGeo = ((VelocityDependentEllipse)agent.getPhysicalAgentRepresentation()).getGeometry();





		List<PolygonalVelocityObstacle> VOs = new ArrayList<PolygonalVelocityObstacle>();


		calcOtherAgentsVOs(VOs,agent,aGeo);
		calcEnvVOs(VOs,agent,aGeo);


		Coordinate c0 = agent.getPosition();
		Coordinate c1 = new Coordinate(c0.x+df[0],c0.y + df[1]);


////		//DEBUG
//		GeometryFactory geofac = new GeometryFactory();
//		Coordinate c2 = new Coordinate(c0.x+agent.getVx(),c0.y + agent.getVy());
//		LineString lsdvA = geofac.createLineString(new Coordinate []{c0,c1});
//		GisDebugger.addGeometry(lsdvA,"dvA");
//		LineString lsvA = geofac.createLineString(new Coordinate []{c0,c2});
//		GisDebugger.addGeometry(lsvA,"vA");
//		GisDebugger.dump("/Users/laemmel/devel/tmp/v.shp");
//		boolean brk = false;
////		//DEBUG
		
		if (Algorithms.testForCollision(VOs, c1)) {
			this.velocityChooser.chooseAlterantiveVelocity(VOs,c0,c1,df,agent);
//			brk = true;
		}


		double fx = PhysicalAgentRepresentation.AGENT_WEIGHT*(df[0] - agent.getVx())/this.tau;
		double fy = PhysicalAgentRepresentation.AGENT_WEIGHT*(df[1] - agent.getVy())/this.tau;

		f.incrementX(fx);
		f.incrementY(fy);

////		//DEBUG
//		LineString lr = geofac.createLineString(aGeo.getCCWRing());
//		GisDebugger.addGeometry(lr,"A:"+ agent.getId().toString());
//		GisDebugger.dump("/Users/laemmel/devel/tmp/agent.shp");
//		if (brk && agent.getCurrentLinkId().toString().equals("26") && agent.getId().toString().equals("r6") && time>=65) {
//			System.out.println();
//		}
////		//DEBUG

	}

	private void calcEnvVOs(List<PolygonalVelocityObstacle> VOs, Agent2D agent,
			CCWPolygon aGeo) {
		QuadTree<CCWPolygon> q = this.sc.getScenarioElement(MyDataContainer.class).getSegmentsQuadTree();


		//		GeometryFactory geofac = new GeometryFactory();

		Coordinate pos = agent.getPosition();

		//TODO think about this
		double dist = Math.max(4, this.timeHorizont*agent.getDesiredVelocity());
		Collection<CCWPolygon> coords = new HashSet<CCWPolygon>();
		q.get(pos.x-dist , pos.y-dist, pos.x+dist, pos.y+dist, coords);
		for (CCWPolygon c : coords) {
			Coordinate [] envObst = ConfigurationSpaceObstacle.getCObstacle(c, aGeo);

			int [] idx;
			double collTime = Double.POSITIVE_INFINITY;
			if (Algorithms.contains(agent.getPosition(),envObst)) {
				//TODO no magic numbers here!!
				double move = aGeo.d;
				Coordinate cobst = this.sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().get(agent.getPosition().x, agent.getPosition().y);

				double x = move*(cobst.x - agent.getPosition().x);
				double y = move*(cobst.y - agent.getPosition().y);
				Coordinate agPos = new Coordinate(agent.getPosition().x-x,agent.getPosition().y-y);
				idx = Algorithms.getTangentIndices(agPos,envObst);

				//				//DEBUG
				//				LineString lrO = geofac.createLineString(c.getCCWRing());
				//				GisDebugger.addGeometry(lrO,"B");
				//				LineString lrCso = geofac.createLineString(envObst);
				//				GisDebugger.addGeometry(lrCso,"A(+)-B");
				collTime = 0;
				//				agent.getForce().setVx(.9*agent.getVx());
				//				agent.getForce().setVy(.9*agent.getVy());

			} else {
				idx = Algorithms.getTangentIndices(agent.getPosition(),envObst);

			}

			Coordinate [] tan = new Coordinate []{new Coordinate(agent.getPosition()),new Coordinate(envObst[idx[0]]),new Coordinate(envObst[idx[1]])};
			PolygonalVelocityObstacle info = new PolygonalVelocityObstacle();
			info.setCso(envObst);
			info.setVo(tan);
			info.setvBx(0);
			info.setvBy(0);
			info.setCollTime(collTime);

			VOs.add(info);

			//			//DEBUG
			//			LineString ranR = geofac.createLineString(new Coordinate []{tan[1],tan[0],tan[2],tan[1]});
			//			GisDebugger.addGeometry(ranR,"VO");
		}
	}


	private void calcOtherAgentsVOs(List<PolygonalVelocityObstacle> VOs, Agent2D agent, CCWPolygon aGeo) {
		double sensingRange = agent.getSensingRange();

		//		double minX = agent.getPosition().x - sensingRange;
		//		double maxX = agent.getPosition().x + sensingRange;
		//		double minY = agent.getPosition().y - sensingRange;
		//		double maxY = agent.getPosition().y + sensingRange;



//		GeometryFactory geofac = new GeometryFactory();
//		boolean dump = false;

		Collection<Agent2D> l = this.agentsQuad.get(agent.getPosition().x, agent.getPosition().y, sensingRange);

//		if (l.size() > 32) {
//			agent.setSensingRange(sensingRange*.9);
//		} else if (l.size() < 16) {
//			agent.setSensingRange(sensingRange *1.2);
//		}

		for (Agent2D other : l) {
			if (other == agent) {
				continue;
			}



			CCWPolygon oGeo = ((VelocityDependentEllipse)other.getPhysicalAgentRepresentation()).getGeometry();

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

			Coordinate [] tan = null;

			double collTime = Double.POSITIVE_INFINITY;
			Coordinate agPos = agent.getPosition();
			double dist = other.getPosition().distance(agent.getPosition());
			if (dist < (oGeo.d+aGeo.d) && Algorithms.contains(agent.getPosition(), cso)) {
				//				//DEBUG
				//				LineString lrO = geofac.createLineString(oGeo.getCCWRing());
				//				GisDebugger.addGeometry(lrO,"B");
				//				LineString lrCso = geofac.createLineString(cso);
				//				GisDebugger.addGeometry(lrCso,"A(+)-B");

				double move = oGeo.d + aGeo.d;
				double x = move*(other.getPosition().x - agent.getPosition().x)/dist;
				double y = move*(other.getPosition().y - agent.getPosition().y)/dist;
				agPos = new Coordinate(agent.getPosition().x-x,agent.getPosition().y-y);

				//				double x = other.getPosition().x - agent.getPosition().x;
				//				double y = other.getPosition().y - agent.getPosition().y;
				//
				//				//90deg is not necessarily outside the VO TODO needs to be revised!!
				//				Coordinate c0 = new Coordinate(agent.getPosition().x - y, agent.getPosition().y + x);
				//				Coordinate c1 = new Coordinate(agent.getPosition().x + y, agent.getPosition().y - x);
				//				tan = new Coordinate[]{new Coordinate(agent.getPosition()),c1,c0};

				//DEBUG
				int [] idx = Algorithms.getTangentIndices(agPos,cso);
				tan = new Coordinate[]{new Coordinate(agent.getPosition()),new Coordinate(cso[idx[0]]),new Coordinate(cso[idx[1]])};
				//				LineString ranR = geofac.createLineString(new Coordinate []{tan[1],tan[0],tan[2]});
				//				GisDebugger.addGeometry(ranR,"VO");

				collTime = 0;
			} else {
				int [] idx = Algorithms.getTangentIndices(agent.getPosition(),cso);
				tan = new Coordinate[]{new Coordinate(agent.getPosition()),new Coordinate(cso[idx[0]]),new Coordinate(cso[idx[1]])};
			}

			double mvCoeff = 1 - agent.kindness /(agent.kindness + other.kindness);

			double mvX = mvCoeff*(agent.getVx() - other.getVx());
			double mvY = mvCoeff*(agent.getVy() - other.getVy());

			double tX = other.getVx() + mvX;
			double tY = other.getVy() + mvY;

			Algorithms.translate(tX,tY, tan);
			PolygonalVelocityObstacle info = new PolygonalVelocityObstacle();
			info.setCso(cso);
			info.setVo(tan);
			info.setvBx(other.getVx());
			info.setvBy(other.getVy());
			info.setCollTime(collTime);
			VOs.add(info);


////			//DEBUG
//			double dx0 = 100*(tan[1].x - tan[0].x);
//			double dy0 = 100*(tan[1].y - tan[0].y);
//			double dx1 = 100*(tan[2].x - tan[0].x);
//			double dy1 = 100*(tan[2].y - tan[0].y);
//			LinearRing ranR = geofac.createLinearRing(new Coordinate []{new Coordinate(tan[0]),new Coordinate(tan[0].x+dx0,tan[0].y + dy0),new Coordinate(tan[0].x+dx1,tan[0].y + dy1),tan[0]});
//			Polygon p = geofac.createPolygon(ranR, null);
//			GisDebugger.addGeometry(p,"VO B:" + other.getId().toString());
//			dump = true;
		}

//		if (dump) {
//			GisDebugger.dump("/Users/laemmel/devel/tmp/vo.shp");
//			for (Agent2D other : l) {
//				if (other == agent) {
//					continue;
//				}
//				LineString lr = geofac.createLineString(other.getGeometry().getCCWRing());
//				GisDebugger.addGeometry(lr,"B:" + other.getId().toString());
//				 
//			}
// 			 GisDebugger.dump("/Users/laemmel/devel/tmp/others.shp");
//			for (Agent2D other : l) {
//				if (other == agent) {
//					continue;
//				}
//				LineString lr = geofac.createLineString(new Coordinate[]{other.getPosition(),new Coordinate(other.getPosition().x+other.getVx(),other.getPosition().y+other.getVy())});
//				GisDebugger.addGeometry(lr,"vb");
//				 
//			}
//			GisDebugger.dump("/Users/laemmel/devel/tmp/vothers.shp");
//			
//		}

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

		this.agentsQuad.clear();
		for (Agent2D agent : this.floor.getAgents()) {
			this.agentsQuad.put(agent.getPosition().x, agent.getPosition().y, agent);
		}

	}

}
