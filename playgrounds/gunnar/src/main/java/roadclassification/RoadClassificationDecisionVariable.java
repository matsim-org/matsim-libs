package roadclassification;


import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.utils.objectattributes.ObjectAttributes;

import floetteroed.opdyts.DecisionVariable;

class RoadClassificationDecisionVariable implements DecisionVariable {

	static Logger log = Logger.getLogger(RoadClassificationDecisionVariable.class);


	public List<LinkSettings> getLinkSettingses() {
		return linkSettingses;
	}

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
		log.info("--DecisionVariable follows--");
		for (LinkSettings linkSettings : getLinkSettingses()) {
			log.info(String.format("%d %d %d\n", (int) linkSettings.getCapacity(), (int) linkSettings.getFreespeed(), (int) linkSettings.getNofLanes()));
		}
		for (Link link : network.getLinks().values()) {
			LinkSettings roadCategory = linkSettingses.get((int) linkAttributes.getAttribute(link.getId().toString(), "roadCategory"));
			link.setFreespeed(roadCategory.getFreespeed());
			link.setCapacity(roadCategory.getCapacity());
			link.setNumberOfLanes(roadCategory.getNofLanes());
		}
	}

}
