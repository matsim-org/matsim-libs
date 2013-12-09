/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.kai.run;

import java.util.Formatter;

import org.joda.time.DateTime;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.kai.MyCalcLegTimes;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
public class KNEventsAnalyzer {

	public static void main(String[] args) {
		
		DateTime date = new org.joda.time.DateTime() ;

		Formatter formatter = new Formatter() ;
		String minute = formatter.format( "%2d", date.getMinuteOfHour() ).toString() ;
		formatter.close();
		
		String eventsFilename = args[0] ;
		String populationFilename = args[1] ;
		String networkFilename = args[2] ;
		
		Config config = ConfigUtils.createConfig() ;
		Scenario scenario = ScenarioUtils.createScenario(config) ;
		
		new MatsimNetworkReader(scenario).parse(networkFilename);
		new MatsimPopulationReader(scenario).parse(populationFilename) ;
		
		EventsManager events = new EventsManagerImpl() ;
		
		final MyCalcLegTimes calcLegTimes = new MyCalcLegTimes(scenario);
		events.addHandler(calcLegTimes);
		
		new MatsimEventsReader(events).readFile(eventsFilename) ;
		
		String myDate = date.getYear() + "-" + date.getMonthOfYear() + "-" + date.getDayOfMonth() + "-" + 
				date.getHourOfDay() + "h" + minute ;
		
		calcLegTimes.writeStats(myDate + "_stats_");
	}

}
