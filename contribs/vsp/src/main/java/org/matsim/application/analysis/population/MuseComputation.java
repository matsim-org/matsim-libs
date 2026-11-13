package org.matsim.application.analysis.population;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;

/**
 * MUSE = marginal utility of starting early.  In score space, only time structure, i.e. no direct disutilities of travelling etc.
 *
 * <p>The person's whole day is replayed through the run's scoring function, once as it was and once per destination
 * with only that destination starting a second earlier. The difference does not depend on when a scorer accounts for
 * an activity's utility, or on which other callbacks it uses.
 */
public class MuseComputation{
	// Use the scenario's bindings, including person/subpopulation-specific scoring behavior.
	@Inject private ScoringFunctionFactory scoringFunctionFactory;

	/** Entry point for plans that already belong to their original person, as in the KN comparison. */
	public void computeMuseForAllActs( Plan plan ){
		computeMuseForAllActs( plan.getPerson(), plan );
	}

	/** Experienced plans may have no person backpointer; use the original person to create scorers. */
	public void computeMuseForAllActs( Person person, Plan plan ){

		var trips = TripStructureUtils.getTrips( plan );
		double unchanged = trips.isEmpty() ? Double.NaN : ScoringReplay.score( scoringFunctionFactory, person, trips );
		double sumMuse_h = 0.;
		double cntMuse_h = 0.;
		for( var trip : trips ){
			Activity destination = trip.getDestinationActivity();
			if( destination.getStartTime().isUndefined() ){
				continue;
			}
			// A scoring experiment only: the start time is restored, and attributes stay on the activity.
			double start = destination.getStartTime().seconds();
			destination.setStartTime( start - 1 );
			double muse_h;
			try{
				// A one-second perturbation gives score/second; scale to the reported score/hour unit.
				muse_h = (ScoringReplay.score( scoringFunctionFactory, person, trips ) - unchanged) * 3600.;
			} finally{
				destination.setStartTime( start );
			}
			VttsAttributes.setMuse( destination, muse_h );
			sumMuse_h += muse_h;
			cntMuse_h++;
		}
		// Preserve KN's unweighted destination mean, including NaN when there are no destinations.
		// The dashboard analysis handles non-mobile and incomplete plans before calling MUSE.
		VttsAttributes.setMuse( plan, sumMuse_h / cntMuse_h );
	}
}
