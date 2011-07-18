package playground.mmoyo.io;

import java.util.Collection;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;

import playground.mmoyo.utils.DataLoader;

/**Reads and creates only one person sequencially from the population file. Useful for filter algorithms*/
public class PopSecReader  extends MatsimXmlParser implements PopulationReader {
	private final static String PERSON = "person";
	private MatsimXmlParser delegPopReader;
	public Collection <? extends Person> persons;
	private PersonAlgorithm personAlgorithm;
	
	public PopSecReader(ScenarioImpl scn, PersonAlgorithm personAlgorithm){
		this.persons = scn.getPopulation().getPersons().values();
		this.personAlgorithm = personAlgorithm;
		this.delegPopReader = new MatsimPopulationReader(scn);
	}
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		delegPopReader.startTag(name, atts, context);
	}
	
	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		delegPopReader.endTag(name, content, context);
		if (PERSON.equals(name)) {
			Person currPerson = persons.iterator().next();
			this.personAlgorithm.run(currPerson);
			persons.clear();
		} 
	}
	
	@Override
	public InputSource resolveEntity(final String publicId, final String systemId) {
		return delegPopReader.resolveEntity(publicId, systemId);
	}

	@Override
	public void readFile(final String filename) throws UncheckedIOException {
		parse(filename);
	}
	
	public static void main(String[] args) {
		String netFilePath;
		String popFilePath;
		if (args.length>0){
			popFilePath = args[0];
			netFilePath = args[1];
		}else{
			popFilePath = "../../input/newDemand/bvg.run190.25pct.100.plans.xml";
			netFilePath = "../../input/newDemand/network.final.xml.gz";   // "../../input/newDemand/multimodalNet.xml.gz";
		}

		DataLoader dataLoader = new DataLoader ();
		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario(); 
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scn);
		matsimNetReader.readFile(netFilePath);
			
		IdPrinter idPrinter = new IdPrinter();
		new PopSecReader(scn, idPrinter).readFile(popFilePath);
	}
	
}

class IdPrinter implements PersonAlgorithm{
	private final static Logger log = Logger.getLogger(IdPrinter.class);
	
	@Override
	public void run(Person person) {
		log.info(person.getId());
	}
}
