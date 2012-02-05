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
					double deltaAmounts = quantitiesPlanfall.getByEntry( Entry.XX ) - quantitiesNullfall.getByEntry( Entry.XX ) ;
					if ( quantitiesPlanfall.getByEntry(Entry.XX)!=0. && quantitiesNullfall.getByEntry(Entry.XX)!=0. ) {
						System.out.printf("%10s; %10s; %10s; old demand: %10.1f; new demand: %10.1f; demand change: %10.1f\n", 
								id, mode, type, quantitiesNullfall.getByEntry(Entry.XX), 
								quantitiesPlanfall.getByEntry(Entry.XX), deltaAmounts ) ;
					}
					for ( Entry entry : Entry.values() ) { // for all entries (e.g. km or hrs)
						if ( entry != Entry.XX ) {
							UtlChanges utlChanges = computeUtilities(econValues, quantitiesNullfall, quantitiesPlanfall, entry);

							if ( utlChanges.utlGainByOldUsers != 0. || utlChanges.utlGainByNewUsers != 0. ) {
								System.out.printf("%35s change per person/ton: %10.1f %10s", "-->",
										utlChanges.deltaQuantity, entry) ;
								System.out.printf("; utl (gain) old//new demand: %10.1f", utlChanges.utlGainByOldUsers ) ;
								System.out.printf(" //%10.1f\n", utlChanges.utlGainByNewUsers ) ;
								utils += utlChanges.utlGainByOldUsers + utlChanges.utlGainByNewUsers  ;
							}
							else if ( utlChanges.utl != 0.){
								System.out.printf("%35s utl change: %10.1f", "-->", utlChanges.utl ) ;
								utils += utlChanges.utl;
							}
						}
					}
				}
			}
		}
		System.out.printf("utl gain: %10.1f\n", utils ) ;
	}
	abstract UtlChanges computeUtilities(ValuesForAUserType econValues, ValuesForAUserType quantitiesNullfall, 
			ValuesForAUserType quantitiesPlanfall, Entry entry);
}