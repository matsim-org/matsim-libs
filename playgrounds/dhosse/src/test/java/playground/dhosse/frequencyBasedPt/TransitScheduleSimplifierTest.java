package playground.dhosse.frequencyBasedPt;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import playground.dhosse.frequencyBasedPt.utils.CreateTestNetwork;
import playground.dhosse.frequencyBasedPt.utils.CreateTestPopulation;
import playground.dhosse.frequencyBasedPt.utils.CreateTestTransit;

public class TransitScheduleSimplifierTest {
	
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testWithoutAndWithSimplifiedSchedule(){
		
//		String output = utils.getOutputDirectory();
//		
//		Config config = ConfigUtils.createConfig();
//		
//		config.scenario().setUseTransit(true);
//		config.scenario().setUseVehicles(true);
//		
//		config.controler().setLastIteration(0);
//		config.controler().setMobsim("qsim");
//		config.setQSimConfigGroup(new QSimConfigGroup());
//		
//		config.planCalcScore().addParam("activityType_0", "h");
//		config.planCalcScore().addParam("activityTypicalDuration_0", "08:00:00");
//		config.planCalcScore().addParam("activityType_1", "w");
//		config.planCalcScore().addParam("activityTypicalDuration_1", "08:00:00");
//		
//		Network network = CreateTestNetwork.createTestNetwork();
//		new NetworkWriter(network).write(output + "network.xml");
//		config.network().setInputFile(output + "network.xml");
//		
//		Population population = CreateTestPopulation.createTestPopulation(network, 100);
//		new PopulationWriter(population, network).write(output + "plans.xml");
//		config.plans().setInputFile(output + "plans.xml");
//		
//		config.strategy().addParam("Module_1", "ChangeExpBeta");
//		config.strategy().addParam("ModuleProbability_1", "0.5");
//		config.strategy().addParam("Module_2", "ReRoute");
//		config.strategy().addParam("ModuleProbability_2", "0.5");
//		
//		for(int i = 0; i < 2; i++){
//			
//			TransitSchedule schedule = CreateTestTransit.createTestSchedule(network, i+1);
//			new TransitScheduleWriter(schedule).writeFileV1(output + "schedule" + i + ".xml");
//			config.transit().setTransitScheduleFile(output + "schedule" + i + ".xml");
//			
//			Vehicles vehicles = CreateTestTransit.createTestTransitVehicles();
//			new VehicleWriterV1(vehicles).writeFile(output + "vehicles" + i + ".xml");
//			config.transit().setVehiclesFile(output + "vehicles" + i + ".xml");
//			
//			Scenario scenario = ScenarioUtils.loadScenario(config);
//		
//			config.controler().setOutputDirectory(output + "run"+i);
//			Controler ctrl = new Controler(config);
//			ctrl.setOverwriteFiles(true);
//			ctrl.run();
//			
//		}
		
	}

}
