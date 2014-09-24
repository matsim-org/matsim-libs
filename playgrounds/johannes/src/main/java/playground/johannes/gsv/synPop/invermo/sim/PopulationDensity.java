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

package playground.johannes.gsv.synPop.invermo.sim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.sim2.Hamiltonian;
import playground.johannes.gsv.synPop.sim2.SamplerListener;
import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.sna.util.ProgressLogger;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;

/**
 * @author johannes
 * 
 */
public class PopulationDensity implements Hamiltonian, SamplerListener {

	private static final Logger logger = Logger.getLogger(PopulationDensity.class);

	public static final Object ZONE_KEY = new Object();

	private ZoneLayer<ZoneData> zones;

	private Collection<ProxyPerson> persons;

	private boolean isInitialized = false;

	public PopulationDensity(Collection<ProxyPerson> persons, ZoneLayer<Double> inhabitants, int N, Random random) {
		this.persons = persons;

		logger.info("Initializing population density hamiltonian...");
		double maxFrac = 0;
		Set<Zone<ZoneData>> newZones = new HashSet<Zone<ZoneData>>(inhabitants.getZones().size());
		for (Zone<Double> zone : inhabitants.getZones()) {
			Zone<ZoneData> newZone = new Zone<ZoneData>(zone.getGeometry());
			newZone.setAttribute(new ZoneData());
			newZone.getAttribute().targetFraction = zone.getAttribute();

			newZones.add(newZone);

			maxFrac = Math.max(maxFrac, newZone.getAttribute().targetFraction);
		}

		zones = new ZoneLayer<PopulationDensity.ZoneData>(newZones);

		List<Zone<ZoneData>> zoneList = new ArrayList<Zone<ZoneData>>(zones.getZones());
		int total = 0;
		ProgressLogger.init(N, 2, 10);
		while (total < N) {
			Zone<ZoneData> zone = zoneList.get(random.nextInt(zoneList.size()));
			double p = zone.getAttribute().targetFraction / maxFrac;
			if (p > random.nextDouble()) {
				zone.getAttribute().target++;
				total++;
				ProgressLogger.step();
			}
		}
		ProgressLogger.termiante();

		// writeZoneData("/home/johannes/gsv/synpop/output/targetPopDen.shp");
	}

	@Override
	public double evaluate(ProxyPerson person) {
		initializeZones();
		
//		ProxyPlan plan = person.getPlans().get(0);

		double diff = 0;

		ActivityFacility home = (ActivityFacility) person.getUserData(SwitchHomeLocations.HOME_FACIL_KEY);
		Zone<ZoneData> zone = zones.getZone(MatsimCoordUtils.coordToPoint(home.getCoord()));
		
		diff += Math.abs(zone.getAttribute().current - zone.getAttribute().target)/(double)zone.getAttribute().target;
//		diff = diff/zone.getAttribute().targetFraction;
		
//		for (ProxyObject act : plan.getActivities()) {
//			Boolean startFacil = ((Boolean)act.getUserData(MutateStartLocation.START_FACILITY_KEY)); 
//			if(startFacil != null && startFacil == true) {
//				Zone<ZoneData> zone = (Zone<ZoneData>) act.getUserData(ZONE_KEY);
//////				if (zone == null) {
////					ActivityFacility facility = (ActivityFacility) act.getUserData(MutateStartLocation.FACILITY_KEY);
////					Coord coord = facility.getCoord();
////					Zone<ZoneData> zone = zones.getZone(MatsimCoordUtils.coordToPoint(coord));
//////				}
//
//				diff += Math.abs(zone.getAttribute().current - zone.getAttribute().target)/(double)zone.getAttribute().target;
//				diff = diff/zone.getAttribute().targetFraction;
//				break;
//			}
//		}

		return diff;
	}

	@Override
	public void afterModify(ProxyPerson person) {
		updateZoneData(person);
	}

