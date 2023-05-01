package ch.sbb.matsim.contrib.railsim.prototype;

import ch.sbb.matsim.contrib.railsim.prototype.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.prototype.RailsimUtils;
import ch.sbb.matsim.contrib.railsim.prototype.RunRailsim;
import org.apache.logging.log4j.LogManager;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import ch.sbb.matsim.contrib.railsim.prototype.RailsimConfigGroup.TrainAccelerationApproach;
import ch.sbb.matsim.contrib.railsim.prototype.RailsimConfigGroup.TrainSpeedApproach;
import ch.sbb.matsim.contrib.railsim.prototype.analysis.ConvertTrainEventsToDefaultEvents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Ihab Kaddoura
 */
public class RunRailsimAdvancedCorridorTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void test0() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getInputDirectory() + "config.xml"}; // one direction

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			RailsimConfigGroup rscfg = ConfigUtils.addOrGetModule(config, RailsimConfigGroup.class);
			rscfg.setSplitLinks(true);
			rscfg.setSplitLinksLength(200.);
			rscfg.setTrainAccelerationApproach(TrainAccelerationApproach.speedOnPreviousLink);
			rscfg.setTrainSpeedApproach(TrainSpeedApproach.fromLinkAttributesForEachVehicleType);

			Scenario scenario = RunRailsim.prepareScenario(config);

			// set minimum time for one link
			scenario.getNetwork().getLinks().get(Id.createLinkId("15")).getAttributes().putAttribute(RailsimUtils.LINK_ATTRIBUTE_MINIMUM_TIME, 30 * 60.);

			// set different maximum speed levels for different train types
			for (Link link : scenario.getNetwork().getLinks().values()) {
				link.getAttributes().putAttribute("Express", 160. / 3.6);
				link.getAttributes().putAttribute("Regio", 160. / 3.6);
				link.getAttributes().putAttribute("Cargo", 80. / 3.6);
			}

//            List<Link> linksToAdd = new ArrayList<>();
//            for (Link link : scenario.getNetwork().getLinks().values()) {
//            	Link linkR = scenario.getNetwork().getFactory().createLink(Id.createLinkId(link.getId().toString() + "_r"), link.getToNode(), link.getFromNode());
//            	linkR.setAllowedModes(link.getAllowedModes());
//            	linkR.setCapacity(link.getCapacity());
//            	linkR.setLength(link.getLength());
//            	linkR.setFreespeed(link.getFreespeed());
//            	linkR.setNumberOfLanes(link.getNumberOfLanes());
//            	for (String attribute : link.getAttributes().getAsMap().keySet()) {
//            		linkR.getAttributes().putAttribute(attribute, link.getAttributes().getAttribute(attribute));
//            	}
//
//            	linksToAdd.add(linkR);
//            }
//            for (Link linkR : linksToAdd) {
//            	scenario.getNetwork().addLink(linkR);
//            }

			// add some one direction links
//            scenario.getNetwork().getLinks().get(Id.createLinkId("20")).getAttributes().putAttribute(RailsimUtils.LINK_ATTRIBUTE_OPPOSITE_DIRECTION, "20_r");
//            scenario.getNetwork().getLinks().get(Id.createLinkId("20_r")).getAttributes().putAttribute(RailsimUtils.LINK_ATTRIBUTE_OPPOSITE_DIRECTION, "20");

//            scenario.getNetwork().getLinks().get(Id.createLinkId("36")).getAttributes().putAttribute(RailsimUtils.LINK_ATTRIBUTE_OPPOSITE_DIRECTION, "36_r");
//            scenario.getNetwork().getLinks().get(Id.createLinkId("36_r")).getAttributes().putAttribute(RailsimUtils.LINK_ATTRIBUTE_OPPOSITE_DIRECTION, "36");

//            scenario.getNetwork().getLinks().get(Id.createLinkId("DUG")).getAttributes().putAttribute(RailsimUtils.LINK_ATTRIBUTE_OPPOSITE_DIRECTION, "DUG_r");
//            scenario.getNetwork().getLinks().get(Id.createLinkId("DUG_r")).getAttributes().putAttribute(RailsimUtils.LINK_ATTRIBUTE_OPPOSITE_DIRECTION, "DUG");

			Controler controler = RunRailsim.prepareControler(scenario);

			controler.run();

			// visualize trains in addition to train path
			ConvertTrainEventsToDefaultEvents analysis = new ConvertTrainEventsToDefaultEvents();
			analysis.run(config.controler().getRunId(), utils.getOutputDirectory());

		} catch (Exception ee) {
			ee.printStackTrace();
			LogManager.getLogger(this.getClass()).fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

}
