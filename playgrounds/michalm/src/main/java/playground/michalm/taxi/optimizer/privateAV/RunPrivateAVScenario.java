package playground.michalm.taxi.optimizer.privateAV;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiOptimizerModules;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;


public class RunPrivateAVScenario {

  public static void main(String[] args) {
    RunPrivateAVScenario.run("config.xml", "somept_small", false);

  }

  private static void run(String configFile, String runName, boolean runTaxis) {
    // TODO Auto-generated method stub
	  Config config = ConfigUtils.loadConfig(configFile);
	  config.controler().setRunId(runName);
	  config.controler().setOutputDirectory("output/" + runName);
    
	  config.network().setInputFile("input/network.xml.gz");
	  config.facilities().setInputFile("input/facilities.xml");
	  config.plans().setInputFile("input/smallpopulation.xml");
	  config.vehicles().setVehiclesFile("input/vehicles.xml");
	  
	  // set controler 
	  Controler controler = createTaxiControler(config, runTaxis, "input/vehicles.xml");
	  
		controler.run();

  }
  
  
  public static Controler createTaxiControler(Config config, boolean runTaxis, String taxisFile){


    Controler controler = new Controler(config);
    
    if(runTaxis){
      config.addModule(new TaxiConfigGroup());
      config.addModule(new DvrpConfigGroup());

      DvrpConfigGroup.get(config).setMode(TaxiOptimizerModules.TAXI_MODE);
      TaxiConfigGroup taxi = (TaxiConfigGroup) config.getModules().get(TaxiConfigGroup.GROUP_NAME); 
      taxi.setTaxisFile(taxisFile);

      config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
      config.checkConsistency();

      Scenario scenario = ScenarioUtils.loadScenario(config);
      FleetImpl fleet = new FleetImpl();
      new VehicleReader(scenario.getNetwork(), fleet).readFile(taxi.getTaxisFileUrl(config.getContext()).getFile());
      
      controler.addOverridingModule(TaxiOptimizerModules.createModule(fleet, PrivateATOptimizerProvider.class));

      
    }
    
    
    return controler;
    
  }
  

}
