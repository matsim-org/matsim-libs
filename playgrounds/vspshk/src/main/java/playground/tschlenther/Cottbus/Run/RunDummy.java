/**
 * 
 */
package playground.tschlenther.Cottbus.Run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * @author Tilmann Schlenther
 *
 */
public class RunDummy {


	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("C:/Users/Tille/WORK/Cottbus/Cottbus-pt/INPUT_mod/config_1.xml");
		config.controler().setOutputDirectory("C:/Users/Tille/WORK/Cottbus/Cottbus-pt/ADDEDLINKS_output");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.transit().setUseTransit(true);
		config.transit().setTransitScheduleFile("C:/Users/Tille/WORK/Cottbus/Cottbus-pt/INPUT_mod/schedule.xml");
		config.network().setInputFile("C:/Users/Tille/WORK/Cottbus/Cottbus-pt/INPUT_mod/ADDEDLINKS_cap60.xml");
		config.plans().setInputFile("C:/Users/Tille/WORK/Cottbus/Cottbus-pt/INPUT_mod/plans_scale0.015.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
//		LinkModifier mod = new LinkModifier(scenario);
////		mod.modifyLinkCapacity(Id.createLinkId("ptl62"), 300);
////		mod.modifyLinkCapacity(Id.createLinkId("ptb62"), 300);
//		
//		
//		Id<Link> ptb62 = Id.createLinkId("ptb62");
//		Id<Link> ptl62 = Id.createLinkId("ptl62");
//		Id<Link> ptb61 = Id.createLinkId("ptb61");
//		Id<Link> ptl61 = Id.createLinkId("ptl61");
//		
//		Id<Node> kreuzung = Id.createNodeId("31198080_31198081"); 
//		Id<Node> stop = Id.createNodeId("pt123");
//		
//		mod.modifyToNode(ptb61, kreuzung);
//		mod.modifyFromNode(ptl61, kreuzung);
//		
////		scenario.getNetwork().getFactory().
////		scenario.getNetwork().getLinks().remove(ptb62);
//		
//		mod.writeNetwork("C:/Users/Tille/WORK/Cottbus/Cottbus-pt/network_pt_modified_removed.xml");
		
		Controler controler = new Controler(scenario);
		controler.run();
	
	}

	private static void createPopulation(Scenario scenario) {
		Population pop = scenario.getPopulation();
		Person lonely = pop.getFactory().createPerson(Id.createPersonId("lonely"));
		Plan plan = pop.getFactory().createPlan();
		lonely.addPlan(plan);
		pop.addPerson(lonely);
	}
}
