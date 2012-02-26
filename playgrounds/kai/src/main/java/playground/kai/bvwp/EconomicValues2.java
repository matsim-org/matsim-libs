package playground.kai.bvwp;

import playground.kai.bvwp.Values.Entry;
import playground.kai.bvwp.Values.Mode;
import playground.kai.bvwp.Values.Type;

/**
 * @author Ihab
 *
 */

class EconomicValues2 {

	static Values createEconomicValues1() {
		Values economicValues = new Values() ;
		{
			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
			{
				ValuesForAUserType pvValues = roadValues.getByType(Type.PV_NON_COMMERCIAL) ;
				pvValues.setByEntry( Entry.km, -0.0 ) ;
				pvValues.setByEntry( Entry.hrs, -18.00 ) ;
			}
		}
		{
			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
			{
				ValuesForAUserType pvValues = railValues.getByType(Type.PV_NON_COMMERCIAL) ;
				pvValues.setByEntry( Entry.km, -0.1 ) ;
				pvValues.setByEntry( Entry.hrs, -18.00 ) ;
			}
		}
	
		return economicValues ;
	}

}
