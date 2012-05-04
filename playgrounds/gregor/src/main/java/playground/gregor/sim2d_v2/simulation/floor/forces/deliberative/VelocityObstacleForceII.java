package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.scenario.MyDataContainer;
import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalAgentRepresentation;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalFloor;
import playground.gregor.sim2d_v2.simulation.floor.forces.DynamicForceModule;
import playground.gregor.sim2d_v2.simulation.floor.forces.Force;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.CCWPolygon;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.CircularVelocityObstacle;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.ConfigurationSpaceObstacle;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.PolygonalVelocityObstacle;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.VelocityObstacle;

import com.vividsolutions.jts.geom.Coordinate;

public class VelocityObstacleForceII implements DynamicForceModule {

	private final QuadTree<Agent2D> agentsQuad;
	private final PhysicalFloor floor;

//	private boolean debug = false;


	private final double quadUpdateInterval = 0.1;
	private double lastQuadUpdate = Double.NEGATIVE_INFINITY;

	private final PathAndDrivingAcceleration driver;


	private final double timeHorizont = 5;

	private final double tau;

	private final Scenario sc;

	private final AlternativeVelocityChooser velocityChooser;

	private final Map<CCWPolygon,Coordinate []> csos = new HashMap<CCWPolygon,Coordinate[]>();
	private CCWPolygon aGeo;
	private final double timeStepSize;


	public VelocityObstacleForceII(PhysicalFloor floor, Scenario sc) {
		this.floor = floor;
		this.driver = new PathAndDrivingAcceleration(floor,sc);
		this.sc = sc;
		double maxX = 1000*this.sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMaxEasting();
		double minX = -1000 + this.sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMinEasting();
		double maxY = 1000*this.sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMaxNorthing();
		double minY = -1000 + this.sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMinNorthing();
		this.agentsQuad = new QuadTree<Agent2D>(minX, minY, maxX, maxY);

		this.velocityChooser = new RandomAlternativeVelocityChooser(sc);
		this.timeStepSize = ((Sim2DConfigGroup)sc.getConfig().getModule("sim2d")).getTimeStepSize();
		this.tau = ((Sim2DConfigGroup)sc.getConfig().getModule("sim2d")).getTau();
		//		this.velocityChooser = new PenaltyBasedAlternativeVelocityChooser();
	}

	@Override
	public void run(Agent2D agent, double time) {
//				if ((agent.getId().toString().equals("g1"))) {
//					System.out.println("this.debug = true;");
//				}

		Force f = agent.getForce();
		double[] df = this.driver.getDesiredVelocity(agent);
		List<VelocityObstacle> VOs = new ArrayList<VelocityObstacle>();
		if (!calcOtherAgentsVOs(VOs,agent,df)) {
			double fx = PhysicalAgentRepresentation.AGENT_WEIGHT*(0 - agent.getVx())/this.tau;
			double fy = PhysicalAgentRepresentation.AGENT_WEIGHT*(0 - agent.getVy())/this.tau;

			f.incrementX(fx);
			f.incrementY(fy);;
			return;
		}
		calcEnvVOs(VOs,agent);
		Coordinate c0 = agent.getPosition();

		double vxNext = agent.getVx() + this.timeStepSize*(df[0] - agent.getVx())/this.tau;
		double vyNext = agent.getVy() + this.timeStepSize*(df[1] - agent.getVy())/this.tau;

		Coordinate c1 = new Coordinate(c0.x+vxNext,c0.y + vyNext);

		//		//DEBUG
		//		GisDebugger.addCircle(agent.getPosition(),agent.getPhysicalAgentRepresentation().getAgentDiameter()/2,"A");
		//		Coordinate [] vA = {c0,new Coordinate(c0.x + agent.getVx(), c0.y + agent.getVy())};
		//		LineString ls = GisDebugger.geofac.createLineString(vA);
		//		GisDebugger.addGeometry(ls, "vA");


		if (Algorithms.testForCollision(VOs, c1)) {


			this.velocityChooser.chooseAlterantiveVelocity(VOs,c0,c1,df,agent);

		}

		//		try {
		//			//DEBUG
		//			GisDebugger.dump("/Users/laemmel/tmp/tangents.shp");
		//		} catch (Exception e) {
		//		}

		double fx = PhysicalAgentRepresentation.AGENT_WEIGHT*(df[0] - agent.getVx())/this.tau;
		double fy = PhysicalAgentRepresentation.AGENT_WEIGHT*(df[1] - agent.getVy())/this.tau;

//		double fx = PhysicalAgentRepresentation.AGENT_WEIGHT*df[0];
//		double fy = PhysicalAgentRepresentation.AGENT_WEIGHT*df[1];

		
		f.incrementX(fx);
		f.incrementY(fy);

	}

