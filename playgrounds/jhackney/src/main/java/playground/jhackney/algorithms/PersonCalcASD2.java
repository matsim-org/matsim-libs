package playground.jhackney.algorithms;

import org.matsim.api.core.v01.population.Person;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

import playground.jhackney.socialnetworks.algorithms.PersonCalculateActivitySpaces;
import edu.uci.ics.jung.statistics.StatisticalMoments;

public class PersonCalcASD2  extends AbstractPersonAlgorithm{
	PersonCalculateActivitySpaces pcasd1 = new PersonCalculateActivitySpaces();
	public StatisticalMoments smASD2 = new StatisticalMoments();

	private void addVal(double val){
		if (val != val) {
			// val is NaN
		}
		else {
			smASD2.accumulate(val);
		}
	}
	
	@Override
	public void run(Person person) {
		// TODO Auto-generated method stub
		double aSd2 = pcasd1.getPersonASD2(person.getSelectedPlan());
		System.out.println("#Result "+aSd2);
		addVal(aSd2);

	}

}
