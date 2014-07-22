package playground.artemc.scenarioTools;

/**
 * Population generator for a corridor scenario 
 * 
 * @author achakirov
 * 
 */


import java.io.IOException;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.analysis.Bins;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class SimplePopulationGenerator {

	private static Integer populationSize = 20000;
	private static Double noCarPercentage = 0.1;
	private static Integer corridorLength = 20000;
	
	private Random random = new Random(102830259L);
	private ObjectAttributes incomes = new ObjectAttributes();
	private Bins incomeBins = new Bins(5000, 200000, "incomes");
	private final static Logger log = Logger.getLogger(SimplePopulationGenerator.class);

	
	public static void main(String[] args) throws IOException {

		String outputPath = args[0];

		SimplePopulationGenerator simplePopulationGenerator = new SimplePopulationGenerator();
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		PopulationImpl population = (PopulationImpl) scenario.getPopulation();
		PopulationFactory pf = population.getFactory();
		population.setIsStreaming(true);
		PopulationWriter popWriter = new PopulationWriter(population, scenario.getNetwork());
		popWriter.startStreaming(outputPath+"/corridorPopulation_"+populationSize+".xml");

		Random generator = new Random();	

		/*Assign random home zone*/
		for(Integer i=0;i<populationSize;i++){	
			PersonImpl person = (PersonImpl) pf.createPerson(new IdImpl(i));
			Plan plan = pf.createPlan();

			//System.out.println("Agent: "+i+" from "+populationSize);

			Double x=0.0;
			Double y=0.0;
			CoordImpl homeLocation;
			CoordImpl workLocation;
			Integer income;

			do{
				/*Home location*/
				do{
					x = (corridorLength/6*generator.nextGaussian() + corridorLength /3);
				}while(x<0 || x>corridorLength );
				y = (generator.nextDouble()-0.5)*2000 + 1010;	

				homeLocation = new CoordImpl(x,y);

				/*Work location*/
				do{
					x = (corridorLength /6*generator.nextGaussian() + 2*corridorLength/3);
				}while(x<0 || x>corridorLength);
				y = (generator.nextDouble()-0.5)*2000 + 1010;	

				workLocation = new CoordImpl(x,y);
			}while(homeLocation.getX() > workLocation.getX());

			//Add person attributes
			double carAvailToss = generator.nextDouble();
			if(carAvailToss<noCarPercentage){
				person.setCarAvail("never");
			}
			else{
				person.setCarAvail("always");
			}
			person.setEmployed(true);
			simplePopulationGenerator.createIncome(person);

			//Add home location to the plan
			ActivityImpl actHome = (ActivityImpl) pf.createActivityFromCoord("home", homeLocation);
			ActivityImpl actWork = (ActivityImpl) pf.createActivityFromCoord("work", workLocation);
			ActivityImpl actHome2 = (ActivityImpl) pf.createActivityFromCoord("home", homeLocation);
			LegImpl leg = (LegImpl) pf.createLeg("pt");
			actHome.setEndTime(3600.00*8.00 + generator.nextGaussian()*3600);
			plan.addActivity(actHome);
			plan.addLeg(leg);
			actWork.setStartTime(3600.00*9);
			actWork.setEndTime(3600.00*18 + generator.nextGaussian()*1800);
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
	
	private void createIncome(PersonImpl person){
		Double mean=Math.log(19600.0);
		Double std=0.78;
		Integer income = (int) Math.round(Math.exp(mean+std*random.nextGaussian()));			
		incomes.putAttribute(person.getId().toString(), "income", income);		
		incomeBins.addVal(income, 1.0);
	}
	
	private void writeIncomes(String outputPath) {
		log.info("Writing incomes to " + outputPath+"/income_"+populationSize+".xml");
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(incomes);
		attributesWriter.writeFile(outputPath+"/income_"+populationSize+".xml");
		incomeBins.plotBinnedDistribution(outputPath+"/", "income", "money");	
	}
}
