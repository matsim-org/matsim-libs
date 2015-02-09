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

    private static Logger log = Logger.getLogger(SumScoringFunction.class);
    private final PersonForPlanGenomes person;
    private ArrayList<SumScoringFunction.BasicScoring> basicScoringFunctions = new ArrayList<SumScoringFunction.BasicScoring>();
    private ArrayList<SumScoringFunction.ActivityScoring> activityScoringFunctions = new ArrayList<SumScoringFunction.ActivityScoring>();
    private ArrayList<SumScoringFunction.MoneyScoring> moneyScoringFunctions = new ArrayList<SumScoringFunction.MoneyScoring>();
    private ArrayList<SumScoringFunction.LegScoring> legScoringFunctions = new ArrayList<SumScoringFunction.LegScoring>();
    private ArrayList<SumScoringFunction.AgentStuckScoring> agentStuckScoringFunctions = new ArrayList<SumScoringFunction.AgentStuckScoring>();
    private ArrayList<SumScoringFunction.ArbitraryEventScoring> arbitraryEventScoringFunctions = new ArrayList<SumScoringFunction.ArbitraryEventScoring>();

    public SumScoringFunctionForPlanGenomes(Person person) {
        super();
        this.person = (PersonForPlanGenomes) person;
    }

    @Override
    public final void handleActivity(Activity activity) {
        double startTime = activity.getStartTime();
        double endTime = activity.getEndTime();
        if (startTime == Time.UNDEFINED_TIME && endTime != Time.UNDEFINED_TIME) {
            for (SumScoringFunction.ActivityScoring activityScoringFunction : activityScoringFunctions) {
                activityScoringFunction.handleFirstActivity(activity);
            }
        } else if (startTime != Time.UNDEFINED_TIME && endTime != Time.UNDEFINED_TIME) {
            for (SumScoringFunction.ActivityScoring activityScoringFunction : activityScoringFunctions) {
                activityScoringFunction.handleActivity(activity);
            }
        } else if (startTime != Time.UNDEFINED_TIME && endTime == Time.UNDEFINED_TIME) {
            for (SumScoringFunction.ActivityScoring activityScoringFunction : activityScoringFunctions) {
                activityScoringFunction.handleLastActivity(activity);
            }
        } else {
            throw new RuntimeException("Trying to score an activity without start or end time. Should not happen.");
        }
    }

    @Override
    public final void handleLeg(Leg leg) {
        for (SumScoringFunction.LegScoring legScoringFunction : legScoringFunctions) {
            legScoringFunction.handleLeg(leg);
        }
    }

    @Override
    public void addMoney(double amount) {
        for (SumScoringFunction.MoneyScoring moneyScoringFunction : moneyScoringFunctions) {
            moneyScoringFunction.addMoney(amount);
        }
    }

    @Override
    public void agentStuck(double time) {
        for (SumScoringFunction.AgentStuckScoring agentStuckScoringFunction : agentStuckScoringFunctions) {
            agentStuckScoringFunction.agentStuck(time);
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
            String type = basicScoringFunction.getClass().getSimpleName();
            PlanGenome planGenome = (PlanGenome) person.getSelectedPlan();
            if (type.contains("Money"))
                planGenome.addScoreComponent(ScoreComponentType.Money, contribution);
            if (type.contains("Stuck"))
                planGenome.addScoreComponent(ScoreComponentType.Stuck, contribution);
            if (type.contains("Activity"))
                planGenome.addScoreComponent(ScoreComponentType.Activity, contribution);
            if (type.contains("Leg"))
                planGenome.addScoreComponent(ScoreComponentType.Leg, contribution);
            if (type.contains("Arbitrary"))
                planGenome.addScoreComponent(ScoreComponentType.Event, contribution);
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
