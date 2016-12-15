package playground.gleich.av_bus.prepareScenario;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.router.TransitActsRemover;

import com.vividsolutions.jts.geom.Geometry;

import playground.gleich.av_bus.FilePaths;
import playground.jbischoff.utils.JbUtils;

/**
 * @author gleich
 * 
 * Return all agents who have activities within a certain geographic area or pass through this area
 * by car (and ignore all other agents).
 */
public class ExtractAgentsInArea {
	
	private final String inputNetworkPath = FilePaths.PATH_NETWORK_BERLIN__10PCT;
	private final String inputPopulationPath = FilePaths.PATH_POPULATION_BERLIN__10PCT_UNFILTERED;
	private final String studyAreaShpPath = FilePaths.PATH_STUDY_AREA_SHP;
	private final String studyAreaShpKey = FilePaths.STUDY_AREA_SHP_KEY;
	private final String studyAreaShpElement = FilePaths.STUDY_AREA_SHP_ELEMENT;
	private final String networkInAreaPath = FilePaths.PATH_NETWORK_IN_STUDY_AREA_BERLIN__10PCT;
	private final String outputPopulationPath = FilePaths.PATH_POPULATION_BERLIN__10PCT_FILTERED_TEST;
	private Scenario inputScenario;
	private Scenario outputScenario;
	private Network networkEnclosedInStudyArea;
	private Set<Id<Link>> linksInArea = new HashSet<Id<Link>>();
	private Geometry geometryStudyArea;
	
	public static void main(String[] args) {
		(new ExtractAgentsInArea()).run();
	}
	
	void run(){
		initialize();
		selectAgentsByActivitiesInArea();
		findLinksInArea();
		selectAgentsByRoutesThroughArea();
		removeTransitActsAndCarRoutes();
		new PopulationWriter(outputScenario.getPopulation()).writeV4(outputPopulationPath);
	}
	
	/** Find all links whose start and end are situated within the area */
	private void findLinksInArea() {
		networkEnclosedInStudyArea = NetworkUtils.createNetwork();
		for(Link link: inputScenario.getNetwork().getLinks().values()){
			if(geometryStudyArea.contains(MGC.coord2Point(link.getFromNode().getCoord())) &&
					geometryStudyArea.contains(MGC.coord2Point(link.getToNode().getCoord()))){
				linksInArea.add(link.getId());
				networkEnclosedInStudyArea.addNode(link.getFromNode());
				networkEnclosedInStudyArea.addNode(link.getToNode());
				networkEnclosedInStudyArea.addLink(link);
			}
		}
		new NetworkWriter( inputScenario.getNetwork() ).write(networkInAreaPath);
	}

	private void selectAgentsByRoutesThroughArea() {
		int i = 0;
		for (Person p : inputScenario.getPopulation().getPersons().values()){
			i++;
			if (i%200000==0) System.out.println(i);
			
			boolean agentAlreadySelected = outputScenario.getPopulation().getPersons().containsKey(p.getId());
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()){
				if(agentAlreadySelected){
					break;
				} else if (pe instanceof Leg){
					Leg leg = (Leg) pe;
					if (leg.getRoute() != null && leg.getRoute() instanceof NetworkRoute){
						NetworkRoute route = (NetworkRoute) leg.getRoute();
						for(Id<Link> link: linksInArea){
							if(route.getLinkIds().contains(link)){
								outputScenario.getPopulation().addPerson(p);
								agentAlreadySelected = true;
								break;
							}
						}
					}
				}
			}
		}
	}

	private void initialize(){		
		inputScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		outputScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(inputScenario.getNetwork()).readFile(inputNetworkPath);
		new PopulationReader(inputScenario).readFile(inputPopulationPath);
		geometryStudyArea = JbUtils.readShapeFileAndExtractGeometry(studyAreaShpPath,studyAreaShpKey).get(studyAreaShpElement);
	}
	
	private void selectAgentsByActivitiesInArea(){
		int i = 0;
		for (Person p : inputScenario.getPopulation().getPersons().values()){
			i++;
			if (i%200000==0) System.out.println(i);
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Activity){
					Activity act = (Activity) pe;
					if (!act.getType().contains("pt interaction")){
						Coord coord = act.getCoord();
						if (geometryStudyArea.contains(MGC.coord2Point(coord))){
							outputScenario.getPopulation().addPerson(p);
							break;
						}
					}
				}
			}
		}
	}

	private void removeTransitActsAndCarRoutes(){
		for (Person p : outputScenario.getPopulation().getPersons().values()){
			Plan plan = p.getSelectedPlan();
			new TransitActsRemover().run(plan);
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Leg){
					Leg leg = (Leg) pe;
					if (leg.getMode().equals("car")){
						leg.setRoute(null);
					}
				}
			}
		}
	}
}
