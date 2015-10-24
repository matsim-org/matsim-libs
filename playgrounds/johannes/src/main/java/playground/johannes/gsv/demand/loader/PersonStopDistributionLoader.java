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

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.common.gis.CRSUtils;
import playground.johannes.gsv.demand.AbstractTaskWrapper;
import playground.johannes.gsv.demand.LoaderUtils;
import playground.johannes.gsv.demand.tasks.PersonStopDistribution;
import playground.johannes.sna.gis.ZoneLayer;

import java.io.IOException;
import java.util.Random;

/**
 * @author johannes
 *
 */
public class PersonStopDistributionLoader extends AbstractTaskWrapper {

	public PersonStopDistributionLoader(String inhabitants, String key, Scenario scenario, Random random) throws IOException {
		ZoneLayer<Double> zoneLayer = LoaderUtils.loadSingleColumnRelative(inhabitants, key);
		zoneLayer.overwriteCRS(CRSUtils.getCRS(4326));
		
		delegate = new PersonStopDistribution(scenario.getTransitSchedule(), zoneLayer, random);
	}
}
