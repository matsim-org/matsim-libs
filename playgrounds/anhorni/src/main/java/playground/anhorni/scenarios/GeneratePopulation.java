package playground.anhorni.scenarios;

import java.util.Random;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;

public class GeneratePopulation {
		
	private Random randomNumberGenerator;
	
	public GeneratePopulation(Random randomNumberGenerator) {
		this.randomNumberGenerator = randomNumberGenerator;
	}
	
	public void generatePopulation(int populationSize, ExpenditureAssigner expenditureAssigner,  PopulationImpl staticPopulation) {
		
		for (int i = 0; i < populationSize; i++) {
			PersonImpl p = new PersonImpl(new IdImpl(i));

			// assign home town
			int townId = 0;
			if (i >= (populationSize / 2)) {
				townId = 1;
			}
			p.getCustomAttributes().put("townId", townId);
			
			// assign work location
			boolean workingIntheCity = this.randomNumberGenerator.nextBoolean();
			if (workingIntheCity) {
				p.getCustomAttributes().put("cityWorker", true);
			}
			else {
				p.getCustomAttributes().put("cityWorker", false);
			}
			staticPopulation.addPerson(p);
		}
		expenditureAssigner.assignExpenditures(staticPopulation);
	}
}
