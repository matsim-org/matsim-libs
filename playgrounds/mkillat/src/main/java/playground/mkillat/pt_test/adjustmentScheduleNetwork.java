package playground.mkillat.pt_test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;



public class adjustmentScheduleNetwork implements Runnable{

	
	private String configFile = "C:/Users/Marie/workspace_MATsim_d/playgrounds/mkillat/input/bus_berlin/config.xml";
	private String eventsFile = "C:/Users/Marie/workspace_MATsim_d/playgrounds/mkillat/output/bus_berlin3_nce5/ITERS/it.0/0.events.xml.gz";
	private String networkFile = "C:/Users/Marie/workspace_MATsim_d/playgrounds/mkillat/input/bus_berlin/network.final.xml.gz";
	private String transitScheduleFile = "C:/Users/Marie/workspace_MATsim_d/playgrounds/mkillat/input/bus_berlin/transitScheduleOut2.xml";
	private String networkChangeEventFile = "./input/bus_berlin/nce5.xml";
	String diffOutFilename = "./output/bus_berlin/diff6.txt";
	private String lineStr = "187";
	private String routeHinStr = "13-0";
	String fileDir = "./output/bus_berlin/test.txt";
//	private String configFile = "C:/Users/Marie/workspace_MATsim_d/playgrounds/mkillat/input/bus_test2/config.xml";
//	private String eventsFile = "C:/Users/Marie/workspace_MATsim_d/playgrounds/mkillat/output/bus_test5/ITERS/it.0/0.events.xml.gz";
//	private String networkFile = "C:/Users/Marie/workspace_MATsim_d/playgrounds/mkillat/input/bus_test2/network.xml";
//	private String transitScheduleFile = "C:/Users/Marie/workspace_MATsim_d/playgrounds/mkillat/input/bus_test2/transitschedule.xml";
//	private String networkChangeEventFile = "./input/bus_test2/nce4.xml";
//	String diffOutFilename = "./output/bus_test5/diff1.txt";
//	private String lineStr = "Blue Line";
//	private String routeHinStr = "1to3";
//	private Id busId = new IdImpl(id) 
//	private String routeRueckStr = "13-0";
	

//	private List <StopInformation> haltezeitRueck = new ArrayList<StopInformation>();
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
//	Welche Linie soll angepasst werden
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//	private Id lineId = new IdImpl(lineStr);
//	private Id routeId = new IdImpl(routeHinStr);
//	private Id routeIdRueck = new IdImpl(routeRueckStr);
	
	public static void main(String[] args) {
		adjustmentScheduleNetwork testPt = new adjustmentScheduleNetwork();
		testPt.run();
	}

	@Override
	public void run() {
//		readPublicTransitEvents(lineId, routeId);
//		readTrasitSchedule(lineId, routeId);
//		modifingTheNetwork();
//		test();
		automatischDurchAlleRouten();

		
	}



