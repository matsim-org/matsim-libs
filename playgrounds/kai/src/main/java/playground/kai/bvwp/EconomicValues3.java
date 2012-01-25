package playground.kai.bvwp;

import playground.kai.bvwp.Values.Entry;
import playground.kai.bvwp.Values.Mode;
import playground.kai.bvwp.Values.Type;

/**
 * @author Ihab
 *
 */

class EconomicValues3 {

	static Values createEconomicValues1() {
		Values economicValues = new Values() ;
		{
			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
			{
				ValuesForAUserType pvValues = roadValues.getByType(Type.GV) ;
				pvValues.setByEntry( Entry.km, -0.00 ) ;
				pvValues.setByEntry( Entry.hrs, -0.00 ) ;
			}
		}
		{
			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
			{
				ValuesForAUserType pvValues = railValues.getByType(Type.GV) ;
				pvValues.setByEntry( Entry.km, -5. ) ;
				pvValues.setByEntry( Entry.hrs, -0.00 ) ;
			}
		}
	
		return economicValues ;
	}

}
