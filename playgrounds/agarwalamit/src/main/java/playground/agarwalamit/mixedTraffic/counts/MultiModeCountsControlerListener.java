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

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.matsim.analysis.IterationStopWatch;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.Link;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Counts;

/**
 * @author amit after dgrether
 */
public class MultiModeCountsControlerListener implements StartupListener, IterationEndsListener {

	/*
	 * String used to identify the operation in the IterationStopWatch.
	 */
	public static final String OPERATION_COMPARECOUNTS = "compare with counts";

	private ControlerConfigGroup controlerConfigGroup;
	private final CountsConfigGroup config; // useful to get the link ids of counting stations and other infor for averaging
	private final Set<String> analyzedModes;
	private final VolumesAnalyzer volumesAnalyzer;
	private final IterationStopWatch iterationStopwatch;
	private final OutputDirectoryHierarchy controlerIO;

	@com.google.inject.Inject(optional=true)
	private Counts<Link> counts = null;

	private final Map<Id<Link>, Map<String,double[]>> linkStats = new HashMap<>();
	private int iterationsUsed = 0;

	@Inject
	MultiModeCountsControlerListener(QSimConfigGroup qsimConfigGroup, ControlerConfigGroup controlerConfigGroup, CountsConfigGroup countsConfigGroup, VolumesAnalyzer volumesAnalyzer, IterationStopWatch iterationStopwatch, OutputDirectoryHierarchy controlerIO) {
		this.controlerConfigGroup = controlerConfigGroup;
		this.config = countsConfigGroup; 
		this.volumesAnalyzer = volumesAnalyzer;
		this.analyzedModes =  new HashSet<>(qsimConfigGroup.getMainModes());
		this.iterationStopwatch = iterationStopwatch;
		this.controlerIO = controlerIO;
	}

	@Override
	public void notifyStartup(final StartupEvent controlerStartupEvent) {
		if (counts != null) {
			for (Id<Link> linkId : counts.getCounts().keySet()) {
				Map<String, double[]> mode2counts = new HashMap<>();
				for(String mode : this.analyzedModes){
					mode2counts.put(mode, new double [24]);
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

				String filename = controlerIO.getIterationFilename(event.getIteration(), "multiMode_countscompare.txt");
				BufferedWriter writer = IOUtils.getBufferedWriter(filename);
				try {
					writer.write("linkID\tMode\t");
					for(int t =1; t <=24; t++) {
						writer.write(t+"\t");
					}
					writer.newLine();

					for(Id<Link> linkId : averages.keySet()) {
						for(String mode : this.linkStats.get(linkId).keySet()) {
							writer.write(linkId+"\t");
							writer.write(mode+"\t");
							
							if(this.linkStats.get(linkId).get(mode).length<24) throw new RuntimeException("time bins are smaller than 24. Aborting...");
							
							for(double d : this.linkStats.get(linkId).get(mode)) {
								writer.write(d*config.getCountsScaleFactor()+"\t");
							}
							writer.newLine();
						}
					}
					writer.close();
				} catch (Exception e) {
					throw new RuntimeException("Data is not written. Reason :"+e);
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