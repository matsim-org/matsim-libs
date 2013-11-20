/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice;

import java.util.ArrayList;
import java.util.Arrays;

public class Surprice {
	
	public static ArrayList<String> days = new ArrayList<String>(Arrays.asList("mon", "tue", "wed", "thu", "fri", "sat", "sun"));
	public static ArrayList<String> modes = new ArrayList<String>(Arrays.asList("car", "pt", "slm"));
	public static final String SURPRICE_RUN = "surprice_run";
	public static final String SURPRICE_PREPROCESS = "surprice_preprocess";
	
	// -------------------------------------------------
	// betas and constants need to be negative!!!
			
	public static double constant_car = 0.0;
    public static double constant_pt = 0.0;
    public static double constant_slm = 0.0;
     
    // time disutility
    private static double hs = 1.0 / 3600.0; // scaling from hour to seconds
	public static double beta_TT_car_com = -6.0 * hs;
	public static double beta_TT_car_shp = -3.0 * hs;	
	public static double beta_TT_car_lei = -1.0 * hs;
	
	public static double beta_TT_pt_com = -4.0 * hs;		
	public static double beta_TT_pt_shp = -2.0 * hs;
	public static double beta_TT_pt_lei = -1.0 * hs;
	
	public static double beta_TT_slm_com = -2.0 * hs;
	public static double beta_TT_slm_shp = -2.0 * hs;
	public static double beta_TT_slm_lei = -2.0 * hs;

	// distance disutility
	public static double beta_TD_car_com = 0.0;
	public static double beta_TD_car_shp = 0.0;
	public static double beta_TD_car_lei = 0.0;
	
	public static double beta_TD_pt_com = 0.0;
	public static double beta_TD_pt_shp = 0.0;
	public static double beta_TD_pt_lei = 0.0;
	
	public static double beta_TD_slm_com = 0.0;
	public static double beta_TD_slm_shp = 0.0;
	public static double beta_TD_slm_lei = 0.0;
	
	// monetary costs
	public static double distanceCost_car = -0.0005; 
	public static double constantCost_car = -5.0; // parking
	
	public static double distanceCost_pt = -0.00025;
	public static double constantCost_pt = -10.0; // zone tickets
	
	public static double distanceCost_slm = 0.0;
	public static double constantCost_slm = 0.0;	
	
	// -------------------------------------------------
	
    public static double lag_purpose_car = - 2.0;
    public static double lag_purpose_pt = 2.0;
    public static double lag_purpose_slm = 2.0;
    
    public static double lag_time_car = 2.0;
    public static double lag_time_pt = 2.0;
    public static double lag_time_slm = 2.0;
}
