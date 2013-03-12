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

package playground.christoph.burgdorf.cadyts;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.measurements.SingleLinkMeasurement.TYPE;
import cadyts.supply.SimResults;

/**
 * {@link PlanStrategy Plan Strategy} used for replanning in MATSim which uses Cadyts to
 * select plans that better match to given occupancy counts.
 */
public class CadytsContext implements StartupListener, IterationEndsListener {

	private final static Logger log = Logger.getLogger(CadytsContext.class);

	private final static String LINKOFFSET_FILENAME = "linkCostOffsets.xml";
	private static final String FLOWANALYSIS_FILENAME = "flowAnalysis.txt";
	
	private final double countsScaleFactor;
	private final Counts counts = new Counts();
	private final boolean writeAnalysisFile;
	private final CadytsCarConfigGroup cadytsConfig;
	
	private AnalyticalCalibrator<Link> calibrator;
	private PlanToPlanStepBasedOnEvents ptStep;
	private SimResultsContainerImpl simResults;
	
	public CadytsContext(Config config) {
		
		this.countsScaleFactor = config.counts().getCountsScaleFactor();
		
		this.cadytsConfig = new CadytsCarConfigGroup();
		config.addModule(CadytsCarConfigGroup.GROUP_NAME, cadytsConfig);
		// addModule() also initializes the config group with the values read from the config file
		cadytsConfig.setWriteAnalysisFile(true);
		
		String occupancyCountsFilename = config.counts().getCountsFileName();
		new MatsimCountsReader(this.counts).readFile(occupancyCountsFilename);
		
		Set<Id> countedLinks = new TreeSet<Id>();
		for (Id id : this.counts.getCounts().keySet()) countedLinks.add(id);
		
		cadytsConfig.setCalibratedLinks(countedLinks);
		
		this.writeAnalysisFile = cadytsConfig.isWriteAnalysisFile();
	}

	public PlanToPlanStepBasedOnEvents getPlanToPlanStepBasedOnEvents() {
		return this.ptStep;
	}
	
	public AnalyticalCalibrator<Link> getAnalyticalCalibrator() {
		return this.calibrator;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		Scenario scenario = event.getControler().getScenario();
		
		VolumesAnalyzer volumesAnalyzer = event.getControler().getVolumes();
		
		this.simResults = new SimResultsContainerImpl(volumesAnalyzer, this.countsScaleFactor);
		
		// this collects events and generates cadyts plans from it
		this.ptStep = new PlanToPlanStepBasedOnEvents(scenario, cadytsConfig.getCalibratedLinks());
		event.getControler().getEvents().addHandler(ptStep);

		// build the calibrator. This is a static method, and in consequence has no side effects
		this.calibrator = CadytsBuilder.buildCalibrator(scenario, this.counts /*, cadytsConfig.getTimeBinSize()*/);
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
			new CadytsLinkCostOffsetsXMLFileIO(event.getControler().getScenario()).write(filename, this.calibrator.getLinkCostOffsets());
		} catch (IOException e) {
			log.error("Could not write link cost offsets!", e);
		}
	}

	/**
	 * for testing purposes only
	 */
	/*package*/ AnalyticalCalibrator<Link> getCalibrator() {
		return this.calibrator;
	}

	// ===========================================================================================================================
	// private methods & pure delegate methods only below this line

	private boolean isActiveInThisIteration(final int iter, final Controler controler) {
		return (iter > 0 && iter % controler.getConfig().counts().getWriteCountsInterval() == 0);
//		return (iter % controler.getConfig().counts().getWriteCountsInterval() == 0);
	}
	
	/*package*/ static class SimResultsContainerImpl implements SimResults<Link> {
		private static final long serialVersionUID = 1L;
		private final VolumesAnalyzer volumesAnalyzer;
		private final double countsScaleFactor;

		SimResultsContainerImpl(final VolumesAnalyzer volumesAnalyzer, final double countsScaleFactor) {
			this.volumesAnalyzer = volumesAnalyzer;
			this.countsScaleFactor = countsScaleFactor;
		}

		@Override
		public double getSimValue(final Link link, final int startTime_s, final int endTime_s, final TYPE type) { // stopFacility or link

			int hour = startTime_s / 3600;
			Id linkId = link.getId();
			double[] values = volumesAnalyzer.getVolumesPerHourForLink(linkId);
			
			if (values == null) {
				return 0;
			}

			return values[hour] * this.countsScaleFactor;
		}

		@Override
		public String toString() {
			final StringBuffer stringBuffer2 = new StringBuffer();
			final String LINKID = "linkId: ";
			final String VALUES = "; values:";
			final char TAB = '\t';
			final char RETURN = '\n';

			for (Id linkId : this.volumesAnalyzer.getLinkIds()) { // Only occupancy!
				StringBuffer stringBuffer = new StringBuffer();
				stringBuffer.append(LINKID);
				stringBuffer.append(linkId);
				stringBuffer.append(VALUES);

				boolean hasValues = false; // only prints stops with volumes > 0
				int[] values = this.volumesAnalyzer.getVolumesForLink(linkId);

				for (int ii = 0; ii < values.length; ii++) {
					hasValues = hasValues || (values[ii] > 0);

					stringBuffer.append(TAB);
					stringBuffer.append(values[ii]);
				}
				stringBuffer.append(RETURN);
				if (hasValues) stringBuffer2.append(stringBuffer.toString());
			}
			return stringBuffer2.toString();
		}

	}
}