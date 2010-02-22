package playground.mzilske.bvg09;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitScheduleWriter;
import org.matsim.transitSchedule.api.TransitStopFacility;

public class HafasReader {
	
	private static String PATH_PREFIX = "../berlin-bvg09/urdaten/BVG-Fahrplan_2008/Daten/1_Mo-Do/";
	private static String FILENAME = "../berlin-bvg09/pt/nullfall_M44_344_U8/alldat";
	private static String OUT_FILENAME = "../berlin-bvg09/pt/nullfall_M44_344_U8/transitSchedule-HAFAS.xml";
	private String filename;
	private Collection<TransitRoute> routes = new ArrayList<TransitRoute>();
	private List<TransitRouteStop> currentStops = new ArrayList<TransitRouteStop>();
	private Id currentLineId;
	private Id currentDepartureId;
	private Double currentDepartureTime;
	private TransitScheduleFactory transitScheduleFactory = new TransitScheduleFactoryImpl();;
	private TransitSchedule transitSchedule = transitScheduleFactory.createTransitSchedule();
	private Map<Id, TransitStopFacility> facilities = new HashMap<Id, TransitStopFacility>();
	
	public static void main(String[] args) {
		HafasReader hafasReader = new HafasReader();
		try {
			Scanner scanner = new Scanner(new File(FILENAME));
			scanner.useDelimiter("\r\n|\n");
			while (scanner.hasNext()) {
				String filename = scanner.next();
				hafasReader.setFilename(PATH_PREFIX + filename);
				hafasReader.readFile();
			}
			hafasReader.writeSchedule();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	void setFilename(String filename) {
		this.filename = filename;
	}

	private void writeSchedule() {
		for (TransitStopFacility facility : facilities.values()) {
			transitSchedule.addStopFacility(facility);
		}
		TransitScheduleWriter transitScheduleWriter = new TransitScheduleWriter(transitSchedule);
		try {
			transitScheduleWriter.writeFile(OUT_FILENAME);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void addLine() {
		TransitLine transitLine = transitScheduleFactory.createTransitLine(currentLineId);
		for (TransitRoute transitRoute : routes) {
			transitLine.addRoute(transitRoute);
		}
		transitSchedule.addTransitLine(transitLine);
		routes.clear();
		currentLineId = null;
	}

	private void readFile() {
		File file = new File(filename);
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				processLine(scanner.nextLine());
			}
			enterRoute();
			addLine();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private void processLine(String nextLine) {
		if ("*Z".equals(nextLine.substring(0, 2))) {
			if (currentDepartureId != null) {
				enterRoute();
			}
			String idString = nextLine.substring(3, 8);
			currentDepartureId = new IdImpl(idString);
		} else if ("*L".equals(nextLine.substring(0, 2))) {
			Id newLineId = new IdImpl(nextLine.substring(3, 8));
			if (!newLineId.equals(currentLineId)) {
				if (currentLineId != null) {
					addLine();
				}
				currentLineId = newLineId;
			}
		} else if ("*".equals(nextLine.substring(0, 1))) {
			// Ignore
		} else {
			String stopPostString = nextLine.substring(0, 7);
			Integer arrivalTime = null;
			try {
				arrivalTime = Integer.valueOf(nextLine.substring(29, 33));
			} catch (NumberFormatException e) {
				
			}
			Integer departureTime = null;
			try {
				departureTime = Integer.valueOf(nextLine.substring(34, 38));
			} catch (NumberFormatException e) {
				
			}
			if (departureTime == null) {
				departureTime = arrivalTime;
			} else if (arrivalTime == null) {
				arrivalTime = departureTime;
			}
			if (currentDepartureTime == null) {
				currentDepartureTime = Time.convertHHMMInteger(departureTime);
			}
			TransitStopFacility stopPost = getOrCreateTransitStopFacility(new IdImpl(stopPostString));
			TransitRouteStop stop = transitScheduleFactory.createTransitRouteStop(stopPost, Time.convertHHMMInteger(arrivalTime) - currentDepartureTime, Time.convertHHMMInteger(departureTime) - currentDepartureTime);
			stop.setAwaitDepartureTime(true);
			currentStops.add(stop);
		}
	}

	private void enterRoute() {
		TransitRoute route = lookForRoute();
		if (route == null) {
			route = transitScheduleFactory.createTransitRoute(currentDepartureId, null, new ArrayList<TransitRouteStop>(currentStops), TransportMode.pt);
			routes.add(route);
		}
		Departure departure = transitScheduleFactory.createDeparture(currentDepartureId, currentDepartureTime);
		route.addDeparture(departure);
		currentStops.clear();
		currentDepartureId = null;
		currentDepartureTime = null;
	}

	private TransitRoute lookForRoute() {
		for (TransitRoute otherRoute : routes) {
			if (currentStops.equals(otherRoute.getStops())) {
				return otherRoute;
			}
		}
		return null;
	}

	private TransitStopFacility getOrCreateTransitStopFacility(IdImpl idImpl) {
		TransitStopFacility facility = facilities.get(idImpl);
		if (facility == null) {
			facility = transitScheduleFactory.createTransitStopFacility(idImpl, new CoordImpl(0.0, 0.0), false);
			facilities.put(idImpl, facility);
		}
		return facility;
	}

}