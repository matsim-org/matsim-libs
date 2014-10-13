package playground.wrashid.parkingSearch.planLevel.parkingPrice;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.households.HouseholdsFactoryImpl;
import org.matsim.households.Income;
import org.matsim.households.Income.IncomePeriod;

public class IncomeRelevantForParking {

	public double getIncome(Id<Person> personId){
		HouseholdsFactoryImpl hfi=new HouseholdsFactoryImpl();
		Income income= hfi.createIncome(5000, IncomePeriod.month);
		return income.getIncome();
	}
	
}
