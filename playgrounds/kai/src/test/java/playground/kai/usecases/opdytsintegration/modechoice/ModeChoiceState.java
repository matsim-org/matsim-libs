package playground.kai.usecases.opdytsintegration.modechoice;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;
import opdytsintegration.MATSimState;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class ModeChoiceState extends MATSimState {

	private Double betaPay;

	private Double betaAlloc;

	private Double avgScore;

	ModeChoiceState(final Population population, final Vector vectorRepresentation, final Double betaPay, final Double betaAlloc) {

		super(population, vectorRepresentation);

		this.betaPay = betaPay;
		this.betaAlloc = betaAlloc;

		double totalScore = 0.0;
		for (Person person : population.getPersons().values()) {
			if (person.getSelectedPlan() != null) {
				totalScore += person.getSelectedPlan().getScore();
			}
		}
		this.avgScore = totalScore / population.getPersons().size();
	}

	public double getAvgScore() {
		return this.avgScore;
	}

	public double getBetaPay() {
		return this.betaPay;
	}

	public double getBetaAlloc() {
		return this.betaAlloc;
	}

}
