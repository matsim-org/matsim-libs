package playground.wrashid.scoring;

import org.apache.log4j.Logger;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Route;
import org.matsim.population.ActUtilityParameters;
import org.matsim.scoring.CharyparNagelScoringFunction;
import org.matsim.scoring.CharyparNagelScoringParameters;
import org.matsim.utils.misc.Time;

import playground.wrashid.scoring.interfaces.BasicScoringFunction;
import playground.wrashid.scoring.interfaces.LegScoringFunction;
import playground.wrashid.scoring.interfaces.MoneyScoringFunction;

public class CharyparNagelMoneyScoringFunction implements MoneyScoringFunction, BasicScoringFunction {

	protected double score;

	private static final double INITIAL_SCORE = 0.0;

	/** The parameters used for scoring */
	protected final CharyparNagelScoringParameters params;

	public CharyparNagelMoneyScoringFunction(final CharyparNagelScoringParameters params) {
		this.params = params;
		this.reset();

	}

	public void reset() {
		this.score = INITIAL_SCORE;
	}

	public void addMoney(final double amount) {
		this.score += amount; // linear mapping of money to score
	}

	public void finish() {

	}

	public double getScore() {
		return this.score;
	}

}
