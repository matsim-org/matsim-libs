/* *********************************************************************** *
 * project: org.matsim.*
 * NOGATypes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.preprocess.plans.modifications;

import java.util.List;
import java.util.Vector;

public class NOGATypes {
	
	// Exact definition of Supermarket etc.
	public String [] shopGrocery = {
			"B015227B",     // 52.27B Sonstiger Fachdetailhandel mit Nahrungsmitteln, Getränken und Tabak a.n.g. (in Verkaufsräumen)
	};
	
	public String [] shopNonGrocery = {
		"B015274A",     // 52.74A Reparatur von sonstigen Gebrauchsgütern
	};
	
	public List<String> getGroceryTypes() {
		List<String> groceryTypes = new Vector<String>();		
		for (int i = 0; i < shopGrocery.length; i++) {
			groceryTypes.add(shopGrocery[i]);
		}
		return groceryTypes;
	}
	
	public List<String> getNonGroceryTypes() {
		List<String> nongroceryTypes = new Vector<String>();		
		for (int i = 0; i < shopNonGrocery.length; i++) {
			nongroceryTypes.add(shopNonGrocery[i]);
		}
		return nongroceryTypes;
	}
}
