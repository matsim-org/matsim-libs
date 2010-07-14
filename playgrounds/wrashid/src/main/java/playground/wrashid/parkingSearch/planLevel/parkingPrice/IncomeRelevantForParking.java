package playground.wrashid.parkingSearch.planLevel.parkingPrice;

import org.matsim.api.core.v01.Id;
import org.matsim.households.HouseholdsFactoryImpl;
import org.matsim.households.Income;
import org.matsim.households.Income.IncomePeriod;

public class IncomeRelevantForParking {

	public double getIncome(Id personId){
		HouseholdsFactoryImpl hfi=new HouseholdsFactoryImpl();
		Income income= hfi.createIncome(5000, IncomePeriod.month);
		return income.getIncome();
	}
	
}
