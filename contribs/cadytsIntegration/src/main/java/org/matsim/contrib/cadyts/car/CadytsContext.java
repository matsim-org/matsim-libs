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

package org.matsim.contrib.cadyts.car;

import cadyts.calibrators.analytical.AnalyticalCalibrator;

import org.apache.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.*;
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
import org.matsim.counts.MatsimCountsReader;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

/**
 * {@link PlanStrategy Plan Strategy} used for replanning in MATSim which uses Cadyts to
 * select plans that better match to given occupancy counts.
 */
public class CadytsContext implements CadytsContextI<Link>, StartupListener, IterationEndsListener, BeforeMobsimListener {

	private final static Logger log = Logger.getLogger(CadytsContext.class);

	private final static String LINKOFFSET_FILENAME = "linkCostOffsets.xml";
	private static final String FLOWANALYSIS_FILENAME = "flowAnalysis.txt";
	
	private final double countsScaleFactor;
	private final Counts<Link> counts;
	private final boolean writeAnalysisFile;

	private AnalyticalCalibrator<Link> calibrator;
	private PlanToPlanStepBasedOnEvents planToPlanStep;
	private SimResultsContainerImpl simResults;
	
	public CadytsContext(Config config, Counts counts ) {
		
		this.countsScaleFactor = config.counts().getCountsScaleFactor();

		CadytsConfigGroup cadytsConfig = new CadytsConfigGroup();
		config.addModule(cadytsConfig);
		// addModule() also initializes the config group with the values read from the config file
		cadytsConfig.setWriteAnalysisFile(true);
		
		if ( counts==null ) {
			this.counts = new Counts();
			String occupancyCountsFilename = config.counts().getCountsFileName();
			new MatsimCountsReader(this.counts).readFile(occupancyCountsFilename);
		} else {
			this.counts = counts ;
		}
		
		Set<String> countedLinks = new TreeSet<>();
		for (Id<Link> id : this.counts.getCounts().keySet()) {
			countedLinks.add(id.toString());
		}
		
		cadytsConfig.setCalibratedItems(countedLinks);
		
		this.writeAnalysisFile = cadytsConfig.isWriteAnalysisFile();
	}
	
	public CadytsContext(Config config ) {
		this( config, null ) ;
	}

	@Override
	public PlansTranslator<Link> getPlansTranslator() {
		return this.planToPlanStep;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		Scenario scenario = event.getControler().getScenario();
		
		VolumesAnalyzer volumesAnalyzer = event.getControler().getVolumes();
		
		this.simResults = new SimResultsContainerImpl(volumesAnalyzer, this.countsScaleFactor);
		
		// this collects events and generates cadyts plans from it
		this.planToPlanStep = new PlanToPlanStepBasedOnEvents(scenario);
		event.getControler().getEvents().addHandler(planToPlanStep);

		this.calibrator = CadytsBuilder.buildCalibrator(scenario.getConfig(), this.counts , new LinkLookUp(scenario) /*, cadytsConfig.getTimeBinSize()*/, Link.class);
	}

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		// Register demand for this iteration with Cadyts.
		// Note that planToPlanStep will return null for plans which have never been executed.
		// This is fine, since the number of these plans will go to zero in normal simulations,
		// and Cadyts can handle this "noise". Checked this with Gunnar.
		// mz 2015
        for (Person person : event.getControler().getScenario().getPopulation().getPersons().values()) {
            this.calibrator.addToDemand(planToPlanStep.getPlanSteps(person.getSelectedPlan()));
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

		this.calibrator.afterNetworkLoading(this.simResults);

		// write some output
		String filename = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), LINKOFFSET_FILENAME);
		try {
			new CadytsCostOffsetsXMLFileIO<Link>(new LinkLookUp(event.getControler().getScenario()), Link.class)
   			   .write(filename, this.calibrator.getLinkCostOffsets());
		} catch (IOException e) {
			log.error("Could not write link cost offsets!", e);
		}
	}

	/**
	 * for testing purposes only
	 */
	@Override
	public AnalyticalCalibrator<Link> getCalibrator() {
		return this.calibrator;
	}

	// ===========================================================================================================================
	// private methods & pure delegate methods only below this line

	@SuppressWarnings("static-method")
	private boolean isActiveInThisIteration(final int iter, final Controler controler) {
		return (iter > 0 && iter % controler.getConfig().counts().getWriteCountsInterval() == 0);
//		return (iter % controler.getConfig().counts().getWriteCountsInterval() == 0);
	}
}