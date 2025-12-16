package org.matsim.dsim.simulation.net;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.dsim.simulation.SimStepMessaging;

import java.util.HashMap;
import java.util.Map;

public class ActiveLinks implements Steppable {

	private final Map<Id<Link>, SimLink> activeLinks = new HashMap<>();
	private final SimStepMessaging simStepMessaging;

	@Inject
	public ActiveLinks(SimStepMessaging simStepMessaging) {
		this.simStepMessaging = simStepMessaging;
	}

	void activate(SimLink link) {
		activeLinks.put(link.getId(), link);
	}

	@Override
	public void doSimStep(double now) {
		var it = activeLinks.entrySet().iterator();
		while (it.hasNext()) {
			var link = it.next().getValue();
			var keepActive = link.doSimStep(simStepMessaging, now);

			if (!keepActive) {
				it.remove();
			}
		}
	}

	@Override
	public String toString() {
		return "#links=" + activeLinks.size();
	}
}
