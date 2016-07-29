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

import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;
import playground.johannes.gsv.sim.GsvConfigGroup;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixIO;

import java.io.IOException;

/**
 * @author johannes
 * 
 */
public class ODAdjustorListener implements IterationStartsListener {

	// private static final Logger logger =
	// Logger.getLogger(ODAdjustorListener.class);

	private final int interval;

	private final ODAdjustor adjustor;

	public ODAdjustorListener(MatsimServices controler) {
		TripRouter router = controler.getTripRouterProvider().get();
		ActivityFacilities facilities = controler.getScenario().getActivityFacilities();

		NumericMatrix refMatrix = loadRefMatrix(controler.getConfig().getParam(GsvConfigGroup.GSV_CONFIG_MODULE_NAME, "odMatrixFile"));
		ZoneCollection zones = loadZones(controler.getConfig().getParam(GsvConfigGroup.GSV_CONFIG_MODULE_NAME, "zonesFile"));

		double distThreshold = Double.parseDouble(controler.getConfig().getParam(GsvConfigGroup.GSV_CONFIG_MODULE_NAME, "odDistThreshold"));
		double countThreshold = Double.parseDouble(controler.getConfig().getParam(GsvConfigGroup.GSV_CONFIG_MODULE_NAME, "odCountThreshold"));
		ODUtils.cleanDistances(refMatrix, zones, distThreshold);
		ODUtils.cleanVolumes(refMatrix, zones, countThreshold);

		adjustor = new ODAdjustor(facilities, router, zones, refMatrix, controler.getControlerIO().getOutputPath());

		interval = Integer.parseInt(controler.getConfig().getParam(GsvConfigGroup.GSV_CONFIG_MODULE_NAME, "odAdjustorInterval"));
	}

	private NumericMatrix loadRefMatrix(String filename) {
//		NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
//		reader.setValidating(false);
//		reader.parse(filename);
//		return reader.getMatrix();
		return NumericMatrixIO.read(filename);
	}

	private ZoneCollection loadZones(String filename) {
//		ZoneCollection zones = new ZoneCollection();
//		String data;
//		try {
//			data = new String(Files.readAllBytes(Paths.get(filename)));
//			zones.addAll(ZoneGeoJsonIO.parseFeatureCollection(data));
//			zones.setPrimaryKey(ODAdjustor.ZONE_ID_KEY);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return zones;
		try {
			return ZoneGeoJsonIO.readFromGeoJSON(filename, ODAdjustor.ZONE_ID_KEY, null);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (interval > 0) {
			if (event.getIteration() % interval == 0) {
				adjustor.run(event.getServices().getScenario().getPopulation());
			}
		}
	}

}
