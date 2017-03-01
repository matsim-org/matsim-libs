package playground.gleich.av_bus.prepareScenario;

import com.vividsolutions.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.router.TransitActsRemover;
import playground.gleich.av_bus.FilePaths;
import playground.gthunig.utils.CSVWriter;
import playground.jbischoff.utils.JbUtils;

import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

import java.util.HashSet;
import java.util.Set;

/**
 * @author gleich
 * 
 * Return all agents who have activities within a certain geographic area or pass through this area
 * by car (and ignore all other agents).
 * 
 * TODO:
 * output network file is not recognized as a network by via, but can be read with the MatsimNetworkReader
 */
public class ExtractAgentsInArea {
	
	private String inputNetworkPath;
	private String inputPopulationPath;
	private String studyAreaShpPath;
	private String studyAreaShpKey;
	private String studyAreaShpElement;
	private String outputPopulationPath;
	private String outputNetworkPath;
	private String outputLinksInAreaPath;
	private String outputLinksInAreaShpPath;
	private Scenario inputScenario;
	private Scenario outputScenario;
	private Network networkEnclosedInStudyArea;
	private Set<Id<Link>> linksInArea = new HashSet<>();
	private Geometry geometryStudyArea;
	
	public static void main(String[] args) {
		ExtractAgentsInArea extractor = new ExtractAgentsInArea();
		
		if (args.length != 0) {
			extractor.inputNetworkPath = args[0];
			extractor.inputPopulationPath = args[1];
			extractor.studyAreaShpPath = args[2];
			extractor.studyAreaShpKey = args[3];
			extractor.studyAreaShpElement = args[4];
			extractor.outputPopulationPath = args[5];
			extractor.outputNetworkPath = args[6];
			extractor.outputLinksInAreaPath = args[7];
			extractor.outputLinksInAreaShpPath = args[8];
			
		} else {
			extractor.inputNetworkPath = FilePaths.PATH_NETWORK_BERLIN_100PCT;
			extractor.inputPopulationPath = FilePaths.PATH_POPULATION_BERLIN_100PCT_UNFILTERED;
			extractor.studyAreaShpPath = FilePaths.PATH_STUDY_AREA_SHP;
			extractor.studyAreaShpKey = FilePaths.STUDY_AREA_SHP_KEY;
			extractor.studyAreaShpElement = FilePaths.STUDY_AREA_SHP_ELEMENT;
			extractor.outputPopulationPath = FilePaths.PATH_POPULATION_BERLIN_100PCT_FILTERED;
			extractor.outputNetworkPath = FilePaths.PATH_NETWORK_BERLIN_100PCT_ENCLOSED_IN_AREA;
			extractor.outputLinksInAreaPath = FilePaths.PATH_BERLIN_100PCT_LINKS_ENCLOSED_IN_AREA;
			extractor.outputLinksInAreaShpPath = FilePaths.PATH_BERLIN_100PCT_SHP_LINKS_ENCLOSED_IN_AREA;
		}
		extractor.run();
	}
	
	private void run(){
		initialize();
		System.out.println("initialize done");
		selectAgentsByActivitiesInArea();
		System.out.println("selectAgentsByActivitiesInArea() done");
		findLinksInArea();
		System.out.println("findLinksInArea() done");
		selectAgentsByRoutesThroughArea();
		System.out.println("selectAgentsByRoutesThroughArea() done");
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
		Links2ESRIShape shp = new Links2ESRIShape(networkEnclosedInStudyArea, outputLinksInAreaShpPath, "DHDN_GK4");
		shp.write();
		new NetworkWriter(networkEnclosedInStudyArea).write(outputNetworkPath);
		CSVWriter linksWriter = new CSVWriter(outputLinksInAreaPath, ",");
		for(Id<Link> link : linksInArea){
			linksWriter.writeField(link.toString());
			linksWriter.writeField(Double.toString(networkEnclosedInStudyArea.getLinks().get(link).getFromNode().getCoord().getX())); 
			linksWriter.writeField(Double.toString(networkEnclosedInStudyArea.getLinks().get(link).getFromNode().getCoord().getY())); 
			linksWriter.writeField(Double.toString(networkEnclosedInStudyArea.getLinks().get(link).getToNode().getCoord().getX())); 
			linksWriter.writeField(Double.toString(networkEnclosedInStudyArea.getLinks().get(link).getToNode().getCoord().getY())); 
			linksWriter.writeNewLine();
		}
		linksWriter.close();
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
