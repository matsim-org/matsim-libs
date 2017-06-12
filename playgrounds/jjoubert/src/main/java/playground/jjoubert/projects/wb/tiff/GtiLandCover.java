/* *********************************************************************** *
 * project: org.matsim.*
 * GtiLandCover.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jjoubert.projects.wb.tiff;

/**
 * A land cover classification based on GeoTerraImage's 72 Class GTI South 
 * African National Land Cover Dataset (2013/2014).
 * 
 * @author jwjoubert
 */
public enum GtiLandCover {
	PERMANENT_WATER (1, "Permanent water"),
	SEASONAL_WATER (2, "Seasonal water"),
	WETLAND (3, "Wetland"),
	INDIGENOUS_FOREST (4, "Indigenous forest"),
	DENSE_BUSH (5, "Dense bush, thicket & tall dense shrubs"),
	WOODLAND (6, "Woodland and open bushland"),
	GRASSLAND (7, "Grassland"),
	LOW_SHRUBLAND_FYNBOS (8, "Low shrubland: Fynbos"),
	LOW_SHRUBLAND_OTHER (9, "Low shrubland: Other"),
	CULTIVATED_COMMERCIAL_ANNUAL_HIGH (10, "Commercial annuals (rainfed): NDVI profile high"),
	CULTIVATED_COMMERCIAL_ANNUAL_MED (11, "Commercial annuals (rainfed): NDVI profile med"),
	CULTIVATED_COMMERCIAL_ANNUAL_LOW (12, "Commercial annuals (rainfed): NDVI profile low"),
	CULTIVATED_COMMERCIAL_PIVOT_HIGH (13, "Commercial pivot: NDVI profile high"),
	CULTIVATED_COMMERCIAL_PIVOT_MED (14, "Commercial pivot: NDVI profile med"),
	CULTIVATED_COMMERCIAL_PIVOT_LOW (15, "Commercial pivot: NDVI profile low"),
	CULTIVATED_COMMERCIAL_ORCHARD_HIGH (16, "Commercial permanent orchards: NDVI profile high"),
	CULTIVATED_COMMERCIAL_ORCHARD_MED (17, "Commercial permanent orchards: NDVI profile med"),
	CULTIVATED_COMMERCIAL_ORCHARD_LOW (18, "Commercial permanent orchards: NDVI profile low"),
	CULTIVATED_COMMERCIAL_VINES_HIGH (19, "Commercial permanent viticulture: NDVI profile high"),
	CULTIVATED_COMMERCIAL_VINES_MED (20, "Commercial permanent viticulture: NDVI profile med"),
	CULTIVATED_COMMERCIAL_VINES_LOW (21, "Commercial permanent viticulture: NDVI profile low"),
	CULTIVATED_COMMERCIAL_PINE (22, "Commercial permanent pineapples"),
	CULTIVATED_SUBSISTENCE_HIGH (23, "Subsistence: NDVI profile high"),
	CULTIVATED_SUBSISTENCE_MED (24, "Subsistence: NDVI profile med"),
	CULTIVATED_SUBSISTENCE_LOW (25, "Subsistence: NDVI profile low"),
	CULTIVATED_SUGARCANE_PIVOT_STANDING (26, "Sugarcane pivot: standing crop"),
	CULTIVATED_SUGARCANE_PIVOT_TEMPORARY (27, "Sugarcane pivot: temporary fallow"),
	CULTIVATED_SUGARCANE_NONPIVOT_COMMERCIAL_STANDING (28, "Sugarcane non-pivot: commercial farmer standing crop"),
	CULTIVATED_SUGARCANE_NONPIVOT_COMMERCIAL_TEMPORARY (29, "Sugarcane non-pivot: comercial farmer temporary fallow"),
	CULTIVATED_SUGARCANE_NONPIVOT_EMERGING_STANDING (30, "Sugarcane non-pivot: emerging farmer standing crop"),
	CULTIVATED_SUGARCANE_NONPIVOT_EMERGING_TEMPORARY (31, "Sugarcane non-pivot: emerging farmer temporary fallow"),
	FOREST_MATURE (32, "Forest plantations: mature trees"),
	FOREST_YOUNG (33, "Forest plantations: young trees"),
	FOREST_CLEARFELLED (34, "Forest plantations: temporary clearfelled stands"),
	MINE_BARE (35, "Mine bare"),
	MINE_SEMIBARE (36, "Mine semi-bare"),
	MINE_WATER_SEASONAL (37, "Mine water seasonal"),
	MINE_WATER_PERMANENT (38, "Mine water permanent"),
	MINE_BUILDINGS (39, "Mine buildings"),
	BARE_EROSION (40, "Erosion dongas and gullies"),
	BARE_NONVEGTATED (41, "Bare (non vegetated)"),
	BUILTUP_COMMERCIAL (42, "Commercial"),
	BUILTUP_INDUSTRIAL (43, "Industrial"),
	BUILTUP_INFORMAL_TREE (44, "Informal (tree dominated)"),
	BUILTUP_INFORMAL_BUSH (45, "Informal (bush dominated)"),
	BUILTUP_INFORMAL_GRASS (46, "Informal (grass dominated)"),
	BUILTUP_INFORMAL_BARE (47, "Informal (bare dominated)"),
	BUILTUP_RESIDENTIAL_TREE (48, "Residential (tree dominated)"),
	BUILTUP_RESIDENTIAL_BUSH (49, "Residential (bush dominated)"),
	BUILTUP_RESIDENTIAL_GRASS (50, "Residential (grass dominated)"),
	BUILTUP_RESIDENTIAL_BARE (51, "Residential (bare dominated)"),
	BUILTUP_SCHOOL (52, "Schools & sport grounds"),
	BUILTUP_SMALLHOLDING_TREE (53, "Smallholding (tree dominated)"),
	BUILTUP_SMALLHOLDING_BUSH (54, "Smallholding (bush dominated)"),
	BUILTUP_SMALLHOLDING_GRASS (55, "Smallholding (grass dominated)"),
	BUILTUP_SMALLHOLDING_BARE (56, "Smallholding (bare dominated)"),
	BUILTUP_SPORT_TREE (57, "Sports & golf (tree dominated)"),
	BUILTUP_SPORT_BUSH (58, "Sports & golf (bush dominated)"),
	BUILTUP_SPORT_GRASS (59, "Sports & golf (grass dominated)"),
	BUILTUP_SPORT_BARE (60, "Sports & golf (bare dominated)"),
	BUILTUP_TOWNSHIP_TREE (61, "Township (tree dominated)"),
	BUILTUP_TOWNSHIP_BUSH (62, "Township (bush dominated)"),
	BUILTUP_TOWNSHIP_GRASS (63, "Township (grass dominated)"),
	BUILTUP_TOWNSHIP_BARE (64, "Township (bare dominated)"),
	BUILTUP_VILLAGE_TREE (65, "Village (tree dominated)"),
	BUILTUP_VILLAGE_BUSH (66, "Village (bush dominated)"),
	BUILTUP_VILLAGE_GRASS (67, "Village (grass dominated)"),
	BUILTUP_VILLAGE_BARE (68, "Village (bare dominated)"),
	BUILTUP_OTHER_TREE (69, "Built-up (tree dominated)"),
	BUILTUP_OTHER_BUSH (70, "Built-up (bush dominated)"),
	BUILTUP_OTHER_GRASS (71, "Built-up (grass dominated)"),
	BUILTUP_OTHER_BARE (72, "Built-up (bare dominated)")
	;
	
	
	public static GtiLandCover parseLandcoverFromCode(short s){
		switch (s) {
		case 1: return GtiLandCover.PERMANENT_WATER;
		case 2: return GtiLandCover.SEASONAL_WATER;
		case 3:	return GtiLandCover.WETLAND;
		case 4:	return GtiLandCover.INDIGENOUS_FOREST;
		case 5: return GtiLandCover.DENSE_BUSH;
		case 6: return GtiLandCover.WOODLAND;
		case 7: return GtiLandCover.GRASSLAND;
		case 8: return GtiLandCover.LOW_SHRUBLAND_FYNBOS;
		case 9: return GtiLandCover.LOW_SHRUBLAND_OTHER;
		case 10: return GtiLandCover.CULTIVATED_COMMERCIAL_ANNUAL_HIGH;
		case 11: return GtiLandCover.CULTIVATED_COMMERCIAL_ANNUAL_MED;
		case 12: return GtiLandCover.CULTIVATED_COMMERCIAL_ANNUAL_LOW;
		case 13: return GtiLandCover.CULTIVATED_COMMERCIAL_PIVOT_HIGH;
		case 14: return GtiLandCover.CULTIVATED_COMMERCIAL_PIVOT_MED;
		case 15: return GtiLandCover.CULTIVATED_COMMERCIAL_PIVOT_LOW;
		case 16: return GtiLandCover.CULTIVATED_COMMERCIAL_ORCHARD_HIGH;
		case 17: return GtiLandCover.CULTIVATED_COMMERCIAL_ORCHARD_MED;
		case 18: return GtiLandCover.CULTIVATED_COMMERCIAL_ORCHARD_LOW;
		case 19: return GtiLandCover.CULTIVATED_COMMERCIAL_VINES_HIGH;
		case 20: return GtiLandCover.CULTIVATED_COMMERCIAL_VINES_MED;
		case 21: return GtiLandCover.CULTIVATED_COMMERCIAL_VINES_LOW;
		case 22: return GtiLandCover.CULTIVATED_COMMERCIAL_PINE;
		case 23: return GtiLandCover.CULTIVATED_SUBSISTENCE_HIGH;
		case 24: return GtiLandCover.CULTIVATED_SUBSISTENCE_MED;
		case 25: return GtiLandCover.CULTIVATED_SUBSISTENCE_LOW;
		case 26: return GtiLandCover.CULTIVATED_SUGARCANE_PIVOT_STANDING;
		case 27: return GtiLandCover.CULTIVATED_SUGARCANE_PIVOT_TEMPORARY;
		case 28: return GtiLandCover.CULTIVATED_SUGARCANE_NONPIVOT_COMMERCIAL_STANDING;
		case 29: return GtiLandCover.CULTIVATED_SUGARCANE_NONPIVOT_COMMERCIAL_TEMPORARY;
		case 30: return GtiLandCover.CULTIVATED_SUGARCANE_NONPIVOT_EMERGING_STANDING;
		case 31: return GtiLandCover.CULTIVATED_SUGARCANE_NONPIVOT_EMERGING_TEMPORARY;
		case 32: return GtiLandCover.FOREST_MATURE;
		case 33: return GtiLandCover.FOREST_YOUNG;
		case 34: return GtiLandCover.FOREST_CLEARFELLED;
		case 35: return GtiLandCover.MINE_BARE;
		case 36: return GtiLandCover.MINE_SEMIBARE;
		case 37: return GtiLandCover.MINE_WATER_SEASONAL;
		case 38: return GtiLandCover.MINE_WATER_PERMANENT;
		case 39: return GtiLandCover.MINE_BUILDINGS;
		case 40: return GtiLandCover.BARE_EROSION;
		case 41: return GtiLandCover.BARE_NONVEGTATED;
		case 42: return GtiLandCover.BUILTUP_COMMERCIAL;
		case 43: return GtiLandCover.BUILTUP_INDUSTRIAL;
		case 44: return GtiLandCover.BUILTUP_INFORMAL_TREE;
		case 45: return GtiLandCover.BUILTUP_INFORMAL_BUSH;
		case 46: return GtiLandCover.BUILTUP_INFORMAL_GRASS;
		case 47: return GtiLandCover.BUILTUP_INFORMAL_BARE;
		case 48: return GtiLandCover.BUILTUP_RESIDENTIAL_TREE;
		case 49: return GtiLandCover.BUILTUP_RESIDENTIAL_BUSH;
		case 50: return GtiLandCover.BUILTUP_RESIDENTIAL_GRASS;
		case 51: return GtiLandCover.BUILTUP_RESIDENTIAL_BARE;
		case 52: return GtiLandCover.BUILTUP_SCHOOL;
		case 53: return GtiLandCover.BUILTUP_SMALLHOLDING_TREE;
		case 54: return GtiLandCover.BUILTUP_SMALLHOLDING_BUSH;
		case 55: return GtiLandCover.BUILTUP_SMALLHOLDING_GRASS;
		case 56: return GtiLandCover.BUILTUP_SMALLHOLDING_BARE;
		case 57: return GtiLandCover.BUILTUP_SPORT_TREE;
		case 58: return GtiLandCover.BUILTUP_SPORT_BUSH;
		case 59: return GtiLandCover.BUILTUP_SPORT_GRASS;
		case 60: return GtiLandCover.BUILTUP_SPORT_BARE;
		case 61: return GtiLandCover.BUILTUP_TOWNSHIP_TREE;
		case 62: return GtiLandCover.BUILTUP_TOWNSHIP_BUSH;
		case 63: return GtiLandCover.BUILTUP_TOWNSHIP_GRASS;
		case 64: return GtiLandCover.BUILTUP_TOWNSHIP_BARE;
		case 65: return GtiLandCover.BUILTUP_VILLAGE_TREE;
		case 66: return GtiLandCover.BUILTUP_VILLAGE_BUSH;
		case 67: return GtiLandCover.BUILTUP_VILLAGE_GRASS;
		case 68: return GtiLandCover.BUILTUP_VILLAGE_BARE;
		case 69: return GtiLandCover.BUILTUP_OTHER_TREE; 
		case 70: return GtiLandCover.BUILTUP_OTHER_BUSH; 
		case 71: return GtiLandCover.BUILTUP_OTHER_GRASS; 
		case 72: return GtiLandCover.BUILTUP_OTHER_BARE; 
		default:
			return null;
		}
		
	}
	
