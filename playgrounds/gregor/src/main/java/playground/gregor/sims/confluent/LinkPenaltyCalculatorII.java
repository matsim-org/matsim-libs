package playground.gregor.sims.confluent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.AgentMoneyEventImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.router.util.TravelCost;


public class LinkPenaltyCalculatorII implements TravelCost, AfterMobsimListener, LinkEnterEventHandler, AgentStuckEventHandler, AgentArrivalEventHandler {

	private static final Logger log = Logger.getLogger(LinkPenaltyCalculatorII.class);

	private static final double STUCK_PENALTY = 7200;

	private static final double DISCOUNT = 1;

	private static final double CONFLUENCE_TRESHOLD = 1.1;

	private static final int MSA_OFFSET = 0;

	private static final double epsilon = 0.01;

	private enum MODE { confluent, nonconfluent}

	private final HashMap<Id,LinkInfo> linkInfos = new HashMap<Id, LinkInfo>();
	private final HashMap<Id,NodeInfo> nodeInfos = new HashMap<Id, NodeInfo>();
	private final List<AgentArrivalEvent> arrivedPersons = new ArrayList<AgentArrivalEvent>();

	private int it = 0;

	private final Network net;
	private final Population pop;

	public LinkPenaltyCalculatorII(Network net, Population pop) {
		this.net = net;
		this.pop = pop;
	}

	public double getLinkTravelCost(Link link, double time) {
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
		updateMSACin();
		if (this.it > 350){
			scorePlans(event.getControler().getEvents());
			resetCalculator();
			printStats();
			return;
		}
		updatePenalties();
		scorePlans(event.getControler().getEvents());
		resetCalculator();
		printStats();
	}



	private void updateMSACin() {
		double n = this.it + 1;
		double oldCoeff = n / (n+1);
		double newCoeff = 1 /(n+1);
		
		for (LinkInfo li : this.linkInfos.values()) {
			li.avgCin = oldCoeff * li.avgCin + newCoeff * li.cin;
			if (li.avgCin == 0) {
				System.out.println("");
			}
		}
		
	}

	private void printStats() {
		int conf = 0;
		int nonconf = 0;
		for (NodeInfo ni : this.nodeInfos.values()) {
			if (ni.mode == MODE.confluent) {
				conf++;
			} else if (ni.mode == MODE.nonconfluent) {
				nonconf++;
			}
		}
		double cumPen = 0.;
		for (LinkInfo li : this.linkInfos.values()) {
			cumPen += li.penalty;
		}
		log.info("Confluent nodes:" + conf + "   nonconfluent nodes:" + nonconf + "   penalty sum:" + cumPen);
	}

	private void scorePlans(EventsManager events) {
		int charged = 0;
		for (AgentArrivalEvent e : this.arrivedPersons) {
			Person pers = pop.getPersons().get(e.getPersonId());
			Plan plan = pers.getSelectedPlan();
			List<Id> links = ((NetworkRouteWRefs) ((PlanImpl) plan).getNextLeg(((PlanImpl) plan).getFirstActivity()).getRoute()).getLinkIds();
			for (Id id : links) {
				LinkInfo li = this.linkInfos.get(id);
				if (li.penalty > 0) {
					Double time = li.enterTimes.get(pers.getId());
					AgentMoneyEventImpl a = new AgentMoneyEventImpl(time,pers.getId(),li.penalty/-600);
					events.processEvent(a);
					charged++;
				} else if (li.penalty < 0){
										System.err.println(li.penalty);
				}
			}
		}
		log.error("ChargedAgents:" + charged);

	}

	private void resetCalculator() {
		this.arrivedPersons.clear();
		for (LinkInfo li : this.linkInfos.values()) {
			li.enterTimes.clear();
			li.cin = 0;
			li.cumuTT = 0.;
		}

	}

	public void reset(int iteration) {
		this.it = iteration;
	}

