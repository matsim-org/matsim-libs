package tutorial.programming.exampleMobsimListener;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;

public class RunMobsimListenerExample {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setLastIteration(1);
        Controler controler = new Controler(config);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
            	// has to be an eagerSingleton because if not for every mobsim an own listener will
            	// be created so one listener will never listen to multiple  mobsims
                addMobsimListenerBinding().to(CountingMobsimListener.class).asEagerSingleton();;
            }
        });
        controler.run();
	}
	
    private static class CountingMobsimListener implements MobsimInitializedListener {

        int count = 0;

        @Override
        public void notifyMobsimInitialized(MobsimInitializedEvent e) {
            count++;
            if (count > 1) {
                throw new RuntimeException("This mobsim listener ran more than once.");
            }
        }

    }
	
}