	private final int code;
	private final String description;
	
	private GtiLandCover(int code, String descr) {
		this.code = code;
		this.description = descr;
	}
	
	public int getClassCode(){
		return this.code;
	}
	
	public String getDescription(){
		return this.description;
	}
	
	public boolean isBuiltUp(int code){
		return code >= 42 && code <= 72 ? true : false;
	}

	
	public double getAADT(double sqmArea, boolean am, boolean in){
		double d = 0.0;
		double tripRateMultplier = 0.0;
		double am_peak = 0.0;
		double am_in = 0.0;
		double am_out = 0.0;
		double pm_peak = 0.0;
		double pm_in = 0.0;
		double pm_out = 0.0;
		
		switch (this.code) {
		case 42: // Commercial
			am_peak = 5.25; am_in = 0.66; am_out = 0.34; 
			pm_peak = 7.48; pm_in = 0.43; pm_out = 0.63;
			tripRateMultplier = 49.22;
			break;
		case 43: // Industrial
			am_peak = 0.58; am_in = 0.70; am_out = 0.30; 
			pm_peak = 0.63; pm_in = 0.32; pm_out = 0.68;
			tripRateMultplier = 3.46;
			break;
		case 44:
		case 45:
		case 46:
		case 47: // Informal
			am_peak = 1.02; am_in = 0.37; am_out = 0.63; 
			pm_peak = 1.08; pm_in = 0.59; pm_out = 0.41;
			tripRateMultplier = 6.00;
			break;
		case 48:
		case 49:
		case 50:
		case 51: // Residential (formal)
			am_peak = 0.51; am_in = 0.37; am_out = 0.63; 
			pm_peak = 0.54; pm_in = 0.59; pm_out = 0.41;
			tripRateMultplier = 2.99;
			break;
		case 52: // Schools & sport grounds.
			am_peak = 0.65; am_in = 0.58; am_out = 0.43; 
			pm_peak = 0.26; pm_in = 0.45; pm_out = 0.55;
			tripRateMultplier = 3.86;
			break;
		case 53:
		case 54:
		case 55:
		case 56: // Smallholding.
			break;
		case 57:
		case 58:
		case 59:
		case 60: // Sports & Golf.
			am_peak = 11.59; am_in = 0.73; am_out = 0.28; 
			pm_peak = 18.63; pm_in = 0.54; pm_out = 0.46;
			tripRateMultplier = 10.00;
		case 61:
		case 62:
		case 63:
		case 64: // Township
			am_peak = 1.02; am_in = 0.37; am_out = 0.63; 
			pm_peak = 1.08; pm_in = 0.59; pm_out = 0.41;
			tripRateMultplier = 6.00;
			break;
		case 65:
		case 66:
		case 67:
		case 68: // Village
			am_peak = 0.51; am_in = 0.37; am_out = 0.63; 
			pm_peak = 0.54; pm_in = 0.59; pm_out = 0.41;
			tripRateMultplier = 2.99;
			break;
		case 69:
		case 70:
		case 71:
		case 72:// Built-up (general)
			tripRateMultplier = 0.50;
			break;
		default:
			break;
		}
		
		double factor = 1.0;
		
		if(am){
			/* Morning */
			factor = am_peak;
			if(in){
				/* In */
				factor *= am_in;
			} else{
				/* Out */
				factor *= am_out;
			}
		} else{
			/* Afternoon */
			factor = pm_peak;
			if(in){
				/* In */
				factor *= pm_in;
			} else{
				/* Out */
				factor *= pm_out;
			}
		}
		
		/* All peak slots are 2.5 hours long, and the peak rate is in trips per
		 * hour. */
				
		d = (sqmArea/100.0)*factor*2.5;
		
		return d;
	}
	
}