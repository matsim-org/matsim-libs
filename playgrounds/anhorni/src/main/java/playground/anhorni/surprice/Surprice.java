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
	
	public static double mean = 0.0;
	public static double stdDev = 1.0;
	
	public static double costPerKm;
	
	public static double constantCar;
    public static double constantPt;
    public static double constantSlm;
       
    public static double beta_TT_car_com;
    public static double beta_TT_car_bus;
    public static double beta_TT_car_shp;
    public static double beta_TT_car_lei;
    
    public static double beta_TT_pt_com;
    public static double beta_TT_pt_bus;
    public static double beta_TT_pt_shp;
    public static double beta_TT_pt_lei;
    
    public static double beta_TT_slm_com;
    public static double beta_TT_slm_bus;
    public static double beta_TT_slm_shp;
    public static double beta_TT_slm_lei;
    
    public static double beta_TC_pt_com;
    public static double beta_TC_pt_bus;
    public static double beta_TC_pt_shp;
    public static double beta_TC_pt_lei;
    
    public static double beta_TC_car_com;
    public static double beta_TC_car_bus;
    public static double beta_TC_car_shp;
    public static double beta_TC_car_lei;
    
    public static double beta_TC_slm_com;
    public static double beta_TC_slm_bus;
    public static double beta_TC_slm_shp;
    public static double beta_TC_slm_lei;
        
    public static double lambda_I_TT_car_com;
    public static double lambda_I_TT_car_bus;
    public static double lambda_I_TT_car_shp;
    public static double lambda_I_TT_car_lei;
    
    public static double lambda_I_TT_slm_com;
    public static double lambda_I_TT_slm_bus;
    public static double lambda_I_TT_slm_shp;
    public static double lambda_I_TT_slm_lei;
        
    public static double lambda_I_TT_pt_com;
    public static double lambda_I_TT_pt_bus;
    public static double lambda_I_TT_pt_shp;
    public static double lambda_I_TT_pt_lei;
    
    public static double lambda_I_TC_car_com;
    public static double lambda_I_TC_car_bus;
    public static double lambda_I_TC_car_shp;
    public static double lambda_I_TC_car_lei;
    
    public static double lambda_I_TC_pt_com;
    public static double lambda_I_TC_pt_bus;
    public static double lambda_I_TC_pt_shp;
    public static double lambda_I_TC_pt_lei;
        
    public static double lambda_D_TC_slm_com;
    public static double lambda_D_TC_slm_bus;
    public static double lambda_D_TC_slm_shp;
    public static double lambda_D_TC_slm_lei;
    
    public static double lambda_D_TT_car_com;
    public static double lambda_D_TT_car_bus;
    public static double lambda_D_TT_car_shp;
    public static double lambda_D_TT_car_lei;
    
    public static double lambda_D_TT_slm_com;
    public static double lambda_D_TT_slm_bus;
    public static double lambda_D_TT_slm_shp;
    public static double lambda_D_TT_slm_lei;
        
    public static double lambda_D_TT_pt_com;
    public static double lambda_D_TT_pt_bus;
    public static double lambda_D_TT_pt_shp;
    public static double lambda_D_TT_pt_lei;
    
    public static double lambda_D_TC_car_com;
    public static double lambda_D_TC_car_bus;
    public static double lambda_D_TC_car_shp;
    public static double lambda_D_TC_car_lei;
    
    public static double lambda_D_TC_pt_com;
    public static double lambda_D_TC_pt_bus;
    public static double lambda_D_TC_pt_shp;
    public static double lambda_D_TC_pt_lei;
        
    public static double lambda_I_TC_slm_com;
    public static double lambda_I_TC_slm_bus;
    public static double lambda_I_TC_slm_shp;
    public static double lambda_I_TC_slm_lei;
    
    public static double lag_purpose_car;
    public static double lag_purpose_pt;
    public static double lag_purpose_slm;
    
    public static double lag_time_car;
    public static double lag_time_pt;
    public static double lag_time_slm;
}
