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
				ValuesForAUserType gvValues = roadValues.getByType(Type.GV) ;
				gvValues.setByEntry( Entry.km, -0.00 ) ;
				gvValues.setByEntry( Entry.hrs, -0.00 ) ;
			}
		}
		{
			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
			{
				ValuesForAUserType gvValues = railValues.getByType(Type.GV) ;
				gvValues.setByEntry( Entry.km, -5. ) ;
				gvValues.setByEntry( Entry.hrs, -0.00 ) ;
			}
		}
	
		return economicValues ;
	}

}
