/* *********************************************************************** *
 * project: org.matsim.*
 * Gbl.java
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

package playground.anhorni.crossborder;


public class Config {
	
	// Writer: --------------------------------------------------------------------------
	public static final String DTD = "http://www.matsim.org/files/dtd/plans_v4.dtd";
	public static final String OUTFILE = "output/plansCB.xml";
	public static final String plansName="crossborder trafic";
	
	// taken from "Nationale Quell/Zielmatrizen"
	// is this valid for "Tageszeitliche Matrizen"?	
	//public static final int chNumbers=1000000 NQZ S.21;
	
	public static final int chNumbers=1000000;
	//-----------------------------------------------------------------------------------
		
	//InitDemandCreation-----------------------------------------------------------------
	public static final String zones2NodesFile="input/Bezirke2Knoten.att";
	public static final String  networkFile="input/network.xml";
	//-----------------------------------------------------------------------------------
	
	public static boolean lookAtTransit=true;
	
	//Values see TGZM: page 31
	public static double [] calibration={1.1931,
										 0.6857,
										 0.9676,
										 1.4612,
										 1.4984,
										 1.9264,
										 1.4817,
										 1.2663,
										 1.1775,
										 1.0486,
										 1.0231,
										 0.9868,
										 0.9589,
										 0.9489,
										 1.0694,
										 1.0861,
										 0.9950,
										 0.8863,
										 0.8201,
										 0.7463,
										 0.7155,
										 0.8038,
										 0.7914,
										 0.7840};
}
