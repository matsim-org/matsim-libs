/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesOpentimesKTIYear2.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.facilities;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.Facility;
import org.matsim.facilities.Opentime;
import org.matsim.facilities.algorithms.FacilityAlgorithm;
import org.matsim.gbl.Gbl;
import org.matsim.world.Location;

/**
 * Assign every shop an opening time based on shopsOf2005 survey.
 * 
 * @author meisterk
 *
 */
public class FacilitiesOpentimesKTIYear2 extends FacilityAlgorithm {

	public static final String ACT_TYPE_SHOP = "shop";
	public static final String ACT_TYPE_WORK = "work";

	private Facilities shopsOf2005 = new Facilities("shopsOf2005", Facilities.FACILITIES_NO_STREAMING);

	private String shopsOf2005Filename = "/home/meisterk/sandbox00/ivt/studies/switzerland/facilities/shopsOf2005/facilities_shopsOf2005.xml";

	public FacilitiesOpentimesKTIYear2() {
		super();
		// TODO Auto-generated constructor stub
	}

	public void init() {

		System.out.println("Reading shops Of 2005 xml file... ");
		FacilitiesReaderMatsimV1 facilities_reader = new FacilitiesReaderMatsimV1(shopsOf2005);
		facilities_reader.readFile(shopsOf2005Filename);
		System.out.println("Reading shops Of 2005 xml file...done.");

	}

	@Override
	public void run(Facility facility) {

		TreeMap<String, TreeSet<Opentime>> openTimes = new TreeMap<String, TreeSet<Opentime>>();

		ArrayList<Location> closestShops = shopsOf2005.getNearestLocations(facility.getCenter());
		Activity act = ((Facility) closestShops.get(0)).getActivity(ACT_TYPE_SHOP);
		if (act != null) {
			openTimes = act.getOpentimes();
		}
		for (String actType : new String[]{ACT_TYPE_SHOP, ACT_TYPE_WORK}) {
			facility.getActivity(actType).setOpentimes(openTimes);
		}
	}

}
