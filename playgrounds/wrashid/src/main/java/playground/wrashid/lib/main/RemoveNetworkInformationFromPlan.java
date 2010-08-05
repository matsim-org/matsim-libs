package playground.wrashid.lib.main;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;

import playground.wrashid.lib.GeneralLib;


public class RemoveNetworkInformationFromPlan {
	public static void main(String[] args) {
		String inputPlansFile="V:/data/cvs/ivt/studies/switzerland/plans/ivtch/census2000v2_dilZh30km_10pct/plans.xml.gz";
		String inputNetworkFile="V:/data/cvs/ivt/studies/switzerland/networks/ivtch/network.xml";
		String inputFacilities="V:/data/cvs/ivt/studies/switzerland/facilities/facilities.xml.gz";
		
		String outputPlansFile="v:/data/v-temp/plans-new.xml.gz";		
		
		Scenario scenario= GeneralLib.readPopulation(inputPlansFile, inputNetworkFile,inputFacilities);
		
		GeneralLib.removeNetworkInformationFromPlans(scenario.getPopulation());
		
		GeneralLib.writePopulation(scenario.getPopulation(),scenario.getNetwork(),outputPlansFile);
	}
}
