package org.matsim.dsim.simulation.net;

import lombok.RequiredArgsConstructor;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.dsim.simulation.SimStepMessaging;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public class ActiveLinks implements Steppable {

	private final Set<SimLink> activeLinks = new HashSet<>();
	private final SimStepMessaging simStepMessaging;

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
		}
	}
}
