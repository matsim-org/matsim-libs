package playground.mzilske.teach;

import java.util.Collection;
import java.util.HashSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.controller.Controller;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class CordonTemplate {
	
	private final class MyLinkEventHandler implements LinkLeaveEventHandler {
		private final Network network;

		private MyLinkEventHandler(Network network) {
			this.network = network;
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			// Den EventHandler müssen Sie natürlich selbst programmieren.
		}

		@Override
		public void reset(int iteration) {
			
		}
	}

	private Collection<Link> inLinks = new HashSet<Link>();
	private Collection<Link> outLinks = new HashSet<Link>();
	
	
	public static void main(String[] args) {
		CordonTemplate cordon = new CordonTemplate();
		cordon.run();
	}

	private void run() {
		String fileName = "examples/munich-small/config.xml";
		ScenarioLoader scenarioLoader = new ScenarioLoaderImpl(fileName);
		scenarioLoader.loadScenario();
		Scenario scenario = scenarioLoader.getScenario();
		final Network network = scenario.getNetwork();
		getCordonLinks(network);

		System.out.println(inLinks.size());
		System.out.println(outLinks.size());
		
		
		final LinkLeaveEventHandler linkLeaveEventHandler = new MyLinkEventHandler(network);

		final Controller controller = new Controller(fileName);
		controller.setOverwriteFiles(true);
		controller.addEventHandler(linkLeaveEventHandler);
		controller.run();
		
		printResults();

	}

	private void printResults() {
		// Geben Sie Ihre Ergebnisse aus.
	}


	private void getCordonLinks(Network network) {		
		// Hier sollten die Kanten bestimmt werden, die in den Cordon hineinführen (inLinks) und die, die aus ihm herausführen (outLinks).
	}

	private boolean contained(Node node) {
		// Vielleicht können Sie eine Methode gebrauchen (und schreiben), die prüft, ob ein Knoten im Cordon liegt.
		return false;
	}

	/**
	 * 
	 * So erzeugt man mit GeoTools ein Viereck.
	 * Vielleicht können Sie das ja gebrauchen (und verbessern).
	 * 
	 * @param xmin
	 * @param xmax
	 * @param ymin
	 * @param ymax
	 * @return
	 */
	private Polygon erzeugeViereck(double xmin,
			double xmax, double ymin, double ymax) {
		Coordinate c1 = new Coordinate(xmin, ymin);
		Coordinate c2 = new Coordinate(xmin, ymax);
		Coordinate c3 = new Coordinate(xmax, ymax);
		Coordinate c4 = new Coordinate(xmax, ymin);
		Coordinate c5 = c1;
		GeometryFactory geofac = new GeometryFactory();
		Coordinate[] coordinates = new Coordinate[] {c1, c2, c3, c4, c5};
		LinearRing linearRing = geofac.createLinearRing(coordinates);
		Polygon polygon = geofac.createPolygon(linearRing, null);
		return polygon;
	}
	


	
}
