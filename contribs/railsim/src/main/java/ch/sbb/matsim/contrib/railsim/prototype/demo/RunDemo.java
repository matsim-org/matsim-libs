package ch.sbb.matsim.contrib.railsim.prototype.demo;

import ch.sbb.matsim.contrib.railsim.prototype.RunRailsim;
import ch.sbb.matsim.contrib.railsim.prototype.analysis.ConvertTrainEventsToDefaultEvents;
import ch.sbb.matsim.contrib.railsim.prototype.analysis.TransitEventHandler;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class RunDemo {

	public static void main(String[] args) {

		System.setProperty("matsim.preferLocalDtds", "true");
//		String [] args0 = {"contribs/railsim/test/input/ch/sbb/matsim/contrib/railsim/prototype/RunRailsimTest/test0/config.xml"}; // one direction
//		String [] args0 = {"contribs/railsim/test/input/ch/sbb/matsim/contrib/railsim/prototype/RunRailsimTest/test1/config.xml"}; // two directions
//		String [] args0 = {"contribs/railsim/test/input/ch/sbb/matsim/contrib/railsim/prototype/RunRailsimTest/test2/config.xml"}; // two directions, several trains
		String[] args0 = {"contribs/railsim/test/input/ch/sbb/matsim/contrib/railsim/prototype/RunRailsimTest/test3/config.xml"}; // T shaped network
//		String [] args0 = {"contribs/railsim/test/input/ch/sbb/matsim/contrib/railsim/prototype/RunRailsimTest/test4/config.xml"}; // Genf-Bern
//		String [] args0 = {"contribs/railsim/test/input/ch/sbb/matsim/contrib/railsim/prototype/RunRailsimTest/test5/config.xml"}; // very short links
//		String [] args0 = {"contribs/railsim/test/input/ch/sbb/matsim/contrib/railsim/prototype/RunRailsimTest/test6/config.xml"}; // crossing
//		String [] args0 = {"contribs/railsim/test/input/ch/sbb/matsim/contrib/railsim/prototype/RunRailsimTest/test7/config.xml"}; // one corridor, very short links
//		String [] args0 = {"contribs/railsim/test/input/ch/sbb/matsim/contrib/railsim/prototype/RunRailsimTest/test8/config.xml"}; // advanced crossing, same links
//		String [] args0 = {"contribs/railsim/test/input/ch/sbb/matsim/contrib/railsim/prototype/RunRailsimTest/test9/config.xml"}; // advanced crossing, crossing...
//		String [] args0 = {"contribs/railsim/test/input/ch/sbb/matsim/contrib/railsim/prototype/RunRailsimTest/test10/config.xml"}; // simple crossing, different links
//		String [] args0 = {"contribs/railsim/test/input/ch/sbb/matsim/contrib/railsim/prototype/RunRailsimTest/test11/config.xml"}; // simplified GE-BN situation, capacity = 2
//		String [] args0 = {"contribs/railsim/test/input/ch/sbb/matsim/contrib/railsim/prototype/RunRailsimTest/test12/config.xml"}; // simplified GE-BN situation, capacity = 1
//		String [] args0 = {"contribs/railsim/test/input/ch/sbb/matsim/contrib/railsim/prototype/RunRailsimTest/test13/config.xml"}; // one direction, passing queue

		Config config = RunRailsim.prepareConfig(args0);
		config.controler().setLastIteration(0);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);
		config.controler().setOutputDirectory("contribs/railsim/test/output/ch/sbb/matsim/contrib/railsim/RunRailsimTest/demo/output");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		OTFVisConfigGroup otfvisCfg = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.class);
		otfvisCfg.setDrawNonMovingItems(false);
		otfvisCfg.setDrawTime(true);
		otfvisCfg.setLinkWidth(20);
		otfvisCfg.setLinkWidthIsProportionalTo("capacity");
		otfvisCfg.setColoringScheme(OTFVisConfigGroup.ColoringScheme.standard);

		Scenario scenario = RunRailsim.prepareScenario(config);
		Controler controler = RunRailsim.prepareControler(scenario);

		controler.addOverridingModule(new OTFVisLiveModule());
		controler.run();

		// read events
		EventsManager events = EventsUtils.createEventsManager();
		TransitEventHandler transitEventHandler = new TransitEventHandler();
		events.addHandler(transitEventHandler);
		events.initProcessing();
		new MatsimEventsReader(events).readFile(config.controler().getOutputDirectory() + "/" + config.controler().getRunId() + ".output_events.xml.gz");
		events.finishProcessing();

		transitEventHandler.printResults(config.controler().getOutputDirectory(), config.controler().getRunId());

		// visualize trains in addition to train path
		ConvertTrainEventsToDefaultEvents analysis = new ConvertTrainEventsToDefaultEvents();
		analysis.run(config.controler().getRunId(), config.controler().getOutputDirectory());
	}

}
