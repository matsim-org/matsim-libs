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

package playground.agarwalamit.mixedTraffic.multiModeCadyts;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.supply.SimResults;

import org.apache.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
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
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.counts.Counts;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * {@link PlanStrategy Plan Strategy} used for replanning in MATSim which uses Cadyts to
 * select plans that better match to given occupancy counts.
 */
public class ModalCadytsContext implements ModalCadytsContextI<Link>, StartupListener, IterationEndsListener, BeforeMobsimListener {

	private final static Logger log = Logger.getLogger(ModalCadytsContext.class);

	private final static String LINKOFFSET_FILENAME = "linkCostOffsets.xml";
	private static final String FLOWANALYSIS_FILENAME = "flowAnalysis.txt";
	
	private final double countsScaleFactor;
	private final Set<String> modes ;
	private final Map<String,Counts<Link>> mode2calibrationCounts = new HashMap<>();
	private final boolean writeAnalysisFile;

	private Map<String,AnalyticalCalibrator<Link>> mode2calibrator;
	private ModalPlansTranslatorBasedOnEvents plansTranslator;
	private SimResults<Link> simResults;
	private Scenario scenario;
	private EventsManager eventsManager;
	private VolumesAnalyzer volumesAnalyzer;
	private OutputDirectoryHierarchy controlerIO;

	@Inject
	@Named("car") Counts<Link> carCalibrationCounts;
	@Named("bike") Counts<Link> bikeCalibrationCounts;
	
	@Inject
	ModalCadytsContext(Config config, Scenario scenario, @Named("calibration") Counts<Link> calibrationCounts, EventsManager eventsManager, VolumesAnalyzer volumesAnalyzer, OutputDirectoryHierarchy controlerIO) {
		this.scenario = scenario;
		this.modes = CollectionUtils.stringToSet( config.counts().getAnalyzedModes() );
		
		for(String mode : modes){
			this.mode2calibrationCounts.put(mode, carCalibrationCounts);
			this.mode2calibrationCounts.put(mode, bikeCalibrationCounts);
		}
		
		this.eventsManager = eventsManager;
		this.volumesAnalyzer = volumesAnalyzer;
		this.controlerIO = controlerIO;
		this.countsScaleFactor = config.counts().getCountsScaleFactor();

		CadytsConfigGroup cadytsConfig = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
		// addModule() also initializes the config group with the values read from the config file
		cadytsConfig.setWriteAnalysisFile(true);


		Set<String> countedLinks = new TreeSet<>();
		for (Id<Link> id : this.mode2calibrationCounts.get("car").getCounts().keySet()) {
			countedLinks.add(id.toString());
		}

		cadytsConfig.setCalibratedItems(countedLinks);

		this.writeAnalysisFile = cadytsConfig.isWriteAnalysisFile();
	}

	@Override
	public PlansTranslator<Link> getPlansTranslator() {
		return this.plansTranslator;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		// this collects events and generates cadyts plans from it
		this.plansTranslator = new ModalPlansTranslatorBasedOnEvents(scenario);
		this.eventsManager.addHandler(plansTranslator);
		
		for (String mode : this.modes ) {
			this.simResults = new ModalSimResultsContainerImpl(volumesAnalyzer, countsScaleFactor, mode);
			AnalyticalCalibrator<Link> analyticalCalibration = CadytsBuilder.buildCalibratorAndAddMeasurements(scenario.getConfig(), this.mode2calibrationCounts.get(mode) , new LinkLookUp(scenario) /*, cadytsConfig.getTimeBinSize()*/, Link.class);
			this.mode2calibrator.put(mode, analyticalCalibration);
		}
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		// Register demand for this iteration with Cadyts.
		// Note that planToPlanStep will return null for plans which have never been executed.
		// This is fine, since the number of these plans will go to zero in normal simulations,
		// and Cadyts can handle this "noise". Checked this with Gunnar.
		// mz 2015
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (String mode : this.modes){
				this.mode2calibrator.get(mode).addToDemand(plansTranslator.getCadytsPlan(person.getSelectedPlan()));
				this.mode2calibrator.get(mode).addToDemand(plansTranslator.getCadytsPlan(person.getSelectedPlan()));
			}
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		for (String mode : this.modes){
			if (this.writeAnalysisFile) {
				String analysisFilepath = null;
				if (isActiveInThisIteration(event.getIteration(), scenario.getConfig())) {
					analysisFilepath = controlerIO.getIterationFilename(event.getIteration(), FLOWANALYSIS_FILENAME);
				}
				this.mode2calibrator.get(mode).setFlowAnalysisFile(analysisFilepath);
			}
			this.mode2calibrator.get(mode).afterNetworkLoading(this.simResults);
			// write some output
			String filename = controlerIO.getIterationFilename(event.getIteration(), LINKOFFSET_FILENAME);
			try {
				new CadytsCostOffsetsXMLFileIO<>(new LinkLookUp(scenario), Link.class)
				.write(filename, this.mode2calibrator.get(mode).getLinkCostOffsets());
			} catch (IOException e) {
				log.error("Could not write link cost offsets!", e);
			}
		}
	}

	/**
	 * for testing purposes only
	 */
	@Override
	public AnalyticalCalibrator<Link> getCalibrator(String mode) {
		return this.mode2calibrator.get(mode);
	}

	// ===========================================================================================================================
	// private methods & pure delegate methods only below this line

	private static boolean isActiveInThisIteration(final int iter, final Config config) {
		return (iter > 0 && iter % config.counts().getWriteCountsInterval() == 0);
	}
}