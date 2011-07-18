package playground.mmoyo.algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

import playground.mmoyo.io.PopSecReader;
import playground.mmoyo.io.TextFileWriter;
import playground.mmoyo.utils.DataLoader;

/** Counts agents and lists their id's*/
public class PopulationList extends AbstractPersonAlgorithm {
	private final static Logger log = Logger.getLogger(PopulationList.class);
	private List<String> strIdList = new ArrayList<String>();
	
	@Override
	public void run(Person person) {
		strIdList.add(person.getId().toString());
	}

	/** Only shows the ids on console*/
	protected void printIds(){
		for (String strId : this.strIdList){
			log.info(strId);
		}
		log.info("Agents: " + this.strIdList.size());
	}
	
	/**creates a txt file with the ids in the same director of the population file*/
	protected void saveIds(final String popFilepath){
		final String NR= "\n";
		StringBuffer sBuff = new StringBuffer();
		for (String  strId: this.strIdList){
			sBuff.append(strId);
			sBuff.append(NR);
		}
		sBuff.append("Num of agents: " + this.strIdList.size());
		File file = new File (popFilepath);
		String outFile = file.getParent() + "/" + file.getName() + "_IdList.txt";
		new TextFileWriter().write(sBuff.toString(), outFile ,false);
		log.info(this.strIdList.size() + " agents found in population file.");
	}
	
	public static void main(String[] args) {
		String popFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/baseplan_10x_subset_xy2links.xml.gz";
		log.info("counting...");
		
		ScenarioImpl scn = (ScenarioImpl) new DataLoader().createScenario();
		PopulationList popList2 = new PopulationList (); 
		PopSecReader popSecReader = new PopSecReader (scn, popList2);
		popSecReader.readFile(popFilePath);
		
		popList2.printIds();
	}
	
}
