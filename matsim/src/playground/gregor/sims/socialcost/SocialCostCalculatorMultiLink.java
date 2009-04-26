package playground.gregor.sims.socialcost;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.events.AgentMoneyEvent;
import org.matsim.core.events.AgentStuckEvent;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.events.handler.AgentStuckEventHandler;
import org.matsim.core.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.handler.LinkLeaveEventHandler;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationBeforeCleanupListener;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.trafficmonitoring.AbstractTravelTimeCalculator;
import org.matsim.core.utils.misc.IntegerCache;

public class SocialCostCalculatorMultiLink implements SocialCostCalculator,BeforeMobsimListener, QueueSimulationBeforeCleanupListener, LinkEnterEventHandler, LinkLeaveEventHandler, AgentStuckEventHandler{

	private static final Logger  log = Logger.getLogger(SocialCostCalculatorMultiLink.class);
	
	private final NetworkLayer network;
	private final int binSize;
	private final AbstractTravelTimeCalculator travelTimeCalculator;
	private final Population population;
	
	private Integer maxK;
	private final int minK;
	
	Map<Id,LinkInfo> linkInfos = new HashMap<Id,LinkInfo>();
	Set<Id> stuckedAgents = new HashSet<Id>();
	private int it;

	private final double discount;
	

	private final static int MSA_OFFSET = 0;
	
	public SocialCostCalculatorMultiLink(NetworkLayer network, int binSize,AbstractTravelTimeCalculator travelTimeCalculator, Population population) {
		this.network = network;
		this.binSize = binSize;
		this.minK = (int)(3 * 3600 / (double)binSize); //just a HACK needs to be fixed
		this.travelTimeCalculator = travelTimeCalculator;
		this.population = population;
		this.discount = MarginalCostControlerMultiLink.QUICKnDIRTY;
	}
	
	public double getSocialCost(Link link, double time) {
		LinkInfo li = this.linkInfos.get(link.getId());
		if (li == null) {
			return 0.;
		}
		
		LinkTimeCostInfo ltc = li.getLinkTimeCostInfo(getTimeBin(time));
		return Math.max(0,ltc.cost);
//		return 0.;
	}

	public void reset(int iteration) {
		this.it = iteration;
		
	}

	
	public void notifySimulationBeforeCleanup(
			QueueSimulationBeforeCleanupEvent e) {
		
		recalculateSocialCosts();
//		this.linkInfos.clear();

	}

	private void recalculateSocialCosts() {
		for (Person pers : this.population.getPersons().values()) {
			if ( this.stuckedAgents.contains(pers.getId())) {
				continue;
			}
			Plan plan = pers.getSelectedPlan();
			List<Id> links = plan.getNextLeg(plan.getFirstActivity()).getRoute().getLinkIds();
			traceAgentsRoute(links,pers.getId());
			
		}
		calcLinkTimeCosts();
		updateCosts();
		scorePlans();
		cleanUp();
	}

	private void cleanUp() {
		for (LinkInfo li : this.linkInfos.values()) {
			li.resetAgentEnterInfos();
		}
		
	}

	private void updateCosts() {
		double maxCost = 0;
		double minCost = Double.POSITIVE_INFINITY;
		double costSum = 0;
		int count = 0;
		if (this.it > MSA_OFFSET) {
			double n = this.it - MSA_OFFSET;
			double oldCoef = n / (n+1);
			double newCoef = 1 /(n+1);
			for (LinkInfo li : this.linkInfos.values()) {
				for (LinkTimeCostInfo lci : li.linkTimeCosts.values()) {
					lci.cost = newCoef * (lci.c1 + lci.c2) + oldCoef * lci.cost;
					if (lci.cost > maxCost) {
						maxCost = lci.cost;
					}
					if (lci.cost < minCost) {
						minCost = lci.cost;
					}
					costSum += lci.cost;
					count++;
					lci.in = 0;
					lci.c1 = 0;
					lci.c2 = 0;
					lci.linkDelay = 0;
					lci.out = 0; 
				}
			}
		}else {
			for (LinkInfo li : this.linkInfos.values()) {
				for (LinkTimeCostInfo lci : li.linkTimeCosts.values()) {
					lci.cost = lci.c1 + lci.c2;
					if (lci.cost > maxCost) {
						maxCost = lci.cost;
					}
					if (lci.cost < minCost) {
						minCost = lci.cost;
					}
					costSum += lci.cost;
					count++;
					lci.in = 0;
					lci.c1 = 0;
					lci.c2 = 0;
					lci.linkDelay = 0;
					lci.out = 0;
				}
			}			
		}
		log.info("maxCost: " + maxCost + " minCost: " + minCost + " avg: " + costSum/count);
	}

	private void scorePlans() {
		for (Person pers : this.population.getPersons().values()) {
			if ( this.stuckedAgents.contains(pers.getId())) {
				continue;
			}
			Plan plan = pers.getSelectedPlan();
			List<Id> links = plan.getNextLeg(plan.getFirstActivity()).getRoute().getLinkIds();
			double cost = 0;
			for (Id id : links) {
				LinkInfo li = this.linkInfos.get(id);
				Double enterTime = li.getAgentEnterTime(pers.getId());
				if (enterTime == null) {
					return;
				}
				cost += getSocialCost(this.network.getLink(id), li.getAgentEnterTime(pers.getId()));
			}
			AgentMoneyEvent e = new AgentMoneyEvent(this.maxK * this.binSize,pers.getId(),cost/-600);
			QueueSimulation.getEvents().processEvent(e);
		}
		
	}

