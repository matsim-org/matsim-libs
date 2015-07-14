package playground.sergioo.singapore2012;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class CapacitiesReduction {

	private static final double NUM_FACES = 2.0;
	private static final double MIN_FACTOR = 0.25;
	private static final double MAX_FACTOR = 0.9;

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Map<Id<Node>, Set<Id<Node>>> intersections = new HashMap<Id<Node>, Set<Id<Node>>>();
		Set<Id<Link>> noLinks = new HashSet<Id<Link>>();
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		String line = reader.readLine();
		line = reader.readLine();
		do {
			String[] parts = line.split(",");
			Id<Node> intId = Id.createNodeId(parts[4]);
			Set<Id<Node>> intersection = intersections.get(intId);
			if(intersection==null) {
				intersection = new HashSet<Id<Node>>();
				intersections.put(intId, intersection);
			}
			if(!hasCaps(parts[1]))
				intersection.add(Id.createNodeId(parts[1]));
			line = reader.readLine();
		} while(line!=null);
		reader.close();
		reader = new BufferedReader(new FileReader(args[1]));
		line = reader.readLine();
		line = reader.readLine();
		do {
			String[] parts = line.split(";");
			noLinks.add(Id.createLinkId(parts[1]));
			line = reader.readLine();
		} while(line!=null);
		reader.close();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		new MatsimNetworkReader(scenario).readFile(args[2]);
		new TransitScheduleReader(scenario).readFile(args[3]);
		Set<String> lineLinks = new HashSet<String>();
		Map<Id<Link>, Integer> numBusRoutes = new HashMap<Id<Link>, Integer>();
		for(TransitLine tLine:scenario.getTransitSchedule().getTransitLines().values())
			for(TransitRoute tRoute:tLine.getRoutes().values()) {
				Integer num = null;
				if(!lineLinks.contains(tLine.getId()+"$$$"+tRoute.getRoute().getStartLinkId())) {
					num = numBusRoutes.get(tRoute.getRoute().getStartLinkId());
					if(num==null)
						num= 0;
					num++;
					numBusRoutes.put(tRoute.getRoute().getStartLinkId(), num);
					lineLinks.add(tLine.getId()+"$$$"+tRoute.getRoute().getStartLinkId());
				}
				for(Id<Link> linkId:tRoute.getRoute().getLinkIds())
					if(!lineLinks.contains(tLine.getId()+"$$$"+linkId)) {
						num = numBusRoutes.get(linkId);
						if(num==null)
							num= 0;
						num++;
						numBusRoutes.put(linkId, num);
						lineLinks.add(tLine.getId()+"$$$"+linkId);
					}
				if(!lineLinks.contains(tLine.getId()+"$$$"+tRoute.getRoute().getEndLinkId())) {	
					num = numBusRoutes.get(tRoute.getRoute().getEndLinkId());
					if(num==null)
						num= 0;
					num++;
					numBusRoutes.put(tRoute.getRoute().getEndLinkId(), num);
					lineLinks.add(tLine.getId()+"$$$"+tRoute.getRoute().getEndLinkId());
				}
			}
		Set<Id<Link>> stopLinks = new HashSet<Id<Link>>();
		for(TransitStopFacility stop:scenario.getTransitSchedule().getFacilities().values())
			stopLinks.add(stop.getLinkId());
		List<Double> stopFactors = new ArrayList<Double>();
		List<Double> noStopFactors = new ArrayList<Double>();
		for(Set<Id<Node>> intersection:intersections.values()) {
			double total = 0;
			double num = 0;
			for(Id<Node> node:intersection)
				for(Link link:scenario.getNetwork().getNodes().get(node).getInLinks().values())
					if(!intersection.contains(link.getFromNode().getId()) && !noLinks.contains(link.getId()) && !(link.getId().toString().startsWith("fl")||link.getId().toString().startsWith("cl"))) {
						total+=numBusRoutes.get(link.getId())==null?1:(1+numBusRoutes.get(link.getId()));
						num++;
					}
			for(Id<Node> node:intersection)
				for(Link link:scenario.getNetwork().getNodes().get(node).getInLinks().values())
					if(!intersection.contains(link.getFromNode().getId()) && !noLinks.contains(link.getId())) {
						Id<Link> linkId = link.getId();
						if(link.getId().toString().startsWith("fl")||link.getId().toString().startsWith("cl"))
							linkId = Id.createLinkId(linkId.toString().substring(2, linkId.toString().length()));
						double factor = (num/NUM_FACES)*(numBusRoutes.get(linkId)==null?1:(1+numBusRoutes.get(linkId)))/total;
						factor = factor<MIN_FACTOR?MIN_FACTOR:factor>MAX_FACTOR?MAX_FACTOR:factor;
						link.setCapacity(link.getCapacity()*factor);
						link.setFreespeed(link.getFreespeed()*factor);
						if(stopLinks.contains(link.getId())) {
							stopFactors.add(factor);
						}
						else
							noStopFactors.add(factor);
					}
		}
		double[] sfa = new double[stopFactors.size()];
		for(int i=0; i<sfa.length; i++)
			sfa[i] = stopFactors.get(i);
		HistogramDataset dataset = new HistogramDataset();
		int numSeries = 100;
		dataset.addSeries("Stops", sfa, numSeries);
		JFreeChart chart = ChartFactory.createHistogram( "Factors of links with stops", "factor", "number", dataset, PlotOrientation.VERTICAL, true, false, false);
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new ChartPanel(chart));
		frame.pack();
		frame.setVisible(true);
		double[] nsfa = new double[noStopFactors.size()];
		for(int i=0; i<nsfa.length; i++)
			nsfa[i] = noStopFactors.get(i);
		dataset = new HistogramDataset();
		numSeries = 100;
		dataset.addSeries("NoStops", nsfa, numSeries);
		chart = ChartFactory.createHistogram( "Factors of links without stops", "time", "number", dataset, PlotOrientation.VERTICAL, true, false, false);
		JFrame frame2 = new JFrame();
		frame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame2.add(new ChartPanel(chart));
		frame2.pack();
		frame2.setVisible(true);
		(new NetworkWriter(scenario.getNetwork())).write(args[4]);
	}

	private static boolean hasCaps(String string) {
		for(byte a:string.getBytes())
			if(a>='A' && a<='Z')
				return true;
		return false;
	}

}