	private void automatischDurchAlleRouten() {
		Config config = ConfigUtils.loadConfig(configFile);
		config.network().setInputFile(networkFile);
		config.transit().setTransitScheduleFile(transitScheduleFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Map <Id<TransitLine>, TransitLine> transitLinesMap = scenario.getTransitSchedule().getTransitLines();
		List <NetworkChangeEvent> nces = new ArrayList<NetworkChangeEvent>();
		for (Iterator<TransitLine> it = transitLinesMap.values().iterator(); it.hasNext();) {
			Id<TransitLine> lineId = it.next().getId();
			TransitLine line = transitLinesMap.get(lineId);
			Map <Id<TransitRoute>, TransitRoute> routes = line.getRoutes();
			for (Iterator <TransitRoute> iterator = routes.values().iterator(); iterator.hasNext();) {
				Id<TransitRoute> routeId = iterator.next().getId();
				
				test(lineId, routeId);
				List <NetworkChangeEvent> networkChangeEvents = modifingTheNetwork2(lineId, routeId);
				for (int i = 0; i<networkChangeEvents.size(); i++) {
					nces.add(networkChangeEvents.get(i));
				}
				nces = anpassungNetworkChangeEvent(nces);
				
			}
			
		}
		NetworkChangeEventsWriter test = new NetworkChangeEventsWriter();
		test.write(networkChangeEventFile, nces);
		
	}

	private void test(Id lineId, Id routeId) {
		List <CompleteTransitRoute> completeTransitRoutes = readPublicTransitEvents(lineId, routeId);
		List <TransitScheduleTime> transitScheduleTimes = readTrasitSchedule(lineId, routeId);
		for (int i=0; i<transitScheduleTimes.size(); i++){
			for (int j = 0; j < completeTransitRoutes.get(i).departures.size(); j++) {
				double diff = comparisonActualAndTargetState2(transitScheduleTimes, completeTransitRoutes, i, j);
					if(diff<-1 || diff >1){ 
						try {
							
							BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileDir), true));

							writer.write("Der Bus der Line " + completeTransitRoutes.get(i).transitLineId + " mit der Route " + completeTransitRoutes.get(i).transitRouteId +
									" ist an der Haltestelle " + transitScheduleTimes.get(i).haltezeit.get(j+1).id + " " + diff + " s zu spät/früh angekommen.");							
							writer.newLine();
						
							writer.flush();
							writer.close();			
						} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					

					}

					}
			}
		}
	}
	

	private List <TransitScheduleTime> readTrasitSchedule(Id lineId, Id routeId) {
		List <TransitScheduleTime> transitScheduleTimes = new ArrayList <TransitScheduleTime> ();
		Config config = ConfigUtils.loadConfig(configFile);
		config.network().setInputFile(networkFile);
		config.transit().setTransitScheduleFile(transitScheduleFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Map <Id<TransitLine>, TransitLine> transitLinesMap = scenario.getTransitSchedule().getTransitLines();
		TransitLine line = transitLinesMap.get(lineId);	
		Map <Id<TransitRoute>, TransitRoute> routes = line.getRoutes();
		
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//		Erstmal einlesen der Hinrichtung
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		TransitRoute routeHin = routes.get(routeId);
		Map <Id<Departure>, Departure> departuresHin = routeHin.getDepartures();
		List <Id<Departure>> departuresIdsHin = new ArrayList<Id<Departure>>();
		for (Iterator <Departure> it = departuresHin.values().iterator(); it.hasNext();){
			departuresIdsHin.add(it.next().getId());
		}
		
//		erstmal nur für ein Departure, muss noch allgemein gemacht werden...

		double departureTimeHin01 = departuresHin.get(departuresIdsHin.get(0)).getDepartureTime();
		
		List<TransitRouteStop> routeStopsHin = routeHin.getStops();
		List <StopInformation> haltezeitHin = new ArrayList<StopInformation>(); 
		for (int i = 0; i < routeStopsHin.size(); i++) {
			StopInformation bb = new StopInformation(routeStopsHin.get(i).getStopFacility().getId(), routeStopsHin.get(i).getArrivalOffset(), routeStopsHin.get(i).getDepartureOffset());
			haltezeitHin.add(bb);
		}
		
		TransitScheduleTime transitScheduleTimeHin = new TransitScheduleTime(departureTimeHin01, haltezeitHin);
		transitScheduleTimes.add(transitScheduleTimeHin);
		
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//		Jetzt die Rückrichtung
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//		TransitRoute routeRueck = routes.get(routeIdRueck);
//		Map <Id, Departure> departuresRueck = routeRueck.getDepartures();
//		List <Id> departuresIdsRuck = new ArrayList<Id>();
//		for (Iterator <Departure> it = departuresRueck.values().iterator(); it.hasNext();){
//			departuresIdsRuck.add(it.next().getId());
//		}
//		
////		ersmal nur für ein Departure, muss noch allgemein gemacht werden...
//
//		double departureTimeRueck = departuresRueck.get(departuresIdsRuck.get(0)).getDepartureTime();
//		
//		List<TransitRouteStop> routeStopsRueck = routeRueck.getStops();
//		 
//		for (int i = 0; i < routeStopsRueck.size(); i++) {
//			StopInformation bb = new StopInformation(routeStopsRueck.get(i).getStopFacility().getId(), routeStopsRueck.get(i).getArrivalOffset(), routeStopsRueck.get(i).getDepartureOffset());
//			haltezeitRueck.add(bb);
//		}
//		
//		TransitScheduleTime transitScheduleTimeRück = new TransitScheduleTime(departureTimeRueck, haltezeitRueck);
//		transitScheduleTimes.add(transitScheduleTimeRück);
		

		return transitScheduleTimes;
		
		
		
	}

	private List<CompleteTransitRoute> readPublicTransitEvents(Id lineId, Id routeId) {

		List <CompleteTransitRoute> completeTransitRoutes = MyEventFileReaderPt.EventFileReader(lineId, routeId, configFile, eventsFile);
		
		System.out.println(completeTransitRoutes);
		return completeTransitRoutes;
	}
	
	
	
//	private void modifingTheNetwork() {
//		Config config = ConfigUtils.loadConfig(configFile);
//		config.network().setInputFile(networkFile);
//		config.transit().setTransitScheduleFile(transitScheduleFile);
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		List <CompleteTransitRoute> completeTransitRoutes = readPublicTransitEvents();
//		List <TransitScheduleTime> transitScheduleTimes = readTrasitSchedule(lineId, routeId);
//		System.out.println(transitScheduleTimes);
//		System.out.println(completeTransitRoutes);
//		List <NetworkChangeEvent> networkChangeEvents = new ArrayList<NetworkChangeEvent>();
//		try {
//			
//			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(diffOutFilename)));
//			writer.write("# from node id; x; y; to node id; x; y; diffPerLink"); writer.newLine();
//			
//		
//		
//			for (int i=0; i<transitScheduleTimes.size(); i++){
//				for (int j = 0; j < completeTransitRoutes.get(i).departures.size(); j++) {
//					double diff = comparisonActualAndTargetState2(transitScheduleTimes, completeTransitRoutes, i, j);
//						if(diff!=0){
//							List <Id> links = MyEventFileReaderPt2.EventFileReader(configFile, eventsFile, lineId, routeId, transitScheduleTimes.get(i).haltezeit.get(j).id, transitScheduleTimes.get(i).haltezeit.get(j+1).id);
//							Network net = scenario.getNetwork();
//							double linkLength=0;
//							for (int k = 0; k < links.size(); k++) {
//								linkLength =  linkLength + net.getLinks().get(links.get(k)).getLength();
//								
//							}
//							for (int k = 0; k < links.size(); k++) {
//								double diffPerLink = (diff/linkLength) * net.getLinks().get(links.get(k)).getLength();
//								writer.write(net.getLinks().get(links.get(k)).getFromNode().getId() + "; " + net.getLinks().get(links.get(k)).getFromNode().getCoord().getX()
//										+ "; " + net.getLinks().get(links.get(k)).getFromNode().getCoord().getY() + "; " + net.getLinks().get(links.get(k)).getToNode().getId()+ "; " + net.getLinks().get(links.get(k)).getToNode().getCoord().getX()
//										+ "; " + net.getLinks().get(links.get(k)).getToNode().getCoord().getY() + "; " + diffPerLink);
//										writer.newLine();
//							}
//							double fahrzeit = Math.abs(transitScheduleTimes.get(i).haltezeit.get(j+1).arrival-transitScheduleTimes.get(i).haltezeit.get(j).departure);
//							double speed = linkLength/fahrzeit;
//							ChangeType type = ChangeType.ABSOLUTE;
//							ChangeValue cv = new ChangeValue(type, speed);
//							NetworkChangeEvent nce = ((NetworkImpl) net).getFactory().createNetworkChangeEvent(0.0); //über die Zeit muss ich noch nachdenken
//							for (int k = 0; k < links.size(); k++) {
//								nce.addLink(net.getLinks().get(links.get(k)));
//							}
//						nce.setFreespeedChange(cv);	
//						networkChangeEvents.add(nce);						
//				}}
//					
//				
//				
//			}
//			writer.flush();
//			writer.close();			
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		
//		NetworkChangeEventsWriter test = new NetworkChangeEventsWriter();
//		test.write(networkChangeEventFile, networkChangeEvents);
//		System.out.println("Das NetworkChangeEventFile wurde nach " + networkChangeEventFile + " geschrieben.");
//		System.out.println("Die Daten der Differenz wurden nach " + diffOutFilename + " geschrieben.");
//		}
	
private List <NetworkChangeEvent> modifingTheNetwork2(Id lineId, Id routeId) {	
	Config config = ConfigUtils.loadConfig(configFile);
	config.network().setInputFile(networkFile);
	config.transit().setTransitScheduleFile(transitScheduleFile);
	Scenario scenario = ScenarioUtils.loadScenario(config);
	List <CompleteTransitRoute> completeTransitRoutes = readPublicTransitEvents(lineId, routeId);
	List <TransitScheduleTime> transitScheduleTimes = readTrasitSchedule(lineId, routeId);
	List <NetworkChangeEvent> networkChangeEvents = new ArrayList<NetworkChangeEvent>();

	try {
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(diffOutFilename),true));
		writer.write("# from node id; x; y; to node id; x; y; diffPerLink"); writer.newLine();
	
		for (int i=0; i<transitScheduleTimes.size(); i++){
			for (int j = 0; j < completeTransitRoutes.get(i).departures.size(); j++) {
				
				double diff = comparisonActualAndTargetState2(transitScheduleTimes, completeTransitRoutes, i, j);
					if(diff!=0){
						List <Id> links = MyEventFileReaderPt2.EventFileReader(configFile, eventsFile, lineId, routeId, transitScheduleTimes.get(i).haltezeit.get(j).id, transitScheduleTimes.get(i).haltezeit.get(j+1).id);
						Network net = scenario.getNetwork();
						double linkLength=0;
						for (int k = 0; k < links.size(); k++) {
							linkLength =  linkLength + net.getLinks().get(links.get(k)).getLength();
							
						}
						for (int k = 0; k < links.size(); k++) {
							double diffPerLink = (diff/linkLength) * net.getLinks().get(links.get(k)).getLength();
							writer.write(net.getLinks().get(links.get(k)).getFromNode().getId() + "; " + net.getLinks().get(links.get(k)).getFromNode().getCoord().getX()
									+ "; " + net.getLinks().get(links.get(k)).getFromNode().getCoord().getY() + "; " + net.getLinks().get(links.get(k)).getToNode().getId()+ "; " + net.getLinks().get(links.get(k)).getToNode().getCoord().getX()
									+ "; " + net.getLinks().get(links.get(k)).getToNode().getCoord().getY() + "; " + diffPerLink);
									writer.newLine();
						}
						double fahrzeit = Math.abs(transitScheduleTimes.get(i).haltezeit.get(j+1).arrival-transitScheduleTimes.get(i).haltezeit.get(j).departure);
						double speed = linkLength/fahrzeit;
						ChangeType type = ChangeType.ABSOLUTE;
						ChangeValue cv = new ChangeValue(type, speed);
						NetworkChangeEvent nce = ((NetworkImpl) net).getFactory().createNetworkChangeEvent(0.0); //über die Zeit muss ich noch nachdenken
						for (int k = 0; k < links.size(); k++) {
							nce.addLink(net.getLinks().get(links.get(k)));
						}
						nce.setFreespeedChange(cv);	
						networkChangeEvents.add(nce);
					
			}}}
			writer.flush();
			writer.close();			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
			
	
	
	return networkChangeEvents;
	}
	
	private double comparisonActualAndTargetState2(List <TransitScheduleTime> transitScheduleTimes, List <CompleteTransitRoute> completeTransitRoutes, int i, int j ) {
		double diff=0;
		double diffTST = 0;
		if(j!=completeTransitRoutes.get(i).departures.size()-1){
			
			double diffCTR = Math.abs(completeTransitRoutes.get(i).arrives.get(j+1)- completeTransitRoutes.get(i).departures.get(j));
			if(j==0){
				diffTST = Math.abs(transitScheduleTimes.get(i).haltezeit.get(j+1).arrival - transitScheduleTimes.get(i).haltezeit.get(j).departure);
			}
			
			if(j>0 && j<completeTransitRoutes.get(i).departures.size()){
				diffTST = Math.abs(transitScheduleTimes.get(i).haltezeit.get(j+1).arrival - transitScheduleTimes.get(i).haltezeit.get(j).departure);
			}
			
			if(j==completeTransitRoutes.get(i).departures.size()){
				return diff=0;
			}
			

			diff = diffTST - diffCTR;
		}
		
		return diff;
		
	}
	
