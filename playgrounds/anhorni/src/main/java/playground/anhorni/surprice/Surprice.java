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
	//public static ArrayList<String> days = new ArrayList<String>(Arrays.asList("mon"));
	public static ArrayList<String> modes = new ArrayList<String>(Arrays.asList("car", "pt", "bike", "walk"));
	public static final String SURPRICE_RUN = "surprice_run";
	public static final String SURPRICE_PREPROCESS = "surprice_preprocess";
	
	public static int numberOfIncomeCategories = 9;
	
	// -------------------------------------------------
	// betas need to be negative!!!			
	public static double constant_car = -2.5; // vorher -2.0
    public static double constant_pt = -5.0;
    public static double constant_bike = -7.0;
    public static double constant_walk = 0.0;
     
    // time disutility
    private static double hs = 1.0 / 3600.0; // scaling from hour to seconds
	public static double beta_TT_car_com = -6.0 * hs; // -11.0
	public static double beta_TT_car_shp = -6.0 * hs;	
	public static double beta_TT_car_lei = -6.0 * hs; // -1.0
	
	public static double beta_TT_pt_com = -6.0 * hs;		
	public static double beta_TT_pt_shp = -6.0 * hs;
	public static double beta_TT_pt_lei = -6.0 * hs;
	
	public static double beta_TT_bike_com = -6.0 * hs;
	public static double beta_TT_bike_shp = -6.0 * hs;
	public static double beta_TT_bike_lei = -6.0 * hs;
	
	public static double beta_TT_walk_com = -6.0 * hs;
	public static double beta_TT_walk_shp = -6.0 * hs;
	public static double beta_TT_walk_lei = -6.0 * hs;

	// distance disutility
	private static double km = 0.001; // scaling from km to m
	public static double beta_TD_car_com = -0.2 * km;
	public static double beta_TD_car_shp = -0.2 * km;
	public static double beta_TD_car_lei = -0.2 * km;
	
	public static double beta_TD_pt_com = 0.0;
	public static double beta_TD_pt_shp = 0.0;
	public static double beta_TD_pt_lei = 0.0;
	
	public static double beta_TD_bike_com = -4.0 * km;
	public static double beta_TD_bike_shp = -4.0 * km;
	public static double beta_TD_bike_lei = -4.0 * km;
	
	public static double beta_TD_walk_com = -1.0 * km;
	public static double beta_TD_walk_shp = -1.0 * km;
	public static double beta_TD_walk_lei = -1.0 * km;
	
	// monetary costs
	public static double distanceCost_car = -0.0005; 
	public static double constantCost_car = -5.0; // parking
	
	public static double distanceCost_pt = -0.0001;
	public static double constantCost_pt = -5.0; // zone tickets
	
	public static double distanceCost_bike = 0.0;
	public static double constantCost_bike = 0.0;
	
	public static double distanceCost_walk = 0.0;
	public static double constantCost_walk = 0.0;
	
	// -------------------------------------------------
	
    public static double lag_purpose_car = 0.0;
    public static double lag_purpose_pt = 0.0;
    public static double lag_purpose_bike = 0.0;
    public static double lag_purpose_walk = 0.0;
    
    public static double lag_time_car = 0.0;
    public static double lag_time_pt = 0.0;
    public static double lag_time_bike = 0.0;
    public static double lag_time_walk = 0.0;
}
