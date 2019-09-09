package playground.vsp.andreas.utils.pt;

import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


/**
 * Removes every line from the transit schedule serving a designated stop or a stop in a given area 
 * 
 * @author aneumann
 *
 */
public class TransitLineRemover {
	
	private static final Logger log = Logger.getLogger(TransitLineRemover.class);

	public static void main(String[] args) {
		final String SCHEDULEFILE = "e:/_shared-svn/andreas/paratransit/input/trb_2012/transitSchedules/transitSchedule_basecase.xml.gz";
		final String NETWORKFILE  = "e:/_shared-svn/andreas/paratransit/input/trb_2012/network.final.xml.gz";
		final String NO_TXL_SCHEDULE_FILE = "e:/_shared-svn/andreas/paratransit/input/trb_2012/transitSchedules/transitSchedule_noTxlBusLines.xml.gz";
		final String TXL_BUS_LINES = "e:/_shared-svn/andreas/paratransit/input/trb_2012/transitSchedules/transitSchedule_onlyTxlBusLines.xml.gz";
		final String NO_BVG_BUSES = "e:/_shared-svn/andreas/paratransit/input/trb_2012/transitSchedules/transitSchedule_noBvgBusLines.xml.gz";
		final String ONLY_BVG_BUSES = "e:/_shared-svn/andreas/paratransit/input/trb_2012/transitSchedules/transitSchedule_onlyBvgBusLines.xml.gz";

		Coord minCoord = new Coord(4587744.0, 5824664.0);
		Coord maxCoord = new Coord(4588400.0, 5825400.0);
		
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule baseCaseTransitSchedule = builder.createTransitSchedule();

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORKFILE);
//		new TransitScheduleReaderV1(baseCaseTransitSchedule, network).readFile(SCHEDULEFILE);
		new TransitScheduleReaderV1(baseCaseTransitSchedule, new RouteFactories()).readFile(SCHEDULEFILE);
		
		// Move one stop to a new link (wrong matching in original model)
//		for (TransitStopFacility stop : baseCaseTransitSchedule.getFacilities().values()) {
//			if (stop.getId().toString().equalsIgnoreCase("202070.1")) {
//				log.info("Moving stop facility " + stop.getId() + " " + stop.getName() + " from link " + stop.getLinkId() + "...");
//				stop.setLinkId(new IdImpl("108036"));
//				log.info("... to link " + stop.getLinkId());
//			}
//		}
		
		
		Set<Id<TransitStopFacility>> stopsInArea = TransitLineRemover.getStopIdsWithinArea(baseCaseTransitSchedule, minCoord, maxCoord);
		Set<Id<TransitLine>> linesToRemove = TransitLineRemover.getLinesServingTheseStops(baseCaseTransitSchedule, stopsInArea);
		
		TransitSchedule noTxlTransitSchedule = TransitLineRemover.removeTransitLinesFromTransitSchedule(baseCaseTransitSchedule, linesToRemove);
		new TransitScheduleWriterV1(noTxlTransitSchedule).write(NO_TXL_SCHEDULE_FILE);
		
		Set<Id<TransitLine>> linesToKeep = new TreeSet<>();
		for (Id<TransitLine> lineId : noTxlTransitSchedule.getTransitLines().keySet()) {
			linesToKeep.add(lineId);
		}
		TransitSchedule txlBusLinesTransitSchedule = TransitLineRemover.removeTransitLinesFromTransitSchedule(baseCaseTransitSchedule, linesToKeep);
		new TransitScheduleWriterV1(txlBusLinesTransitSchedule).write(TXL_BUS_LINES);
		
		Set<Id<TransitLine>> buslines = new TreeSet<>();
		for (Id<TransitLine> lineId : baseCaseTransitSchedule.getTransitLines().keySet()) {
			if (lineId.toString().contains("-B-")) {
				buslines.add(lineId);
			}
		}
		TransitSchedule noBvgBusLinesSchedule = TransitLineRemover.removeTransitLinesFromTransitSchedule(baseCaseTransitSchedule, buslines);
		new TransitScheduleWriterV1(noBvgBusLinesSchedule).write(NO_BVG_BUSES);
		
		Set<Id<TransitLine>> noBuslines = new TreeSet<Id<TransitLine>>();
		for (Id<TransitLine> lineId : baseCaseTransitSchedule.getTransitLines().keySet()) {
			if (!lineId.toString().contains("-B-")) {
				noBuslines.add(lineId);
			}
		}
		TransitSchedule onlyBvgBusLinesSchedule = TransitLineRemover.removeTransitLinesFromTransitSchedule(baseCaseTransitSchedule, noBuslines);
		new TransitScheduleWriterV1(onlyBvgBusLinesSchedule).write(ONLY_BVG_BUSES);
	}
	
	public static Set<Id<TransitLine>> getLinesServingTheseStops(TransitSchedule transitSchedule, Set<Id<TransitStopFacility>> stopIds){
		log.info("Searching for lines serving one of the following stops:" + stopIds);
		Set<Id<TransitLine>> linesServingOneOfThoseStops = new TreeSet<>();
		
		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					if(stopIds.contains(stop.getStopFacility().getId())){
						linesServingOneOfThoseStops.add(line.getId());
					}						
				}
			}
		}
		
		log.info("Found the following " + linesServingOneOfThoseStops.size() + " lines: " + linesServingOneOfThoseStops);
		return linesServingOneOfThoseStops;
	}
	
	public static Set<Id<TransitStopFacility>> getStopIdsWithinArea(TransitSchedule transitSchedule, Coord minCoord, Coord maxCoord){
		log.info("Searching for stops within the area of " + minCoord.toString() + " - " + maxCoord.toString());

		Set<Id<TransitStopFacility>> stopsInArea = new TreeSet<>();
		
		for (TransitStopFacility stop : transitSchedule.getFacilities().values()) {
			if (minCoord.getX() < stop.getCoord().getX() && maxCoord.getX() > stop.getCoord().getX()) {
				if (minCoord.getY() < stop.getCoord().getY() && maxCoord.getY() > stop.getCoord().getY()) {
					stopsInArea.add(stop.getId());
				}
			}
		}
		
		log.info("The following " + stopsInArea.size() + " stops are within the area: " + stopsInArea);		
		return stopsInArea;
	}

	public static TransitSchedule removeTransitLinesFromTransitSchedule(TransitSchedule transitSchedule, Set<Id<TransitLine>> linesToRemove){
		log.info("Removing " + linesToRemove + " lines from transit schedule...");
		
		TransitSchedule tS = new TransitScheduleFactoryImpl().createTransitSchedule();
		
		for (TransitStopFacility stop : transitSchedule.getFacilities().values()) {
			tS.addStopFacility(stop);			
		}
		
		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			if(!linesToRemove.contains(line.getId())) {
				tS.addTransitLine(line);
			}
		}
		
		log.info("Old schedule contained " + transitSchedule.getTransitLines().values().size() + " lines.");
		log.info("New schedule contains " + tS.getTransitLines().values().size() + " lines.");
		return tS;		
	}

}
