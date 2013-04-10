package playground.sergioo.passivePlanning2012.core.population.decisionMakers.types;

import org.matsim.api.core.v01.Coord;

public interface LocationDecisionMaker extends DecisionMaker {

	//Methods
	public Coord decideLocation(double time);

}
