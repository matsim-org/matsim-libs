/* *********************************************************************** *
 * project: org.matsim.*
 * DetailedEvalFiles
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

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public interface DgDetailedEvalFiles {
	
	public static final String BAVARIA_SHAPE_FILE = DgPaths.REPOS + "shared-svn/projects/detailedEval/Net/shapeFromVISUM/Verkehrszellen_Umrisse_area.SHP";
	
	public static final String PROGNOSE_2025_2004_NETWORK = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/demand/network_cleaned_wgs84.xml.gz";

	public static final String PROGNOSE_2025_2004_PV_NETWORK = DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/demand/network_pv_cleaned_wgs84.xml.gz";
	
	public static final String GV_POPULATION_INPUT_FILE = DgPaths.REPOS  + "runs-svn/run1060/1060.output_plans.xml.gz";

	public static final String GV_EVENTS_FILE = DgPaths.REPOS  + "runs-svn/run1060/ITERS/it.0/1060.0.events.xml.gz";
	
	public static final String GV_POPULATION_OUTPUT_FILE = DgPaths.REPOS + "shared-svn/projects/detailedEval/pop/gueterVerkehr/population_gv_bavaria_10pct_wgs84.xml.gz";

	public static final String PV_POPULATION_INPUT_FILE = DgPaths.REPOS  + "runs-svn/run1063/1063.output_plans.xml.gz";

	public static final String PV_EVENTS_FILE = DgPaths.REPOS  + "runs-svn/run1063/ITERS/it.0/1063.0.events.xml.gz";
	
	public static final String PV_POPULATION_OUTPUT_FILE = DgPaths.REPOS + "shared-svn/projects/detailedEval/pop/personenVerkehr/population_pv_bavaria_1pct_wgs84.xml.gz";

	
}
