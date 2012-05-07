package playground.southafrica.population.utilities;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.southafrica.utilities.Header;

public class Census2001SampleReader {
	private static final Logger LOG = Logger.getLogger(Census2001SampleParser.class);
	private Scenario sc;
	private HouseholdsImpl households;
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
		PopulationReaderMatsimV5 pr = new PopulationReaderMatsimV5(this.sc);
		pr.parse(inputfolder + "Population.xml");
		
		/* Read population attributes. */
		ObjectAttributes populationAttributes = new ObjectAttributes();
		ObjectAttributesXmlReader oar1 = new ObjectAttributesXmlReader(populationAttributes);
		oar1.parse(inputfolder + "PersonAttributes.xml");
		
		/* Read households */
		households = new HouseholdsImpl();
		HouseholdsReaderV10 hhr = new HouseholdsReaderV10(households);
		hhr.parse(inputfolder + "Households.xml");
		
		/* Read household attributes. */ 
		ObjectAttributes householdAttributes = new ObjectAttributes();
		ObjectAttributesXmlReader oar2 = new ObjectAttributesXmlReader(householdAttributes);
		oar1.parse(inputfolder + "householdAttributes.xml");

	}

}
