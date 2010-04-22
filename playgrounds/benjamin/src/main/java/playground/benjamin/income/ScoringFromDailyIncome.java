package playground.benjamin.income;

import org.matsim.core.scoring.interfaces.BasicScoring;

public class ScoringFromDailyIncome implements BasicScoring {

	private double score;
	private double betaIncomeCar;
	private double incomePerDay;

	@Override
	public void finish() {
		this.score += (betaIncomeCar * Math.log(this.incomePerDay));

	}

}
