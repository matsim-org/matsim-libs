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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.counts.algorithms.CountsHtmlAndGraphsWriter;
import org.matsim.counts.algorithms.graphs.CountsErrorGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimReal24GraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimRealPerHourGraphCreator;

/**
 * @author dgrether
 */
public class CountControlerListener implements StartupListener, IterationEndsListener {

	private final CountsConfigGroup config;
	private final Counts counts;
	private final Map<Id, double[]> linkStats = new HashMap<Id, double[]>();
	private int iterationsUsed = 0;

	public CountControlerListener(final CountsConfigGroup config) {
		this.config = config;
		this.counts = new Counts();
	}

	@Override
	public void notifyStartup(final StartupEvent controlerStartupEvent) {
		MatsimCountsReader counts_parser = new MatsimCountsReader(this.counts);
		counts_parser.readFile(this.config.getCountsFileName());
		
		for (Id linkId : this.counts.getCounts().keySet()) {
			this.linkStats.put(linkId, new double[24]);
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if (this.config.getWriteCountsInterval() <= 0) {
			return;
		}
		Controler controler = event.getControler();
		
		if (useVolumesOfIteration(event.getIteration(), controler.getFirstIteration())) {
			addVolumes(controler.getVolumes());
		}

		if (createCountsInIteration(event.getIteration())) {
			controler.stopwatch.beginOperation("compare with counts");
			
			Map<Id, double[]> averages = null;
			if (this.iterationsUsed > 1) {
				averages = new HashMap<Id, double[]>();
				for (Map.Entry<Id, double[]> e : this.linkStats.entrySet()) {
					Id linkId = e.getKey();
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
//			CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(controler.getLinkStats(), this.counts, controler.getNetwork(), controler.getConfig().counts().getCountsScaleFactor());
			CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(averages, this.counts, controler.getNetwork(), controler.getConfig().counts().getCountsScaleFactor());
			if ((this.config.getDistanceFilter() != null) && (this.config.getDistanceFilterCenterNode() != null)) {
				cca.setDistanceFilter(this.config.getDistanceFilter(), this.config.getDistanceFilterCenterNode());
			}
			cca.setCountsScaleFactor(this.config.getCountsScaleFactor());
			cca.run();

			if (this.config.getOutputFormat().contains("html") ||
					this.config.getOutputFormat().contains("all")) {
				CountsHtmlAndGraphsWriter cgw = new CountsHtmlAndGraphsWriter(event.getControler().getControlerIO().getIterationPath(event.getIteration()), cca.getComparison(), event.getIteration());
				cgw.addGraphsCreator(new CountsSimRealPerHourGraphCreator("sim and real volumes"));
				cgw.addGraphsCreator(new CountsErrorGraphCreator("errors"));
				cgw.addGraphsCreator(new CountsLoadCurveGraphCreator("link volumes"));
				cgw.addGraphsCreator(new CountsSimReal24GraphCreator("average working day sim and count volumes"));
				cgw.createHtmlAndGraphs();
			}
			if (this.config.getOutputFormat().contains("kml")||
					this.config.getOutputFormat().contains("all")) {
				String filename = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "countscompare.kmz");
				CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
						cca.getComparison(), controler.getNetwork(), TransformationFactory.getCoordinateTransformation(controler.getConfig().global().getCoordinateSystem(), TransformationFactory.WGS84 ));
				kmlWriter.setIterationNumber(event.getIteration());
				kmlWriter.writeFile(filename);
			}
			if (this.config.getOutputFormat().contains("txt")||
					this.config.getOutputFormat().contains("all")) {
				String filename = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "countscompare.txt");
				CountSimComparisonTableWriter ctw=new CountSimComparisonTableWriter(cca.getComparison(),Locale.ENGLISH);
				ctw.writeFile(filename);
			}
			reset();
			controler.stopwatch.endOperation("compare with counts");
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
		for (Map.Entry<Id, double[]> e : this.linkStats.entrySet()) {
			Id linkId = e.getKey();
			double[] volumesPerHour = e.getValue(); 
			double[] newVolume = volumes.getVolumesPerHourForLink(linkId);
			for (int i = 0; i < 24; i++) {
				volumesPerHour[i] += newVolume[i];
			}
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

	public Counts getCounts() {
		return this.counts;
	}
}
