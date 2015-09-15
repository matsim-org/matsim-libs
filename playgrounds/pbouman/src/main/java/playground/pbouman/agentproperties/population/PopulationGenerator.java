package playground.pbouman.agentproperties.population;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import playground.pbouman.agentproperties.ActivityProperties;
import playground.pbouman.agentproperties.AgentProperties;
import playground.pbouman.agentproperties.LocationDescription;
import playground.pbouman.agentproperties.TimePreferences;

import java.util.*;

public class PopulationGenerator
{
	private Map<String,AgentProperties> properties;
	private Scenario scen;
	
	public PopulationGenerator(Scenario s, Map<String,AgentProperties> props)
	{
		properties = props;
		scen = s;
	}
	
	public void run()
	{
		Population pop = scen.getPopulation();
		PopulationFactory factory = pop.getFactory();
		NetworkImpl network = (NetworkImpl) scen.getNetwork();
		
		for (String s : properties.keySet())
		{
			Id<Person> id = Id.create(s, Person.class);
		
			Person person = factory.createPerson(id);
			Plan plan = factory.createPlan();
			
			
			//  This is crude and ugly...
			
			
			
			
			AgentProperties aprop = properties.get(s);
			
			LocationDescription home = getFirst(aprop.getHomeLocations());
			
			
			ArrayList<ActivityProperties> activities = new ArrayList<ActivityProperties>(aprop.getActivities());
			Collections.sort(activities,actComparator);
			boolean first = true;
			
			for (ActivityProperties ap : activities)
			{
				TimePreferences timepref = ap.getTimePreferences();
				if (first)
				{
					
					ActivityImpl act;
					if (home.describesLink())
					{
						Link l = findLink(home.getLinkId(),network); //network.getLinks().get(scen.createId(home.getLinkId()));
						act = (ActivityImpl) factory.createActivityFromCoord("home", l.getCoord());
						act.setLinkId(l.getId());
					}
					else
					{
						act = (ActivityImpl) factory.createActivityFromCoord("home", new Coord(home.getX(), home.getY()));
						act.setLinkId(NetworkUtils.getNearestLink(network, act.getCoord()).getId());
					}
			
					if (timepref.getStartDevUtility() != 0)
						act.setEndTime(timepref.getStartMean());
					else
						act.setEndTime(timepref.getEndMean() - timepref.getDurationMean());
					
					plan.addActivity(act);
					first = false;
				}
				
				plan.addLeg(factory.createLeg("pt"));
				LocationDescription loc = getFirst(ap.getLocations());
				ActivityImpl act;
				if (loc.describesLink())
				{
					Link l = findLink(loc.getLinkId(),network);
					//Link l = network.getLinks().get(scen.createId(loc.getLinkId()));
					act = (ActivityImpl) factory.createActivityFromCoord(ap.getName(), l.getCoord());
					act.setLinkId(l.getId());
				}
				else
				{
					act = (ActivityImpl) factory.createActivityFromCoord(ap.getName(), new Coord(loc.getX(), loc.getY()));
					act.setLinkId(NetworkUtils.getNearestLink(network, act.getCoord()).getId());
				}
				
				if (timepref.getStartDevUtility() != 0)
					act.setEndTime(timepref.getStartMean() + timepref.getDurationMean());
				else
					act.setEndTime(timepref.getEndMean());
				
				plan.addActivity(act);
			}
			
			plan.addLeg(factory.createLeg("pt"));
			ActivityImpl act;
			if (home.describesLink())
			{
				Link l = findLink(home.getLinkId(),network);
				act = (ActivityImpl) factory.createActivityFromCoord("home", l.getCoord());
				act.setLinkId(l.getId());
			}
			else
			{
				act = (ActivityImpl) factory.createActivityFromCoord("home", new Coord(home.getX(), home.getY()));
				act.setLinkId(NetworkUtils.getNearestLink(network, act.getCoord()).getId());
			}
			plan.addActivity(act);
			
			
			person.addPlan(plan);
			pop.addPerson(person);
		}
	}
	
	private static LocationDescription getFirst(Collection<LocationDescription> locs)
	{
		for (LocationDescription l : locs)
			return l;
		return null;
	}
	
	private static Link findLink(String name, NetworkImpl net)
	{
		for (Link l : net.getLinks().values())
			if (l.getId().toString().equals(name))
				return l;
		return null;
	}
	
	private static Comparator<ActivityProperties> actComparator = new Comparator<ActivityProperties>()
			{

				@Override
				public int compare(ActivityProperties o1, ActivityProperties o2)
				{
					TimePreferences timePref1 = o1.getTimePreferences();
					TimePreferences timePref2 = o2.getTimePreferences();
					int startComp = Double.compare(timePref1.getStartMean(),timePref2.getStartMean());
					if (startComp != 0)
						return startComp;
					int durationComp = Double.compare(timePref1.getDurationMean(), timePref2.getDurationMean());
					return durationComp;
				}
		
			};
	
}
