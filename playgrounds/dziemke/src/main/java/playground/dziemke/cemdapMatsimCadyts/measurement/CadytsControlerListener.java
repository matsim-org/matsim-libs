/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.dziemke.cemdapMatsimCadyts.measurement;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.PlansTranslatorBasedOnEvents;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.measurements.SingleLinkMeasurement.TYPE;
import cadyts.supply.SimResults;

/**
 * {@link org.matsim.core.replanning.PlanStrategy Plan Strategy} used for replanning in MATSim which uses Cadyts to
 * select plans that better match to given occupancy counts.
 */
@Singleton
class CadytsControlerListener implements BeforeMobsimListener, AfterMobsimListener, IterationEndsListener {

    private static final String FLOWANALYSIS_FILENAME = "flowAnalysis.txt";

    @Inject
    PlansTranslatorBasedOnEvents ptStep;

	private final double countsScaleFactor;
    private final boolean writeAnalysisFile;
    private AnalyticalCalibrator<Link> calibrator;
    private OutputDirectoryHierarchy controlerIO;
    private Scenario scenario;
    private VolumesAnalyzer volumesAnalyzer;

    @Inject
	CadytsControlerListener(Config config,
                            OutputDirectoryHierarchy controlerIO,
                            Scenario scenario,
                            VolumesAnalyzer volumesAnalyzer,
                            AnalyticalCalibrator calibrator) {
        this.controlerIO = controlerIO;
        this.scenario = scenario;
        this.volumesAnalyzer = volumesAnalyzer;
        this.calibrator = calibrator;
        this.countsScaleFactor = config.counts().getCountsScaleFactor();
        CadytsConfigGroup cadytsConfig = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
		cadytsConfig.setWriteAnalysisFile(true);
        this.writeAnalysisFile = cadytsConfig.isWriteAnalysisFile();
	}

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        for (Person person : scenario.getPopulation().getPersons().values()) {
            this.calibrator.addToDemand(ptStep.getCadytsPlan(person.getSelectedPlan()));
        }
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        this.calibrator.afterNetworkLoading(new SimResults<Link>() {

            @Override
            public double getSimValue(final Link link, final int startTime_s, final int endTime_s, final TYPE type) {
                int hour = startTime_s / 3600;
                Id<Link> linkId = link.getId();
                double[] values = volumesAnalyzer.getVolumesPerHourForLink(linkId);
                if (values == null) {
                    return 0;
                }
                return values[hour] * countsScaleFactor;
            }

        });
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
	}

	private boolean isActiveInThisIteration(final int iter) {
		return (iter > 0 && iter % scenario.getConfig().counts().getWriteCountsInterval() == 0);
	}
}