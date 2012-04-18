package playground.gregor.prorityqueuesimtest;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.internal.MatsimComparator;
import org.matsim.core.config.Module;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.simulation.floor.Agent2D;

public class PrioQEngine implements MobsimEngine {

	private final Scenario scenario;

	private final Sim2DConfigGroup sim2ConfigGroup;
	private final double sim2DStepSize;
	private final QSim sim;
	
	private final Queue<Agent2D> activityEndsList = new PriorityQueue<Agent2D>(500,new Agent2DDepartureTimeComparator());
	
	
	private InternalInterface internalInterface = null ;

	private PrioQNetwork simNetwork;
	@Override
	public void setInternalInterface( InternalInterface internalInterface ) {
		this.internalInterface = internalInterface ;
	}

	/**
	 * @param sim
	 * @param random
	 */
	public PrioQEngine(QSim sim) {
		this.scenario = sim.getScenario();
		Module m = this.scenario.getConfig().getModule("sim2d");
		this.sim2ConfigGroup = new Sim2DConfigGroup(m); 
		this.sim = sim;
		this.sim2DStepSize = this.sim2ConfigGroup.getTimeStepSize();
		double factor = this.scenario.getConfig().getQSimConfigGroup().getTimeStepSize() / this.sim2DStepSize;
		if (factor != Math.round(factor)) {
			throw new RuntimeException("QSim time step size has to be a multiple of sim2d time step size");
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.core.mobsim.framework.Steppable#doSimStep(double)
	 */
	@Override
	public void doSimStep(double time) {
//		long start = System.currentTimeMillis();
		double sim2DTime = time;
		while (sim2DTime < time + this.scenario.getConfig().getQSimConfigGroup().getTimeStepSize()) {
			handleDepartures(sim2DTime);
			this.simNetwork.move(sim2DTime);
			
			sim2DTime += this.sim2DStepSize;
		}
//		long stop = System.currentTimeMillis();
//		long timet = (stop - start)/25;
//		System.out.println(1000/timet + "fps    " + this.floor.getAgents().size() +"agents" );
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.ptproject.qsim.interfaces.SimEngine#afterSim()
	 */
	@Override
	public void afterSim() {
		// throw new RuntimeException("not (yet) implemented!");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.ptproject.qsim.interfaces.SimEngine#onPrepareSim()
	 */
	@Override
	public void onPrepareSim() {

		this.simNetwork = new PrioQNetwork(this.scenario,this.sim.getEventsManager(),this.internalInterface);
	}

	public PrioQNetwork getPrioQNetwork() {
		return this.simNetwork;
	}

	private void handleDepartures(double time) {
		while (this.activityEndsList.peek() != null) {
			Agent2D agent = this.activityEndsList.peek();
			if (agent.getRealActivityEndTime() <= time) {
				this.activityEndsList.poll();
				this.simNetwork.agentDepart(agent);
			} else {
				return;
			}
		}
		
	}
	
	public void agentDepart(MobsimDriverAgent agent) {
		this.activityEndsList.add((Agent2D)agent);
		
	}

	private static class Agent2DDepartureTimeComparator implements Comparator<Agent2D>, MatsimComparator {

		@Override
		public int compare(Agent2D agent1, Agent2D agent2) {
			int cmp = Double.compare(agent1.getRealActivityEndTime(), agent2.getRealActivityEndTime());
			if (cmp == 0) {
				// Both depart at the same time -> let the one with the larger id be first (=smaller)
				return agent2.getId().compareTo(agent1.getId());
			}
			return cmp;
		}
		
	}

}
