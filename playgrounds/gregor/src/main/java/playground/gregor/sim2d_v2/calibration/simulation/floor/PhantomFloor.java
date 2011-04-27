package playground.gregor.sim2d_v2.calibration.simulation.floor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;

import com.vividsolutions.jts.geom.Coordinate;

import playground.gregor.sim2d_v2.calibration.Validator;
import playground.gregor.sim2d_v2.calibration.scenario.PhantomEvents;
import playground.gregor.sim2d_v2.events.XYZAzimuthEvent;
import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2d_v2.simulation.Agent2D;
import playground.gregor.sim2d_v2.simulation.Sim2D;
import playground.gregor.sim2d_v2.simulation.floor.DynamicForceModule;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalFloor;

public class PhantomFloor extends PhysicalFloor {


	//	private final Queue<Event> phantomPopulation;
	private final Map<Id,AgentInfo> agentsMap = new HashMap<Id, AgentInfo>();
	private final Sim2D sim;
	private Agent2D calibrationAgent;
	private final Id calibrationAgentId;

	private final double[] times;
	private final Event[] events;
	private int pointer =0;
	private final Validator validator;

	public PhantomFloor(PhantomEvents phantomEvents, Id calibrationAgentId, Sim2D sim, List<Link> list, Scenario2DImpl scenario, Validator validator) {
		super(scenario,list,sim,true);
		this.sim = sim;
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
		while (this.pointer < this.times.length && this.times[this.pointer] <= time) {
			Event e = this.events[this.pointer];
			this.pointer++;

			if (e instanceof LinkLeaveEvent){
				this.sim.getEventsManager().processEvent(e);
				AgentInfo ai = this.agentsMap.get(((LinkLeaveEvent)e).getPersonId());
				Id id = ai.agent.chooseNextLinkId();
				// end of route
				if (id == null) {
					ai.agent.endLegAndAssumeControl(time);

				} else {
					ai.agent.notifyMoveOverNode();
				}
			} else if (e instanceof LinkEnterEvent){
				this.sim.getEventsManager().processEvent(e);
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
					ai.agent.setCurrentVelocity(vx, vy);
				}
				ai.agent.moveToPostion(event.getCoordinate());
				ai.lastUpdate = event.getTime();
				this.sim.getEventsManager().processEvent(e);
			}
		}



		if (this.calibrationAgent != null && moveAgentAndCheckForEndOfLeg(this.calibrationAgent, time)) {
			this.agents.remove(this.calibrationAgent);
			this.calibrationAgent = null;
		}
		Coordinate oldPos = this.validator.validate(time);
		if (oldPos != null && this.calibrationAgent != null) {
			if (checkForEndOfLinkReached(this.calibrationAgent, oldPos, this.calibrationAgent.getPosition(), time)) {
				this.agents.remove(this.calibrationAgent);
				this.calibrationAgent = null;
			}
		}
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


	@Override
	public void addAgent(Agent2D agent) {
		super.addAgent(agent);
		if (agent.getId().equals(this.calibrationAgentId)){
			this.calibrationAgent  = agent;
			this.validator.setCalibrationAgent(agent);
		}
		AgentInfo ai = new AgentInfo();
		ai.agent = agent;
		ai.lastUpdate = -1;
		this.agentsMap.put(agent.getId(), ai);

	}

	private static class AgentInfo {
		Agent2D agent;
		double lastUpdate;
	}

}
