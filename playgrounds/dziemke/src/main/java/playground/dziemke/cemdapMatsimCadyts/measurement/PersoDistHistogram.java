package playground.dziemke.cemdapMatsimCadyts.measurement;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.util.HashMap;

public interface PersoDistHistogram {
	HashMap<Id<Person>, Double> getDistances();
}
