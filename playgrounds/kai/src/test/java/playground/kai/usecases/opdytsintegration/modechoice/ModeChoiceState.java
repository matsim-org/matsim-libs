package playground.kai.usecases.opdytsintegration.modechoice;

import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;
import opdytsintegration.MATSimState;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class ModeChoiceState extends MATSimState {

	ModeChoiceState(final Population population, final Vector vectorRepresentation) {
		super(population, vectorRepresentation);
	}

}
