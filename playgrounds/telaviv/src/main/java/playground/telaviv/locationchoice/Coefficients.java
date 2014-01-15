/* *********************************************************************** *
 * project: org.matsim.*
 * Coefficients
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

package playground.telaviv.locationchoice;

import java.util.ArrayList;
import java.util.Arrays;

/*
 * Some constants for the location choice modules.
 */
public class Coefficients {
	
	//public static ArrayList<String> types = new ArrayList<String>(Arrays.asList("Shop", "Other", "Work", "Education")); // added by anhorni
	public static ArrayList<String> types = new ArrayList<String>(Arrays.asList("shopping", "leisure")); // adapted for shopping and leisure (==other???)


	// Zonal Indicators						Shop	Other		Work		Education
	public double[] metroCult 		= new double[]{	0.75,	 1.34,		 0.0,		 0.0};
	public double[] locCultur 		= new double[]{	0.291,	 0.188,		 0.0, 		 0.0};
	public double[] university 	= new double[]{-2.79,	-2.1,		 0.0,		 0.617};
	public double[] highschool 	= new double[]{ 0.135,	 0.174,		 0.0,		 0.617};
	public double[] school 		= new double[]{ 0.346,	 0.324,		 0.0, 	 	 0.617};
	public double[] majorOffice 	= new double[]{ 0.0,	-0.0578,	 0.0456,	 0.0};
	public double[] otherOffice 	= new double[]{ 0.0,	 0.426,		 0.0456,	 0.0};
	public double[] mall 			= new double[]{ 1.53,	 0.291,		 0.0365,	 0.0};
	public double[] shopstreet 	= new double[]{ 0.191,	 0.0916, 	 0.0365,	 0.0};	
	public double[] market 		= new double[]{ 2.14,	 0.875,		 0.0365,	 0.0};
	public double[] health 		= new double[]{ 0.0,	 1.61,		 0.207, 	 0.111};
	public double[] urban 			= new double[]{ 0.683,	-0.395,		 0.0,		-1.18};
	public double[] jew 			= new double[]{-0.358,	-0.919,		-0.545,		-0.165};
	public double[] islam 			= new double[]{ 0.0494,	 0.0703,	-0.182,		 0.252};
	
	//	Size variables
	public double[] areaSizeFactor 	= new double[]{0.0,	 	 0.0,	0.0, 	0.0};
	public double[] serviceEmployment 	= new double[]{-5.73,	 -999,	0.0, 	0.0};
	public double[] totalEmployment 	= new double[]{-6.35,	-6.06,	0.541,	0.0};
	public double[] students 			= new double[]{ -999,	-2.84,	0.0,	0.571};
	public double[] households			= new double[]{-5.17,	-4.66,	0.0,	0.0};

	// Level of Service Variables
	public double[] autodist	= new double[]{-0.455,		-0.358,		-0.275,		 -0.4};
	public double[] autodist2	= new double[]{ 0.0099,		 0.007,	 	 0.0058,	 0.0088};
	public double[] autodist3	= new double[]{-0.000099,	-0.000062,	-0.000054,	-0.000081};

	public double[] dum10_10	= new double[]{1.45,	2.45,	 0.90,	1.05};
	public double[] dum10_20	= new double[]{0.00,	0.30,	 0.60,	0.00};
	public double[] dum10_3040	= new double[]{0.00,	0.50,	 0.60,	0.00};
	public double[] dum20_10	= new double[]{0.40,	1.29,	 0.10,	0.20};
	public double[] dum3040_10	= new double[]{1.35,	1.89,	 0.10,	0.20};
	public double[] dum3040_20	= new double[]{0.00,	0.00,	-0.30,	0.00};

	// LogSum from ModeChoice
	public double[] logSumModeChoice 	= new double[]{0.0269,	0.0432,	0.263,	0.117};
	public double[] betaDriverTime 	= new double[]{-0.0458,	-0.0458,	-0.0402,	-0.0402};
}
