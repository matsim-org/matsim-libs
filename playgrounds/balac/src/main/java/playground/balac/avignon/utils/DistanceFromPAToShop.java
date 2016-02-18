package playground.balac.avignon.utils;

import java.util.ArrayList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

public class DistanceFromPAToShop {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		
			populationReader.readFile(args[0]);
			networkReader.readFile(args[1]);
			int[] in = new int[60];
			int[] out = new int[60];
			double coordX = 683217.0;
			double coordY = 247300.0;
			double distance1 = 0.0;
			double distance2 = 0.0;
			int count1  = 0;
			int count2 = 0;
			for(Person p: scenario.getPopulation().getPersons().values()) {
				ArrayList<Coord> pacoord = new ArrayList<Coord>();
				ArrayList<Coord> shopcoordin = new ArrayList<Coord>();
				ArrayList<Coord> shopcoordout = new ArrayList<Coord>();

				boolean shop = false;
				for(PlanElement pe: p.getSelectedPlan().getPlanElements()) {
					
					if (pe instanceof Activity) {
						
						if (((Activity) pe).getType().equals( "shopgrocery" )) {
							shop = true;
							if (Math.sqrt(Math.pow(((Activity) pe).getCoord().getX() - coordX, 2) +(Math.pow(((Activity) pe).getCoord().getY() - coordY, 2))) > 5000) {
								
								shopcoordout.add(((Activity) pe).getCoord());
								
								
							}
							else 
								shopcoordin.add(((Activity) pe).getCoord());
							
							
						}
						else if (((Activity) pe).getType().equals( "home" ) || ((Activity) pe).getType().startsWith( "work" )) {
							
							pacoord.add(((Activity) pe).getCoord());
							
							
						}
					}
					
					
				}
				
				
				if (shop) {
					
					if (!shopcoordin.isEmpty()) {
						for(Coord c1 : shopcoordin) {
							double temp = Double.MAX_VALUE;
							
							for(Coord c2:pacoord) {
								if (CoordUtils.calcEuclideanDistance(c1, c2) < temp)
									temp = CoordUtils.calcEuclideanDistance(c1, c2);
								
							}
							in[(int)temp / 1000]++;
							distance1 +=temp;
							count1++;
						}
					}
					
					if (!shopcoordout.isEmpty()) {
						for(Coord c1 : shopcoordout) {
							double temp = Double.MAX_VALUE;
							
							for(Coord c2:pacoord) {
								if (CoordUtils.calcEuclideanDistance(c1, c2) < temp)
									temp = CoordUtils.calcEuclideanDistance(c1, c2);
								
							}
							out[(int)temp / 1000]++;
							distance2 +=temp;
							count2++;
						}
					}
					
					
				}
				
				
			}	
			
			
			System.out.println( distance1/(double)count1);
			System.out.println( distance2/(double)count2);
			for (int i = 0; i < in.length; i++) 
				System.out.println((double)in[i]);
			
			for (int i = 0; i < out.length; i++) 
				System.out.println((double)out[i]);

	}

}
