package org.matsim.core.controler;


import com.google.inject.Provider;
import org.junit.Assert;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import javax.inject.Inject;
import java.util.Map;

public class OverrideCarTraveltimeTest {

    public static void main(String[] args) {
        final Config config = ConfigUtils.createConfig();
        config.controler().setLastIteration(1);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        Controler controler = new Controler(ScenarioUtils.createScenario(config));
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bindCarTravelDisutilityFactory().to(InterestingTravelDisutilityFactory.class);
                bindCarTravelTime().to(InterestingTravelTime.class);
                addControlerListenerBinding().to(InterestingControlerListener.class);
            }
        });
        controler.run();
    }

    private static class InterestingTravelTime implements TravelTime {
        @Override
        public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
            return 42.0;
        }
    }

    private static class InterestingTravelDisutilityFactory implements TravelDisutilityFactory {
        @Override
        public TravelDisutility createTravelDisutility(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
            return new TravelDisutility() {
                @Override
                public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
                    return 37.0;
                }

                @Override
                public double getLinkMinimumTravelDisutility(Link link) {
                    return 37.0;
                }
            };
        }
    }

    private static class InterestingControlerListener implements ReplanningListener {

        @Inject
        Provider<ReplanningContext> c;

        @Override
        public void notifyReplanning(ReplanningEvent event) {
            Assert.assertEquals(42.0, c.get().getTravelTime().getLinkTravelTime(null, 0.0, null, null), 0.0);
            Assert.assertEquals(37.0, c.get().getTravelDisutility().getLinkTravelDisutility(null, 0.0, null, null), 0.0);
        }
    }
}
