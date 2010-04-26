package playground.benjamin.income;

import org.matsim.core.scoring.interfaces.BasicScoring;

public class ScoringFromDailyIncome implements BasicScoring {

/*	in order to convert utility units into money terms, this parameter has to be equal
	to the one in ScoringFromLeg, ScoringFromToll and other money related parts of the scoring function.
	"Car" in the parameter name is not relevant for the same reason.*/
	private static double betaIncomeCar = 4.58;
	
	private double incomePerDay;

	public ScoringFromDailyIncome(double householdIncomePerDay) {
		this.incomePerDay = householdIncomePerDay;
	}


	@Override
	public void finish() {

	}

	@Override
	public double getScore() {
		return betaIncomeCar * Math.log(this.incomePerDay);
	}

	@Override
	public void reset() {

	}


}
