package playground.pbouman.agentproperties;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class AgentProperties
{

	private TimePreferences generalPreferences;
	private List<String> activityNames;
	private List<ActivityProperties> activities;
	
	private List<LocationDescription> homeLocations;
	private double homeUtility;
	private double morningEnd;
	private double eveningStart;
	


	private Map<String,Double> travelUtilities;
	private double travelUtility = 0;
	private double moneyUtility = 0;
	
	private double maximumLoading = 1;
	private double maximumLoadingExceededUtility = 0;
	private double maximumMoney = Double.POSITIVE_INFINITY;
	

	public AgentProperties()
	{
		generalPreferences = new TimePreferences();
		activityNames = new ArrayList<String>();
		activities = new ArrayList<ActivityProperties>();
		homeLocations = new ArrayList<LocationDescription>();
		travelUtilities = new HashMap<String,Double>();
	}
	
	public double getHomeUtility() {
		return homeUtility;
	}

	public void setHomeUtility(double homeUtility) {
		this.homeUtility = homeUtility;
	}

	public double getMorningEnd() {
		return morningEnd;
	}

	public void setMorningEnd(double morningEnd) {
		this.morningEnd = morningEnd;
	}

	public double getEveningStart() {
		return eveningStart;
	}

	public void setEveningStart(double eveningStart) {
		this.eveningStart = eveningStart;
	}
	
	public double getMaximumLoading() {
		return maximumLoading;
	}

	public void setMaximumLoading(double maximumLoading) {
		this.maximumLoading = maximumLoading;
	}

	public double getMaximumLoadingExceededUtility() {
		return maximumLoadingExceededUtility;
	}

	public void setMaximumLoadingExceededUtility(
			double maximumLoadingExceededUtility) {
		this.maximumLoadingExceededUtility = maximumLoadingExceededUtility;
	}

	public double getTravelUtility(String mode)
	{
		if (travelUtilities.containsKey(mode))
			return travelUtilities.get(mode);
		return travelUtility;
	}

	public void setTravelUtility(double travelUtility) {
		this.travelUtility = travelUtility;
	}
	
	public void setTravelUtility(String mode, double travelUtility) {
		travelUtilities.put(mode, travelUtility);
	}

	public double getMoneyUtility() {
		return moneyUtility;
	}

	public void setMoneyUtility(double moneyUtility) {
		this.moneyUtility = moneyUtility;
	}
	
	public Collection<LocationDescription> getHomeLocations()
	{
		return Collections.unmodifiableCollection(homeLocations);
	}
	
	@SuppressWarnings("unchecked")
	public Collection<LocationDescription> getLocations(String activityName)
	{
		for (ActivityProperties act : activities)
		{
			if (act.getName().equals(activityName))
				return act.getLocations();
		}
		return Collections.EMPTY_SET;
	}
	
	public Collection<String> getActivityNames()
	{
		return Collections.unmodifiableCollection(activityNames);
	}
	
	public void addActivityProperties(ActivityProperties prop)
	{
		if (activityNames.contains(prop.getName()))
		{
			removeActivityProperties(prop.getName());
		}
		activities.add(prop);
		activityNames.add(prop.getName());
	}
	
	public void removeActivityProperties(String activityName)
	{
		if (activityNames.contains(activityName))
		{
			ActivityProperties rem = null;
			for (ActivityProperties act : activities)
				if (act.getName().equals(activityName))
					rem = act;
			activities.remove(rem);
			activityNames.remove(activityName);
		}
	}
	
	public TimePreferences getPreferences(String activityName)
	{
		for (ActivityProperties act : activities)
		{
			if (act.getName().equals(activityName))
				return act.getTimePreferences();
		}
		return generalPreferences;
	}
	
	public TimePreferences getDefaultPreferences()
	{
		return generalPreferences;
	}
	
	
	public static AgentProperties getRandom(Random r)
	{
		AgentProperties result = new AgentProperties();
		
		TimePreferences tp = new TimePreferences();
		
		tp.setDurationUtility(24 * r.nextDouble());
		tp.setDurationMean(7 + (2 - (4*r.nextDouble())));
		tp.setDurationStdDev(2 * r.nextDouble());
		
		if (r.nextDouble() < 0.5)
		{
			tp.setStartDevUtility(- (2 * r.nextDouble()));
			tp.setStartMean(9 + (2 - (4*r.nextDouble())));
			tp.setStartStdDev(2 * r.nextDouble());
		}
		else
		{
			tp.setEndDevUtility(- (2 * r.nextDouble()));
			tp.setEndMean(17 + (2 - (4*r.nextDouble())));
			tp.setEndStdDev(2 * r.nextDouble());
		}
		
		result.generalPreferences = tp;
		
		result.moneyUtility = 20 * r.nextDouble();
		result.travelUtility = 16 * r.nextDouble();
		
		return result;
	}

	public void setMoneyMaximum(double max)
	{
		maximumMoney  = max;		
	}

	public void addHomeLocation(LocationDescription ld)
	{
		homeLocations.add(ld);		
	}

	public Collection<? extends ActivityProperties> getActivities()
	{
		return Collections.unmodifiableList(activities);
	}

	public ActivityProperties getActivityProperties(String type)
	{
		for (ActivityProperties a : activities)
		{
			if (a.getName().equals(type))
				return a;
		}
		return null;
	}

	public double getMoneyMaximum()
	{
		return maximumMoney;
	}
	



}
