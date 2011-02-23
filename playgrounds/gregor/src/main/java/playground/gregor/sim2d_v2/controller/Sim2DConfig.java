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
package playground.gregor.sim2d_v2.controller;

//TODO make a config group instead of using this hard coded static stuff!!!
@Deprecated
public class Sim2DConfig {

	public static double STATIC_FORCE_RESOLUTION = .05;
	public static final double TIME_STEP_SIZE =  1. /25;

	public static final double Bpath = .5;
	public static final double PSqrSensingRange = 100;
	public static final double PNeighborhoddRange = 10;

	public static final double Bp = 5;
	public static final double MaxWallSensingRange = 5;// wall
	public static final double Bw = 0.5;// wall
	public static final double App = 10.;
	public static final double Apath =1000. * TIME_STEP_SIZE;
	public static final double Apw = 30.;
	public static final double tau = 1; //1./ TIME_STEP_SIZE;
	public static final double B_PATH = 3;

	public static final boolean LOAD_STATIC_ENV_FIELD_FROM_FILE = true;

	public static final boolean LOAD_NETWORK_FROM_XML_FILE = false;
	public static final boolean NETWORK_LOADERII = false;
	public static final boolean NETWORK_LOADER_LS = true;

	public static final double NEIGHBORHOOD_UPDATE = 1;
	public static final boolean DEBUG = false;
	public static final boolean XYZEvents = true;

}
