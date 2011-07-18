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
	
	public FirstPersonExtractorFromFile(final Scenario scn){
		super(scn);
		this.scn = scn;
	}
	
	public Population readFile(String filename, int personNum){
		this.personNum = personNum;
		this.readFile(filename);
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

					//write the persons and break the execution . Not very good but avoid parsing the whole document
					PopulationWriter popWriter = new PopulationWriter(scn.getPopulation(), scn.getNetwork());
					popWriter.write(scn.getConfig().controler().getOutputDirectory() + "/firstPersons.xml" );
					System.exit(0);
				}			
			}
		}
	}

	public static void main(String[] args) {
		final String popFilePath = "../../input/newDemand/bvg.run190.25pct.100.plans.xml";
		final String netFilePath = "../../input/newDemand/network.final.xml.gz";
		
		DataLoader dataloader = new DataLoader(); 
		ScenarioImpl scn = (ScenarioImpl) dataloader.createScenario();
		new MatsimNetworkReader(scn).readFile(netFilePath);
		scn.getConfig().controler().setOutputDirectory(new File (popFilePath).getParent());
		new FirstPersonExtractorFromFile(scn).readFile(popFilePath, 10);
	}
	
}
