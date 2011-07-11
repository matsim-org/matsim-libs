package herbie.running.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;


import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class ReanalyseMZ2005Plans {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String configFile = args[0] ;
		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.createScenario(config);

		PopulationImpl pop = (PopulationImpl) scenario.getPopulation();		
		new MatsimPopulationReader(scenario).readFile(config.plans().getInputFile());		

		double[] distanceClasses = new double[]{
				Double.MAX_VALUE, 100000
				, 50000, 40000, 30000, 20000, 
				10000, 5000, 4000, 3000, 2000, 
				1000, 0.0};

		
		HashMap<Double, Integer> walkDC = new HashMap<Double, Integer>();	
		HashMap<Double, Integer> bikeDC = new HashMap<Double, Integer>();	

		System.out.println("Number of persons: "+pop.getPersons().size());

		for (Person p : pop.getPersons().values()) {

			Plan plan = p.getSelectedPlan();

			List<PlanElement> planElements = plan.getPlanElements();

			for (PlanElement pE : planElements) {
				if (pE instanceof Leg) {
					LegImpl leg = (LegImpl) pE;
					
					if (leg.getMode().equals("walk")) {
						double lastDistClass = 0;
						boolean placed = false;
						
						for (double distClass : distanceClasses) {							
							if (leg.getRoute().getDistance() > distClass) {
								if (walkDC.containsKey(lastDistClass)) {
									int newFreq = walkDC.get(lastDistClass) + 1;
									walkDC.put(lastDistClass, newFreq);
								} else  {
									walkDC.put(lastDistClass, 1);
								}
								placed = true;
								break;
							}
							lastDistClass = distClass;
						}
						if (!placed) {
							if (walkDC.containsKey(lastDistClass)) {
								int newFreq = walkDC.get(lastDistClass) + 1;
								walkDC.put(lastDistClass, newFreq);
							} else  {
								walkDC.put(lastDistClass, 1);
							}
						}
						
					}
					else if (leg.getMode().equals("bike")) {
						double lastDistClass = 0;
						boolean placed = false;
						
						for (double distClass : distanceClasses) {							
							if (leg.getRoute().getDistance() > distClass) {
								if (bikeDC.containsKey(lastDistClass)) {
									int newFreq = bikeDC.get(lastDistClass) + 1;
									bikeDC.put(lastDistClass, newFreq);
								} else  {
									bikeDC.put(lastDistClass, 1);
								}
								placed = true;
								break;
							}
							lastDistClass = distClass;
						}
						if (!placed) {
							if (bikeDC.containsKey(lastDistClass)) {
								int newFreq = bikeDC.get(lastDistClass) + 1;
								bikeDC.put(lastDistClass, newFreq);
							} else  {
								bikeDC.put(lastDistClass, 1);
							}
						}
						
					}
														
				}
			}

		}
		System.out.println("walk distr");
		for (double i : walkDC.keySet()) {
			System.out.println(i+" = "+walkDC.get(i));
		}
		System.out.println("bike distr");
		for (double i : bikeDC.keySet()) {
			System.out.println(i+" = "+bikeDC.get(i));
		}
		

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(new File ("C:/Documents and Settings/scnadine/My Documents/Projekte/VW Verkehrsmittelwahl/output/newDistanceDistributionMZ2005.txt")));
			out.write("Mode\t0\t1000\t2000\t3000\t4000\t5000\t10000\t20000\t30000\t40000\t50000\t100000\tMax\n");
			



		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}






	}



}
