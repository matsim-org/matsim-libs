package org.matsim.application.analysis.population;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.ExcludeStageActivities;

/**
 * MUSE = marginal utility of starting early.  In score space, only time structure, i.e. no direct disutilities of travelling etc.
 *
 * <p>Preserves the KN incremental calculation with two reused scoring functions. Activities are
 * delivered in plan order and finish() is called only after the final activity, so person-based
 * typical durations can be aligned with the selected plan. Scorers must expose intermediate scores.
 */
public class MuseComputation{
	// Use the scenario's bindings, including person/subpopulation-specific scoring behavior.
	@Inject private ScoringFunctionFactory scoringFunctionFactory;
	@Inject private PopulationFactory pf;

	/** Entry point for plans that already belong to their original person, as in the KN comparison. */
	public void computeMuseForAllActs( Plan plan ){
		computeMuseForAllActs( plan.getPerson(), plan );
	}

	/** Experienced plans may have no person backpointer; use the original person to create scorers. */
	public void computeMuseForAllActs( Person person, Plan plan ){

		// Keep one normal and one early scorer for the whole plan, as in the original KN code.
		// Their scores accumulate: each destination's MUSE uses the new score increments below.
		ScoringFunction sfNormal = scoringFunctionFactory.createNewScoringFunction( person );
		ScoringFunction sfEarly = scoringFunctionFactory.createNewScoringFunction( person );
		MuseComputationForOneAct museComputation = new MuseComputationForOneAct( sfNormal, sfEarly );
		double sumMuse_h = 0.;
		double cntMuse_h = 0.;
		// Stage interactions are not destinations in the mean. Legs are not scored here either:
		// MUSE contains only the activity effect; VttsAnalysis adds the direct travel-time effect.
		for( Activity act : TripStructureUtils.getActivities( plan, ExcludeStageActivities ) ){
			if( act.getStartTime().isDefined() && act.getEndTime().isDefined() ){
				// A normal intermediate activity: remember the cumulative scores before adding it.
				double scoreNormalBefore = sfNormal.getScore();
				double scoreEarlyBefore = sfEarly.getScore();
				sumMuse_h += museComputation.computeMUSE_h( act, pf, scoreNormalBefore, scoreEarlyBefore );
				cntMuse_h++;
			} else if( act.getStartTime().isUndefined() ){
				// The first activity is an origin, not an additional incoming-trip destination. Deliver it
				// unchanged to both scorers now: person-based typical durations require plan order.
				// The standard activity scorer keeps it for the later morning/evening combination.
				// A lone activity with neither time defined has no destinations and is not scored.
				if( act.getEndTime().isDefined() ){
					sfNormal.handleActivity( act );
					sfEarly.handleActivity( act );
				}
			} else{
				Gbl.assertIf( act.getEndTime().isUndefined() );

				double scoreNormalBefore = sfNormal.getScore();
				double scoreEarlyBefore = sfEarly.getScore();

				// Handle the final activity (any type, with an undefined end time):
				sumMuse_h += museComputation.computeMUSE_h( act, pf, scoreNormalBefore, scoreEarlyBefore );
				// It is started once at the normal time and once one second early.

				cntMuse_h++;
			}
		}
		// Preserve KN's unweighted destination mean, including NaN when there are no destinations.
		// The dashboard analysis handles non-mobile and incomplete plans before calling MUSE.
		VttsAttributes.setMuse( plan, sumMuse_h / cntMuse_h );
	}

	static class MuseComputationForOneAct{
		// yy not sure if this is a good design; it came out of refactoring. kai, mar'26

		private final ScoringFunction sfNormal;
		private final ScoringFunction sfEarly;

		MuseComputationForOneAct( ScoringFunction sfNormal, ScoringFunction sfEarly ){
			this.sfNormal = sfNormal;
			this.sfEarly = sfEarly;
		}

		double computeMUSE_h( Activity act, PopulationFactory pf, double scoreNormalBefore, double scoreEarlyBefore ){

			// normal handling of the activity:
			sfNormal.handleActivity( act );

			// Copy all activity fields and attributes, then change only the start time. The original
			// experienced activity and its end time remain unchanged by this scoring experiment.
			Activity earlyActivity = pf.createActivityFromLinkId( act.getType(), act.getLinkId() );
			PopulationUtils.copyFromTo( act, earlyActivity );
			earlyActivity.setStartTime( earlyActivity.getStartTime().seconds() - 1 );
			sfEarly.handleActivity( earlyActivity );

			// Finish once, after the final activity; its type need not be home. Finishing earlier would
			// prematurely score the stored morning activity before the overnight pair is complete.
			// Intermediate activities already contribute their scores through handleActivity().
			if( act.getEndTime().isUndefined() ){
				sfNormal.finish();
				sfEarly.finish();
			}

			// Subtract the earlier cumulative totals to isolate this destination's score increments.
			double scoreDiffNormal = sfNormal.getScore() - scoreNormalBefore;
			double scoreDiffEarly = sfEarly.getScore() - scoreEarlyBefore;

			// A one-second perturbation gives score/second; scale to the reported score/hour unit.
			final double muse_h = (scoreDiffEarly - scoreDiffNormal) * 3600.;

			VttsAttributes.setMuse( act, muse_h );

			return muse_h;
		}
	}

}
