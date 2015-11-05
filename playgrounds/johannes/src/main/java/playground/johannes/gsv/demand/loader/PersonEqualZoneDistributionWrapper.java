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

import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import playground.johannes.gsv.demand.AbstractTaskWrapper;
import playground.johannes.gsv.demand.LoaderUtils;
import playground.johannes.gsv.demand.tasks.PersonEqualZoneDistribution;

import java.io.IOException;
import java.util.Random;


/**
 * @author johannes
 *
 */
public class PersonEqualZoneDistributionWrapper extends AbstractTaskWrapper {

	public PersonEqualZoneDistributionWrapper(String file, String key, Random random) throws IOException {
		ZoneLayer<Double> zoneLayer = LoaderUtils.loadSingleColumnRelative(file, key);
		this.delegate = new PersonEqualZoneDistribution(zoneLayer, random);
	}

}
