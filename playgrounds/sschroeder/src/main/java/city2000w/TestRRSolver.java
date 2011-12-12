package city2000w;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanReader;
import org.matsim.contrib.freight.carrier.CarrierPlanWriter;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.vrp.VRPCarrierPlanBuilder;
import org.matsim.contrib.freight.vrp.api.Costs;
import org.matsim.contrib.freight.vrp.basics.CrowFlyCosts;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class TestRRSolver {
	
	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(Level.INFO);
		Config config = new Config();
		config.addCoreModules();
		ScenarioImpl scenario = (ScenarioImpl)ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile("networks/grid.xml");
		Collection<Carrier> carrierImpls = new ArrayList<Carrier>();
		new CarrierPlanReader(new Carriers(carrierImpls)).read("anotherInput/testCarriers.xml");
		Costs costs = new CrowFlyCosts();
		for(Carrier carrier : carrierImpls){
			VRPCarrierPlanBuilder carrierPlanBuilder = new VRPCarrierPlanBuilder(carrier.getCarrierCapabilities(), carrier.getContracts(), scenario.getNetwork(), costs);
			CarrierPlan plan = carrierPlanBuilder.buildPlan();
			if(plan != null){
				carrier.setSelectedPlan(plan);
			}
		}
		new CarrierPlanWriter(carrierImpls).write("output/testCarriers.xml");
	}

}
