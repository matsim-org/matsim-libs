package playground.sergioo.mixedtraffic2016;

import java.util.Arrays;
import java.util.HashSet;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.scenario.ScenarioUtils;

public class PrepareScenario {

	public static void main(String[] args) {
		Controler controler = new Controler(ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0])));
		controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		/*CrowdingSpeedCalculator csc = new CrowdingSpeedCalculator(new HashSet<>(Arrays.asList(args[1].split(","))), Double.parseDouble(args[2]));
		final ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory( controler.getEvents(), controler.getScenario() ) ;
		factory.setLinkSpeedCalculator(csc);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(QNetworkFactory.class).toInstance(factory);
				addEventHandlerBinding().toInstance(csc);
			}
		});*/
		controler.run();
	}

}
