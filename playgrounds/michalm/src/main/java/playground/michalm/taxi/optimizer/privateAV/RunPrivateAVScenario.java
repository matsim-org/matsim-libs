package playground.michalm.taxi.optimizer.privateAV;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
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
    RunPrivateAVScenario.run("config.xml", "someav_small");
  }

  private static void run(String configFile, String runName) {
    // TODO Auto-generated method stub
    Config config = ConfigUtils.loadConfig(configFile,
        new DvrpConfigGroup(),
        new TaxiConfigGroup(),
        new TaxiFareConfigGroup());
    
    config.controler().setRunId(runName);
    config.controler().setOutputDirectory("output/" + runName);

    
    config.network().setInputFile("input/network.xml.gz");
    config.facilities().setInputFile("input/facilities.xml.gz");
    config.plans().setInputFile("input/smallpopulation.xml");
    config.vehicles().setVehiclesFile("input/vehicles.xml");
    
    // set controler 
    createTaxiControler(config).run();

  }
  
  
  public static Controler createTaxiControler(Config config){
    
    // added in response to error in run
    config.qsim().addParam("simStarttimeInterpretation", "onlyUseStarttime");
    config.qsim().setStartTime(0);

    TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(config);
    config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
    config.checkConsistency();


    Scenario scenario = ScenarioUtils.loadScenario(config);
    FleetImpl fleet = new FleetImpl();
    new VehicleReader(scenario.getNetwork(), fleet).parse(taxiCfg.getTaxisFileUrl(config.getContext()));
      
    Controler controler = new Controler(config);
    
    controler.addOverridingModule(
        TaxiOptimizerModules.createModule(fleet, PrivateATOptimizerProvider.class));

    return controler;
    
  }
  

}