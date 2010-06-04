/* *********************************************************************** *
 * project: org.matsim.*
 * DRoedRunner
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
package playground.droeder;

import playground.droeder.gershensonSignals.GershensonRunner;



/**
 * @author dgrether
 *
 */
public class DgDRoedRunner {

  /**
   * @param args
   */
  public static void main(String[] args) {
  	boolean liveVis = true;
    String cottbusConfigMath = "/media/data/work/repos/shared-svn/studies/dgrether/cottbus/cottbusConfig.xml";
    String denverConfigGershenson = "/home/dgrether/shared-svn/studies/droeder/denver/dgDenverConfig.xml";
    
//    String config = denverConfigGershenson;
    String config = cottbusConfigMath;
    
    
    GershensonRunner runner;
		//Denver opti
    //	runner = new GershensonRunner(14, 468, 0.65, 55, 38, false, false);
    //	runner.setSignalPlanBounds(21600, 22000, 22240);
    //	runner.runScenario(config);
	
		//cottbus opti		
//	runner = new GershensonRunner(31, 215, 0.75, 55, 107, false, false);
//	runner.setSignalPlanBounds(21600, 22000, 22240);
//	runner.runScenario("C");
	
//	time	d	u	cap	n	maxRed
//	318.64	89.0	22	0.7	145	30	
    runner = new GershensonRunner(22,145, 0.7,89.0,30, false, liveVis);
    runner.setSignalPlanBounds(21600, 22000, 22240);
    runner.runScenario(config);
    
    
  }

}
