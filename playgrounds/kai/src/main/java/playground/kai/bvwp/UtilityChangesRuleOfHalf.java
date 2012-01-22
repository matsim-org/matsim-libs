package playground.kai.bvwp;

import playground.kai.bvwp.Values.Entry;



/**
 * Example for a class to be maintained by users.
 * 
 * @author nagel
 */
class UtilityChangesRuleOfHalf extends UtilityChanges {
	UtilityChangesRuleOfHalf() {
		System.out.println("\nSetting utility computation method to " + this.getClass() ) ;
	}
	
	@Override
	UtlChanges computeUtilities(ValuesForAUserType econValues, ValuesForAUserType quantitiesNullfall,
			ValuesForAUserType quantitiesPlanfall, Entry entry, double deltaAmounts) 
	{
		UtlChanges utlChanges = new UtlChanges() ;
		utlChanges.deltaQuantity = quantitiesPlanfall.getByEntry( entry ) 
		- quantitiesNullfall.getByEntry( entry ) ;

		utlChanges.utlGainByOldUsers = utlChanges.deltaQuantity * quantitiesNullfall.getByEntry( Entry.XX ) 
		* econValues.getByEntry( entry );

		if ( deltaAmounts > 0. ) {
			// (compute only for receiving facility)
			utlChanges.utlGainByNewUsers = 0.5 * utlChanges.deltaQuantity * deltaAmounts * econValues.getByEntry(entry);
		}
		return utlChanges;
	}
}