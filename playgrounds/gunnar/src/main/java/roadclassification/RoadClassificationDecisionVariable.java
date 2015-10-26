package roadclassification;


import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.utils.objectattributes.ObjectAttributes;

import floetteroed.opdyts.DecisionVariable;

class RoadClassificationDecisionVariable implements DecisionVariable {

	private final List<LinkSettings> linkSettingses;
	private final ObjectAttributes linkAttributes;
	private Network network;

	public RoadClassificationDecisionVariable(final Network network, ObjectAttributes linkAttributes, List<LinkSettings> linkSettingses) {
		this.network = network;
		this.linkAttributes = linkAttributes;
		this.linkSettingses = linkSettingses;
	}

	@Override
	public final void implementInSimulation() {
		for (Link link : network.getLinks().values()) {
			LinkSettings roadCategory = linkSettingses.get((int) linkAttributes.getAttribute(link.getId().toString(), "roadCategory"));
			link.setFreespeed(roadCategory.getFreespeed());
			link.setCapacity(roadCategory.getCapacity());
			link.setNumberOfLanes(roadCategory.getNofLanes());
		}
	}

}
