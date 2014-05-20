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

package playground.johannes.gsv.synPop.mid;

import java.util.Map;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyLeg;

/**
 * @author johannes
 *
 */
public class LegMainPurposeHandler implements LegAttributeHandler {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.mid.LegAttributeHandler#handle(playground.johannes.gsv.synPop.ProxyLeg, java.util.Map)
	 */
	@Override
	public void handle(ProxyLeg leg, Map<String, String> attributes) {
		String typeId = attributes.get(MIDKeys.LEG_MAIN_TYPE);

		if(typeId == null) {
			throw new NullPointerException();
		}
		
		if(typeId.equalsIgnoreCase("Erreichen des Arbeitsplatzes")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "work");
		} else if(typeId.equalsIgnoreCase("dienstlich oder geschäftlich")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "work");
		} else if(typeId.equalsIgnoreCase("Erreichen der Ausbildungsstätte oder Schule")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "edu");
		} else if(typeId.equalsIgnoreCase("Einkauf")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "shop");
		} else if(typeId.equalsIgnoreCase("private Erledigungen")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "private");
		} else if(typeId.equalsIgnoreCase("Bringen oder Holen von Personen")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "droppoff_pickup");
		} else if(typeId.equalsIgnoreCase("Freizeitaktivität")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "leisure");
		} else if(typeId.equalsIgnoreCase("nach Hause")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "home");
		} else if(typeId.equalsIgnoreCase("zur Schule oder Vorschule")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "edu");
		} else if(typeId.equalsIgnoreCase("Kindertagesstätte oder Kindergarten")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "edu");
		} else {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "misc");
		}
		
	}

}
