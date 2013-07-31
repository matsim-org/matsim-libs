package playground.wrashid.thelma.lav;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.wrashid.fd.AbstractDualSimHandler;

public class MainOrientationAndSlopeMerged {

	public static void main(String[] args) {
		String networkFile = "H:/data/experiments/TRBAug2011/runs/ktiRun24/output/output_network.xml.gz";
		String eventsFile =  "H:/data/experiments/TRBAug2011/runs/ktiRun24/output/ITERS/it.50/50.events.xml.gz";
		
		String linkSlopeAttributeFile =  "C:/tmp/New folder (2)/teleAtlasLinks.xml.gz";
		
		ObjectAttributes linkSlopes = new ObjectAttributes();
		new ObjectAttributesXmlReader(linkSlopes).parse(linkSlopeAttributeFile);
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		
		EventsManager events = EventsUtils.createEventsManager();
		InfoCollector ic=new InfoCollector(scenario.getNetwork(),linkSlopes);

		events.addHandler(ic); 

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		
		System.out.println("personId" + "\t" + "linkLength" + "\t" + "linkFreespeed"  + "\t" + "linkEnterTime" + "\t" + "linkLeaveTime" + "\t" + "slope" + "\t" + "deltaX"  + "\t" + "deltaY");
		reader.parse(eventsFile);
	}
	
	private static class InfoCollector extends AbstractDualSimHandler{

		private Network network;
		private ObjectAttributes linkSlopes;

		public InfoCollector(Network network, ObjectAttributes linkSlopes){
			this.network = network;
			this.linkSlopes = linkSlopes;
		}
		
		@Override
		public boolean isJDEQSim() {
			return true;
		}

		@Override
		public boolean isLinkPartOfStudyArea(Id linkId) {
			return true;
		}

		@Override
		public void processLeaveLink(Id linkId, Id personId, double enterTime,
				double leaveTime) {
			Link link = network.getLinks().get(linkId);
			double linkLength=link.getLength();
			double deltaX=link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX();
			double deltaY=link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY();
			
			System.out.println(personId+  "\t" + linkLength +  "\t" + link.getFreespeed() +  "\t" + enterTime + "\t" + leaveTime  + "\t" +  linkSlopes.getAttribute(link.getId().toString(), "slope") + "\t" + deltaX  + "\t" + deltaY);
			
		}
		
		
		
	}

}
