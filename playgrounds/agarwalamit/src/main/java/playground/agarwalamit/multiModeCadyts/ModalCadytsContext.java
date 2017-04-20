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

package playground.agarwalamit.multiModeCadyts;

import java.io.IOException;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.supply.SimResults;
import org.apache.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.counts.Counts;

/**
 * {@link PlanStrategy Plan Strategy} used for replanning in MATSim which uses Cadyts to
 * select plans that better match to given occupancy counts.
 */
public class ModalCadytsContext implements CadytsContextI<ModalLink>, StartupListener, IterationEndsListener, BeforeMobsimListener {

	private final static Logger log = Logger.getLogger(ModalCadytsContext.class);

	private final static String LINKOFFSET_FILENAME = "linkCostOffsets.xml";
	private static final String FLOWANALYSIS_FILENAME = "flowAnalysis.txt";

	private final double countsScaleFactor;
	private final Counts<ModalLink> calibrationCounts;
	private final boolean writeAnalysisFile;

	private AnalyticalCalibrator<ModalLink> calibrator;
	private ModalPlansTranslatorBasedOnEvents plansTranslator;
	private SimResults<ModalLink> simResults;
	private final Scenario scenario;
	private final EventsManager eventsManager;
	private final VolumesAnalyzer volumesAnalyzer;
	private final OutputDirectoryHierarchy controlerIO;
	
	private final Map<String,ModalLink> modalLinkContainer;

	@Inject
	private ModalCadytsContext(Config config, Scenario scenario, @Named("calibration") Counts<ModalLink> calibrationCounts, EventsManager eventsManager,
			VolumesAnalyzer volumesAnalyzer, OutputDirectoryHierarchy controlerIO, Map<String,ModalLink> modalLinkContainer) {
		this.scenario = scenario;
		this.calibrationCounts = calibrationCounts;

		this.eventsManager = eventsManager;
		this.volumesAnalyzer = volumesAnalyzer;
		this.controlerIO = controlerIO;
		this.countsScaleFactor = config.counts().getCountsScaleFactor();
		this.modalLinkContainer = modalLinkContainer;

		CadytsConfigGroup cadytsConfig = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
		// addModule() also initializes the config group with the values read from the config file
		cadytsConfig.setWriteAnalysisFile(true);

		cadytsConfig.setCalibratedItems(modalLinkContainer.keySet());
		
		this.writeAnalysisFile = cadytsConfig.isWriteAnalysisFile();
	}

	@Override
	public PlansTranslator<ModalLink> getPlansTranslator() {
		return this.plansTranslator;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		this.simResults = new ModalSimResultsContainerImpl(volumesAnalyzer, countsScaleFactor);
		
		// this collects events and generates cadyts plans from it
		this.plansTranslator = new ModalPlansTranslatorBasedOnEvents(scenario, modalLinkContainer);
		this.eventsManager.addHandler(plansTranslator);

		this.calibrator =  CadytsBuilderImpl.buildCalibratorAndAddMeasurements(scenario.getConfig(), this.calibrationCounts , new ModalLinkLookUp(modalLinkContainer) /*, cadytsConfig.getTimeBinSize()*/, ModalLink.class);
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		// Register demand for this iteration with Cadyts.
		// Note that planToPlanStep will return null for plans which have never been executed.
		// This is fine, since the number of these plans will go to zero in normal simulations,
		// and Cadyts can handle this "noise". Checked this with Gunnar.
		// mz 2015
		for (Person person : scenario.getPopulation().getPersons().values()) { // this is wrong. adding plan 
			this.calibrator.addToDemand(plansTranslator.getCadytsPlan(person.getSelectedPlan()));
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if (this.writeAnalysisFile) {
			String analysisFilepath = null;
			if (isActiveInThisIteration(event.getIteration(), scenario.getConfig())) {
				analysisFilepath = controlerIO.getIterationFilename(event.getIteration(), FLOWANALYSIS_FILENAME);
			}
			this.calibrator.setFlowAnalysisFile(analysisFilepath);
		}
		this.calibrator.afterNetworkLoading(this.simResults);
		// write some output
		String filename = controlerIO.getIterationFilename(event.getIteration(), LINKOFFSET_FILENAME);
		try {
			new CadytsCostOffsetsXMLFileIO<>(new ModalLinkLookUp(modalLinkContainer), ModalLink.class)
			.write(filename, this.calibrator.getLinkCostOffsets());
		} catch (IOException e) {
			log.error("Could not write link cost offsets!", e);
		}
	}

	/**
	 * for testing purposes only
	 */
	@Override
	public AnalyticalCalibrator<ModalLink> getCalibrator() {
		return this.calibrator;
	}

	// ===========================================================================================================================
	// private methods & pure delegate methods only below this line

	private static boolean isActiveInThisIteration(final int iter, final Config config) {
		return (iter > 0 && iter % config.counts().getWriteCountsInterval() == 0);
	}
}