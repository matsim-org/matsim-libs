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

import playground.wrashid.fd.AbstractDualSimHandler;
public class MainCalcOrientation {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String networkFile = "H:/data/experiments/TRBAug2011/runs/ktiRun24/output/output_network.xml.gz";
		String eventsFile =  "H:/data/experiments/TRBAug2011/runs/ktiRun24/output/ITERS/it.50/50.events.xml.gz";
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		
		EventsManager events = EventsUtils.createEventsManager();
		InfoCollector ic=new InfoCollector(scenario.getNetwork());

		events.addHandler(ic); 

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		
		System.out.println("enterTime" + "\t" + "leaveTime" + "\t" + "deltaX"  + "\t" + "deltaY");
		reader.parse(eventsFile);
	}
	
	private static class InfoCollector extends AbstractDualSimHandler{

		private Network network;

		public InfoCollector(Network network){
			this.network = network;
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
			double deltaX=link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX();
			double deltaY=link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY();
			
			System.out.println(enterTime + "\t" + leaveTime + "\t" + deltaX  + "\t" + deltaY);
			
		}
		
		
		
	}

}
