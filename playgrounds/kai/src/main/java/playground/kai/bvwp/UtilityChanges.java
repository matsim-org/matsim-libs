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
	final void utilityChange( Values economicValues, ScenarioForEval nullfall, ScenarioForEval planfall ) {
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
					ValuesForAUserType econValues = econValuesByMode.getByType(type) ;
					ValuesForAUserType quantitiesNullfall = quantitiesNullfallByMode.getByType(type) ;
					ValuesForAUserType quantitiesPlanfall = quantitiesPlanfallByMode.getByType(type) ;
					for ( Entry entry : Entry.values() ) { // for all entries (e.g. km or hrs)
						double deltaAmounts = quantitiesPlanfall.getByEntry( Entry.amount ) 
						- quantitiesNullfall.getByEntry( Entry.amount ) ;
						if ( entry == Entry.amount ) {
							System.out.printf("%10s; %10s; %10s; old demand: %10.1f; demand change: %10.1f\n", id, mode, type, 
									quantitiesNullfall.getByEntry(Entry.amount) , deltaAmounts ) ;
						} else {
							UtlChanges utlChanges = computeUtilities(econValues, quantitiesNullfall, quantitiesPlanfall, entry,
									deltaAmounts);

							if ( utlChanges.deltaQuantity != 0. ) {
								System.out.printf("%10s; %10s; %10s; change per demand item: %10.1f %10s", id, mode, type, 
										utlChanges.deltaQuantity, entry) ;
								System.out.printf("; utl gain old//new demand: %10.1f", utlChanges.utlGainByOldUsers ) ;
								System.out.printf(" //%10.1f\n", utlChanges.utlGainByNewUsers ) ;
							}

							utils += utlChanges.utlGainByOldUsers + utlChanges.utlGainByNewUsers  ;
						}
					}
				}
			}
		}
		System.out.printf("utl gain: %10.1f\n", utils ) ;
	}
	abstract UtlChanges computeUtilities(ValuesForAUserType econValues, ValuesForAUserType quantitiesNullfall, 
			ValuesForAUserType quantitiesPlanfall, Entry entry, double deltaAmounts);
}