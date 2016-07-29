package playground.artemc.scenarioTools;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

/**
 * Population generator for a corridor scenario 
 * 
 * @author achakirov
 * 
 */


import org.apache.log4j.Logger;
import org.matsim.analysis.Bins;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.population.io.StreamingUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.artemc.utils.Writer;

public class SimplePopulationGenerator {

	private static Integer populationSize = 8000;
	private static Double noCarPercentage = 0.0;
	private static Integer corridorLength = 20000;
	
	private Random random = new Random(10830239345L);

	private ObjectAttributes incomes = new ObjectAttributes();
	private HashMap<Id<Person>,Integer> incomeData = new HashMap<Id<Person>,Integer>();
	
	Bins incomeBins = new Bins(5000, 200000, "incomes");
	private final static Logger log = Logger.getLogger(SimplePopulationGenerator.class);
	
	public static void main(String[] args) throws IOException {

		String outputPath = args[0];
		SimplePopulationGenerator simplePopulationGenerator = new SimplePopulationGenerator();
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Population population = (Population) scenario.getPopulation();
		PopulationFactory pf = population.getFactory();
		StreamingUtils.setIsStreaming(population, true);
		StreamingPopulationWriter popWriter = new StreamingPopulationWriter(population, scenario.getNetwork());
		popWriter.startStreaming(outputPath+"/corridorPopulation_"+populationSize+".xml");

		Random generator = new Random();

		/*Assign random home zone*/
		for(Integer i=0;i<populationSize;i++){	
			Person person = pf.createPerson(Id.create(i, Person.class));
			Plan plan = pf.createPlan();

			//System.out.println("Agent: "+i+" from "+populationSize);

			Double x=0.0;
			Double y=0.0;
			Coord homeLocation;
			Coord workLocation;
			
			do{
				/*Home location*/
				do{
					x = (corridorLength/6*generator.nextGaussian() + corridorLength /3);
				}while(x<0 || x>corridorLength );
				y = (generator.nextDouble()-0.5)*1000 + 1000;

				homeLocation = new Coord(x, y);

				/*Work location*/
				do{
					x = (corridorLength /6*generator.nextGaussian() + 2*corridorLength/3);
				}while(x<0 || x>corridorLength);
				y = (generator.nextDouble()-0.5)*1000 + 1000;

				workLocation = new Coord(x, y);
			}while(homeLocation.getX() > workLocation.getX());

			//Add person attributes
			double carAvailToss = generator.nextDouble();
			if(carAvailToss<noCarPercentage){
				PersonUtils.setCarAvail(person, "never");
			}
			else{
				PersonUtils.setCarAvail(person, "always");
			}
			PersonUtils.setEmployed(person, true);
			simplePopulationGenerator.createIncome(person);

			//Add home location to the plan
			Activity actHome = (Activity) pf.createActivityFromCoord("home", homeLocation);
			Activity actWork = (Activity) pf.createActivityFromCoord("work", workLocation);
			Activity actHome2 = (Activity) pf.createActivityFromCoord("home", homeLocation);
			Leg leg = (Leg) pf.createLeg("pt");
			actHome.setEndTime(3600.00*8.30 + generator.nextGaussian()*1800);
			plan.addActivity(actHome);
			plan.addLeg(leg);
			actWork.setStartTime(actHome.getEndTime()+1800.0);
			actWork.setEndTime(3600.00*18.5 + generator.nextGaussian()*1800);
			plan.addActivity(actWork);
			plan.addLeg(leg);
			plan.addActivity(actHome2);
			
			person.addPlan(plan);

			population.addPerson(person);
			popWriter.writePerson(person);	

		}
		popWriter.closeStreaming();
		simplePopulationGenerator.writeIncomes(outputPath);
	}
	
	private void createIncome(Person person){
//		Double mean=Math.log(19600.0);
//		Double std=0.78;
		//Values from working population of Sioux Falls Scenario
		Double mean=10.954092187;
		Double std= 0.730406478;
		Integer income = (int) Math.round(Math.exp(mean+std*random.nextGaussian()));
		incomes.putAttribute(person.getId().toString(), "income", income);	
		incomeData.put(person.getId(), income);
		incomeBins.addVal(income, 1.0);
	}
	
	private void writeIncomes(String outputPath) {
		log.info("Writing incomes to " + outputPath+"/income_"+populationSize+".xml");
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(incomes);
		attributesWriter.writeFile(outputPath+"/income_"+populationSize+".xml");
		incomeBins.plotBinnedDistribution(outputPath+"/", "income", "money");	
		
		Writer writer = new Writer();
		writer.creteFile(outputPath+"/incomeData_"+populationSize+".csv");
		for(Id<Person> id:incomeData.keySet()){
			writer.writeLine(id.toString()+","+incomeData.get(id).toString());
		}
		writer.close();
	}
}
