/* *********************************************************************** *
 * project: org.matsim.*
 * RunBraessExperiments.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package matsimConnector.run;

import matsimConnector.utility.Constants;



public class RunBraessExperiments {
	public static void main(String [] args) {
		Constants.VIS = true;

		Constants.ENVIRONMENT_FILE = "environmentGrid_Braess_WL.csv";
		Constants.BRAESS_WL = true;
		Constants.MARGINAL_SOCIAL_COST_OPTIMIZATION = true;
		Constants.OUTPUT_PATH = "/Users/laemmel/devel/CACAIE/braess_wl_so/";
		LoadAndRunCASimulation.main(new String[]{});

		Constants.ENVIRONMENT_FILE = "environmentGrid_Braess_WL.csv";
		Constants.BRAESS_WL = true;
		Constants.MARGINAL_SOCIAL_COST_OPTIMIZATION = false;
		Constants.OUTPUT_PATH = "/Users/laemmel/devel/CACAIE/braess_wl_ne/";
		LoadAndRunCASimulation.main(new String[]{});
		
		Constants.ENVIRONMENT_FILE = "environmentGrid_Braess.csv";
		Constants.BRAESS_WL = false;
		Constants.MARGINAL_SOCIAL_COST_OPTIMIZATION = true;
		Constants.OUTPUT_PATH = "/Users/laemmel/devel/CACAIE/braess_so/";
		LoadAndRunCASimulation.main(new String[]{});
		
		Constants.ENVIRONMENT_FILE = "environmentGrid_Braess.csv";
		Constants.BRAESS_WL = false;
		Constants.MARGINAL_SOCIAL_COST_OPTIMIZATION = false;
		Constants.OUTPUT_PATH = "/Users/laemmel/devel/CACAIE/braess_ne/";
		LoadAndRunCASimulation.main(new String[]{});

	}

}
