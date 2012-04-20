/* *********************************************************************** *
 * project: org.matsim.*
 * SfAirScheduleBuilder
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package air.scenario;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * 
 * @deprecated use DgCreateFlightScenario to create a flight scenario
 */
@Deprecated
public class SfOsm2Matsim {

	public static void main(String[] args) {
		SfOsmAerowayParser osmReader = new SfOsmAerowayParser(TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
						TransformationFactory.WGS84));
		String input = "Z:\\world_air.osm";				// OSM Input File
		osmReader.parse(input);
		osmReader.writeToFile("Z:\\world_air_16apr2012.xml");
	}

}
