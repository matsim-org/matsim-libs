/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatConfig.java
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

package org.matsim.planomat;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;

/**
 * Holds the configuration parameters of the planomat external strategy module,
 * and performs checks for their validity. 
 * 
 * @author meisterk
 *
 */
public class PlanomatConfig {

	public static final String PLANOMAT = "planomat";
	public static final String PLANOMAT_LEG_TRAVEL_TIME_ESTIMATOR = "legTravelTimeEstimator";
	private static String legTravelTimeEstimatorName;
	
	public static final String PLANOMAT_LINK_TRAVEL_TIME_ESTIMATOR = "linkTravelTimeEstimator";
	private static String linkTravelTimeEstimatorName;
	
	public static final String PLANOMAT_JGAP_MAX_GENERATIONS = "jgapMaxGenerations";
	private static int jgapMaxGenerations;
	
	public static final String PLANOMAT_OPTIMIZATION_TOOLBOX = "optimizationToolbox";
	public static final String PLANOMAT_OPTIMIZATION_TOOLBOX_JGAP = "jgap";
	private static String optimizationToolboxName;
	
	public static final String PLANOMAT_INDIFFERENCE = "indifference";
	private static double indifference;
	
	public static final String PLANOMAT_POPSIZE = "populationSize";
	private static int popSize;
	
	private static final String PLANOMAT_BE_VERBOSE = "beVerbose";
	private static boolean beVerbose;
	
	private final static Logger log = Logger.getLogger(PlanomatConfig.class);
	
	public static void init() {
		
		Config config = Gbl.getConfig();
		
		PlanomatConfig.beVerbose = Boolean.parseBoolean(config.findParam(PlanomatConfig.PLANOMAT, PlanomatConfig.PLANOMAT_BE_VERBOSE));
		PlanomatConfig.popSize = Integer.parseInt(config.findParam(PlanomatConfig.PLANOMAT, PlanomatConfig.PLANOMAT_POPSIZE));
		PlanomatConfig.indifference = Double.parseDouble(config.findParam(PlanomatConfig.PLANOMAT, PlanomatConfig.PLANOMAT_INDIFFERENCE));
		PlanomatConfig.jgapMaxGenerations = Integer.parseInt(config.findParam(PlanomatConfig.PLANOMAT, PlanomatConfig.PLANOMAT_JGAP_MAX_GENERATIONS));
		PlanomatConfig.optimizationToolboxName = config.getParam(PlanomatConfig.PLANOMAT, PlanomatConfig.PLANOMAT_OPTIMIZATION_TOOLBOX);
		
		if (PlanomatConfig.optimizationToolboxName.equals(PlanomatConfig.PLANOMAT_OPTIMIZATION_TOOLBOX_JGAP)) {
			
			log.info("Using JGAP optimization toolbox.");
			
		} else {
			Gbl.errorMsg(
					"[Planomat] - Unknown optimization toolbox identifier \"" + 
					PlanomatConfig.optimizationToolboxName + 
					"\". Check parameter 'optimizationToolbox' in module 'planomat'.");
		}

		PlanomatConfig.legTravelTimeEstimatorName = config.getParam(PlanomatConfig.PLANOMAT, PlanomatConfig.PLANOMAT_LEG_TRAVEL_TIME_ESTIMATOR);
		PlanomatConfig.linkTravelTimeEstimatorName = config.getParam(PlanomatConfig.PLANOMAT, PlanomatConfig.PLANOMAT_LINK_TRAVEL_TIME_ESTIMATOR);

	}
	
	public static boolean isBeVerbose() {
		return beVerbose;
	}

	public static int getPopSize() {
		return popSize;
	}

	public static double getIndifference() {
		return indifference;
	}

	public static int getJgapMaxGenerations() {
		return jgapMaxGenerations;
	}

	public static String getOptimizationToolboxName() {
		return optimizationToolboxName;
	}

	public static String getLegTravelTimeEstimatorName() {
		return legTravelTimeEstimatorName;
	}

	public static String getLinkTravelTimeEstimatorName() {
		return linkTravelTimeEstimatorName;
	}

}
