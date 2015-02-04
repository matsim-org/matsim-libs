package playground.artemc.heterogeneity;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import playground.artemc.utils.MapWriter;

/**
 * Created by artemc on 29/1/15.
 */
public class IncomePopulationReader {

	private static final Logger log = Logger.getLogger(IncomePopulationReader.class);

	private Population population;
	private IncomeHeterogeneityImpl incomeHeterogeneityImpl;

	public IncomePopulationReader(IncomeHeterogeneityImpl incomeHeterogeneityImpl, Population population)
	{
		this.incomeHeterogeneityImpl = incomeHeterogeneityImpl;
		this.population = population;
	}

	public void parse(String filename) {

		log.info("loading income data from " + filename);
		new ObjectAttributesXmlReader(population.getPersonAttributes()).parse(filename);

		/*Calculate Income Statistics*/
		Integer incomeSum=0;
		Double incomeMean = 0.0;

		for(Id<Person> personId:this.population.getPersons().keySet()){
			incomeSum = incomeSum + (int) this.population.getPersonAttributes().getAttribute(personId.toString(), "income");
			incomeMean = (double) incomeSum / (double) this.population.getPersons().size();

//			if(simulationType.equals("heteroAlphaProp")){
//				double randomFactor= 0.0;
//				do{
//					randomFactor = (MatsimRandom.getRandom().nextGaussian() * 0.2) + 1;
//				}while(randomFactor <0 && randomFactor >2);
//				System.out.println();
//				betaFactors.put(personId, randomFactor);
//			}
		}

		/*Create map of personal income factors*/
		Double factorSum=0.0;
		for(Id<Person> personId:this.population.getPersons().keySet()){
			Integer personIncome = (int) this.population.getPersonAttributes().getAttribute(personId.toString(), "income");
			double incomeFactor = Math.pow((double) personIncome/incomeMean,(this.incomeHeterogeneityImpl.getLambda_income()));
			this.incomeHeterogeneityImpl.getIncomeFactors().put(personId, incomeFactor);
			factorSum = factorSum + incomeFactor;
		}

		//		It is more accurate to adjust the parameters in case of heterogeneous simulation, so that the mean still corresponds the original value -artemc nov '14
		//		/*For simulation with homogeneous parameters but adjusted for income factor mean*/
		//		if(simulationType.equals("homo")){
		//			log.info("Homogeneuos simulation with parameter adjustments for income factor mean is enabled...");
		//			factoreMean = factorSum / (double) incomeFactors.size();
		//			for(Id<Person> personId:incomeFactors.keySet()){
		//				incomeFactors.put(personId, factoreMean);
		//			}
		//		}
	}


}
