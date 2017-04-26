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
		//check if the activity brings a penalty if not a transporter is sued
		Activity a = (Activity) plan.getPlanElements().get(index + 1);
		
		String type = a.getType();
		
		if (type.startsWith("secondary") && currentLeg.getMode().equals("freefloating")) {
			
			if (((Boolean)plan.getPerson().getAttributes().getAttribute("bulky")))
				return "transporter";
			else
				return "car";
						
		}
		else
			return "car";		
	}
}
