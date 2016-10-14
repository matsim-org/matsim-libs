package cba;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provider;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class ChoiceModel {

	// -------------------- MEMBERS --------------------

	private final Scenario scenario;

	private final Provider<TripRouter> tripRouterProvider;

	private final Map<String, TravelTime> mode2travelTime;

	// private final List<TourSequence> alternatives;

	private final int maxTrials;

	private final int maxFailures;

	// -------------------- CONSTRUCTION --------------------

	// static List<TourSequence> newTourSeqAlternatives(final Scenario scenario)
	// {
	// /*
	// * CREATE ALL ALTERNATIVES ONCE.
	// */
	//
	// final List<TourSequence> tmpAlternatives = new ArrayList<>();
	//
	// // NO TOUR -- TODO special departure time case, omitted
	// // {
	// // final TourSequence alternative = new TourSequence();
	// // tmpAlternatives.add(alternative);
	// // }
	//
	// // ONE TOUR (WORK OR OTHER)
	// {
	// for (Link loc : scenario.getNetwork().getLinks().values()) {
	// for (Tour.Act act : new Tour.Act[] { Tour.Act.work, Tour.Act.other }) {
	// for (Tour.Mode mode : Tour.Mode.values()) {
	// final TourSequence alternative = new TourSequence();
	// alternative.tours.add(new Tour(loc, act, mode));
	// tmpAlternatives.add(alternative);
	// }
	// }
	// }
	// }
	//
	// // TWO TOURS (WORK, THEN OTHER)
	// {
	// for (Link workLoc : scenario.getNetwork().getLinks().values()) {
	// for (Tour.Mode workMode : Tour.Mode.values()) {
	// for (Link otherLoc : scenario.getNetwork().getLinks().values()) {
	// for (Tour.Mode otherMode : Tour.Mode.values()) {
	// final TourSequence alternative = new TourSequence();
	// alternative.tours.add(new Tour(workLoc, Tour.Act.work, workMode));
	// alternative.tours.add(new Tour(otherLoc, Tour.Act.other, otherMode));
	// tmpAlternatives.add(alternative);
	// }
	// }
	// }
	// }
	// }
	// return Collections.unmodifiableList(tmpAlternatives);
	// }

	private static List<Link> sampleLinks(final Scenario scenario, final int cnt) {
		final LinkedList<Link> fullList = new LinkedList<>(scenario.getNetwork().getLinks().values());
		final ArrayList<Link> result = new ArrayList<>(cnt);
		while (result.size() < cnt) {
			result.add(fullList.remove(MatsimRandom.getRandom().nextInt(fullList.size())));
		}
		return result;
	}

	// static List<TourSequence> newTourSeqAlternatives(final Scenario scenario)
	// {
	// final int linkCnt = scenario.getNetwork().getLinks().size();
	// return newTourSeqAlternatives(scenario, linkCnt, linkCnt);
	// }

	static List<TourSequence> newTourSeqAlternatives(final Scenario scenario, final int workLocCnt,
			final int otherLocCnt, final boolean carAvailable) {

		final List<Link> workLocs = sampleLinks(scenario, workLocCnt);
		final List<Link> otherLocs = sampleLinks(scenario, otherLocCnt);

		final Tour.Mode[] availableModes;
		if (carAvailable) {
			availableModes = new Tour.Mode[] { Tour.Mode.car, Tour.Mode.pt };
		} else {
			availableModes = new Tour.Mode[] { Tour.Mode.pt };
		}

		/*
		 * CREATE ALL ALTERNATIVES ONCE.
		 */

		final List<TourSequence> tmpAlternatives = new ArrayList<>();

		// NO TOUR -- TODO special departure time case, omitted
		// {
		// final TourSequence alternative = new TourSequence();
		// tmpAlternatives.add(alternative);
		// }

		// >>> TODO HACK >>>
		// ONE TOUR (WORK OR OTHER)
		// {
		// for (Link loc : workLocs) {
		// // for (Link loc : scenario.getNetwork().getLinks().values()) {
		// for (Tour.Act act : new Tour.Act[] { Tour.Act.work}) {
		// for (Tour.Mode mode : availableModes) {
		// final TourSequence alternative = new TourSequence();
		// alternative.tours.add(new Tour(loc, act, mode));
		// tmpAlternatives.add(alternative);
		// }
		// }
		// }
		// }
		// <<< TODO HACK <<<

		// ONE TOUR (WORK OR OTHER)
		{
			for (Link loc : workLocs) {
				// for (Link loc : scenario.getNetwork().getLinks().values()) {
				for (Tour.Act act : new Tour.Act[] { Tour.Act.work, Tour.Act.other }) {
					for (Tour.Mode mode : availableModes) {
						final TourSequence alternative = new TourSequence();
						alternative.tours.add(new Tour(loc, act, mode));
						tmpAlternatives.add(alternative);
					}
				}
			}
		}

		// TWO TOURS (WORK, THEN OTHER)
		{
			for (Link workLoc : workLocs) {
				// for (Link workLoc :
				// scenario.getNetwork().getLinks().values()) {
				for (Tour.Mode workMode : availableModes) {
					for (Link otherLoc : otherLocs) {
						// for (Link otherLoc :
						// scenario.getNetwork().getLinks().values()) {
						for (Tour.Mode otherMode : availableModes) {
							final TourSequence alternative = new TourSequence();
							alternative.tours.add(new Tour(workLoc, Tour.Act.work, workMode));
							alternative.tours.add(new Tour(otherLoc, Tour.Act.other, otherMode));
							tmpAlternatives.add(alternative);
						}
					}
				}
			}
		}

		return Collections.unmodifiableList(tmpAlternatives);
	}

	ChoiceModel(final Scenario scenario, final Provider<TripRouter> tripRouterProvider,
			final Map<String, TravelTime> mode2travelTime, final int maxTrials, final int maxFailures) {
		this.scenario = scenario;
		this.tripRouterProvider = tripRouterProvider;
		this.mode2travelTime = mode2travelTime;
		this.maxTrials = maxTrials;
		this.maxFailures = maxFailures;
		// this.alternatives = newTourSeqAlternatives(scenario);

	}

	// -------------------- IMPLEMENTATION --------------------

	ChoiceRunner newChoiceRunner(final Link homeLoc, final Person person, final int workAlts, final int otherAlts,
			final boolean carAvailable) {
		return new ChoiceRunner(this.scenario, this.tripRouterProvider, this.mode2travelTime, homeLoc, person,
				newTourSeqAlternatives(this.scenario, workAlts, otherAlts, carAvailable), this.maxTrials,
				this.maxFailures);
	}

	static Plan selectUniformly(final Link homeLoc, final Person person,
			final List<TourSequence> tourSequenceAlternatives, final Scenario scenario) {
		return tourSequenceAlternatives.get(MatsimRandom.getRandom().nextInt(tourSequenceAlternatives.size()))
				.asPlan(scenario, homeLoc.getId(), person);
	}
}
