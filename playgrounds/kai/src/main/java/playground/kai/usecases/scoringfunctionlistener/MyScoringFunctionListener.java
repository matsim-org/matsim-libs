package playground.kai.usecases.scoringfunctionlistener;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

class MyScoringFunctionListener {
	public void reportMoney(double d, Id<Person> personId ) {
		System.err.println( " id=" + personId + " money=" + d ) ;
	}
}