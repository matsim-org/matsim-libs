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

import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.plans.Plans;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.selectors.KeepSelected;
import org.matsim.trafficmonitoring.TravelTimeCalculatorArray;

/**
 * @author illenberger
 *
 */
public class EUTController extends Controler {
	
	private KStateLinkCostProvider provider;

	/**
	 * @param args
	 */
	public EUTController(String[] args) {
		super(args);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param configFileName
	 */
	public EUTController(String configFileName) {
		super(configFileName);
		setOverwriteFiles(true);
		provider = new KStateLinkCostProvider();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param configFileName
	 * @param dtdFileName
	 */
	public EUTController(String configFileName, String dtdFileName) {
		super(configFileName, dtdFileName);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param config
	 */
	public EUTController(Config config) {
		super(config);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param config
	 * @param network
	 * @param population
	 */
	public EUTController(Config config, QueueNetworkLayer network,
			Plans population) {
		super(config, network, population);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		manager.setMaxPlansPerAgent(1);
		
		PlanStrategy strategy = new PlanStrategy(new KeepSelected());
		strategy.addStrategyModule(new EUTReRoute(getNetwork(), provider));
		manager.addStrategy(strategy, 0.2);
		
		strategy = new PlanStrategy(new KeepSelected());
		manager.addStrategy(strategy, 0.8);
		
		return manager;
	}

	@Override
	protected void setup() {
		
		super.setup();		
		addControlerListener(new TTCalculatorController());
		addControlerListener(new NetworkModifier());
	}

	private class TTCalculatorController implements IterationStartsListener, IterationEndsListener {

		private TravelTimeCalculatorArray myttcalc;
		
		public void notifyIterationStarts(IterationStartsEvent event) {
			myttcalc = new TravelTimeCalculatorArray(EUTController.this.getNetwork());
			EUTController.this.events.addHandler(myttcalc);
		}

		public void notifyIterationEnds(IterationEndsEvent event) {
			EUTController.this.events.removeHandler(myttcalc);
			provider.appendTTSet(myttcalc);
			
		}
		
	}
	
	private class NetworkModifier implements IterationStartsListener {
		
		private double origCap;
		/* (non-Javadoc)
		 * @see org.matsim.controler.listener.IterationStartsListener#notifyIterationStarts(org.matsim.controler.events.IterationStartsEvent)
		 */
		public void notifyIterationStarts(IterationStartsEvent event) {
			/*
			 * Reduce capacity here...
			 */
			QueueLink link = (QueueLink) EUTController.this.getNetwork().getLink("9");
			if(event.getIteration() % 2 == 0) {
				origCap = link.getCapacity();
				link.setCapacity(origCap * 0.7);
			} else {
				link.setCapacity(origCap);
			}

		}

	}
	
	public static void main(String args[]) {
		EUTController controller = new EUTController("/Users/fearonni/vsp-work/eut/twoway/config/config.xml");
		controller.run();
	}

}
