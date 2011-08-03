/* *********************************************************************** *
 * project: org.matsim.*
 * QVProfilControlerListener.java
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

package playground.yu.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.counts.Counts;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.parametersCorrection.BseParamCalibrationControlerListener;
import playground.yu.utils.io.SimpleWriter;
import playground.yu.utils.math.SimpleStatistics;

public class QVProfilControlerListener implements StartupListener,
		AfterMobsimListener, ShutdownListener {
	private Logger log = Logger.getLogger(QVProfilControlerListener.class);

	// private class
	private class QVTupleConverter {
		private Set<String> qvTupleStrs;
		private double[] qs, vs;

		public QVTupleConverter(Set<String> tupleStrs) {
			qvTupleStrs = tupleStrs;
			int size = qvTupleStrs.size();
			qs = new double[size];
			vs = new double[size];
			convert();
		}

		private void convert() {
			int n = 0;
			for (String qvTupleStr : qvTupleStrs) {
				String[] qv = qvTupleStr.split("-");
				if (qv.length != 2) {
					System.err.println(qvTupleStr
							+ " should can be splitted into 2 parts");
				}
				qs[n] = Double.parseDouble(qv[0]);
				vs[n] = Double.parseDouble(qv[1]);
				n++;
			}
		}

		/**
		 * @return an array of traffic volumes
		 */
		public double[] getQs() {
			return qs;
		}

		/**
		 * @return an array of speeds
		 */
		public double[] getVs() {
			return vs;
		}
	}

	// ////////////////////////////////////////////////////////
	private Set<Id/* link ID */> interestedLinkIds = null;
	private Map<Id/* link ID */, Set<String/* qv-tuple */>> qvTuples = new HashMap<Id, Set<String>>();
	private CalcLinksAvgSpeed clas;

	private int caliStartTime = 1, caliEndTime = 24,
			cumulativeIterations = 100;
	private double countsScaleFactor = 1d;

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		Controler ctl = event.getControler();
		VolumesAnalyzer va = ctl.getVolumes();/* default timeBin = 3600 */
		Network net = ctl.getNetwork();

		for (Id linkId : interestedLinkIds) {
			// updates q, v
			Set<String> qvTuple = qvTuples.get(linkId);
			if (qvTuple == null) {
				qvTuple = new HashSet<String>();
				qvTuples.put(linkId, qvTuple);
			}

			int[] q = va.getVolumesForLink(linkId);
			if (q == null) {
				q = new int[24];
			}

			for (int time = caliStartTime - 1; time < caliEndTime; time++) {
				double v = clas.getAvgSpeed(linkId, time * 3600);

				qvTuple.add(q[time] * countsScaleFactor/* traffic volume */
						+ "-" + v/* speed [km/h] */);
			}

			// check, whether q,v pair for this link shows jam
			if (event.getIteration() > cumulativeIterations) {
				QVTupleConverter converter = new QVTupleConverter(qvTuple);
				double[] qs = converter.getQs(), vs = converter.getVs();
				Link link = net.getLinks().get(linkId);
				double vCritical = Math.max(getCriticalVelocity(qs, vs),
						0.15 * link.getFreespeed() * 3.6
				/* empirical value approx. 15% of freespeed */);

				double linkCap = net.getLinks().get(linkId).getCapacity()
						/ net.getCapacityPeriod() * 3600d;
				for (int time = caliStartTime - 1; time < caliEndTime; time++) {
					double v = clas.getAvgSpeed(linkId, time * 3600);
					if (v < vCritical) {
						log.warn("Link\t" + linkId + "\tfrom\t" + time
								+ "\tto\t" + (time + 1) + "\tjam\tcapacity\t"
								+ linkCap + "\t[veh/h]\ttraffic volumes\t"
								+ q[time] * countsScaleFactor
								+ "\t[veh/h]\tcritical velocity\t" + vCritical
								+ "\t[km/h]\tspeed\t" + v + "\t[km/h]");
					}
				}
			}
		}

	}

	@Override
	public void notifyStartup(StartupEvent event) {
		Controler ctl = event.getControler();
		Network net = ctl.getNetwork();
		Counts counts = ctl.getCounts();

		// defines links, for which qv-diagrams will be created
		interestedLinkIds = counts != null ? counts.getCounts().keySet() : net
				.getLinks().keySet();

		// defines avg. speed eventshandler
		clas = new CalcLinksAvgSpeed(net, 3600);
		ctl.getEvents().addHandler(clas);

		Config config = ctl.getConfig();
		String caliStartTimeStr = config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				"startTime"), caliEndTimeStr = config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				"endTime");
		if (caliStartTimeStr != null) {
			caliStartTime = Integer.parseInt(caliStartTimeStr);
		}
		if (caliEndTimeStr != null) {
			caliEndTime = Integer.parseInt(caliEndTimeStr);
		}

		countsScaleFactor = ctl.getConfig().counts().getCountsScaleFactor();

		String cumulativeIterationsStr = config.findParam(
				BseParamCalibrationControlerListener.BSE_CONFIG_MODULE_NAME,
				"cumulativeIterations");
		if (cumulativeIterationsStr != null) {
			cumulativeIterations = Integer.parseInt(cumulativeIterationsStr);
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		Controler ctl = event.getControler();
		ControlerIO io = ctl.getControlerIO();

		SimpleWriter indexWriter = new SimpleWriter(
				io.getOutputFilename("linkIdIndex.log"));
		indexWriter.writeln("Index\tlinkId");

		int linkIdx = 0;
		for (Id linkId : qvTuples.keySet()) {
			Set<String> qvTupleSet = qvTuples.get(linkId);
			QVTupleConverter converter = new QVTupleConverter(qvTupleSet);
			double[] qs = converter.getQs(), vs = converter.getVs();

			// writes chart for qv-pair of link?
			XYScatterChart chart = new XYScatterChart("QV-Diagram of link "
					+ linkId, "Q (traffic volumes) [veh/h]",
					"V (speeds) [km/h]");
			chart.addSeries("traffic volumes", qs, vs);
			chart.saveAsPng(io.getOutputFilename("qv" + linkIdx + ".png"),
					1024, 768);

			indexWriter.writeln(linkIdx + "\t" + linkId);
			indexWriter.flush();

			// writes txt for qv-pairs of link?
			SimpleWriter qvWriter = new SimpleWriter(io.getOutputFilename("qv"
					+ linkIdx + ".log"));
			qvWriter.writeln("Q (traffic volumes) [veh/h]\tV (speed) [km/h]");
			for (int ii = 0; ii < qs.length; ii++) {
				qvWriter.writeln(qs[ii] + "\t" + vs[ii]);
				qvWriter.flush();
			}
			qvWriter.close();

			linkIdx++;
		}

		indexWriter.close();
	}

	private double getCriticalVelocity(double[] qs, double[] vs) {
		double qmax = SimpleStatistics.max(qs);
		Set<Integer> qMaxIndexes = new HashSet<Integer>();
		for (int qsIdx = 0; qsIdx < qs.length; qsIdx++) {
			if (qmax == qs[qsIdx]) {
				qMaxIndexes.add(qsIdx);
			}
		}

		if (qMaxIndexes.size() > 1) {
			return vs[SimpleStatistics.maxOfIntegerCollection(qMaxIndexes)];
		}

		return -1d;
	}

}
