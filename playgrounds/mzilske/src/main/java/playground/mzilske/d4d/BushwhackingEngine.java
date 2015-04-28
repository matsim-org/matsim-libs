package playground.mzilske.d4d;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.comparators.TeleportationArrivalTimeComparator;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.snapshotwriters.*;

import java.util.*;

public class BushwhackingEngine implements DepartureHandler, MobsimEngine, VisData {
	/**
	 * Includes all agents that have transportation modes unknown to the
	 * QueueSimulation (i.e. != "car") or have two activities on the same link
	 */
	private Queue<Tuple<Double, MobsimAgent>> teleportationList = new PriorityQueue<Tuple<Double, MobsimAgent>>(30, new TeleportationArrivalTimeComparator());
	private final LinkedHashMap<Id, TeleportationVisData> teleportationData = new LinkedHashMap<Id, TeleportationVisData>();
	private InternalInterface internalInterface;
	private final Set<Id> trackedAgents = new HashSet<Id>();
	private final Map<Id, MobsimAgent> agents = new HashMap<Id, MobsimAgent>();
	private boolean doVisualizeTeleportedAgents;
	private Collection<AgentSnapshotInfo> snapshots = new ArrayList<AgentSnapshotInfo>();
	
	
	public void removeTrackedAgent(Id id) {
		trackedAgents.remove(id);
	}

	public void addTrackedAgent(Id agentId) {
		trackedAgents.add(agentId);
	}


	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id linkId) {
		double arrivalTime = now + agent.getExpectedTravelTime();
		this.teleportationList.add(new Tuple<Double, MobsimAgent>(arrivalTime, agent));
		Id agentId = agent.getId();
		PlanAgent planAgent = (PlanAgent) agent;
		Leg leg = (Leg) planAgent.getCurrentPlanElement();
		List<PlanElement> planElements = planAgent.getCurrentPlan().getPlanElements();
		int idx = planElements.indexOf(leg);
		PlanElement fromAct = planElements.get(idx-1);
		PlanElement toAct = planElements.get(idx+1);
		double travTime = agent.getExpectedTravelTime();
		Coord fromCoord = getCoordFrom(fromAct);
		Coord toCoord = getCoordto(toAct);
		TeleportationVisData agentInfo = new TeleportationVisData(now, agentId, fromCoord, toCoord, travTime);
		this.teleportationData.put( agentId , agentInfo );
		return true;
	}


	private Coord getCoordFrom(PlanElement fromAct) {
		if (fromAct instanceof Activity) {
			Activity act = (Activity) fromAct;
			return act.getCoord();
		} else {
			Leg leg = (Leg) fromAct;
			return ((QSim) internalInterface.getMobsim()).getNetsimNetwork().getNetwork().getLinks().get(leg.getRoute().getEndLinkId()).getToNode().getCoord();
		}
	}
	
	
	private Coord getCoordto(PlanElement toAct) {
		if (toAct instanceof Activity) {
			Activity act = (Activity) toAct;
			return act.getCoord();
		} else {
			Leg leg = (Leg) toAct;
			return ((QSim) internalInterface.getMobsim()).getNetsimNetwork().getNetwork().getLinks().get(leg.getRoute().getStartLinkId()).getToNode().getCoord();
		}
	}

	public Collection<AgentSnapshotInfo> getTrackedAndTeleportedAgentsView() {
		return snapshots;
	}

	private void updateSnapshots() {
		snapshots.clear();
		for (TeleportationVisData agentInfo : teleportationData.values()) {
			if (this.doVisualizeTeleportedAgents || trackedAgents.contains(agentInfo.getId())) {
				snapshots.add(agentInfo);
			}
		}
		for (Id personId : trackedAgents) {
			Collection<AgentSnapshotInfo> positions = new ArrayList<AgentSnapshotInfo>();
			MobsimAgent agent = agents.get(personId);
			VisLink visLink = ((QSim) internalInterface.getMobsim()).getNetsimNetwork().getVisLinks().get(agent.getCurrentLinkId());
			visLink.getVisData().addAgentSnapshotInfo(positions);
			for (AgentSnapshotInfo position : positions) {
				if (position.getId().equals(personId)) {
					snapshots.add(position);
				}
			}
		}
	}

	@Override
	public void doSimStep(double time) {
		this.updateSnapshots(time);
		handleTeleportationArrivals();
	}
	
	private void updateSnapshots(double time) {
		snapshots.clear();
		if (this.doVisualizeTeleportedAgents) {
			for (TeleportationVisData teleportationVisData : teleportationData.values()) {
				teleportationVisData.calculatePosition(time);
				snapshots.add(teleportationVisData);
			}
		}
		for (Id personId : trackedAgents) {
			TeleportationVisData teleportationVisData = teleportationData.get(personId);
			if (teleportationVisData != null) {
				teleportationVisData.calculatePosition(time);
				snapshots.add(teleportationVisData);
			} else {
				Collection<AgentSnapshotInfo> positions = new ArrayList<AgentSnapshotInfo>();
				MobsimAgent agent = agents.get(personId);
				VisLink visLink = ((QSim) internalInterface.getMobsim()).getNetsimNetwork().getVisLinks().get(agent.getCurrentLinkId());
				visLink.getVisData().addAgentSnapshotInfo(positions);
				for (AgentSnapshotInfo position : positions) {
					if (position.getId().equals(personId)) {
						snapshots.add(position);
					}
				}
			}
		}
	}
	
	private void handleTeleportationArrivals() {
		double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		while (teleportationList.peek() != null) {
			Tuple<Double, MobsimAgent> entry = teleportationList.peek();
			if (entry.getFirst().doubleValue() <= now) {
				teleportationList.poll();
				MobsimAgent personAgent = entry.getSecond();
				personAgent.notifyArrivalOnLinkByNonNetworkMode(personAgent.getDestinationLinkId());
				double distance = ((Leg) ((PlanAgent) personAgent).getCurrentPlanElement()).getRoute().getDistance();
				((QSim) internalInterface.getMobsim()).getEventsManager().processEvent(new TeleportationArrivalEvent(this.internalInterface.getMobsim().getSimTimer().getTimeOfDay(), personAgent.getId(), distance));
				personAgent.endLegAndComputeNextState(now);
				this.teleportationData.remove(personAgent.getId());
				internalInterface.arrangeNextAgentState(personAgent) ;
			} else {
				break;
			}
		}
	}

	@Override
	public void onPrepareSim() {
		this.doVisualizeTeleportedAgents = ConfigUtils.addOrGetModule(((QSim) internalInterface.getMobsim()).getScenario().getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).isShowTeleportedAgents();
		for (MobsimAgent agent : ((VisMobsim) internalInterface.getMobsim()).getAgents()) {
			agents.put(agent.getId(), agent);
		}
	}

	@Override
	public void afterSim() {
		double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		for (Tuple<Double, MobsimAgent> entry : teleportationList) {
			MobsimAgent agent = entry.getSecond();
			EventsManager eventsManager = ((QSim) internalInterface.getMobsim()).getEventsManager();
			eventsManager.processEvent(new PersonStuckEvent(now, agent.getId(), agent.getDestinationLinkId(), agent.getMode()));
		}
		teleportationList.clear();
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	@Override
	public Collection<AgentSnapshotInfo> addAgentSnapshotInfo(Collection<AgentSnapshotInfo> snapshotList) {
		snapshotList.addAll(this.snapshots);
		return snapshotList;
	}

}