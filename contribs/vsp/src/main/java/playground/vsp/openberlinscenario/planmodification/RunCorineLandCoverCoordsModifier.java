/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.vsp.openberlinscenario.planmodification;

import java.util.HashMap;
import java.util.Map;

import playground.vsp.corineLandcover.CORINELandCoverCoordsModifier;

public class RunCorineLandCoverCoordsModifier {
	
	public static void main(String[] args) {
	    String inputPlansFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_5/population/500/plans_500-10-1_10pct.xml.gz";
	    String outputPlansFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_5/population/500/plans_500-10-1_10pct_clc.xml.gz";
	    
		String corineLandCoverFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/corine_landcover/corine_lancover_berlin-brandenburg_GK4.shp";

	    String zonalShapeFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/2016/gemeinden_Planungsraum.shp";
	    String zoneIdTag = "NR";
	
	    boolean simplifyGeom = false;
	    boolean combiningGeoms = false;
	    boolean sameHomeActivity = true;
	    String homeActivityPrefix = "home";
	
	    Map<String, String> shapeFileToFeatureKey = new HashMap<>();
	    shapeFileToFeatureKey.put(zonalShapeFile, zoneIdTag);
	
	    CORINELandCoverCoordsModifier plansFilterForCORINELandCover = new CORINELandCoverCoordsModifier(inputPlansFile, shapeFileToFeatureKey,
	            corineLandCoverFile, simplifyGeom, combiningGeoms, sameHomeActivity, homeActivityPrefix);
	    plansFilterForCORINELandCover.process();
	    plansFilterForCORINELandCover.writePlans(outputPlansFile);
    }
}