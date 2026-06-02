package org.matsim.codeexamples.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.net.URL;

public class RunPtExample {
    public static void main(String[] args) {
        URL ptScenarioURL = ExamplesUtils.getTestScenarioURL("pt-tutorial");

        Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ptScenarioURL, "0.config.xml"));
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controller().setLastIteration(0);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controller controller = ControllerUtils.createController(scenario);

        controller.run();
    }
}
