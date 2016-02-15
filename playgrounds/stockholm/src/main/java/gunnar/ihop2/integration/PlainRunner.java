package gunnar.ihop2.integration;

import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.router.InvertedNetworkRoutingModuleModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.lanes.data.v11.LaneDefinitonsV11ToV20Converter;

public class PlainRunner {

	public static void main(String[] args) {

		String configFileName = "./input/matsim-config.xml";
		Config config = ConfigUtils.loadConfig(configFileName);

		// TODO Gunnar had to change this, otherwise it infers u-turns that should not be!
		LaneDefinitonsV11ToV20Converter.main(new String[] {
				"./input/lanes.xml", "./input/lanes20.xml",
				"./input/network-plain.xml" });

		config.network().setLaneDefinitionsFile("./input/lanes20.xml");
		config.qsim().setUseLanes(true);
		config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
		config.controler().setLinkToLinkRoutingEnabled(true);
		config.controler()
				.setOverwriteFileSetting(
						OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		Controler controler = new Controler(config);
		controler.addOverridingModule(new SignalsModule()); // TODO NEEDED?
		controler.addOverridingModule(new InvertedNetworkRoutingModuleModule());

		controler.run();

	}

}
