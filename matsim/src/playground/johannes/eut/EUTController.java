/* *********************************************************************** *
 * project: org.matsim.*
 * EUTController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.johannes.eut;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentWait2Link;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentWait2LinkI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.Link;
import org.matsim.plans.Person;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.selectors.KeepSelected;
import org.matsim.replanning.selectors.PlanSelectorI;
import org.matsim.router.util.TravelTimeI;
import org.matsim.utils.collections.Tuple;
import org.matsim.utils.io.IOUtils;
import org.matsim.withinday.WithindayControler;
import org.matsim.withinday.WithindayCreateVehiclePersonAlgorithm;
import org.matsim.withinday.mobsim.WithindayQueueSimulation;
import org.matsim.withinday.trafficmanagement.TrafficManagement;

/**
 * @author illenberger
 *
 */
public class EUTController extends WithindayControler {
	
//	private static final Logger log = Logger.getLogger(EUTController.class);
	
	private TravelTimeMemory ttmemory;
	
//	private TTDecorator guidedTTs;
	
	private TravelTimeI ttcalc;
//	private EventBasedTTProvider ttcalc;

	public final static double incidentProba = 0.1;
	
	private EUTRouterAnalyzer routerAnalyzer;
	
	/**
	 * @param args
	 */
	public EUTController(String[] args) {
		super(args);
		setOverwriteFiles(true);
		
	}

	@Override
	protected StrategyManager loadStrategyManager() {
		ttmemory = new TravelTimeMemory();
		TimevariantTTStorage storage = ttmemory.makeTTStorage(getTravelTimeCalculator(), network, getTraveltimeBinSize(), 0, 86400);
		ttmemory.appendNewStorage(storage);
		
		StrategyManager manager = new StrategyManager();
		manager.setMaxPlansPerAgent(1);
		
		PlanSelectorI selector = new KeepSelected();
		PlanStrategy strategy = new PlanStrategy(selector);
		EUTReRoute eutReRoute = new EUTReRoute(getNetwork(), ttmemory);
		routerAnalyzer = new EUTRouterAnalyzer(eutReRoute.getUtilFunction());
		eutReRoute.setRouterAnalyzer(routerAnalyzer);
		strategy.addStrategyModule(eutReRoute);
		manager.addStrategy(strategy, 0.05);
		
		strategy = new PlanStrategy(selector);
		manager.addStrategy(strategy, 0.95);
		
		return manager;
//		return super.loadStrategyManager();
	}

	@Override
	protected void setup() {
		setTraveltimeBinSize(60);
		super.setup();
		
		ttcalc = new EstimReactiveLinkTT();
		events.addHandler((EstimReactiveLinkTT)ttcalc);
		addControlerListener(new TTCalculatorController());
		addControlerListener(new NetworkModifier());
		
		addControlerListener(routerAnalyzer);
		
		TripAndScoreStats stats = new TripAndScoreStats(routerAnalyzer); 
		addControlerListener(stats);
		events.addHandler(stats);

		factory = new GuidedAgentFactory(network, config.charyparNagelScoring(), ttcalc);
		((GuidedAgentFactory)factory).setRouteAnalyzer(routerAnalyzer);
		this.addControlerListener(((GuidedAgentFactory)factory).router);
		
		LinkTTVarianceStats linkStats = new LinkTTVarianceStats(getTravelTimeCalculator(), 25200, 32400, 60);
		addControlerListener(linkStats);
	}

	@Override
	protected void runMobSim() {
//		factory = new GuidedAgentFactory(network, config.charyparNagelScoring(), getTravelTimeCalculator());
		((GuidedAgentFactory)factory).random = new Random(1);
		
		config.withinday().addParam("contentThreshold", "1");
		config.withinday().addParam("replanningInterval", "1");
		WithindayCreateVehiclePersonAlgorithm vehicleAlgo = new WithindayCreateVehiclePersonAlgorithm(this);

		//build the queuesim
		WithindayQueueSimulation sim = new WithindayQueueSimulation((QueueNetworkLayer)this.network, this.population, this.events, this);
		sim.setVehicleCreateAlgo(vehicleAlgo);
		trafficManagement = new TrafficManagement();
		//run the simulation
		long time = System.currentTimeMillis();
//		QueueSimulation sim = new QueueSimulation((QueueNetworkLayer)this.network, this.population, this.events);
		sim.run();
		System.err.println("Mobsim took " + (System.currentTimeMillis() - time) +" ms.");
	}

