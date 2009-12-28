package playground.gregor.sims.confluent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.AgentMoneyEventImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;


public class LinkPenaltyCalculator implements LinkPenalty, AfterMobsimListener {

	private static final double STUCK_PENALTY = 1*3600;

	private static final int MSA_OFFSET = 0;
	
	private static final double epsilon = 0.01;
	
	private final HashMap<Id,LinkInfo> linkInfos = new HashMap<Id, LinkInfo>();
	private final List<AgentArrivalEvent> arrivedPersons = new ArrayList<AgentArrivalEvent>();

	private int it;

	private NetworkLayer net;
	private final Population pop;
	
	public LinkPenaltyCalculator(NetworkLayer net, Population pop) {
		this.net = net;
		this.pop = pop;
	}
	
	public double getLinkCost(Link link) {
		LinkInfo li = this.linkInfos.get(link.getId());
		if (li == null){
			return 0;
		}
		return li.penalty;
	}

	public void handleEvent(LinkEnterEvent event) {
		LinkInfo info = getLinkInfo(event.getLinkId());
		info.enterTimes.put(event.getPersonId(), event.getTime());
	}

	public void notifyAfterMobsim(AfterMobsimEvent event) {
		updateAvgTT();
		updateMSATT();
		if (this.it < 350){
			updatePenalties();
		}
		scorePlans(event.getControler().getEvents());
		resetCalculator();
	}


	private void scorePlans(EventsManager events) {
		for (AgentArrivalEvent e : this.arrivedPersons) {
			Person pers = this.pop.getPersons().get(e.getPersonId());
			Plan plan = pers.getSelectedPlan();
			List<Id> links = ((NetworkRouteWRefs) ((PlanImpl) plan).getNextLeg(((PlanImpl) plan).getFirstActivity()).getRoute()).getLinkIds();
			for (Id id : links) {
				LinkInfo li = this.linkInfos.get(id);
				if (li.penalty > 0) {
					Double time = li.enterTimes.get(pers.getId());
					AgentMoneyEventImpl a = new AgentMoneyEventImpl(time,pers.getId(),STUCK_PENALTY/-600);
					events.processEvent(a);
				}
			}
		}
		
	}

	private void resetCalculator() {
		this.arrivedPersons.clear();
		for (LinkInfo li : this.linkInfos.values()) {
			li.enterTimes.clear();
			li.cin = 0;
			li.cumTT = 0;
		}
		
	}

	public void reset(int iteration) {
		this.it = iteration;
	}

	public void handleEvent(AgentStuckEvent event) {
		LinkInfo info = getLinkInfo(event.getLinkId());
		info.cin++;
		info.cumTT += STUCK_PENALTY;
	}
	
	public void handleEvent(AgentArrivalEvent event) {
		this.arrivedPersons.add(event);
		
	}
	
	private void updateAvgTT() {
		for (AgentArrivalEvent e : this.arrivedPersons) {
			Person pers = this.pop.getPersons().get(e.getPersonId());
			Plan plan = pers.getSelectedPlan();
			List<Id> links = ((NetworkRouteWRefs) ((PlanImpl) plan).getNextLeg(((PlanImpl) plan).getFirstActivity()).getRoute()).getLinkIds();
			traceAgent(links,pers.getId(),e.getTime());
		}
		
	}
	private void updateMSATT() {
		
		double n = this.it - MSA_OFFSET;
		double oldCoef = n / (n+1);
		double newCoef = 1 /(n+1);
		for (LinkInfo li : this.linkInfos.values()) {
			li.avgTT = oldCoef * li.avgTT + newCoef * ((double)li.cumTT / li.cin);
		}
		
	}
	
	private void traceAgent(List<Id> links, Id pId, double arrivalTime) {
		for (Id lId : links) {
			LinkInfo li = this.linkInfos.get(lId);
			if (li == null) {
				throw new RuntimeException("It seems to be that agent:" + pId + " has never entered link:" + lId + ", what is impossible!!");
			}
			Double enterTime = li.enterTimes.get(pId);
			if (enterTime == null) {
				throw new RuntimeException("It seems to be that agent:" + pId + " has never entered link:" + lId + ", what is impossible!!");
			}			
			li.cin++;
			li.cumTT += arrivalTime-enterTime;
		}
	}

	private void updatePenalties() {
		for (NodeImpl node : this.net.getNodes().values()) {
			List<LinkInfo> infos = new ArrayList<LinkInfo>();
			for (Link link : node.getOutLinks().values()) {
				LinkInfo li = this.linkInfos.get(link.getId());
				if (li != null) {
					li.penalty = 0;
					infos.add(li);
				}
			}
			if (infos.size() <= 1) {
				continue;
			}
			int idx = -1;
			if (MatsimRandom.getRandom().nextDouble() < epsilon){
				idx = MatsimRandom.getRandom().nextInt(infos.size()-1);
			} else {
				double best = Double.POSITIVE_INFINITY;
				for (int i = 0; i < infos.size(); i ++) {
					LinkInfo li = infos.get(i);
					if (li.avgTT < best) {
						best = li.avgTT;
						idx = i;
					}
				}
			}
			for (int i = 0; i < infos.size(); i ++) {
				if (i == idx) {
					continue;
				}
				LinkInfo li = infos.get(i);
				li.penalty = STUCK_PENALTY;
			}
		}
		
	}
	private LinkInfo getLinkInfo(final Id id) {
		LinkInfo ret = this.linkInfos.get(id);
		if (ret == null) {
			ret = new LinkInfo();
			this.linkInfos.put(id, ret);
		}
		return ret;
	}
	
	private static class LinkInfo {
		public double penalty = 0;
		HashMap<Id,Double> enterTimes = new HashMap<Id,Double>();
		int cin = 0;
		double cumTT = 0.;
		double avgTT = 0.;
	}


}
