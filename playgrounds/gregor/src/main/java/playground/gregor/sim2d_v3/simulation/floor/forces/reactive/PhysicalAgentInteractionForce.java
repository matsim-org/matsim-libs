package playground.gregor.sim2d_v3.simulation.floor.forces.reactive;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d_v3.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v3.scenario.MyDataContainer;
import playground.gregor.sim2d_v3.simulation.floor.Agent2D;
import playground.gregor.sim2d_v3.simulation.floor.PhysicalFloor;
import playground.gregor.sim2d_v3.simulation.floor.forces.DynamicForceModule;

public class PhysicalAgentInteractionForce implements DynamicForceModule {

	protected final PhysicalFloor floor;
	protected final Scenario sc;

	private final double quadUpdateInterval = 0.1;
	private double lastQuadUpdate = Double.NEGATIVE_INFINITY;
	private QuadTree<Agent2D> coordsQuad;
	private final Sim2DConfigGroup s2d;

	//Helbing constant
	private static final double Bi=0.08;
	private static final double Ai=2000;


	/**
	 * @param floor
	 * @param sceanrio
	 */
	public PhysicalAgentInteractionForce(PhysicalFloor floor, Scenario scenario) {
		this.floor = floor;
		this.sc = scenario;
		this.s2d = (Sim2DConfigGroup) scenario.getConfig().getModule("sim2d");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.gregor.sim2_v2.simulation.floor.ForceModule#run(playground
	 * .gregor.sim2_v2.simulation.Agent2D)
	 */
	@Override
	public void run(Agent2D agent,double time) {
		updateForces(agent);
	}

	/**
	 * @param agent
	 * @param neighbors
	 */
	/* package */void updateForces(Agent2D agent) {//, List<Coordinate> neighbors) {
		double fx = 0;
		double fy = 0;

		Collection<Agent2D> l = this.coordsQuad.get(agent.getPosition().x, agent.getPosition().y, 2*agent.getPhysicalAgentRepresentation().getAgentDiameter());

		for (Agent2D other : l) {
			if (other == agent) {
				continue;
			}

			double dist = other.getPosition().distance(agent.getPosition());
			
//			double dv_max = agent.getDesiredVelocity() * this.s2d.getTau() * this.s2d.getTimeStepSize();
			double csoR = other.getPhysicalAgentRepresentation().getAgentDiameter()/2 + agent.getPhysicalAgentRepresentation().getAgentDiameter()/2;
			if (dist > csoR+0.01) {
				continue;
			}
			double dx = (agent.getPosition().x - other.getPosition().x) / dist;
			double dy = (agent.getPosition().y - other.getPosition().y) / dist;


			double xc = (Ai * Math.exp( Bi/dist))* dx;
			double yc = (Ai * Math.exp(Bi/dist))* dy;
			//			double xc = ( k*g)* dx;
			//			double yc = (k*g)* dy ;

			fx += xc;
			fy += yc;


		}


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
		double maxX = this.sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMaxEasting();
		double minX = this.sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMinEasting();
		double maxY = this.sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMaxNorthing();
		double minY = this.sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMinNorthing();
		this.coordsQuad = new QuadTree<Agent2D>(minX, minY, maxX, maxY);
	}

	/**
	 * 
	 */
	protected void updateAgentQuadtree() {
		this.coordsQuad.clear();
		for (Agent2D agent : this.floor.getAgents()) {
			this.coordsQuad.put(agent.getPosition().x, agent.getPosition().y, agent);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.gregor.sim2d_v2.simulation.floor.DynamicForceModule#update
	 * (double)
	 */
	@Override
	public void update(double time) {
		if (time >= this.lastQuadUpdate + this.quadUpdateInterval) {

			updateAgentQuadtree();

			this.lastQuadUpdate = time;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.gregor.sim2d_v2.simulation.floor.DynamicForceModule#forceUpdate
	 * (double)
	 */
	@Override
	public void forceUpdate() {
		updateAgentQuadtree();
	}

}
