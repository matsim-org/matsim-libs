package playground.sergioo.passivePlanning.core.population;

import org.matsim.api.core.v01.Id;
import org.matsim.core.population.ActivityImpl;

import playground.sergioo.passivePlanning.api.population.EmptyActivity;

public class EmptyActivityImpl extends ActivityImpl implements EmptyActivity {

	//Attributes

	//Constructors
	public EmptyActivityImpl() {
		super("empty", (Id)null);
	}

	//Methods

}
