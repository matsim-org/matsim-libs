package playground.sergioo.mixedTraffic2017;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import playground.sergioo.weeklySimulation.scenario.ScenarioUtils;

/**
 * Created by sergioo on 24/2/17.
 */
public class RunControler {
    public static void main(String[] args) {
        final Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0]));
        final Controler controler = new Controler(scenario);
        /*controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
            bindMobsim().toProvider(new Provider<Mobsim>() {
                @Override
                public Mobsim get() {
                    return new
                }
            });
            }
        });*/
        controler.run();
    }
}
