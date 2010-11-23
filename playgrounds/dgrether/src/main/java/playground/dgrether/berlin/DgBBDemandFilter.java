/* *********************************************************************** *
 * project: org.matsim.*
 * DgBBDemandFilter
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
package playground.dgrether.berlin;

import java.io.IOException;

import playground.dgrether.DgPaths;
import playground.dgrether.prognose2025.DgPrognose2025GvDemandFilter;
import playground.dgrether.prognose2025.DgPrognose2025PvDemandFilter;


/**
 * Script to create background traffic
 * @author dgrether
 *
 */
public class DgBBDemandFilter {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String gvNet = "shared-svn/studies/countries/de/prognose_2025/demand/network_cleaned_wgs84.xml.gz";
		String pvNet = "shared-svn/studies/countries/de/prognose_2025/demand/network_pv_cleaned_wgs84.xml.gz";
		String gvIn1pct = "runs-svn/run1061/1061.output_plans.xml.gz";
		String pvIn1pct = "runs-svn/run1063/1063.output_plans.xml.gz";
		String gvIn10pct = "runs-svn/run1060/1060.output_plans.xml.gz";
		String pvIn10pct = "runs-svn/run1062/1062.output_plans.xml.gz";
		String bbshape = "shared-svn/studies/countries/de/osm_berlinbrandenburg/urdaten/brandenburg.shp";
		String gvOut1pct = "shared-svn/studies/countries/de/osm_berlinbrandenburg/bb_gv_1pct.xml.gz";
		String gvOut10pct = "shared-svn/studies/countries/de/osm_berlinbrandenburg/bb_gv_10pct.xml.gz";
		String pvOut1pct = "shared-svn/studies/countries/de/osm_berlinbrandenburg/bb_pendler_1pct.xml.gz";
		String pvOut10pct = "shared-svn/studies/countries/de/osm_berlinbrandenburg/bb_pendler_10pct.xml.gz";
		
		String base = "";
		if (args == null || args.length == 0){
			base = DgPaths.REPOS;
		}
		else if (args.length == 1){
			base = args[0];
		}
		gvNet = base + gvNet;
		pvNet = base + pvNet;
		gvIn1pct = base + gvIn1pct;
		gvIn10pct = base + gvIn10pct;
		pvIn1pct = base + pvIn1pct;
		pvIn10pct = base + pvIn10pct;
		bbshape = base + bbshape;
		gvOut1pct = base + gvOut1pct;
		gvOut10pct = base + gvOut10pct;
		pvOut1pct = base + pvOut1pct;
		pvOut10pct = base + pvOut10pct;
		
		new DgPrognose2025GvDemandFilter().filterAndWriteDemand(gvNet, 
				gvIn1pct, bbshape, gvOut1pct);
		
		new DgPrognose2025PvDemandFilter().filterAndWriteDemand(pvNet, pvIn1pct, bbshape, pvOut1pct);

		new DgPrognose2025GvDemandFilter().filterAndWriteDemand(gvNet, 
				gvIn10pct, bbshape, gvOut10pct);
		
		new DgPrognose2025PvDemandFilter().filterAndWriteDemand(pvNet, pvIn10pct, bbshape, pvOut10pct);
		
	}

}
