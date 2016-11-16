package playground.balac.contribs.carsharing.models;

import java.util.Random;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.carsharing.models.ChooseVehicleType;
import org.matsim.core.gbl.MatsimRandom;

public class VehicleTypeChoice implements ChooseVehicleType {

	@Override
	public String getPreferredVehicleType(Plan plan, Leg currentLeg) {

		
		int index = plan.getPlanElements().indexOf(currentLeg);
		
		Activity a = (Activity) plan.getPlanElements().get(index + 1);
		
		String type = a.getType();
		
		Random random = MatsimRandom.getRandom();
		if (type.startsWith("secondary") && currentLeg.getMode().equals("freefloating")) {
			if (random.nextDouble() < 0.1)
				return "transporter";
			else
				return "car";
						
		}
		else
			return "car";		
	}
}
