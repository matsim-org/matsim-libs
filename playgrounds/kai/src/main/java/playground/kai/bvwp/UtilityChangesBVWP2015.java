/* *********************************************************************** *
 * project: org.matsim.*
 * UtilityChangesBVWP2003.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.kai.bvwp;

import playground.kai.bvwp.Values.Entry;


/**
 * @author Ihab
 *
 */
public class UtilityChangesBVWP2015 extends UtilityChanges {
	UtilityChangesBVWP2015() {
		System.out.println("\nSetting utility computation method to " + this.getClass() ) ;
	}
	
	@Override
	UtlChanges computeUtilities(ValuesForAUserType econValues, ValuesForAUserType quantitiesNullfall, 
			ValuesForAUserType quantitiesPlanfall, Entry entry) {
		
		UtlChanges utlChanges = new UtlChanges() ;
		
		double personenXNull = quantitiesNullfall.getByEntry(Entry.XX) * quantitiesNullfall.getByEntry(entry);
		double personenXPlan = quantitiesPlanfall.getByEntry(Entry.XX) * quantitiesPlanfall.getByEntry(entry);
		double diff = personenXPlan - personenXNull;
		
		utlChanges.utl = diff * econValues.getByEntry(entry);	
		
		return utlChanges;
	}

}
