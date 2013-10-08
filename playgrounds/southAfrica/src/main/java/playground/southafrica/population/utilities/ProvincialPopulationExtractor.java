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
		List<String> startCodes = new ArrayList<String>();
		for(int i = 3; i < args.length; i++){
			startCodes.add(args[i]);
		}
		
		ProvincialPopulationExtractor ppe = new ProvincialPopulationExtractor();
		ppe.extractProvince(networkFile, startCodes, inputfolder, outputFolder);
		
		Header.printFooter();
	}
	
	public ProvincialPopulationExtractor() {
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.households = new HouseholdsImpl();
		this.householdAttributes = new ObjectAttributes();
		this.personAttributes = new ObjectAttributes();
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
				/* Copy the household */
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
				
				/* Copy the members of the household */
				for(Id memberId : hh.getMemberIds()){
					Person p = cr.getScenario().getPopulation().getPersons().get(memberId);
					sc.getPopulation().addPerson(p);
					
					personAttributes.putAttribute(memberId.toString(), "income", 
							cr.getPersonAttributes().getAttribute(memberId.toString(), "income"));
					personAttributes.putAttribute(memberId.toString(), "mainPlaceOfWork", 
							cr.getPersonAttributes().getAttribute(memberId.toString(), "mainPlaceOfWork"));
					personAttributes.putAttribute(memberId.toString(), "modeToMain", 
							cr.getPersonAttributes().getAttribute(memberId.toString(), "modeToMain"));
					personAttributes.putAttribute(memberId.toString(), "quarterType", 
							cr.getPersonAttributes().getAttribute(memberId.toString(), "quarterType"));
					personAttributes.putAttribute(memberId.toString(), "race", 
							cr.getPersonAttributes().getAttribute(memberId.toString(), "race"));
					personAttributes.putAttribute(memberId.toString(), "relationship", 
							cr.getPersonAttributes().getAttribute(memberId.toString(), "relationship"));
					personAttributes.putAttribute(memberId.toString(), "school", 
							cr.getPersonAttributes().getAttribute(memberId.toString(), "school"));
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
		if(this.households == null || this.householdAttributes == null){
			throw new RuntimeException("Either no households or household attributes to write.");
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
			throw new RuntimeException("Either no persons or person attributes to write.");
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
