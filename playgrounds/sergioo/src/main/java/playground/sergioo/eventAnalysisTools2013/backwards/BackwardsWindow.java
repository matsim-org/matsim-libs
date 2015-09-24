package playground.sergioo.eventAnalysisTools2013.backwards;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.sergioo.visualizer2D2012.Layer;
import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.LayersWindow;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainter;

public class BackwardsWindow extends LayersWindow implements PersonAlgorithm {

	private enum PanelIds implements LayersWindow.PanelIds {
		ONE;
	}
	private static PrintWriter printWriter;
	private static double scale = 2;
	{
		try {
			printWriter = new PrintWriter(new FileWriter("./data/coordsBack.txt", true));
		} catch(Exception e) {
			System.out.println();
		}
	}
	private String[] args;
	private StationsPainter stationsPainterA;
	private StationsPainter stationsPainterB;
	private Map<Id<TransitStopFacility>, Coord> coords = new HashMap<Id<TransitStopFacility>, Coord>();

	public BackwardsWindow(TransitRoute[] routes,  String[] args, StationsPainter[] stationsPainters, LayersPanel panel) {
		this.args = args;
		int i=0;
		for(TransitRoute route:routes) {
			for(TransitRouteStop stop:route.getStops()) {
				coords.put(stop.getStopFacility().getId(), stop.getStopFacility().getCoord());
				stationsPainters[i].addPoint(stop.getStopFacility().getCoord());
			}
			i++;
		}
		this.stationsPainterA = stationsPainters[0];
		this.stationsPainterB = stationsPainters[1];
		layersPanels.put(PanelIds.ONE, panel);
		this.setLayout(new BorderLayout());
		this.add(panel, BorderLayout.CENTER);
		super.setSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
	}

	private static boolean sameDirection(String line, String complex, String simple) {
		if(line.equals("EW")) {
			String part = simple.substring(0, simple.length()-1);
			return complex.contains(part) && complex.charAt(complex.length()-1)==simple.charAt(simple.length()-1);
		}
		else if(line.equals("NS")) {
			String part = simple.substring(0, simple.length()-1);
			return complex.contains(part) && complex.charAt(complex.length()-1)==simple.charAt(simple.length()-1);
		}
		else if(line.equals("NE"))
			return complex.contains(simple);
		else if(line.equals("CC"))
			return complex.contains(simple);
		return false;
	}

	@Override
	public void run(Person person) {
		String backwards = "";
		String possibleBackwards = "";
		String startStop = null;
		for(PlanElement planElement:person.getSelectedPlan().getPlanElements()) {
			if(planElement instanceof Leg && ((Leg)planElement).getMode().equals("pt")) {
				String[] parts = (((Leg)planElement).getRoute()).getRouteDescription().split("===");
				if(parts[2].equals(args[3]) && sameDirection(args[3], parts[3], args[4])) {
					if(startStop==null) {
						startStop = parts[1];
						possibleBackwards = "A";
					}
					else if(possibleBackwards.equals("B")) {
						backwards = "A";
					}
				}
				else if(parts[2].equals(args[3]) && sameDirection(args[3], parts[3], args[5])) {
					if(startStop==null) {
						startStop = parts[1];
						possibleBackwards = "B";
					}
					else if(possibleBackwards.equals("A")) {
						backwards = "B";
					}
				}
				else {
					startStop = null;
					possibleBackwards = "";
				}
			}
			else if(planElement instanceof Activity && !((Activity)planElement).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
				startStop = null;
				possibleBackwards = "";
			}
			if(!backwards.isEmpty()) {
				if(backwards.equals("A")) {
					stationsPainterA.increaseSize(coords.get(Id.create(startStop, TransitStopFacility.class)));
					printWriter.println("G "+coords.get(Id.create(startStop, TransitStopFacility.class)).getX()+" "+coords.get(Id.create(startStop, TransitStopFacility.class)).getY());
				}
				else {
					stationsPainterB.increaseSize(coords.get(Id.create(startStop, TransitStopFacility.class)));
					printWriter.println("H "+coords.get(Id.create(startStop, TransitStopFacility.class)).getX()+" "+coords.get(Id.create(startStop, TransitStopFacility.class)).getY());
				}
				backwards = "";
				startStop = null;
				possibleBackwards = "";
			}
		}
	}

