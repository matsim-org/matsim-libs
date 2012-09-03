package playground.southafrica.affordability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.southafrica.utilities.Header;

public class AccessibilityCalculator {
	private final static Logger LOG = Logger.getLogger(AccessibilityCalculator.class);
	
	private ScenarioImpl sc;
	private Households hhs;
	
	/* Parameter values assumed. */
	private final double WALK_SPEED = 0.8333; /* 3km/h */
	
	/*TODO Remove after validation. */
	private List<Integer> numberInClasses;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AccessibilityCalculator.class.toString(), args);
		
		/* Read households. */
		String householdFile = args[0];
		Households hhs = new HouseholdsImpl();
		HouseholdsReaderV10 hhr = new HouseholdsReaderV10(hhs);
		hhr.readFile(householdFile);
		LOG.info("Number of households: " + hhs.getHouseholds().size());
		
		/* Read population */
		String populationFile = args[1];
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimPopulationReader mpr = new MatsimPopulationReader(sc);
		mpr.readFile(populationFile);
		LOG.info("Number of persons: " + sc.getPopulation().getPersons().size());
		
		/* Read population attributes */
		String personAttributesFile = args[2];
		ObjectAttributes oa = new ObjectAttributes();
		ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(oa);
		oar.parse(personAttributesFile);
		/* Add attributes to population. */
		for(Id id : sc.getPopulation().getPersons().keySet()){
			String hhId = (String) oa.getAttribute(id.toString(), "householdId");
			sc.getPopulation().getPersons().get(id).getCustomAttributes().put("householdId", new IdImpl(hhId));
			Double hhIncome = (Double) oa.getAttribute(id.toString(), "householdIncome");
			sc.getPopulation().getPersons().get(id).getCustomAttributes().put("householdIncome", hhIncome);
			String race = (String) oa.getAttribute(id.toString(), "race");
			sc.getPopulation().getPersons().get(id).getCustomAttributes().put("race", race);
			String school = (String) oa.getAttribute(id.toString(), "school");
			sc.getPopulation().getPersons().get(id).getCustomAttributes().put("school", school);
		}
		LOG.info("Done adding custom attributes: household Id; household income; and race.");
		
		/* Read facilities */
		String facilitiesFile = args[3];
		ActivityFacilities afs = new ActivityFacilitiesImpl();
		FacilitiesReaderMatsimV1 fr = new FacilitiesReaderMatsimV1(sc);
		fr.readFile(facilitiesFile);
		LOG.info("Number of facilities: " + sc.getActivityFacilities().getFacilities().size());
		
		/* Read network */
		String networkFile = args[4];
		MatsimNetworkReader mnr = new MatsimNetworkReader(sc);
		mnr.readFile(networkFile);
		
		AccessibilityCalculator ac = new AccessibilityCalculator(sc, hhs);
		ac.testRun();
		
		Header.printFooter();
	}
	
	
	
	public AccessibilityCalculator(Scenario scenario, Households households) {
		this.sc = (ScenarioImpl) scenario;
		this.hhs = households;
		
		/* Validation */
		numberInClasses = new ArrayList<Integer>();
		for(int i = 0; i < 6; i++){
			numberInClasses.add(0);
		}
	}
	
	
	public void testRun(){
		LOG.info("Start running...");
		Counter counter = new Counter("   person # ");
		for(Person person : this.sc.getPopulation().getPersons().values()){
			calculateAccessibility((PersonImpl)person);
			counter.incCounter();
		}
		counter.printCounter();
		
		LOG.info("----------------------------------------------------");
		LOG.info("Number of persons in different classes:");
		for(int i = 0; i < numberInClasses.size(); i++){
			LOG.info(i + ": " + numberInClasses.get(i));
		}
		LOG.info("----------------------------------------------------");
	}	
	
	public double calculateAccessibility(PersonImpl person){
	
		getMobilityScore(person);
		return 0.0;
	}
	
	
	private double getMobilityScore(PersonImpl person){
		double score = 0;
		int accessibilityClass = getAccessibilityClass(person);
		
		/*TODO Remove after validation */
		int oldValue = numberInClasses.get(accessibilityClass);
		numberInClasses.set(accessibilityClass, oldValue+1);
		
		switch (accessibilityClass) {
		case 1:
			
			break;
		case 2:
			
			break;
		case 3:
			
			break;
		case 4:
			
			break;
		case 5:
			
			break;			
		default:
			break;
		}
		
		return score;
	}
	
	
	private int getAccessibilityClass(PersonImpl person){
	
		/* If the person has a custom attribute for school with value "School" or
		 * "PreSchool", s/he is school-going. 
		 * TODO Can challenge this: if the person is younger than 16, s/he SHOULD 
		 * be going to school... and we treat them as if they WERE school-going. */
		boolean attendSchool = ((String)person.getCustomAttributes().get("school")).contains("School")  ||
				person.getAge() < 16 ? true : false;
		if(attendSchool){
			return 1;
		}
		
		boolean isWorking = activityChainContainsWork(person.getSelectedPlan()) ? true : false;
		boolean isAccompanyingScholar = activityChainContainsDroppingScholar(person.getSelectedPlan());
		
		if(isWorking){
			if(isAccompanyingScholar){
				return 2;
			} else{
				return 3;
			}
		} else{
			if(isAccompanyingScholar){
				return 4;
			} else{
				return 5;
			}
		}
	}
	
	
	private boolean activityChainContainsWork(Plan plan){
		boolean hasWork = false;
		for(PlanElement pe : plan.getPlanElements()){
			if(pe instanceof ActivityImpl){
				ActivityImpl act = (ActivityImpl) pe;
				if(act.getType().contains("w")){
					hasWork = true;
				}
			}
		}
		return hasWork;
	}
	
	
	private boolean activityChainContainsDroppingScholar(Plan plan){
		boolean accompanyingScholar = false;
		for(PlanElement pe : plan.getPlanElements()){
			if(pe instanceof ActivityImpl){
				ActivityImpl act = (ActivityImpl) pe;
				if(act.getType().equalsIgnoreCase("e3")){
					accompanyingScholar = true;
				}
			}
		}
		return accompanyingScholar;
	}
	

}
