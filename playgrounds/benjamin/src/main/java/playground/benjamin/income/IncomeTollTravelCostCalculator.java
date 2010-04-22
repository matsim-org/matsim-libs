package playground.benjamin.income;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.households.Income;
import org.matsim.households.PersonHouseholdMapping;
import org.matsim.households.Income.IncomePeriod;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.TollTravelCostCalculator;

public class IncomeTollTravelCostCalculator implements PersonalizableTravelCost {

	public class NullTravelCostCalculator implements PersonalizableTravelCost {

		@Override
		public void setPerson(Person person) {
			
		}

		@Override
		public double getLinkTravelCost(Link link, double time) {
			return 0;
		}

	}

	private static final double betaIncomeCar = 4.58;
	private TollTravelCostCalculator tollTravelCostCalculator;
	private double incomePerDay;
	private PersonHouseholdMapping hhdb;
	
	public IncomeTollTravelCostCalculator(PersonHouseholdMapping hhdb, RoadPricingScheme scheme) {
		this.hhdb = hhdb;
		PersonalizableTravelCost nullTravelCostCalculator = new NullTravelCostCalculator();
		this.tollTravelCostCalculator = new TollTravelCostCalculator(nullTravelCostCalculator , scheme);
	}

	@Override
	public void setPerson(Person person) {
		this.incomePerDay = getHouseholdIncomePerDay(person, hhdb);
	}

	@Override
	public double getLinkTravelCost(Link link, double time) {
		double amount = tollTravelCostCalculator.getLinkTravelCost(link, time);
		return (betaIncomeCar / incomePerDay) * amount;
	}

	private double getHouseholdIncomePerDay(Person person, PersonHouseholdMapping hhdb) {
		Income income = hhdb.getHousehold(person.getId()).getIncome();
		double incomePerDay = this.calculateIncomePerDay(income);
		if (Double.isNaN(incomePerDay)){
			throw new IllegalStateException("cannot calculate income for person: " + person.getId());
		}
		return incomePerDay;
	}

	private double calculateIncomePerDay(Income income) {
		if (income.getIncomePeriod().equals(IncomePeriod.year)) {
			double incomePerDay = income.getIncome() / 240;
			return incomePerDay;
		} else {
			throw new UnsupportedOperationException("Can't calculate income per day");
		}
	}
	
}
