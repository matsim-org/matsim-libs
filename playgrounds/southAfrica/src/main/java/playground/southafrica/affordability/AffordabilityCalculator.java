package playground.southafrica.affordability;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.households.Income;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.SouthAfricaInflationCorrector;

public class AffordabilityCalculator {
	private final static Logger LOG = Logger.getLogger(AccessibilityCalculator.class);
	private Scenario sc;
	private Households hhs;
	private Map<Id, Double> hhScore;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AffordabilityCalculator.class.toString(), args);

		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(config);
		
		/* Read households. */
		String householdFile = args[0];
		Households hhs = new HouseholdsImpl();
		HouseholdsReaderV10 hhr = new HouseholdsReaderV10(hhs);
		hhr.readFile(householdFile);
		LOG.info("Number of households: " + hhs.getHouseholds().size());
		
		/* Read population */
		String populationFile = args[1];
		sc.getTransitSchedule();
		MatsimPopulationReader mpr = new MatsimPopulationReader(sc);
		mpr.readFile(populationFile);
		LOG.info("Number of persons: " + sc.getPopulation().getPersons().size());
		
		/* Read population attributes */
		String personAttributesFile = args[2];
		ObjectAttributes oa = new ObjectAttributes();
		ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(oa);
		oar.parse(personAttributesFile);
		/* Add attributes to population. */
		for(Id<Person> id : sc.getPopulation().getPersons().keySet()){
			String hhId = (String) oa.getAttribute(id.toString(), "householdId");
			sc.getPopulation().getPersons().get(id).getCustomAttributes().put("householdId", Id.create(hhId, Household.class));
			Double hhIncome = (Double) oa.getAttribute(id.toString(), "householdIncome");
			sc.getPopulation().getPersons().get(id).getCustomAttributes().put("householdIncome", hhIncome);
			String race = (String) oa.getAttribute(id.toString(), "race");
			sc.getPopulation().getPersons().get(id).getCustomAttributes().put("race", race);
			String school = (String) oa.getAttribute(id.toString(), "school");
			sc.getPopulation().getPersons().get(id).getCustomAttributes().put("school", school);
		}
		LOG.info("Done adding custom attributes: household Id; household income; and race.");

		AffordabilityCalculator ac = new AffordabilityCalculator(sc, hhs);
		ac.readHouseholdAccessibility(args[3]);
		ac.run(args[4]);
		
		
		
		Header.printFooter();
	}
	
	
	
	public AffordabilityCalculator(Scenario scenario, Households households) {
		this.sc = scenario;
		this.hhs = households;
		
		
	}
	
	
	public void readHouseholdAccessibility(String filename){
		LOG.info("Reading household accessibility from " + filename);
		hhScore = new TreeMap<Id, Double>();
		Counter counter = new Counter("   household # ");
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = br.readLine(); /* Header. */
			while((line = br.readLine()) != null){
				String [] sa = line.split(",");
				Id<Household> hhId = Id.create(sa[0], Household.class);
				double score = Double.parseDouble(sa[3]);
				Household hh = this.hhs.getHouseholds().get(hhId);
				if(hh != null){
					hhScore.put(hhId, score);
				} else{
					LOG.warn("Couldn't find household " + hhId.toString() + ". Ignored.");
				}
				counter.incCounter();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			};
		}
		counter.printCounter();
		LOG.info("Done reading household accessibility.");
	}
	
	
	public void run(String outputFolder){
		LOG.info("Running... (" + hhs.getHouseholds().size() + ")");
		Counter counter = new Counter("  household # ");
		
		
		String bwName = outputFolder + "householdAffordability.csv";
		BufferedWriter bw = IOUtils.getBufferedWriter(bwName);
		try{
			bw.write("Id,Income,Access,AccessClass,IncomeClass,hhClass,TimeA,IncA,Long,Lat");
			bw.newLine();
			for(Id hhId : hhScore.keySet()){
				Household hh = hhs.getHouseholds().get(hhId);

				/* Find an individual in the household to get their home coordinate. */
				List<Id<Person>> members = hh.getMemberIds();
				Person person = null;
				int index = 0;
				while(person == null && index < members.size()){
					person = sc.getPopulation().getPersons().get(members.get(index));
					index++;
				}
				if(person != null){
					Coord homeCoord = ((ActivityImpl) person.getSelectedPlan().getPlanElements().get(0)).getCoord();					
					Tuple<Double, Double> tuple = getRatios(hh);
//					if(!Double.isInfinite(tuple.getSecond()) && !Double.isNaN(tuple.getSecond())){
						bw.write(String.format("%s,%.0f,%.4f,%d,%d,%d,%.4f,%.4f,%.0f,%.0f\n", 
								hh.getId().toString(),
								hh.getIncome().getIncome(),
								hhScore.get(hhId),
								getAccessibilityClass(hh),
								getIncomeClass(hh),
								getHouseholdClass(hh),
								tuple.getFirst(),
								tuple.getSecond(),
								homeCoord.getX(),
								homeCoord.getY()));						
//					}
				} else{
					LOG.warn("Couldn't find any members for household " + hhId + " - household is ignored.");
				}

				counter.incCounter();
			}
			counter.printCounter();
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter " + bwName);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter " + bwName);
			}
		}
	}
	
	
	
	/**
	 * The accessibility score is a result of {@link AccessibilityCalculator}. 
	 * The thresholds were determined using the 33rd and 67th percentile of
	 * all the calculated values. 
	 * @param household
	 * @return
	 */
	private int getAccessibilityClass(Household household){
		double score = hhScore.get(household.getId());
		if(score <= 44.67){
			return 0;
		} else if(score <= 54.71){
			return 1;
		} else{
			return 2;
		}
	}
	

	/**
	 * Income classes as distinguished in the Nelson Mandela Bay household 
	 * travel survey of 2004.<br>
	 * <ul> 
	 * 	<li> Low: income < R1500/month;
	 * 	<li> Medium: R1500 < income <= R6000/month;
	 * 	<li> High: R6000 < income;
	 * </ul> 
	 * @param household
	 * @return
	 */
	private int getIncomeClass(Household household){
		Income income = household.getIncome(); /* Monthly */
		if(income.getIncome() <= SouthAfricaInflationCorrector.convert(1500, 2004, 2011)){
			return 0;
		} else if(income.getIncome() <= SouthAfricaInflationCorrector.convert(6000, 2004, 2011)){
			return 1;
		} else{
			return 2;
		}
	}
	
	
	/**
	 * Identify the type of household:
	 * <ol>
	 * 	<li> single adult without children;
	 * 	<li> single adult with children;
	 * 	<li> multiple adults without children;
	 * 	<li> multiple adults with children.
	 * </ol>
	 * @param household
	 * @return
	 */
	private int getHouseholdClass(Household household){
		int numberOfAdults = 0;
		int numberOfChildren = 0;
		
		/* Calculate the number fo adults and children. */
		for(Id id: household.getMemberIds()){
			Person person = this.sc.getPopulation().getPersons().get(id);
			if(person != null){
				if(PersonUtils.getAge(person) <= 18){
					numberOfChildren++;
				} else{
					numberOfAdults++;
				}
			}
		}
		
		/* Determine the classes. */
		if(numberOfAdults < 2){
			if(numberOfChildren == 0){
				return 1;
			} else{
				return 2;
			}
		} else{
			if(numberOfChildren == 0){
				return 3;
			} else{
				return 4;
			}
		}
	}


	public Tuple<Double, Double> getRatios(Household household){
		Tuple<Double, Double> ratios;
		double actualTravelTimes = 0.0;
		double actualActivityTimes = 0.0;
		double actualTravelExpense = 0.0;
		int numberOfMembers = 0;
		
		for(Id id : household.getMemberIds()){
			Person person = sc.getPopulation().getPersons().get(id);
			if(person != null){
				numberOfMembers++;
				for(int i = 2; i < person.getSelectedPlan().getPlanElements().size()-1; i++){
					PlanElement pe = person.getSelectedPlan().getPlanElements().get(i);
					if(pe instanceof ActivityImpl){
						Activity act = (ActivityImpl) pe;

						/* Get actual travel time and cost to relevant activities. */
						String actType = act.getType();
						if(		actType.equalsIgnoreCase("h") ||
								actType.contains("w") ||
								actType.contains("e") ||
								actType.contains("s") ||
								actType.contains("m")){
							Leg leg = (Leg)person.getSelectedPlan().getPlanElements().get(i-1);
							
							/*FIXME There are negative travel times. */ 
							if(leg.getTravelTime() > 0){
								actualTravelTimes += leg.getTravelTime();
							}

							/* Estimate travel cost. */
							actualTravelExpense += getTravelCost(leg);
						}

						/* Get actual activity time for relevant activities. */ 
						if(		actType.contains("w") ||
								actType.contains("e") ||
								actType.contains("s") ||
								actType.contains("m")){
							actualActivityTimes += (act.getEndTime() - act.getStartTime());
						}
					}
				}
			}
			
		}

		double timeRatio = actualTravelTimes / ((24*3600)*numberOfMembers - actualActivityTimes);
		double incomeRatio = actualTravelExpense / household.getIncome().getIncome();
		
		if(timeRatio < 0 || incomeRatio < 0){
			LOG.error("Have a problem!");
		}
		
		ratios = new Tuple<Double, Double>(timeRatio, incomeRatio);
		
		return ratios;
	}
	
	
	private double getTravelCost(Leg leg){
		double distance = 0.0;
		double unitCost = 0.0;
		double time = leg.getTravelTime();
		
		/*FIXME Why are there negative travel times?! */
		if(time > 0){
			if(leg.getMode().equalsIgnoreCase("car")){
				distance = time / (60.0 * 1000.0 / 3600.0);
				unitCost = 3.50;
			} else if(leg.getMode().equalsIgnoreCase("taxi")){
				distance = time / (45.0 * 1000.0 / 3600.0);
				unitCost = SouthAfricaInflationCorrector.convert(0.34, 2004, 2011);
			} else if(leg.getMode().equalsIgnoreCase("pt1")){
				distance = time / (35.0 * 1000.0 / 3600.0);
				unitCost = SouthAfricaInflationCorrector.convert(0.19, 2004, 2011);
			} else if(leg.getMode().equalsIgnoreCase("pt2")){
				distance = time / (35.0 * 1000.0 / 3600.0);
				unitCost = SouthAfricaInflationCorrector.convert(0.17, 2004, 2011);
			}			
		}
				
		double cost = distance * unitCost;
		if(cost < 0){
			LOG.error("Earning income from travelling.");
		}
		return cost;
	}






}
