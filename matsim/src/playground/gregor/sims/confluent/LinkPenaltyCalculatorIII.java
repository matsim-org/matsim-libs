package playground.gregor.sims.confluent;

import java.util.ArrayList;
import java.util.Collections;
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
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.misc.Time;


public class LinkPenaltyCalculatorIII implements LinkPenalty, AfterMobsimListener {

	private static final double STUCK_PENALTY = 1*3600;

	private static final int MSA_OFFSET = 0;
	
	private static final double epsilon = 0.01;
	private static final double gamma = 0.5;
	
	private final HashMap<Id,LinkInfo> linkInfos = new HashMap<Id, LinkInfo>();
	private final List<AgentArrivalEvent> arrivedPersons = new ArrayList<AgentArrivalEvent>();

	private int it;

	private NetworkLayer net;
	private Population pop;
	
	public LinkPenaltyCalculatorIII(NetworkLayer net, Population pop) {
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
			li.cumLTT = 0;
//			li.avgTT = 0;
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
			Collections.reverse(links);
			traceAgent(links,pers.getId(),e.getTime());
		}
		
	}
	private void updateMSATT() {
		

		for (LinkInfo li : this.linkInfos.values()) {
			if (li.cin == 0) {
				continue;
			}
			double n = li.liIt++;
			double oldCoef = n / (n+1);
			double newCoef = 1 /(n+1);
			li.avgTT = oldCoef * li.avgTT + newCoef * ((double)li.cumTT / li.cin);
			li.avgLTT= oldCoef * li.avgLTT  + newCoef * ((double)li.cumLTT / li.cin);
//			li.avgTT = ((double)li.cumTT / li.cin);
		}
		
	}
	
	private void traceAgent(List<Id> links, Id pId, double arrivalTime) {
		double old = arrivalTime;
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
			li.cumLTT += old - enterTime;
			old = enterTime;
		}
	}

	private void updatePenalties() {
		for (NodeImpl node : this.net.getNodes().values()) {
			List<LinkInfo> infos = new ArrayList<LinkInfo>();
			int cinAll = 0;
			for (Link link : node.getOutLinks().values()) {
				LinkInfo li = this.linkInfos.get(link.getId());
				if (li != null) {
//					li.penalty = 0;
					cinAll += li.cin;
					infos.add(li);
				}
			}
			if (infos.size() <= 1) {
				continue;
			}
			
			for (LinkInfo li : infos) {
				double n = li.liIt++;
				double oldCoef = n / (n+1);
				double newCoef = 1 /(n+1);
//				double frac = gamma/((double)li.cin/(double)cinAll);
				
				double current =li.avgLTT/li.freeTT -1;
				if (current < 0) {
					current = 0;
				}
				li.penalty = oldCoef * li.penalty + newCoef * current;
			}
		}
		
	}
	private LinkInfo getLinkInfo(final Id linkId) {
		LinkInfo ret = this.linkInfos.get(linkId);
		if (ret == null) {
			ret = new LinkInfo();
			ret.freeTT = this.net.getLinks().get(linkId).getFreespeedTravelTime(Time.UNDEFINED_TIME);
			this.linkInfos.put(linkId, ret);
		}
		return ret;
	}
	
	private static class LinkInfo {
		public double avgLTT = 0;
		public double freeTT;
		public double penalty = 0;
		HashMap<Id,Double> enterTimes = new HashMap<Id,Double>();
		int cin = 0;
		double cumTT = 0.;
		double cumLTT = 0.;
		double avgTT = 0.;
		int liIt = 0;
	}


}
