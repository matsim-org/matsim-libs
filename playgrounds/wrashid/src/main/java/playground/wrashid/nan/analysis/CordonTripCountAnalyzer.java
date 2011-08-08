package playground.wrashid.nan.analysis;


import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.lib.GeneralLib;

public class CordonTripCountAnalyzer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String basePath="H:/data/experiments/ARTEMIS/output/run2/";
		String plansFile=basePath + "output_plans.xml.gz";
		String networkFile=basePath + "output_network.xml.gz";
		String facilititiesPath=basePath + "output_facilities.xml.gz";
		ScenarioImpl scenario = (ScenarioImpl) GeneralLib.readScenario(plansFile, networkFile, facilititiesPath);
		
		
		int nuberTripsWorkRelated=0;
		int numberOfTripsShopAndLeisureRelated=0;
		
		Population population = scenario.getPopulation();
		
		for (Person person:population.getPersons().values()){
			Plan plan=person.getSelectedPlan();
			
			Activity prevAct=null;
			Leg prevLeg=null;
			
			for (PlanElement pe:plan.getPlanElements()){
				if (pe instanceof Activity){
					Activity activity=(Activity) pe;
					if (prevLeg!=null && prevLeg.getMode().equalsIgnoreCase("car")){
						
						//a) COMING FROM OC TO C FOR WORK
						if (!isInsideCordon(prevAct) && isInsideCordon(activity)){
							if (equalsActType(activity, "work")){
								nuberTripsWorkRelated++;
							}
						}
						
						//b)LEAVING C FROM WORK TO OC 
						if (isInsideCordon(prevAct) && !isInsideCordon(activity)){
							if (equalsActType(prevAct, "work")){
								nuberTripsWorkRelated++;
							}
						}
						
						//c)COMING FROM C TO C FOR WORK 
						if (isInsideCordon(prevAct) && isInsideCordon(activity)){
							if (equalsActType(activity, "work")){
								nuberTripsWorkRelated++;
							}
						}
						
						//d) LEAVING C FROM WORK TO C
						if (isInsideCordon(prevAct) && isInsideCordon(activity)){
							if (equalsActType(prevAct, "work")){
								nuberTripsWorkRelated++;
							}
						}
						
						//e) COMING FROM OC TO C FOR ACT (shop/leisure)
						if (!isInsideCordon(prevAct) && isInsideCordon(activity)){
							if (equalsActType(activity, "shop") || equalsActType(activity, "leisure")){
								numberOfTripsShopAndLeisureRelated++;
							}
						}
						
						//f) COMING FROM C TO C FOR ACT
						if (isInsideCordon(prevAct) && isInsideCordon(activity)){
							if (equalsActType(activity, "shop") || equalsActType(activity, "leisure")){
								numberOfTripsShopAndLeisureRelated++;
							}
						}						
					}
				
					
					prevAct=activity;
				}
				
				if (pe instanceof Leg){
					prevLeg=(Leg) pe;
				}
			}
			
		}
		
		
		System.out.println("nuberTripsWorkRelated:" + nuberTripsWorkRelated);
		System.out.println("numberOfTripsShopAndLeisureRelated:" + numberOfTripsShopAndLeisureRelated);
		
	}
	
	private static boolean equalsActType(Activity act, String type){
		return act.getType().startsWith(type);
	}
	
	public static boolean isInsideCordon(Coord coord){
		Coord center=new CoordImpl(683400.75,247500.0687); 
		double radius=1000;
		
		return GeneralLib.getDistance(center,coord)<radius;
	}
	
	public static boolean isInsideCordon(Activity act){
		return isInsideCordon(act.getCoord());
	}
	
}
