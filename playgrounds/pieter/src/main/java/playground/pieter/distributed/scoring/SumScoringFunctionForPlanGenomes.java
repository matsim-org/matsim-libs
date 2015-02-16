package playground.pieter.distributed.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.misc.Time;
import playground.pieter.distributed.plans.PersonForPlanGenomes;
import playground.pieter.distributed.plans.PlanGenome;

import java.util.ArrayList;

/**
 * Created by fouriep on 2/9/15.
 */
public class SumScoringFunctionForPlanGenomes implements ScoringFunction {

    static final double scoreRecordingThreshold = 0.0001;
    private static Logger log = Logger.getLogger(SumScoringFunction.class);
    private final PersonForPlanGenomes person;
    private final PlanGenome planGenome;
    private ArrayList<SumScoringFunction.BasicScoring> basicScoringFunctions = new ArrayList<SumScoringFunction.BasicScoring>();
    private ArrayList<SumScoringFunction.ActivityScoring> activityScoringFunctions = new ArrayList<SumScoringFunction.ActivityScoring>();
    private ArrayList<SumScoringFunction.MoneyScoring> moneyScoringFunctions = new ArrayList<SumScoringFunction.MoneyScoring>();
    private ArrayList<SumScoringFunction.LegScoring> legScoringFunctions = new ArrayList<SumScoringFunction.LegScoring>();
    private ArrayList<SumScoringFunction.AgentStuckScoring> agentStuckScoringFunctions = new ArrayList<SumScoringFunction.AgentStuckScoring>();
    private ArrayList<SumScoringFunction.ArbitraryEventScoring> arbitraryEventScoringFunctions = new ArrayList<SumScoringFunction.ArbitraryEventScoring>();

    public SumScoringFunctionForPlanGenomes(Person person) {
        super();
        this.person = (PersonForPlanGenomes) person;
        planGenome = (PlanGenome) person.getSelectedPlan();
    }

    @Override
    public final void handleActivity(Activity activity) {
        double startScore;
        double contribution;
        double startTime = activity.getStartTime();
        double endTime = activity.getEndTime();
        if (startTime == Time.UNDEFINED_TIME && endTime != Time.UNDEFINED_TIME) {
            for (SumScoringFunction.ActivityScoring activityScoringFunction : activityScoringFunctions) {
                startScore = activityScoringFunction.getScore();
                activityScoringFunction.handleFirstActivity(activity);
                contribution = activityScoringFunction.getScore() - startScore;
                if (Math.abs(contribution) > scoreRecordingThreshold) {
                    planGenome.addScoreComponent(ScoreComponentType.Activity, contribution, activity.getType()+"_first");
                }
            }
        } else if (startTime != Time.UNDEFINED_TIME && endTime != Time.UNDEFINED_TIME) {
            for (SumScoringFunction.ActivityScoring activityScoringFunction : activityScoringFunctions) {
                startScore = activityScoringFunction.getScore();
                activityScoringFunction.handleActivity(activity);
                contribution = activityScoringFunction.getScore() - startScore;
                if (Math.abs(contribution) > scoreRecordingThreshold) {
                    planGenome.addScoreComponent(ScoreComponentType.Activity, contribution, activity.getType());
                }
            }
        } else if (startTime != Time.UNDEFINED_TIME && endTime == Time.UNDEFINED_TIME) {
            for (SumScoringFunction.ActivityScoring activityScoringFunction : activityScoringFunctions) {
                startScore = activityScoringFunction.getScore();
                activityScoringFunction.handleLastActivity(activity);
                contribution = activityScoringFunction.getScore() - startScore;
                if (Math.abs(contribution) > scoreRecordingThreshold) {
                    planGenome.addScoreComponent(ScoreComponentType.Activity, contribution, activity.getType()+"_last");
                }
            }
        } else {
            throw new RuntimeException("Trying to score an activity without start or end time. Should not happen.");
        }
    }

    @Override
    public final void handleLeg(Leg leg) {
        for (SumScoringFunction.LegScoring legScoringFunction : legScoringFunctions) {
            String type = legScoringFunction.getClass().getSimpleName();
            double startScore = legScoringFunction.getScore();
            legScoringFunction.handleLeg(leg);
            double contribution = legScoringFunction.getScore() - startScore;
            if (Math.abs(contribution) > scoreRecordingThreshold) {
                if (type.contains("Leg"))
                    planGenome.addScoreComponent(ScoreComponentType.Leg, contribution, leg.getMode());
                if (type.contains("Fare"))
                    planGenome.addScoreComponent(ScoreComponentType.Fare, contribution, "fare");
            }
        }
    }

    @Override
    public void addMoney(double amount) {
        for (SumScoringFunction.MoneyScoring moneyScoringFunction : moneyScoringFunctions) {
            double startScore = moneyScoringFunction.getScore();
            moneyScoringFunction.addMoney(amount);
            double contribution = moneyScoringFunction.getScore() - startScore;
            if (Math.abs(contribution) > scoreRecordingThreshold) {
                planGenome.addScoreComponent(ScoreComponentType.Money, contribution, "money");
            }
        }
    }

    @Override
    public void agentStuck(double time) {
        for (SumScoringFunction.AgentStuckScoring agentStuckScoringFunction : agentStuckScoringFunctions) {
            double startScore = agentStuckScoringFunction.getScore();
            agentStuckScoringFunction.agentStuck(time);
            double contribution = agentStuckScoringFunction.getScore() - startScore;
            if (Math.abs(contribution) > scoreRecordingThreshold) {
                planGenome.addScoreComponent(ScoreComponentType.Stuck, contribution, "stuck");
            }
        }
    }

    @Override
    public void handleEvent(Event event) {
        for (SumScoringFunction.ArbitraryEventScoring eventScoringFunction : this.arbitraryEventScoringFunctions) {
            eventScoringFunction.handleEvent(event);
        }
    }

    @Override
    public void finish() {
        for (SumScoringFunction.BasicScoring basicScoringFunction : basicScoringFunctions) {
            basicScoringFunction.finish();
        }
    }

    /**
     * Add the score of all functions.
     */
    @Override
    public double getScore() {
        double score = 0.0;
        for (SumScoringFunction.BasicScoring basicScoringFunction : basicScoringFunctions) {
            double contribution = basicScoringFunction.getScore();
            score += contribution;
        }
        return score;
    }

    public void addScoringFunction(SumScoringFunction.BasicScoring scoringFunction) {
        basicScoringFunctions.add(scoringFunction);

        if (scoringFunction instanceof SumScoringFunction.ActivityScoring) {
            activityScoringFunctions.add((SumScoringFunction.ActivityScoring) scoringFunction);
        }

        if (scoringFunction instanceof SumScoringFunction.AgentStuckScoring) {
            agentStuckScoringFunctions.add((SumScoringFunction.AgentStuckScoring) scoringFunction);
        }

        if (scoringFunction instanceof SumScoringFunction.LegScoring) {
            legScoringFunctions.add((SumScoringFunction.LegScoring) scoringFunction);
        }

        if (scoringFunction instanceof SumScoringFunction.MoneyScoring) {
            moneyScoringFunctions.add((SumScoringFunction.MoneyScoring) scoringFunction);
        }

        if (scoringFunction instanceof SumScoringFunction.ArbitraryEventScoring) {
            this.arbitraryEventScoringFunctions.add((SumScoringFunction.ArbitraryEventScoring) scoringFunction);
        }

    }


}
