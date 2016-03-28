/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.matrices.postprocess;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.MatsimFacilitiesReader;
import playground.johannes.gsv.sim.cadyts.ODAdjustor;
import playground.johannes.gsv.sim.cadyts.ODUtils;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLReader;

import javax.inject.Provider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author johannes
 *
 */
public class ODAdjustorRun {
	
	private static final Logger logger = Logger.getLogger(ODAdjustorRun.class);

	public static void main(String args[]) throws IOException {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario.getNetwork());
//		netReader.readFile("/home/johannes/gsv/ger/data/network.xml.gz");
		netReader.readFile(args[0]);

		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
//		facReader.readFile("/home/johannes/gsv/ger/data/facilities.xml.gz");
		facReader.readFile(args[1]);

		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
//		popReader.readFile("/home/johannes/gsv/ger/data/plans.xml.gz");
		popReader.readFile(args[2]);

		logger.info("Connecting facilities to links...");
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			Coord coord = facility.getCoord();
			Link link = NetworkUtils.getNearestLink(network, coord);
			((ActivityFacilityImpl) facility).setLinkId(link.getId());
		}

		TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults();
		FreespeedTravelTimeAndDisutility tt = new FreespeedTravelTimeAndDisutility(-1, 0, 0);
		builder.setTravelTime(tt);
		builder.setTravelDisutility(tt);
		Provider<TripRouter> factory = builder.build(scenario);
		TripRouter router = factory.get();

		NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
		reader.setValidating(false);
//		reader.parse("/home/johannes/gsv/matrices/refmatrices/tomtom.de.xml");
		reader.parse(args[3]);
		NumericMatrix refMatrix = reader.getMatrix();
//		MatrixOperations.multiply(refMatrix, 1 / 16.0);
//		MatrixOperations.multiply(refMatrix, 1 / (4 * 11.8));

		ZoneCollection zones = new ZoneCollection(null);
//		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/nuts/de.nuts3.gk3.geojson")));
		String data = new String(Files.readAllBytes(Paths.get(args[4])));
		zones.addAll(ZoneGeoJsonIO.parseFeatureCollection(data));
		zones.setPrimaryKey("gsvId");
		data = null;

		ODUtils.cleanDistances(refMatrix, zones, 100000);
		ODUtils.cleanVolumes(refMatrix, zones, 1500);
		
		ODAdjustor calibrator = new ODAdjustor(scenario.getActivityFacilities(), router, zones, refMatrix, args[5]);
		calibrator.run(scenario.getPopulation());
		
		logger.info("Writing population...");
		PopulationWriter writer = new PopulationWriter(scenario.getPopulation());
		writer.write(String.format("%s/plans.xml.gz", args[5]));
		logger.info("Done.");
	}

	
}