	@Override
	public void refreshLabel(Labels label) {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args) throws IOException {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(args[0]);
		scenario.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile(args[1]);
		TransitRoute routeA = scenario.getTransitSchedule().getTransitLines().get(Id.create("EW", TransitLine.class)).getRoutes().get(Id.create("EW_weeksatday_1", TransitRoute.class));
		TransitRoute routeB = scenario.getTransitSchedule().getTransitLines().get(Id.create("EW", TransitLine.class)).getRoutes().get(Id.create("EW_weeksatday_2", TransitRoute.class));
		TransitRoute routeC = scenario.getTransitSchedule().getTransitLines().get(Id.create("NS", TransitLine.class)).getRoutes().get(Id.create("NS_weeksatday_1", TransitRoute.class));
		TransitRoute routeD = scenario.getTransitSchedule().getTransitLines().get(Id.create("NS", TransitLine.class)).getRoutes().get(Id.create("NS_weeksatday_2", TransitRoute.class));
		TransitRoute routeE = scenario.getTransitSchedule().getTransitLines().get(Id.create("NE", TransitLine.class)).getRoutes().get(Id.create("NE_weeksatday_1", TransitRoute.class));
		TransitRoute routeF = scenario.getTransitSchedule().getTransitLines().get(Id.create("NE", TransitLine.class)).getRoutes().get(Id.create("NE_weeksatday_2", TransitRoute.class));
		TransitRoute routeG = scenario.getTransitSchedule().getTransitLines().get(Id.create("CC", TransitLine.class)).getRoutes().get(Id.create("CC_daily_new_1", TransitRoute.class));
		TransitRoute routeH = scenario.getTransitSchedule().getTransitLines().get(Id.create("CC", TransitLine.class)).getRoutes().get(Id.create("CC_daily_new_2", TransitRoute.class));
		final StationsPainter stationsPainterA = new StationsPainter(new Color(80,255,80,100), scale);
		final StationsPainter stationsPainterB = new StationsPainter(new Color(0,135,0,100), scale);
		final StationsPainter stationsPainterC = new StationsPainter(new Color(255,80,80,100), scale);
		final StationsPainter stationsPainterD = new StationsPainter(new Color(135,0,0,100), scale);
		final StationsPainter stationsPainterE = new StationsPainter(new Color(255,80,255,100), scale);
		final StationsPainter stationsPainterF = new StationsPainter(new Color(135,0,135,100), scale );
		final StationsPainter stationsPainterG = new StationsPainter(new Color(255,255,80,100), scale);
		final StationsPainter stationsPainterH = new StationsPainter(new Color(135,135,0,100), scale);
		LayersPanel panel = new LayersPanel() {
			{
				addLayer(new Layer(new NetworkPainter(scenario.getNetwork())));
				addLayer(new Layer(stationsPainterA));
				addLayer(new Layer(stationsPainterB));
				addLayer(new Layer(stationsPainterC));
				addLayer(new Layer(stationsPainterD));
				addLayer(new Layer(stationsPainterE));
				addLayer(new Layer(stationsPainterF));
				addLayer(new Layer(stationsPainterG));
				addLayer(new Layer(stationsPainterH));
				addLayer(new Layer(new CircleLegendPainter(Color.LIGHT_GRAY, scale, new double[]{1000,500,200,100,50}, 2000, new Coord(376844, 139837))));
				Collection<double[]> bounds = new ArrayList<double[]>();
				bounds.add(new double[]{346153, 162947});
				bounds.add(new double[]{391844, 136837});
				calculateBoundaries(bounds);
			}
		};
		BackwardsWindow window = new BackwardsWindow(new TransitRoute[]{routeA, routeB, routeC, routeD, routeE, routeF, routeG, routeH}, args, new StationsPainter[]{stationsPainterA, stationsPainterB, stationsPainterC, stationsPainterD, stationsPainterE, stationsPainterF, stationsPainterG, stationsPainterH}, panel);
		((PopulationImpl)scenario.getPopulation()).setIsStreaming(true);
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(window);
		new MatsimPopulationReader(scenario).readFile(args[2]);
		printWriter.close();
		/*BufferedReader reader = new BufferedReader(new FileReader("./data/coordsBack.txt"));
		String line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(" ");
			if(parts[0].equals("A"))
				stationsPainterA.increaseSize(new CoordImpl(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
			else if(parts[0].equals("B"))
				stationsPainterB.increaseSize(new CoordImpl(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
			else if(parts[0].equals("C"))
				stationsPainterC.increaseSize(new CoordImpl(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
			else if(parts[0].equals("D"))
				stationsPainterD.increaseSize(new CoordImpl(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
			else if(parts[0].equals("E"))
				stationsPainterE.increaseSize(new CoordImpl(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
			else if(parts[0].equals("F"))
				stationsPainterF.increaseSize(new CoordImpl(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
			else if(parts[0].equals("G"))
				stationsPainterE.increaseSize(new CoordImpl(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
			else if(parts[0].equals("H"))
				stationsPainterF.increaseSize(new CoordImpl(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
			line = reader.readLine();
		}
		reader.close();*/
		window.setVisible(true);
	}

}
