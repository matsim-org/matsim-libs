package playground.kai.bvwp;

import org.matsim.api.core.v01.Id;

import playground.kai.bvwp.Values.Entry;
import playground.kai.bvwp.Values.Mode;
import playground.kai.bvwp.Values.Type;


/**
 * Class that provides services.  To be maintained by ``programmers''.
 * 
 * @author nagel
 */
abstract class UtilityChanges {
	UtilityChanges() {
		System.out.println("\nSetting utility computation method to " + this.getClass() ) ;
	}
	
	final void utilityChange( Values economicValues, ScenarioForEvalData nullfall, ScenarioForEvalData planfall ) {
		// (GK-GK') * x + 0.5 * (GK-GK') (x'-x) =
		// 0.5 * (GK-GK') (x+x') = 0.5 * ( GK*x + GK*x' - GK'*x - GK'*x' )

		double utils = 0. ;

		for ( Id id : nullfall.values.keySet() ) { // for all OD relations
			Values nullfallForODRelation = nullfall.values.get(id) ;
			Values planfallForODRelation = planfall.values.get(id) ;
			for ( Mode mode : Mode.values() ) { // for all modes
				ValuesForAMode econValuesByMode = economicValues.getByMode(mode) ;
				ValuesForAMode quantitiesNullfallByMode = nullfallForODRelation.getByMode(mode) ;
				ValuesForAMode quantitiesPlanfallByMode = planfallForODRelation.getByMode(mode) ;
				for ( Type type : Type.values() ) { // for all types (e.g. PV or GV)
					ValuesForAUserType econValues = econValuesByMode.getByDemandSegment(type) ;
					ValuesForAUserType quantitiesNullfall = quantitiesNullfallByMode.getByDemandSegment(type) ;
					ValuesForAUserType quantitiesPlanfall = quantitiesPlanfallByMode.getByDemandSegment(type) ;
					final double amountNullfall = quantitiesNullfall.getByEntry(Entry.XX);
					final double amountPlanfall = quantitiesPlanfall.getByEntry(Entry.XX);

					if ( amountPlanfall!=0. || amountNullfall!=0. ) {
						// (suppress output if this (relation,mode,demand_segment) is never used)
						
						double implicitUtl = this.computeImplicitUtility(econValues, quantitiesNullfall, quantitiesPlanfall) ;
						
						// ###############
						// ALTNUTZER
						final String fmtString = "%17s || %17.2f | %17.1f || %17.2f | %17.1f || %17.2f | %17.1f || %17.2f | %17.1f ||\n" ;
						if ( amountNullfall < amountPlanfall ) {
							// (wir haben (relevante) Altnutzer nur auf dem nehmenden Verkehrsmittel!)
							
							double amountAltnutzer = amountNullfall ;
							
							System.out.printf("%17s; %17s; %17s; verbleibender Verkehr: %17.1f Personen/Tonnen\n", 
									id, mode, type, amountAltnutzer );
							System.out.printf( "%17s || %17s | %17s || %17s | %17s || %17s | %17s || %17s | %17s ||\n",
									"Attribut",
									"Attribut Nullfall", "... mal Menge",
									"Attribut Planfall", "... mal Menge",
									"Attribut Diff", "... mal Menge",
									"Nutzen Diff", "... mal Menge") ;

							for ( Entry entry : Entry.values() ) { // for all entries (e.g. km or hrs)
								if ( entry != Entry.XX && entry != Entry.priceUser ) {
									// yyyy not so great: if policy measure = price change, then RoH and resource consumption are
									// different here.  kai/benjamin, sep'12
									
									double deltaQuantities = quantitiesPlanfall.getByEntry(entry)-quantitiesNullfall.getByEntry(entry) ;
									final double utlChangePerItem = deltaQuantities * econValues.getByEntry(entry);
									final double utlChange = utlChangePerItem * amountAltnutzer;
									System.out.printf(fmtString,
											entry,
											quantitiesNullfall.getByEntry(entry), 
											quantitiesNullfall.getByEntry(entry) * amountAltnutzer,
											quantitiesPlanfall.getByEntry(entry), 
											quantitiesPlanfall.getByEntry(entry) * amountAltnutzer,
											deltaQuantities , deltaQuantities * amountAltnutzer ,
											utlChangePerItem , 
											utlChange
									) ;
									utils += utlChange ;

								}
							}
						}
						
						// ###########
						// abgegeben oder aufgenommen
						
						double deltaAmounts = amountPlanfall - amountNullfall ;

						System.out.printf("%17s; %17s; %17s; " +
						"wechselnder Verkehr: %17.1f Personen/Tonnen\n", 
						id, mode, type, deltaAmounts ) ;
						System.out.printf( "%17s || %17s | %17s || %17s | %17s || %17s | %17s || %17s | %17s ||\n",
								"Attribut",
								"Attribut Nullfall", "... mal Menge",
								"Attribut Planfall", "... mal Menge",
								"Attribut Diff", "... mal Menge",
								"Nutzen Diff", "... mal Menge") ;
						
						for ( Entry entry : Entry.values() ) { // for all entries (e.g. km or hrs)
							if ( entry != Entry.XX && entry != Entry.priceUser ) {
								final double quantityPlanfall = quantitiesPlanfall.getByEntry(entry);
								final double quantityNullfall = quantitiesNullfall.getByEntry(entry);

								UtlChangesData utlChangesPerItem = utlChangePerEntry(entry, deltaAmounts, 
										quantityNullfall, quantityPlanfall, econValues.getByEntry(entry));
								final double utlChange = utlChangesPerItem.utl * Math.abs(deltaAmounts);
								utils += utlChange ;
								

								if ( deltaAmounts > 0 ) {
									// Wir sind aufnehmend!

									System.out.printf(fmtString,
											entry,
											quantityNullfall, 0., 
											quantityPlanfall, 
											quantityPlanfall * deltaAmounts,
											quantityPlanfall, 
											quantityPlanfall * deltaAmounts,
											utlChangesPerItem.utl , 
											utlChange
									) ;

								} else {
									// wir sind abgebend
									System.out.printf(fmtString,
											entry,
											quantityNullfall, 
											quantityNullfall * deltaAmounts,
											quantityPlanfall, 0., 
											quantityPlanfall, 
											quantityPlanfall * deltaAmounts,
											utlChangesPerItem.utl , 
											utlChange
									) ;
								}

							}
						}
						if ( implicitUtl != 0. ) {
							final double implicitUtlOverall = implicitUtl * deltaAmounts;
							System.out.printf(fmtString,
									"implicit utl",
									0.,0.,
									0.,0.,
									0.,0.,
									implicitUtl, implicitUtlOverall 
									) ;
							utils += implicitUtlOverall ;

						}
					}
				}
			}
		}
		System.out.printf("utl gain: %171.1f\n", utils ) ;
	}
	abstract UtlChangesData utlChangePerEntry(Entry entry, double deltaAmount, 
			double quantityNullfall, double quantityPlanfall, double econVal);
	abstract double computeImplicitUtility(ValuesForAUserType econValues, ValuesForAUserType quantitiesNullfall, 
			ValuesForAUserType quantitiesPlanfall) ;
}