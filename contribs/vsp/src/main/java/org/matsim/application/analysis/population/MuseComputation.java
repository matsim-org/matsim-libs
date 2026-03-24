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
		Activity firstActivityOfPlan = null;
		double sumMuse_h = 0.;
		double cntMuse_h = 0.;
		for( Activity act : TripStructureUtils.getActivities( plan, ExcludeStageActivities ) ){
			// Ihab-style MarginalSumScoringFct computation but w/o leg:
			if( act.getStartTime().isDefined() && act.getEndTime().isDefined() ){
				double scoreNormalBefore = sfNormal.getScore();
				double scoreEarlyBefore = sfEarly.getScore();
				sumMuse_h += museComputation.computeMUSE_h( act, pf, scoreNormalBefore, scoreEarlyBefore );
				cntMuse_h++;
			} else if( act.getStartTime().isUndefined() ){
				firstActivityOfPlan = act;
			} else{
				Gbl.assertIf( act.getEndTime().isUndefined() );

				double scoreNormalBefore = sfNormal.getScore();
				double scoreEarlyBefore = sfEarly.getScore();

				// handle the first activity of the plan:
				sfNormal.handleActivity( firstActivityOfPlan );
				sfEarly.handleActivity( firstActivityOfPlan );
				// (the standard scoring function treats an activity without startTime as firstActivityOfPlan.  no matter if it arrives in correct sequence or not, as long as
				// it arrives before the evening activity)

				// handle the last activity of the plan:
				sumMuse_h += museComputation.computeMUSE_h( act, pf, scoreNormalBefore, scoreEarlyBefore );
				// ("act" is now the evening activity; will be started once at the normal time and once one second early)

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

			// finish the scoring computation (this can be multiple times (or so I hope)):
			sfNormal.finish();
			sfEarly.finish();

			double scoreDiffNormal = sfNormal.getScore() - scoreNormalBefore;
			double scoreDiffEarly = sfEarly.getScore() - scoreEarlyBefore;

			final double muse_h = (scoreDiffEarly - scoreDiffNormal) * 3600.;

			AddVttsEtcToActivities.setMUSE_h( act, muse_h );

			return muse_h;
		}
	}

}
