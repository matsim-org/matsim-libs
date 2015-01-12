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

import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;

/**
 * @author johannes
 *
 */
public class LegMainPurposeHandler implements LegAttributeHandler {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.mid.LegAttributeHandler#handle(playground.johannes.gsv.synPop.ProxyLeg, java.util.Map)
	 */
	@Override
	public void handle(ProxyObject leg, Map<String, String> attributes) {
		String typeId = attributes.get(MIDKeys.LEG_MAIN_TYPE);

		if(typeId == null) {
			throw new NullPointerException();
		}
		
		if(typeId.equalsIgnoreCase("Erreichen des Arbeitsplatzes")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.WORK);
			
		} else if(typeId.equalsIgnoreCase("dienstlich oder geschäftlich")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.BUISINESS);
			
		} else if(typeId.equalsIgnoreCase("Erreichen der Ausbildungsstätte oder Schule")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.EDUCATION);
			
		} else if(typeId.equalsIgnoreCase("Einkauf")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.SHOP);
			
		} else if(typeId.equalsIgnoreCase("private Erledigungen")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "private");
			
		} else if(typeId.equalsIgnoreCase("Bringen oder Holen von Personen")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "pickdrop");
			
		} else if(typeId.equalsIgnoreCase("Freizeitaktivität")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.LEISURE);
			
			String subtype = attributes.get(MIDKeys.LEG_SUB_TYPE);
			if(subtype != null) {
				if(subtype.equalsIgnoreCase("Besuch oder Treffen mit|von Freunden, Verwandten, Bekannten")) {
					leg.setAttribute(CommonKeys.LEG_PURPOSE, "visit");
				} else if(subtype.equalsIgnoreCase("Besuch kultureller Einrichtung (z.B. Kino, Theater, Museum")) {
					leg.setAttribute(CommonKeys.LEG_PURPOSE, "culture");
				} else if(subtype.equalsIgnoreCase("Besuch einer Veranstaltung (z.B. Fußballspiel, Markt)")) {
					leg.setAttribute(CommonKeys.LEG_PURPOSE, "culture");
				} else if(subtype.equalsIgnoreCase("Sport (selbst aktiv), Sportverein")) {
					leg.setAttribute(CommonKeys.LEG_PURPOSE, "sport");
				} else if(subtype.equalsIgnoreCase("Restaurant, Gaststätte, Mittagessen etc.")) {
					leg.setAttribute(CommonKeys.LEG_PURPOSE, "gastro");
				} else if(subtype.equalsIgnoreCase("Tagesausflug, mehrtägiger Ausflug (bis 4 Tage)")) {
					leg.setAttribute(CommonKeys.LEG_PURPOSE, "vacations_short");
				} else if(subtype.equalsIgnoreCase("Urlaub (ab 5 Tage)")) {
					leg.setAttribute(CommonKeys.LEG_PURPOSE, "vacations_long");
				}
			}
			
		} else if(typeId.equalsIgnoreCase("nach Hause")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.HOME);
			
		} else if(typeId.equalsIgnoreCase("zur Schule oder Vorschule")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.EDUCATION);
			
		} else if(typeId.equalsIgnoreCase("Kindertagesstätte oder Kindergarten")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.EDUCATION);
			
		} else {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, ActivityType.MISC);
		}
		
	}

}
