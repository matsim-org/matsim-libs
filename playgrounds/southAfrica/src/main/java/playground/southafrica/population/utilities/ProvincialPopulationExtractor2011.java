package playground.southafrica.population.utilities;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.households.Income;
import org.matsim.households.IncomeImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.utilities.Header;

/**
 * Reads a comprehensive population, made up of a person file, person attributes
 * file, household file and household attributes file, and extracting all those
 * members that fall within a given province. 
 *
 * @author jwjoubert
 */
public class ProvincialPopulationExtractor2011 {
	private final static Logger LOG = Logger.getLogger(ProvincialPopulationExtractor2011.class);
	private final Scenario sc;
	private Households households;
	private ObjectAttributes householdAttributes;
	private ObjectAttributes personAttributes;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ProvincialPopulationExtractor2011.class.toString(), args);
		
		String inputFolder = args[0];
		String provinceName = args[1];
		String provinceCode = args[2];
		
		/* Before anything else, first check if the output folder already exists. */
		String path = inputFolder.endsWith("/") ? "" : "/";
		File outputFolder = new File(inputFolder + path + provinceName + path);
		if(outputFolder.exists()){
			throw new RuntimeException("The output folder already exists and will not be overwritten. First delete " + 
					outputFolder.getAbsolutePath());
		} 
		
		ProvincialPopulationExtractor2011 ppe = new ProvincialPopulationExtractor2011();
		ppe.extractProvince(inputFolder, provinceCode);
		
		try {
			ppe.writePopulationAndAttributes(inputFolder, provinceName);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Must first delete output folder.");
		}
		
		Header.printFooter();
	}
	
	
	public ProvincialPopulationExtractor2011() {
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.households = new HouseholdsImpl();
		this.householdAttributes = new ObjectAttributes();
		this.personAttributes = new ObjectAttributes();
	}
	
	
	/**
	 * Extract all those households with a municipal code that starts with the
	 * province code provided. Household attributes, all household members, as
	 * well as their person attributes are copied.
	 *  
	 * @param inputFolder
	 * @param provinceCode
	 */
	public void extractProvince(String inputFolder, String provinceCode){
		ComprehensivePopulationReader cr = new ComprehensivePopulationReader();
		cr.parse(inputFolder);
		
		/* Households */
		LOG.info("Evaluating households in province code(s)...");
		Counter householdCounter = new Counter("   households evaluated # ");
		for(Id id : cr.getHouseholds().getHouseholds().keySet()){
			boolean inProvince = false;
			String thisHouseholdProvinceCode = (String)cr.getHouseholdAttributes().getAttribute(id.toString(), "provinceCode");
			if(thisHouseholdProvinceCode.startsWith(provinceCode)){
				inProvince = true;
			}
			if(inProvince){
				/* Copy the household */
				Household hh = cr.getHouseholds().getHouseholds().get(id);
				households.getHouseholds().put(id, hh);
				
				householdAttributes.putAttribute(id.toString(), "householdSize", 
						cr.getHouseholdAttributes().getAttribute(id.toString(), "householdSize"));
				householdAttributes.putAttribute(id.toString(), "population", 
						cr.getHouseholdAttributes().getAttribute(id.toString(), "population"));
				householdAttributes.putAttribute(id.toString(), "housingType", 
						cr.getHouseholdAttributes().getAttribute(id.toString(), "housingType"));
				householdAttributes.putAttribute(id.toString(), "mainDwellingType", 
						cr.getHouseholdAttributes().getAttribute(id.toString(), "mainDwellingType"));
				householdAttributes.putAttribute(id.toString(), "municipalCode", 
						cr.getHouseholdAttributes().getAttribute(id.toString(), "municipalCode"));
				householdAttributes.putAttribute(id.toString(), "districtCode", 
						cr.getHouseholdAttributes().getAttribute(id.toString(), "districtCode"));
				
				/* Copy the members of the household */
				for(Id memberId : hh.getMemberIds()){
					Person p = cr.getScenario().getPopulation().getPersons().get(memberId);
					sc.getPopulation().addPerson(p);
					
					personAttributes.putAttribute(memberId.toString(), "race", 
							cr.getPersonAttributes().getAttribute(memberId.toString(), "race"));
					personAttributes.putAttribute(memberId.toString(), "relationship", 
							cr.getPersonAttributes().getAttribute(memberId.toString(), "relationship"));
					personAttributes.putAttribute(memberId.toString(), "school", 
							cr.getPersonAttributes().getAttribute(memberId.toString(), "school"));

					/* There is a possibility that no income attribute exists.
					 * That will happen if the income was originally 'Unknown'. */
					Object incomeObject = cr.getPersonAttributes().getAttribute(memberId.toString(), "income");
					if(incomeObject != null && incomeObject instanceof Income){
						personAttributes.putAttribute(memberId.toString(), "income", (IncomeImpl)incomeObject);
					}
				}
			}
			householdCounter.incCounter();
		}
		householdCounter.printCounter();
	}

	
	/**
	 * Class to handle the checking and creation of the output folder. It is 
	 * assumed that a province-specific folder will be created in the input 
	 * folder. 
	 * 
	 * @param inputFolder
	 * @param provinceName
	 * @throws IOException if the output folder already exists for the province.
	 */
	public void writePopulationAndAttributes(String inputFolder, String provinceName) throws IOException{
		/* Check that the province-specific folder exists. If is does, it should
		 * NOT be overwritten, and should throw an exception. If it doesn't 
		 * exist, create it. */
		String path = inputFolder.endsWith("/") ? "" : "/";
		File outputFolder = new File(inputFolder + path + provinceName + path);
		if(outputFolder.exists()){
			throw new IOException("The output folder already exists and will not be overwritten. First delete " + 
					outputFolder.getAbsolutePath());
		} else{
			outputFolder.mkdirs();
		}
		
		writeHouseholds(outputFolder.getAbsolutePath());
		writePopulation(outputFolder.getAbsolutePath());
	}
	
	
	/**
 	 * Writes the households and their attributes to file.
	 * @param outputfolder
	 */
	public void writeHouseholds(String outputfolder){
		/* Ensure output folder ends with a slash. */
		outputfolder = outputfolder + (outputfolder.endsWith("/") ? "" : "/");
		
		if(this.households == null || this.householdAttributes == null){
			throw new RuntimeException("Either no households or household attributes to write.");
		} else{
			LOG.info("Writing households to file... (" + this.households.getHouseholds().size() + ")");
			HouseholdsWriterV10 hw = new HouseholdsWriterV10(this.households);
			hw.setPrettyPrint(true);
			hw.writeFile(outputfolder + "Households.xml");

			LOG.info("Writing household attributes to file...");
			ObjectAttributesXmlWriter oaw = new ObjectAttributesXmlWriter(householdAttributes);
			/* Set up the income converter */
			oaw.putAttributeConverter(IncomeImpl.class, new SAIncomeConverter());
			oaw.setPrettyPrint(true);
			oaw.writeFile(outputfolder + "HouseholdAttributes.xml");
		}
	}

	
	/**
 	 * Writes the population and their attributes to file.
	 * @param outputfolder
	 */
	public void writePopulation(String outputfolder){
		/* Ensure output folder ends with a slash. */
		outputfolder = outputfolder + (outputfolder.endsWith("/") ? "" : "/");
		
		if(this.sc.getPopulation().getPersons().size() == 0 || this.personAttributes == null){
			throw new RuntimeException("Either no persons or person attributes to write.");
		} else{
			LOG.info("Writing population to file... (" + this.sc.getPopulation().getPersons().size() + ")");
			PopulationWriter pw = new PopulationWriter(this.sc.getPopulation(), this.sc.getNetwork());
			pw.writeV5(outputfolder + "Population.xml");

			LOG.info("Writing person attributes to file...");
			ObjectAttributesXmlWriter oaw = new ObjectAttributesXmlWriter(this.personAttributes);
			/* Set up the income converter */
			oaw.putAttributeConverter(IncomeImpl.class, new SAIncomeConverter());
			oaw.setPrettyPrint(true);
			oaw.writeFile(outputfolder + "PersonAttributes.xml");
		}
	}
	
	


}
