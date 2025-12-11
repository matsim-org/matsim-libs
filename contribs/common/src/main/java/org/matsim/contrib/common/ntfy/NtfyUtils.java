package org.matsim.contrib.common.ntfy;

import org.matsim.core.controler.Controller;

/**
 * @author sebhoerl
 */
public class NtfyUtils {
    private NtfyUtils() {
    }

    static public void install(Controller controller) {
        controller.addOverridingModule(new NtfyModule());
    }

    static public void install(Controller controller, String topic) {
        NtfyConfigGroup config = NtfyConfigGroup.get(controller.getConfig(), true);
        config.setTopic(topic);
        controller.addOverridingModule(new NtfyModule());
    }

    static public void install(Controller controller, String topic, String simulationName) {
        NtfyConfigGroup config = NtfyConfigGroup.get(controller.getConfig(), true);
        config.setTopic(topic);
        config.setSimulationName(simulationName);
        controller.addOverridingModule(new NtfyModule());
    }
}
