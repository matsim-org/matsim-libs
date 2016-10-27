package saleem.stockholmscenario.teleportation.gaming;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public final class StockholmMoneyScoring implements SumScoringFunction.MoneyScoring {

	private double score;
	

	private final double marginalUtilityOfMoney;
	
	private Person person;

	public StockholmMoneyScoring(Person person, final CharyparNagelScoringParameters params) {
		this.marginalUtilityOfMoney = params.marginalUtilityOfMoney*GamingConstants.marginalutilityfactor;
		this.person=person;
	}

	public StockholmMoneyScoring(Person person, final double marginalUtilityOfMoney) {
		this.marginalUtilityOfMoney = marginalUtilityOfMoney*GamingConstants.marginalutilityfactor;
	}

	@Override
	public void addMoney(final double amount) {
		this.score += amount * this.marginalUtilityOfMoney ; // linear mapping of money to score
	}

	@Override
	public void finish() {
	}

	@Override
	public double getScore() {
		return this.score;
	}

}
