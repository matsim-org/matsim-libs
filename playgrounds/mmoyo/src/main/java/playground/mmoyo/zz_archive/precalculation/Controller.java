package playground.mmoyo.zz_archive.precalculation;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.xml.sax.SAXException;

/**invokes the precalculation of transit routes. Later on the best of them can be selected according to their properties*/
public class Controller {

	private static final String PATH = "../shared-svn/studies/schweiz-ivtch/pt-experimental/";
	private static final String NETWORK = PATH + "network.xml";
	private static final String PLANFILE = PATH +  "plans.xml";
	private static final String TRANSITSCHEDULEFILE = PATH + "transitSchedule.xml";


	public static void main(String[] args) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule transitSchedule = builder.createTransitSchedule();

		/***************reads the transitSchedule file**********/
		new MatsimNetworkReader(scenario).readFile(NETWORK);
		try {
			new TransitScheduleReaderV1(transitSchedule, scenario.getNetwork(), scenario).readFile(TRANSITSCHEDULEFILE);
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
		MatsimPopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(PLANFILE);

		//Create Kroutecalculator with global variables to fill the connection map and near stations
		KroutesCalculator kRoutesCalculator= new KroutesCalculator(transitSchedule, scenario.getNetwork(), connectionMap, nearStopMap);

		//precalculate routes and near stations of the population
		PlanRouteCalculator precalPopulation = new PlanRouteCalculator(transitSchedule, scenario.getNetwork(), connectionMap, scenario.getPopulation(), kRoutesCalculator);
		precalPopulation.PreCalRoutes();


	}



}
