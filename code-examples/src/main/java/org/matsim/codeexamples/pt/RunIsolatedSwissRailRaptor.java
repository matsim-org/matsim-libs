package org.matsim.codeexamples.pt;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorRoutingModule;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorRoutingModuleProvider;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.net.URL;
import java.util.List;

public class RunIsolatedSwissRailRaptor {
    public static void main(String[] args) {
        URL ptScenarioURL = ExamplesUtils.getTestScenarioURL("pt-tutorial");
        Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ptScenarioURL, "0.config.xml"));
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controller controller = ControllerUtils.createController(scenario);

        RoutingModule swissRailRaptor = controller.getInjector().getInstance(Key.get(RoutingModule.class, Names.named("pt")));

        RoutingRequest request = new RoutingRequest() {
            @Override
            public Facility getFromFacility() {
                return new LinkWrapperFacility(scenario.getNetwork().getLinks().get(Id.createLinkId("3323")));
            }

            @Override
            public Facility getToFacility() {
                return new LinkWrapperFacility(scenario.getNetwork().getLinks().get(Id.createLinkId("1121")));
            }

            @Override
            public double getDepartureTime() {
                return 7 * 3600;
            }

            @Override
            public Person getPerson() {
                return null;
            }

            @Override
            public Attributes getAttributes() {
                return null;
            }
        };

        List<? extends PlanElement> planElements = swissRailRaptor.calcRoute(request);

        planElements.forEach(System.out::println);
    }
}
