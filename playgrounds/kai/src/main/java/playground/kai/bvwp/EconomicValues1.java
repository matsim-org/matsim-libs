package playground.kai.bvwp;

import playground.kai.bvwp.Values.Entry;
import playground.kai.bvwp.Values.Mode;
import playground.kai.bvwp.Values.Type;

class EconomicValues1 {

	static Values createEconomicValues1() {
		Values economicValues = new Values() ;
		{
			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
			{
				ValuesForAUserType pvValues = roadValues.getByType(Type.PV) ;
				pvValues.setByEntry( Entry.km, -0.23 ) ;
				pvValues.setByEntry( Entry.hrs, -5.00 ) ;
			}
			{
				ValuesForAUserType gvValues = roadValues.getByType(Type.GV) ;
				gvValues.setByEntry( Entry.km, -1.00 ) ;
				gvValues.setByEntry( Entry.hrs, -0.00 ) ;
			}
		}
		{
			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
			{
				ValuesForAUserType pvValues = railValues.getByType(Type.PV) ;
				pvValues.setByEntry( Entry.km, -0.1 ) ;
				pvValues.setByEntry( Entry.hrs, -5.00 ) ;
			}
			{
				ValuesForAUserType gvValues = railValues.getByType(Type.GV) ;
				gvValues.setByEntry( Entry.km, -0.1 ) ;
				gvValues.setByEntry( Entry.hrs, -0.00 ) ;
			}
		}
	
		return economicValues ;
	}

}