	private class TTCalculatorController implements IterationStartsListener, IterationEndsListener {

//		private TravelTimeCalculatorArray myttcalc;
//		
		public void notifyIterationStarts(IterationStartsEvent event) {
//			ttcalc.resetTravelTimes();
//			myttcalc = new TravelTimeCalculatorArray(EUTController.this.getNetwork());
//			EUTController.this.events.addHandler(myttcalc);
		}

		public void notifyIterationEnds(IterationEndsEvent event) {
//			EUTController.this.events.removeHandler(myttcalc);
//			provider.appendTTSet(myttcalc);
			TimevariantTTStorage storage = ttmemory.makeTTStorage(getTravelTimeCalculator(), network, getTraveltimeBinSize(), 0, 86400);
//			log.info(storage.toString());
			ttmemory.appendNewStorage(storage);
			
		}
		
	}
	
	private class NetworkModifier implements IterationStartsListener, IterationEndsListener {
		
//		private boolean changedcap = false;
		
		private List<QueueLink> changedCaps = new LinkedList<QueueLink>();
		
		private BufferedWriter writer;
		
		private List<QueueLink> links = new LinkedList<QueueLink>();
		public NetworkModifier() {
			
			links.add((QueueLink) EUTController.this.getNetwork().getLink("800"));
			links.add((QueueLink) EUTController.this.getNetwork().getLink("1100"));
			links.add((QueueLink) EUTController.this.getNetwork().getLink("1400"));
			try {
				writer = new BufferedWriter(new FileWriter(getOutputFilename("incidents.txt")));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public void notifyIterationStarts(IterationStartsEvent event) {
			/*
			 * Reduce capacity here...
			 */
			try {
				
				writer.write(String.valueOf(event.getIteration()));
				writer.write("\t");

				for (QueueLink link : links) {
					Gbl.random.nextDouble();
					if (Gbl.random.nextDouble() < incidentProba
							&& event.getIteration() > 9) {
						
						link.changeSimulatedFlowCapacity(0.5);
						changedCaps.add(link);

//						guidedTTs.addAccidantLink(link);
						
						writer.write("\t");
						writer.write(link.getId().toString());
					}

				}
				
				writer.newLine();
				writer.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void notifyIterationEnds(IterationEndsEvent event) {
			for (QueueLink link : links) {
				if (changedCaps.contains(link)) {
					link.changeSimulatedFlowCapacity(1.0 / 0.5);
					changedCaps.remove(link);
					
//					guidedTTs.removeAccidantLink(link);
				}
			}

		}

	}
	
	private class TripDurationWriter implements IterationEndsListener, EventHandlerAgentWait2LinkI, EventHandlerAgentArrivalI {
		
		private BufferedWriter writer;
		
		private List<Tuple<Double, String>> tripDursSum = new LinkedList<Tuple<Double, String>>();
		
		private Map<Person, Double> tripdurs = new HashMap<Person, Double>();
				
		public TripDurationWriter() {
			EUTController.this.events.addHandler(this);
			try {
				writer = new BufferedWriter(new FileWriter(EUTController.getOutputFilename("tripstats.txt")));
				writer.write("Iteration\tavr\tguided\tunguided");
				writer.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void notifyIterationEnds(IterationEndsEvent event) {
			List<Tuple<Double, String>> guidedAvr = new LinkedList<Tuple<Double,String>>();
			List<Tuple<Double, String>> unguidedAvr = new LinkedList<Tuple<Double,String>>();

			for(Tuple<Double, String> t : tripDursSum) {
				if(routerAnalyzer.getGuidedPersons().contains(t.getSecond())) {
					guidedAvr.add(t);
				} else {
					unguidedAvr.add(t);
				}
			}
						
			int avr = calcAvr(tripDursSum);
			int guidedAvrDur = calcAvr(guidedAvr);
			int unguidedAvrDur = calcAvr(unguidedAvr);	
				
			try {
				writer.write(String.valueOf(event.getIteration()));
				writer.write("\t");
				writer.write(String.valueOf(avr));
				writer.write("\t");
				writer.write(String.valueOf(guidedAvrDur));
				writer.write("\t");
				writer.write(String.valueOf(unguidedAvrDur));
				writer.newLine();
				writer.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		private int calcAvr(List<Tuple<Double, String>> vals) {
			double sum = 0;
			for(Tuple<Double, String> d : vals)
				sum += d.getFirst();
			
			return (int) (sum/vals.size());
		}

		public void reset(int iteration) {
			tripdurs = new HashMap<Person, Double>();
			tripDursSum = new LinkedList<Tuple<Double, String>>();
//			GuidedAgentFactory.guidedAgents = new LinkedList<String>();
		}

		public void handleEvent(EventAgentArrival event) {
			Double deptime = tripdurs.get(event.agent);
			if(deptime != null) {
				tripDursSum.add(new Tuple<Double, String>(event.time - deptime, event.agent.getId().toString()));
				tripdurs.remove(event.agent);
			}
		}

		public void handleEvent(EventAgentWait2Link event) {
			tripdurs.put(event.agent, event.time);
		}
	
	}
	
//	private class RouteDistribution implements IterationEndsListener, EventHandlerLinkEnterI {
//
//		private int safeRouteCnt = 0;
//		private int riskyRouteCnt = 0;
//		
//		private BufferedWriter writer;
//		
//		public RouteDistribution() {
//			try {
//				writer = new BufferedWriter(new FileWriter(EUTController.getOutputFilename("routefrac.txt")));
//				writer.write("Iteration\tn_safe\tn_risky\tn_riskaverse");
//				writer.newLine();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//			events.addHandler(this);
//		}
//		
//		public void notifyIterationEnds(IterationEndsEvent event) {
//			try {
//				writer.write(event.getIteration()+"\t"+safeRouteCnt+"\t"+riskyRouteCnt);
//				writer.write("\t"+EUTRouter.riskCount);
//				writer.newLine();
//				writer.flush();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			
//		}
//		public void handleEvent(EventLinkEnter event) {
//			if(event.link.getId().toString().equals("5"))
//				riskyRouteCnt++;
//			else if(event.link.getId().toString().equals("4"))
//				safeRouteCnt++;
//			
//		}
//		public void reset(int iteration) {
//			riskyRouteCnt = 0;
//			safeRouteCnt = 0;
//			EUTRouter.riskCount = 0;
//			
//		}
//		
//	}
	
//	private class RouteCosts implements IterationEndsListener {
//
//		private BufferedWriter writer;
//		
//		public RouteCosts() {
//			try {
//				writer = new BufferedWriter(new FileWriter(EUTController.getOutputFilename("routecosts.txt")));
//				writer.write("Iteration\tcosts_safe_avr\tcosts_risky_avr\triskytt_avr\tsafett_avr\tsafe_ce\trisky_ce");
//				writer.newLine();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//		public void notifyIterationEnds(IterationEndsEvent event) {
//			try {
//				writer.write(String.valueOf(event.getIteration()));
//				writer.write("\t");
//				writer.write(String.valueOf(calcAvr(EUTRouter.safeCostsAvr)));
//				writer.write("\t");
//				writer.write(String.valueOf(calcAvr(EUTRouter.riskyCostsAvr)));
//				writer.write("\t");
//				writer.write(String.valueOf(calcAvr(EUTRouter.riskyTravTimeAvr)));
//				writer.write("\t");
//				writer.write(String.valueOf(calcAvr(EUTRouter.safeTravTimeAvr)));
//				writer.write("\t");
//				writer.write(String.valueOf(calcAvr(EUTRouter.safeCE)));
//				writer.write("\t");
//				writer.write(String.valueOf(calcAvr(EUTRouter.riskyCE)));
//				writer.newLine();
//				writer.flush();
//				
//				EUTRouter.safeCostsAvr = new LinkedList<Double>();
//				EUTRouter.riskyCostsAvr = new LinkedList<Double>();
//				EUTRouter.riskyTravTimeAvr = new LinkedList<Double>();
//				EUTRouter.safeTravTimeAvr = new LinkedList<Double>();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//		
//		private double calcAvr(List<Double> vals) {
//			double sum = 0;
//			for(Double val : vals) {
////				try {
//				sum += val;
////				} catch (Exception e) {
////					e.printStackTrace();
////				}
//			}
//			
//			return sum/(double)vals.size();
//		}
//	}
	
//	private class GuidedAgentsWriter implements IterationEndsListener {
//
//		private BufferedWriter writer;
//		
//		public GuidedAgentsWriter() {
//			try {
//				writer = new BufferedWriter(new FileWriter(EUTController.getOutputFilename("guidedagents.txt")));
//				writer.write("Iteration\tcount\tagent_ids");
//				writer.newLine();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//		
//		public void notifyIterationEnds(IterationEndsEvent event) {
//			try {
//				writer.write(String.valueOf(event.getIteration()));
//				writer.write("\t");
//				writer.write(String.valueOf(GuidedAgentFactory.guidedAgents.size()));
//				
//				for(String id : GuidedAgentFactory.guidedAgents) {
//					writer.write("\t");
//					writer.write(id);
//				}
//				writer.newLine();
//				writer.flush();
//				
//				GuidedAgentFactory.guidedAgents = new LinkedList<String>();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			
//		}
		
//	}
	
//	private class ScoreStats implements IterationStartsListener, IterationEndsListener, ShutdownListener {
//
//		private List<Double> scoresReplanned = new LinkedList<Double>();
//		
//		private List<Double> scores = new LinkedList<Double>();
//		
//		public void notifyIterationEnds(IterationEndsEvent event) {
//			double sumScoreWorst = 0.0;
//			double sumScoreBest = 0.0;
//			double sumAvgScores = 0.0;
//			double sumAvgScoresReplanned = 0.0;
//			double sumExecutedScores = 0.0;
//			int nofScoreWorst = 0;
//			int nofScoreBest = 0;
//			int nofAvgScores = 0;
//			int nofAvgScoresReplanned = 0;
//			int nofExecutedScores = 0;
//
//			for (Person person : EUTController.this.population.getPersons().values()) {
//				Plan worstPlan = null;
//				Plan bestPlan = null;
//				double sumScores = 0.0;
//				double cntScores = 0;
//				for (Plan plan : person.getPlans()) {
//
//					if (Plan.isUndefinedScore(plan.getScore())) {
//						continue;
//					}
//
//					// worst plan
//					if (worstPlan == null) {
//						worstPlan = plan;
//					} else if (plan.getScore() < worstPlan.getScore()) {
//						worstPlan = plan;
//					}
//
//					// best plan
//					if (bestPlan == null) {
//						bestPlan = plan;
//					} else if (plan.getScore() > bestPlan.getScore()) {
//						bestPlan = plan;
//					}
//
//					// avg. score
//					sumScores += plan.getScore();
//					cntScores++;
//
//					// executed plan?
//					if (plan.isSelected()) {
//						sumExecutedScores += plan.getScore();
//						nofExecutedScores++;
//					}
//				}
//
//				if (worstPlan != null) {
//					nofScoreWorst++;
//					sumScoreWorst += worstPlan.getScore();
//				}
//				if (bestPlan != null) {
//					nofScoreBest++;
//					sumScoreBest += bestPlan.getScore();
//				}
//				if (cntScores > 0) {
//					sumAvgScores += (sumScores / cntScores);
//					nofAvgScores++;
//				}
//				
//				if(EUTReRoute.replanedPersons.contains(person.getId().toString())) {
//					if (cntScores > 0) {
//					sumAvgScoresReplanned += (sumScores / cntScores);
//					nofAvgScoresReplanned++;
//					}
//				}
//			}
//			if(nofAvgScoresReplanned > 0)
//				scoresReplanned.add(sumAvgScoresReplanned/nofAvgScoresReplanned);
//			
//			scores.add(sumAvgScores/nofAvgScores);
//			
//			
//		}
//
//		public void notifyShutdown(ShutdownEvent event) {
//			double sum = 0;
//			for(Double d : scores)
//				sum += d;
//			
//			log.info("Avr score is " + sum/scores.size());
//			
//			sum = 0;
//			for(Double d : scoresReplanned)
//				sum += d;
//			
//			log.info("Avr score of replanned persons is " + sum/scoresReplanned.size());
//		}
//
//		public void notifyIterationStarts(IterationStartsEvent event) {
//			EUTReRoute.replanedPersons = new LinkedList<String>();
//			
//		}
//		
//	}
	
	private class LinkTTObserver implements EventHandlerAgentArrivalI, IterationStartsListener, IterationEndsListener {
		
		private double lastcall;
		
//		private Link link;
		
		private BufferedWriter writer;
		
		private BufferedWriter linkStatWriter;
		
		private Map<Link, Double> maxTTs;
		
		public LinkTTObserver() {
			
		}

		public void handleEvent(EventAgentArrival event) {
//			if(getIteration() == 10) {
			if(event.time > lastcall) {
				lastcall = event.time;
//				((EventBasedTTProvider)ttcalc).requestLinkCost();
				
				for(Link link : network.getLinks().values()) {
					double tt = ttcalc.getLinkTravelTime(link, event.time);
					double maxtt = maxTTs.get(link);
					if(tt > maxtt)
						maxTTs.put(link, tt);
					
					if(link.getId().toString().equals("1100")) {
						try {
							linkStatWriter.write(String.valueOf(event.time));
							linkStatWriter.write("\t");
							linkStatWriter.write(String.valueOf(tt));
							linkStatWriter.newLine();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}			
		}

		public void reset(int iteration) {
			// TODO Auto-generated method stub
			lastcall = 0;
		}

		public void notifyIterationStarts(IterationStartsEvent event) {
			try {
				if (writer != null)
					writer.close();

				if (linkStatWriter != null)
					writer.close();

				maxTTs = new HashMap<Link, Double>();
				for (Link link : network.getLinks().values()) {
					maxTTs.put(link, 0.0);
				}
				// link = network.getLink("900");
				writer = IOUtils.getBufferedWriter(EUTController
						.getOutputFilename(getIteration() + ".linktts.txt"));
				writer.write("link\ttravtime");
				writer.newLine();

				linkStatWriter = IOUtils
						.getBufferedWriter(EUTController
								.getOutputFilename(getIteration()
										+ ".1100.linktts.txt"));
				linkStatWriter.write("time\ttravtime");
				linkStatWriter.newLine();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void notifyIterationEnds(IterationEndsEvent event) {
			try {
				for (Link link : network.getLinks().values()) {
					writer.write(link.getId().toString());
					writer.write("\t");
					writer.write(String.valueOf(maxTTs.get(link)));
					writer.newLine();
				}
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private class LinkCounter implements EventHandlerLinkEnterI, IterationEndsListener, IterationStartsListener {

		private int count;
		
		private List<EventLinkEnter> events;
		
		private BufferedWriter writer;
		
		private BufferedWriter countswriter;
		
		private int firstEvent = 0;
		
		private int lastEvent = 0;
		
		public LinkCounter() {
			try {
				writer = IOUtils.getBufferedWriter(EUTController.getOutputFilename("1100.linkcounts.txt"));
				writer.write("iteration\tcounts");
				writer.newLine();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		public void handleEvent(EventLinkEnter event) {
			if(firstEvent == 0)
				firstEvent = (int) event.time;
			
			lastEvent = (int)event.time;
			if(event.link.getId().toString().equals("1100")) {
				events.add(event);
				count++;
			}
		}

		public void reset(int iteration) {
			events = new LinkedList<EventLinkEnter>();
			firstEvent = 0;
			lastEvent = 0;
		}
		
		public void notifyIterationEnds(IterationEndsEvent event) {
			int binsize = 60;
			int bincount = (lastEvent-firstEvent)/binsize;
			int[] bins = new int[bincount];
			for(EventLinkEnter e : events) {
				int idx = ((int)e.time - firstEvent)/binsize;
				bins[idx]++;
			}
			
			try {
				for(int i = 0; i < bins.length; i++) {
					countswriter.write(String.valueOf(i*binsize+firstEvent));
					countswriter.write("\t");
					countswriter.write(String.valueOf(bins[i]));
					countswriter.newLine();
				}
				countswriter.close();
				
				writer.write(String.valueOf(EUTController.getIteration()));
				writer.write("\t");
				writer.write(String.valueOf(count));
				writer.newLine();
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			count = 0;
		}
		public void notifyIterationStarts(IterationStartsEvent event) {
			try {
				countswriter = IOUtils.getBufferedWriter(EUTController
						.getOutputFilename(EUTController.getIteration()
								+ ".1100.linkcounts.txt"));
				countswriter.write("time\tcounts");
				countswriter.newLine();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


			
		}
	}
	
	public static void main(String args[]) {
		EUTController controller = new EUTController(new String[]{"/Users/fearonni/vsp-work/eut/corridor/config/config.xml"});
		controller.run();
	}

}
