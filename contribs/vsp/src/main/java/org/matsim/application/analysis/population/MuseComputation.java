package org.matsim.application.analysis.population;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.ExcludeStageActivities;

/**
 * MUSE = marginal utility of starting early.  In score space, only time structure, i.e. no direct disutilities of travelling etc.
 */
class MuseComputation{
	@Inject private ScoringFunctionFactory scoringFunctionFactory;
	@Inject private PopulationFactory pf;

	void computeMuseForAllActs( Plan plan ){
		// yy we need the person to get the scoring function.  However, how do we know which plan to use?  Could provide the plan instead; person is available as backpointer.

		// compute MUSE for rule-of-half:
		ScoringFunction sfNormal = scoringFunctionFactory.createNewScoringFunction( plan.getPerson() );
		ScoringFunction sfEarly = scoringFunctionFactory.createNewScoringFunction( plan.getPerson() );
		MuseComputationForOneAct museComputation = new MuseComputationForOneAct( sfNormal, sfEarly );
		double sumMuse_h = 0.;
		double cntMuse_h = 0.;
		for( Activity act : TripStructureUtils.getActivities( plan, ExcludeStageActivities ) ){
			// Ihab-style MarginalSumScoringFct computation but w/o leg:
			if( act.getStartTime().isUndefined() ){
				// handle the first activity of the plan:
				sfNormal.handleActivity( act );
				sfEarly.handleActivity( act );
				// (the standard scoring function only memorizes it here, and scores it together with the evening activity.  It needs to
				// arrive first nevertheless: with typical durations from the person (scoring.typicalDurationSource=personAttribute, as in
				// matsim-berlin v7.2), the scoring pairs the activities with the main activities of the selected plan in the order in which they arrive.)
			} else{
				sumMuse_h += museComputation.computeMUSE_h( act, pf, sfNormal.getScore(), sfEarly.getScore() );
				// (for the evening activity, which is started once at the normal time and once one second early, this includes the score of
				// the first activity of the plan)
				cntMuse_h++;
			}
		}
		AddVttsEtcToActivities.setMUSE_h( plan, sumMuse_h / cntMuse_h );
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

			// construct an activity that starts one second early, and score that one in sfEarly:
			Activity earlyActivity = pf.createActivityFromLinkId( act.getType(), act.getLinkId() );
			PopulationUtils.copyFromTo( act, earlyActivity );
			earlyActivity.setStartTime( earlyActivity.getStartTime().seconds() - 1 );
			sfEarly.handleActivity( earlyActivity );

			// no finish() here: the standard scoring function has the score of the activity already, and its finish() would score the
			// memorized first activity of the plan as a morning activity, once more each time it is called before the evening activity.

			double scoreDiffNormal = sfNormal.getScore() - scoreNormalBefore;
			double scoreDiffEarly = sfEarly.getScore() - scoreEarlyBefore;

			final double muse_h = (scoreDiffEarly - scoreDiffNormal) * 3600.;

			AddVttsEtcToActivities.setMUSE_h( act, muse_h );

			return muse_h;
		}
	}

}