	private void calcLinkTimeCosts() {
		for (Link link : this.network.getLinks().values()) {
			LinkInfo li = this.linkInfos.get(link.getId());
			if (li == null) { //Link has never been used by anny agent
				continue;
			}
			int kE = this.maxK;
			for (int k = this.maxK; k >= this.minK; k--) {
				Integer kInteger = IntegerCache.getInteger(k);
				
				
				double tauAk = this.travelTimeCalculator.getLinkTravelTime(link, k*this.binSize);
				double tauAFree = Math.ceil(link.getFreespeedTravelTime(k*this.binSize));
				if (tauAk <= tauAFree) {
					kE = k;
					continue;
				} 
				
				LinkTimeCostInfo ltc = li.getLinkTimeCostInfo(kInteger);
				double last = li.getLinkTimeCostInfo(IntegerCache.getInteger(k+1)).c2;
				double delay = li.getLinkTimeCostInfo(kInteger).linkDelay; 
				ltc.c2 =  last + delay;  
				ltc.c1 = Math.max(0, (kE-k)*this.binSize - tauAFree);
			}
		}
	}

	private void traceAgentsRoute(List<Id> links,Id agentId) {
		double agentDelay = 0;
		for (int i = links.size()-1; i >= 0; i--) {
			Id linkId = links.get(i);
			LinkInfo li = this.linkInfos.get(linkId); //Direct access
			Double enterTime = li.getAgentEnterTime(agentId);
			if (enterTime == null) {
				return;
			}
			
			Integer timeBin = getTimeBin(enterTime);
			LinkTimeCostInfo ltc = li.getLinkTimeCostInfo(timeBin);
			ltc.linkDelay += agentDelay;
			double tau = this.travelTimeCalculator.getLinkTravelTime(this.network.getLink(linkId),enterTime);
			
			Integer timeBin2 = getTimeBin(this.binSize*timeBin + tau);
			LinkTimeCostInfo ltc2 = li.getLinkTimeCostInfo(timeBin2);
			
			if (ltc2 != null && ltc2.out > 0){
				agentDelay = this.discount * agentDelay + ((double)ltc.in/this.binSize - (double)ltc.out/this.binSize) / ((double)ltc2.out/this.binSize);
			} 
		}
		AgentMoneyEvent e = new AgentMoneyEvent(this.maxK * this.binSize,agentId,agentDelay/-600);
		QueueSimulation.getEvents().processEvent(e);
		
	}

	public void handleEvent(LinkEnterEvent event) {
		LinkInfo li = getLinkInfo(event.getLinkId());
		li.incrementInFlow(getTimeBin(event.getTime()));
		li.setAgentEnterTime(event.getPersonId(), event.getTime());
	}

	public void handleEvent(LinkLeaveEvent event) {
		this.maxK = getTimeBin(event.getTime());
		getLinkInfo(event.getLinkId()).incrementOutFlow(this.maxK);
	}

	public void handleEvent(AgentStuckEvent event) {
		this.stuckedAgents.add(event.getPersonId());
		
	}
	
	private LinkInfo getLinkInfo(Id id) {
		LinkInfo li = this.linkInfos.get(id);
		if (li == null) {
			li = new LinkInfo();
			this.linkInfos.put(id, li);
		}
		return li;
	}
	
	private Integer getTimeBin(double time) {
		int slice = ((int) time)/this.binSize;
		return IntegerCache.getInteger(slice);
	}
	
	private static class LinkInfo {
		HashMap<Integer,LinkTimeCostInfo> linkTimeCosts = new HashMap<Integer, LinkTimeCostInfo>(500);
		HashMap<Id,Double> agentEnterInfos = new HashMap<Id, Double>(500);
		
		public void incrementInFlow(Integer timeBin) {
			LinkTimeCostInfo ltc = this.linkTimeCosts.get(timeBin);
			if (ltc == null) {
				ltc = new LinkTimeCostInfo();
				this.linkTimeCosts.put(timeBin, ltc);
			}
			ltc.in++;
		}

		public void incrementOutFlow(Integer timeBin) {
			LinkTimeCostInfo ltc = this.linkTimeCosts.get(timeBin);
			if (ltc == null) {
				ltc = new LinkTimeCostInfo();
				this.linkTimeCosts.put(timeBin, ltc);
			}
			ltc.out++;
		}
		
//		public LinkTimeFlowInfo getLinkTimeFlowInfo(Integer timeBin) {
//			 LinkTimeFlowInfo ltf = this.flowInfos.get(timeBin);
//			 if (ltf == null) {
//				 ltf = new LinkTimeFlowInfo();
//				 this.flowInfos.put(timeBin, ltf);
//			 }
//			 return ltf; 
//		}
		public void setAgentEnterTime(Id agentId, double enterTime) {
			this.agentEnterInfos.put(agentId, enterTime);
		}
		
		public Double getAgentEnterTime(Id agentId) {
			return this.agentEnterInfos.get(agentId);
		}
		
		public void resetAgentEnterInfos() {
			this.agentEnterInfos.clear();
		}
		
		public LinkTimeCostInfo getLinkTimeCostInfo(Integer timeBin) {
			LinkTimeCostInfo ltc = this.linkTimeCosts.get(timeBin);
			if (ltc == null) {
				ltc = new LinkTimeCostInfo();
				this.linkTimeCosts.put(timeBin, ltc);
			}
			return ltc;
		}
		
	}

	private static class LinkTimeCostInfo {
		public double cost;
		double c2 = 0;
		double c1 = 0;
		double linkDelay = 0;
		public int out = 0;
		public int in = 0;
	}

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		this.stuckedAgents.clear();
		this.linkInfos.clear();
		
	}


	



}
