/* *********************************************************************** *
 * project: org.matsim.*
 * CountControlerListener.java
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

package org.matsim.counts;

import org.matsim.analysis.IterationStopWatch;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.counts.algorithms.CountsHtmlAndGraphsWriter;
import org.matsim.counts.algorithms.graphs.CountsErrorGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimReal24GraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimRealPerHourGraphCreator;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author dgrether
 */
class CountsControlerListener implements StartupListener, IterationEndsListener {

	/*
	 * String used to identify the operation in the IterationStopWatch.
	 */
	public static final String OPERATION_COMPARECOUNTS = "compare with counts";
	
	private final CountsConfigGroup config;
    private final Set<String> analyzedModes;
    private final Scenario scenario;
    private final VolumesAnalyzer volumesAnalyzer;
    private final IterationStopWatch iterationStopwatch;
    private final OutputDirectoryHierarchy controlerIO;

    private Counts<Link> counts;

    private final Map<Id<Link>, double[]> linkStats = new HashMap<>();
    private int iterationsUsed = 0;

    @Inject
    CountsControlerListener(final Scenario scenario, VolumesAnalyzer volumesAnalyzer, IterationStopWatch iterationStopwatch, OutputDirectoryHierarchy controlerIO) {
        this.config = scenario.getConfig().counts();
		this.scenario = scenario;
        this.volumesAnalyzer = volumesAnalyzer;
		this.analyzedModes = CollectionUtils.stringToSet(this.config.getAnalyzedModes());
        this.iterationStopwatch = iterationStopwatch;
        this.controlerIO = controlerIO;
	}

	@Override
	public void notifyStartup(final StartupEvent controlerStartupEvent) {
        counts = (Counts<Link>) this.scenario.getScenarioElement(Counts.ELEMENT_NAME);
        loadCountsIfNecessary();
        if (counts != null) {
            for (Id<Link> linkId : counts.getCounts().keySet()) {
                this.linkStats.put(linkId, new double[24]);
            }
        }
	}

    private void loadCountsIfNecessary() {
        if (counts == null) {
            if (this.config.getCountsFileName() != null) {
                counts = new Counts();
                MatsimCountsReader counts_parser = new MatsimCountsReader(counts);
                counts_parser.readFile(this.config.getCountsFileName());
                this.scenario.addScenarioElement(Counts.ELEMENT_NAME, counts);
            }
        }
    }

