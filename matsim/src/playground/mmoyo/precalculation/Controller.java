package playground.mmoyo.precalculation;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.xml.sax.SAXException;

/**invokes the precalculation of transit routes. Later on the best of them can be selected according to their properties*/ 
public class Controller {

	private static final String PATH = "../shared-svn/studies/schweiz-ivtch/pt-experimental/";
	private static final String NETWORK = PATH + "network.xml";
	private static final String PLANFILE = PATH +  "plans.xml";
	private static final String TRANSITSCHEDULEFILE = PATH + "transitSchedule.xml";
	
	
	public static void main(String[] args) {
		NetworkLayer net= new NetworkLayer(new NetworkFactoryImpl());
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule transitSchedule = builder.createTransitSchedule();
		
		/***************reads the transitSchedule file**********/
		new MatsimNetworkReader(net).readFile(NETWORK);
		try {
			new TransitScheduleReaderV1(transitSchedule, net).readFile(TRANSITSCHEDULEFILE);
		} catch (SAXException e){
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
		/*******************************************************/
	
		Map <Id, List<StaticConnection>> connectionMap= new TreeMap <Id, List<StaticConnection>>();
		Map <Coord, Collection<NodeImpl>> nearStopMap = new TreeMap <Coord, Collection<NodeImpl>>();
		
		//read population 
		PopulationImpl population = new PopulationImpl();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(population, net);
		plansReader.readFile(PLANFILE);
		
		//Create Kroutecalculator with global variables to fill the connection map and near stations
		KroutesCalculator kRoutesCalculator= new KroutesCalculator(transitSchedule, net, connectionMap, nearStopMap);
		
		//precalculate routes and near stations of the population
		PlanRouteCalculator precalPopulation = new PlanRouteCalculator(transitSchedule, net, connectionMap, population, kRoutesCalculator);
		precalPopulation.PreCalRoutes();
		
		
	}
	
	
	
}