	private void calcEnvVOs(List<VelocityObstacle> VOs, Agent2D agent) {
		QuadTree<CCWPolygon> q = this.sc.getScenarioElement(MyDataContainer.class).getSegmentsQuadTree();


		//		GeometryFactory geofac = new GeometryFactory();

		Coordinate pos = agent.getPosition();

		//TODO think about this
		double dist = Math.max(4, this.timeHorizont*agent.getDesiredVelocity());
		Collection<CCWPolygon> coords = new HashSet<CCWPolygon>();
		q.get(pos.x-dist , pos.y-dist, pos.x+dist, pos.y+dist, coords);
		for (CCWPolygon c : coords) {
			Coordinate [] envObst = this.csos.get(c); 
			if (envObst == null) {
				envObst = ConfigurationSpaceObstacle.getCObstacle(c, this.aGeo);
				this.csos.put(c, envObst);
			}


			int [] idx;
			double collTime = Double.POSITIVE_INFINITY;
			if (Algorithms.contains(agent.getPosition(),envObst)) {
				//TODO no magic numbers here!!
				double move = agent.getPhysicalAgentRepresentation().getAgentDiameter();
				Coordinate cobst = this.sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().get(agent.getPosition().x, agent.getPosition().y);

				double x = move*(cobst.x - agent.getPosition().x);
				double y = move*(cobst.y - agent.getPosition().y);
				Coordinate agPos = new Coordinate(agent.getPosition().x-x,agent.getPosition().y-y);
				idx = Algorithms.getTangentIndices(agPos,envObst);
				Coordinate c0 = envObst[idx[0]];
				Coordinate c1 = envObst[idx[1]];
				double dx = c0.x - c1.x;
				double dy = c0.y - c1.y;
				
				double l = Math.sqrt(dx*dx+dy*dy);
				
//				double mx = (move/2)*dy/l;
//				double my = (move/2)*-dx/l;
				
//				Coordinate cO0 = new Coordinate(agent.getPosition().x+mx,agent.getPosition().y+my);
				Coordinate cO0 = new Coordinate(agent.getPosition().x,agent.getPosition().y);
				Coordinate cO1 =  new Coordinate(cO0.x+dx,cO0.y+dy);
				Coordinate cO2 =  new Coordinate(cO0.x-dx,cO0.y-dy);
				
//				GisDebugger.addCircle(agent.getPosition(), agent.getPhysicalAgentRepresentation().getAgentDiameter()/2, "A");
//				GisDebugger.addCircle(agent.getPosition(), 1.34*0.04*2, "VA");
//				GeometryFactory geofac = new GeometryFactory();
//				LineString ls = geofac.createLineString(envObst);
//								GisDebugger.addGeometry(ls,"envObst");
//
//				LineString ls2 = geofac.createLineString(new Coordinate[]{cO0,cO1});
//				GisDebugger.addGeometry(ls2, "right");
//				LineString ls3 = geofac.createLineString(new Coordinate[]{cO0,cO2});
//				GisDebugger.addGeometry(ls3, "left");
//				GisDebugger.dump("/Users/laemmel/tmp/!!dmp.shp");
				
				
				CircularVelocityObstacle info = new CircularVelocityObstacle();
				info.setvBx(0);
				info.setvBy(0);
				VOs.add(info);
				Coordinate[] tan = new Coordinate[]{cO0,cO2,cO1};
				info.setCollTime(0);
				info.setVo(tan);

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
				Coordinate [] tan = new Coordinate []{new Coordinate(agent.getPosition()),new Coordinate(envObst[idx[0]]),new Coordinate(envObst[idx[1]])};
				PolygonalVelocityObstacle info = new PolygonalVelocityObstacle();
				info.setCso(envObst);
				info.setVo(tan);
				info.setvBx(0);
				info.setvBy(0);
				info.setCollTime(collTime);
				VOs.add(info);
			}



			

			//			//DEBUG
			//			LineString ranR = geofac.createLineString(new Coordinate []{tan[1],tan[0],tan[2],tan[1]});
			//			GisDebugger.addGeometry(ranR,"VO");
		}
	}