	public void handleEvent(AgentStuckEvent event) {
		LinkInfo li = getLinkInfo(event.getLinkId());
		double n = li.liIt++;
		double oldCoeff = n / (n+1);
		double newCoeff = 1 /(n+1);
		li.avgTT = oldCoeff * li.avgTT + newCoeff * STUCK_PENALTY;

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

	private void traceAgent(List<Id> links, Id pId, double arrivalTime) {
		double lastLeaveTime = arrivalTime;
		double cumuTime = 0;
		for (Id lId : links) {
			LinkInfo li = this.linkInfos.get(lId);
			if (li == null) {
				throw new RuntimeException("It seems to be that agent:" + pId + " has never entered link:" + lId + ", what is impossible!!");
			}
			Double enterTime = li.enterTimes.get(pId);
			if (enterTime == null) {
				throw new RuntimeException("It seems to be that agent:" + pId + " has never entered link:" + lId + ", what is impossible!!");
			}

			double tt = lastLeaveTime-enterTime;
			if (tt < 0) {
				throw new RuntimeException(""+tt);
			}
			cumuTime = DISCOUNT*cumuTime + tt;//(1-DISCOUNT) * ratio;
			double n = li.liIt++;
			double oldCoeff = n / (n+1);
			double newCoeff = 1 /(n+1);
			li.avgTT = oldCoeff * li.avgTT + newCoeff * cumuTime;
			li.cin++;
			li.cumuTT += cumuTime;
			lastLeaveTime = enterTime;
		}
	}

	private void updatePenalties() {
		int switchToNonConf = 0;
		int switchToConf = 0;
		int switchRandToConf = 0;
		int switchRandToNonConf = 0;
		int penSwitch = 0;
		for (Node node : this.net.getNodes().values()) {
			NodeInfo ni = getNodeInfo(node.getId());
			int denominator = 0;
			double nominator = 0.;
			List<LinkInfo> infos = new ArrayList<LinkInfo>();
			for (Link link : node.getOutLinks().values()) {
				LinkInfo li = this.linkInfos.get(link.getId());
				if (li != null) {
					infos.add(li);
					li.oldPen  = li.penalty;
					li.penalty = 0;
					if (ni.mode == MODE.confluent && li.oldPen > 0.) {
						continue;
					}
					denominator += li.cin;
					nominator += li.cumuTT;
				}
			}
			double n;
			if (denominator == 0) {
				//				n = ni.nonconfluentIt++;
				//				double oldCoef = n / (n+1);
				//				ni.avgNonConfluentTT = oldCoef * ni.avgNonConfluentTT;
				//				n = ni.confluentIt++;
				//				oldCoef = n / (n+1);
				//				ni.avgConfluentTT = oldCoef * ni.avgConfluentTT;
			} else {
				if (ni.mode == MODE.confluent) {
					n = ni.confluentIt;
					double oldCoef = n / (n+denominator);
					double newCoef = denominator /(n+denominator);
					ni.avgConfluentTT = oldCoef * ni.avgConfluentTT + newCoef * nominator/denominator;
					ni.confluentIt += denominator;
					//					ni.avgNonConfluentTT = oldCoef * ni.avgNonConfluentTT;
				} else {
					n = ni.nonconfluentIt;
					double oldCoef = n / (n+denominator);
					double newCoef = denominator /(n+denominator);
					ni.avgNonConfluentTT = oldCoef * ni.avgNonConfluentTT + newCoef * nominator/denominator;
					ni.nonconfluentIt += denominator;
					//					ni.avgConfluentTT = oldCoef * ni.avgConfluentTT; 
				}
			}
			if (Double.isNaN(ni.avgConfluentTT) || (Double.isNaN(ni.avgNonConfluentTT))) {
				throw new RuntimeException("ni was NaN!!!");
			}

			double prob = 1./(1+this.it);
						if (this.it > 320) {
							prob = 0.;
						}
			if ( MatsimRandom.getRandom().nextDouble() < prob) {
				if (MatsimRandom.getRandom().nextBoolean()) {
					switchRandToConf++;
					ni.mode = MODE.confluent;
					ni.lockTime = 0;
//					ni.confluentIt = 0;
				} else {
					switchRandToNonConf++;
					ni.mode = MODE.nonconfluent;
					ni.lockTime = 0;
//					ni.nonconfluentIt = 0;
				}


			} else if (ni.lockTime-- <= 0){
				if ((1+ni.avgConfluentTT) / (1+ni.avgNonConfluentTT) > CONFLUENCE_TRESHOLD) {
					if (ni.mode == MODE.confluent) {
						switchToNonConf++;
					}
					ni.mode = MODE.nonconfluent;
				} else {
					if (ni.mode == MODE.nonconfluent) {
						switchToConf++;
					}
					ni.mode = MODE.confluent;
				}
			}


			if (ni.mode == MODE.nonconfluent || infos.size() <= 1) {
				continue;
			}




			double best = Double.POSITIVE_INFINITY;
			int idx = -1;
			if (this.it < 150 && MatsimRandom.getRandom().nextDouble() < epsilon){
				idx = MatsimRandom.getRandom().nextInt(infos.size()-1);
			} else {
				for (int i = 0; i < infos.size(); i ++) {
					LinkInfo li = infos.get(i);
					if (Double.isNaN(li.avgTT)) {
						throw new RuntimeException("li was NaN!!");
					}
					if (li.avgTT < best) {
						best = li.avgTT;
						idx = i;
					}
				}
			}
			for (int i = 0; i < infos.size(); i ++) {
				LinkInfo li = infos.get(i);
				if (i == idx) {
					continue;
				}

//				li.penalty = STUCK_PENALTY;
//				if (li.avgCin == 0) {
//					System.out.println(li.avgCin);
//				}
				li.penalty = li.avgTT / li.avgCin;
				if (li.penalty != li.oldPen) {
					penSwitch++;
				}
			}

		}

		log.info("switchToNonConf:" + switchToNonConf + " switchToConf:" + switchToConf+ " switchRandToNonConf:" + switchRandToNonConf + " switchRandToConf:" + switchRandToConf + " penSwitch:" + penSwitch);

	}
	private NodeInfo getNodeInfo(Id id) {
		NodeInfo ret = this.nodeInfos.get(id);
		if (ret == null) {
			ret = new NodeInfo();
			this.nodeInfos.put(id, ret);
		}
		return ret;
	}

	private LinkInfo getLinkInfo(final Id id) {
		LinkInfo ret = this.linkInfos.get(id);
		if (ret == null) {
			ret = new LinkInfo();
			this.linkInfos.put(id, ret);
		}
		return ret;
	}

	private static class NodeInfo {
		public int lockTime = 0;
		MODE mode = MODE.nonconfluent;
		double avgConfluentTT = 0.;
		double avgNonConfluentTT = 0.;
		int confluentIt = 0;
		int nonconfluentIt = 0;
	}

	private static class LinkInfo {
		public double avgCin = 1;
		public double oldPen = 0;
		public double penalty = 0;
		HashMap<Id,Double> enterTimes = new HashMap<Id,Double>();
		int cin = 0;
		double cumuTT = 0.;
		double avgTT = 0.;
		int liIt = 0;
	}


}
