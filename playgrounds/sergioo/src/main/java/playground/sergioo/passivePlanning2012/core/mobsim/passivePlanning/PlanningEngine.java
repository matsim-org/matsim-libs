package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.PassivePlannerDriverAgent;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.agenda.PassivePlannerAgendaAgent;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.agenda.PassivePlannerTransitAgendaAgent;
import playground.sergioo.passivePlanning2012.core.population.PlaceSharer;
import playground.sergioo.passivePlanning2012.core.population.socialNetwork.SocialNetwork;
import playground.sergioo.passivePlanning2012.core.scenario.ScenarioSocialNetwork;

import java.util.*;
import java.util.Map.Entry;

public class PlanningEngine implements MobsimEngine, DepartureHandler {

	private Map<Id<Person>, PassivePlannerDriverAgent> planningAgents = new HashMap<Id<Person>, PassivePlannerDriverAgent>();
	private InternalInterface internalInterface;
	private QSim qSim;
	private HashMap<String, VehicleType> modeVehicleTypes;
	private Collection<String> mainModes;
	private double time;
	
	public PlanningEngine(QSim qSim) {
		this.qSim = qSim;
		this.modeVehicleTypes = new HashMap<String, VehicleType>();
		this.mainModes = qSim.getScenario().getConfig().qsim().getMainModes();
		for (String mode : mainModes)
			modeVehicleTypes.put(mode, VehicleUtils.getDefaultVehicleType());
	}
	@Override
	public void doSimStep(double time) {
		this.time = time;
		Set<Id<Person>> toDelete = new HashSet<Id<Person>>();
		for(Entry<Id<Person>, PassivePlannerDriverAgent> planningAgent:planningAgents.entrySet())
			if(planningAgent.getValue().isPlanned())
				toDelete.add(planningAgent.getKey());
		for(Id<Person> id:toDelete) {
			PassivePlannerDriverAgent planningAgent = planningAgents.remove(id);
			Set<String> seenModes = new HashSet<String>();
			for (PlanElement planElement : planningAgent.getBasePerson().getSelectedPlan().getPlanElements())
				if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					if (this.mainModes.contains(leg.getMode())) // only simulated modes get vehicles
						if (!seenModes.contains(leg.getMode())) { // create one vehicle per simulated mode, put it on the home location
							Id<Link> vehicleLink = findVehicleLink(planningAgent.getBasePerson().getSelectedPlan());
							qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create(id, Vehicle.class), modeVehicleTypes.get(leg.getMode())), vehicleLink);
							seenModes.add(leg.getMode());
						}
				}
			internalInterface.arrangeNextAgentState(planningAgent);
		}
	}
	public double getTime() {
		return time;
	}
	private Id<Link> findVehicleLink(Plan p) {
		// A more careful way to decide where this agent should have its vehicles created
		// than to ask agent.getCurrentLinkId() after creation.
		for (PlanElement planElement : p.getPlanElements()) {
			if (planElement instanceof Activity) {
				Activity activity = (Activity) planElement;
				if (activity.getLinkId() != null) {
					return activity.getLinkId();
				}
			} else if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (leg.getRoute().getStartLinkId() != null) {
					return leg.getRoute().getStartLinkId();
				}
			}
		}
		throw new RuntimeException("Don't know where to put a vehicle for this agent.");
	}
	public boolean containsAgentId(Id<Person> agentId) {
		return planningAgents.containsKey(agentId);
	}
	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		if(agent instanceof PassivePlannerDriverAgent && !((PassivePlannerDriverAgent)agent).isPlanned()) {
			planningAgents.put(agent.getId(), (PassivePlannerDriverAgent) agent);
			return true;
		}
		return false;
	}
	@Override
	public void onPrepareSim() {
		Map<Id<Person>, PassivePlannerDriverAgent> agents = new HashMap<Id<Person>, PassivePlannerDriverAgent>();
		for(MobsimAgent agent:qSim.getAgents())
			if(agent instanceof PassivePlannerAgendaAgent || agent instanceof PassivePlannerTransitAgendaAgent)
				agents.put(agent.getId(), (PassivePlannerDriverAgent) agent);
		SocialNetwork socialNetwork = ((ScenarioSocialNetwork)qSim.getScenario()).getSocialNetwork();
		for(PassivePlannerDriverAgent agent:agents.values())
			for(Id<Person> alterId:socialNetwork.getAlterIds(agent.getId())) {
				PlaceSharer placeSharer = getPlaceSharer(agents, alterId);
				if(placeSharer!=null)
					if(agent instanceof PassivePlannerAgendaAgent)
						((PassivePlannerAgendaAgent) agent).addKnownPerson(placeSharer);
					else
						((PassivePlannerTransitAgendaAgent) agent).addKnownPerson(placeSharer);
			}
	}
	private PlaceSharer getPlaceSharer(Map<Id<Person>, PassivePlannerDriverAgent> agents, Id<Person> alterId) {
		PassivePlannerDriverAgent agent = agents.get(alterId);
		if(agent instanceof PassivePlannerAgendaAgent)
			return ((PassivePlannerAgendaAgent)agent).getPlaceSharer();
		else if(agent instanceof PassivePlannerTransitAgendaAgent)
			return ((PassivePlannerTransitAgendaAgent)agent).getPlaceSharer();
		else
			return null;
	}
	@Override
	public void afterSim() {
		double now = qSim.getSimTimer().getTimeOfDay();
		for (PassivePlannerDriverAgent planningAgent:planningAgents.values()) {
			EventsManager eventsManager = qSim.getEventsManager();
			eventsManager.processEvent(new PersonStuckEvent(now, planningAgent.getId(), planningAgent.getState().equals(State.ACTIVITY)?planningAgent.getCurrentLinkId():planningAgent.getDestinationLinkId(), "empty"));
		}
		planningAgents.clear();
	}
	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

}