	private boolean calcOtherAgentsVOs(List<VelocityObstacle> vOs,
			Agent2D agent, double[] df) {
		double sensingRange = agent.getSensingRange();
		Collection<Agent2D> l = this.agentsQuad.get(agent.getPosition().x, agent.getPosition().y, sensingRange);


		double r0 = agent.getPhysicalAgentRepresentation().getAgentDiameter()/2;

		double rotX = -agent.getVy();
		double rotY = agent.getVx();



		Coordinate rot1 = new Coordinate(agent.getPosition().x+agent.getVx()+rotX,agent.getPosition().y+agent.getVy()+rotY);
		Coordinate rot2 = new Coordinate(agent.getPosition().x+agent.getVx()-rotX,agent.getPosition().y+agent.getVy()-rotY);

		int colls = 0;
		int close = 0;
		for (Agent2D other : l) {
			if (other == agent) {
				continue;
			}
			double d = agent.getPosition().distance(other.getPosition());

			//			if (d > 2 && (Algorithms.isLeftOfLine(other.getPosition(), agent.getPosition(), rot1) >= 0 || Algorithms.isLeftOfLine(other.getPosition(), agent.getPosition(), rot2) <= 0)) {
			//				continue;
			//			} 
			//			
			//			
			//			if (d < 2) {
			//				close++;
			//			}
			//			
			double r1 = other.getPhysicalAgentRepresentation().getAgentDiameter()/2;

			CircularVelocityObstacle info = new CircularVelocityObstacle();
			info.setvBx(other.getVx());
			info.setvBy(other.getVy());
			vOs.add(info);
			double csoR = r1 + r0;
			info.setCso(other.getPosition(), csoR);

			Coordinate[] tan;
			if (d <= r0+r1) {
				//				return false;
				//				double vx = agent.getVx();
				//				double vy = agent.getVy();
				//				double v = Math.hypot(vx, vy);
				//				vx /= v;
				//				vy /= v;
				//				vx *= agent.getPhysicalAgentRepresentation().getAgentDiameter()/2;
				//				vy *= agent.getPhysicalAgentRepresentation().getAgentDiameter()/2;
				//				Coordinate projected = new Coordinate(agent.getPosition().x-vx, agent.getPosition().y-vy);
				//				tan = Algorithms.computeTangentsThroughPoint(other.getPosition(), csoR, projected);
				info.setCollTime(0);

				Coordinate[] coords = Algorithms.computeCircleIntersection(other.getPosition(), csoR, agent.getPosition(), 1.34*0.04*2); //TODO no magic numbers use dv_max instead 

//				if (this.debug && (agent.getId().toString().equals("r56") || agent.getId().toString().equals("g48"))) {
//					colls++;
//					GisDebugger.addCircle(other.getPosition(), r1, "B");
//					GisDebugger.addCircle(other.getPosition(), csoR, "CSO");
//					GisDebugger.addCircle(agent.getPosition(), r0, "A");
//					GisDebugger.addCircle(agent.getPosition(), 1.34*0.04*2, "VA");
//					GeometryFactory geofac = new GeometryFactory();
//					LineString ls = geofac.createLineString(coords);
//					//				GisDebugger.addGeometry(ls,"intersection");
//
//					LineString ls2 = geofac.createLineString(new Coordinate[]{agent.getPosition(),coords[0]});
//					GisDebugger.addGeometry(ls2, "right");
//					LineString ls3 = geofac.createLineString(new Coordinate[]{agent.getPosition(),coords[1]});
//					GisDebugger.addGeometry(ls3, "left");
//					GisDebugger.dump("/Users/laemmel/tmp/!!dmp.shp");
//				}
				tan = new Coordinate[]{agent.getPosition(),coords[1],coords[0]};
				agent.getForce().setVx(0);
				agent.getForce().setVy(0);
				other.getForce().setVx(0);
				other.getForce().setVy(0);
//				df[0] = 0;
//				df[1] = 0;
				//				return false;

			} else {
				tan = Algorithms.computeTangentsThroughPoint(other.getPosition(), csoR, agent.getPosition());

				double mvX = 0.5 * (agent.getVx() - other.getVx());
				double mvY = 0.5 * (agent.getVy() - other.getVy());

				double tX = other.getVx() + mvX;
				double tY = other.getVy() + mvY;

				Algorithms.translate(tX,tY, tan);
			}

			info.setVo(tan);
		}

		if (vOs.size() > 32+close) {
			agent.setSensingRange(sensingRange*.9);
		} else if (vOs.size() < 16+close) {
			agent.setSensingRange(sensingRange *1.2);
		}

		return true;

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

	@Override
	public void init() {
		Coordinate[] e = new Coordinate[9]; 
		int idx = 0;
		for (double alpha = 0; alpha < 2*Math.PI; alpha += (2*Math.PI)/8) {
//			double x = Math.cos(alpha) * PhysicalAgentRepresentation.AGENT_DIAMETER/2;
//			double y = Math.sin(alpha) * PhysicalAgentRepresentation.AGENT_DIAMETER/2;
			double x = Math.cos(alpha) * .7/2; //TODO repair this!! [GL Mar 2012]
			double y = Math.sin(alpha) * .7/2;						
			e[idx++] = new Coordinate(x,y);

		}
		e[idx] = e[0];
		//		LineString ls = GisDebugger.geofac.createLineString(e);
		//		GisDebugger.addGeometry(ls, "?");
		//		GisDebugger.dump("/Users/laemmel/tmp/circle.shp");
		this.aGeo = new CCWPolygon(e, new Coordinate(0,0), 0);

	}

}
