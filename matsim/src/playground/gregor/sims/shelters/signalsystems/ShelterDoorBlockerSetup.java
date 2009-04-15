package playground.gregor.sims.shelters.signalsystems;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.basic.network.BasicLane;
import org.matsim.core.basic.network.BasicLaneDefinitionsBuilder;
import org.matsim.core.basic.network.BasicLaneDefinitionsBuilderImpl;
import org.matsim.core.basic.network.BasicLanesToLinkAssignment;
import org.matsim.core.basic.signalsystems.BasicSignalGroupDefinition;
import org.matsim.core.basic.signalsystems.BasicSignalSystemDefinition;
import org.matsim.core.basic.signalsystems.BasicSignalSystemsBuilder;
import org.matsim.core.basic.signalsystems.BasicSignalSystemsImpl;
import org.matsim.core.basic.signalsystemsconfig.BasicAdaptiveSignalSystemControlInfo;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemConfiguration;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemConfigurationsBuilder;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkLayer;

import playground.gregor.sims.shelters.ShelterEvacuationController;

public class ShelterDoorBlockerSetup implements StartupListener{

	public void notifyStartup(StartupEvent event) {
		
		if (!(event.getControler() instanceof ShelterEvacuationController) ) {
			throw new RuntimeException("This code only works with ShelterEvacuationController");
		}
		ShelterEvacuationController c = (ShelterEvacuationController) event.getControler();
		List<Link> doorBlockerLinks = getDoorBlockerLinks(c.getNetwork());
		BasicSignalSystemsImpl bssi = new BasicSignalSystemsImpl();
		BasicLaneDefinitionsBuilder bldb = new BasicLaneDefinitionsBuilderImpl();
		BasicSignalSystemsBuilder bssb = bssi.getSignalSystemsBuilder();
		BasicSignalSystemConfigurationsBuilder sscb = new BasicSignalSystemConfigurationsBuilder();
		
		
		int laneId = 0;
		int lsdId = 0;
		int lsgId = 0;
		
		for (Link link : doorBlockerLinks) {

			BasicLanesToLinkAssignment b = bldb.createLanesToLinkAssignment(link.getId());
			
			Id toLink = link.getToNode().getOutLinks().values().iterator().next().getId();
			BasicLane lane = bldb.createLane(new IdImpl(laneId++));
			lane.addToLinkId(toLink);
			lane.setLength(link.getLength()/2.);
			
			b.addLane(lane);
			
			Id id = new IdImpl(lsdId++);
			
			BasicSignalSystemDefinition ssd = bssb.createLightSignalSystemDefinition(id);

			Id id2 = new IdImpl(lsgId);
			BasicSignalGroupDefinition ssg = bssb.createLightSignalGroupDefinition(link.getId(), id2 );
			ssg.addLaneId(lane.getId());
			ssg.addToLinkId(toLink);
			ssg.setLightSignalSystemDefinitionId((IdImpl) id);
			
			BasicSignalSystemConfiguration conf = sscb.createSignalSystemConfiguration(id);
			BasicAdaptiveSignalSystemControlInfo adaptiveControlInfo = sscb.createAdaptiveSignalSystemControlInfo();
			adaptiveControlInfo.setAdaptiveControlerClass("playground.gregor.sims.shelters.signalsystems.SheltersDoorBlockerController.class");
			adaptiveControlInfo.addSignalGroupId(id2);
			
			conf.setSignalSystemControlInfo(adaptiveControlInfo);
						
		}
		
		
		
	}

	private List<Link> getDoorBlockerLinks(NetworkLayer network) {
		List<Link> ret = new ArrayList<Link>();
		for (Link link : network.getLinks().values()) {
			if (link.getId().toString().contains("sl") && link.getId().toString().contains("a")) {
				ret.add(link);
			}
		}
		return ret;
	}

}
