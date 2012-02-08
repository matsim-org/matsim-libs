package playground.kai.bvwp;

import playground.kai.bvwp.Values.Entry;
import playground.kai.bvwp.Values.Mode;
import playground.kai.bvwp.Values.Type;

/**
 * @author Ihab
 *
 */

class IllustrationAP200PVEconomicValues {

	static Values createEconomicValues1() {
		Values economicValues = new Values() ;
		{
			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
			{
				ValuesForAUserType pvValues = roadValues.getByType(Type.PV) ;
				pvValues.setByEntry( Entry.km, -0.50 ) ;
				pvValues.setByEntry( Entry.hrs, -6.00 ) ;
			}
		}
		{
			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
			{
				ValuesForAUserType pvValues = railValues.getByType(Type.PV) ;
				pvValues.setByEntry( Entry.km, -0.1 ) ;
				pvValues.setByEntry( Entry.hrs, -6.00 ) ;
			}
		}
	
		return economicValues ;
	}

}
