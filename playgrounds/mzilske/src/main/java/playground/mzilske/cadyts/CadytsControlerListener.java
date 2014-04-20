/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CadytsControlerListener.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.cadyts;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.measurements.SingleLinkMeasurement.TYPE;
import cadyts.supply.SimResults;
import org.apache.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.cadyts.general.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.counts.Counts;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

/**
 * {@link org.matsim.core.replanning.PlanStrategy Plan Strategy} used for replanning in MATSim which uses Cadyts to
 * select plans that better match to given occupancy counts.
 */
@Singleton
class CadytsControlerListener implements StartupListener, IterationEndsListener {

	private final static Logger log = Logger.getLogger(CadytsControlerListener.class);

	private final static String LINKOFFSET_FILENAME = "linkCostOffsets.xml";
	private static final String FLOWANALYSIS_FILENAME = "flowAnalysis.txt";

	private final double countsScaleFactor;
	private final Counts counts;
	private final boolean writeAnalysisFile;
	private final CadytsConfigGroup cadytsConfig;

	private AnalyticalCalibrator<Link> calibrator;
	@Inject PlanToPlanStepBasedOnEvents ptStep;
    private EventsManager eventsManager;
    private OutputDirectoryHierarchy controlerIO;
    private Scenario scenario;
    private VolumesAnalyzer volumesAnalyzer;

    @Inject
	CadytsControlerListener(Config config,
                            EventsManager eventsManager,
                            OutputDirectoryHierarchy controlerIO,
                            Scenario scenario,
                            VolumesAnalyzer volumesAnalyzer,
                            @Named("calibrationCounts") Counts counts,
                            AnalyticalCalibrator<Link> calibrator) {
        this.eventsManager = eventsManager;
        this.controlerIO = controlerIO;
        this.scenario = scenario;
        this.volumesAnalyzer = volumesAnalyzer;
        this.calibrator = calibrator;
        this.countsScaleFactor = config.counts().getCountsScaleFactor();
		
		this.cadytsConfig = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
		cadytsConfig.setWriteAnalysisFile(true);
		
		this.counts = counts;

		
		this.writeAnalysisFile = cadytsConfig.isWriteAnalysisFile();
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		eventsManager.addHandler(ptStep);
	}
	
	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if (this.writeAnalysisFile) {
			String analysisFilepath = null;
			if (isActiveInThisIteration(event.getIteration())) {
				analysisFilepath = controlerIO.getIterationFilename(event.getIteration(), FLOWANALYSIS_FILENAME);
			}
			this.calibrator.setFlowAnalysisFile(analysisFilepath);
		}

		this.calibrator.afterNetworkLoading(new SimResultsContainerImpl(volumesAnalyzer, this.countsScaleFactor));
	}

	private boolean isActiveInThisIteration(final int iter) {
		return (iter > 0 && iter % scenario.getConfig().counts().getWriteCountsInterval() == 0);
	}

	static class SimResultsContainerImpl implements SimResults<Link> {
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