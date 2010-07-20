package playground.wrashid.parkingSearch.planLevel.initDemand;

import java.util.HashMap;

import org.jgap.gp.function.If;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;

import playground.wrashid.parkingSearch.planLevel.ranking.ClosestParkingMatrix;
import playground.wrashid.tryouts.plan.NewPopulation;

/*
 * just reads an input plans file and produces an output on the console of activity chains and their occurance in percentage.
 */

public class MainGenerateParkingAcitivities{

	public static void main(String[] args) {
		String inputPlansFilePath = "C:\\data\\workspace\\playgrounds\\wrashid\\test\\scenarios\\berlin\\plans_hwh_1pct.xml.gz";
		String outputPlansFilePath = "C:\\data\\workspace\\playgrounds\\wrashid\\test\\scenarios\\berlin\\plansWithParkingActs.xml.gz";
		String networkFilePath = "C:\\data\\workspace\\playgrounds\\wrashid\\test\\scenarios\\berlin\\network.xml.gz";
		String facilitiesFilePath = "C:\\data\\workspace\\playgrounds\\wrashid\\test\\scenarios\\berlin\\facilities.xml";
		
		PlanGeneratorWithParkingActivities pghc=new PlanGeneratorWithParkingActivities(inputPlansFilePath, networkFilePath, facilitiesFilePath);
		
		pghc.processPlans();

		pghc.writePlans(outputPlansFilePath);
		
	}

	

	
}
