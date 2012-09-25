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



/**
 * @author Ihab
 *
 */
 class UtilityChangesRuleOfHalf extends UtilityChanges {
	
		
		@Override
		UtlChangesData utlChangePerItem(double deltaAmount,
				double quantityNullfall, double quantityPlanfall, double margUtl) {

		UtlChangesData utlChanges = new UtlChangesData() ;
		
		if ( deltaAmount > 0 ) {
			// wir sind aufnehmend; es gilt die RoH
			utlChanges.utl = (quantityPlanfall-quantityNullfall) * margUtl / 2. ;
		} else {
			utlChanges.utl = 0. ;
		}

		return utlChanges;
	}

	@Override
	double computeImplicitUtility(ValuesForAUserType econValues,
			ValuesForAUserType quantitiesNullfall,
			ValuesForAUserType quantitiesPlanfall) {
		return 0;
	}

}
