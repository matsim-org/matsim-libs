package playground.gregor.sim2d_v3.simulation.floor.forces.reactive;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d_v3.scenario.MyDataContainer;
import playground.gregor.sim2d_v3.simulation.floor.Agent2D;
import playground.gregor.sim2d_v3.simulation.floor.Floor;
import playground.gregor.sim2d_v3.simulation.floor.forces.ForceModule;

import com.vividsolutions.jts.geom.Coordinate;

public class PhysicalEnvironmentForce implements ForceModule{

	private final Scenario sc;

	private QuadTree<Coordinate> quad;

	//Helbing constant
	private static final double Bi=0.25;
	private static final double Ai=5;



	/**
	 * @param floor
	 * @param scenario
	 */
	public PhysicalEnvironmentForce(Floor floor, Scenario scenario) {
		this.sc = scenario;
		//sensing range to maximum
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.gregor.sim2_v2.simulation.floor.ForceModule#run(playground
	 * .gregor.sim2_v2.simulation.Agent2D)
	 */
	@Override
	public void run(Agent2D agent, double time) {
		double fx = 0;
		double fy = 0;

		Coordinate obj = this.quad.get(agent.getPosition().x, agent.getPosition().y);
		double dist = obj.distance(agent.getPosition());
		if (dist > agent.getPhysicalAgentRepresentation().getAgentDiameter()/2) {
			return;
		}

		double dx =(agent.getPosition().x - obj.x) / dist;
		double dy =(agent.getPosition().y - obj.y) / dist;

		//		double g = Agent2D.AGENT_DIAMETER/2 - dist;
		//		double tanDvx = (- agent.getVx()) * dx;
		//		double tanDvy = (- agent.getVy()) * dy;
		//
		//		double tanX = tanDvx * -dy;
		//		double tanY = tanDvy * dx;

		double xc = (Ai * Math.exp(Bi/dist))* dx;
		double yc = (Ai * Math.exp(Bi/dist))* dy;
		//		double xc = (k*g)* dx;
		//		double yc = (k*g)* dy;
		fx += xc;
		fy += yc;



		agent.getForce().incrementX(fx);
		agent.getForce().incrementY(fy);

	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.gregor.sim2d_v2.simulation.floor.ForceModule#init()
	 */
	@Override
	public void init() {
		this.quad = this.sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree();
	}

}
