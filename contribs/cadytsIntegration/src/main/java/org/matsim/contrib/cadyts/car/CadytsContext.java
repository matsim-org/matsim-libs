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
import cadyts.supply.SimResults;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.matsim.counts.Counts;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import static org.matsim.contrib.cadyts.general.CadytsBuilderImpl.*;

/**
 * {@link PlanStrategy Plan Strategy} used for replanning in MATSim which uses Cadyts to
 * select plans that better match to given occupancy counts.
 */
public class CadytsContext implements CadytsContextI<Link>, StartupListener, IterationEndsListener, BeforeMobsimListener {

	private final static Logger log = LogManager.getLogger(CadytsContext.class);

	private final static String LINKOFFSET_FILENAME = "linkCostOffsets.xml";
	private static final String FLOWANALYSIS_FILENAME = "flowAnalysis.txt";

	private final double countsScaleFactor;
	private final Counts<Link> calibrationCounts;
	private final boolean writeAnalysisFile;

	private AnalyticalCalibrator<Link> calibrator;
	private PlansTranslatorBasedOnEvents plansTranslator;
	private SimResults<Link> simResults;
	private Scenario scenario;
	private EventsManager eventsManager;
	private OutputDirectoryHierarchy controlerIO;

	private PcuVolumesAnalyzer pcuVolumesAnalyzer;

	@Inject CadytsContext( Config config, Scenario scenario, @Named(CadytsCarModule.CALIBRATION) Counts<Link> calibrationCounts,
						   EventsManager eventsManager, OutputDirectoryHierarchy controlerIO) {
		this.scenario = scenario;
		this.calibrationCounts = calibrationCounts;
		this.eventsManager = eventsManager;
		this.controlerIO = controlerIO;
		this.countsScaleFactor = config.counts().getCountsScaleFactor();

		CadytsConfigGroup cadytsConfig = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.class);
		// (also initializes the config group with the values read from the config file)

		cadytsConfig.setWriteAnalysisFile(true);
		// yyyy not so good to just overwrite config.  kai, feb'20

		if ( cadytsConfig.getCalibratedLinks().isEmpty() ){
			// found this without the above condition, i.e. it would always set the calibrated links to all links for which there exist counts.
			// However, the logic for ptCounts is different, and there it would probably make sense to keep the functionality that only some
			// lines are calibrated.  So, for symmetry, would make sense to also make it functional here.  ????  kai, feb'20

			Set<String> countedLinks = new TreeSet<>();
			for( Id<Link> id : this.calibrationCounts.getCounts().keySet() ){
				countedLinks.add( id.toString() );
			}
			cadytsConfig.setCalibratedLinks( countedLinks );
		}
		this.writeAnalysisFile = cadytsConfig.isWriteAnalysisFile();
	}

	@Override
	public PlansTranslator<Link> getPlansTranslator() {
		return this.plansTranslator;
	}

	@Override
	public double getAgentWeight(Person person) {
		// Look up the vehicle for the person and return its PCU
		if (scenario.getVehicles() != null) {
			// Assuming standard MATSim mapping where vehicleId == personId
			Vehicle v = scenario.getVehicles().getVehicles().get(Id.create(person.getId(), Vehicle.class));
			if (v != null && v.getType() != null) {
				return v.getType().getPcuEquivalents();
			}
		}
		return 1.0;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		// Initialize the PCU analyzer
		this.pcuVolumesAnalyzer = new PcuVolumesAnalyzer(scenario.getVehicles(), 3600, 30 * 3600);
		this.eventsManager.addHandler(this.pcuVolumesAnalyzer);

		this.simResults = new SimResultsContainerImpl(this.pcuVolumesAnalyzer, this.countsScaleFactor);
		this.plansTranslator = new PlansTranslatorBasedOnEvents(scenario);
		this.eventsManager.addHandler(plansTranslator);

		this.calibrator = buildCalibratorAndAddMeasurements(scenario.getConfig(), this.calibrationCounts , new LinkLookUp(scenario), Link.class );
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		// Register demand for this iteration with Cadyts.
		// Note that planToPlanStep will return null for plans which have never been executed.
		// This is fine, since the number of these plans will go to zero in normal simulations,
		// and Cadyts can handle this "noise". Checked this with Gunnar.
		// mz 2015
		for (Person person : scenario.getPopulation().getPersons().values()) {
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
			new CadytsCostOffsetsXMLFileIO<>(new LinkLookUp(scenario), Link.class)
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

	private static boolean isActiveInThisIteration(final int iter, final Config config) {
		return (iter > 0 && iter % config.counts().getWriteCountsInterval() == 0);
	}
}
