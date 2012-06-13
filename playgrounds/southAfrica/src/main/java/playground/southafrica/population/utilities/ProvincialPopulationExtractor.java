package playground.southafrica.population.utilities;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.utilities.Header;

public class ProvincialPopulationExtractor {
	private final static Logger LOG = Logger.getLogger(ProvincialPopulationExtractor.class);
	private final Scenario sc;
	private Households households;
	private ObjectAttributes householdAttributes;
	private ObjectAttributes personAttributes;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ProvincialPopulationExtractor.class.toString(), args);
		
		String networkFile = args[0];
		String inputfolder = args[1];
		String outputFolder = args[2];
		int numberOfThreads = Integer.parseInt(args[3]);
		List<String> startCodes = new ArrayList<String>();
		for(int i = 4; i < args.length; i++){
			startCodes.add(args[i]);
		}
		
		ProvincialPopulationExtractor ppe = new ProvincialPopulationExtractor();
		ppe.extractProvince(networkFile, startCodes, inputfolder, outputFolder, numberOfThreads);
		
		Header.printFooter();
	}
	
	public ProvincialPopulationExtractor() {
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.households = new HouseholdsImpl();
		this.householdAttributes = new ObjectAttributes();
		this.personAttributes = new ObjectAttributes();
	}
	
	public void extractProvince(String network, List<String> provincialCodes, String inputFolder, String outputFolder, int threads3){
		Census2001SampleReader cr = new Census2001SampleReader();
		cr.parse(inputFolder);
		
		/* Read the network */
		MatsimNetworkReader nr = new MatsimNetworkReader(cr.getScenario());
		nr.parse(network);
		
		/* Households */
		LOG.info("Evaluating households in province code(s)...");
		Counter householdCounter = new Counter("   households evaluated # ");
		for(Id id : cr.getHouseholds().getHouseholds().keySet()){
			boolean inProvince = false;
			int index = 0;
			while(!inProvince && index < provincialCodes.size()){
				if(((String)cr.getHouseholdAttributes().getAttribute(id.toString(), "provinceCode")).equalsIgnoreCase(provincialCodes.get(index))){
					inProvince = true;
				} else{
					index++;
				}
			}
			if(inProvince){
				Household hh = cr.getHouseholds().getHouseholds().get(id);
				households.getHouseholds().put(id, hh);
				
				householdAttributes.putAttribute(id.toString(), "dwellingType", 
						cr.getHouseholdAttributes().getAttribute(id.toString(), "dwellingType"));
				householdAttributes.putAttribute(id.toString(), "householdSize", 
						cr.getHouseholdAttributes().getAttribute(id.toString(), "householdSize"));
				householdAttributes.putAttribute(id.toString(), "population", 
						cr.getHouseholdAttributes().getAttribute(id.toString(), "population"));
				householdAttributes.putAttribute(id.toString(), "municipalCode", 
						cr.getHouseholdAttributes().getAttribute(id.toString(), "municipalCode"));
				householdAttributes.putAttribute(id.toString(), "municipalName", 
						cr.getHouseholdAttributes().getAttribute(id.toString(), "municipalName"));
				householdAttributes.putAttribute(id.toString(), "magisterialCode", 
						cr.getHouseholdAttributes().getAttribute(id.toString(), "magisterialCode"));
				householdAttributes.putAttribute(id.toString(), "magisterialName", 
						cr.getHouseholdAttributes().getAttribute(id.toString(), "magisterialName"));
				householdAttributes.putAttribute(id.toString(), "districtCode", 
						cr.getHouseholdAttributes().getAttribute(id.toString(), "districtCode"));
				householdAttributes.putAttribute(id.toString(), "districtName", 
						cr.getHouseholdAttributes().getAttribute(id.toString(), "districtName"));
				householdAttributes.putAttribute(id.toString(), "provinceCode", 
						cr.getHouseholdAttributes().getAttribute(id.toString(), "provinceCode"));
				householdAttributes.putAttribute(id.toString(), "provinceName", 
						cr.getHouseholdAttributes().getAttribute(id.toString(), "provinceName"));
				householdAttributes.putAttribute(id.toString(), "eaType", 
						cr.getHouseholdAttributes().getAttribute(id.toString(), "eaType"));
			}
			householdCounter.incCounter();
		}
		householdCounter.printCounter();
		
		/* Persons */
		LOG.info("Evaluating households in province code(s)...");
		Counter personCounter = new Counter("   persons evaluated # ");
		for(Id id : cr.getScenario().getPopulation().getPersons().keySet()){
			boolean inProvince = false;
			int index = 0;
			while(!inProvince && index < provincialCodes.size()){
				if(id.toString().startsWith(provincialCodes.get(index))){
					inProvince = true;
				} else{
					index++;
				}
			}
			if(inProvince){
				Person p = cr.getScenario().getPopulation().getPersons().get(id);
				sc.getPopulation().addPerson(p);
				
				personAttributes.putAttribute(id.toString(), "income", 
						cr.getPersonAttributes().getAttribute(id.toString(), "income"));
				personAttributes.putAttribute(id.toString(), "mainPlaceOfWork", 
						cr.getPersonAttributes().getAttribute(id.toString(), "mainPlaceOfWork"));
				personAttributes.putAttribute(id.toString(), "modeToMain", 
						cr.getPersonAttributes().getAttribute(id.toString(), "modeToMain"));
				personAttributes.putAttribute(id.toString(), "quarterType", 
						cr.getPersonAttributes().getAttribute(id.toString(), "quarterType"));
				personAttributes.putAttribute(id.toString(), "race", 
						cr.getPersonAttributes().getAttribute(id.toString(), "race"));
				personAttributes.putAttribute(id.toString(), "relationship", 
						cr.getPersonAttributes().getAttribute(id.toString(), "relationship"));
				personAttributes.putAttribute(id.toString(), "school", 
						cr.getPersonAttributes().getAttribute(id.toString(), "school"));
				personCounter.incCounter();
			}
			personCounter.printCounter();
		}
		
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
		if(this.households == null || this.householdAttributes == null){
			Gbl.errorMsg("Either no households or household attributes to write.");
		} else{
			LOG.info("Writing households to file... (" + this.households.getHouseholds().size() + ")");
			HouseholdsWriterV10 hw = new HouseholdsWriterV10(this.households);
			hw.setPrettyPrint(true);
			hw.writeFile(outputfolder + "Households.xml");

			LOG.info("Writing household attributes to file...");
			ObjectAttributesXmlWriter oaw = new ObjectAttributesXmlWriter(householdAttributes);
			oaw.setPrettyPrint(true);
			oaw.writeFile(outputfolder + "HouseholdAttributes.xml");
		}
	}

	
	/**
 	 * Writes the population and their attributes to file.
	 * @param outputfolder
	 */
	public void writePopulation(String outputfolder){
		if(this.sc.getPopulation().getPersons().size() == 0 || this.personAttributes == null){
			Gbl.errorMsg("Either no persons or person attributes to write.");
		} else{
			LOG.info("Writing population to file... (" + this.sc.getPopulation().getPersons().size() + ")");
			PopulationWriter pw = new PopulationWriter(this.sc.getPopulation(), this.sc.getNetwork());
			pw.writeV5(outputfolder + "Population.xml");

			LOG.info("Writing person attributes to file...");
			ObjectAttributesXmlWriter oaw = new ObjectAttributesXmlWriter(this.personAttributes);
			oaw.setPrettyPrint(true);
			oaw.writeFile(outputfolder + "PersonAttributes.xml");
		}
	}


}
