/**
 * 
 */
package playground.southafrica.utilities.mapmatching;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.xml.sax.SAXParseException;

import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * @author jwjoubert
 *
 */
public class TrackMatchingConverter {
	final private static Logger LOG = Logger.getLogger(TrackMatchingConverter.class);
	private Scenario sc;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(TrackMatchingConverter.class.toString(), args);
		String networkFile = args[0];
		String tmFolder = args[1];
		String outputFile = args[2];

		/* Parse the network. */
		TrackMatchingConverter tmc = new TrackMatchingConverter(networkFile);

		/* Repeat for each TrackMatching file. */
		List<File> files = FileUtils.sampleFiles(new File(tmFolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml"));
		for(File file : files){
			String tmFile = file.getAbsolutePath();
			LOG.info(" ==> Converting TrackMatching to MATSim person: " + tmFile.substring(tmFile.lastIndexOf("/")+1));

			/* Parse the TrackMatching file. */
			TrackMatchingXmlReader tmx = new TrackMatchingXmlReader();
			tmx.setValidating(false);
			try{
				tmx.parse(tmFile);

				/* Get the route from the TrackMatching file. */
				List<Route> route = tmc.mapRoutesToNetwork(tmx.getAllRoutes());
				
				if(route.size() > 0){
					/* Build the person. */
					String ids = tmFile.substring(tmFile.lastIndexOf("/")+1, tmFile.indexOf("_"));
					
					String time = tmFile.substring(tmFile.indexOf("_")+1, tmFile.length()-4);
					Plan plan = tmc.constructPlanFromRoute(route, "waste", "waste", time);
					
					Person person = tmc.getScenario().getPopulation().getFactory().createPerson(Id.createPersonId(ids));
					person.addPlan(plan);
					tmc.getScenario().getPopulation().addPerson(person);
				}
			} catch(Exception e){
				/* Possible error with TrackMatching xml file. */
				LOG.error("Could not parse " + tmFile.substring(tmFile.lastIndexOf("/")+1));
			}
		}
		
		/* Write the population to file. */
		new PopulationWriter(tmc.getScenario().getPopulation()).write(outputFile);
		
		Header.printFooter();
	}
	
	public TrackMatchingConverter(String network) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(network);
		this.sc = ScenarioUtils.loadScenario(config);
	}

	
	
	
	public Plan constructPlanFromRoute(List<Route> routes, String activityType, String mode, String startTime){
		PopulationFactory pf = this.sc.getPopulation().getFactory();
		Plan plan = pf.createPlan();
		int hour = Integer.parseInt(startTime.substring(0, 2));
		int min = Integer.parseInt(startTime.substring(2, 4));
		
		for(int i = 0; i < routes.size(); i++){
			if(i > 0){
				Leg dummy = pf.createLeg(mode);
				plan.addLeg(dummy);
			}
			Route route  = routes.get(i);
			Activity a1 = pf.createActivityFromLinkId(activityType, routes.get(i).getStartLinkId());
			if(i==0){
				a1.setEndTime(hour*3600 + min*60);
			} else{
				a1.setMaximumDuration(Time.parseTime("00:05:00"));
			}
			plan.addActivity(a1);
			
			Leg leg = pf.createLeg(mode);
			leg.setRoute(route);
			plan.addLeg(leg);
			
			Activity a2 = pf.createActivityFromLinkId(activityType, route.getEndLinkId());
			a2.setMaximumDuration(Time.parseTime("00:05:00"));
			plan.addActivity(a2);
		}
		
		return plan;
	}
	
	public Scenario getScenario(){
		return this.sc;
	}
	
	
	public Route mapRouteToNetwork(List<Tuple<Id<Node>, Id<Node>>> route) {
		List<Id<Link>> linkList = new ArrayList<>();
		
		int index = 0;
		Id<Node> start = null;
		while(index < route.size()){
			Tuple<Id<Node>, Id<Node>> tuple = route.get(index);
			Id<Node> o = tuple.getFirst();
			Id<Node> d = tuple.getSecond();
			
			/* Check the origin node. */
			if(start == null){
				if(this.sc.getNetwork().getNodes().containsKey(o)){
					start = o;
				} else{
					index++;
				}
			}
			
			if(start != null){
				Node oNode = this.sc.getNetwork().getNodes().get(start);
				
				/* Check the destination node. */
				if(this.sc.getNetwork().getNodes().containsKey(d)){
					/* Both OD nodes exists. Now check if the link actually exists. */
					boolean foundLink = false;
					Iterator<? extends Link> linkIt = oNode.getOutLinks().values().iterator();
					while(!foundLink && linkIt.hasNext()){
						Link l = linkIt.next();
						if(d.equals(l.getToNode().getId())){
							linkList.add(l.getId());
							foundLink = true;
							start = null;
							index++;
						}
					}
					if(!foundLink){
						index++;
					}
				} else{
					index++;
				}
			}
		}
		
		
		Route theRoute = null;
		if(linkList.size() > 1){
			Id<Link> first = linkList.get(0);
			Id<Link> last = linkList.get(linkList.size()-1);

			linkList.remove(0);
			linkList.remove(linkList.size()-1);
			theRoute = new LinkNetworkRouteImpl(first, linkList, last);
		} else{
			LOG.warn("Found a route that has fewer than 2 links. Route will be ignored. Returning NULL");
		}
		return theRoute;
	}
	
	public List<Route> mapRoutesToNetwork(List<List<Tuple<Id<Node>, Id<Node>>>> routes){
		List<Route> routeList = new ArrayList<>();
		for(List<Tuple<Id<Node>, Id<Node>>> route : routes){
			if(route.size() > 1){
				Route thisRoute = this.mapRouteToNetwork(route);
				if(thisRoute != null){
					routeList.add(thisRoute);
				} else{
					/* Ignore the route given by TrackMatching. */
				}
			} else{
				/* Ignore the route given by TrackMatching. */
			}
		}
		return routeList;
	}

}
