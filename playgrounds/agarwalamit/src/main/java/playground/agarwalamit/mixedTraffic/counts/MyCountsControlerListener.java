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

package playground.agarwalamit.mixedTraffic.counts;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.matsim.analysis.IterationStopWatch;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;

/**
 * @author amit after dgrether
 */
public class MyCountsControlerListener implements StartupListener, IterationEndsListener {

	/*
	 * String used to identify the operation in the IterationStopWatch.
	 */
	public static final String OPERATION_COMPARECOUNTS = "compare with counts";

    private Network network;
    private ControlerConfigGroup controlerConfigGroup;
    private final CountsConfigGroup config;
    private final Set<String> analyzedModes;
    private final VolumesAnalyzer volumesAnalyzer;
    private final IterationStopWatch iterationStopwatch;
    private final OutputDirectoryHierarchy controlerIO;

    @com.google.inject.Inject(optional=true)
    private Counts<Link> counts = null;

    private final Map<Id<Link>, Map<String,double[]>> linkStats = new HashMap<>();
    private int iterationsUsed = 0;

    @Inject
    MyCountsControlerListener(Network network, ControlerConfigGroup controlerConfigGroup, CountsConfigGroup countsConfigGroup, VolumesAnalyzer volumesAnalyzer, IterationStopWatch iterationStopwatch, OutputDirectoryHierarchy controlerIO) {
        this.network = network;
        this.controlerConfigGroup = controlerConfigGroup;
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
				Map<String, double[]> mode2counts = new HashMap<>();
				if(this.config.isFilterModes()){
					for(String mode : this.analyzedModes){
						mode2counts.put(mode, new double [24]);
					}
				} else {
					throw new RuntimeException("If not filtering modes for counts, doesn't make sense to use it. Aborting ...");
				}
				this.linkStats.put(linkId, mode2counts);
			}
		}
	}

    @Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if (counts != null && this.config.getWriteCountsInterval() > 0) {
            if (useVolumesOfIteration(event.getIteration(), controlerConfigGroup.getFirstIteration())) {
                addVolumes(volumesAnalyzer);
            }

            if (createCountsInIteration(event.getIteration())) {
                iterationStopwatch.beginOperation(OPERATION_COMPARECOUNTS);
                Map<Id<Link>, Map<String, double[]>> averages;
                if (this.iterationsUsed > 1) {
                    averages = new HashMap<>();
                    for (Map.Entry<Id<Link>, Map<String, double[]>> e : this.linkStats.entrySet()) {
                        Id<Link> linkId = e.getKey();
                        Map<String, double[]> mode2avgcounts = new HashMap<>();
                        for(Entry<String, double[]> entry : e.getValue().entrySet()) {
                        	double[] totalVolumesPerHour = entry.getValue();
                        	double[] averageVolumesPerHour = new double[totalVolumesPerHour.length];
                        	for (int i = 0; i < totalVolumesPerHour.length; i++) {
                                averageVolumesPerHour[i] = totalVolumesPerHour[i] / this.iterationsUsed;
                            }
                        	mode2avgcounts.put(entry.getKey(), averageVolumesPerHour);
                        }
                        averages.put(linkId, mode2avgcounts);
                    }
                } else {
                    averages = this.linkStats;
                }
                
                if(this.config.isFilterModes()) {
                	for(String mode : this.analyzedModes) {
                		Map<Id<Link>, double[]> modalAverage = new HashMap<>();
                		for(Id<Link> linkId : averages.keySet()) {
                			modalAverage.put(linkId, averages.get(linkId).get(mode));                    	
                		}
						final Map<Id<Link>, double[]> modalAverage1 = modalAverage;
						final String mode1 = mode;
                	
						CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(modalAverage1, counts, network, config.getCountsScaleFactor());
						if ((this.config.getDistanceFilter() != null) && (this.config.getDistanceFilterCenterNode() != null)) {
							cca.setDistanceFilter(this.config.getDistanceFilter(), this.config.getDistanceFilterCenterNode());
						}
						cca.setCountsScaleFactor(this.config.getCountsScaleFactor());
						cca.run();
						
						if (this.config.getOutputFormat().contains("txt") ||
								this.config.getOutputFormat().contains("all")) {
							String filename = controlerIO.getIterationFilename(event.getIteration(), mode1.concat("countscompare.txt"));
							CountSimComparisonTableWriter ctw = new CountSimComparisonTableWriter(cca.getComparison(), Locale.ENGLISH);
							ctw.writeFile(filename);
						}	
                	}
                } else {
                	//dont do anything
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
		for (Map.Entry<Id<Link>, Map<String, double[]>> e : this.linkStats.entrySet()) {
			Id<Link> linkId = e.getKey();
			Map<String, double[]> mode2counts =  e.getValue();
			if(  this.config.isFilterModes() ) {
				for (String mode : mode2counts.keySet()) {
					double[] volumesPerHour = mode2counts.get(mode); 
					double[] newVolume = volumes.getVolumesPerHourForLink(linkId, mode); 
					for (int i = 0; i < 24; i++) {
						volumesPerHour[i] += newVolume[i];
					}
					mode2counts.put(mode, volumesPerHour);
				}
				this.linkStats.put(linkId, mode2counts);	
			}
		}
	}
	
	private void reset() {
		this.iterationsUsed = 0;
		for(Map<String, double[]> mode2counts : this.linkStats.values()) {
			for (double[] hours : mode2counts.values()) {
				for (int i = 0; i < hours.length; i++) {
					hours[i] = 0.0;
				}
			}
		}
	}
}