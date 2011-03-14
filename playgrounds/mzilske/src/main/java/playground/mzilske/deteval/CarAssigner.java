package playground.mzilske.deteval;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.Vehicles;

public class CarAssigner implements Runnable {

	private static final VehicleType defaultType = new VehicleTypeImpl(new IdImpl("default-vehicle-Type"));
	private Population population;
	private Vehicles vehicles;

	public CarAssigner(Population population, Vehicles vehicles) {
		this.population = population;
		this.vehicles = vehicles;
	}

	@Override
	public void run() {
		for (Person person : population.getPersons().values()) {
			if (wantsCar(person)) {
				createCarWithPersonId(person);;
			}
		}
	}

	public static boolean wantsCar(Person person) {
		if (person.getPlans().size() == 1) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Activity) {
						
					}
					if (planElement instanceof Leg) {
						Leg leg = (Leg) planElement;
						if (TransportMode.car.equals(leg.getMode())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private void createCarWithPersonId(Person person) {
		Id vehicleId = person.getId();
		Vehicle vehicle = vehicles.getFactory().createVehicle(vehicleId, defaultType);
		vehicles.getVehicles().put(vehicleId, vehicle);
	}

}
