package playground.sergioo.eventAnalysisTools2013.excessWaitingTime;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.sergioo.eventAnalysisTools2013.excessWaitingTime.ExcessWaitingTimeCalculator.Mode;
import playground.sergioo.visualizer2D2012.Layer;
import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.LayersWindow;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainter;

public class EWTWindow extends LayersWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private enum PanelIds implements LayersWindow.PanelIds {
		ONE;
	}
	private static double scale = 12;
	
	public EWTWindow(LayersPanel panel, Id<TransitLine> line, TransitRoute route, StationsPainter stationsPainter, ExcessWaitingTimeCalculator eWTCalculator) {
		for(TransitRouteStop stop:route.getStops()) {
			Double[] values = new Double[3];
			values[0] = eWTCalculator.getExcessWaitTime(line, route, stop.getStopFacility().getId(), Mode.TIME_WEIGHT);
			values[1] = eWTCalculator.getExcessWaitTime(line, route, stop.getStopFacility().getId(), Mode.NUM_PEOPLE_WEIGHT);
			values[2] = eWTCalculator.getExcessWaitTime(line, route, stop.getStopFacility().getId(), Mode.FULL_SAMPLE);		
			stationsPainter.addPoint(stop.getStopFacility().getCoord(), values);
		}
		layersPanels.put(PanelIds.ONE, panel);
		this.setLayout(new BorderLayout());
		this.add(panel, BorderLayout.CENTER);
		super.setSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	@Override
	public void refreshLabel(Labels label) {
		
	}
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(args[0]);
		scenario.getConfig().scenario().setUseTransit(true);
		final StationsPainter stationsPainter = new StationsPainter(new Color[]{new Color(255,255,0,100),new Color(0,0,255,100),new Color(255,0,0,100)}, scale);
		new TransitScheduleReader(scenario).readFile(args[1]);
		EventsManager events = EventsUtils.createEventsManager();
		//ObjectInputStream ois = new ObjectInputStream(new FileInputStream(args[5]));
		final ExcessWaitingTimeCalculator eWTCalculator = /*(ExcessWaitingTimeCalculator) ois.readObject();*/new ExcessWaitingTimeCalculator();
		events.addHandler(eWTCalculator);
		new EventsReaderXMLv1(events).parse(args[2]);
		/*ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(args[5]));
		oos.writeObject(eWTCalculator);
		oos.close();
		ois.close();*/
		final TransitRoute route = scenario.getTransitSchedule().getTransitLines().get(Id.create(args[3], TransitLine.class)).getRoutes().get(Id.create(args[4], TransitRoute.class));
		final PrintWriter writer = new PrintWriter(args[6]+args[4]+".csv");
		writer.println("stopId,EWT_headway,EWT_numPeople,EWT_byPerson");
		final Id<TransitLine> lineId = Id.create(args[3], TransitLine.class);
		LayersPanel panel = new LayersPanel() {
			{
				addLayer(new Layer(new NetworkPainter(scenario.getNetwork())));
				addLayer(new Layer(stationsPainter));
				addLayer(new Layer(new CircleLegendPainter(Color.LIGHT_GRAY, scale, new double[]{500,200,100,50,20}, 2000, new CoordImpl(376844, 139837))));
				double xMin = Double.MAX_VALUE, yMin = Double.MAX_VALUE, xMax = -Double.MAX_VALUE, yMax = -Double.MAX_VALUE;
				for(TransitRouteStop stop:route.getStops()) {
					writer.println(stop.getStopFacility().getId()+
							","+eWTCalculator.getExcessWaitTime(lineId, route, stop.getStopFacility().getId(), ExcessWaitingTimeCalculator.Mode.TIME_WEIGHT)+
							","+eWTCalculator.getExcessWaitTime(lineId, route, stop.getStopFacility().getId(), ExcessWaitingTimeCalculator.Mode.NUM_PEOPLE_WEIGHT)+
							","+eWTCalculator.getExcessWaitTime(lineId, route, stop.getStopFacility().getId(), ExcessWaitingTimeCalculator.Mode.FULL_SAMPLE));
					Coord coord = stop.getStopFacility().getCoord();
					double x=coord.getX(), y=coord.getY();
					if(x<xMin)
						xMin = x;
					if(y<yMin)
						yMin = y;
					if(x>xMax)
						xMax = x;
					if(y>yMax)
						yMax = y;
				}
				Collection<double[]> bounds = new ArrayList<double[]>();
				double border = 100;
				bounds.add(new double[]{xMin-border, yMax+border});
				bounds.add(new double[]{xMax+border, yMin-border});
				calculateBoundaries(bounds);
			}
		};
		writer.close();
		EWTWindow window = new EWTWindow(panel, Id.create(args[3], TransitLine.class), route, stationsPainter, eWTCalculator);
		window.setVisible(true);
	}

}
