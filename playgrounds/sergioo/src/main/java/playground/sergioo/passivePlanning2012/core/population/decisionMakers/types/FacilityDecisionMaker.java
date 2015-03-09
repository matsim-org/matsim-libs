package playground.sergioo.passivePlanning2012.core.population.decisionMakers.types;

import org.matsim.api.core.v01.Coord;
import org.matsim.facilities.ActivityFacility;

public interface FacilityDecisionMaker extends DecisionMaker {

	//Methods
	public ActivityFacility decideFacility(double time, Coord location, String typeOfActivity);

}
