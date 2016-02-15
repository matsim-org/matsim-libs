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

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.CadytsBuilder;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsContextI;
import org.matsim.contrib.cadyts.general.CadytsCostOffsetsXMLFileIO;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.counts.Counts;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.demand.Plan;

public class MeasurementCadytsContext implements CadytsContextI<Measurement>, StartupListener, IterationEndsListener, BeforeMobsimListener {

	final static Logger log = Logger.getLogger(MeasurementCadytsContext.class);

	private final static String COSTOFFSET_FILENAME = "costOffsets.xml";
	private static final String ANALYSIS_FILENAME = "analysis.txt";

	private final Counts<Measurement> counts;
	private final boolean writeAnalysisFile;

	private AnalyticalCalibrator<Measurement> calibrator;
	private MeasurementListener measurementListener;

	private final Measurements measurements ;

	public MeasurementCadytsContext(Config config, Tuple<Counts<Measurement>, Measurements> tuple) {

		CadytsConfigGroup cadytsConfig = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class ) ;
		cadytsConfig.setWriteAnalysisFile(true);

		this.counts = tuple.getFirst() ;
		this.measurements = tuple.getSecond() ;

		Set<String> measurementsSet = new TreeSet<>();
		for (Id<Measurement> id : this.counts.getCounts().keySet()) {
			measurementsSet.add(id.toString());
		}

		cadytsConfig.setCalibratedItems(measurementsSet);

		this.writeAnalysisFile = cadytsConfig.isWriteAnalysisFile();
	}

	@Override
	public PlansTranslator<Measurement> getPlansTranslator() {
		return this.measurementListener;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		// yy could probably move all of this method into the constructor, and then get rid of this method.  But should
		// be debugged and a test case written before. kai, oct'15

		Scenario scenario = event.getServices().getScenario();
		Config config = scenario.getConfig();

		// 1st major Cadyts method is "calibrator.addMesurement"
		// in this implementation it is called by the "CadytsBuilder", dz 09/15
		this.calibrator = CadytsBuilder.buildCalibratorAndAddMeasurements(config, this.counts, measurements, Measurement.class) ;

		this.measurementListener = new MeasurementListener(scenario, measurements );
		event.getServices().getEvents().addHandler(measurementListener);
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {

		// 2nd major Cadyts method is "analyzer.addToDemand", dz 09/15
		
		// Register demand for this iteration with Cadyts.
		// Note that planToPlanStep will return null for plans which have never been executed.
		// This is fine, since the number of these plans will go to zero in normal simulations,
		// and Cadyts can handle this "noise". Checked this with Gunnar.
		// mz 2015
		for (Person person : event.getServices().getScenario().getPopulation().getPersons().values()) {
			Plan<Measurement> planSteps = this.measurementListener.getCadytsPlan(person.getSelectedPlan());
			this.calibrator.addToDemand(planSteps);
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if (this.writeAnalysisFile) {
			String analysisFilepath = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), ANALYSIS_FILENAME);
			this.calibrator.setFlowAnalysisFile(analysisFilepath);
		}

		// 3rd major Cadyts method "calibrator.afterNetworkLoading", dz 09/15
		this.calibrator.afterNetworkLoading(this.measurementListener);

		// write some output
		String filename = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), COSTOFFSET_FILENAME);
		try {
			new CadytsCostOffsetsXMLFileIO<Measurement>( this.measurements, Measurement.class)
			.write(filename, this.calibrator.getLinkCostOffsets());
		} catch (IOException e) {
			log.error("Could not write link cost offsets!", e);
		}
	}

	// ===========================================================================================================================
	// private methods & pure delegate methods only below this line

	@Override
	public AnalyticalCalibrator<Measurement> getCalibrator() {
		return this.calibrator ;
	}
}