/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.johannes.gsv.demand.loader;

import gnu.trove.TObjectDoubleHashMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import playground.johannes.gsv.demand.AbstractTaskWrapper;
import playground.johannes.gsv.demand.LoaderUtils;
import playground.johannes.gsv.demand.tasks.PlanPrimaryActivity;

import java.io.IOException;
import java.util.Random;

/**
 * @author johannes
 *
 */
public class PlanPrimaryActivityLoader extends AbstractTaskWrapper {

	public PlanPrimaryActivityLoader(String file, String key, Scenario scenario, Random random) throws IOException {
		TObjectDoubleHashMap<String> values = LoaderUtils.loadSingleColumn(file, key);
		ZoneLayer<Double> zoneLayer = LoaderUtils.mapValuesToZones(values);
		zoneLayer.overwriteCRS(CRSUtils.getCRS(4326));
		
		delegate = new PlanPrimaryActivity(zoneLayer, scenario.getTransitSchedule());
	}
}
