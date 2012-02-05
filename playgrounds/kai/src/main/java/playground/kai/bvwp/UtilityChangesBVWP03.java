package playground.kai.bvwp;

import playground.kai.bvwp.Values.Entry;



/**
 * Example for a class to be maintained by users.
 * 
 * @author nagel
 */
class UtilityChangesBVWP03 extends UtilityChanges { 
	UtilityChangesBVWP03() {
		System.out.println("\nSetting utility computation method to " + this.getClass()  ) ;
	}

	@Override
	UtlChanges computeUtilities(ValuesForAUserType econValues, ValuesForAUserType nullfall,
			ValuesForAUserType planfall, Entry entry, double deltaXX) 
	{
		UtlChanges utlChanges = new UtlChanges() ;

		// x * t * VoT
		{
			double utlOld = nullfall.getByEntry(Entry.XX) * nullfall.getByEntry(entry) * econValues.getByEntry(entry) ;
			double utlNew = nullfall.getByEntry(Entry.XX) * planfall.getByEntry(entry) * econValues.getByEntry(entry) ;
			utlChanges.utlGainByOldUsers = utlNew - utlOld ;
		}
		{
			double utlOld = 0. ;
			double utlNew = deltaXX * planfall.getByEntry(entry) * econValues.getByEntry(entry) ;
			utlChanges.utlGainByNewUsers = utlNew - utlOld ;
		}

		return utlChanges;
	}
}