package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d_v2.scenario.MyDataContainer;
import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalAgentRepresentation;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalFloor;
import playground.gregor.sim2d_v2.simulation.floor.forces.DynamicForceModule;
import playground.gregor.sim2d_v2.simulation.floor.forces.Force;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.CircularVelocityObstacle;

import com.vividsolutions.jts.geom.Coordinate;

public class VelocityObstacleForceII implements DynamicForceModule {

	private final QuadTree<Agent2D> agentsQuad;
	private final PhysicalFloor floor;



	private final double quadUpdateInterval = 0.1;
	private double lastQuadUpdate = Double.NEGATIVE_INFINITY;

	private final PathAndDrivingAcceleration driver;


	private final double timeHorizont = 5;

	private final double tau = 0.5;

	private final Scenario sc;

	private final AlternativeVelocityChooser velocityChooser;
	
	
	public VelocityObstacleForceII(PhysicalFloor floor, Scenario sc) {
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
		List<CircularVelocityObstacle> VOs = new ArrayList<CircularVelocityObstacle>();
		calcOtherAgentsVOs(VOs,agent);
		
		Coordinate c0 = agent.getPosition();
		Coordinate c1 = new Coordinate(c0.x+df[0],c0.y + df[1]);
		
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

		f.incrementX(fx);
		f.incrementY(fy);

	}

	private void calcOtherAgentsVOs(List<CircularVelocityObstacle> vOs,
			Agent2D agent) {
		double sensingRange = agent.getSensingRange();
		Collection<Agent2D> l = this.agentsQuad.get(agent.getPosition().x, agent.getPosition().y, sensingRange);
		if (l.size() > 32) {
			agent.setSensingRange(sensingRange*.9);
		} else if (l.size() < 16) {
			agent.setSensingRange(sensingRange *1.2);
		}

		double r0 = agent.getPhysicalAgentRepresentation().getAgentDiameter()/2;
		
		for (Agent2D other : l) {
			if (other == agent) {
				continue;
			}
			double r1 = other.getPhysicalAgentRepresentation().getAgentDiameter()/2;

			CircularVelocityObstacle info = new CircularVelocityObstacle();
			info.setvBx(other.getVx());
			info.setvBy(other.getVy());
			vOs.add(info);
			double csoR = r1 + r0;
			info.setCso(other.getPosition(), csoR);
			
			double d = agent.getPosition().distance(other.getPosition());
			Coordinate[] tan;
			if (d <= r0+r1) {
				
				double vx = agent.getVx();
				double vy = agent.getVy();
				double v = Math.hypot(vx, vy);
				vx /= v;
				vy /= v;
				vx *= agent.getPhysicalAgentRepresentation().getAgentDiameter()/2;
				vy *= agent.getPhysicalAgentRepresentation().getAgentDiameter()/2;
				Coordinate projected = new Coordinate(agent.getPosition().x-vx, agent.getPosition().y-vy);
				tan = Algorithms.computeTangentsThroughPoint(other.getPosition(), csoR, projected);
			} else {
				tan = Algorithms.computeTangentsThroughPoint(other.getPosition(), csoR, agent.getPosition());
			}
			
			double mvX = 0.5 * (agent.getVx() - other.getVx());
			double mvY = 0.5 * (agent.getVy() - other.getVy());

			double tX = other.getVx() + mvX;
			double tY = other.getVy() + mvY;

			Algorithms.translate(tX,tY, tan);
			
			info.setVo(tan);
		}
		
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
		// TODO Auto-generated method stub
		
	}

}
