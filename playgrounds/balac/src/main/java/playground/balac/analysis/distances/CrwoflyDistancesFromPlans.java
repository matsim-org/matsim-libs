package playground.balac.analysis.distances;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class CrwoflyDistancesFromPlans {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);
		
		ArrayList<Double> walkDistances = new ArrayList<Double>();
		ArrayList<Double> bikeDistances = new ArrayList<Double>();
		ArrayList<Double> ptDistances = new ArrayList<Double>();
		ArrayList<Double> carDistances = new ArrayList<Double>();
		double coordX = 683217.0;
		double coordY = 247300.0;	
		
		final BufferedWriter outLinkBike = IOUtils.getBufferedWriter(args[2] + "_bike.txt");
		final BufferedWriter outLinkWalk = IOUtils.getBufferedWriter(args[2] + "_walk.txt");
		final BufferedWriter outLinkCar = IOUtils.getBufferedWriter(args[2] + "_car.txt");
		final BufferedWriter outLinkPt = IOUtils.getBufferedWriter(args[2] + "_pt.txt");
		
		
		for (Person p : scenario.getPopulation().getPersons().values()) {
			
			Plan plan = p.getSelectedPlan();
			Coord coord_start = null;
			Coord coord_end = null;
			String trip = "";
			for (PlanElement pe : plan.getPlanElements()) {
				
				if (pe instanceof Activity) {
					if (coord_start == null)
						coord_start = scenario.getNetwork().getLinks().get(((Activity) pe).getLinkId()).getCoord();
					else {
						coord_end = scenario.getNetwork().getLinks().get(((Activity) pe).getLinkId()).getCoord();
						
						if (Math.sqrt(Math.pow(coord_start.getX() - coordX, 2) +(Math.pow(coord_start.getY() - coordY, 2))) < 30000 
								&& Math.sqrt(Math.pow(coord_end.getX() - coordX, 2) +(Math.pow(coord_end.getY() - coordY, 2))) < 30000)	{
							
							if (trip.equals("walk")) {
														
								walkDistances.add(Math.sqrt(Math.pow(coord_start.getX() - coord_end.getX(), 2) +(Math.pow(coord_start.getY() - coord_end.getY(), 2))));
								outLinkWalk.write(Double.toString(Math.sqrt(Math.pow(coord_start.getX() - coord_end.getX(), 2) +(Math.pow(coord_start.getY() - coord_end.getY(), 2)))));
								outLinkWalk.newLine();

							}	
							
							else if (trip.equals("car")) {
								
								carDistances.add(Math.sqrt(Math.pow(coord_start.getX() - coord_end.getX(), 2) +(Math.pow(coord_start.getY() - coord_end.getY(), 2))));
								outLinkCar.write(Double.toString(Math.sqrt(Math.pow(coord_start.getX() - coord_end.getX(), 2) +(Math.pow(coord_start.getY() - coord_end.getY(), 2)))));
								outLinkCar.newLine();

							}
							else if (trip.equals("pt")) {
								
								ptDistances.add(Math.sqrt(Math.pow(coord_start.getX() - coord_end.getX(), 2) +(Math.pow(coord_start.getY() - coord_end.getY(), 2))));
								outLinkPt.write(Double.toString(Math.sqrt(Math.pow(coord_start.getX() - coord_end.getX(), 2) +(Math.pow(coord_start.getY() - coord_end.getY(), 2)))));
								outLinkPt.newLine();
							}
							else if (trip.equals("bike")) {
								
								bikeDistances.add(Math.sqrt(Math.pow(coord_start.getX() - coord_end.getX(), 2) +(Math.pow(coord_start.getY() - coord_end.getY(), 2))));
								outLinkBike.write(Double.toString(Math.sqrt(Math.pow(coord_start.getX() - coord_end.getX(), 2) +(Math.pow(coord_start.getY() - coord_end.getY(), 2)))));
								outLinkBike.newLine();
							}
							
						}					
						coord_start = scenario.getNetwork().getLinks().get(((Activity) pe).getLinkId()).getCoord();
					}
					
				}
				
				else if (pe instanceof Leg) {
					
					trip = ((Leg) pe).getMode();
				}
			}
			
			
		}
		
		outLinkBike.flush();
		outLinkBike.close();
		
		outLinkCar.flush();
		outLinkCar.close();
		
		outLinkWalk.flush();
		outLinkWalk.close();
		
		outLinkPt.flush();
		outLinkPt.close();

		
		
		
	}

}
