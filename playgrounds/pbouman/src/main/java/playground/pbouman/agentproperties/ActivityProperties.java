package playground.pbouman.agentproperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ActivityProperties
{
	private String activityName;
	private TimePreferences temporalProperties;
	private List<LocationDescription> locations;
	private double availableFrom = Double.NEGATIVE_INFINITY;
	private double availableTo = Double.POSITIVE_INFINITY;
	
	public ActivityProperties(String name)
	{
		activityName = name;
		temporalProperties = new TimePreferences();
		locations = new ArrayList<LocationDescription>();
	}
	
	public String getName()
	{
		return activityName;
	}

	public TimePreferences getTimePreferences()
	{
		return temporalProperties;
	}

	public Collection<LocationDescription> getLocations()
	{
		return Collections.unmodifiableList(locations);
	}

	public void addLocation(LocationDescription ld)
	{
		locations.add(ld);
	}
	
	public void setAvailableFrom(double d)
	{
		availableFrom = d;
	}
	
	public void setAvailableTo(double d)
	{
		availableTo = d;
	}
	
	public double getAvailableFrom()
	{
		return availableFrom;
	}
	
	public double getAvailableTo()
	{
		return availableTo;
	}
	
}
