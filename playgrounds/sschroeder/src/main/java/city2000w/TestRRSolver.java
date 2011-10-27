package city2000w;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanReader;
import org.matsim.contrib.freight.carrier.CarrierPlanWriter;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.Collection;

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
