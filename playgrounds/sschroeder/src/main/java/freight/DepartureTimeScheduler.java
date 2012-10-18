package freight;

import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.freight.carrier.CarrierFactory;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyModule;
import org.matsim.core.gbl.MatsimRandom;

public class DepartureTimeScheduler implements CarrierPlanStrategyModule {

	@Override
	public void handlePlan(CarrierPlan carrierPlan) {
		List<ScheduledTour> sTours = new ArrayList<ScheduledTour>();
		for (ScheduledTour sTour : carrierPlan.getScheduledTours()) {
			ScheduledTour newScheduledTour = new CarrierFactory()
					.createScheduledTour(sTour.getTour(), sTour.getVehicle(),
							sTour.getTour().getEarliestDeparture());
			sTours.add(newScheduledTour);
		}
		carrierPlan.getScheduledTours().clear();
		carrierPlan.getScheduledTours().addAll(sTours);
	}

	private double getRandomDepartureTime(double earliestDeparture,
			double latestDeparture) {
		return Math.round(earliestDeparture
				+ (latestDeparture - earliestDeparture)
				* MatsimRandom.getRandom().nextDouble());
	}

}