private List <NetworkChangeEvent> anpassungNetworkChangeEvent (List <NetworkChangeEvent> nces){
	Config config = ConfigUtils.loadConfig(configFile);
	config.network().setInputFile(networkFile);
	config.transit().setTransitScheduleFile(transitScheduleFile);
	Scenario scenario = ScenarioUtils.loadScenario(config);
	Network net = scenario.getNetwork();
	
	Map <Id, List <Double>> linksPlusFactors = new HashMap <Id, List <Double>>(); 
	for (int i = 0; i < nces.size(); i++) {
		for (Iterator <Link> iterator = nces.get(i).getLinks().iterator(); iterator.hasNext();) {
			Link link = iterator.next();
			if (linksPlusFactors.containsKey(link.getId())){
				List <Double> bla = linksPlusFactors.get(link.getId());
				bla.add(nces.get(i).getFreespeedChange().getValue());
				linksPlusFactors.put(link.getId(), bla);
			}
			else{
				List <Double> blas = new ArrayList<Double>();
				blas.add(nces.get(i).getFreespeedChange().getValue());
				linksPlusFactors.put(link.getId(), blas);
				
			}
		}
			
		}
	
	List <NetworkChangeEvent> angepassteNces = new ArrayList<NetworkChangeEvent>();
	
	
	for (Entry<Id, List<Double>> entry : linksPlusFactors.entrySet()) {
		double sumValue=0;
		List <Double> temp = entry.getValue();
		for (int i = 0; i < temp.size(); i++) {
			sumValue=sumValue + temp.get(i); 
		}
		double meanValue = sumValue/temp.size();
		ChangeType type = ChangeType.ABSOLUTE;
		ChangeValue cv = new ChangeValue(type, meanValue);
		NetworkChangeEvent nce = ((NetworkImpl) net).getFactory().createNetworkChangeEvent(0.0); //über die Zeit muss ich noch nachdenken
		
		nce.addLink(net.getLinks().get(entry.getKey()));
		nce.setFreespeedChange(cv);
		angepassteNces.add(nce);
		 
		}
	
		
	
	
	return angepassteNces;
	
}
	
	
	}
