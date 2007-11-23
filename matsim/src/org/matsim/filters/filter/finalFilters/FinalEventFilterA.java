/* *********************************************************************** *
 * project: org.matsim.*
 * FinalEventFilterA.java
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

package org.matsim.filters.filter.finalFilters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.events.BasicEvent;
import org.matsim.events.EventLinkEnter;
import org.matsim.filters.filter.EventFilterA;
import org.matsim.filters.filter.EventFilterI;
import org.matsim.filters.writer.UserDefAtt;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plans;

/**
 * offers some important possibility to export attributs defined by VISUM-user
 * und their corresponding value
 * 
 * @author ychen
 */
public abstract class FinalEventFilterA extends EventFilterA implements
		EventFilterI {
	/*-----------------------MEMBER VARIABLES---------------------*/
	protected Plans plans;

	protected NetworkLayer network;

	protected List<UserDefAtt> udas = new ArrayList<UserDefAtt>();

	/**
	 * a TreeMap<Integer linkID, List<Double the value of attribut defined by
	 * User of VISUM 9.32>>
	 */
	protected Map<String, List<Double>> udaws = new HashMap<String, List<Double>>();

	/*-----------------------CONSTRUCTOR-----------------------*/
	/**
	 * builds a FinalEventFilterA
	 * 
	 * @param plans -
	 *            the Plans, which will be created in test
	 * @param network -
	 *            the NetworkLayer, which will be created in test
	 */
	public FinalEventFilterA(Plans plans, NetworkLayer network) {
		this.plans = plans;
		this.network = network;
	}

	/*-----------------------NORMAL METHOD---------------------*/
	/**
	 * rebuilds a real EventLinkEnter-event.
	 * 
	 * @param enter -
	 *            the event, that a Person enters in a link.
	 * @return a real EventLinkEnter-event.
	 */
	public EventLinkEnter rebuildEventLinkEnter(EventLinkEnter enter) {
		// very important to rebuild LinkEventData Object: event, aim to get
		// the id and the length of the right link
		enter.rebuild(plans, network);
		// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
		return enter;
	}

	/*-----------------------ABSTRACT METHODS-----------------------*/
	/**
	 * Returns the list of attributs defined by VISUM9.3-user
	 * 
	 * @return the set of attributs defined by VISUM9.3-user
	 */
	public abstract List<UserDefAtt> UDAexport();

	/**
	 * Returns the TreeMap of values of attributs defined by VISUM9.3-user
	 * 
	 * @return the TreeMap of values of attributs defined by VISUM9.3-user
	 */
	public abstract Map<String, List<Double>> UDAWexport();

	/* (non-Javadoc)
	 * @see org.matsim.demandmodeling.filters.filter.EventFilterA#judge(org.matsim.demandmodeling.events.BasicEvent)
	 */
	@Override
	public boolean judge(BasicEvent event) {
		return false;
	}
	
}
