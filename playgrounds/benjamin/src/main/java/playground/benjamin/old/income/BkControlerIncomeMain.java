/* *********************************************************************** *
 * project: org.matsim.*
 * BKickIncomeControlerMain
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
package playground.benjamin.old.income;

import playground.benjamin.BkPaths;


public class BkControlerIncomeMain {
	
	public static void main(String[] args){
//		String config = DgPaths.STUDIESDG + "einkommenSchweiz/config_households_all_zrh30km_10pct.xml";
//		String config = DgPaths.RUNBASE + "run724/resumeConfig.xml";
//		new BKickIncomeControler(config).run();
		
		String config = BkPaths.RUNSSVN + "run734/resumeConfig.xml";
		new BkControlerIncome(config).run();
	}

}
