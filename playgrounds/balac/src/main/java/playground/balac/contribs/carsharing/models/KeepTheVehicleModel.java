package playground.balac.contribs.carsharing.models;

import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.core.gbl.MatsimRandom;

public class KeepTheVehicleModel  implements KeepingTheCarModel {

	@Override
	public boolean keepTheCarDuringNextActivity(double durationOfActivity, Person person, String type) {
		
		double cutofDuration = 1.0 * 3600.0;
		Random random = MatsimRandom.getRandom();

		if (durationOfActivity >= cutofDuration)
			return false;
		else {			
			
			return random.nextDouble() < ( Math.pow((durationOfActivity - cutofDuration) / cutofDuration, 2));
		}

	}

}
