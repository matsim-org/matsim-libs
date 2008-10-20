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
package playground.johannes.itsc08;

import java.util.Set;

import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.population.Person;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.withinday.WithindayControler;
import org.matsim.withinday.mobsim.WithindayQueueSimulation;
import org.matsim.withinday.trafficmanagement.TrafficManagement;

import playground.balmermi.census2000.data.Persons;
import playground.johannes.eut.EstimReactiveLinkTT;
import playground.johannes.eut.GuidedAgentFactory;

/**
 * @author illenberger
 *
 */
public class Controler extends WithindayControler {
	
	//==============================================================================
	// private fields
	//==============================================================================

//	private static final String CONFIG_MODULE_NAME = "eut";

//	private static final Logger log = Logger.getLogger(EUTController.class);

	private EstimReactiveLinkTT reactTTs;

//	private double incidentProba;

	private double equipmentFraction;

	//==============================================================================
	// 
	//==============================================================================
	
	public Controler(String[] args) {
		super(args);
		setOverwriteFiles(true);
	}

	@Override
	protected void setup() {
		IncidentGenerator i = new IncidentGenerator(Double.parseDouble(config.getParam("eut", "capacityFactor")));
		this.addControlerListener(i);
		
		Analyzer analyzer = new Analyzer(this);
		this.addControlerListener(analyzer);
		this.equipmentFraction = string2Double(config.getParam("eut", "alpha"));
		super.setup();
		/*
		 * Create a new factory for our withinday agents.
		 */
		addControlerListener(new WithindayControlerListener());
		
		this.reactTTs = new EstimReactiveLinkTT(1);
		this.events.addHandler(this.reactTTs);
		this.reactTTs.reset(getIteration());

	}

	protected StrategyManager loadStrategyManager() {
		StrategyManager m = new StrategyManager();
		m.addStrategy(new PlanStrategy(new ExpBetaPlanChanger()), 0.98);
//		m.addStrategy(new PlanStrategy(new SelectWorstPlan()), 0.01);
//		m.addStrategy(new PlanStrategy(new RandomPlanSelector()), 0.05);
		m.addStrategy(new PlanStrategy(new ForceSelectPlan(getNetwork().getLink("4"))), 0.01);
		m.addStrategy(new PlanStrategy(new ForceSelectPlan(getNetwork().getLink("5"))), 0.01);
		return m;
	}
	
	@Override
	protected void runMobSim() {

		this.config.withinday().addParam("contentThreshold", "1");
		this.config.withinday().addParam("replanningInterval", "1");

		WithindayQueueSimulation sim = new WithindayQueueSimulation(this.network, this.population, this.events, this);
		this.trafficManagement = new TrafficManagement();
		sim.setTrafficManagement(this.trafficManagement);
		
		sim.run();
	}

	public Set<Person> getGuidedPersons() {
		return ((GuidedAgentFactory)factory).getGuidedPersons();
	}
	
	private class WithindayControlerListener implements StartupListener, IterationStartsListener {

		public void notifyStartup(StartupEvent event) {
			Controler.this.factory = new GuidedAgentFactory(Controler.this.network, Controler.this.config.charyparNagelScoring(), Controler.this.reactTTs, Controler.this.equipmentFraction);

		}

		public void notifyIterationStarts(IterationStartsEvent event) {
			((GuidedAgentFactory)Controler.this.factory).reset();
		}

	}

	public static void main(String args[]) {
		Controler controller = new Controler(args);
		controller.setOverwriteFiles(true);
		controller.setCreateGraphs(false);
		controller.setWriteEvents(false);
		
		
		controller.run();
		
	}

	private double string2Double(String str) {
		if(str.endsWith("%"))
			return Integer.parseInt(str.substring(0, str.length()-1))/100.0;
		else
			return Double.parseDouble(str);

	}
}
