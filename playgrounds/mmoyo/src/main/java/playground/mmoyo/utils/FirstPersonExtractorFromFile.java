package playground.mmoyo.utils;

import java.io.File;
import java.util.Stack;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.xml.sax.Attributes;

/** reads a population file but creates only the first n persons as population*/
public class FirstPersonExtractorFromFile extends MatsimPopulationReader {
	private final static String PERSON = "person";
	
	int personCounter;
	int personNum;
	boolean cont = true;
	Scenario scn;
	String inputFileName;
	
	public FirstPersonExtractorFromFile(final Scenario scn){
		super(scn);
		this.scn = scn;
	}
	
	public Population readFile(final String fileName, int personNum){
		this.personNum = personNum;
		this.inputFileName = fileName;
		this.readFile(fileName);
		return this.scn.getPopulation();
	}

	@Override
	public void readFile(String filename) {
		parse(filename);
	}
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if(cont){
			super.startTag(name, atts, context);
		}
	}
	
	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if(cont){
			super.endTag(name, content, context);
			if (PERSON.equals(name)) {
				this.personCounter ++;
				if (this.personCounter == this.personNum){

					//write the persons and break the execution. Not very good but avoid parsing the whole document
					//use thread.interrupt();
					PopulationWriter popWriter = new PopulationWriter(scn.getPopulation(), scn.getNetwork());
					popWriter.write(inputFileName + personNum + "firstPersons.xml");
					System.exit(0);
				}
			}
		}
	}

	public static void main(String[] args) {
		String netFilePath;
		String popFilePath;
		final int agentNum;	
		
		if (args.length>0){
			netFilePath = args[0];
			popFilePath = args[1];
			agentNum = Integer.valueOf (args[2]);
		}else{
			netFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
			popFilePath = "../../input/choiceM44/100.plans.xml";
			agentNum=50;	
		}
		
		//load data
		DataLoader dataloader = new DataLoader(); 
		ScenarioImpl scn = (ScenarioImpl) dataloader.createScenario();
		new MatsimNetworkReader(scn).readFile(netFilePath);
		
		//get first agents
		Population pop = new FirstPersonExtractorFromFile(scn).readFile(popFilePath, agentNum);
		
		//write first agents in same directory
		PopulationWriter popWriter = new PopulationWriter(pop, scn.getNetwork());
		File file = new File(popFilePath);
		popWriter.write(file.getPath() + ".firstPersons.xml") ;
		
	}
	
}
