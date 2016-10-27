package cba;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import floetteroed.utilities.Units;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TourSequence {

	final List<Tour> tours = new ArrayList<>();

	TourSequence() {
	}

	List<Tour.Act> getTourPurposes() {
		final List<Tour.Act> purposes = new ArrayList<>(this.tours.size());
		for (Tour tour : this.tours) {
			purposes.add(tour.act);
		}
		return purposes;
	}

	synchronized Plan asPlan(final Scenario scenario, final Id<Link> homeLinkId, final Person person) {

		final Plan result = scenario.getPopulation().getFactory().createPlan();

		/*-
		 * Initialize within uniform departure times. To ensure uniformity also for the 
		 * 24h wrap-around, set the first departure to half the departure time interval.
		 * 
		 * On tour:   |-------- dpt1a --------------- dpt1b -------|  => 24h / 2
		 * 
		 * Two tours: |-- dpt1a ---- dpt1b ---- dpt2a ---- dpt2b --|  => 24h / 4
		 * 
		 * N tours: 24h / (2N).
		 */
		final int dptTimeInc_s = (int) (Units.S_PER_D / (2 * this.tours.size()));
		int dptTime_s = dptTimeInc_s / 2;

		for (Tour tour : this.tours) {

			final Activity home = scenario.getPopulation().getFactory()
					.createActivityFromLinkId(Tour.Act.home.toString(), homeLinkId);
			home.setEndTime(dptTime_s);
			// home.setEndTime(dptTimes_s.removeFirst());
			result.addActivity(home);
			dptTime_s += dptTimeInc_s;

			result.addLeg(scenario.getPopulation().getFactory().createLeg(tour.mode.toString()));

			final Activity tourAct = scenario.getPopulation().getFactory().createActivityFromLinkId(tour.act.toString(),
					tour.destination.getId());
			tourAct.setEndTime(dptTime_s);
			result.addActivity(tourAct);
			dptTime_s += dptTimeInc_s;

			result.addLeg(scenario.getPopulation().getFactory().createLeg(tour.mode.toString()));
		}

		result.addActivity(
				scenario.getPopulation().getFactory().createActivityFromLinkId(Tour.Act.home.toString(), homeLinkId));
		result.setPerson(person);

		return result;
	}
}
