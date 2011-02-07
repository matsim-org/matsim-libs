/* *********************************************************************** *
 * project: org.matsim.*
 * DestinationUtilOffsetExtractor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

import playground.yu.integration.cadyts.demandCalibration.withCarCounts.BseLinkCostOffsetsXMLFileIO;
import playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.ActivityTimeDistribution.RealActivityTimes;
import playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset.forLeg.ActivityLocationUtilOffsetExtractor;
import playground.yu.integration.cadyts.demandCalibration.withCarCounts.utils.qgis.ActivityLocationUtilOffset2QGIS;
import playground.yu.utils.container.Collection2Array;
import playground.yu.utils.io.DistributionCreator;
import playground.yu.utils.qgis.X2QGIS;
import cadyts.utilities.misc.DynamicData;

/**
 * shows the Utility Offset of activity location for terminating traffic
 * 
 * @author yu
 * 
 */
public class DestinationUtilOffset_realActStartTime extends
		ActivityLocationUtilOffsetExtractor implements
		ActivityStartEventHandler, ActivityEndEventHandler, X2QGIS {

	protected final CharyparNagelScoringParameters params;
	private Map<String/* shortActType */, List<Double>> aggrAct_tripUtilOffsets = new HashMap<String, List<Double>>();
	private Map<String/* shortActType */, List<Double>> aggrAct_realActStartTimes = new HashMap<String, List<Double>>();
	private Map<Id/* agentId */, Double/* startTime */> agentArrivalTimes = new HashMap<Id, Double>();
	private Map<Double/**/, List<Double>> realStartTime_tripUtilOffsets = new HashMap<Double, List<Double>>();

	public DestinationUtilOffset_realActStartTime(Network net, Counts counts,
			DynamicData<Link> linkUtilOffsets, int caliStartTime,
			int caliEndTime, int lowerLimit, double gridLength,
			CharyparNagelScoringParameters params) {
		super(net, counts, linkUtilOffsets, caliStartTime, caliEndTime,
				lowerLimit, gridLength);
		this.params = params;
	}

	public void handleEvent(ActivityStartEvent event) {
		if (event != null) {
			Id agentId = event.getPersonId();

			// ----------------for real activity startTime-----------------
			agentArrivalTimes.put(agentId, event.getTime());
			// ------------------------------------------------------------

		}
	}

	public void handleEvent(ActivityEndEvent event) {
		if (event != null) {
			Id agentId = event.getPersonId();
			if (agentArrivalTimes.containsKey(agentId)) {
				double arrivalTime = agentArrivalTimes.remove(agentId);
				double departureTime = event.getTime();
				String type = event.getActType();

				RealActivityTimes realActTimes = new RealActivityTimes(
						arrivalTime, departureTime, params.utilParams.get(type));
				double realStartTime = realActTimes.getRealActivityStartTime();

				String actType = event.getActType();
				String shortActType = actType.substring(0, 1);

				Double legUtilOffset = tmpAgentLegUtilOffsets
						./**/remove(agentId)/**/;
				if (legUtilOffset != null) {

					List<Double> tripUtilOffsets = aggrAct_tripUtilOffsets
							.get(shortActType);
					if (tripUtilOffsets == null) {
						tripUtilOffsets = new ArrayList<Double>();
						aggrAct_tripUtilOffsets.put(shortActType,
								tripUtilOffsets);
					}
					tripUtilOffsets.add(legUtilOffset);
					// ------------------------------------------------

					if (legUtilOffset != 0d) {
						double starTimeKey = (int) realStartTime / 1800 * 1800;
						List<Double> tuosOfRast = realStartTime_tripUtilOffsets
								.get(starTimeKey);/*
												 * e.g. [0-900), [900-1800)
												 */
						if (tuosOfRast == null) {
							tuosOfRast = new ArrayList<Double>();
							realStartTime_tripUtilOffsets.put(starTimeKey,
									tuosOfRast);
						}
						tuosOfRast.add(legUtilOffset);
					}
					// ---------------------------------------------
					List<Double> realActStartTimes = aggrAct_realActStartTimes
							.get(shortActType);
					if (realActStartTimes == null) {
						realActStartTimes = new ArrayList<Double>();
						aggrAct_realActStartTimes.put(shortActType,
								realActStartTimes);

					}
					realActStartTimes.add(realStartTime);

				}

			}
		}
	}

	public void output(String outputFilenameBase) {
		XYScatterChart chart = new XYScatterChart(
				"tripUtilOffset <-> real activity start time",
				"trip utility offset", "real activity start time");
		for (String shortActType : aggrAct_tripUtilOffsets.keySet()) {
			chart.addSeries(shortActType, Collection2Array
					.toArrayFromDouble(aggrAct_tripUtilOffsets
							.get(shortActType)), Collection2Array
					.toArrayFromDouble(aggrAct_realActStartTimes
							.get(shortActType)));
		}

		chart.saveAsPng(outputFilenameBase + ".png", 1024, 768);
		// -------------------------------------------------------
		for (Double startTimekey : realStartTime_tripUtilOffsets.keySet()) {
			DistributionCreator creator = new DistributionCreator(
					realStartTime_tripUtilOffsets.get(startTimekey), 0.25);
			creator
					.createChart(
							outputFilenameBase + ".from" + startTimekey
									+ ".png",
							"utiltiy offset distribution of trips with the destination activity real start time "
									+ startTimekey, "trip utility offset",
							"number of trips with utility offset in x axis");
		}
		// /////////////////////////////////////////////////////
		for (String shortActType : aggrAct_tripUtilOffsets.keySet()) {
			XYScatterChart chart2 = new XYScatterChart(
					"tripUtilOffset <-> real activity start time of "
							+ shortActType, "trip utility offset",
					"real activity start time of " + shortActType);
			chart2.addSeries(shortActType, Collection2Array
					.toArrayFromDouble(aggrAct_tripUtilOffsets
							.get(shortActType)), Collection2Array
					.toArrayFromDouble(aggrAct_realActStartTimes
							.get(shortActType)));
			chart2.saveAsPng(outputFilenameBase + "." + shortActType + ".png",
					1024, 768);
		}
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String linkOffsetUtilOffsetFilename = "test/DestinationUtilOffset/1000.linkCostOffsets.xml"//
		, networkFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml"//
		, countsFilename = "../schweiz-ivtch-SVN/baseCase/counts/countsIVTCH.xml"//
		, eventsFilename = "test/DestinationUtilOffset/1000.events.txt.gz"//
		, outputFilenameBase = "test/DestinationUtilOffset2/1000.destUtiloffset.tuo_rast"//
		, configFilename = "test/DestinationUtilOffset/analysis.xml"//
		;

		int arStartTime = 7, arEndTime = 20, lowerLimit = 50;

		Config config = ConfigUtils.loadConfig(configFilename);
		Scenario scenario = new ScenarioImpl(config);
		Network net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		BseLinkCostOffsetsXMLFileIO utilOffsetIO = new BseLinkCostOffsetsXMLFileIO(
				net);
		DynamicData<Link> linkUtilOffsets = utilOffsetIO
				.read(linkOffsetUtilOffsetFilename);

		ActivityLocationUtilOffsetExtractor aluoe = new DestinationUtilOffset_realActStartTime(
				net, counts, linkUtilOffsets, arStartTime, arEndTime,
				lowerLimit, 1000d, new CharyparNagelScoringParameters(config
						.planCalcScore()));

		EventsManager events = new EventsManagerImpl();
		// /////////////////////////////////
		events.addHandler(aluoe);
		// /////////////////////////////////
		new MatsimEventsReader(events).readFile(eventsFilename);

		aluoe.output(outputFilenameBase);

		Map<Integer, Map<Coord, Tuple<Integer, Double>>> gridUtilOffsets = aluoe
				.getGridUtilOffsets();
		for (Entry<Integer, Map<Coord, Tuple<Integer, Double>>> timeGridUtilOffsetsPair : gridUtilOffsets
				.entrySet()) {
			if (timeGridUtilOffsetsPair.getValue().size() > 0) {
				ActivityLocationUtilOffset2QGIS aluo2qgis = new ActivityLocationUtilOffset2QGIS(
						scenario, ch1903, 1000d, timeGridUtilOffsetsPair
								.getValue());
				aluo2qgis.writeShapeFile(outputFilenameBase + "grid."
						+ timeGridUtilOffsetsPair.getKey() + ".shp");
			}
		}
	}

}
