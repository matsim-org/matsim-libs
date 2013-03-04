package energy;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import energy.controlers.DisChargingControler;



public class ElectricBerlinMain {
	
	private Scenario sc;
	
	private static final String DIR = "\\\\vsp-nas/jbischoff/WinHome/Docs/svn-checkouts/volkswagen_internal/";
//	private static final String DIR = "/home/dgrether/shared-svn/projects/volkswagen_internal/";
	public static final String IDENTIFIER = "emob_";
	private static final String CONFIG = DIR + "scenario/config_empty_scenario.xml";
//	private static final String CONFIG = DIR + "scenario/config_empty_scenario.xml";
	private static final String ADDPLANS = DIR + "scenario/input/testPlans.xml";
	
	
	
	public void loadScenario(String configFile, String identifier, String additionalPlansFile){
		this.sc = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
		
		if(!(additionalPlansFile == null)){
			PopulationFactory f = this.sc.getPopulation().getFactory();
			Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			new MatsimNetworkReader(sc).readFile(this.sc.getConfig().getParam(NetworkConfigGroup.GROUP_NAME, "inputNetworkFile"));
			new MatsimPopulationReader(sc).readFile(additionalPlansFile);
			Person newPerson;
			for(Person p: sc.getPopulation().getPersons().values()){
				newPerson = f.createPerson(this.sc.createId(identifier + p.getId().toString()));
				newPerson.addPlan(p.getSelectedPlan());
				this.sc.getPopulation().addPerson(newPerson);
			}
		}
		
	}
	
	
	
	public void run(){
//		Controler c = new DisChargingControler(this.sc);
//		c.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		
	}
	public static void main(String[] args){
		ElectricBerlinMain runner = new ElectricBerlinMain();
		runner.loadScenario(CONFIG, IDENTIFIER, ADDPLANS);
		runner.run();
	}
	
	
}
