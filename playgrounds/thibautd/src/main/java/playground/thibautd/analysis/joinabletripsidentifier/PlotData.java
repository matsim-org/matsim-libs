/* *********************************************************************** *
 * project: org.matsim.*
 * PlotData.java
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
package playground.thibautd.analysis.joinabletripsidentifier;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.collections.Tuple;
import playground.ivt.utils.MoreIOUtils;
import playground.thibautd.utils.charts.ChartsAxisUnifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Executable class able to plot statistics about exported XML data
 *
 * @author thibautd
 */
public class PlotData {
	private static final Logger log =
		Logger.getLogger(PlotData.class);

	// for lisibility, charts per distance with a maximal
	// distance are produced. For Zürich, distances longer than 30km
	// do not make sense due to the way the population is defined.
	private final static double LONGER_DIST = 25 * 1000;
	private final static double LONGER_DRIVER_DIST = 25 * 1000;

	// the "condition validator" will not only reject trips not
	// fullfilling a condition, but also the ones which fullfill the condition
	// but with a "walk distance" > ALPHA_WALK * trip_distance.
	// the idea is to eliminate short trips for which a lot of driver trips are
	// identified, but for which it is not very relevant
	private final static double ALPHA_WALK = 0.25;

	// config file: data dump, conditions (comme pour extract)
	private static final String MODULE = "jointTripIdentifier";
	private static final String DIST = "acceptableDistance_.*";
	private static final String TIME = "acceptableTime_";
	private static final String DIR = "outputDir";
	private static final String XML_FILE = "xmlFile";
	//private static final int WIDTH = 1024;
	//private static final int HEIGHT = 800;
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;

	public static void main(final String[] args) {
		// TODO: define a set of minimal distances
		String configFile = args[0];
		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();

		ConfigGroup module = config.getModule(MODULE);
		String outputDir = module.getValue(DIR);
		String xmlFile = module.getValue(XML_FILE);

		MoreIOUtils.initOut( outputDir );

		List<ConditionValidator> conditions = new ArrayList<ConditionValidator>();

		Map<String, String> params = module.getParams();

		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (entry.getKey().matches(DIST)) {
				int dist = (int) Math.round( Double.parseDouble(entry.getValue()) );
				String num = entry.getKey().split("_")[1];
				int time = (int) Math.round( Double.parseDouble(params.get(TIME + num)) );
				conditions.add(
						new ConditionValidator(
							network,
							dist,
							time,
							ALPHA_WALK));
			}
		}

		// run ploting function and export
		JoinableTripsXmlReader reader = new JoinableTripsXmlReader();
		reader.parse(xmlFile);
		DataPloter ploter = new DataPloter(reader.getJoinableTrips());
		CommutersFilter filter = new CommutersFilter(network, -1, -1);
		CommutersFilter shortFilter = new CommutersFilter(network, -1, LONGER_DIST);
		ShortDriverTripValidator shortDriverTripValidator =
			new ShortDriverTripValidator( network , LONGER_DRIVER_DIST );

		List<ChartsAxisUnifier> unifiers = new ArrayList<ChartsAxisUnifier>();
		ChartsAxisUnifier perDistanceUnifier = new ChartsAxisUnifier( false , true );
		unifiers.add( perDistanceUnifier );
		ChartsAxisUnifier perTimeUnifier = new ChartsAxisUnifier( false , true );
		unifiers.add( perTimeUnifier );
		ChartsAxisUnifier passengersPerDriverUnifier = new ChartsAxisUnifier( false , true );
		unifiers.add( passengersPerDriverUnifier );

		List< Tuple< String , ChartUtil > > charts = 
			 new ArrayList< Tuple< String , ChartUtil > >();

		int count = 0;
		for (ConditionValidator condition : conditions) {
			log.info("creating charts for condition "+condition);
			count++;

			// number of joinable trips per passenger
			// -----------------------------------------------------------------
			ChartUtil chart = ploter.getBasicBoxAndWhiskerChart(
					filter,
					condition);
			perTimeUnifier.addChart( chart );
			charts.add( new Tuple<String, ChartUtil>(
						outputDir+count+"-TimePlot",
						chart) );

			chart = ploter.getBoxAndWhiskerChartPerTripLength(
					filter,
					condition,
					network);
			perDistanceUnifier.addChart( chart );
			charts.add( new Tuple<String, ChartUtil>(
						outputDir+count+"-DistancePlot",
						chart) );

			chart = ploter.getBoxAndWhiskerChartPerTripLength(
					shortFilter,
					condition,
					network);
			perDistanceUnifier.addChart( chart );
			charts.add( new Tuple<String, ChartUtil>(
						outputDir+count+"-DistancePlot-short",
						chart) );

			// Number of possible passenger per driver
			// -----------------------------------------------------------------
			chart = ploter.getBoxAndWhiskerChartNPassengersPerDriverTripLength(
					filter,
					condition,
					network);
			passengersPerDriverUnifier.addChart( chart );
			charts.add( new Tuple<String, ChartUtil>(
						outputDir+count+"-nPassengers-per-drivers",
						chart) );

			shortDriverTripValidator.setValidator( condition );
			chart = ploter.getBoxAndWhiskerChartNPassengersPerDriverTripLength(
					filter,
					shortDriverTripValidator,
					network);
			passengersPerDriverUnifier.addChart( chart );
			charts.add( new Tuple<String, ChartUtil>(
						outputDir+count+"-nPassengers-per-drivers-short",
						chart) );


			// VIA: home and work locations of passengers and drivers
			// -----------------------------------------------------------------
			List<Coord> locations = ploter.getMatchingLocations(
					filter,
					condition,
					network,
					true,
					true,
					"h.*");
			ploter.writeViaXy(
					locations,
					outputDir+condition.getFirstCriterion()+"-"
						+condition.getSecondCriterion()+"-homeLocations.xy");

			locations = ploter.getMatchingLocations(
					filter,
					condition,
					network,
					true,
					true,
					"w.*");
			ploter.writeViaXy(
					locations,
					outputDir+condition.getFirstCriterion()+"-"
						+condition.getSecondCriterion()+"-workLocations.xy");
		}


		// "global" (multicondition) charts
		{
			// number of joint trips per condition
			// -----------------------------------------------------------------
			ChartUtil chart = ploter.getTwofoldConditionComparisonChart(filter, conditions);
			charts.add( new Tuple<String, ChartUtil>(
						outputDir+"comparisonPlot",
						chart) );

			// departure histogram for the filtered passenger trips
			// -----------------------------------------------------------------
			chart = ploter.getTripsForCondition(filter);
			charts.add( new Tuple<String, ChartUtil>(
						outputDir+"departuresPerTimeSlotPlot",
						chart) );

			// Proportion of passengers with joint trip
			// -----------------------------------------------------------------
			chart = ploter.getTwoFoldConditionProportionOfPassengers(filter, conditions);
			charts.add( new Tuple<String, ChartUtil>(
						outputDir+"proportionOfPotentialRideSharers",
						chart) );
		}

		// format and save
		// ---------------------------------------------------------------------
		for (ChartsAxisUnifier unifier : unifiers) {
			unifier.applyUniformisation();
		}
		for (Tuple<String, ChartUtil> chart : charts) {
			ChartUtil chartUtil = chart.getSecond();
			chartUtil.saveAsPng(
					chart.getFirst()+".png",
					WIDTH,
					HEIGHT);
			chartUtil.getChart().setTitle( "" );
			chartUtil.saveAsPng(
					chart.getFirst()+"-no-title.png",
					WIDTH,
					HEIGHT);
		}
	}
}
