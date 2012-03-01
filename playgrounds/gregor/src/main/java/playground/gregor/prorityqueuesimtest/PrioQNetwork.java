package playground.gregor.prorityqueuesimtest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.ptproject.qsim.InternalInterface;

import playground.gregor.microcalibratedqsim.LinkVelocityClassifier;
import playground.gregor.microcalibratedqsim.SVMClassifier;
import playground.gregor.sim2d_v2.events.XYVxVyEventsFileReader;
import playground.gregor.sim2d_v2.statistics.OnLinkStatistics;
import playground.gregor.sim2d_v2.statistics.OnLinkStatistics.AgentInfo;

public class PrioQNetwork {

	
	private final Map<Id,PrioQLink> links = new LinkedHashMap<Id,PrioQLink>();
	private final Map<Id,LinkVelocityClassifier> cls = new LinkedHashMap<Id,LinkVelocityClassifier>();
	
	OnLinkStatistics lees = new OnLinkStatistics();
	private final EventsManager mgr;
	private final InternalInterface internalInterface;
	
	public PrioQNetwork(Scenario scenario, EventsManager eventsManager,
			InternalInterface internalInterface) {
		this.mgr = eventsManager;
		this.internalInterface = internalInterface;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			PrioQLink pqLink = new PrioQLink(link.getLength(),link.getId());
			this.links.put(link.getId(), pqLink);
			SVMClassifier cl = new SVMClassifier();
			
			int sz = link.getToNode().getInLinks().size();
			sz += link.getToNode().getOutLinks().size();
			sz += link.getFromNode().getInLinks().size();
			Id [] ids = new Id[sz];
			int idx =0;
			for (Link ll : link.getToNode().getInLinks().values()) {
				ids[idx++] = ll.getId();
			}
			for (Link ll : link.getToNode().getOutLinks().values()) {
				ids[idx++] = ll.getId();
			}
			for (Link ll : link.getFromNode().getInLinks().values()) {
				ids[idx++] = ll.getId();
			}
			cl.setIds(ids);
			this.lees.addObservationLinkId(link.getId());
			this.cls.put(link.getId(), cl);
			
		}
		init();
	}
	
	
	private void init() {
		
		EventsManager emngr = EventsUtils.createEventsManager();
		emngr.addHandler(this.lees);
		new XYVxVyEventsFileReader(emngr).parse("/Users/laemmel/devel/gr90/input/events_walk2d.xml.gz");
		for (Entry<Id, LinkVelocityClassifier> entry : this.cls.entrySet()) {
			init(entry.getValue(),entry.getKey());
		}
	}


	private void init(LinkVelocityClassifier cl, Id id) {
		List<AgentInfo> ais = this.lees.getAndRemoveAgentInfos(id);
		double ll = this.links.get(id).getLength();
		Id[] ids = cl.getIds();
		for (AgentInfo ai : ais) {
			double [] predictors = new double [ids.length];
			double t = ai.enterTime;
			double v = ll/(ai.travelTime);
			for (int i = 0; i < ids.length; i++) {
				Id pid = ids[i];
				double[] tmp = this.lees.getOnLinkHistory(pid, t, 1);
				predictors[i] = tmp[0];
			}
			cl.addInstance(predictors, v);
		}
		cl.addInstance(new double[ids.length], 1.34);
		cl.build();
	}


	public void move(double sim2dTime) {
		for (PrioQLink link : this.links.values()) {
			PrioQAgent agent = link.tryPoll(sim2dTime);
			while (agent != null) {
				this.mgr.processEvent(new LinkLeaveEventImpl(sim2dTime,agent.getAgent().getId(), link.getId(), agent.getAgent().getId()));
				
				Id id = agent.getAgent().chooseNextLinkId();
				if (id == null) {
					agent.getAgent().endLegAndComputeNextState(sim2dTime);
					this.internalInterface.arrangeNextAgentState(agent.getAgent());
					agent = link.tryPoll(sim2dTime);
					continue;
				}
				double travelTime = getCurrentTravelTime(id);
				agent.setNextLinkLeaveTime(travelTime+sim2dTime);
				this.links.get(id).push(agent);
				
				agent.getAgent().notifyMoveOverNode(id);
				this.mgr.processEvent(new LinkEnterEventImpl(sim2dTime,agent.getAgent().getId(), id, agent.getAgent().getId()));
				agent = link.tryPoll(sim2dTime);
			}
		}
		
		for (PrioQLink link : this.links.values()) {
			link.update();
		}
		
	}

	private double getCurrentTravelTime(Id id) {
		
		LinkVelocityClassifier cl = this.cls.get(id);
		Id[] ids = cl.getIds();
		double [] predictors = new double[ids.length];
		for (int i = 0; i < ids.length; i++) {
			predictors[i] = this.links.get(ids[i]).getNumOfAgentsOnLink();
		}
		double v = cl.getEstimatedVelocity(predictors);
		
		return this.links.get(id).getLength() / v;
	}

	public void agentDepart(MobsimDriverAgent agent) {
		Id id = agent.getCurrentLinkId();
		this.links.get(id).push(new PrioQAgent(agent));
	}
	

}
