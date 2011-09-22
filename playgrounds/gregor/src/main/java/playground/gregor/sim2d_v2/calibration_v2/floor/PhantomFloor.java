package playground.gregor.sim2d_v2.calibration_v2.floor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;

import com.vividsolutions.jts.geom.Coordinate;

import playground.gregor.sim2d_v2.calibration_v2.LLCalculator;
import playground.gregor.sim2d_v2.calibration_v2.PhantomAgent2D;
import playground.gregor.sim2d_v2.calibration_v2.scenario.PhantomEvents;
import playground.gregor.sim2d_v2.events.XYZAzimuthEvent;
import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.DynamicForceModule;
import playground.gregor.sim2d_v2.simulation.floor.Force;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalFloor;

public class PhantomFloor extends PhysicalFloor {


	//	private final Queue<Event> phantomPopulation;
	private final Map<Id,AgentInfo> agentsMap = new HashMap<Id, AgentInfo>();
	private PhantomAgent2D calibrationAgent;
	private final Id calibrationAgentId;

	private final double[] times;
	private final Event[] events;
	private int pointer =0;


	private final boolean silent = true;
	private boolean finished = false;



	double epsilon = 0.00001;
	private final LLCalculator llCalc;
	//	private final double deltaT;

	public PhantomFloor(PhantomEvents phantomEvents, Id calibrationAgentId, Collection<? extends Link> collection, Scenario scenario, LLCalculator llCalc, EventsManager em) {
		super(scenario,em,true);
		this.calibrationAgentId = calibrationAgentId;

		this.times = phantomEvents.getTimesArray();
		this.events = phantomEvents.getEventsArray();

		this.pointer = 0;
		this.llCalc = llCalc;


		//		this.deltaT = ((Sim2DConfigGroup)scenario.getConfig().getModule("sim2d")).getTimeStepSize();
	}


	@Override
	public void move(double time) {
		updateForces(time);
		moveAgents(time);
	}


	@Override
	protected void moveAgents(double time) {
		double avx = 0;
		double avy = 0;
		if (this.calibrationAgent != null) {
			Force f = this.calibrationAgent.getForce();
			double fx = f.getXComponent();
			double fy = f.getYComponent();
			f.reset();

			//acceleration
			avx =  fx / this.calibrationAgent.getWeight();
			avy =  fy / this.calibrationAgent.getWeight();
		}
		while (!this.finished && this.pointer < this.times.length && this.times[this.pointer] <= time) {
			Event e = this.events[this.pointer];
			this.pointer++;

			if (e instanceof AgentDepartureEvent){
				AgentDepartureEvent ed = (AgentDepartureEvent)e;
				addAgent(ed.getPersonId());
				this.agentsMap.get(ed.getPersonId()).agent.setCurrentLinkId(ed.getLinkId());
				if (!this.silent) {
					getEventsManager().processEvent(e);
				}

			} else if (e instanceof LinkEnterEvent){
				LinkEnterEvent le = (LinkEnterEvent)e;
				this.agentsMap.get(le.getPersonId()).agent.setCurrentLinkId(le.getLinkId());
				if (!this.silent) {
					getEventsManager().processEvent(e);
				}

			} else if (e instanceof XYZAzimuthEvent) {
				XYZAzimuthEvent event = (XYZAzimuthEvent)e;
				AgentInfo ai = this.agentsMap.get(event.getPersonId());
				if (ai.lastUpdate < 0) {
					ai.agent.setCurrentVelocity(0, 0);
				} else {
					double deltaT = event.getTime() - ai.lastUpdate;
					double dX = event.getX() - ai.agent.getPosition().x;
					double dY = event.getY() - ai.agent.getPosition().y;



					double vx = dX / deltaT;
					double vy = dY / deltaT;

					if (this.calibrationAgent != null && event.getPersonId().equals(this.calibrationAgentId) && (ai.agent.getVx() != 0 || ai.agent.getVy() != 0) ){
						double oldvx = ai.agent.getVx();
						double oldvy = ai.agent.getVy();
						double avxObs = (vx - oldvx) / deltaT;
						double avyObs = (vy - oldvy) / deltaT;
						double se = Math.pow(avx-avxObs,2) + Math.pow(avy-avyObs, 2);

						//						if (se > 0.01) {
						//							System.out.println("se:" + se + " avxObs:" + avxObs + " avx:" + avx + " avyObs:" + avyObs + " avy:" + avy);
						//							int stop = 0;
						//							stop++;
						//						}

						this.llCalc.addSE(se);

					}



					ai.agent.setCurrentVelocity(vx, vy);
					ai.agent.getForce().setVx(vx);
					ai.agent.getForce().setVy(vy);
				}
				ai.agent.moveToPostion(event.getCoordinate());
				ai.lastUpdate = event.getTime();
				if (!this.silent) {
					getEventsManager().processEvent(e);
				}
			} else if (e instanceof AgentArrivalEvent) {
				AgentArrivalEvent ea = (AgentArrivalEvent)e;
				removeAgent(ea.getPersonId());
				if (!this.silent) {
					getEventsManager().processEvent(e);
				}
			}
		}

	}




	@Override
	protected boolean checkForEndOfLinkReached(Agent2D agent,
			Coordinate oldPos, Coordinate newPos, double time) {
		return false;
	}


	@Override
	protected void updateForces(double time) {
		for (DynamicForceModule m : this.dynamicForceModules) {
			m.update(time);
		}
		if (this.calibrationAgent != null) {
			updateForces(this.calibrationAgent, time);
		}
	}


	private void removeAgent(Id id) {
		AgentInfo pa = this.agentsMap.remove(id);
		this.agents.remove(pa.agent);
		if (id.equals(this.calibrationAgentId)) {
			this.calibrationAgent = null;
			this.finished  = true;
		}
	}

	private void addAgent(Id id) {
		PhantomAgent2D pa = new PhantomAgent2D(id);
		pa.setPostion(new Coordinate(0,0));
		AgentInfo ai = new AgentInfo();
		ai.agent = pa;
		ai.lastUpdate = -1;
		this.agents.add(pa);
		this.agentsMap.put(id, ai);
		if (id.equals(this.calibrationAgentId)) {
			this.calibrationAgent = pa;
		}
	}

	private static class AgentInfo {
		PhantomAgent2D agent;
		double lastUpdate;
	}

}
