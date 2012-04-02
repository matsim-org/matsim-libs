package playground.gregor.prorityqueuesimtest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;

import playground.gregor.microcalibratedqsim.LinkVelocityClassifier;
import playground.gregor.microcalibratedqsim.WekaEstimator;
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
			//			LinkVelocityClassifier cl = new SVMClassifier();
			LinkVelocityClassifier cl = new WekaEstimator();

			Set<Id> temp = new HashSet<Id>(); 
			for (Link ll : link.getToNode().getInLinks().values()) {
				temp.add(ll.getId());
			}
			for (Link ll : link.getToNode().getOutLinks().values()) {
				temp.add(ll.getId());
			}
			for (Link ll : link.getFromNode().getInLinks().values()) {
				temp.add(ll.getId());
			}
			for (Link ll : link.getFromNode().getOutLinks().values()) {
				temp.add(ll.getId());
			}
			Id [] ids = new Id[temp.size()];
			int idx =0;
			for (Id id : temp) {
				ids[idx++] = id;
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
//				new XYVxVyEventsFileReader(emngr).parse("/Users/laemmel/devel/gr90/input/events.xml");
		new XYVxVyEventsFileReader(emngr).parse("/Users/laemmel/devel/gr90/input/events_walk2d.xml.gz");
		for (Entry<Id, LinkVelocityClassifier> entry : this.cls.entrySet()) {
			//			if (entry.getKey().toString().equals("4")){
			//				try {
			//					dumpInit(entry.getValue(),entry.getKey());
			//				} catch (IOException e) {
			//					// TODO Auto-generated catch block
			//					e.printStackTrace();
			//				}
			//			}
			init(entry.getValue(),entry.getKey());
		}
	}


	private void dumpInit(LinkVelocityClassifier cl, Id id) throws IOException {
		List<AgentInfo> ais = this.lees.getAndRemoveAgentInfos(id);
		double ll = this.links.get(id).getLength();

		BufferedWriter w = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/gr90/dbg/link_stats.txt")));

		Id[] ids = cl.getIds();
		w.append("v" + id.toString());
		for (Id iid : ids) {
			w.append(",l" + iid.toString());
		}
		w.append('\n');

		for (AgentInfo ai : ais) {
			double [] predictors = new double [ids.length];
			double t = ai.enterTime;
			double v = ll/(ai.travelTime);
			w.append(v+"");
			for (int i = 0; i < ids.length; i++) {
				Id pid = ids[i];
				double[] tmp = this.lees.getOnLinkHistory(pid, t, 1);
				predictors[i] = tmp[0];
				w.append(","+tmp[0] );
			}
			w.append('\n');
		}
		w.close();

	}


	private void init(LinkVelocityClassifier cl, Id id) {
		List<AgentInfo> ais = this.lees.getAndRemoveAgentInfos(id);
		double ll = this.links.get(id).getLength();
		Id[] ids = cl.getIds();

		for (AgentInfo ai : ais) {
			if (MatsimRandom.getRandom().nextDouble() > 0.25) {
				continue;
			}
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
//		if (ais.size() == 0) {
			cl.addInstance(new double[ids.length], 1.34);
			double [] full = new double[ids.length];
			Arrays.fill(full, 10000);
			cl.addInstance(new double[ids.length], 0.001);
//		}
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
