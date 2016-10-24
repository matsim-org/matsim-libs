package playground.gleich.analyzer.exampleScenario;

import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class ScenarioCreator {
	
	private static String pathToExampleScenario = "Z:/WinHome/ArbeitWorkspace/Analyzer/";
	
	public static void main(String[] args){
		new ScenarioCreator().generatePopulation();
	}

	public void generatePopulation() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Population pop = scenario.getPopulation();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(pathToExampleScenario + "input/network.xml");
		MatsimRandom.reset(4711);
		Random rnd = MatsimRandom.getLocalInstance();
		int nPersonsBase = 32;
		int nPersonsTotal = 0;
		
		
		/* Creates (nPersonBase/((xFrom+yFrom)*(xTo+yTo))) persons per hour 
		 * travelling from node ij to kl during peak time (6-9) and 
		 * (nPersonBase/((i+j)*(k+l)*2)) 5-6 and 9-12 */
		for(int xFrom = 1; xFrom <= 4; xFrom++){
			for(int yFrom = 1; yFrom<= 4; yFrom++){
				for(int xTo = 1; xTo <= 4; xTo++){
					for(int yTo = 1; yTo <= 4; yTo++){
						if((xFrom*10+yFrom)==(xTo*10+yTo)){
							continue;
						}
						int nPersons = 1 + nPersonsBase/((xFrom+yFrom)*(xTo+yTo)); //minimum 1
						nPersonsTotal += nPersons;

						Coord fromCoord = new Coord((double) (xFrom * 1000), (double) (yFrom * 1000));
						Coord toCoord = new Coord((double) (xTo * 1000), (double) (yTo * 1000));//arrivalCoordinates in Act2Mode appear to be different
						System.out.println(nPersons+"pt Agents from " + xFrom + yFrom + " (" + fromCoord.toString() + ") to "+xTo+yTo+" ("+toCoord.toString());
						createPersons(rnd, pop, nPersons, fromCoord, toCoord, 5, 12, "pt", xFrom, yFrom, xTo, yTo);//i,j,k,l for Agent Ids which include node numbers where work and home places are located
						createPersons(rnd, pop, nPersons, fromCoord, toCoord, 7, 9, "pt", xFrom, yFrom, xTo, yTo);
						createPersons(rnd, pop, nPersons, fromCoord, toCoord, 5, 12, "car", xFrom, yFrom, xTo, yTo);
						createPersons(rnd, pop, nPersons, fromCoord, toCoord, 7, 9, "car", xFrom, yFrom, xTo, yTo);
					}
				}
			}
		}
		System.out.println("nPersonsTotal: "+nPersonsTotal);
		new PopulationWriter(pop, scenario.getNetwork()).write(pathToExampleScenario + "input/ijkl_plans.xml");		
	}
	
	
	// Modified copy of playground.andreas.utils.dummy.PopGenerator 
	private static void createPersons(Random rnd, Population pop, int nPersonsPerHour, Coord fromCoord, Coord toCoord, int departureIntervalStart, int departureIntervalEnd, String transportMode, int xFrom, int yFrom, int xTo, int yTo){
		int nPersonsCreated = pop.getPersons().size();
		int nPersonsToBeCreated = (departureIntervalEnd - departureIntervalStart) * nPersonsPerHour;
		
		for (int ii = 0; ii < nPersonsToBeCreated; ii++) {
			nPersonsCreated++;
			Person person = pop.getFactory().createPerson(Id.create(transportMode+"_"+xFrom+yFrom+"_to_"+xTo+yTo+"_Nr"+nPersonsCreated, Person.class));//ii not in Id, because this method is called twice for car and twice for pt, so every ii is included twice in the ids
			Plan plan = pop.getFactory().createPlan();
			
			Activity h = pop.getFactory().createActivityFromCoord("h", fromCoord);
			h.setEndTime(departureIntervalStart * 3600.0 + rnd.nextDouble() * (departureIntervalEnd - departureIntervalStart) * 3600.0);
			plan.addActivity(h);

			Leg leg = pop.getFactory().createLeg(transportMode);
			plan.addLeg(leg);

			Activity w = pop.getFactory().createActivityFromCoord("w", toCoord);
			plan.addActivity(w);

			person.addPlan(plan);
			pop.addPerson(person);
		}
	}

}
