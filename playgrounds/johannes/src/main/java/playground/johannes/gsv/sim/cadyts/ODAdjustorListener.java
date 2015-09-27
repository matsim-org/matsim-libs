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

package playground.johannes.gsv.sim.cadyts;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;
import playground.johannes.gsv.sim.Simulator;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;
import playground.johannes.gsv.zones.io.Zone2GeoJSON;
import playground.johannes.synpop.gis.ZoneCollection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author johannes
 * 
 */
public class ODAdjustorListener implements IterationStartsListener {

	// private static final Logger logger =
	// Logger.getLogger(ODAdjustorListener.class);

	private final int interval;

	private final ODAdjustor adjustor;

	public ODAdjustorListener(Controler controler) {
		TripRouter router = controler.getTripRouterProvider().get();
		ActivityFacilities facilities = controler.getScenario().getActivityFacilities();

		KeyMatrix refMatrix = loadRefMatrix(controler.getConfig().getParam(Simulator.GSV_CONFIG_MODULE_NAME, "odMatrixFile"));
		ZoneCollection zones = loadZones(controler.getConfig().getParam(Simulator.GSV_CONFIG_MODULE_NAME, "zonesFile"));

		double distThreshold = Double.parseDouble(controler.getConfig().getParam(Simulator.GSV_CONFIG_MODULE_NAME, "odDistThreshold"));
		double countThreshold = Double.parseDouble(controler.getConfig().getParam(Simulator.GSV_CONFIG_MODULE_NAME, "odCountThreshold"));
		ODUtils.cleanDistances(refMatrix, zones, distThreshold);
		ODUtils.cleanVolumes(refMatrix, zones, countThreshold);

		adjustor = new ODAdjustor(facilities, router, zones, refMatrix, controler.getControlerIO().getOutputPath());

		interval = Integer.parseInt(controler.getConfig().getParam(Simulator.GSV_CONFIG_MODULE_NAME, "odAdjustorInterval"));
	}

	private KeyMatrix loadRefMatrix(String filename) {
		KeyMatrixXMLReader reader = new KeyMatrixXMLReader();
		reader.setValidating(false);
		reader.parse(filename);
		return reader.getMatrix();
	}

	private ZoneCollection loadZones(String filename) {
		ZoneCollection zones = new ZoneCollection();
		String data;
		try {
			data = new String(Files.readAllBytes(Paths.get(filename)));
			zones.addAll(Zone2GeoJSON.parseFeatureCollection(data));
			zones.setPrimaryKey(ODAdjustor.ZONE_ID_KEY);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return zones;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (interval > 0) {
			if (event.getIteration() % interval == 0) {
				adjustor.run(event.getControler().getScenario().getPopulation());
			}
		}
	}

}
