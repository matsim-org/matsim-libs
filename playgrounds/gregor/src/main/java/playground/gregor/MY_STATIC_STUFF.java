/* *********************************************************************** *
 * project: org.matsim.*
 * MY_STATIC_STUFF.java
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
package playground.gregor;



public class MY_STATIC_STUFF {
	public final static double FLOODED_DIST_THRESHOLD = 10.;
	public final static double BUFFER_SIZE = 500.;

	public final static String OUTPUTS = "../../../../outputs/";
	
	public static final String SVN_ROOT ="/home/laemmel/arbeit/svn";
	public static final String SHARED_SVN ="shared-svn";
	
//	public static final String SVN_ROOT ="/net/ils/";
//	public static final String SHARED_SVN ="data/";
	
	public final static String RUNS_SVN = "runs-svn";
	public final static String PADANG_SVN = SVN_ROOT + "/" + SHARED_SVN + "/studies/countries/id/padang";
//	public final static String PADANG_SVN = SVN_ROOT + "/" + SHARED_SVN + "/countries/id/padang";
	public static final String PADANG_SVN_INUNDATION = PADANG_SVN + "/inundation";	
	
//	public static final String SWW_ROOT = PADANG_SVN_INUNDATION + "/20090924_081241_run_final_0.8_SZ_hilman_2b_high_tide_subsidence_goseberg";
	public static final String SWW_ROOT = PADANG_SVN_INUNDATION + "/20100201_sz_pc_2b_tide_subsidence";
	public static final int SWW_COUNT = 8; 
//	public  static final String SWW_PREFIX = "SZ_hilman_2b_high_tide_subsidence_P";
	public  static final String SWW_PREFIX = "SZ_hilman_2b_subsidence_more_points_P";
	public  static final String SWW_SUFFIX = "_8.sww";
	
	public static final String PADANG_ADDITIONAL_GIS = PADANG_SVN + "/data/GIS";
	public static final String PADANG_REGION_SHAPE = PADANG_ADDITIONAL_GIS + "/keluraha_region.shp";
	
	public static final String CVS_GIS = "/home/laemmel/workspace/vsp-cvs/studies/padang/gis/";
	
	
	public static final String PADANG_SVN_DATA = PADANG_SVN + "/data";


}
