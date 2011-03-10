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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

import playground.yu.integration.cadyts.demandCalibration.withCarCounts.BseLinkCostOffsetsXMLFileIO;
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
public class DestinationUtilOffset_ActDuration extends
		ActivityLocationUtilOffsetExtractor implements
		ActivityStartEventHandler, X2QGIS {
	protected final CharyparNagelScoringParameters params;
	private Map<String/* shortActType */, List<Double>> aggrAct_tripUtilOffsets = new HashMap<String, List<Double>>();
	private Map<String/* shortActType */, List<Double>> aggrAct_typicalDurations = new HashMap<String, List<Double>>();
	private Map<String/* ActType */, List<Double>> act_tripUtilOffsets = new HashMap<String, List<Double>>();

	public DestinationUtilOffset_ActDuration(Network net, Counts counts,
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
			Double legUtilOffset = tmpAgentLegUtilOffsets./**/remove(agentId)/**/;
			if (legUtilOffset != null) {
				// int time = getTimeStep(event.getTime());
				// Coord actLoc = this.getGridCenterCoord(event.getLinkId());
				// // //////////////////////
				// this.addGridUtilOffset(time, actLoc, legUtilOffset);
				// // /////////////////////////
				String actType = event.getActType();
				String shortActType = actType.substring(0, 1);

				List<Double> tripUtilOffsets = aggrAct_tripUtilOffsets
						.get(shortActType);
				if (tripUtilOffsets == null) {
					tripUtilOffsets = new ArrayList<Double>();
					aggrAct_tripUtilOffsets.put(shortActType, tripUtilOffsets);
				}
				tripUtilOffsets.add(legUtilOffset);

				// ---------------------------TESTS---------------------
				if (legUtilOffset != 0d) {
					List<Double> tuo4act = act_tripUtilOffsets.get(actType);
					if (tuo4act == null) {
						tuo4act = new ArrayList<Double>();
						act_tripUtilOffsets.put(actType, tuo4act);
					}
					tuo4act.add(legUtilOffset);
				}
				// ---------------------------TESTS---------------------

				List<Double> typicalDurations = aggrAct_typicalDurations
						.get(shortActType);
				if (typicalDurations == null) {
					typicalDurations = new ArrayList<Double>();
					aggrAct_typicalDurations
							.put(shortActType, typicalDurations);
				}
				typicalDurations.add(params.utilParams.get(actType)
						.getTypicalDuration() / 3600d/* [h] */);
			}
		}
	}

	public void output(String outputFilenameBase) {
		XYScatterChart chart = new XYScatterChart(
				"tripUtilOffset <-> activityTypicalDuration",
				"trip utility offset", "activity typical duration");
		for (String shortActType : aggrAct_tripUtilOffsets.keySet()) {
			chart.addSeries(shortActType, Collection2Array
					.toArrayFromDouble(aggrAct_tripUtilOffsets
							.get(shortActType)), Collection2Array
					.toArrayFromDouble(aggrAct_typicalDurations
							.get(shortActType)));
		}

		chart.saveAsPng(outputFilenameBase + ".png", 1024, 768);

		// ----------------------TEST--------------------------
		for (String actType : act_tripUtilOffsets.keySet()) {
			DistributionCreator creator = new DistributionCreator(
					act_tripUtilOffsets.get(actType), 0.1);
			creator.createChart(outputFilenameBase + "Distribution." + actType
					+ ".png",
					"utility offset distribution of trip towards activity "
							+ actType, "trip utility offset",
					"number of trips with utility offset");

		}

		// ----------------------TEST--------------------------

		// /////////////////////////////////////////////////////
		for (String shortActType : aggrAct_tripUtilOffsets.keySet()) {
			XYScatterChart chart2 = new XYScatterChart(
					"tripUtilOffset <-> activityTypicalDuration",
					"trip utility offset", "activity typical duration");
			chart2.addSeries(shortActType, Collection2Array
					.toArrayFromDouble(aggrAct_tripUtilOffsets
							.get(shortActType)), Collection2Array
					.toArrayFromDouble(aggrAct_typicalDurations
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
		, outputFilenameBase = "test/DestinationUtilOffset2/1000.destUtiloffset.tuo_atd"//
		, configFilename = "test/DestinationUtilOffset/analysis.xml"//
		;

		int arStartTime = 7, arEndTime = 20, lowerLimit = 50;

		Config config = ConfigUtils.loadConfig(configFilename);
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		Network net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		BseLinkCostOffsetsXMLFileIO utilOffsetIO = new BseLinkCostOffsetsXMLFileIO(
				net);
		DynamicData<Link> linkUtilOffsets = utilOffsetIO
				.read(linkOffsetUtilOffsetFilename);

		ActivityLocationUtilOffsetExtractor aluoe = new DestinationUtilOffset_ActDuration(
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
