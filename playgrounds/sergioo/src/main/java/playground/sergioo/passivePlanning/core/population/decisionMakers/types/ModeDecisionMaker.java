package playground.sergioo.passivePlanning.core.population.decisionMakers.types;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;

public interface ModeDecisionMaker extends DecisionMaker {

	//Methods
	public void setLeg(Leg leg);
	public void setStartLink(Link startLink);
	public void setEndLink(Link endLink);
	public String decideMode();
	
}
