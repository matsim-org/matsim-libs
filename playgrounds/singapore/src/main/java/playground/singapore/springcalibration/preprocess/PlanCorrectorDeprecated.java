package playground.singapore.springcalibration.preprocess;

import java.text.DecimalFormat;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;


public class PlanCorrectorDeprecated {
	
	private final static Logger log = Logger.getLogger(PlanCorrectorDeprecated.class);
	private DecimalFormat df = new DecimalFormat("0.00");

	public static void main(String[] args) {
		PlanCorrectorDeprecated corrector = new PlanCorrectorDeprecated();
		corrector.run(args[0], args[1], args[2]);

	}
	
	public void run(String inputFile, String outFile, String populationAttributesFile) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenario).readFile(inputFile);
		
		this.addCarAvailability(scenario.getPopulation(), populationAttributesFile);
		this.convertWalk2Passenger(scenario.getPopulation());
		
		this.writePlans(scenario.getPopulation(), scenario.getNetwork(), outFile);
		log.info("finished");
		
	}
	
	// there are many absurdly long walk legs, which do NOT diminish quick enough -> 
	// convert them to passenger legs (not car legs as passenger is a teleportation mode)
	private void convertWalk2Passenger(Population population) {
		Random random = new Random(); 
		for (Person p : population.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			
//			for (PlanElement pe : plan.getPlanElements()){	
//				if(pe instanceof Leg){
//					String mode = ((Leg) pe).getMode();
//					
//					if (mode.equals(TransportMode.pt)) {					
//						if (PersonUtils.getCarAvail(p).equals("always")) ((Leg) pe).setMode("car");
//						else {
//							if (random.nextFloat() > 0.75) ((Leg) pe).setMode("taxi");
//							else ((Leg) pe).setMode("passenger");
//						}
//					}
// 				}
//			}	
		}
	}
	
	private void addCarAvailability(Population population, String populationAttributesFile) {
		new ObjectAttributesXmlReader(population.getPersonAttributes()).parse(populationAttributesFile);
		
		int hasCar = 0;
		int hasLicense = 0;
		int hasBoth = 0;
		for (Person person : population.getPersons().values()) {		
			// TODO: adapt the replannning module -> PITA
			String carAvail = (String)  population.getPersonAttributes().getAttribute(person.getId().toString(), "car");	
			if (carAvail.equals("0")) PersonUtils.setCarAvail(person, "never");
			else {
				PersonUtils.setCarAvail(person, "always");	
				hasCar++;
			}
			String license = (String)  population.getPersonAttributes().getAttribute(person.getId().toString(), "license");	
			if (license.equals("0")) PersonUtils.setLicence(person, "no");
			else {
				PersonUtils.setLicence(person, "yes");	
				hasLicense++;
			}
			
			if (!carAvail.equals("0") && license.equals("1")) hasBoth++;
			
		}
		double size = population.getPersons().size();
		double shareCar = hasCar / size;
		double shareLicense = hasLicense / size;
		double shareBoth = hasBoth / size;
		log.info("Share of persons with car: " + df.format(shareCar) + " with license: " + df.format(shareLicense) + " both: " + df.format(shareBoth));
	}
	
	private void writePlans(Population population, Network network, String outFile) {
		PopulationWriter writer = new PopulationWriter(population);
		writer.write(outFile);
	}

}
