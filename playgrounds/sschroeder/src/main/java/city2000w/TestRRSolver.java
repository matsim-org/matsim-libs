package city2000w;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.mzilske.freight.carrier.Carrier;
import playground.mzilske.freight.carrier.CarrierPlan;
import playground.mzilske.freight.carrier.CarrierPlanReader;
import playground.mzilske.freight.carrier.CarrierPlanWriter;

public class TestRRSolver {
	
	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(Level.INFO);
		Config config = new Config();
		config.addCoreModules();
		ScenarioImpl scenario = (ScenarioImpl)ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile("networks/grid.xml");
		Collection<Carrier> carrierImpls = new ArrayList<Carrier>();
		new CarrierPlanReader(carrierImpls).read("anotherInput/testCarriers.xml");
		for(Carrier carrier : carrierImpls){
			VRPCarrierPlanBuilder carrierPlanBuilder = new VRPCarrierPlanBuilder(carrier.getCarrierCapabilities(), carrier.getContracts(), scenario.getNetwork());
			CarrierPlan plan = carrierPlanBuilder.buildPlan();
			if(plan != null){
				carrier.setSelectedPlan(plan);
			}
		}
		new CarrierPlanWriter(carrierImpls).write("output/testCarriers.xml");
	}

}
