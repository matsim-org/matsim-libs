package playground.sergioo.eventAnalysisTools2012;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.core.config.ConfigUtils;
import org.xml.sax.SAXException;

public class TimeSpaceDistribution implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler {
	
	public static double TIME_INTERVAL;
	public final static long millisSingapore = 27000000;
	private static final String SEPARATOR_CSV = ",";
	private static final String SEPARATOR_TXT = "\t";
	
	private int numIntervals;
	private Map<Id<Link>, LinkData> linksData;
	private Network network;
	
	/**
	 * 
	 * @param args
	 * 				0-Network file location
	 * 				1-Events file location
	 * 				2-CSV file location
	 * 				3-Time interval in minutes
	 * 				4-Graphs option: -link (link graph), -avg (links average graph), -avgint (links average with time interval), other (print the most congested link)
	 * 				5-With -link is the link id, With -avgint is the initial bin
	 * 				6-With -avgint is the final bin
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException, ParseException {
		TimeSpaceDistribution.TIME_INTERVAL = 60*Double.parseDouble(args[3]);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(scenario.getNetwork()).readFile(args[0]);
		EventsManager events = EventsUtils.createEventsManager();
		TimeSpaceDistribution tSD = new TimeSpaceDistribution(scenario.getNetwork());
		events.addHandler(tSD);
		new EventsReaderXMLv1(events).readFile(args[1]);
		tSD.printCSVFiles(args[2]);
		//tSD.printTXTFiles(args[2]);
		if(args.length>4) {
			if(args[4].equals("-link")) {
				tSD.showFlowLinkGraph(args[5]);
				tSD.showDensityLinkGraph(args[5]);
				tSD.showTTLinkGraph(args[5]);
				tSD.showSpeedLinkGraph(args[5]);
				tSD.showKLinkGraph(args[5]);
				tSD.showFlowSpeedGraph(args[5]);
				tSD.showDensityFlowGraph(args[5]);
				tSD.showDensitySpeedGraph(args[5]);
			}
			else if(args[4].equals("-avg")) {
				tSD.showFlowAvgGraph();
				tSD.showDensityAvgGraph();
				tSD.showSpeedAvgGraph();
				tSD.showTTAvgGraph();
				tSD.showKAvgGraph();
			}
			else if(args[4].equals("-avgint")) {
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
				tSD.showFlowAvgGraph((int)((sdf.parse(args[5]).getTime()+millisSingapore)/(1000.0*TIME_INTERVAL)),(int)((sdf.parse(args[6]).getTime()+millisSingapore)/(1000.0*TIME_INTERVAL)));
				tSD.showDensityAvgGraph((int)((sdf.parse(args[5]).getTime()+millisSingapore)/(1000.0*TIME_INTERVAL)),(int)((sdf.parse(args[6]).getTime()+millisSingapore)/(1000.0*TIME_INTERVAL)));
				tSD.showSpeedAvgGraph((int)((sdf.parse(args[5]).getTime()+millisSingapore)/(1000.0*TIME_INTERVAL)),(int)((sdf.parse(args[6]).getTime()+millisSingapore)/(1000.0*TIME_INTERVAL)));
				tSD.showTTAvgGraph((int)((sdf.parse(args[5]).getTime()+millisSingapore)/(1000.0*TIME_INTERVAL)),(int)((sdf.parse(args[6]).getTime()+millisSingapore)/(1000.0*TIME_INTERVAL)));
				tSD.showKAvgGraph((int)((sdf.parse(args[5]).getTime()+millisSingapore)/(1000.0*TIME_INTERVAL)),(int)((sdf.parse(args[6]).getTime()+millisSingapore)/(1000.0*TIME_INTERVAL)));
			}
			else {
				Id<Link> moreCongested = null;
				for(Entry<Id<Link>, LinkData> linkData:tSD.getLinksData().entrySet())
					if(moreCongested==null || (linkData.getValue().getConcentration()>tSD.getLinksData().get(moreCongested).getConcentration()))
						moreCongested = linkData.getKey();
				System.out.println(moreCongested+" "+tSD.getLinksData().get(moreCongested).getConcentration());
				moreCongested = null;
				for(Entry<Id<Link>, LinkData> linkData:tSD.getLinksData().entrySet()) {
					if(moreCongested==null || linkData.getValue().getDensity()>tSD.getLinksData().get(moreCongested).getDensity())
						moreCongested = linkData.getKey();
				}
				System.out.println(moreCongested+" "+tSD.getLinksData().get(moreCongested).getDensity());
			}
		}
	}

	public TimeSpaceDistribution(Network network) {
		super();
		numIntervals = 0;
		this.linksData = new HashMap<Id<Link>, LinkData>();
		this.network = network;
		for(Link link:network.getLinks().values())
			if(link.getLength()>0)
				linksData.put(link.getId(), new LinkData(link));
	}
	public void printCSVFiles(String folder) throws FileNotFoundException {
		PrintWriter printWriter = new PrintWriter(folder+"/flows.csv");
		printWriter.print("link"+SEPARATOR_CSV+"average\\time"+SEPARATOR_CSV);
		for(int t=0;t<numIntervals;t++) {
			double time=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			printWriter.print(time+SEPARATOR_CSV);
		}
		printWriter.println();
		for(Entry<Id<Link>, LinkData> linkDataE:linksData.entrySet()) {
			printWriter.print(linkDataE.getKey().toString()+SEPARATOR_CSV+linkDataE.getValue().getFlow()*3600+SEPARATOR_CSV);
			for(int t=0;t<linkDataE.getValue().getTimeSize();t++) {
				double value=linkDataE.getValue().getFlow(t)*3600;
				printWriter.print(value+SEPARATOR_CSV);
			}
			printWriter.println();
		}
		printWriter.close();
		printWriter = new PrintWriter(new File(folder+"/densities.csv"));
		printWriter.print("link"+SEPARATOR_CSV+"average\\time"+SEPARATOR_CSV);
		for(int t=0;t<numIntervals;t++) {
			double time=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			printWriter.print(time+SEPARATOR_CSV);
		}
		printWriter.println();
		for(Entry<Id<Link>,LinkData> linkDataE:linksData.entrySet()) {
			printWriter.print(linkDataE.getKey().toString()+SEPARATOR_CSV+linkDataE.getValue().getDensity()*1000+SEPARATOR_CSV);
			for(int t=0;t<linkDataE.getValue().getTimeSize();t++) {
				double value=linkDataE.getValue().getDensity(t)*1000;
				printWriter.print(value+SEPARATOR_CSV);
			}
			printWriter.println();
		}
		printWriter.close();
		printWriter = new PrintWriter(new File(folder+"/travelTimes.csv"));
		printWriter.print("link\\time"+SEPARATOR_CSV);
		for(int t=0;t<numIntervals;t++) {
			double time=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			printWriter.print(time+SEPARATOR_CSV);
		}
		printWriter.println();
		for(Entry<Id<Link>, LinkData> linkDataE:linksData.entrySet()) {
			printWriter.print(linkDataE.getKey().toString()+SEPARATOR_CSV);
			for(int t=0;t<linkDataE.getValue().getTimeSize();t++) {
				double value=linkDataE.getValue().getAvgTravelTimes(t);
				printWriter.print((Double.isNaN(value)?"":value)+SEPARATOR_CSV);
			}
			printWriter.println();
		}
		printWriter.close();
		printWriter = new PrintWriter(new File(folder+"/avgSpeeds.csv"));
		printWriter.print("link\\time"+SEPARATOR_CSV);
		for(int t=0;t<numIntervals;t++) {
			double time=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			printWriter.print(time+SEPARATOR_CSV);
		}
		printWriter.println();
		for(Entry<Id<Link>, LinkData> linkDataE:linksData.entrySet()) {
			printWriter.print(linkDataE.getKey().toString()+SEPARATOR_CSV);
			for(int t=0;t<linkDataE.getValue().getTimeSize();t++) {
				double value=linkDataE.getValue().getAvgSpeeds(t)*3600/1000;
				printWriter.print((Double.isNaN(value)?"":value)+SEPARATOR_CSV);
			}
			printWriter.println();
		}
		printWriter.close();
		printWriter = new PrintWriter(new File(folder+"/concentrations.csv"));
		printWriter.print("link"+SEPARATOR_CSV+"average\\time"+SEPARATOR_CSV);
		for(int t=0;t<numIntervals;t++) {
			double time=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			printWriter.print(time+SEPARATOR_CSV);
		}
		printWriter.println();
		for(Entry<Id<Link>, LinkData> linkDataE:linksData.entrySet()) {
			printWriter.print(linkDataE.getKey().toString()+SEPARATOR_CSV+linkDataE.getValue().getConcentration()+SEPARATOR_CSV);
			for(int t=0;t<linkDataE.getValue().getTimeSize();t++) {
				double value=linkDataE.getValue().getConcentration(t)*1000;
				printWriter.print(value+SEPARATOR_CSV);
			}
			printWriter.println();
		}
		printWriter.close();
	}
	public void printTXTFiles(String folder) throws FileNotFoundException {
		PrintWriter printWriter = new PrintWriter(folder+"/flows.txt");
		printWriter.print("link"+SEPARATOR_TXT+"average\\time"+SEPARATOR_TXT);
		for(int t=0;t<numIntervals;t++) {
			double time=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			printWriter.print(time+SEPARATOR_TXT);
		}
		printWriter.println();
		for(Entry<Id<Link>, LinkData> linkDataE:linksData.entrySet()) {
			printWriter.print(linkDataE.getKey().toString()+SEPARATOR_TXT+linkDataE.getValue().getFlow()*3600+SEPARATOR_TXT);
			for(int t=0;t<linkDataE.getValue().getTimeSize();t++) {
				double value=linkDataE.getValue().getFlow(t)*3600;
				printWriter.print(value+SEPARATOR_TXT);
			}
			printWriter.println();
		}
		printWriter.close();
		printWriter = new PrintWriter(new File(folder+"/densities.txt"));
		printWriter.print("link"+SEPARATOR_TXT+"average\\time"+SEPARATOR_TXT);
		for(int t=0;t<numIntervals;t++) {
			double time=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			printWriter.print(time+SEPARATOR_TXT);
		}
		printWriter.println();
		for(Entry<Id<Link>, LinkData> linkDataE:linksData.entrySet()) {
			printWriter.print(linkDataE.getKey().toString()+SEPARATOR_TXT+linkDataE.getValue().getDensity()*1000+SEPARATOR_TXT);
			for(int t=0;t<linkDataE.getValue().getTimeSize();t++) {
				double value=linkDataE.getValue().getDensity(t)*1000;
				printWriter.print(value+SEPARATOR_TXT);
			}
			printWriter.println();
		}
		printWriter.close();
		printWriter = new PrintWriter(new File(folder+"/travelTimes.txt"));
		printWriter.print("link\\time"+SEPARATOR_TXT);
		for(int t=0;t<numIntervals;t++) {
			double time=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			printWriter.print(time+SEPARATOR_TXT);
		}
		printWriter.println();
		for(Entry<Id<Link>, LinkData> linkDataE:linksData.entrySet()) {
			printWriter.print(linkDataE.getKey().toString()+SEPARATOR_TXT);
			for(int t=0;t<linkDataE.getValue().getTimeSize();t++) {
				double value=linkDataE.getValue().getAvgTravelTimes(t);
				printWriter.print((Double.isNaN(value)?"":value)+SEPARATOR_TXT);
			}
			printWriter.println();
		}
		printWriter.close();
		printWriter = new PrintWriter(new File(folder+"/avgSpeeds.txt"));
		printWriter.print("link\\time"+SEPARATOR_TXT);
		for(int t=0;t<numIntervals;t++) {
			double time=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			printWriter.print(time+SEPARATOR_TXT);
		}
		printWriter.println();
		for(Entry<Id<Link>, LinkData> linkDataE:linksData.entrySet()) {
			printWriter.print(linkDataE.getKey().toString()+SEPARATOR_TXT);
			for(int t=0;t<linkDataE.getValue().getTimeSize();t++) {
				double value=linkDataE.getValue().getAvgSpeeds(t)*3600/1000;
				printWriter.print((Double.isNaN(value)?"":value)+SEPARATOR_TXT);
			}
			printWriter.println();
		}
		printWriter.close();
		printWriter = new PrintWriter(new File(folder+"/concentrations.txt"));
		printWriter.print("link"+SEPARATOR_TXT+"average\\time"+SEPARATOR_TXT);
		for(int t=0;t<numIntervals;t++) {
			double time=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			printWriter.print(time+SEPARATOR_TXT);
		}
		printWriter.println();
		for(Entry<Id<Link>, LinkData> linkDataE:linksData.entrySet()) {
			printWriter.print(linkDataE.getKey().toString()+SEPARATOR_TXT+linkDataE.getValue().getConcentration()+SEPARATOR_TXT);
			for(int t=0;t<linkDataE.getValue().getTimeSize();t++) {
				double value=linkDataE.getValue().getConcentration(t)*1000;
				printWriter.print(value+SEPARATOR_TXT);
			}
			printWriter.println();
		}
		printWriter.close();
	}
	public Map<Id<Link>, LinkData> getLinksData() {
		return linksData;
	}
	public void showFlowAvgGraph(int fromBin, int toBin) {
		XYLineChart chart = new XYLineChart("Flow Average", "Time(h)", "Flow(veh/h)");
		double[] xs = new double[toBin-fromBin];
		double[] ys = new double[toBin-fromBin];
		for(int t=fromBin;t<toBin;t++) {
			xs[t-fromBin]=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			double value = 0;
			int numLinks = 0;
			for(Link link:network.getLinks().values()) {
				LinkData linkData = linksData.get(link.getId());
				if(linkData!=null && linkData.isUsed()) {
					value+=linkData.getFlow(t);
					numLinks++;
				}
			}
			ys[t-fromBin]=value*3600/numLinks;
		}
		chart.addSeries("Flow", xs, ys);
		chart.saveAsPng("./data/youssef/flowLinksAverage"+"-"+fromBin+"-"+toBin+"-"+".png", 800, 600);
	}
	public void showDensityAvgGraph(int fromBin, int toBin) {
		XYLineChart chart = new XYLineChart("Density Average", "Time(h)", "Density(veh/m)");
		double[] xs = new double[toBin-fromBin];
		double[] ys = new double[toBin-fromBin];
		for(int t=fromBin;t<toBin;t++) {
			xs[t-fromBin]=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			double value = 0;
			int numLinks = 0;
			for(Link link:network.getLinks().values()) {
				LinkData linkData = linksData.get(link.getId());
				if(linkData!=null && linkData.isUsed()) {
					value+=linkData.getDensity(t);
					numLinks++;
				}
			}
			ys[t-fromBin]=value/numLinks;
		}
		chart.addSeries("Flow", xs, ys);
		chart.saveAsPng("./data/youssef/densityLinksAverage"+"-"+fromBin+"-"+toBin+"-"+".png", 800, 600);
	}
	public void showSpeedAvgGraph(int fromBin, int toBin) {
		XYLineChart chart = new XYLineChart("Speed Average", "Time(h)", "Speed(m/s)");
		double[] xs = new double[toBin-fromBin];
		double[] ys = new double[toBin-fromBin];
		for(int t=fromBin;t<toBin;t++) {
			xs[t-fromBin]=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			double value = 0;
			int numLinks = 0;
			for(Link link:network.getLinks().values()) {
				LinkData linkData = linksData.get(link.getId());
				if(linkData!=null && linkData.isUsed()) {
					value+=linkData.getAvgSpeedsGraphs(t);
					numLinks++;
				}
			}
			ys[t-fromBin]=value/numLinks;
		}
		chart.addSeries("Speed", xs, ys);
		chart.saveAsPng("./data/youssef/speedLinksAverage"+"-"+fromBin+"-"+toBin+"-"+".png", 800, 600);
	}
	public void showTTAvgGraph(int fromBin, int toBin) {
		XYLineChart chart = new XYLineChart("TT Avg", "Time(h)", "Travel Time(s)");
		double[] xs = new double[toBin-fromBin];
		double[] ys = new double[toBin-fromBin];
		for(int t=fromBin;t<toBin;t++) {
			xs[t-fromBin]=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			double value = 0;
			int numLinks = 0;
			for(Link link:network.getLinks().values()) {
				LinkData linkData = linksData.get(link.getId());
				if(linkData!=null && linkData.isUsed()) {
					value+=linkData.getAvgTravelTimesGraphs(t);
					numLinks++;
				}
			}
			ys[t-fromBin]=value/numLinks;
		}
		chart.addSeries("Travel Time", xs, ys);
		chart.saveAsPng("./data/youssef/tTLinksAverage"+"-"+fromBin+"-"+toBin+"-"+".png", 800, 600);
	}
	public void showKAvgGraph(int fromBin, int toBin) {
		XYLineChart chart = new XYLineChart("Concentration", "Time(h)", "K(veh/m)");
		double[] xs = new double[toBin-fromBin];
		double[] ys = new double[toBin-fromBin];
		for(int t=fromBin;t<toBin;t++) {
			xs[t-fromBin]=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			double value = 0;
			int numLinks = 0;
			for(Link link:network.getLinks().values()) {
				LinkData linkData = linksData.get(link.getId());
				if(linkData!=null && linkData.isUsed()) {
					value+=linkData.getConcentration(t);
					numLinks++;
				}
			}
			ys[t-fromBin]=value/numLinks;
		}
		chart.addSeries("Concentration", xs, ys);
		chart.saveAsPng("./data/youssef/kAverage"+"-"+fromBin+"-"+toBin+"-"+".png", 800, 600);
	}
	public void showFlowAvgGraph() {
		XYLineChart chart = new XYLineChart("Flow Average", "Time(h)", "Flow(veh/h)");
		double[] xs = new double[numIntervals];
		double[] ys = new double[numIntervals];
		for(int t=0;t<numIntervals;t++) {
			xs[t]=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			double value = 0;
			int numLinks = 0;
			for(Link link:network.getLinks().values()) {
				LinkData linkData = linksData.get(link.getId());
				if(linkData!=null && linkData.isUsed()) {
					value+=linkData.getFlow(t);
					numLinks++;
				}
			}
			ys[t]=value*3600/numLinks;
		}
		chart.addSeries("Flow", xs, ys);
		chart.saveAsPng("./data/youssef/flowLinksAverage.png", 800, 600);
	}
	public void showDensityAvgGraph() {
		XYLineChart chart = new XYLineChart("Density Average", "Time(h)", "Density(veh/m)");
		double[] xs = new double[numIntervals];
		double[] ys = new double[numIntervals];
		for(int t=0;t<numIntervals;t++) {
			xs[t]=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			double value = 0;
			int numLinks = 0;
			for(Link link:network.getLinks().values()) {
				LinkData linkData = linksData.get(link.getId());
				if(linkData!=null && linkData.isUsed()) {
					value+=linkData.getDensity(t);
					numLinks++;
				}
			}
			ys[t]=value/numLinks;
		}
		chart.addSeries("Density", xs, ys);
		chart.saveAsPng("./data/youssef/densityLinksAverage.png", 800, 600);
	}
	public void showSpeedAvgGraph() {
		XYLineChart chart = new XYLineChart("Speed Average", "Time(h)", "Speed(m/s)");
		double[] xs = new double[numIntervals];
		double[] ys = new double[numIntervals];
		for(int t=0;t<numIntervals;t++) {
			xs[t]=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			double value = 0;
			int numLinks = 0;
			for(Link link:network.getLinks().values()) {
				LinkData linkData = linksData.get(link.getId());
				if(linkData.isUsed()) {
					value+=linkData.getAvgSpeedsGraphs(t);
					numLinks++;
				}
			}
			ys[t]=value/numLinks;
		}
		chart.addSeries("Speed", xs, ys);
		chart.saveAsPng("./data/youssef/SpeedAverage.png", 800, 600);
	}
	public void showTTAvgGraph() {
		XYLineChart chart = new XYLineChart("TT Avg", "Time(h)", "Travel Time(s)");
		double[] xs = new double[numIntervals];
		double[] ys = new double[numIntervals];
		for(int t=0;t<numIntervals;t++) {
			xs[t]=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			double value = 0;
			int numLinks = 0;
			for(Link link:network.getLinks().values()) {
				LinkData linkData = linksData.get(link.getId());
				if(linkData.isUsed()) {
					value+=linkData.getAvgTravelTimesGraphs(t);
					numLinks++;
				}
			}
			ys[t]=value/numLinks;
		}
		chart.addSeries("Travel Time", xs, ys);
		chart.saveAsPng("./data/youssef/TTAverage.png", 800, 600);
	}
	public void showKAvgGraph() {
		XYLineChart chart = new XYLineChart("Concentration", "Time(h)", "K(veh/m)");
		double[] xs = new double[numIntervals];
		double[] ys = new double[numIntervals];
		for(int t=0;t<numIntervals;t++) {
			xs[t]=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			double value = 0;
			int numLinks = 0;
			for(Link link:network.getLinks().values()) {
				LinkData linkData = linksData.get(link.getId());
				if(linkData!=null && linkData.isUsed()) {
					value+=linkData.getConcentration(t);
					numLinks++;
				}
			}
			ys[t]=value/numLinks;
		}
		chart.addSeries("Concentration", xs, ys);
		chart.saveAsPng("./data/youssef/ConcentrationAverage.png", 800, 600);
	}
	public void showFlowLinkGraph(String idString) {
		Id<Link> linkId = Id.createLinkId(idString);
		XYLineChart chart = new XYLineChart("Flow "+idString, "Time(h)", "Flow(veh/h)");
		LinkData linkData = linksData.get(linkId);
		double[] xs = new double[linkData.getTimeSize()];
		double[] ys = new double[linkData.getTimeSize()];
		for(int t=0;t<linkData.getTimeSize();t++) {
			xs[t]=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			ys[t]=linkData.getFlow(t)*3600;
		}
		chart.addSeries("Flow", xs, ys);
		chart.saveAsPng("./data/youssef/flowLink_"+linkId.toString()+".png", 800, 600);
	}
	public void showDensityLinkGraph(String idString) {
		Id<Link> linkId = Id.createLinkId(idString);
		XYLineChart chart = new XYLineChart("Density "+idString, "Time(h)", "Density(veh/Km)");
		LinkData linkData = linksData.get(linkId);
		double[] xs = new double[linkData.getTimeSize()];
		double[] ys = new double[linkData.getTimeSize()];
		for(int t=0;t<linkData.getTimeSize();t++) {
			xs[t]=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			ys[t]=linkData.getDensity(t)*1000;
		}
		chart.addSeries("Density", xs, ys);
		chart.saveAsPng("./data/youssef/densityLink_"+linkId.toString()+".png", 800, 600);
	}
	public void showTTLinkGraph(String idString) {
		Id<Link> linkId = Id.createLinkId(idString);
		XYLineChart chart = new XYLineChart("Travel Time "+idString, "Time(h)", "Travel Time(s)");
		LinkData linkData = linksData.get(linkId);
		double[] xs = new double[linkData.getTimeSize()];
		double[] ys = new double[linkData.getTimeSize()];
		for(int t=0;t<linkData.getTimeSize();t++) {
			xs[t]=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			ys[t]=linkData.getAvgTravelTimesGraphs(t);
		}
		chart.addSeries("Travel time", xs, ys);
		chart.saveAsPng("./data/youssef/ttLink_"+linkId.toString()+".png", 800, 600);
	}
	public void showSpeedLinkGraph(String idString) {
		Id<Link> linkId = Id.createLinkId(idString);
		XYLineChart chart = new XYLineChart("Avg Speed "+idString, "Time(h)", "Speed(Km/h)");
		LinkData linkData = linksData.get(linkId);
		double[] xs = new double[linkData.getTimeSize()];
		double[] ys = new double[linkData.getTimeSize()];
		for(int t=0;t<linkData.getTimeSize();t++) {
			xs[t]=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			ys[t]=linkData.getAvgSpeedsGraphs(t)*3600/1000;
		}
		chart.addSeries("Avg. Speed", xs, ys);
		chart.saveAsPng("./data/youssef/speedLink_"+linkId.toString()+".png", 800, 600);
	}
	public void showKLinkGraph(String idString) {
		Id<Link> linkId = Id.createLinkId(idString);
		XYLineChart chart = new XYLineChart("Concentration", "Time(h)", "K(veh/Km)");
		LinkData linkData = linksData.get(linkId);
		double[] xs = new double[linkData.getTimeSize()];
		double[] ys = new double[linkData.getTimeSize()];
		for(int t=0;t<linkData.getTimeSize();t++) {
			xs[t]=(t*TIME_INTERVAL+TIME_INTERVAL/2)/(60*60);
			ys[t]=linkData.getConcentration(t)*1000;
		}
		chart.addSeries("Concentration", xs, ys);
		chart.saveAsPng("./data/youssef/kLink_"+linkId.toString()+".png", 800, 600);
	}
	public void showFlowSpeedGraph(String idString) {
		Id<Link> linkId = Id.createLinkId(idString);
		XYScatterChart chart = new XYScatterChart("Flow Speed", "Flow(veh/s)", "Speed(m/s)");
		LinkData linkData = linksData.get(linkId);
		double[] xs = new double[linkData.getTimeSize()];
		double[] ys = new double[linkData.getTimeSize()];
		for(int t=0;t<linkData.getTimeSize();t++) {
			xs[t]=linkData.getFlow(t);
			ys[t]=linkData.getAvgSpeedsGraphs(t);
		}
		chart.addSeries("FlowSpeed", xs, ys);
		chart.saveAsPng("./data/youssef/fSLink_"+linkId.toString()+".png", 800, 600);
	}
	public void showDensityFlowGraph(String idString) {
		Id<Link> linkId = Id.createLinkId(idString);
		XYScatterChart chart = new XYScatterChart("Density Flow", "Density(veh/m)", "Flow(veh/s)");
		LinkData linkData = linksData.get(linkId);
		double[] xs = new double[linkData.getTimeSize()];
		double[] ys = new double[linkData.getTimeSize()];
		for(int t=0;t<linkData.getTimeSize();t++) {
			xs[t]=linkData.getDensity(t);
			ys[t]=linkData.getFlow(t);
		}
		chart.addSeries("DensityFlow", xs, ys);
		chart.saveAsPng("./data/youssef/dFLink_"+linkId.toString()+".png", 800, 600);
	}
	public void showDensitySpeedGraph(String idString) {
		Id<Link> linkId = Id.createLinkId(idString);
		XYScatterChart chart = new XYScatterChart("Density Speed", "Density(veh/m)", "Speed(m/s)");
		LinkData linkData = linksData.get(linkId);
		double[] xs = new double[linkData.getTimeSize()];
		double[] ys = new double[linkData.getTimeSize()];
		for(int t=0;t<linkData.getTimeSize();t++) {
			xs[t]=linkData.getDensity(t);
			ys[t]=linkData.getAvgSpeedsGraphs(t);
		}
		chart.addSeries("DensitySpeed", xs, ys);
		chart.saveAsPng("./data/youssef/dSLink_"+linkId.toString()+".png", 800, 600);
	}
	@Override
	public void handleEvent(LinkEnterEvent event) {
		enterLink(event.getTime(), event.getLinkId(), event.getDriverId());
	}
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		exitLink(event.getTime(), event.getLinkId(), event.getDriverId());
	}
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		beginActivityLink(event.getTime(), event.getLinkId(), event.getPersonId());
	}
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		finishActivityLink(event.getTime(), event.getLinkId(), event.getPersonId());
	}
	@Override
	public void handleEvent(PersonStuckEvent event) {
		System.out.println("stuck");
	}
	public void enterLink(double time, Id<Link> linkId, Id<Person> personId) {
		verifyNewInterval(time, linkId);
		LinkData linkData = linksData.get(linkId);
		if(linkData!=null)
			linkData.addEnterVehicle(personId, time);
	}
	public void exitLink(double time, Id<Link> linkId, Id<Person> personId) {
		verifyNewInterval(time, linkId);
		LinkData linkData = linksData.get(linkId);
		if(linkData!=null)
			linkData.addExitVehicle(personId, time);
	}
	public void finishActivityLink(double time, Id<Link> linkId, Id<Person> personId) {
		verifyNewInterval(time, linkId);
		LinkData linkData = linksData.get(linkId);
		if(linkData!=null)
			linkData.addEndActivity(personId, time);
	}
	public void beginActivityLink(double time, Id<Link> linkId, Id<Person> personId) {
		verifyNewInterval(time, linkId);
		LinkData linkData = linksData.get(linkId);
		if(linkData!=null)
			linkData.addStartActivity(personId, time);
	}
	public void verifyNewInterval(double time, Id<Link> linkId) {
		LinkData linkData = linksData.get(linkId);
		while(linkData!=null && time-linkData.getTimeSize()*TIME_INTERVAL>TIME_INTERVAL) {
			linkData.addTimeInterval();
			linkData = linksData.get(linkId);
		}
		if(linkData!=null && linkData.getTimeSize()>numIntervals)
			numIntervals=linkData.getTimeSize();
	}
	@Override
	public void reset(int iteration) {

	}

}
