package playground.balac.twowaycarsharing.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class IncomeCarSharing {

	public static void main(String[] args) throws IOException {
		
		final BufferedReader readLink = IOUtils.getBufferedReader(args[0]);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		Config config = scenario.getConfig();
		final BufferedWriter outLink = IOUtils.getBufferedWriter(args[3]);
		
		scenario.getConfig().getModule("qsim").addParam("mainMode", "cs_fix_gas");
		
		int count = 0;
		networkReader.readFile(args[1]);
		populationReader.readFile(args[2]);		
		
		Network network = scenario.getNetwork();
		String s = readLink.readLine();
		s = readLink.readLine();
		String previous = null;
		int counterrb = 0;
		int counterff = 0;
		double income = 0.0;
		boolean ff = false;
		boolean rb = false;
		while (s != null) {
			Leg lastLeg = null;
			String[] arr = s.split("\t");
			
			double startTime = Double.parseDouble(arr[2]);
			double endTime = Double.parseDouble(arr[3]);
			Id<Person> personId = Id.create(arr[0], Person.class);
			
			if (previous == null || !previous.equals(arr[0])) {
				counterrb = 0;
				counterff = 0;
			}
			
			if (arr[1].contains("c")) {
				rb = true;
				ff = false;
			}
			else {
				rb = false;
				ff = true;
			}
			
			
			if (rb) {
				
				Person p = scenario.getPopulation().getPersons().get(personId);
				boolean cs = false;
				double distance = 0.0;
				int c = 0;
				for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
					
					if (pe instanceof Activity) {
						if (c == counterrb) {
							if (((Activity) pe).getType().equals("cs_interaction") && ((Activity)pe).getLinkId().toString().equals(arr[4]) && !cs) {
								cs = true;
							}
							else if (((Activity) pe).getType().equals("cs_interaction") && ((Activity)pe).getLinkId().toString().equals(arr[4]) && cs && lastLeg.getMode().equals("cs_fix_gas")) {
							
								cs = false;
								counterrb += 2;
								//write the distance here
								outLink.write(arr[0]);
								outLink.write(" ");
								outLink.write(arr[1]);
								outLink.write(" ");
								outLink.write(arr[2]);
								outLink.write(" ");
								outLink.write(arr[3]);
								outLink.write(" ");
								outLink.write(Double.toString(distance));
								outLink.newLine();
								break;
							}
						}
						
					}
					else if (pe instanceof Leg) {
							lastLeg = (Leg) pe;
							if (c < counterrb) {
								if (((Leg) pe).getMode().equals("cs_walk")) {
									c++;
								
								}
							}
							else if (cs) {
							
						
								if (((Leg) pe).getMode().equals("cs_fix_gas")) {
									
									Route r = ((Leg) pe).getRoute();
									GenericRouteImpl rg = (GenericRouteImpl) r;
									String grr = rg.getRouteDescription();
									
									String[] arr1 = grr.split("\\s");
									
									for (int i = 0; i < arr1.length; i++) {
										
										distance += network.getLinks().get(Id.create(arr1[i], Link.class)).getLength();
									}
								
								}
						
						}
						
					}
					
				}
			}
			//else {
			/*
				Person p = scenario.getPopulation().getPersons().get(personId);
				double distance = 0.0;
				
				for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
					int c = 0;
					
					if (pe instanceof Leg) {
							lastLeg = (Leg) pe;
							if (c < counterff) {
								if (((Leg) pe).getMode().equals("cs_flex_gas")) {
									c++;
								
								}
							}
							else if (((Leg) pe).getMode().equals("cs_flex_gas")) {
									
									Route r = ((Leg) pe).getRoute();
									GenericRouteImpl rg = (GenericRouteImpl) r;
									String grr = rg.getRouteDescription();
									System.out.println(grr);
									if (grr == null) {
										System.out.println("bla");
										counterff++;
										break;
									}
									String[] arr1 = grr.split("\\s");
									
									for (int i = 0; i < arr1.length; i++) {
										
										distance += network.getLinks().get(Id.create(arr1[i])).getLength();
									}
									
									//write here the distance
									outLink.write(arr[0]);
									outLink.write(" ");
									outLink.write(arr[1]);
									outLink.write(" ");
									outLink.write(arr[2]);
									outLink.write(" ");
									outLink.write(arr[3]);
									outLink.write(" ");
									outLink.write(Double.toString(distance));
									outLink.newLine();
									counterff++;
									break;
								
						}
						
					}
				}
				
			}*/
			
			previous = arr[0];
			s = readLink.readLine();

		}		
		outLink.flush();
		outLink.close();
		System.out.println(income);
		System.out.println(count);
	}

}
