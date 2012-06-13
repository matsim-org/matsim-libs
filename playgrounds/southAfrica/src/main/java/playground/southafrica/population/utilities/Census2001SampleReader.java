package playground.southafrica.population.utilities;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.southafrica.utilities.Header;

public class Census2001SampleReader {
	private static final Logger LOG = Logger.getLogger(Census2001SampleParser.class);
	private Scenario sc;
	private HouseholdsImpl households = new HouseholdsImpl();
	private ObjectAttributes householdAttributes = new ObjectAttributes();
	private ObjectAttributes personAttributes = new ObjectAttributes();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(Census2001SampleReader.class.toString(), args);
		
		String inputFolder = args[0];
		Census2001SampleReader csr = new Census2001SampleReader();
		csr.parse(inputFolder);
		
		Header.printFooter();
	}
	
	public Census2001SampleReader() {
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}
	
	public void parse(String inputfolder){
		/* Read population. */
		LOG.info("Reading population...");
		PopulationReaderMatsimV5 pr = new PopulationReaderMatsimV5(this.sc);
		pr.parse(inputfolder + "Population.xml");
		
		/* Read population attributes. */
		LOG.info("Reading person attributes...");
		ObjectAttributesXmlReader oar1 = new ObjectAttributesXmlReader(personAttributes);
		oar1.parse(inputfolder + "PersonAttributes.xml");
		
		/* Read households */
		LOG.info("Reading households...");
		HouseholdsReaderV10 hhr = new HouseholdsReaderV10(households);
		hhr.parse(inputfolder + "Households.xml");
		
		/* Read household attributes. */ 
		LOG.info("Reading household attributes...");
		ObjectAttributesXmlReader oar2 = new ObjectAttributesXmlReader(householdAttributes);
		oar2.parse(inputfolder + "HouseholdAttributes.xml");

		LOG.info("================================================================");
		LOG.info("Population size: " + sc.getPopulation().getPersons().size());
		LOG.info("Number of households: " + households.getHouseholds().size());
		LOG.info("================================================================");
	}

	public Scenario getScenario() {
		return sc;
	}
	
	public HouseholdsImpl getHouseholds() {
		return households;
	}
	
	public ObjectAttributes getHouseholdAttributes() {
		return householdAttributes;
	}
	
	public ObjectAttributes getPersonAttributes() {
		return personAttributes;
	}
	
}
