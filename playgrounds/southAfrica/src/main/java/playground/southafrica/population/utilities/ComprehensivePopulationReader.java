package playground.southafrica.population.utilities;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.households.IncomeImpl;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.southafrica.population.census2001.Census2001SampleParser;
import playground.southafrica.population.census2011.attributeConverters.CoordConverter;
import playground.southafrica.population.nmbmTravelSurvey.NmbmSurveyParser;
import playground.southafrica.utilities.Header;

/**
 * Reading a comprehensive population that contains (all) four population-
 * related files: a persons file, a person attributes file, household file, and
 * household attributes file.
 *
 * @author jwjoubert
 */
public class ComprehensivePopulationReader {
	private static final Logger LOG = Logger.getLogger(Census2001SampleParser.class);
	private Scenario sc;

	public static void main(String[] args) {
		Header.printHeader(ComprehensivePopulationReader.class.toString(), args);
		
		String inputFolder = args[0];
		ComprehensivePopulationReader csr = new ComprehensivePopulationReader();
		csr.parse(inputFolder);
		
		Header.printFooter();
	}
	
	/**
	 * Class to read a set of four population-related files:
	 * <ol>
	 * 		<li> <code>population.xml</code>
	 * 		<li> <code>personAttributes.xml</code>
	 * 		<li> <code>households.xml</code>
	 * 		<li> <code>householdAttributes.xml</code>
	 * </ol>
	 * These files are usually the result from census or travel survey data, 
	 * for example using {@link Census2001SampleParser} or {@link NmbmSurveyParser}.
	 * @param args
	 */
	public ComprehensivePopulationReader() {
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}
	
	public void parse(String inputfolder){
		/* Check that the given folder ends with a '/'. */
		inputfolder = inputfolder + (inputfolder.endsWith("/") ? "" : "/");
		
		/* Read population. */
		LOG.info("Reading population...");
		PopulationReaderMatsimV5 pr = new PopulationReaderMatsimV5(this.sc);
		pr.parse(inputfolder + "population.xml.gz");
		
		/* Read population attributes. */
		LOG.info("Reading person attributes...");
		ObjectAttributesXmlReader oar1 = new ObjectAttributesXmlReader(this.sc.getPopulation().getPersonAttributes());
		oar1.putAttributeConverter(IncomeImpl.class, new SAIncomeConverter());
		oar1.parse(inputfolder + "populationAttributes.xml.gz");
		
		/* Read households */
		LOG.info("Reading households...");
		HouseholdsReaderV10 hhr = new HouseholdsReaderV10(this.sc.getHouseholds());
		hhr.parse(inputfolder + "households.xml.gz");
		
		/* Read household attributes. */ 
		LOG.info("Reading household attributes...");
		ObjectAttributesXmlReader oar2 = new ObjectAttributesXmlReader(this.sc.getHouseholds().getHouseholdAttributes());
		oar2.putAttributeConverter(IncomeImpl.class, new SAIncomeConverter());
		oar2.putAttributeConverter(Coord.class, new CoordConverter());
		oar2.parse(inputfolder + "householdAttributes.xml.gz");

		LOG.info("================================================================");
		LOG.info("Population size: " + sc.getPopulation().getPersons().size());
		LOG.info("Number of households: " + sc.getHouseholds().getHouseholds().size());
		LOG.info("================================================================");
	}

	public Scenario getScenario() {
		return sc;
	}
		
}
