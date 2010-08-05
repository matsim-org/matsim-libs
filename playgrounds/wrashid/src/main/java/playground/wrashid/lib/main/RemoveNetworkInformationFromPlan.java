package playground.wrashid.lib.main;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;

import playground.wrashid.lib.GeneralLib;

public class RemoveNetworkInformationFromPlan {
	public static void main(String[] args) {
		String inputPlansFile="V:/data/cvs/ivt/studies/switzerland/plans/ivtch/census2000v2_dilZh30km_10pct/plans.xml.gz";
		String inputNetworkFile="V:/data/cvs/ivt/studies/switzerland/networks/ivtch/network.xml";
		
		String outputPlansFile="v:/data/v-temp/plans-new.xml.gz";
		String networkOfTargetPlansFile="V:/data/cvs/ivt/studies/switzerland/networks/navteq/network.xml";
		
		
		
		Scenario scenario = GeneralLib.readPopulation(inputPlansFile, inputNetworkFile);
		Network networkOld=scenario.getNetwork();
		
		Network networkNew=GeneralLib.readNetwork(inputNetworkFile);
		
		GeneralLib.writePopulation(scenario.getPopulation(),networkNew,outputPlansFile);
	}
}
