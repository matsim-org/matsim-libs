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

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.jfree.chart.plot.XYPlot;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.io.CollectLogMessagesAppender;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.thibautd.analysis.joinabletripsidentifier.DataPloter;
import playground.thibautd.analysis.joinabletripsidentifier.DataPloter.PassengerFilter;
import playground.thibautd.analysis.joinabletripsidentifier.DataPloter.TwofoldTripValidator;
import playground.thibautd.analysis.joinabletripsidentifier.JoinableTrips.JoinableTrip;
import playground.thibautd.analysis.joinabletripsidentifier.JoinableTrips.TripRecord;

/**
 * Executable class able to plot statistics about exported XML data
 *
 * @author thibautd
 */
public class PlotData {
	private static final Logger log =
		Logger.getLogger(PlotData.class);

	private static double LONGER_DIST = 25;

	// config file: data dump, conditions (comme pour extract)
	private static final String MODULE = "jointTripIdentifier";
	private static final String DIST = "acceptableDistance_.*";
	private static final String TIME = "acceptableTime_";
	private static final String DIR = "outputDir";
	private static final String XML_FILE = "xmlFile";
	private static final int WIDTH = 1024;
	private static final int HEIGHT = 800;

	public static void main(final String[] args) {
		// TODO: define a set of minimal distances
		String configFile = args[0];
		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();

		Module module = config.getModule(MODULE);
		String outputDir = module.getValue(DIR);
		String xmlFile = module.getValue(XML_FILE);

		try {
			// create directory if does not exist
			if (!outputDir.substring(outputDir.length() - 1, outputDir.length()).equals("/")) {
				outputDir += "/";
			}
			File outputDirFile = new File(outputDir);
			if (!outputDirFile.exists()) {
				outputDirFile.mkdirs();
			}

			// init logFile
			CollectLogMessagesAppender appender = new CollectLogMessagesAppender();
			Logger.getRootLogger().addAppender(appender);

			IOUtils.initOutputDirLogging(
				outputDir,
				appender.getLogEvents());
		} catch (IOException e) {
			// do NOT continue without proper logging!
			throw new RuntimeException("error while creating log file",e);
		}

		List<ConditionValidator> conditions = new ArrayList<ConditionValidator>();

		Map<String, String> params = module.getParams();

		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (entry.getKey().matches(DIST)) {
				double dist = Double.parseDouble(entry.getValue());
				String num = entry.getKey().split("_")[1];
				double time = Double.parseDouble(params.get(TIME + num));
				conditions.add(new ConditionValidator(dist, time));
			}
		}

		// run ploting function and export
		JoinableTripsXmlReader reader = new JoinableTripsXmlReader();
		reader.parse(xmlFile);
		DataPloter ploter = new DataPloter(reader.getJoinableTrips());
		CommutersFilter filter = new CommutersFilter(network, -1, -1);
		CommutersFilter shortFilter = new CommutersFilter(network, -1, 25);

		int count = 0;
		for (ConditionValidator condition : conditions) {
			log.info("creating charts for condition "+condition);
			count++;
			ChartUtil chart = ploter.getBasicBoxAndWhiskerChart(filter, condition);
			chart.saveAsPng(outputDir+count+"-TimePlot.png", WIDTH, HEIGHT);
			chart = ploter.getBoxAndWhiskerChartPerTripLength(filter, condition, network);
			chart.saveAsPng(outputDir+count+"-DistancePlot.png", WIDTH, HEIGHT);
			chart = ploter.getBoxAndWhiskerChartPerTripLength(shortFilter, condition, network);
			chart.saveAsPng(outputDir+count+"-DistancePlot-short.png", WIDTH, HEIGHT);
		}

		ChartUtil chart = ploter.getTwofoldConditionComparisonChart(filter, conditions);
		chart.saveAsPng(outputDir+"comparisonPlot.png", WIDTH, HEIGHT);
	}
}

class CommutersFilter implements PassengerFilter {
	private static final String WORK_REGEXP = "w.*";
	private static final String HOME_REGEXP = "h.*";

	private final Network network;
	private final double minDistance;
	private final double maxDistance;

	/**
	 * @param network the network to use to compute distance information
	 * @param minTravelDistance the minimal trip length for a trip to be included
	 * in the list of trips to treat. Negative or 0-valued means no lower bound
	 * @param maxTravelDistance the maximal trip length for a trip to be included
	 * in the list of trips to treat. Negative or 0-valued means no upper bound
	 */
	public CommutersFilter(
			final Network network,
			final double minTravelDistance,
			final double maxTravelDistance) {
		this.network = network;
		this.minDistance = minTravelDistance;
		this.maxDistance = maxTravelDistance;
	}

	@Override
	public List<TripRecord> filterRecords(final JoinableTrips trips) {
		List<TripRecord> filtered = new ArrayList<TripRecord>();

		for (TripRecord record : trips.getTripRecords().values()) {
			// only add commuters
			if ( (record.getOriginActivityType().matches(HOME_REGEXP) &&
					 record.getDestinationActivityType().matches(WORK_REGEXP)) ||
					(record.getDestinationActivityType().matches(HOME_REGEXP) &&
					 record.getOriginActivityType().matches(WORK_REGEXP)) ) {
				// check for distance
				if (( (minDistance <= 0) || (minDistance <= record.getDistance(network)) ) &&
				 ( (maxDistance <= 0) || (maxDistance >= record.getDistance(network)) )) {
					filtered.add(record);
				}
			}
		}

		return filtered;
	}

	@Override
	public String getConditionDescription() {
		return "commuter passengers only"+
			(minDistance > 0 ?
				 ", trips longer than "+minDistance+"m" :
				 "")+
			(maxDistance > 0 ?
				 ", trips shorter than "+maxDistance+"m" :
				 "");
	}

	public String toString() {
		return getConditionDescription();
	}
}

class ConditionValidator implements TwofoldTripValidator {
	private final AcceptabilityCondition condition;

	public ConditionValidator(final double distance, final double time) {
		this.condition = new AcceptabilityCondition(distance, time);
	}

	@Override
	public void setJoinableTrips(final JoinableTrips joinableTrips) {}

	@Override
	public boolean isValid(final JoinableTrip driverTrip) {
		return driverTrip.getFullfilledConditions().contains(condition);
	}

	@Override
	public String getConditionDescription() {
		return "all drivers\nacceptable distance = "+condition.getDistance()+" m"+
			"\nacceptable time = "+(condition.getTime()/60d)+" min";
	}

	public String toString() {
		return getConditionDescription();
	}

	@Override
	public Comparable getFirstCriterion() {
		return new Label(condition.getDistance(), "", "m");
	}

	@Override
	public Comparable getSecondCriterion() {
		return new Label (condition.getTime()/60d, "", "min");
	}

	@Override
	public boolean equals(final Object object) {
		if ( !(object instanceof ConditionValidator) ) {
			return false;
		}

		AcceptabilityCondition otherCondition = ((ConditionValidator) object).condition;
		return condition.equals(otherCondition);
	}

	@Override
	public int hashCode() {
		return condition.hashCode();
	}

	private static class Label implements Comparable {
		private final double value;
		private final String name;

		public Label(
				final double value,
				final String prefix,
				final String suffix) {
			this.value = value;
			this.name = prefix + value + suffix;
		}

		public int compareTo(final Object o) {
			return Double.compare(value, ((Label) o).value);
		}

		public String toString() {
			return name;
		}

		public int hashCode() {
			return name.hashCode();
		}

		public boolean equals(final Object o) {
			return name.equals(o);
		}
	}
}

