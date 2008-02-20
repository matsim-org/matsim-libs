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
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import org.matsim.mobsim.QueueNode;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.selectors.BestPlanSelector;
import org.matsim.withinday.WithindayControler;

/**
 * @author illenberger
 *
 */
public class EUTController extends WithindayControler {
	
	private TravelTimeMemory provider;

	public final static double incidentProba = 0.1;
	
	/**
	 * @param args
	 */
	public EUTController(String[] args) {
		super(args);
		// TODO Auto-generated constructor stub
	}

//	/**
//	 * @param configFileName
//	 */
//	public EUTController(String configFileName) {
//		super(configFileName);
//		setOverwriteFiles(true);
//		
//		// TODO Auto-generated constructor stub
//	}

//	/**
//	 * @param configFileName
//	 * @param dtdFileName
//	 */
//	public EUTController(String configFileName, String dtdFileName) {
//		super(configFileName, dtdFileName);
//		// TODO Auto-generated constructor stub
//	}

//	/**
//	 * @param config
//	 */
//	public EUTController(Config config) {
//		super(config);
//		// TODO Auto-generated constructor stub
//	}

//	/**
//	 * @param config
//	 * @param network
//	 * @param population
//	 */
//	public EUTController(Config config, QueueNetworkLayer network,
//			Plans population) {
//		super(config, network, population);
//		// TODO Auto-generated constructor stub
//	}

	@Override
	protected StrategyManager loadStrategyManager() {
		setTraveltimeBinSize(60);
		provider = new TravelTimeMemory();
		StrategyManager manager = new StrategyManager();
		manager.setMaxPlansPerAgent(1);
		
		PlanStrategy strategy = new PlanStrategy(new BestPlanSelector());
		strategy.addStrategyModule(new EUTReRoute(getNetwork(), provider));
		manager.addStrategy(strategy, 0.05);
		
		strategy = new PlanStrategy(new BestPlanSelector());
		manager.addStrategy(strategy, 0.95);
		
		return manager;
////		return super.loadStrategyManager();
	}

	@Override
	protected void setup() {
		
		super.setup();		
		
		addControlerListener(new TTCalculatorController());
//		addControlerListener(new NetworkModifier());
		addControlerListener(new TripDurationWriter());
		addControlerListener(new RouteDistribution());
		addControlerListener(new RouteCosts());
//		setScoringFunctionFactory(new EUTScoringFunctionFactory());
		
//		legDurationWriter = new CalcLegTimes(population); 
//		events.addHandler(legDurationWriter);
		factory = new GuidedAgentFactory(network, config.charyparNagelScoring(), getTravelTimeCalculator());
		
	}

	private class TTCalculatorController implements IterationStartsListener, IterationEndsListener {

//		private TravelTimeCalculatorArray myttcalc;
//		
		public void notifyIterationStarts(IterationStartsEvent event) {
//			myttcalc = new TravelTimeCalculatorArray(EUTController.this.getNetwork());
//			EUTController.this.events.addHandler(myttcalc);
		}

		public void notifyIterationEnds(IterationEndsEvent event) {
//			EUTController.this.events.removeHandler(myttcalc);
//			provider.appendTTSet(myttcalc);
			
			provider.appendNewStorage(provider.makeTTStorage(getTravelTimeCalculator(), network, getTraveltimeBinSize(), 0, 86400));
		}
		
	}
	
	private class NetworkModifier implements IterationStartsListener {
		
		private boolean changedcap = false;
		
		private BufferedWriter writer;
		
		public NetworkModifier() {
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
			QueueLink link = (QueueLink) EUTController.this.getNetwork().getLink("9");

			Gbl.random.nextDouble();
			if(Gbl.random.nextDouble() < incidentProba && event.getIteration() > 50) {
				link.changeSimulatedFlowCapacity(0.5);
				changedcap = true;
				try {
					writer.write(String.valueOf(event.getIteration()));
					writer.newLine();
					writer.flush();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				if(changedcap) {
					link.changeSimulatedFlowCapacity(1.0/0.5);
					changedcap = false;
				}
			}

		}

	}
	
	private class TripDurationWriter implements IterationEndsListener, EventHandlerAgentWait2LinkI, EventHandlerAgentArrivalI {
		
		private BufferedWriter writer;
		
		private Map<Person, Double> tripdurs = new HashMap<Person, Double>();
		
		private java.util.List<Double> safeRouteDurs = new LinkedList<Double>();
		
		private java.util.List<Double> riskyRouteDurs = new LinkedList<Double>();
		
		private QueueNode safeNode;
		
		private QueueNode riskyNode;
		
