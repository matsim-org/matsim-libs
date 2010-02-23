package playground.mmoyo.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.algorithms.PlansFilterByLegMode;
import org.matsim.pt.routes.ExperimentalTransitRoute;

import playground.mmoyo.PTRouter.PTValues;
import playground.mmoyo.analysis.comp.PlanRouter;

/**Uses the router with increasing time priority**/ 
public class ComparisonTimeDistancePriority {

	public double[] values ;
	
	public ComparisonTimeDistancePriority(Population population){
		double travelTime = 0;
		double travelDistance = 0;
		int transfers = 0;
		int detTransfers = 0;
		int walkDistance = 0;
		values = new double[6];
		
		for (Person person : population.getPersons().values() ){
			Plan plan =  person.getSelectedPlan();   //assuming we are using fragmented plans!!!!, otherwise it analyzes only the first leg
			
			List<Activity> transitActList = new ArrayList<Activity>();

			List<PlanElement> elemList = plan.getPlanElements();
			Activity aAct = ((Activity)elemList.get(0));
			Activity bAct = ((Activity)elemList.get(elemList.size()-1));
			
			int i = 0;
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Activity) {
					Activity act = (Activity)pe;
					if (!(act == aAct || act==bAct)){ 
						transitActList.add(act);
						if(act.getType().equals("pt interaction") && i != 2  && i== elemList.size()-2){
							transfers++ ;
						}
					}
				}else{
					Leg leg = (Leg)pe;

					///find out walk distances
					if (leg.getMode().equals(TransportMode.undefined)){
						Activity lastAct= (Activity) plan.getPlanElements().get(i-1);
						Activity nextAct= (Activity) plan.getPlanElements().get(i+1);
						double legWalkDist=  CoordUtils.calcDistance(lastAct.getCoord() , nextAct.getCoord());
						walkDistance += legWalkDist;
						if (i != 1  || i!= elemList.size()-2){
							detTransfers++;
						}
					}else{
						if (leg.getRoute()!= null){
							travelTime += leg.getTravelTime();
							ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute(); 
							
							travelDistance += route.getTravelTime();
						}
					}
					
				}
				i++;
			}//for planelement
		}//	for person

		System.out.println(Double.toString(PTValues.timeCoefficient));
		System.out.println("travelTime: " + travelTime);
		System.out.println("travelDistance: " + travelDistance);
		System.out.println("transfers: " + transfers);
		System.out.println("detTransfers: " + detTransfers);
		System.out.println("walkDistance: " + walkDistance);
		
		values[0] = PTValues.timeCoefficient;
		values[1] = travelTime;
		values[2] = travelDistance;
		values[3] = transfers;
		values[4] = detTransfers;
		values[5] = walkDistance;
	}
	
	public static void main(String[] args) {
		String configFile= null;
		if (args[0]!= null) {
			configFile = args[0];	
		}else{
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_5x_subset_xy2links_ptplansonly/config/config_5xptPlansOnly_noRouted.xml";	
		}
		
		PTValues.routerCalculator = 3;
		double[][] a_values = new double[21][6];
		
		for (double x= 0; x<=1; x = x + 0.05 ){
			PTValues.timeCoefficient= x;
			PTValues.distanceCoefficient= 1-x;
			PTValues.scenario = "time" + PTValues.timeCoefficient + "_dist" + PTValues.distanceCoefficient;

			ScenarioImpl scenario = new TransScenarioLoader().loadScenario(configFile);
			new PlanRouter(scenario);
			System.out.println("Coefficients:" + PTValues.scenario);
			
			//Get rid of only car plans
			PlansFilterByLegMode plansFilter = new PlansFilterByLegMode(TransportMode.car,  PlansFilterByLegMode.FilterType.removeAllPlansWithMode) ;
			plansFilter.run(scenario.getPopulation());

			//split pt connections into plans
			new PTLegIntoPlanConverter().run(scenario);
			
			ComparisonTimeDistancePriority comparisonTimeDistancePriority = new ComparisonTimeDistancePriority(scenario.getPopulation());
						
			int it = (int) (x/0.05);
			a_values [it][0]= comparisonTimeDistancePriority.values[0];
			a_values [it][1]= comparisonTimeDistancePriority.values[1];
			a_values [it][2]= comparisonTimeDistancePriority.values[2];
			a_values [it][3]= comparisonTimeDistancePriority.values[3];
			a_values [it][4]= comparisonTimeDistancePriority.values[4];
			a_values [it][5]= comparisonTimeDistancePriority.values[5];
		}
		
		System.out.println("writing travel time and distances ..." );
		try { 
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("../playgrounds/mmoyo/output/" + new File( "travParatmeters_timecoef_" + PTValues.timeCoefficient + ".txt" ))); 
			 for(int i = 0; i < 22; i++) {
				 bufferedWriter.write("timeCoefficient\ttravelTime\ttravelDistance\ttransfers\tdetTransfers\twalkDistance");
				 bufferedWriter.write( a_values[0] + "\t" + a_values[1] + "\t" + a_values[2] + "\t" + a_values[3] + "\t" + a_values[4] + "\t" + a_values[5] + "\n");
			 }    
			bufferedWriter.close(); 
		} catch (IOException e) {
			System.out.println("done");
		}
	}

}


