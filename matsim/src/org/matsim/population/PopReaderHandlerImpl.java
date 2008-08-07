/* *********************************************************************** *
 * project: org.matsim.*
 * PopReaderHandlerImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.population;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.xml.sax.Attributes;

public class PopReaderHandlerImpl	implements PopReaderHandler {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	protected Plans plans;
	protected Person currperson = null;
	protected Knowledge currhomeknowledge = null;
	protected Knowledge currworkknowledge = null;

	private boolean income_message = true;
//	private boolean zone_creation_message = true;

	// this is not that nice, but i do not think about it yet....
	// and anyway... it's only for the synpop by martin and not an
	// 'official' matsim xml version....
//	private int local_id = 1000000000;

	private final static Logger log = Logger.getLogger(PopReaderHandlerImpl.class);
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PopReaderHandlerImpl(final Plans plans) {
		this.plans = plans;
	}

	//////////////////////////////////////////////////////////////////////
	//
	// interface implementation
	//
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// <synthetic_population ... > ... </synthetic_population>
	//////////////////////////////////////////////////////////////////////

	public void startSynPop(final Attributes meta) {
		this.plans.setName("Synthetic Population made by " + meta.getValue("author") +
											 ", mail: " + meta.getValue("mail") + ", Date: " + meta.getValue("date"));
	}

	public void endSynPop() {
	}

	//////////////////////////////////////////////////////////////////////
	// <agent ... > ... </agent>
	//////////////////////////////////////////////////////////////////////

	public void startAgent(final Attributes meta) {
		this.currperson = new Person(new IdImpl(meta.getValue("id").substring(1)));
	}

	public void endAgent() {
		try {
			this.plans.addPerson(this.currperson);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		this.currperson = null;
	}

	//////////////////////////////////////////////////////////////////////
	// <home ... > ... </home>
	//////////////////////////////////////////////////////////////////////

	public void startHome(final Attributes meta) {
		this.currhomeknowledge = this.currperson.createKnowledge(null);
	}

	public void endHome() {
		this.currhomeknowledge = null;
	}

	//////////////////////////////////////////////////////////////////////
	// <workplace ... > ... </workplace>
	//////////////////////////////////////////////////////////////////////

	public void startWorkplace(final Attributes meta) {
		this.currworkknowledge = this.currperson.createKnowledge(null);
	}

	public void endWorkplace() {
		this.currworkknowledge = null;
	}

	//////////////////////////////////////////////////////////////////////
	// <location ... />
	//////////////////////////////////////////////////////////////////////

//	private final Zone addZone(final String zone_type, final String zone_id) {
//		if (zone_id == null) {
//			Gbl.errorMsg(this.getClass(),"getZone(...)","[zone_id=" + zone_id + "] is not allowed.");
//		}
//		if ((this.currhomeknowledge != null) && (this.currworkknowledge == null)) {
//			ActivityLocations al = this.currhomeknowledge.createActivityLocation("home");
//			return (Zone)al.addLocation(zone_type,zone_id,null);
//		}
//		else if ((this.currhomeknowledge == null) && (this.currworkknowledge != null)) {
//			ActivityLocations al = this.currworkknowledge.createActivityLocation("work");
//			return (Zone)al.addLocation(zone_type,zone_id,null);
//		}
//		else {
//			Gbl.errorMsg(this.getClass(), "getZone(...)","Something is wrong...");
//			return null;
//		}
//	}

//	private final Zone addZone(final String zone_type, final Coord min, final Zone muni_zone) {
//		Iterator h_it = muni_zone.getDownMapping().values().iterator();
//		while (h_it.hasNext()) {
//			Zone zone = (Zone)h_it.next();
//			if (zone.getMin().equals(min)) {
//				return this.addZone(zone_type, Integer.toString(zone.getId()));
//			}
//		}
//		Gbl.errorMsg(this.getClass(), "getZone(...)","[person_id=" + this.currperson.getId() + "]" + "[min=" + min + "]" + "zone does not exist!");
//		return null;
//	}

	public void startLocation(final Attributes meta) {
		// get the municipality id and the hektar coord
//		String muni_id = meta.getValue("municipality_number_2000");
//		Coord hektar_min = null;
//		if ((meta.getValue("x100") != null) && (meta.getValue("y100") != null)) {
//			hektar_min = new Coord(meta.getValue("x100"), meta.getValue("y100"));
//		}
		// add the zones
		Gbl.errorMsg("[this does not work anymore. needs to be done somewhen....]");
//		Zone muni_zone = this.addZone("municipality",muni_id);
//		if (hektar_min != null) {
//			this.addZone("hektar",hektar_min,muni_zone);
//		}
	}

	public void endLocation() {
	}

	//////////////////////////////////////////////////////////////////////
	// <age ... />
	//////////////////////////////////////////////////////////////////////

	public void startAge(final Attributes meta) {
		String [] strs = meta.getValue("value").split("[gt-]");
		int age = -1;
		if (strs.length == 1) { // value = 0
			age = Integer.parseInt(strs[0]);
		}
		else if (strs.length == 2) { // value = xx-yy
			int min = Integer.parseInt(strs[0]);
			int max = Integer.parseInt(strs[1]);
			age = min + Gbl.random.nextInt(max-min+1);
		}
		else if (strs.length == 3) { // value = gt105
			age = Integer.parseInt(strs[2]);
		}
		else {
			Gbl.errorMsg("[value=" + meta.getValue("value") + "]" +
									 "[strs.length=" + strs.length + "]" + "some unknown error.");
		}
		this.currperson.setAge(age);
	}

	public void endAge() {
	}

	//////////////////////////////////////////////////////////////////////
	// <sex ... />
	//////////////////////////////////////////////////////////////////////

	public void startSex(final Attributes meta) {
		if (meta.getValue("label").equals("male"))
			this.currperson.setSex("m");
		else // female
			this.currperson.setSex("f");
	}

	public void endSex() {
	}

	//////////////////////////////////////////////////////////////////////
	// <driver_licence_ownership ... />
	//////////////////////////////////////////////////////////////////////

	public void startDLicence(final Attributes meta) {
		this.currperson.setLicence(meta.getValue("label"));
	}

	public void endDLicence() {
	}

	//////////////////////////////////////////////////////////////////////
	// <car_availibility ... />
	//////////////////////////////////////////////////////////////////////

	public void startCarAvail(final Attributes meta) {
		if (meta.getValue("label").equals("after_consultation")) {
			this.currperson.setCarAvail("sometimes");
		}
		else {
			this.currperson.setCarAvail(meta.getValue("label"));
		}
	}

	public void endCarAvail() {
	}

	//////////////////////////////////////////////////////////////////////
	// <employed ... />
	//////////////////////////////////////////////////////////////////////

	public void startEmployed(final Attributes meta) {
		this.currperson.setEmployed(meta.getValue("label"));
	}

	public void endEmployed() {
	}

	//////////////////////////////////////////////////////////////////////
	// <half_fare_ticket_ownership ... />
	//////////////////////////////////////////////////////////////////////

	public void startHalfFare(final Attributes meta) {
		String type = null;
		if (meta.getValue("label").equals("yes1year")) {
			type = "ch-HT-1y";
		}
		else if (meta.getValue("label").equals("yes2years")) {
			type = "ch-HT-2y";
		}
		else if (meta.getValue("label").equals("yes2ymobility")) {
			type = "ch-HT-mobility";
		}
		else {
			// label = no
		}

		if (type != null) {
			this.currperson.addTravelcard(type);
		}
	}

	public void endHalfFare() {
	}

	//////////////////////////////////////////////////////////////////////
	// <general_abonnement_ownership ... />
	//////////////////////////////////////////////////////////////////////

	public void startGA(final Attributes meta) {
		String type = null;
		if (meta.getValue("label").equals("yes")) {
			type = "ch-GA";
		}
		else {
			// label = no
		}

		if (type != null) {
			this.currperson.addTravelcard(type);
		}
	}

	public void endGA() {
	}

	//////////////////////////////////////////////////////////////////////
	// <household_monthly_income ... />
	//////////////////////////////////////////////////////////////////////

	public void startIncome(final Attributes meta) {
		if (this.income_message) {
			log.info("not handled at the moment.");
			this.income_message = false;
		}
	}

	public void endIncome() {
	}
}
