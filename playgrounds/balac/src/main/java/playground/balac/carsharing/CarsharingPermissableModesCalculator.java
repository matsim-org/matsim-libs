package playground.balac.carsharing;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.population.algorithms.PermissibleModesCalculator;

public class CarsharingPermissableModesCalculator implements PermissibleModesCalculator{

	/**
	 * @param args
	 */
	
	@Override
	public Collection<String> getPermissibleModes(Plan plan) {
		ArrayList<String> modes = new ArrayList<String>();
		Person p = plan.getPerson();
		modes.add("bike");
		modes.add("walk");
		modes.add("pt");
		if (PersonImpl.getLicense(p).equals( "yes" ) && !PersonImpl.getCarAvail(p).equals( "never" ))
			modes.add("car");
		//if (p.getTravelcards() != null)
		//	if (p.getLicense() == "yes" && p.getTravelcards().contains("ch-HT-mobility"))
		
		//		modes.add("carsharing");
		
		return modes;
	}

}
