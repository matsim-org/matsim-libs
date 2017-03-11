package playground.gleich.av_bus.prepareScenario;

import com.vividsolutions.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.router.TransitActsRemover;
import playground.gleich.av_bus.FilePaths;
import playground.jbischoff.utils.JbUtils;

import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

import java.util.HashSet;
import java.util.Set;

/**
 * @author gleich
 * 
 * Return a population with all agents who have activities within a certain geographic area or 
 * pass through this area by car (and ignore all other agents).
 * 
 */
public class ExtractAgentsInAreaStreamReading {
	
	private String inputNetworkPath;
	private String inputPopulationPath;
	private String studyAreaShpPath;
	private String studyAreaShpKey;
	private String studyAreaShpElement;
	private String outputPopulationPath;
	private String outputLinksInAreaShpPath;
	private String outputLinksInAreaShpCoordinateSystem;
	private Scenario inputScenario;
	private Scenario outputScenario;
	private Network networkEnclosedInStudyArea;
	private Set<Id<Link>> linksInArea = new HashSet<>();
	private Geometry geometryStudyArea;
	private boolean selectAgentsByRoutesThroughArea;
	private boolean selectAgentsByActivitiesInArea;
	private boolean produceOutputLinksInAreaShp;
	
	public static void main(String[] args) {
		ExtractAgentsInAreaStreamReading extractor;

		if (args.length == 8) {
			String inputNetworkPath = args[0];
			String inputPopulationPath = args[1];
			String studyAreaShpPath = args[2];
			String studyAreaShpKey = args[3];
			String studyAreaShpElement = args[4];
			String outputPopulationPath = args[5];
			boolean selectAgentsByActivitiesInArea = Boolean.parseBoolean(args[6]);
			boolean selectAgentsByRoutesThroughArea = Boolean.parseBoolean(args[7]);
			extractor = new ExtractAgentsInAreaStreamReading(inputNetworkPath, inputPopulationPath, studyAreaShpPath, 
					studyAreaShpKey, studyAreaShpElement, outputPopulationPath, selectAgentsByActivitiesInArea, 
					selectAgentsByRoutesThroughArea);
			
		} else if (args.length == 10) {
			String inputNetworkPath = args[0];
			String inputPopulationPath = args[1];
			String studyAreaShpPath = args[2];
			String studyAreaShpKey = args[3];
			String studyAreaShpElement = args[4];
			String outputPopulationPath = args[5];
			boolean selectAgentsByActivitiesInArea = Boolean.parseBoolean(args[6]);
			boolean selectAgentsByRoutesThroughArea = Boolean.parseBoolean(args[7]);
			String outputLinksInAreaShpPath = args[8];
			String outputLinksInAreaShpCoordinateSystem = args[9];
			extractor = new ExtractAgentsInAreaStreamReading(inputNetworkPath, inputPopulationPath, studyAreaShpPath, 
					studyAreaShpKey, studyAreaShpElement, outputPopulationPath, selectAgentsByActivitiesInArea, 
					selectAgentsByRoutesThroughArea, true, outputLinksInAreaShpPath, outputLinksInAreaShpCoordinateSystem);
			
		} else {
			String inputNetworkPath = FilePaths.PATH_NETWORK_BERLIN__10PCT;
			String inputPopulationPath = FilePaths.PATH_POPULATION_BERLIN__10PCT_UNFILTERED;
			String studyAreaShpPath = FilePaths.PATH_STUDY_AREA_SHP;
			String studyAreaShpKey = FilePaths.STUDY_AREA_SHP_KEY;
			String studyAreaShpElement = FilePaths.STUDY_AREA_SHP_ELEMENT;
			String outputPopulationPath = FilePaths.PATH_POPULATION_BERLIN__10PCT_FILTERED;
			boolean selectAgentsByRoutesThroughArea = true;
			boolean selectAgentsByActivitiesInArea = true;
			String outputLinksInAreaShpPath = FilePaths.PATH_SHP_LINKS_ENCLOSED_IN_AREA_BERLIN__10PCT;
			String outputLinksInAreaShpCoordinateSystem = "DHDN_GK4";
			extractor = new ExtractAgentsInAreaStreamReading(inputNetworkPath, inputPopulationPath, studyAreaShpPath, 
					studyAreaShpKey, studyAreaShpElement, outputPopulationPath, selectAgentsByActivitiesInArea, 
					selectAgentsByRoutesThroughArea, false, outputLinksInAreaShpPath, outputLinksInAreaShpCoordinateSystem);

		}
		extractor.run();
	}
	
	ExtractAgentsInAreaStreamReading(String inputNetworkPath, String inputPopulationPath, String studyAreaShpPath, 
			String studyAreaShpKey, String studyAreaShpElement, String outputPopulationPath, 
			boolean selectAgentsByRoutesThroughArea, boolean selectAgentsByActivitiesInArea){
		new ExtractAgentsInAreaStreamReading(inputNetworkPath, inputPopulationPath, studyAreaShpPath, 
				studyAreaShpKey, studyAreaShpElement, outputPopulationPath, selectAgentsByActivitiesInArea, 
				selectAgentsByRoutesThroughArea, false, "", "");
	}
	