		public TripDurationWriter() {
			EUTController.this.events.addHandler(this);
			safeNode = (QueueNode) EUTController.this.getNetwork().getNode("6");
			riskyNode = (QueueNode) EUTController.this.getNetwork().getNode("7");
			try {
				writer = new BufferedWriter(new FileWriter(EUTController.getOutputFilename("tripstats.txt")));
				writer.write("Iteration\tdur_safe\tdur_risky");
				writer.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void notifyIterationEnds(IterationEndsEvent event) {
			int safedur = calcAvr(safeRouteDurs);
			int riskydur = calcAvr(riskyRouteDurs);
			try {
				writer.write(String.valueOf(event.getIteration()));
				writer.write("\t");
				writer.write(String.valueOf(safedur));
				writer.write("\t");
				writer.write(String.valueOf(riskydur));
				writer.newLine();
				writer.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		private int calcAvr(java.util.List<Double> vals) {
			double sum = 0;
			for(Double d : vals)
				sum += d;
			
			return (int) (sum/vals.size());
		}

		public void reset(int iteration) {
			tripdurs = new HashMap<Person, Double>();
			safeRouteDurs = new LinkedList<Double>();
			riskyRouteDurs = new LinkedList<Double>();
		}

		public void handleEvent(EventAgentArrival event) {
			if(event.linkId.equals("11")) {
			Double deptime = tripdurs.get(event.agent);
			if(deptime != null) {
				Leg leg = (Leg) event.agent.getSelectedPlan().getActsLegs().get(1); 
				if(leg.getRoute().getRoute().contains(safeNode)) {
					safeRouteDurs.add(event.time - deptime);
				} else if(leg.getRoute().getRoute().contains(riskyNode)) {
					riskyRouteDurs.add(event.time - deptime);
				}
				tripdurs.remove(event.agent);
			}
			}
		}

		public void handleEvent(EventAgentWait2Link event) {
			tripdurs.put(event.agent, event.time);
		}
	
	}
	
	private class RouteDistribution implements IterationEndsListener, EventHandlerLinkEnterI {

		private int safeRouteCnt = 0;
		private int riskyRouteCnt = 0;
		
		private BufferedWriter writer;
		
		public RouteDistribution() {
			try {
				writer = new BufferedWriter(new FileWriter(EUTController.getOutputFilename("routefrac.txt")));
				writer.write("Iteration\tn_safe\tn_risky\tn_riskaverse");
				writer.newLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			events.addHandler(this);
		}
		
		public void notifyIterationEnds(IterationEndsEvent event) {
			try {
				writer.write(event.getIteration()+"\t"+safeRouteCnt+"\t"+riskyRouteCnt);
				writer.write("\t"+EUTRouter.riskCount);
				writer.newLine();
				writer.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		public void handleEvent(EventLinkEnter event) {
			if(event.link.getId().toString().equals("9"))
				riskyRouteCnt++;
			else if(event.link.getId().toString().equals("8"))
				safeRouteCnt++;
			
		}
		public void reset(int iteration) {
			riskyRouteCnt = 0;
			safeRouteCnt = 0;
			EUTRouter.riskCount = 0;
			
		}
		
	}
	
	private class RouteCosts implements IterationEndsListener {

		private BufferedWriter writer;
		
		public RouteCosts() {
			try {
				writer = new BufferedWriter(new FileWriter(EUTController.getOutputFilename("routecosts.txt")));
				writer.write("Iteration\tcosts_safe_avr\tcosts_risky_avr\triskytt_avr\tsafett_avr\tsafe_ce\trisky_ce");
				writer.newLine();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		public void notifyIterationEnds(IterationEndsEvent event) {
			try {
				writer.write(String.valueOf(event.getIteration()));
				writer.write("\t");
				writer.write(String.valueOf(calcAvr(EUTRouter.safeCostsAvr)));
				writer.write("\t");
				writer.write(String.valueOf(calcAvr(EUTRouter.riskyCostsAvr)));
				writer.write("\t");
				writer.write(String.valueOf(calcAvr(EUTRouter.riskyTravTimeAvr)));
				writer.write("\t");
				writer.write(String.valueOf(calcAvr(EUTRouter.safeTravTimeAvr)));
				writer.write("\t");
				writer.write(String.valueOf(calcAvr(EUTRouter.safeCE)));
				writer.write("\t");
				writer.write(String.valueOf(calcAvr(EUTRouter.riskyCE)));
				writer.newLine();
				writer.flush();
				
				EUTRouter.safeCostsAvr = new LinkedList<Double>();
				EUTRouter.riskyCostsAvr = new LinkedList<Double>();
				EUTRouter.riskyTravTimeAvr = new LinkedList<Double>();
				EUTRouter.safeTravTimeAvr = new LinkedList<Double>();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		private double calcAvr(List<Double> vals) {
			double sum = 0;
			for(Double val : vals) {
//				try {
				sum += val;
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
			}
			
			return sum/(double)vals.size();
		}
	}
	
	public static void main(String args[]) {
		EUTController controller = new EUTController(new String[]{"/Users/fearonni/vsp-work/eut/twoway/config/config.xml"});
		controller.run();
	}

}
