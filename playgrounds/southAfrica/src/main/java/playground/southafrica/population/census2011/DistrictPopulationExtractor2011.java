package playground.southafrica.population.census2011;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.households.IncomeImpl;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.population.utilities.ComprehensivePopulationReader;
import playground.southafrica.population.utilities.SAIncomeConverter;
import playground.southafrica.utilities.Header;

/**
 * Reads a comprehensive population, made up of a person file, person attributes
 * file, household file and household attributes file, and extracting all those
 * members that fall within a given province. 
 *
 * @author jwjoubert
 */
public class DistrictPopulationExtractor2011 {
	private final static Logger LOG = Logger.getLogger(DistrictPopulationExtractor2011.class);
	private final Scenario sc;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(DistrictPopulationExtractor2011.class.toString(), args);
		
		String inputFolder = args[0];
		String areaName = args[1];
		
		/* The remainder of the arguments form a list of district codes. */
		List<String> districtCodes = new ArrayList<String>();
		int index = 2;
		while(index < args.length){
			districtCodes.add(args[index++]);
		}
		if(districtCodes.size() == 0){
			throw new IllegalArgumentException("At least one district code must be provided as argument.");
		}
		
		/* Before anything else, first check if the output folder already exists. */
		String path = inputFolder.endsWith("/") ? "" : "/";
		File outputFolder = new File(inputFolder + path + areaName + path);
		if(outputFolder.exists()){
			throw new RuntimeException("The output folder already exists and will not be overwritten. First delete " + 
					outputFolder.getAbsolutePath());
		} 
		
		DistrictPopulationExtractor2011 ppe = new DistrictPopulationExtractor2011();
		ppe.extractProvince(inputFolder, districtCodes);
		
		try {
			ppe.writePopulationAndAttributes(inputFolder, areaName);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Must first delete output folder.");
		}
		
		Header.printFooter();
	}
	
	
	public DistrictPopulationExtractor2011() {
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}
	
	
	/**
	 * Extract all those households with a municipal code that starts with the
	 * province code provided. Household attributes, all household members, as
	 * well as their person attributes are copied.
	 *  
	 * @param inputFolder
	 * @param provinceCode
	 */
	public void extractProvince(String inputFolder, List<String> codes){
		ComprehensivePopulationReader cr = new ComprehensivePopulationReader();
		cr.parse(inputFolder);
		
		/* Households */
		LOG.info("Evaluating households in province code(s)...");
		Counter householdCounter = new Counter("   households evaluated # ");
		for(Id id : cr.getScenario().getHouseholds().getHouseholds().keySet()){
			String thisHouseholdDistrictCode = (String)cr.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(id.toString(), "districtCode");
			
			boolean inArea = false;
			int index = 0;
			while(!inArea && index < codes.size()){
				if(thisHouseholdDistrictCode.startsWith(codes.get(index++))){
					inArea = true;
				}
			}
			if(inArea){
				/* Copy the household */
				Household hh = cr.getScenario().getHouseholds().getHouseholds().get(id);
				sc.getHouseholds().getHouseholds().put(id, hh);
				
				String[] attributes_household = {"householdSize", "population", "housingType", 
						"mainDwellingType", "municipalCode", "districtCode", "provinceCode"};

				for(String attribute : attributes_household){
					Object currentAttribute = cr.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(id.toString(), attribute);
					if(currentAttribute != null){
						sc.getHouseholds().getHouseholdAttributes().putAttribute(id.toString(), attribute, currentAttribute);
					}
				}
				
				/* Copy the members of the household */
				for(Id memberId : hh.getMemberIds()){
					Person p = cr.getScenario().getPopulation().getPersons().get(memberId);
					sc.getPopulation().addPerson(p);
					
					String[] attributes_person = {"race", "relationship", "school", "income"};
					
					for(String attribute : attributes_person){
						Object currentAttribute = cr.getScenario().getPopulation().getPersonAttributes().getAttribute(memberId.toString(), attribute);
						if(currentAttribute != null){
							sc.getPopulation().getPersonAttributes().putAttribute(memberId.toString(), attribute, currentAttribute);
						}
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
		
		if(sc.getHouseholds() == null || sc.getHouseholds().getHouseholdAttributes() == null){
			throw new RuntimeException("Either no households or household attributes to write.");
		} else{
			LOG.info("Writing households to file... (" + sc.getHouseholds().getHouseholds().size() + ")");
			HouseholdsWriterV10 hw = new HouseholdsWriterV10(sc.getHouseholds());
			hw.setPrettyPrint(true);
			hw.writeFile(outputfolder + "Households.xml");

			LOG.info("Writing household attributes to file...");
			ObjectAttributesXmlWriter oaw = new ObjectAttributesXmlWriter(sc.getHouseholds().getHouseholdAttributes());
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
		
		if(sc.getPopulation().getPersons().size() == 0 || sc.getPopulation().getPersonAttributes() == null){
			throw new RuntimeException("Either no persons or person attributes to write.");
		} else{
			LOG.info("Writing population to file... (" + sc.getPopulation().getPersons().size() + ")");
			PopulationWriter pw = new PopulationWriter(sc.getPopulation(), sc.getNetwork());
			pw.writeV5(outputfolder + "Population.xml");

			LOG.info("Writing person attributes to file...");
			ObjectAttributesXmlWriter oaw = new ObjectAttributesXmlWriter(sc.getPopulation().getPersonAttributes());
			/* Set up the income converter */
			oaw.putAttributeConverter(IncomeImpl.class, new SAIncomeConverter());
			oaw.setPrettyPrint(true);
			oaw.writeFile(outputfolder + "PersonAttributes.xml");
		}
	}
	
	


}
