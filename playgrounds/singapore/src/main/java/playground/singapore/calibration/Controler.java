/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;

import java.util.HashSet;


/**
 * This is a substitute for the full Controler incorporating graphical output for calibration.
 *
 * @author artemc
 */

public class Controler {

	private final org.matsim.core.controler.Controler controler;
	
	/**The path for the 2 benchmark/survey .csv files with distance and travel time distributions according to the mode share
	 * The format of these files have to be according to the examples provided in folder "benchmarkData" 
	 * 
	 * */
	
	private static final String[] SURVEY_FILES = {"./input/distanceByModeSurvey.csv", "./input/travelTimeByModeSurvey.csv"};
	
	/**Creates a new controler and adds a CalibrtaionStatsListener to it*/
	
	public Controler(final String[] args) {
		this.controler = new org.matsim.core.controler.Controler(args);
		this.controler.addControlerListener(new CalibrationStatsListener(this.controler.getEvents(), SURVEY_FILES,1, "Travel Survey (Benchmark)", "Red_Scheme", new HashSet<Id<Person>>()));
	}
	
	public Controler(final String configFilename) {
		this.controler = new org.matsim.core.controler.Controler(configFilename);
		this.controler.addControlerListener(new CalibrationStatsListener(this.controler.getEvents(), SURVEY_FILES,1, "Travel Survey (Benchmark)", "Red_Scheme", new HashSet<Id<Person>>()));
	}
	
	public void setOverwriteFiles(final boolean overwriteFiles) {
		this.controler.setOverwriteFiles(overwriteFiles);
	}
	
	public Scenario getScenario() {
		return this.controler.getScenario() ;
	}
	
	public void run() {
		this.controler.run();
	}
	
	public  static void main(String[] args) {
		new Controler(args).run();
	}
}