package playground.vsp.zzArchive.bvwpOld;
///* *********************************************************************** *
// * project: org.matsim.*
// * UtitlityChangesBVWP2010.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2009 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//package playground.kai.bvwp;
//
//import playground.kai.bvwp.Values.Entry;
//
///**
// * @author benjamin
// *
// */
//public class UtilityChangesBVWP2010Old extends UtilityChanges{
//	UtilityChangesBVWP2010Old() {
//		super() ;
//		System.out.println("\nTrying to reproduce methodology from ``Bedarfsplanüberprüfung 2010''...\n" ) ;
//	}
//
//	@Override
//	UtlChangesData utlChangePerItem(double deltaAmounts, double quantityNullfall,
//			double quantityPlanfall, double econVal) {
//
//		UtlChangesData utlChanges = new UtlChangesData();
//		double personenXNull;
//		double personenXPlan;
//		double diff;
//
//		if(entry.equals(Entry.hrs)){
////			double deltaAmounts = quantitiesPlanfall.getByEntry(Entry.XX) - quantitiesNullfall.getByEntry(Entry.XX) ;
//			double utlGainByOldUsers = 0.;
//			double utlGainByNewUsers = 0.;
//			
//			utlChanges.deltaQuantity = quantitiesPlanfall.getByEntry( entry ) - quantitiesNullfall.getByEntry( entry ) ;
//			utlGainByOldUsers = utlChanges.deltaQuantity * quantitiesNullfall.getByEntry( Entry.XX ) * econValues.getByEntry( entry );
//
//			if ( deltaAmounts > 0. ) {
//				// (compute only for receiving facility)
//				utlGainByNewUsers = 0.5 * utlChanges.deltaQuantity * deltaAmounts * econValues.getByEntry(entry);
//			}
//			utlChanges.utl = utlGainByOldUsers + utlGainByNewUsers;
////			System.out.println(utlChanges.utl);
//		} else {
//			personenXNull = quantitiesNullfall.getByEntry(Entry.XX) * quantitiesNullfall.getByEntry(entry);
//			personenXPlan = quantitiesPlanfall.getByEntry(Entry.XX) * quantitiesPlanfall.getByEntry(entry);
//			diff = personenXPlan - personenXNull;
//			utlChanges.utl = diff * econValues.getByEntry(entry);
//		}
//		return utlChanges;
//	}
//
//}
