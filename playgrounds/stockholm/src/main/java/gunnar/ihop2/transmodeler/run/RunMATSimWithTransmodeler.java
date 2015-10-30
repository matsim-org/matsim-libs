package gunnar.ihop2.transmodeler.run;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RunMATSimWithTransmodeler {

	public RunMATSimWithTransmodeler() {
	}

	public static final String TRANSMODELERCONFIG = "transmodeler";

	public static final String EVENTSFILE = "eventsFile";

	public static final String TRANSMODELERFOLDER = "transmodelerFolder";

	public static final String TRANSMODELERCOMMAND = "transmodelerCommand";

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final String configFileName = "./data/run/config.xml";
		final Config config = ConfigUtils.loadConfig(configFileName);
		config.getModule("controler").addParam("lastIteration", "10");

		final ConfigGroup transmodelerConfigGroup = new ConfigGroup(
				TRANSMODELERCONFIG);
		transmodelerConfigGroup.addParam(EVENTSFILE,
				"./data/run/output_BACKUP/ITERS/it.0/0.events.xml.gz");
		transmodelerConfigGroup.addParam(TRANSMODELERFOLDER,
				"[TODO TransmodelerFolder]");
		transmodelerConfigGroup.addParam(TRANSMODELERCOMMAND,
				"[TODO TransmodelerCommand]");
		config.addModule(transmodelerConfigGroup);

		final Controler controller = new Controler(config);
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				binder().bind(Mobsim.class).to(TransmodelerMobsim.class);
			}
		});

		controller.run();

		System.out.println("... DONE");

	}
}
