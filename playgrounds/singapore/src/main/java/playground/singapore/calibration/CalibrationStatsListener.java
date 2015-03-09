/* *********************************************************************** *
 * project: org.matsim.*
 * LegHistogramListener.java
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

package playground.singapore.calibration;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import playground.singapore.calibration.handlers.DistanceDistributionTrip;
import playground.singapore.calibration.handlers.TimeDistributionTrip;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 
 * @author artemc
 *
 * This is a listener for calibration output, which gets called at the end of each iteration
 * Paths for the 2 benchmark/survey files with distance and travel time distributions according to the mode share have to bee provided  
 * Interval parameter defines after how many iterations calibrations graphs have to be produced
 * So far only outputs for
 * - mode share history through the iterations
 * - distance distribution per mode class 
 * - travel time distribution per mode class 
 * have been implemented
 */


public class CalibrationStatsListener implements IterationEndsListener {

	private String surveyName;
	private String colorScheme;
	private int interval;
	private final CalibrationGraphGenerator generator;
	private TimeDistributionTrip timeJourneyHandler;
	private String[] categoriesDistanceDataset;
	private String[] modesDistanceDataset;
	private String[] categoriesTTDataset;
	private String[] modesTTDataset;
	private SortedMap<Integer, Integer[]> numberTripsPerMode = new TreeMap<Integer, Integer[]>();
	private Set<Id<Person>> pIdsToExclude;


	/** Set graph output units, km and min recommended*/
	private String outputDistanceUnit ="km";
	private String outputTimeUnit = "min";

	static private final Logger log = Logger.getLogger(CalibrationStatsListener.class);


	/**Constructor for CalibrationsStats Listener, where the benchmark/survey data files are loaded and calibrataion output interval and color scheme are defined
	 *
	 * Color schemes implemented so far: Red_Scheme, Autumn, Muted Rainbow, Lollapalooza, French Girl, M8_Colors (partially borrowed from kuler.adobe.com)
	 *  Additional color schemes can be added inside the class GraphEditor
	 */

	public CalibrationStatsListener(final EventsManager events, final String[] surveyFiles, int interval, String surveyName, String colorScheme, Set<Id<Person>> pIdsToExclude) {
		this.pIdsToExclude = pIdsToExclude;
		this.surveyName = surveyName;
		this.colorScheme = colorScheme;
		this.interval=interval;
		timeJourneyHandler = new TimeDistributionTrip(this.pIdsToExclude);
		events.addHandler(timeJourneyHandler);
		this.generator = new CalibrationGraphGenerator(outputDistanceUnit, outputTimeUnit);
		try {
			generator.getSurveyData(surveyFiles[0], surveyFiles[1]);
		} catch (IOException e) {
			log.error(e.getMessage());
		}

		/**Get modes and categories of benchmark/survey data*/
		this.categoriesDistanceDataset =  generator.getSurveyDistDataset().getCategories();
		this.modesDistanceDataset = generator.getSurveyDistDataset().getModes();
		this.categoriesTTDataset =  generator.getSurveyTTDataset().getCategories();
		this.modesTTDataset = generator.getSurveyTTDataset().getModes();		
		generator.surveyDistDataset.calculateSharesAndTotals();
		generator.surveyTTDataset.calculateSharesAndTotals();

	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if(event.getIteration()%interval==0) {
			try {
                DistanceDistributionTrip distanceDistribution = new DistanceDistributionTrip(event.getControler().getScenario().getPopulation(), event.getControler().getScenario().getNetwork(), event.getControler().getScenario().getTransitSchedule(),this.pIdsToExclude);
				distanceDistribution.saveChains();
				SortedMap<Integer, Integer[]>  distanceTripsMap = distanceDistribution.getDistribution(categoriesDistanceDataset, modesDistanceDataset);
				distanceDistribution.printDistribution(distanceTripsMap, event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "tripDistanceHistogramByMode.csv"));
				SortedMap<Integer, Integer[]>  travelTimeTripsMap = timeJourneyHandler.getDistribution(categoriesTTDataset, modesTTDataset);
				timeJourneyHandler.printDistribution(travelTimeTripsMap, event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "tripTravelTimeHistogramByMode.csv"));
				Integer[] totals = new Integer[modesDistanceDataset.length];

				for(int i=0; i<totals.length; i++)
					totals[i] = 0;
				for(Integer[] journeys:distanceTripsMap.values())
					for(int i=0; i<journeys.length; i++)
						totals[i] += journeys[i]==null?0:journeys[i];
				
				numberTripsPerMode.put(event.getIteration(), totals);		
				String path = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "");
				(new File(path.substring(0,path.lastIndexOf("/"))+"/calibration")).mkdir();
				generator.createCalibrationCharts(colorScheme, distanceTripsMap, travelTimeTripsMap, numberTripsPerMode, path, surveyName);
				timeJourneyHandler.reset(event.getIteration());
			} catch (IOException e) {
				log.error(e.getMessage());
			}
		}
	}

}
