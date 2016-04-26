package playground.singapore.springcalibration.run.replanning;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.population.algorithms.PermissibleModesCalculator;

public class SingaporePermissibleModesCalculator implements PermissibleModesCalculator {
	
	private Population population;
	final String[] availableModes;
	
	public SingaporePermissibleModesCalculator(Population population, final String[] availableModes) {
		this.population = population;
		this.availableModes = availableModes;
	}
	

	@Override
	public List<String> getPermissibleModes(Plan plan) {
		
		List<String> permissibleModes = new LinkedList<String>(Arrays.asList(availableModes.clone()));
						
		String carAvail = (String) population.getPersonAttributes().getAttribute(plan.getPerson().getId().toString(), "car");
		String license = (String) population.getPersonAttributes().getAttribute(plan.getPerson().getId().toString(), "license");
		
				
		// as defined only people with license and car are allowed to use car
		if ("0".equals(carAvail) || "0".equals(license)) {
			permissibleModes.remove(TransportMode.car);
		}
		if ("0".equals(carAvail)) {
			permissibleModes.remove("passenger");
		}
	
		String ageStr = (String) population.getPersonAttributes().getAttribute(plan.getPerson().getId().toString(), "age");
		// if there is no age given, e.g., for freight agents
		int age = 25;	
		String cleanedAge = ageStr.replace("age", "");
		cleanedAge = cleanedAge.replace("up", "");
		if (ageStr != null) age = Integer.parseInt(cleanedAge);
		if (age < 20) permissibleModes.remove(TransportMode.other);
		if (age > 20) permissibleModes.remove("schoolbus");
		
		return permissibleModes;
	}

}
