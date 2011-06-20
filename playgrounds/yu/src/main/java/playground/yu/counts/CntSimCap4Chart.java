/* *********************************************************************** *
 * project: org.matsim.*
 * CntSimCap4Chart.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 *
 */
package playground.yu.counts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.counts.Count;
import org.matsim.counts.Volume;

import playground.yu.utils.container.Collection2Array;
import playground.yu.utils.io.SimpleWriter;

/**
 * prepares and creates chart with x-achse
 *
 * @author yu
 *
 */
public class CntSimCap4Chart implements StartupListener, AfterMobsimListener,
		ShutdownListener {

	private Collection<Integer> iters = new ArrayList<Integer>();

	private List<SimpleWriter> writers = new ArrayList<SimpleWriter>();
	private List<List<Double>> volumes = new ArrayList<List<Double>>();

	// keep the same sequence of counts*time

	@Override
	public void notifyStartup(StartupEvent event) {
		Controler ctl = event.getControler();
		Map<Id, Count> countsMap = ctl.getCounts().getCounts();
		for (Entry<Id, Count> countEntry : countsMap.entrySet()) {
			StringBuilder str = new StringBuilder("cntSimCap_");
			str.append(countEntry.getKey());// count station ID
			str.append("_");
			for (Integer time : countEntry.getValue().getVolumes().keySet()) {
				StringBuilder str2 = new StringBuilder(str);
				str2.append(time);// when it's counted
				str2.append(".log");
				writers.add(new SimpleWriter(ctl.getControlerIO()
						.getOutputFilename(str2.toString())));
				volumes.add(new ArrayList<Double>());
			}
		}
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		Controler ctl = event.getControler();
		int iter = event.getIteration();
		iters.add(iter);
		double cntScaleFac = ctl.getConfig().counts().getCountsScaleFactor();
		VolumesAnalyzer volsAnalyzer = ctl.getVolumes();

		int idx = 0;
		for (Entry<Id, Count> countEntry : ctl.getCounts().getCounts()
				.entrySet()) {
			Id linkId = countEntry.getKey();
			int[] vols = volsAnalyzer.getVolumesForLink(linkId);
			for (int i = 0; i < vols.length; i++) {
				System.out.println("vols[" + i + "] =\t" + vols[i]);
			}
			// timeBin is 3600d in Controler
			for (Integer time : countEntry.getValue().getVolumes().keySet()) {
				System.out.println("idx =\t" + idx
						+ "\nthis.volumes(List<<List<Double>>) =\t" + volumes);
				volumes.get(idx++).add(vols[time - 1] * cntScaleFac);
				System.out.println("idx =\t" + idx
						+ "\nthis.volumes(List<<List<Double>>) =\t" + volumes);
			}
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {

		Controler ctl = event.getControler();
		int lastIter = ctl.getLastIteration(), firstIter = ctl
				.getFirstIteration();
		Map<Id, Count> countsMap = ctl.getCounts().getCounts();
		double[] xs = Collection2Array.toDoubleArray(iters);
		iters.clear();

		Network net = ctl.getNetwork();

		int volIdx = 0;
		for (Entry<Id, Count> countEntry : countsMap.entrySet()) {
			StringBuilder str = new StringBuilder("cntSimCap_");
			Id cntId = countEntry.getKey();
			String cntIdStr = cntId.toString();
			str.append(cntIdStr);
			str.append("_");

			double[] caps = new double[xs.length];
			double cap = net.getLinks().get(cntId).getCapacity()
					/ (net.getCapacityPeriod() / 3600d);
			for (int idx = 0; idx < caps.length; idx++) {
				caps[idx] = cap;
			}

			for (Entry<Integer, Volume> timeVol : countEntry.getValue()
					.getVolumes().entrySet()) {

				Integer time = timeVol.getKey();

				double[] cntVols = new double[xs.length];
				double cntVol = timeVol.getValue().getValue();
				for (int idx = 0; idx < cntVols.length; idx++) {
					cntVols[idx] = cntVol;
				}

				SimpleWriter writer = writers.get(volIdx);
				StringBuilder head = new StringBuilder("count station Id:\t");
				head.append(cntIdStr);
				head.append("\n");
				head.append("time:\t");
				head.append(time - 1 + ":00 - " + time + ":00\n");
				head.append("capacity:\t");
				head.append(cap);
				head.append("\t[veh/h]\n");
				head.append("measurement:\t");
				head.append(cntVol);
				head.append("\t[veh/h]\n");
				head.append("iteration\tsimulated traffic volume [veh/h]");
				writer.writeln(head);

				double[] vols = new double[xs.length];
				vols = Collection2Array
						.toArrayFromDouble(volumes.get(volIdx++));

				XYLineChart chart = new XYLineChart(
						"Capacity, Measurement, and simulated traffic volume through count station\t"
								+ cntIdStr + "\t(" + (time - 1) + ":00 - "
								+ time + ":00)", "Iteration", "veh/h");

				chart.addSeries("capacity", xs, caps);
				chart.addSeries("measurement", xs, cntVols);
				chart.addSeries("simulated traffic volume", xs, vols);

				StringBuilder str2 = new StringBuilder(str);
				str2.append(time);// when it's counted
				str2.append(".png");
				chart.saveAsPng(
						ctl.getControlerIO().getOutputFilename(str2.toString()),
						1024, 256);

				for (int i = 0; i < xs.length; i++) {
					StringBuilder line = new StringBuilder(
							Double.toString(xs[i]));
					line.append("\t");
					line.append(vols[i]);
					writer.writeln(line);
					writer.flush();
				}
				writer.close();
			}
		}

	}
}
