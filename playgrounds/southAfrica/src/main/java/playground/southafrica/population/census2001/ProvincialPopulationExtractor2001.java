package playground.southafrica.population.census2001;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.population.utilities.ComprehensivePopulationReader;
import playground.southafrica.utilities.Header;

public class ProvincialPopulationExtractor2001 {
	private final static Logger LOG = Logger.getLogger(ProvincialPopulationExtractor2001.class);
	private final Scenario sc;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ProvincialPopulationExtractor2001.class.toString(), args);
		
		String networkFile = args[0];
		String inputfolder = args[1];
		String outputFolder = args[2];
		List<String> startCodes = new ArrayList<String>();
		for(int i = 3; i < args.length; i++){
			startCodes.add(args[i]);
		}
		
		ProvincialPopulationExtractor2001 ppe = new ProvincialPopulationExtractor2001();
		ppe.extractProvince(networkFile, startCodes, inputfolder, outputFolder);
		
		Header.printFooter();
	}
	
	public ProvincialPopulationExtractor2001() {
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}
	
	public void extractProvince(String network, List<String> provincialCodes, String inputFolder, String outputFolder){
		ComprehensivePopulationReader cr = new ComprehensivePopulationReader();
		cr.parse(inputFolder);
		
		/* Read the network */
		MatsimNetworkReader nr = new MatsimNetworkReader(cr.getScenario());
		nr.parse(network);
		
		/* Households */
		LOG.info("Evaluating households in province code(s)...");
		Counter householdCounter = new Counter("   households evaluated # ");
		for(Id id : cr.getScenario().getHouseholds().getHouseholds().keySet()){
			boolean inProvince = false;
			int index = 0;
			while(!inProvince && index < provincialCodes.size()){
				if(((String)cr.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(id.toString(), "provinceCode")).equalsIgnoreCase(provincialCodes.get(index))){
					inProvince = true;
				} else{
					index++;
				}
			}
			if(inProvince){
				/* Copy the household */
				Household hh = cr.getScenario().getHouseholds().getHouseholds().get(id);
				sc.getHouseholds().getHouseholds().put(id, hh);
				
				sc.getHouseholds().getHouseholdAttributes().putAttribute(id.toString(), "dwellingType", 
						cr.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(id.toString(), "dwellingType"));
				sc.getHouseholds().getHouseholdAttributes().putAttribute(id.toString(), "householdSize", 
						cr.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(id.toString(), "householdSize"));
				sc.getHouseholds().getHouseholdAttributes().putAttribute(id.toString(), "population", 
						cr.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(id.toString(), "population"));
				sc.getHouseholds().getHouseholdAttributes().putAttribute(id.toString(), "municipalCode", 
						cr.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(id.toString(), "municipalCode"));
				sc.getHouseholds().getHouseholdAttributes().putAttribute(id.toString(), "municipalName", 
						cr.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(id.toString(), "municipalName"));
				sc.getHouseholds().getHouseholdAttributes().putAttribute(id.toString(), "magisterialCode", 
						cr.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(id.toString(), "magisterialCode"));
				sc.getHouseholds().getHouseholdAttributes().putAttribute(id.toString(), "magisterialName", 
						cr.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(id.toString(), "magisterialName"));
				sc.getHouseholds().getHouseholdAttributes().putAttribute(id.toString(), "districtCode", 
						cr.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(id.toString(), "districtCode"));
				sc.getHouseholds().getHouseholdAttributes().putAttribute(id.toString(), "districtName", 
						cr.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(id.toString(), "districtName"));
				sc.getHouseholds().getHouseholdAttributes().putAttribute(id.toString(), "provinceCode", 
						cr.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(id.toString(), "provinceCode"));
				sc.getHouseholds().getHouseholdAttributes().putAttribute(id.toString(), "provinceName", 
						cr.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(id.toString(), "provinceName"));
				sc.getHouseholds().getHouseholdAttributes().putAttribute(id.toString(), "eaType", 
						cr.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(id.toString(), "eaType"));
				
				/* Copy the members of the household */
				for(Id memberId : hh.getMemberIds()){
					Person p = cr.getScenario().getPopulation().getPersons().get(memberId);
					sc.getPopulation().addPerson(p);
					
					sc.getHouseholds().getHouseholdAttributes().putAttribute(memberId.toString(), "income", 
							cr.getScenario().getPopulation().getPersonAttributes().getAttribute(memberId.toString(), "income"));
					sc.getHouseholds().getHouseholdAttributes().putAttribute(memberId.toString(), "mainPlaceOfWork", 
							cr.getScenario().getPopulation().getPersonAttributes().getAttribute(memberId.toString(), "mainPlaceOfWork"));
					sc.getHouseholds().getHouseholdAttributes().putAttribute(memberId.toString(), "modeToMain", 
							cr.getScenario().getPopulation().getPersonAttributes().getAttribute(memberId.toString(), "modeToMain"));
					sc.getHouseholds().getHouseholdAttributes().putAttribute(memberId.toString(), "quarterType", 
							cr.getScenario().getPopulation().getPersonAttributes().getAttribute(memberId.toString(), "quarterType"));
					sc.getHouseholds().getHouseholdAttributes().putAttribute(memberId.toString(), "race", 
							cr.getScenario().getPopulation().getPersonAttributes().getAttribute(memberId.toString(), "race"));
					sc.getHouseholds().getHouseholdAttributes().putAttribute(memberId.toString(), "relationship", 
							cr.getScenario().getPopulation().getPersonAttributes().getAttribute(memberId.toString(), "relationship"));
					sc.getHouseholds().getHouseholdAttributes().putAttribute(memberId.toString(), "school", 
							cr.getScenario().getPopulation().getPersonAttributes().getAttribute(memberId.toString(), "school"));
				}
			}
			householdCounter.incCounter();
		}
		householdCounter.printCounter();
				
		writePopulationAndAttributes(outputFolder);
	}
	
	public void writePopulationAndAttributes(String outputFolder){
		writeHouseholds(outputFolder);
		writePopulation(outputFolder);
	}
	
	/**
 	 * Writes the households and their attributes to file.
	 * @param outputfolder
	 */
	public void writeHouseholds(String outputfolder){
		if(this.sc.getHouseholds() == null || this.sc.getHouseholds().getHouseholdAttributes() == null){
			throw new RuntimeException("Either no households or household attributes to write.");
		} else{
			LOG.info("Writing households to file... (" + this.sc.getHouseholds().getHouseholds().size() + ")");
			HouseholdsWriterV10 hw = new HouseholdsWriterV10(this.sc.getHouseholds());
			hw.setPrettyPrint(true);
			hw.writeFile(outputfolder + "Households.xml");

			LOG.info("Writing household attributes to file...");
			ObjectAttributesXmlWriter oaw = new ObjectAttributesXmlWriter(sc.getHouseholds().getHouseholdAttributes());
			oaw.setPrettyPrint(true);
			oaw.writeFile(outputfolder + "HouseholdAttributes.xml");
		}
	}

	
	/**
 	 * Writes the population and their attributes to file.
	 * @param outputfolder
	 */
	public void writePopulation(String outputfolder){
		if(sc.getPopulation().getPersons().size() == 0 || sc.getPopulation().getPersonAttributes() == null){
			throw new RuntimeException("Either no persons or person attributes to write.");
		} else{
			LOG.info("Writing population to file... (" + sc.getPopulation().getPersons().size() + ")");
			PopulationWriter pw = new PopulationWriter(sc.getPopulation(), sc.getNetwork());
			pw.writeV5(outputfolder + "Population.xml");

			LOG.info("Writing person attributes to file...");
			ObjectAttributesXmlWriter oaw = new ObjectAttributesXmlWriter(sc.getPopulation().getPersonAttributes());
			oaw.setPrettyPrint(true);
			oaw.writeFile(outputfolder + "PersonAttributes.xml");
		}
	}


}
