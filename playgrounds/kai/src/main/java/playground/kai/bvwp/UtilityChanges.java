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
					double deltaAmounts = amountPlanfall - amountNullfall ;

					if ( amountPlanfall!=0. || amountNullfall!=0. ) {
						// (suppress output if this (relation,mode,demand_segment) is never used)
						
						// ###############
						// ALTNUTZER
						
						final String fmtString = "Attribut: %-10s || " +
								"Attribut Nullfall : %10.1f; ... mal Menge: %10.1f || " +
								"Attribut Planfall : %10.1f; ... mal Menge: %10.1f || " +
								"Attribut Differenz: %10.1f; ... mal Menge: %10.1f || " +
								"Nutzen Differenz: %10.1f; ... mal Menge: %10.1f\n";
						if ( amountNullfall < amountPlanfall ) {
							// (wir haben nur Altnutzer nur auf dem nehmenden Verkehrsmittel!)
							
							double amountAltnutzer = amountNullfall ;
							
							System.out.printf("%10s; %10s; %10s; old demand: %10.1f persons/tons\n", 
									id, mode, type, amountAltnutzer );

							for ( Entry entry : Entry.values() ) { // for all entries (e.g. km or hrs)
								if ( entry != Entry.XX ) {
									UtlChangesData utlChanges = computeUtilities(econValues, quantitiesNullfall, quantitiesPlanfall, entry);
									double deltaQuantities = quantitiesPlanfall.getByEntry(entry)-quantitiesNullfall.getByEntry(entry) ; 
									System.out.printf(fmtString,
											entry,
											quantitiesNullfall.getByEntry(entry), 
											quantitiesNullfall.getByEntry(entry) * amountAltnutzer,
											quantitiesPlanfall.getByEntry(entry), 
											quantitiesPlanfall.getByEntry(entry) * amountAltnutzer,
											deltaQuantities , deltaQuantities * amountAltnutzer ,
											deltaQuantities * econValues.getByEntry(entry) , 
											deltaQuantities * econValues.getByEntry(entry) * amountAltnutzer
									) ;

//									if (  deltaQuantities != 0. || utlChanges.utlGainByOldUsers!=0.
//											|| utlChanges.utlGainByNewUsers!=0. || utlChanges.utl!=0. ) {
//										System.out.printf("%35s change for `old' users (Altnutzer): %10.1f %-10s", "-->",
//												deltaQuantities, entry) ;
//									}
//									if ( utlChanges.utlGainByOldUsers != 0. || utlChanges.utlGainByNewUsers != 0. ) {
//										System.out.printf("; utl (gain) old//new demand: %10.1f", utlChanges.utlGainByOldUsers ) ;
//										System.out.printf(" //%10.1f\n", utlChanges.utlGainByNewUsers ) ;
//										utils += utlChanges.utlGainByOldUsers + utlChanges.utlGainByNewUsers  ;
//									}
//									else if ( utlChanges.utl != 0.){
//										System.out.printf("; utl change: %10.1f\n", utlChanges.utl ) ;
//										utils += utlChanges.utl;
//									}
								}
							}
						}
						
						// ###########
						// abgegeben oder aufgenommen
						
						System.out.printf("%10s; %10s; %10s; old demand (for verification): %10.1f persons/tons; " +
								"new demand (for verification): %10.1f persons/tons; " +
						"demand change: %10.1f persons/tons\n", 
						id, mode, type, amountNullfall, 
						amountPlanfall, deltaAmounts ) ;
						
						double amountWechsler = amountPlanfall - amountNullfall ;

						for ( Entry entry : Entry.values() ) { // for all entries (e.g. km or hrs)
							if ( entry != Entry.XX ) {
								UtlChangesData utlChanges = computeUtilities(econValues, quantitiesNullfall, quantitiesPlanfall, entry);
								double deltaQuantities = quantitiesPlanfall.getByEntry(entry)-quantitiesNullfall.getByEntry(entry) ;
								
								if ( amountWechsler > 0 ) {
									// Wir sind aufnehmend!
								
								System.out.printf(fmtString,
										entry,
										0., 0., 
										quantitiesPlanfall.getByEntry(entry), 
										quantitiesPlanfall.getByEntry(entry) * amountWechsler,
										quantitiesPlanfall.getByEntry(entry), 
										quantitiesPlanfall.getByEntry(entry) * amountWechsler,
										quantitiesPlanfall.getByEntry(entry) * econValues.getByEntry(entry) , 
										quantitiesPlanfall.getByEntry(entry) * amountWechsler * econValues.getByEntry(entry) 
								) ;
								
								} else {
									// wir sind abgebend
									System.out.printf(fmtString,
											entry,
											quantitiesNullfall.getByEntry(entry), 
											quantitiesNullfall.getByEntry(entry) * amountWechsler,
											0., 0., 
											quantitiesPlanfall.getByEntry(entry), 
											quantitiesPlanfall.getByEntry(entry) * amountWechsler,
											quantitiesPlanfall.getByEntry(entry) * econValues.getByEntry(entry) , 
											quantitiesPlanfall.getByEntry(entry) * amountWechsler * econValues.getByEntry(entry) 
									) ;
								}

//								if (  deltaQuantities != 0. || utlChanges.utlGainByOldUsers!=0.
//										|| utlChanges.utlGainByNewUsers!=0. || utlChanges.utl!=0. ) {
//									System.out.printf("%35s change for `old' users (Altnutzer): %10.1f %-10s", "-->",
//											deltaQuantities, entry) ;
//								}
//								if ( utlChanges.utlGainByOldUsers != 0. || utlChanges.utlGainByNewUsers != 0. ) {
//									System.out.printf("; utl (gain) old//new demand: %10.1f", utlChanges.utlGainByOldUsers ) ;
//									System.out.printf(" //%10.1f\n", utlChanges.utlGainByNewUsers ) ;
//									utils += utlChanges.utlGainByOldUsers + utlChanges.utlGainByNewUsers  ;
//								}
//								else if ( utlChanges.utl != 0.){
//									System.out.printf("; utl change: %10.1f\n", utlChanges.utl ) ;
//									utils += utlChanges.utl;
//								}
							}
						}
					}
				}
			}
		}
		System.out.printf("utl gain: %10.1f\n", utils ) ;
	}
	abstract UtlChangesData computeUtilities(ValuesForAUserType econValues, ValuesForAUserType quantitiesNullfall, 
			ValuesForAUserType quantitiesPlanfall, Entry entry);
}