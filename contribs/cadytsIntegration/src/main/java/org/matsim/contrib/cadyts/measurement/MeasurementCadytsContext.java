/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsPlanStrategy.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.cadyts.measurement;

import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.CadytsBuilder;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsContextI;
import org.matsim.contrib.cadyts.general.LookUp;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.counts.Counts;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.demand.Plan;

/**
 * {@link PlanStrategy Plan Strategy} used for replanning in MATSim which uses Cadyts to
 * select plans that better match to given occupancy counts.
 */
public class MeasurementCadytsContext implements CadytsContextI<Measurement>, StartupListener, IterationEndsListener, BeforeMobsimListener {

	final static Logger log = Logger.getLogger(MeasurementCadytsContext.class);

	private final static String LINKOFFSET_FILENAME = "linkCostOffsets.xml";
	private static final String FLOWANALYSIS_FILENAME = "flowAnalysis.txt";
	
	private final Counts<Measurement> counts;
	private final boolean writeAnalysisFile;

	private AnalyticalCalibrator<Measurement> calibrator;
	private MeasurementPlanToPlanStepBasedOnEvents planToPlanStep;
	private SimResultsContainerImpl simResults;
	
	public MeasurementCadytsContext(Config config, Counts<Measurement> measurements) {

		CadytsConfigGroup cadytsConfig = new CadytsConfigGroup();
		config.addModule(cadytsConfig);
		// addModule() also initializes the config group with the values read from the config file
		cadytsConfig.setWriteAnalysisFile(true);
		
		this.counts = measurements;

		Set<String> measurementsSet = new TreeSet<>();
		for (Id<Measurement> id : this.counts.getCounts().keySet()) {
			measurementsSet.add(id.toString());
		}
		
		cadytsConfig.setCalibratedItems(measurementsSet);

		this.writeAnalysisFile = cadytsConfig.isWriteAnalysisFile();
	}
	
	public MeasurementCadytsContext(Config config ) {
		this( config, null ) ;
	}

	@Override
	public PlansTranslator<Measurement> getPlansTranslator() {
		return this.planToPlanStep;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		Scenario scenario = event.getControler().getScenario();
		
		TravelDistanceAnalyzer travelDistanceAnalyzer = new TravelDistanceAnalyzer(3600, 3600*36, scenario.getNetwork());
		event.getControler().getEvents().addHandler(travelDistanceAnalyzer);
		
		this.simResults = new SimResultsContainerImpl(travelDistanceAnalyzer);
		
		// this collects events and generates cadyts plans from it
		this.planToPlanStep = new MeasurementPlanToPlanStepBasedOnEvents(scenario);
		event.getControler().getEvents().addHandler(planToPlanStep);

		LookUp<Measurement> lookUp = new LookUp<Measurement>() {
			@Override public Measurement lookUp(Id<Measurement> id) {
				return new Measurement( id ) ;
			}
		} ;
		this.calibrator = CadytsBuilder.buildCalibrator(scenario.getConfig(), this.counts, lookUp, Measurement.class);
	}

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
    	
    	// ---------- 2nd important Cadyts method is "analyzer.calcLinearPlanEffect"
		// Register demand for this iteration with Cadyts.
		// Note that planToPlanStep will return null for plans which have never been executed.
		// This is fine, since the number of these plans will go to zero in normal simulations,
		// and Cadyts can handle this "noise". Checked this with Gunnar.
		// mz 2015
        for (Person person : event.getControler().getScenario().getPopulation().getPersons().values()) {
            Plan<Measurement> planSteps = this.planToPlanStep.getPlanSteps(person.getSelectedPlan());
			this.calibrator.addToDemand(planSteps);
        }
    }

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if (this.writeAnalysisFile) {
			String analysisFilepath = null;
			if (isActiveInThisIteration(event.getIteration(), event.getControler())) {
				analysisFilepath = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), FLOWANALYSIS_FILENAME);
			}
			this.calibrator.setFlowAnalysisFile(analysisFilepath);
		}
		
		// ---------- 3rd important method "calibrator.afterNetworkLoading"
//		this.calibrator.afterNetworkLoading(this.simResults);
		this.calibrator.afterNetworkLoading(simResults);

		// write some output
		String filename = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), LINKOFFSET_FILENAME);
		// TODO writing does not work currently; reactivate this when other stuff has been sorted out
//		try {
////			new CadytsCostOffsetsXMLFileIO<Link>(new LinkLookUp(event.getControler().getScenario()), Link.class)
//			new CadytsCostOffsetsXMLFileIO<Double>(new DistributionBinLookUp(), Double.class)
//   			   .write(filename, this.calibrator.getLinkCostOffsets());
//		} catch (IOException e) {
//			log.error("Could not write link cost offsets!", e);
//		}
	}

	/**
	 * for testing purposes only
	 */
	@Override
	public AnalyticalCalibrator<Measurement> getCalibrator() {
		return this.calibrator;
	}

	// ===========================================================================================================================
	// private methods & pure delegate methods only below this line

	@SuppressWarnings("static-method")
	private boolean isActiveInThisIteration(final int iter, final Controler controler) {
		return (iter > 0 && iter % controler.getConfig().counts().getWriteCountsInterval() == 0);
	}
}