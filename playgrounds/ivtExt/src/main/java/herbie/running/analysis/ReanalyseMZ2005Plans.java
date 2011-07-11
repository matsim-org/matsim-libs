package herbie.running.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;


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

		
		TreeMap<Double, Integer> walkDC = new TreeMap<Double, Integer>();	
		TreeMap<Double, Integer> bikeDC = new TreeMap<Double, Integer>();
		TreeMap<Double, Integer> carDC = new TreeMap<Double, Integer>();
		TreeMap<Double, Integer> ptDC = new TreeMap<Double, Integer>();

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
					else if (leg.getMode().equals("car")) {
						double lastDistClass = 0;
						boolean placed = false;
						
						for (double distClass : distanceClasses) {							
							if (leg.getRoute().getDistance() > distClass) {
								if (carDC.containsKey(lastDistClass)) {
									int newFreq = carDC.get(lastDistClass) + 1;
									carDC.put(lastDistClass, newFreq);
								} else  {
									carDC.put(lastDistClass, 1);
								}
								placed = true;
								break;
							}
							lastDistClass = distClass;
						}
						if (!placed) {
							if (carDC.containsKey(lastDistClass)) {
								int newFreq = carDC.get(lastDistClass) + 1;
								carDC.put(lastDistClass, newFreq);
							} else  {
								carDC.put(lastDistClass, 1);
							}
						}
						
					}
					else if (leg.getMode().equals("pt")) {
						double lastDistClass = 0;
						boolean placed = false;
						
						for (double distClass : distanceClasses) {							
							if (leg.getRoute().getDistance() > distClass) {
								if (ptDC.containsKey(lastDistClass)) {
									int newFreq = ptDC.get(lastDistClass) + 1;
									ptDC.put(lastDistClass, newFreq);
								} else  {
									ptDC.put(lastDistClass, 1);
								}
								placed = true;
								break;
							}
							lastDistClass = distClass;
						}
						if (!placed) {
							if (ptDC.containsKey(lastDistClass)) {
								int newFreq = ptDC.get(lastDistClass) + 1;
								ptDC.put(lastDistClass, newFreq);
							} else  {
								ptDC.put(lastDistClass, 1);
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
		System.out.println("car distr");
		for (double i : carDC.keySet()) {
			System.out.println(i+" = "+carDC.get(i));
		}
		System.out.println("pt distr");
		for (double i : ptDC.keySet()) {
			System.out.println(i+" = "+ptDC.get(i));
		}
		

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(new File ("D:/Arbeit/Projekte/herbie/output/newDistanceDistributionMZ2005.txt")));
			out.write("Mode\tcar\tbike\tpt\twalk\tsum\n");
			double carSum = 0;
			double bikeSum = 0;
			double ptSum = 0;
			double walkSum = 0;
			for (int i=12; i>=0; i--) {
				double className =  distanceClasses[i];
				int rowSum = 0;
				
				out.write(className+"\t");
				if (carDC.containsKey(className)) {
					out.write(carDC.get(className)+"\t");
					carSum += carDC.get(className);
					rowSum += carDC.get(className);
				}
				else {
					out.write("0\t");
				}
				if (bikeDC.containsKey(className)) {
					out.write(bikeDC.get(className)+"\t");
					bikeSum += bikeDC.get(className);
					rowSum += bikeDC.get(className);
				}
				else {
					out.write("0\t");
				}
				if (ptDC.containsKey(className)) {
					out.write(ptDC.get(className)+"\t");
					ptSum += ptDC.get(className);
					rowSum += ptDC.get(className);
				}
				else {
					out.write("0\t");
				}
				if (walkDC.containsKey(className)) {
					out.write(walkDC.get(className)+"\t");
					walkSum += walkDC.get(className);
					rowSum += walkDC.get(className);
				}
				else {
					out.write("0\t");
				}
				out.write(rowSum+"");
				out.newLine();
				out.flush();
			}
			out.write("Sum\t"+carSum+"\t"+bikeSum+"\t"+ptSum+"\t"+walkSum);
			out.newLine();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}




		System.out.println("Done reanalysing MZ 2005 plans.");

	}



}
