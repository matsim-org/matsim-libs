package playground.tschlenther.parkingSearch.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

public class GridNetPopulationCreator {

//	private final static String pathToZoneOne = "C:/Users/Work/Bachelor Arbeit/input/GridNet/Zonen/Links_Activities.txt";
//	private final static String pathToZoneTwo = "C:/Users/Work/Bachelor Arbeit/input/GridNet/Zonen/Rechts_Activities.txt";
	
	private final static String pathToZoneOne = "C:/Users/Work/Bachelor Arbeit/input/Berlin/Klausener.txt";
	private final static String pathToZoneTwo = "C:/Users/Work/Bachelor Arbeit/input/Berlin/Mierendorff.txt";
	
	public static void main(String[] args){
		int nrOfAgentsPerZone = 60;
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig("C:/Users/Work/Bachelor Arbeit/input/GridNet/config_links_rechts.xml"));
		Population population = scenario.getPopulation();
		PopulationFactory fac = population.getFactory();
		
		List<Id<Link>> linksOfZoneOne = getLinkIDsOfZone(pathToZoneOne);
		List<Id<Link>> linksOfZoneTwo = getLinkIDsOfZone(pathToZoneTwo);
		
		Random rand = MatsimRandom.getRandom();
		
		for( int i = 0; i < nrOfAgentsPerZone; i++){
			Person linkerAgent = fac.createPerson(Id.createPersonId("Links_" + i));
			Plan linkerPlan = fac.createPlan();
			Id<Link> linkerhomeLink = linksOfZoneOne.get(rand.nextInt(linksOfZoneOne.size()));
			Activity homeAct1 = fac.createActivityFromLinkId("home", linkerhomeLink);
			homeAct1.setStartTime(0);
			homeAct1.setEndTime(8*3600 + (i * 3600 / nrOfAgentsPerZone));
			linkerPlan.addActivity(homeAct1);
			
			linkerPlan.addLeg(fac.createLeg("car"));
			
			Activity workAct = fac.createActivityFromLinkId("work", linksOfZoneTwo.get(rand.nextInt(linksOfZoneTwo.size())));
			workAct.setEndTime(11*3600 + (i * 3600 / nrOfAgentsPerZone));
			linkerPlan.addActivity(workAct);
			
			linkerPlan.addLeg(fac.createLeg("car"));
			
			Activity homeAct2 = fac.createActivityFromLinkId("home", linkerhomeLink);
			linkerPlan.addActivity(homeAct2);
			linkerAgent.addPlan(linkerPlan);
			
			Person rechterAgent = fac.createPerson(Id.createPersonId("Rechts_" + i));
			Plan rechterPlan = fac.createPlan();
			Id<Link> rechterHomeLink = linksOfZoneTwo.get(rand.nextInt(linksOfZoneTwo.size()));
			
			Activity homeActRechts1 = fac.createActivityFromLinkId("home", rechterHomeLink); 
			homeActRechts1.setStartTime(0);
			homeActRechts1.setEndTime(8*3600 + (i * 3600 / nrOfAgentsPerZone));
			rechterPlan.addActivity(homeActRechts1);
			
			rechterPlan.addLeg(fac.createLeg("car"));
			
			Activity workActRechts = fac.createActivityFromLinkId("work", linksOfZoneOne.get(rand.nextInt(linksOfZoneOne.size()))); 
			workActRechts.setEndTime(11*3600 + (i * 3600 / nrOfAgentsPerZone));
			rechterPlan.addActivity(workActRechts);
			
			rechterPlan.addLeg(fac.createLeg("car"));
			Activity homeAct2Rechts = fac.createActivityFromLinkId("home", rechterHomeLink); 
			rechterPlan.addActivity(homeAct2Rechts);
			rechterAgent.addPlan(rechterPlan);
			
			population.addPerson(rechterAgent);
			population.addPerson(linkerAgent);			
		}
		
		new PopulationWriter(population).write("C:/Users/Work/Bachelor Arbeit/input/Berlin/population_Test_60.xml");
	}

	private static List<Id<Link>> getLinkIDsOfZone (String pathToZoneFile){
		
		List<Id<Link>> links = new ArrayList<Id<Link>>();
		
		TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] {"\t"});
        config.setFileName(pathToZoneFile);
        config.setCommentTags(new String[] { "#" });
        new TabularFileParser().parse(config, new TabularFileHandler() {
			@Override
			public void startRow(String[] row) {
				Id<Link> linkId = Id.createLinkId(row[0]);
				links.add(linkId);
			}
		
        });
		return links;
	}

}
