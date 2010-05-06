package playground.mzilske.teach;

import java.util.Collection;
import java.util.HashSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.controller.Controller;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.charts.XYLineChart;
import org.postgis.LinearRing;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class Cordon {
	
	private Collection<Link> inLinks = new HashSet<Link>();
	private Collection<Link> outLinks = new HashSet<Link>();
	
	private static final int SLOT_SIZE = 3600;	// 5-min slots
	private static final int MAXINDEX = 24; // slots 0..11 are regular slots, slot 12 is anything above

	double[] in = new double[MAXINDEX + 1];
	double[] out = new double[MAXINDEX + 1];
	
	public static void main(String[] args) {
		Cordon cordon = new Cordon();
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
		
		
		final LinkLeaveEventHandler linkLeaveEventHandler = new LinkLeaveEventHandler() {

			@Override
			public void handleEvent(LinkLeaveEvent event) {
				
				Link link = network.getLinks().get(event.getLinkId());
				if (inLinks.contains(link)) {
					in[getTimeslotIndex(event.getTime())]++;
				} else if (outLinks.contains(link)) {
					out[getTimeslotIndex(event.getTime())]++;
				}
				
			}

			@Override
			public void reset(int iteration) {
				
			}
			
		};

		final Controller controller = new Controller(fileName);
		controller.setOverwriteFiles(true);
		controller.addEventHandler(linkLeaveEventHandler);
		controller.run();
		
		double[] hours = new double[MAXINDEX + 1];
		for (int i=0; i < hours.length; i++) {
			hours[i] = i;
		}
		
		System.out.println();
		for (double iin : in) {
			System.out.print(iin + " ");
		}
		
		for (double oout : out) {
			System.out.print(oout + " ");
		}
		
		XYLineChart chart = new XYLineChart("Traffic link 2", "iteration", "last time");
		chart.addSeries("in", hours, in);
		chart.addSeries("out", hours, out);
		chart.saveAsPng("./output/wurst.png", 800, 600);

	}


	private void getCordonLinks(Network network) {		
		for (Link link : network.getLinks().values()) {
			if (contained(link.getFromNode()) && !contained(link.getToNode())) {
				outLinks.add(link);
			} else if (!contained(link.getFromNode()) && contained(link.getToNode())) {
				inLinks.add(link);
			}
		}
	}

	private boolean contained(Node fromNode) {
		double xmin = 4467184;
		double xmax = 4471382;
		double ymin = 5332355;
		double ymax = 5334572;
		Coordinate c1 = new Coordinate(xmin, ymin);
		Coordinate c2 = new Coordinate(xmin, ymax);
		Coordinate c3 = new Coordinate(xmax, ymax);
		Coordinate c4 = new Coordinate(xmax, ymin);
		Coordinate c5 = c1;
		GeometryFactory geofac = new GeometryFactory();
		Coordinate[] coordinates = new Coordinate[] {c1, c2, c3, c4, c5};
		com.vividsolutions.jts.geom.LinearRing linearRing = geofac.createLinearRing(coordinates);
		com.vividsolutions.jts.geom.Polygon polygon = geofac.createPolygon(linearRing, null);
		
		Coord coord = fromNode.getCoord();
		
		// return coord.getX() > xmin && coord.getX() < xmax && coord.getY() > ymin && coord.getY() < ymax;
		return polygon.contains(geofac.createPoint(new Coordinate(coord.getX(), coord.getY())));
	}
	
	private int getTimeslotIndex(final double time_s) {
		int idx = (int)(time_s / SLOT_SIZE);
		if (idx > MAXINDEX) idx = MAXINDEX;
		return idx;
	}

	
}
