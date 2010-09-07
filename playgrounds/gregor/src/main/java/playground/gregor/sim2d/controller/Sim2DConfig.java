/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DConfig.java
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
package playground.gregor.sim2d.controller;

//TODO make a config group instead of using this hard coded static stuff!!!
@Deprecated
public class Sim2DConfig {

	public static double STATIC_FORCE_RESOLUTION = 0.25;
	public static final double TIME_STEP_SIZE = 0.25;
	
	public static final double Bpath = 0.5;
	public static final double PSqrSensingRange = 12.25;
	public static final double Bp = 1.5;
	public static final double Bw = 1.5;//wall
	public static final double App = 500.;
	public static final double Apath =20.;
	public static final double Apw = 2000.;
	public static final double tau = 1;
	public static final double B_PATH = 3;
	
	public static final String STATIC_FORCE_FIELD_FILE = "/home/laemmel/devel/sim2d/data/staticForceField.xml.gz";
	public static final boolean LOAD_STATIC_FORCE_FIELD_FROM_FILE = true;
	
	public static final boolean LOAD_NETWORK_FROM_XML_FILE = true;
	public static final String FLOOR_SHAPE_FILE = "/home/laemmel/devel/sim2d/data/duisburg/duisburg.shp";
	public static final boolean NETWORK_LOADERII = true;
	public static final boolean NETWORK_LOADER_LS = true;
	public static final String LS_SHAPE_FILE = "/home/laemmel/devel/sim2d/data/duisburg/d_ls.shp";
	public static final double NEIGHBORHOOD_UPDATE = 1;
	
}
