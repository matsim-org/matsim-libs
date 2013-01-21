package playground.sergioo.passivePlanning2012.core.population.decisionMakers;

import java.util.Map;

import org.matsim.api.core.v01.population.Activity;


public interface NeedActivity extends Activity {

	//Methods
	Map<NeedType, Double> getNeeds();

}
