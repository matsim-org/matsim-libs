package playground.pbouman.agentproperties.scoring;

import java.util.Map;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.pbouman.agentproperties.ActivityProperties;
import playground.pbouman.agentproperties.AgentProperties;
import playground.pbouman.agentproperties.TimePreferences;

public class AgentPropertiesScoringFunctionFactory implements ScoringFunctionFactory
{
	private Map<String,AgentProperties> properties;

	public AgentPropertiesScoringFunctionFactory(Map<String,AgentProperties> props)
	{
		properties = props;
	}
	
	@Override
	public ScoringFunction createNewScoringFunction(Person person)
	{
		return createNewScoringFunction(person.getId().toString());
	}

	private ScoringFunction createNewScoringFunction(String id)
	{
		final AgentProperties props = properties.get(id);
		
		return new ScoringFunction()
		{
			private double score = 0;
			private double moneyRemaining = props.getMoneyMaximum();

			@Override
			public void handleActivity(Activity activity)
			{
				
				if (activity.getStartTime() != Double.NEGATIVE_INFINITY && activity.getEndTime() != Double.NEGATIVE_INFINITY && props.getActivityNames().contains(activity.getType()))
				{
					ActivityProperties actProp = props.getActivityProperties(activity.getType());
					TimePreferences prefs = actProp.getTimePreferences();
					
					double startTime = Math.max(activity.getStartTime(), actProp.getAvailableFrom());
					double endTime = Math.min(activity.getEndTime(), actProp.getAvailableTo());
					
					double startDiff = Math.abs(prefs.getStartMean() - startTime);
					double endDiff = Math.abs(prefs.getEndMean() - endTime);
					double duration = endTime - startTime;
					double durationDiff = Math.abs(prefs.getDurationMean() - duration);
					
					double startUtility = 0;
					if (prefs.getStartStdDev() != 0d && prefs.getStartStdDev() != Double.POSITIVE_INFINITY)
						startUtility = prefs.getStartDevUtility() * Math.max(0, (prefs.getStartStdDev() - startDiff) / prefs.getStartStdDev());
					double endUtility = 0;
					if (prefs.getEndStdDev() != 0d && prefs.getEndStdDev() != Double.POSITIVE_INFINITY)
						endUtility = prefs.getEndDevUtility() * Math.max(0, (prefs.getEndStdDev() - endDiff) / prefs.getEndStdDev());
					double durationUtility = prefs.getDurationUtility();
					
					if (durationDiff > prefs.getDurationStdDev())
					{
						 durationUtility *= Math.log(prefs.getDurationStdDev()) / Math.log(durationDiff);
					}
					
					double utility = startUtility + endUtility + durationUtility;
					
					score += (utility * (duration/3600));
				}
				else if (activity.getType().equals("home"))
				{
					if (Double.isInfinite(activity.getStartTime()) && !Double.isInfinite(activity.getEndTime()))
					{
						// Morning case
						double leaveHome = Math.min(activity.getEndTime(), props.getMorningEnd());
						double utility = props.getHomeUtility() * (Math.log(leaveHome) / Math.log(props.getMorningEnd()));
						score += utility;
					}
					if (!Double.isInfinite(activity.getStartTime()) && Double.isInfinite(activity.getEndTime()))
					{
						// Evening case
						double arriveHome = Math.max(activity.getStartTime(), props.getEveningStart());
						double utility = props.getHomeUtility() * (Math.log(props.getEveningStart()) / Math.log(arriveHome));
						score += utility;
					}
					
				}
			}

			@Override
			public void handleLeg(Leg leg)
			{
				// For now, we ignore the "crowdedness" factor...
				double utility = props.getTravelUtility(leg.getMode());
				score += (utility * (leg.getTravelTime() / 3600));
			}

			@Override
			public void agentStuck(double time)
			{
				score -= 100;
			}

			@Override
			public void addMoney(double amount)
			{
				double utility = props.getMoneyUtility();
				score += (utility*amount);
				if (moneyRemaining >= 0 && moneyRemaining - amount < 0)
					score -= 100;
				moneyRemaining -= amount;
			}

			@Override
			public void finish()
			{
				
			}

			@Override
			public double getScore()
			{
				return score;
			}

			@Override
			public void handleEvent(Event event) {
				// TODO Auto-generated method stub
				
			} 
		};
	}
	
}
