package org.matsim.codeexamples.mobsim.mobsimListener;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

/**
 * This is a minor example for how to bind a MobsimListener in matsim.
 * @author gthunig
 *
 */
public class RunMobsimListenerExample {
	
	public static String outputDirectory = "output/mobsimListenerExample/";

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("scenarios/equil/config.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(1);
		config.controler().setOutputDirectory(outputDirectory);
		Controler controler = new Controler(config);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addMobsimListenerBinding().to(CountingMobsimListener.class) ;
			}
		});
		controler.run();
	}

	private static class CountingMobsimListener implements MobsimBeforeSimStepListener {

		private int step = 0;

		@Override
		public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
			LogManager.getLogger(this.getClass()).info("We are at step " + step + ". Note that this restarts counting at zero in every iteration,"
					+ " implying that the class is re-instantiated in every iteration.");
			step++ ;
		}

	}

}
