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

package playground.sergioo.passivePlanning.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scoring.OpeningTimesTollsFaresScoringFunctionFactory;

import playground.sergioo.passivePlanning.core.controler.PassivePlanningControler;
import playground.sergioo.passivePlanning.core.mobsim.passivePlanning.PassivePlanningSocialFactory;


/**
 * This is currently only a substitute to the full Controler. 
 *
 * @author sergioo
 */
public class Controler {

	private final PassivePlanningControler controler;
	
	public Controler(final String[] args) {
		this.controler = new PassivePlanningControler(args);
		controler.setScoringFunctionFactory(new OpeningTimesTollsFaresScoringFunctionFactory(controler.getConfig().planCalcScore(), getScenario()));
		controler.setPassivePlaningSocial(true);
	}
	
	public Controler(final String configFilename) {
		this.controler = new PassivePlanningControler(configFilename);
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
		Controler c = new Controler(args);
		c.setOverwriteFiles(true);
		c.run();
	}
	
}