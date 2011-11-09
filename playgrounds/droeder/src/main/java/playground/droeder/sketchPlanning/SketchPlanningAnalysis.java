/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.sketchPlanning;

import playground.droeder.Analysis.DrAnalysis;

/**
 * @author droeder
 *
 */
public class SketchPlanningAnalysis {
	private final static String CONFIGDIR = "D:/VSP/svn/shared/studies-droeder/sketchPlanning/";
	private final static String CONFIGBASE = CONFIGDIR + "config_base.xml";
	private final static String CONFIGBA16EXT = CONFIGDIR + "config_ba16.xml";
	private final static String CONFIGBA17EXT = CONFIGDIR + "config_ba16_17.xml";
	private final static String CONFIGBA17_ST_EXT = CONFIGDIR + "config_ba16_17_storkower.xml";
	
	
	public static void main(String[] args){
		DrAnalysis base = new DrAnalysis(CONFIGBASE, null);
		base.run();

		DrAnalysis plan = new DrAnalysis(CONFIGBA16EXT, null);
		plan.run();
		DrAnalysis.diffNetShp(base.getNumAgentsByLinkAndSlice(), base.getNet(), plan.getNumAgentsByLinkAndSlice(), plan.getNet(), plan.getOutDir() + "diffNet.shp");
		
		plan = new DrAnalysis(CONFIGBA17EXT, null);
		plan.run();
		DrAnalysis.diffNetShp(base.getNumAgentsByLinkAndSlice(), base.getNet(), plan.getNumAgentsByLinkAndSlice(), plan.getNet(), plan.getOutDir() + "diffNet.shp");
		
		plan = new DrAnalysis(CONFIGBA17_ST_EXT, null);
		plan.run();
		DrAnalysis.diffNetShp(base.getNumAgentsByLinkAndSlice(), base.getNet(), plan.getNumAgentsByLinkAndSlice(), plan.getNet(), plan.getOutDir() + "diffNet.shp");
	}

}
