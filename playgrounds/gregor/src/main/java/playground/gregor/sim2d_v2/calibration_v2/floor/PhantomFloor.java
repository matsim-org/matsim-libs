package playground.gregor.sim2d_v2.calibration_v2.floor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;

import com.vividsolutions.jts.geom.Coordinate;

import playground.gregor.sim2d_v2.calibration_v2.PhantomAgent2D;
import playground.gregor.sim2d_v2.calibration_v2.Validator;
import playground.gregor.sim2d_v2.calibration_v2.scenario.PhantomEvents;
import playground.gregor.sim2d_v2.events.XYZAzimuthEvent;
import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2d_v2.simulation.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.DynamicForceModule;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalFloor;

public class PhantomFloor extends PhysicalFloor {


	//	private final Queue<Event> phantomPopulation;
	private final Map<Id,AgentInfo> agentsMap = new HashMap<Id, AgentInfo>();
	private PhantomAgent2D calibrationAgent;
	private final Id calibrationAgentId;

	private final double[] times;
	private final Event[] events;
	private int pointer =0;
	private final Validator validator;

	private double lastValidated = 0;
	private final double validationIntervall = 8;

	private final boolean silent = true;
	private boolean finished = false;

	public PhantomFloor(PhantomEvents phantomEvents, Id calibrationAgentId, Collection<Link> list, Scenario2DImpl scenario, Validator validator, EventsManager em) {
		super(scenario,list,em,true);
		this.calibrationAgentId = calibrationAgentId;

		this.times = phantomEvents.getTimesArray();
		this.events = phantomEvents.getEventsArray();

		this.pointer = 0;
		this.validator = validator;
	}


	@Override
	public void move(double time) {
		updateForces(time);
		moveAgents(time);
	}


	@Override
	protected void moveAgents(double time) {
		if (this.calibrationAgent != null) {
			moveAgentAndCheckForEndOfLeg(this.calibrationAgent, time);
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
				if (event.getPersonId().equals(this.calibrationAgentId)) {
					if (this.calibrationAgent.getPosition().x == 0 && this.calibrationAgent.getPosition().y == 0) {
						this.calibrationAgent.moveToPostion(event.getCoordinate());
						this.validator.initI(this.calibrationAgentId);
					} else {
						this.validator.addDesiredEvent(event);
					}
					continue;
				}

				AgentInfo ai = this.agentsMap.get(event.getPersonId());

				if (ai.lastUpdate < 0) {
					ai.agent.setCurrentVelocity(0, 0);
				} else {
					double deltaT = event.getTime() - ai.lastUpdate;
					double dX = event.getX() - ai.agent.getPosition().x;
					double dY = event.getY() - ai.agent.getPosition().y;
					double vx = dX / deltaT;
					double vy = dY / deltaT;
					ai.agent.setCurrentVelocity(vx, vy);
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

		if (time >= this.lastValidated+this.validationIntervall && this.calibrationAgent != null) {
			this.validator.validate(this.calibrationAgent);
			this.lastValidated = time;
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
		this.agents.remove(pa);
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
