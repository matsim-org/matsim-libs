package playground.balac.allcsmodestest.replanning;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.population.algorithms.PermissibleModesCalculator;

public class SubTourPermissableModesCalculator implements PermissibleModesCalculator{

	private final Scenario scenario;
	public SubTourPermissableModesCalculator(final Scenario scenario) {
		// TODO Auto-generated constructor stub
		this.scenario = scenario;
	}

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
	//	if (p.getLicense().equals( "yes" ) && p.getCarAvail() != null && !p.getCarAvail().equals( "never" )) 
			modes.add("car");
		
		 if (Boolean.parseBoolean(scenario.getConfig().getModule("TwoWayCarsharing").getParams().get("useTwoWayCarsharing"))
		
		&& Boolean.parseBoolean((String) scenario.getPopulation().getPersonAttributes().getAttribute(p.getId().toString(), "RT_CARD")))
		
				modes.add("twowaycarsharing");
		
		return modes;
	}

}
