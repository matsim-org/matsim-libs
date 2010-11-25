/* *********************************************************************** *
 * project: org.matsim.*
 * DgBavariaDemandFilter
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
package playground.dgrether.prognose2025;

import java.io.IOException;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class DgBavariaDemandFilter {

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
		String shape = "shared-svn/projects/detailedEval/Net/shapeFromVISUM/Verkehrszellen_Umrisse_area.SHP";
		String gvOut1pct = "shared-svn/projects/detailedEval/pop/gueterVerkehr/population_gv_bavaria_1pct_wgs84.xml.gz";
		String gvOut10pct = "shared-svn/projects/detailedEval/pop/gueterVerkehr/population_gv_bavaria_10pct_wgs84.xml.gz";
		String pvOut1pct = "shared-svn/projects/detailedEval/pop/pendlerVerkehr/population_pv_bavaria_1pct_wgs84.xml.gz";
		String pvOut10pct = "shared-svn/projects/detailedEval/pop/pendlerVerkehr/population_pv_bavaria_10pct_wgs84.xml.gz";

		
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
		shape = base + shape;
		gvOut1pct = base + gvOut1pct;
		gvOut10pct = base + gvOut10pct;
		pvOut1pct = base + pvOut1pct;
		pvOut10pct = base + pvOut10pct;
		
		CoordinateTransformation wgs84ToDhdnGk4 = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4); 
		
		DgPrognose2025GvDemandFilter filter = new DgPrognose2025GvDemandFilter();
		filter.setNetwork2ShapefileCoordinateTransformation(wgs84ToDhdnGk4);
		filter.filterAndWriteDemand(gvNet, 
				gvIn1pct, shape, gvOut1pct);
		
		DgPrognose2025PvDemandFilter pvfilter = new DgPrognose2025PvDemandFilter();
		pvfilter.setNetwork2ShapefileCoordinateTransformation(wgs84ToDhdnGk4);
		pvfilter.filterAndWriteDemand(pvNet, pvIn1pct, shape, pvOut1pct);

		filter = new DgPrognose2025GvDemandFilter();
		filter.setNetwork2ShapefileCoordinateTransformation(wgs84ToDhdnGk4);
		filter.filterAndWriteDemand(gvNet, 
				gvIn10pct, shape, gvOut10pct);
		
		pvfilter = new DgPrognose2025PvDemandFilter();
		pvfilter.setNetwork2ShapefileCoordinateTransformation(wgs84ToDhdnGk4);
		pvfilter.filterAndWriteDemand(pvNet, pvIn10pct, shape, pvOut10pct);

		
		
	}

}