	ExtractAgentsInAreaStreamReading(String inputNetworkPath, String inputPopulationPath, String studyAreaShpPath, 
			String studyAreaShpKey, String studyAreaShpElement, String outputPopulationPath, 
			boolean selectAgentsByRoutesThroughArea, boolean selectAgentsByActivitiesInArea, 
			boolean produceOutputLinksInAreaShp, String outputLinksInAreaShpPath, String outputLinksInAreaShpCoordinateSystem){
		this.inputNetworkPath = inputNetworkPath;
		this.inputPopulationPath = inputPopulationPath;
		this.studyAreaShpPath = studyAreaShpPath;
		this.studyAreaShpKey = studyAreaShpKey;
		this.studyAreaShpElement = studyAreaShpElement;
		this.outputPopulationPath = outputPopulationPath;
		this.selectAgentsByRoutesThroughArea = selectAgentsByRoutesThroughArea;
		this.selectAgentsByActivitiesInArea = selectAgentsByActivitiesInArea;
		this.produceOutputLinksInAreaShp = produceOutputLinksInAreaShp;
		this.outputLinksInAreaShpPath = outputLinksInAreaShpPath;
		this.outputLinksInAreaShpCoordinateSystem = outputLinksInAreaShpCoordinateSystem;
	}
	
	private void run(){
		initialize();
		System.out.println("initialize done");
		
		StreamingPopulationReader spr = new StreamingPopulationReader(inputScenario);
		spr.addAlgorithm(new PersonAlgorithm() {

			@Override
			public void run(Person person) {
				if(selectAgentsByRoutesThroughArea){
					if(hasRouteThroughArea(person)){
						outputScenario.getPopulation().addPerson(person);
					} else if(selectAgentsByActivitiesInArea){
						if(hasActivityInArea(person)){
							outputScenario.getPopulation().addPerson(person);
						}
					}
				} else if(selectAgentsByActivitiesInArea){
					if(hasActivityInArea(person)){
						outputScenario.getPopulation().addPerson(person);
					}
				}
			}
		}
				);
		spr.readFile(inputPopulationPath);
		System.out.println("ExtractAgentsInArea done");
		removeTransitActsAndCarRoutes();
		new PopulationWriter(outputScenario.getPopulation()).writeV4(outputPopulationPath);
	}
	
	private void initialize(){		
		inputScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		outputScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(inputScenario.getNetwork()).readFile(inputNetworkPath);
		geometryStudyArea = JbUtils.readShapeFileAndExtractGeometry(studyAreaShpPath,studyAreaShpKey).get(studyAreaShpElement);
		findLinksInArea();
	}
	
	/** Find all links whose start and end are situated within the area */
	private void findLinksInArea() {
		networkEnclosedInStudyArea = NetworkUtils.createNetwork();
		for(Link link: inputScenario.getNetwork().getLinks().values()){
			if(geometryStudyArea.contains(MGC.coord2Point(link.getFromNode().getCoord())) &&
					geometryStudyArea.contains(MGC.coord2Point(link.getToNode().getCoord()))){
				linksInArea.add(link.getId());
				if(produceOutputLinksInAreaShp){
					Node fromNode = link.getFromNode();
					Node newNetworkFromNode; 
					if(!networkEnclosedInStudyArea.getNodes().containsKey(fromNode.getId())){
						newNetworkFromNode = NetworkUtils.createNode(fromNode.getId(), fromNode.getCoord());
						networkEnclosedInStudyArea.addNode(newNetworkFromNode);
					} else {
						newNetworkFromNode = networkEnclosedInStudyArea.getNodes().get(fromNode.getId());
					}
					Node toNode = link.getToNode();
					Node newNetworkToNode;
					if(!networkEnclosedInStudyArea.getNodes().containsKey(toNode.getId())){
						newNetworkToNode = NetworkUtils.createNode(toNode.getId(), toNode.getCoord());
						networkEnclosedInStudyArea.addNode(newNetworkToNode);
					} else {
						newNetworkToNode = networkEnclosedInStudyArea.getNodes().get(toNode.getId());
					}
					Link newNetworkLink = NetworkUtils.createAndAddLink(networkEnclosedInStudyArea, link.getId(), 
							newNetworkFromNode, newNetworkToNode, link.getLength(), link.getFreespeed(), 
							link.getCapacity(), link.getNumberOfLanes());
					newNetworkLink.setAllowedModes(link.getAllowedModes());
				}
			}
		}
		if(produceOutputLinksInAreaShp){
			Links2ESRIShape shp = new Links2ESRIShape(networkEnclosedInStudyArea, outputLinksInAreaShpPath, outputLinksInAreaShpCoordinateSystem);
			shp.write();					
		}
	}

	private boolean hasRouteThroughArea(Person p) {		
		Plan plan = p.getSelectedPlan();
		for (PlanElement pe : plan.getPlanElements()){
			if (pe instanceof Leg){
				Leg leg = (Leg) pe;
				if (leg.getRoute() != null && leg.getRoute() instanceof NetworkRoute){
					NetworkRoute route = (NetworkRoute) leg.getRoute();
					for(Id<Link> link: linksInArea){
						if(route.getLinkIds().contains(link)){
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean hasActivityInArea(Person p) {
		Plan plan = p.getSelectedPlan();
		for (PlanElement pe : plan.getPlanElements()){
			if (pe instanceof Activity){
				Activity act = (Activity) pe;
				if (!act.getType().contains("pt interaction")){
					Coord coord = act.getCoord();
					if (geometryStudyArea.contains(MGC.coord2Point(coord))){
						return true;
					}
				}
			}
		}
		return false;
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
