/* *********************************************************************** *
 * project: org.matsim.*
 * LegHisto.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.yu.analysis.pt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.charts.XYLineChart;

/**
 * Counts the number of person and ptDriver departed, arrived or got stuck per
 * time bin based on events.
 *
 * @author yu
 */
public class LegHisto implements AgentDepartureEventHandler,
		AgentArrivalEventHandler, AgentStuckEventHandler {
	private final Map<String, ModeData> data = new HashMap<String, ModeData>(
			5, 0.85f);
	private int binSize, nofBins;
	private ModeData drvrModesData;

	private static class ModeData {
		public final double[] countsDep;
		public final double[] countsArr;
		public final double[] countsStuck;

		public ModeData(final int nofBins) {
			this.countsDep = new double[nofBins];
			this.countsArr = new double[nofBins];
			this.countsStuck = new double[nofBins];
		}
	}

	/**
	 * @param binSize
	 */
	public LegHisto(int binSize) {
		this(binSize, 30 * 3600 / binSize + 1);
	}

	public LegHisto(final int binSize, final int nofBins) {
		this.binSize = binSize;
		this.nofBins = nofBins;
		this.drvrModesData = new ModeData(nofBins + 1);
	}

	/* private methods */

	private int getBinIndex(final double time) {
		int bin = (int) (time / this.binSize);
		if (bin >= this.nofBins) {
			return this.nofBins;
		}
		return bin;
	}

	private ModeData getDataForMode(String legMode) {
		ModeData modeData = this.data.get(legMode);
		if (modeData == null) {
			modeData = new ModeData(nofBins + 1); // +1 for all times out of our
			// range
			this.data.put(legMode, modeData);
		}
		return modeData;
	}

	private double[] getEnRoutes(ModeData modeData) {
		double[] enRoutes = new double[nofBins + 1];
		enRoutes[0] = modeData.countsDep[0] - modeData.countsArr[0]
				- modeData.countsStuck[0];
		for (int i = 1; i < modeData.countsDep.length; i++)
			enRoutes[i] = enRoutes[i - 1] + modeData.countsDep[i]
					- modeData.countsArr[i] - modeData.countsStuck[i];
		return enRoutes;
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		int index = getBinIndex(event.getTime());
		if (event.getPersonId().toString().startsWith("pt"))
			this.drvrModesData.countsArr[index]++;
		else if (event.getLegMode() != null) {
			ModeData modeData = getDataForMode(event.getLegMode());
			modeData.countsArr[index]++;
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		int index = getBinIndex(event.getTime());
		if (event.getPersonId().toString().startsWith("pt"))
			this.drvrModesData.countsDep[index]++;
		else if (event.getLegMode() != null) {
			ModeData modeData = getDataForMode(event.getLegMode());
			modeData.countsDep[index]++;
		}
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		int index = getBinIndex(event.getTime());
		if (event.getPersonId().toString().startsWith("pt"))
			this.drvrModesData.countsStuck[index]++;
		else if (event.getLegMode() != null) {
			ModeData modeData = getDataForMode(event.getLegMode());
			modeData.countsStuck[index]++;
		}
	}

	@Override
	public void reset(int iteration) {
	}

	public void write(String outputFilenameBase) {
		double[] xs = new double[nofBins + 1];
		for (int i = 0; i < nofBins + 1; i++) {
			xs[i] = (double) binSize * (double) i / 3600.0;
		}

		XYLineChart chartA = new XYLineChart("leg Histogram - arrvials",
				"time (h)", "arrivals");
		XYLineChart chartD = new XYLineChart("leg Histogram - departures",
				"time (h)", "departures");
		XYLineChart chartE = new XYLineChart("leg Histogram - enRoutes",
				"time (h)", "enRoutes");
		XYLineChart chartS = new XYLineChart("leg Histogram - stucks",
				"time (h)", "stucks");

		chartA.addSeries("ptDriver", xs, drvrModesData.countsArr);
		chartD.addSeries("ptDriver", xs, drvrModesData.countsDep);
		chartS.addSeries("ptDriver", xs, drvrModesData.countsStuck);
		chartE.addSeries("ptDriver", xs, getEnRoutes(drvrModesData));

		List<String> modes = new ArrayList<String>();
		modes.addAll(data.keySet());
		Collections.sort(modes);

		for (String mode : modes) {
			String modeName = mode.toString();
			ModeData modeData = data.get(mode);
			chartA.addSeries(modeName, xs, modeData.countsArr);
			chartD.addSeries(modeName, xs, modeData.countsDep);
			chartS.addSeries(modeName, xs, modeData.countsStuck);
			chartE.addSeries(modeName, xs, getEnRoutes(modeData));
		}

		chartA.saveAsPng(outputFilenameBase + "Arr.png", 1024, 768);
		chartD.saveAsPng(outputFilenameBase + "Dep.png", 1024, 768);
		chartE.saveAsPng(outputFilenameBase + "Enr.png", 1024, 768);
		chartS.saveAsPng(outputFilenameBase + "Stu.png", 1024, 768);
	}

	public static void main(String[] args) {
		String eventsFilename = "../berlin-bvg09/pt/m2_schedule_delay/160p600sWaiting-6_4plansWoPerform/ITERS/it.1000/1000.events.xml.gz";

		EventsManager em = new EventsManagerImpl();

		LegHisto lh = new LegHisto(60);
		em.addHandler(lh);

		new MatsimEventsReader(em).readFile(eventsFilename);

		lh
				.write("../berlin-bvg09/pt/m2_schedule_delay/160p600sWaiting-6_4plansWoPerform/ITERS/it.1000/1000.legHisto.");
	}
}
