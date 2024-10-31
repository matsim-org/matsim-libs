package org.matsim.dsim.simulation.net;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.dsim.simulation.SimStepMessaging;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class ActiveLinks implements Steppable {

    private final Set<SimLink> activeLinks = new HashSet<>();
    private final SimStepMessaging simStepMessaging;

    @Setter
    private Consumer<Id<Node>> activateNode;

    void activate(SimLink link) {
        activeLinks.add(link);
    }

    @Override
    public void doSimStep(double now) {
        var it = activeLinks.iterator();
        while (it.hasNext()) {
            var link = it.next();
            var keepActive = link.doSimStep(simStepMessaging, now);

            if (!keepActive) {
                it.remove();
            }
            if (link.isOffering()) {
                activateNode.accept(link.getToNode());
            }
        }
    }
}
