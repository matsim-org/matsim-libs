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
import org.matsim.core.basic.v01.IdImpl;
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
		parkingList.add(new IdImpl("P01"));
		parkingList.add(new IdImpl("P02"));
		parkingList.add(new IdImpl("P03"));
		parkingList.add(new IdImpl("P04"));
		parkingList.add(new IdImpl("P05"));
		parkingList.add(new IdImpl("P07a"));	// can be reached from both
		availableParkings.put(new IdImpl("L30"), parkingList);

		parkingList = new ArrayList<Id<Link>>();
		parkingList.add(new IdImpl("P06"));
		parkingList.add(new IdImpl("P07"));
		parkingList.add(new IdImpl("P08"));
		parkingList.add(new IdImpl("P09"));
		availableParkings.put(new IdImpl("L01"), parkingList);
		
		/*
		 * Create data structure containing the sub-routes from the decision links
		 * to the parkings.
		 */
		List<Id<Link>> list;
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking01) list.add(new IdImpl(id));
		toParkingSubRoutes.put(new IdImpl("P01"), list);

		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking02) list.add(new IdImpl(id));
		toParkingSubRoutes.put(new IdImpl("P02"), list);

		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking03) list.add(new IdImpl(id));
		toParkingSubRoutes.put(new IdImpl("P03"), list);

		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking04) list.add(new IdImpl(id));
		toParkingSubRoutes.put(new IdImpl("P04"), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking05) list.add(new IdImpl(id));
		toParkingSubRoutes.put(new IdImpl("P05"), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking06) list.add(new IdImpl(id));
		toParkingSubRoutes.put(new IdImpl("P06"), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking07) list.add(new IdImpl(id));
		toParkingSubRoutes.put(new IdImpl("P07"), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking07a) list.add(new IdImpl(id));
		toParkingSubRoutes.put(new IdImpl("P07a"), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking08) list.add(new IdImpl(id));
		toParkingSubRoutes.put(new IdImpl("P08"), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.toParking09) list.add(new IdImpl(id));
		toParkingSubRoutes.put(new IdImpl("P09"), list);
		
		/*
		 * from the parkings...
		 */
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking01) list.add(new IdImpl(id));
		fromParkingSubRoutes.put(new IdImpl("P01"), list);

		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking02) list.add(new IdImpl(id));
		fromParkingSubRoutes.put(new IdImpl("P02"), list);

		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking03) list.add(new IdImpl(id));
		fromParkingSubRoutes.put(new IdImpl("P03"), list);

		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking04) list.add(new IdImpl(id));
		fromParkingSubRoutes.put(new IdImpl("P04"), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking05) list.add(new IdImpl(id));
		fromParkingSubRoutes.put(new IdImpl("P05"), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking06) list.add(new IdImpl(id));
		fromParkingSubRoutes.put(new IdImpl("P06"), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking07) list.add(new IdImpl(id));
		fromParkingSubRoutes.put(new IdImpl("P07"), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking07a) list.add(new IdImpl(id));
		fromParkingSubRoutes.put(new IdImpl("P07a"), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking08) list.add(new IdImpl(id));
		fromParkingSubRoutes.put(new IdImpl("P08"), list);
		
		list = new ArrayList<Id<Link>>();
		for (String id : CreateVisitorPopulation.fromParking09) list.add(new IdImpl(id));
		fromParkingSubRoutes.put(new IdImpl("P09"), list);
	}
}