	@Override
	public void afterStep(Collection<ProxyPerson> population, ProxyPerson person, boolean accpeted) {
		if (!accpeted) {
			updateZoneData(person);
		}

	}

	private void updateZoneData(ProxyPerson person) {
		Zone<ZoneData> prevZone = (Zone<ZoneData>) person.getUserData(ZONE_KEY);

		ActivityFacility facility = (ActivityFacility) person.getUserData(SwitchHomeLocations.HOME_FACIL_KEY);
		Coord coord = facility.getCoord();
		Zone<ZoneData> newZone = zones.getZone(MatsimCoordUtils.coordToPoint(coord));

		prevZone.getAttribute().current--;
		newZone.getAttribute().current++;

		person.setUserData(ZONE_KEY, newZone);		
		
//		ProxyPlan plan = person.getPlans().get(0);

//		for (ProxyObject act : plan.getActivities()) {
//			Boolean start = (Boolean) act.getUserData(MutateStartLocation.START_FACILITY_KEY); 
//			if (start != null && start == true) {
//				Zone<ZoneData> prevZone = (Zone<ZoneData>) act.getUserData(ZONE_KEY);
//
//				ActivityFacility facility = (ActivityFacility) act.getUserData(MutateStartLocation.FACILITY_KEY);
//				Coord coord = facility.getCoord();
//				Zone<ZoneData> newZone = zones.getZone(MatsimCoordUtils.coordToPoint(coord));
//
//				prevZone.getAttribute().current--;
//				newZone.getAttribute().current++;
//
//				act.setUserData(ZONE_KEY, newZone);
//
//				break;
//			}
//		}
	}

	public void initializeZones() {
		if (!isInitialized) {
			for (ProxyPerson person : persons) {
				ActivityFacility facility = (ActivityFacility) person.getUserData(SwitchHomeLocations.HOME_FACIL_KEY);
				Coord coord = facility.getCoord();
				Zone<ZoneData> zone = zones.getZone(MatsimCoordUtils.coordToPoint(coord));

				zone.getAttribute().current++;

				person.setUserData(ZONE_KEY, zone);
				
				
//				ProxyPlan plan = person.getPlans().get(0);
//
//				for (ProxyObject act : plan.getActivities()) {
//					Boolean startFacil = ((Boolean)act.getUserData(MutateStartLocation.START_FACILITY_KEY)); 
//					if(startFacil != null && startFacil == true) {
//						ActivityFacility facility = (ActivityFacility) act.getUserData(MutateStartLocation.FACILITY_KEY);
//						Coord coord = facility.getCoord();
//						Zone<ZoneData> zone = zones.getZone(MatsimCoordUtils.coordToPoint(coord));
//
//						zone.getAttribute().current++;
//
//						act.setUserData(ZONE_KEY, zone);
//
//						break;
//					}
//				}
			}
			
			isInitialized = true;
		}
	}
	
	public void writeZoneData(String file) {
		try {
			Set<Zone<Double>> newZones = new HashSet<Zone<Double>>();
			for(Zone<ZoneData> zone : zones.getZones()) {
				Zone<Double> newZone = new Zone<Double>(zone.getGeometry());
//				newZone.setAttribute((double) zone.getAttribute().current/zone.getGeometry().getArea() * 1000000);
//				newZone.setAttribute((double) (zone.getAttribute().current/zone.getGeometry().getArea() * 1000000));
				newZone.setAttribute((double) (zone.getAttribute().target - zone.getAttribute().current)/(double)zone.getAttribute().target);
				newZones.add(newZone);
			}
			
			ZoneLayer<Double> newLayer = new ZoneLayer<Double>(newZones);
			newLayer.overwriteCRS(CRSUtils.getCRS(31467));
			ZoneLayerSHP.write(newLayer, file);
			
//			ZoneLayerKMLWriter writer =	new ZoneLayerKMLWriter();
//			writer.writeWithColor(newLayer, file);
		} catch (IOException e) {
//			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static class ZoneData {

		private int current;

		private int target;

		private double targetFraction;

	}
}
