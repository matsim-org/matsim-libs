/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingInfrastructure.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.burgdorf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;

/**
 * Controls the capacities of the parkings in Burgdorf.
 * 
 * @author cdobler
 */
public class ParkingInfrastructure {

	/*
	 * Links where agents have to decide which parking they will use.
	 */
	public static String[] parkingDecisionLinks = new String[]{"L01", "L30"};
	
	/*
	 * Sub-routes from the parking decision links to the parkings.
	 */
	public static Map<Id<Link>, List<Id<Link>>> toParkingSubRoutes = new HashMap<>();
	public static Map<Id<Link>, List<Id<Link>>> fromParkingSubRoutes = new HashMap<>();
	
	/*
	 * Parkings, that can be reached from a given decision link. 
	 */
	public static Map<Id<Link>, List<Id<Link>>> availableParkings = new HashMap<>();
	
	/*
	 * Stores for each agent which parking is used. Data is inserted by
	 * the ParkingIdentifier and read by the ParkingReplanners.
	 */
	public static Map<Id, Id> selectedParkings = new ConcurrentHashMap<Id, Id>();
	
	public static Id<Link> selectParking(Id<Link> currentLinkId) {
		
		List<Id<Link>> list = availableParkings.get(currentLinkId);
		
		return list.get(MatsimRandom.getRandom().nextInt(list.size()));
	}
	
	static {
		
		List<Id<Link>> parkingList;
		
		/*
		 * Create data structure for available parkings.
		 */
		parkingList = new ArrayList<Id<Link>>();
		parkingList.add(Id.create("P01", Link.class));
		parkingList.add(Id.create("P02", Link.class));
		parkingList.add(Id.create("P03", Link.class));
		parkingList.add(Id.create("P04", Link.class));
		parkingList.add(Id.create("P05", Link.class));
		parkingList.add(Id.create("P07a", Link.class));	// can be reached from both
		availableParkings.put(Id.create("L30", Link.class), parkingList);

		parkingList = new ArrayList<Id<Link>>();
		parkingList.add(Id.create("P06", Link.class));
		parkingList.add(Id.create("P07", Link.class));
		parkingList.add(Id.create("P08", Link.class));
		parkingList.add(Id.create("P09", Link.class));
		availableParkings.put(Id.create("L01", Link.class), parkingList);
		
		/*
		 * Create data structure containing the sub-routes from the decision links
		 * to the parkings.
		 */
		List<Id<Link>> list;
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking01) list.add(Id.create(id, Link.class));
		toParkingSubRoutes.put(Id.create("P01", Link.class), list);

		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking02) list.add(Id.create(id, Link.class));
		toParkingSubRoutes.put(Id.create("P02", Link.class), list);

		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking03) list.add(Id.create(id, Link.class));
		toParkingSubRoutes.put(Id.create("P03", Link.class), list);

		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking04) list.add(Id.create(id, Link.class));
		toParkingSubRoutes.put(Id.create("P04", Link.class), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking05) list.add(Id.create(id, Link.class));
		toParkingSubRoutes.put(Id.create("P05", Link.class), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking06) list.add(Id.create(id, Link.class));
		toParkingSubRoutes.put(Id.create("P06", Link.class), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking07) list.add(Id.create(id, Link.class));
		toParkingSubRoutes.put(Id.create("P07", Link.class), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking07a) list.add(Id.create(id, Link.class));
		toParkingSubRoutes.put(Id.create("P07a", Link.class), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking08) list.add(Id.create(id, Link.class));
		toParkingSubRoutes.put(Id.create("P08", Link.class), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking09) list.add(Id.create(id, Link.class));
		toParkingSubRoutes.put(Id.create("P09", Link.class), list);
		
		/*
		 * from the parkings...
		 */
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking01) list.add(Id.create(id, Link.class));
		fromParkingSubRoutes.put(Id.create("P01", Link.class), list);

		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking02) list.add(Id.create(id, Link.class));
		fromParkingSubRoutes.put(Id.create("P02", Link.class), list);

		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking03) list.add(Id.create(id, Link.class));
		fromParkingSubRoutes.put(Id.create("P03", Link.class), list);

		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking04) list.add(Id.create(id, Link.class));
		fromParkingSubRoutes.put(Id.create("P04", Link.class), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking05) list.add(Id.create(id, Link.class));
		fromParkingSubRoutes.put(Id.create("P05", Link.class), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking06) list.add(Id.create(id, Link.class));
		fromParkingSubRoutes.put(Id.create("P06", Link.class), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking07) list.add(Id.create(id, Link.class));
		fromParkingSubRoutes.put(Id.create("P07", Link.class), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking07a) list.add(Id.create(id, Link.class));
		fromParkingSubRoutes.put(Id.create("P07a", Link.class), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking08) list.add(Id.create(id, Link.class));
		fromParkingSubRoutes.put(Id.create("P08", Link.class), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking09) list.add(Id.create(id, Link.class));
		fromParkingSubRoutes.put(Id.create("P09", Link.class), list);
	}
}
