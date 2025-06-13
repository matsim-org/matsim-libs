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
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.counts.algorithms.CountsHtmlAndGraphsWriter;
import org.matsim.counts.algorithms.graphs.CountsErrorGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimReal24GraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimRealPerHourGraphCreator;

import jakarta.inject.Inject;

import java.util.Arrays;
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

    private GlobalConfigGroup globalConfigGroup;
    private Network network;
    private ControllerConfigGroup controllerConfigGroup;
    private final CountsConfigGroup config;
    private final Set<String> analyzedModes;
    private final VolumesAnalyzer volumesAnalyzer;
    private final IterationStopWatch iterationStopwatch;
    private final OutputDirectoryHierarchy controlerIO;

    @com.google.inject.Inject(optional=true)
    private Counts<Link> counts = null;

    private final IdMap<Link, double[]> linkStats = new IdMap<>(Link.class);
    private int iterationsUsed = 0;

    @Inject
    CountsControlerListener(GlobalConfigGroup globalConfigGroup, Network network, ControllerConfigGroup controllerConfigGroup, CountsConfigGroup countsConfigGroup, VolumesAnalyzer volumesAnalyzer, IterationStopWatch iterationStopwatch, OutputDirectoryHierarchy controlerIO) {
        this.globalConfigGroup = globalConfigGroup;
        this.network = network;
        this.controllerConfigGroup = controllerConfigGroup;
        this.config = countsConfigGroup;
        this.volumesAnalyzer = volumesAnalyzer;
        this.analyzedModes = CollectionUtils.stringToSet(this.config.getAnalyzedModes());
        this.iterationStopwatch = iterationStopwatch;
        this.controlerIO = controlerIO;
	}

	@Override
	public void notifyStartup(final StartupEvent controlerStartupEvent) {
        if (counts != null) {
            for (Id<Link> linkId : counts.getCounts().keySet()) {
                this.linkStats.put(linkId, new double[24]);
            }
        }
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if (counts != null && !counts.getMeasureLocations().isEmpty() && this.config.getWriteCountsInterval() > 0) {
            if (useVolumesOfIteration(event.getIteration(), controllerConfigGroup.getFirstIteration())) {
                addVolumes(volumesAnalyzer);
            }

            if (createCountsInIteration(event.getIteration())) {
                iterationStopwatch.beginOperation(OPERATION_COMPARECOUNTS);
                IdMap<Link, double[]> averages;
                if (this.iterationsUsed > 1) {
                    averages = new IdMap<>(Link.class);
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
                CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(averages, counts, network, config.getCountsScaleFactor());
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
                if (this.config.getOutputFormat().contains("txt") ||
                        this.config.getOutputFormat().contains("all")) {
                    String filename = controlerIO.getIterationFilename(event.getIteration(), "countscompare.txt");
                    CountSimComparisonTableWriter ctw = new CountSimComparisonTableWriter(cca.getComparison(), Locale.ENGLISH);
                    ctw.writeFile(filename);
                }
                if (this.config.getOutputFormat().contains("xml") ||
                        this.config.getOutputFormat().contains("all")) {
                    String filename = controlerIO.getIterationFilename(event.getIteration(), "simulatedCounts.xml.gz");
                    Counts<Link> simCounts = new Counts<>();
                    simCounts.setDescription("sim values from iteration " + event.getIteration()); simCounts.setName("sim values from iteration " + event.getIteration()); simCounts.setYear(event.getIteration());
                    for (CountSimComparison countSimComparison : cca.getComparison()) {
						if (simCounts.getCount(countSimComparison.getId()) == null) {
							simCounts.createAndAddCount(countSimComparison.getId(), counts.getCount(countSimComparison.getId()).getCsLabel());
							simCounts.getCount(countSimComparison.getId()).setCoord(counts.getCount(countSimComparison.getId()).getCoord());
						}
						simCounts.getCount(countSimComparison.getId()).createVolume(countSimComparison.getHour(), countSimComparison.getSimulationValue());
					}
                    CountsWriter countsWriter = new CountsWriter(TransformationFactory.getCoordinateTransformation(globalConfigGroup.getCoordinateSystem(), TransformationFactory.WGS84), simCounts);
                    countsWriter.write(filename);
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
			Arrays.fill(hours, 0.0);
		}
	}

}
