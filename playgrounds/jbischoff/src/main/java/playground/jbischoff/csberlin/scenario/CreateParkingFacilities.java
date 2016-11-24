/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.jbischoff.csberlin.scenario;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesWriter;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CreateParkingFacilities {

	public static void main(String[] args) {
		
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new MatsimNetworkReader(scenario.getNetwork()).readFile("../../../shared-svn/projects/bmw_carsharing/example/grid_network.xml");
		new MatsimNetworkReader(scenario.getNetwork()).readFile("../../../shared-svn/projects/bmw_carsharing/data/scenario/network.xml.gz");
		final ActivityFacilitiesFactory fac = scenario.getActivityFacilities().getFactory();
		TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] {"\t"});
        config.setFileName("../../../shared-svn/projects/bmw_carsharing/data/parkplaetze-poster.txt");
        config.setCommentTags(new String[] { "#" });
        new TabularFileParser().parse(config, new TabularFileHandler() {

			@Override
			public void startRow(String[] row) {
				Id<Link> linkId = Id.createLinkId(row[0]);
				Coord coord = scenario.getNetwork().getLinks().get(linkId).getCoord();
				int capacity = Integer.parseInt(row[1]);
				ActivityFacility parking = fac.createActivityFacility(Id.create(linkId.toString()+"_curbside",ActivityFacility.class ), coord, linkId);
				ActivityOption option = fac.createActivityOption(ParkingUtils.PARKACTIVITYTYPE);
				option.setCapacity(capacity);
				parking.addActivityOption(option);
				scenario.getActivityFacilities().addActivityFacility(parking);
			}
		
	});
        new FacilitiesWriter(scenario.getActivityFacilities()).write("../../../shared-svn/projects/bmw_carsharing/data/scenario/parkingFacilities-poster.xml");
        
	}
	
}
