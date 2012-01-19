package playground.kai.bvwp;

import playground.kai.bvwp.Values.Entry;



/**
 * Example for a class to be maintained by users.
 * 
 * @author nagel
 */
class UtilityChangesNEW extends UtilityChanges { 
	@Override
	UtlChanges computeUtilities(ValuesForAUserType econValues, ValuesForAUserType quantitiesNullfall,
			ValuesForAUserType quantitiesPlanfall, Entry entry, double deltaAmounts) 
	{
		UtlChanges utlChanges = new UtlChanges() ;
		utlChanges.deltaQuantity = quantitiesNullfall.getByEntry( entry ) 
		- quantitiesPlanfall.getByEntry( entry ) ;

		utlChanges.utlGainByOldUsers = utlChanges.deltaQuantity * quantitiesNullfall.getByEntry( Entry.amount ) 
		* econValues.getByEntry( entry );

		utlChanges.utlGainByNewUsers = 0.5 * utlChanges.deltaQuantity * deltaAmounts * econValues.getByEntry(entry);
		return utlChanges;
	}
}