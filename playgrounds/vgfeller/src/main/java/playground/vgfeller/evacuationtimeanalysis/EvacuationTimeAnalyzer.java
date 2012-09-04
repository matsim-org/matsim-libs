package playground.vgfeller.evacuationtimeanalysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;


public class EvacuationTimeAnalyzer {
	
	
	private final String events;
	private final Scenario scenario;

	public EvacuationTimeAnalyzer(String events, Scenario sc) {
		this.events = events;
		this.scenario = sc;
	}
	
	private void run() {
		
		
		DepartureArrivalEventHandler handler = new DepartureArrivalEventHandler(this.scenario.getNetwork());
		
		EventsManager manager = EventsUtils.createEventsManager();
		
		manager.addHandler(handler);
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(manager);
		
		reader.parse(this.events);
		
		
		double time = handler.getAverageTravelTime();
		
		
			int hours = (int) (time / 3600);
			int min = (int) ((time % 3600) / 60);
			int sek = (int) ((time % 3600) % 60);
		
			System.out.println("Durschnittliche Reisezeit: ("+time+")");
			System.out.println("Stunden:" + hours);
			System.out.println("Minuten:" + min);
			System.out.println("Sekunden:" + sek);
		
		
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		
		for (Node n : this.scenario.getNetwork().getNodes().values()) {
			double x = n.getCoord().getX();
			double y = n.getCoord().getY();
			
			if (x < minX) {
				minX = x;
			}
			
			if (x > maxX) {
				maxX = x;
			}
			
			if (y < minY) {
				minY = y;
			}
			
			if (y > maxY) {
				maxY = y;
			}
		}
		
		for (double x = minX; x <= maxX; x += 250) {
			for (double y = minY; y <= minY; y += 250) {
				double t = handler.getAverageCellTravelTime(x, y);
				System.out.println("Reisezeit in Zelle: (" + x + "," + y +") betr�gt:" + t + " Sekunden." );
			}
			
		}
		
		
	}
	

	public static void main(String [] args) {
		
		String events = "Z:/WinHome/workspace2/tests/berlin/matsim_output/output/ITERS/it.0/0.events.xml.gz";
		
		String c = "Z:/WinHome/workspace2/tests/berlin/matsim_output/config.xml";
		
		Config config = ConfigUtils.loadConfig(c);
		Scenario sc = ScenarioUtils.loadScenario(config);
		
		
		EvacuationTimeAnalyzer analyzer = new EvacuationTimeAnalyzer(events,sc);
		analyzer.run();
		
	}

}
