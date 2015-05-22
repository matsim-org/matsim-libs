/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.yu.integration.cadyts.utils;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;

import playground.yu.utils.io.SimpleWriter;
import utilities.math.BasicStatistics;

/**
 * calculates the sample variance of simulated traffic volumes on some places,
 * where the count station are installed
 * 
 * @author Y. Chen
 * 
 */
public class SampleVariance4simVolumes implements StartupListener,
		AfterMobsimListener, ShutdownListener {
	private final Map<Id/* countId */, Map<Integer/* timesteps or hours */, BasicStatistics>> sampleVariances = new TreeMap<Id, Map<Integer, BasicStatistics>>();
	private double countsScaleFactor;

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		Controler ctl = event.getControler();
		VolumesAnalyzer va = ctl.getVolumes();

		for (Id countId : sampleVariances.keySet()) {
			int[] volumes = va.getVolumesForLink(countId);
			Map<Integer, BasicStatistics> localStatistics = sampleVariances
					.get(countId);

			for (Integer timeStep : localStatistics.keySet()) {
				double simTrafVol = volumes != null ? countsScaleFactor
						* volumes[timeStep - 1] : 0d;
				localStatistics.get(timeStep).add(simTrafVol);
				System.out.println(">>>>>\t" + this.getClass().getName()
						+ "\tIteration\t" + event.getIteration()
						+ "\tcount Id\t" + countId.toString() + "\ttimeStep\t"
						+ timeStep + "\tsimulated traffic volume\t"
						+ simTrafVol + " [veh/h]");

			}

		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler ctl = new Controler(args);
		ctl.addControlerListener(new SampleVariance4simVolumes());
		ctl.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		ctl.run();
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		Controler ctl = event.getControler();
        Map<Id<Link>, Count> countsMap = ((Counts) ctl.getScenario().getScenarioElement(Counts.ELEMENT_NAME)).getCounts();

		for (Id countId : countsMap.keySet()) {
			Map<Integer, BasicStatistics> localStatistics = new TreeMap<Integer, BasicStatistics>();
			sampleVariances.put(countId, localStatistics);

			for (Integer timeStep : countsMap.get(countId).getVolumes()
					.keySet()) {
				localStatistics.put(timeStep, new BasicStatistics());
			}
		}

		countsScaleFactor = ctl.getConfig().counts().getCountsScaleFactor();

	}

	public void write(String filename) {
		SimpleWriter writer = new SimpleWriter(filename);
		writer.writeln("countId\ttimeStep (H)\tsample variance");
		writer.flush();
		for (Id countId : sampleVariances.keySet()) {
			Map<Integer, BasicStatistics> localStatistics = sampleVariances
					.get(countId);
			for (Integer timeStep : localStatistics.keySet()) {
				writer.writeln(countId.toString() + "\t" + timeStep + "\t"
						+ localStatistics.get(timeStep).getVar());
				writer.flush();
			}
		}
		writer.close();
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		write(event.getControler().getControlerIO()
				.getOutputFilename("simpleVariance.log"));
	}
}