    @Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if (counts != null && this.config.getWriteCountsInterval() > 0) {
            if (useVolumesOfIteration(event.getIteration(), scenario.getConfig().controler().getFirstIteration())) {
                addVolumes(volumesAnalyzer);
            }

            if (createCountsInIteration(event.getIteration())) {
                iterationStopwatch.beginOperation(OPERATION_COMPARECOUNTS);
                Map<Id<Link>, double[]> averages;
                if (this.iterationsUsed > 1) {
                    averages = new HashMap<>();
                    for (Map.Entry<Id<Link>, double[]> e : this.linkStats.entrySet()) {
                        Id<Link> linkId = e.getKey();
                        double[] totalVolumesPerHour = e.getValue();
                        double[] averageVolumesPerHour = new double[totalVolumesPerHour.length];
                        for (int i = 0; i < totalVolumesPerHour.length; i++) {
                            averageVolumesPerHour[i] = totalVolumesPerHour[i] / this.iterationsUsed;
                        }
                        averages.put(linkId, averageVolumesPerHour);
                    }
                } else {
                    averages = this.linkStats;
                }
                CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(averages, counts, scenario.getNetwork(), config.getCountsScaleFactor());
                if ((this.config.getDistanceFilter() != null) && (this.config.getDistanceFilterCenterNode() != null)) {
                    cca.setDistanceFilter(this.config.getDistanceFilter(), this.config.getDistanceFilterCenterNode());
                }
                cca.setCountsScaleFactor(this.config.getCountsScaleFactor());
                cca.run();

                if (this.config.getOutputFormat().contains("html") ||
                        this.config.getOutputFormat().contains("all")) {
                    CountsHtmlAndGraphsWriter cgw = new CountsHtmlAndGraphsWriter(controlerIO.getIterationPath(event.getIteration()), cca.getComparison(), event.getIteration());
                    cgw.addGraphsCreator(new CountsSimRealPerHourGraphCreator("sim and real volumes"));
                    cgw.addGraphsCreator(new CountsErrorGraphCreator("errors"));
                    cgw.addGraphsCreator(new CountsLoadCurveGraphCreator("link volumes"));
                    cgw.addGraphsCreator(new CountsSimReal24GraphCreator("average working day sim and count volumes"));
                    cgw.createHtmlAndGraphs();
                }
                if (this.config.getOutputFormat().contains("kml") ||
                        this.config.getOutputFormat().contains("all")) {
                    String filename = controlerIO.getIterationFilename(event.getIteration(), "countscompare.kmz");
                    CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
                            cca.getComparison(), scenario.getNetwork(), TransformationFactory.getCoordinateTransformation(scenario.getConfig().global().getCoordinateSystem(), TransformationFactory.WGS84));
                    kmlWriter.setIterationNumber(event.getIteration());
                    kmlWriter.writeFile(filename);
                }
                if (this.config.getOutputFormat().contains("txt") ||
                        this.config.getOutputFormat().contains("all")) {
                    String filename = controlerIO.getIterationFilename(event.getIteration(), "countscompare.txt");
                    CountSimComparisonTableWriter ctw = new CountSimComparisonTableWriter(cca.getComparison(), Locale.ENGLISH);
                    ctw.writeFile(filename);
                }
                reset();
                iterationStopwatch.endOperation(OPERATION_COMPARECOUNTS);
            }
        }
	}

	/*package*/ boolean useVolumesOfIteration(final int iteration, final int firstIteration) {
		int iterationMod = iteration % this.config.getWriteCountsInterval();
		int effectiveIteration = iteration - firstIteration;
		int averaging = Math.min(this.config.getAverageCountsOverIterations(), this.config.getWriteCountsInterval());
		if (iterationMod == 0) {
			return ((this.config.getAverageCountsOverIterations() <= 1) ||
					(effectiveIteration >= averaging));
		}
		return (iterationMod > (this.config.getWriteCountsInterval() - this.config.getAverageCountsOverIterations())
				&& (effectiveIteration + (this.config.getWriteCountsInterval() - iterationMod) >= averaging));
	}
	
	/*package*/ boolean createCountsInIteration(final int iteration) {
		return ((iteration % this.config.getWriteCountsInterval() == 0) && (this.iterationsUsed >= this.config.getAverageCountsOverIterations()));		
	}

	private void addVolumes(final VolumesAnalyzer volumes) {
		this.iterationsUsed++;
		for (Map.Entry<Id<Link>, double[]> e : this.linkStats.entrySet()) {
			Id<Link> linkId = e.getKey();
			double[] volumesPerHour = e.getValue(); 
			double[] newVolume = getVolumesPerHourForLink(volumes, linkId); 
			for (int i = 0; i < 24; i++) {
				volumesPerHour[i] += newVolume[i];
			}
		}
	}
	
	private double[] getVolumesPerHourForLink(final VolumesAnalyzer volumes, final Id<Link> linkId) {
		if (this.config.isFilterModes()) {
			double[] newVolume = new double[24];
			for (String mode : this.analyzedModes) {
				double[] volumesForMode = volumes.getVolumesPerHourForLink(linkId, mode);
				if (volumesForMode == null) continue;
				for (int i = 0; i < 24; i++) {
					newVolume[i] += volumesForMode[i];
				}
			}
			return newVolume;
		} else {
			return volumes.getVolumesPerHourForLink(linkId);
		}
	}
	
	private void reset() {
		this.iterationsUsed = 0;
		for (double[] hours : this.linkStats.values()) {
			for (int i = 0; i < hours.length; i++) {
				hours[i] = 0.0;
			}
		}
	}

}
