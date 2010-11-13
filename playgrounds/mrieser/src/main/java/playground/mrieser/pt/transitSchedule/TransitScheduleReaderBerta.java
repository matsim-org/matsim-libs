/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleReaderBerta.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.mrieser.pt.transitSchedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import playground.mrieser.SoldnerBerlinToWGS84;

/**
 * Reads transit schedule information from BERTA XML files once used
 * by the Berlin Transport Company.
 *
 * @author mrieser
 */
public class TransitScheduleReaderBerta extends MatsimXmlParser {

	private final static Logger log = Logger.getLogger(TransitScheduleReaderBerta.class);

	private static final String LINIENFAHRPLAN = "Linienfahrplan";
	private static final String BETRIEBSZWEIGNAME = "Betriebszweigname";
	private static final String BETRIEBSZWEIGNUMMER = "Betriebszweignummer";

	// Linie
	private static final String LINIE = "Linie";
	private static final String INTERNE_LINIENNUMMER = "interneLiniennummer";
	private static final String OEFFENTLICHE_LINIENNUMMER = "öffentlicheLiniennummer";

	// Haltepunkt
	private static final String HALTEPUNKT = "Haltepunkt";
	private static final String HALTEPUNKTNUMMER = "Haltepunktnummer";
	private static final String XKOORDINATE = "Xkoordinate";
	private static final String YKOORDINATE = "Ykoordinate";

	// Route
	private static final String ROUTE = "Route";
	private static final String ROUTENNUMMER = "Routennummer";
	private static final String ROUTENPUNKT = "Routenpunkt";
	private static final String POSITION = "Position";
	private static final String FAHRGASTWECHSEL = "Fahrgastwechsel";

	// Fahrzeitprofil
	private static final String FAHRZEITPROFIL = "Fahrzeitprofil";
	private static final String FAHRZEITPROFILNUMMER = "Fahrzeitprofilnummer";

	// Fahrzeitprofilpunkt
	private static final String FAHRZEITPROFILPUNKT = "Fahrzeitprofilpunkt";
	private static final String STRECKENFAHRZEIT = "Streckenfahrzeit";
	private static final String WARTEZEIT = "Wartezeit";

	// Fahrt
	private static final String FAHRT = "Fahrt";
	private static final String STARTZEIT = "Startzeit";
	private static final String VEROEFFENTLICHT = "veröffentlicht";


	private final TransitSchedule schedule;
	private final TransitScheduleFactory builder;
	private TransitLine currentTransitLine = null;
	private BLinie tmpLinie = null;
	private BHaltepunkt tmpHaltepunkt = null;
	private BRoute tmpRoute = null;
	private BRoutenpunkt tmpRoutenpunkt = null;
	private BFahrt tmpFahrt = null;
	private BFahrzeitprofil tmpFahrzeitprofil = null;
	private BFahrzeitprofilpunkt tmpFahrzeitprofilpunkt = null;
	private final Map<Id, BHaltepunkt> haltepunkte = new HashMap<Id, BHaltepunkt>();

	private int departureCounter = 0;

	private final SoldnerBerlinToWGS84 soldnerToWgs84 = new SoldnerBerlinToWGS84();
	private final CoordinateTransformation wgs84ToRBS;

	public TransitScheduleReaderBerta(final TransitSchedule schedule, final CoordinateTransformation coordTransformation) {
		this.schedule = schedule;
		this.builder = schedule.getFactory();
		this.wgs84ToRBS = coordTransformation;
	}

	public void readFile(final String filename) throws SAXException, ParserConfigurationException, IOException {
		this.parse(new InputSource(new BufferedReader(new InputStreamReader(new FileInputStream(filename), "ISO-8859-1"))));
	}

	public void readFile(final File file) throws SAXException, ParserConfigurationException, IOException {
		this.parse(new InputSource(new BufferedReader(new InputStreamReader(new FileInputStream(file), "ISO-8859-1"))));
	}

	public void readDirectory(final String directoryName) throws SAXException, ParserConfigurationException, IOException {
		File directory = new File(directoryName);
		File[] files = directory.listFiles();
		for (File file : files) {
			if (file.getName().endsWith(".xml")) {
				log.info("Parsing file: " + file.getAbsolutePath());
				readFile(file);
			}
		}
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (HALTEPUNKT.equals(name)) {
			this.tmpHaltepunkt = new BHaltepunkt();
		} else if (ROUTE.equals(name)) {
			this.tmpRoute = new BRoute();
		} else if (ROUTENPUNKT.equals(name)) {
			this.tmpRoutenpunkt = new BRoutenpunkt();
		} else if (FAHRZEITPROFIL.equals(name)) {
			this.tmpFahrzeitprofil = new BFahrzeitprofil();
		} else if (FAHRZEITPROFILPUNKT.equals(name)) {
			this.tmpFahrzeitprofilpunkt = new BFahrzeitprofilpunkt();
		} else if (FAHRT.equals(name)) {
			this.tmpFahrt = new BFahrt();
		} else if (LINIENFAHRPLAN.equals(name)) {
			this.tmpLinie = new BLinie();
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (HALTEPUNKTNUMMER.equals(name) && HALTEPUNKT.equals(context.peek())) {
			this.tmpHaltepunkt.id = new IdImpl(this.tmpLinie.betriebszweignummer + "_" + content);
		} else if (HALTEPUNKTNUMMER.equals(name) && ROUTENPUNKT.equals(context.peek())) {
			this.tmpRoutenpunkt.haltepunkt = this.haltepunkte.get(new IdImpl(this.tmpLinie.betriebszweignummer + "_" + content));
		} else if (XKOORDINATE.equals(name)) {
			if (content.length() > 0) {
				this.tmpHaltepunkt.x = Double.parseDouble(content);
			}
		} else if (YKOORDINATE.equals(name)) {
			if (content.length() > 0) {
				this.tmpHaltepunkt.y = Double.parseDouble(content);
			}
		} else if (HALTEPUNKT.equals(name)) {
			this.haltepunkte.put(this.tmpHaltepunkt.id, this.tmpHaltepunkt);
			this.tmpHaltepunkt = null;
		} else if (ROUTENNUMMER.equals(name)) {
			this.tmpRoute.id = new IdImpl(content);
		} else if (ROUTE.equals(name)) {
			convertRoute(this.tmpRoute);
			this.tmpRoute = null;
		} else if (ROUTENPUNKT.equals(name)) {
			this.tmpRoute.routenpunkte.put(new IdImpl(this.tmpRoutenpunkt.position), this.tmpRoutenpunkt);
			this.tmpRoutenpunkt = null;
		} else if (POSITION.equals(name) && ROUTENPUNKT.equals(context.peek())) {
			this.tmpRoutenpunkt.position = Integer.parseInt(content);
		} else if (POSITION.equals(name) && FAHRZEITPROFILPUNKT.equals(context.peek())) {
			this.tmpFahrzeitprofilpunkt.routenpunkt = this.tmpRoute.routenpunkte.get(new IdImpl(content));
		} else if (FAHRGASTWECHSEL.equals(name)) {
			this.tmpRoutenpunkt.realStop = !("N".equals(content));
		} else if (OEFFENTLICHE_LINIENNUMMER.equals(name)) {
			this.tmpLinie.publicId = content;
		} else if (INTERNE_LINIENNUMMER.equals(name)) {
			this.tmpLinie.id = content;
		} else if (BETRIEBSZWEIGNAME.equals(name)) {
			this.tmpLinie.betriebszweig = content;
		} else if (BETRIEBSZWEIGNUMMER.equals(name)) {
			this.tmpLinie.betriebszweignummer = content;
		} else if (LINIE.equals(name)) {
			this.currentTransitLine = this.builder.createTransitLine(new IdImpl(this.tmpLinie.betriebszweig + " " + this.tmpLinie.id));
		} else if (LINIENFAHRPLAN.equals(name)) {
			if (this.currentTransitLine.getRoutes().size() > 0) {
				this.schedule.addTransitLine(this.currentTransitLine);
			}
			this.currentTransitLine = null;
			this.tmpLinie = null;
			this.departureCounter = 0;
		} else if (FAHRZEITPROFIL.equals(name)) {
			this.tmpRoute.fahrzeitprofile.put(this.tmpFahrzeitprofil.id, this.tmpFahrzeitprofil);
		} else if (FAHRZEITPROFILPUNKT.equals(name)) {
			this.tmpFahrzeitprofil.profilpunkte.add(this.tmpFahrzeitprofilpunkt);
			this.tmpFahrzeitprofilpunkt = null;
		} else if (FAHRZEITPROFILNUMMER.equals(name) && FAHRZEITPROFIL.equals(context.peek())) {
			this.tmpFahrzeitprofil.id = new IdImpl(content);
		} else if (FAHRZEITPROFILNUMMER.equals(name) && FAHRT.equals(context.peek())) {
			this.tmpFahrt.fahrzeitprofil = this.tmpRoute.fahrzeitprofile.get(new IdImpl(content));
		} else if (STRECKENFAHRZEIT.equals(name)) {
			this.tmpFahrzeitprofilpunkt.fahrzeit = Double.parseDouble(content);
		} else if (WARTEZEIT.equals(name)) {
			this.tmpFahrzeitprofilpunkt.wartezeit = Double.parseDouble(content);
		} else if (FAHRT.equals(name)) {
			if (this.tmpFahrt.published) {
				this.tmpRoute.fahrten.add(this.tmpFahrt);
			}
			this.tmpFahrt = null;
		} else if (STARTZEIT.equals(name)) {
			this.tmpFahrt.departureTime = Double.parseDouble(content);
		} else if (VEROEFFENTLICHT.equals(name)) {
			this.tmpFahrt.published = !("N".equals(content));
		}
	}

	private void convertRoute(final BRoute route) {
		Map<Id, TransitRoute> transitRoutes = new HashMap<Id, TransitRoute>();

		for (BFahrt fahrt : route.fahrten) {
			TransitRoute tRoute = transitRoutes.get(getTransitRouteId(route, fahrt.fahrzeitprofil));
			if (tRoute == null) {
				tRoute = convertFahrzeitprofil(route, fahrt.fahrzeitprofil);
				transitRoutes.put(tRoute.getId(), tRoute);
				this.currentTransitLine.addRoute(tRoute);
			}
			tRoute.addDeparture(this.builder.createDeparture(new IdImpl(this.departureCounter++), fahrt.departureTime));
		}
	}

	private TransitRoute convertFahrzeitprofil(final BRoute route, final BFahrzeitprofil fahrzeitprofil) {
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();

		double timeOffset = 0;
		for (BFahrzeitprofilpunkt profilpunkt : fahrzeitprofil.profilpunkte) {
			timeOffset += profilpunkt.fahrzeit;
			if (profilpunkt.routenpunkt.realStop) {
				stops.add(this.builder.createTransitRouteStop(getStopFacility(profilpunkt.routenpunkt.haltepunkt), timeOffset, timeOffset + profilpunkt.wartezeit));
			}
			timeOffset += profilpunkt.wartezeit;
		}
		Id routeId = getTransitRouteId(route, fahrzeitprofil);
		TransitRoute transitRoute = this.builder.createTransitRoute(routeId, null, stops, "bus"); // TODO find correct transport mode
		transitRoute.setDescription("Linie " + this.tmpLinie.publicId);
		return transitRoute;
	}

	private Id getTransitRouteId(final BRoute route, final BFahrzeitprofil fahrzeitprofil) {
		return new IdImpl(route.id.toString() + "-" + fahrzeitprofil.id.toString());
	}

	private TransitStopFacility getStopFacility(final BHaltepunkt hp) {
		TransitStopFacility facility = this.schedule.getFacilities().get(hp.id);
		if (facility == null) {
			Coord coord = this.wgs84ToRBS.transform(this.soldnerToWgs84.transform(new CoordImpl(hp.x/1000.0, hp.y/1000.0)));
			facility = this.schedule.getFactory().createTransitStopFacility(hp.id, coord, false);
			this.schedule.addStopFacility(facility);
		}
		return facility;
	}

	protected static class BLinie {
		/*package*/ String id = null;
		/*package*/ String publicId = null;
		/*package*/ String betriebszweig = null;
		/*package*/ String betriebszweignummer = null;
	}

	protected static class BHaltepunkt {
		/*package*/ Id id = null;
		/*package*/ double x = 0;
		/*package*/ double y = 0;
	}

	protected static class BRoute {
		/*package*/ Id id = null;
		/*package*/ Map<Id, BRoutenpunkt> routenpunkte = new HashMap<Id, BRoutenpunkt>();
		/*package*/ Map<Id, BFahrzeitprofil> fahrzeitprofile = new HashMap<Id, BFahrzeitprofil>();
		/*package*/ List<BFahrt> fahrten = new ArrayList<BFahrt>();
	}

	protected static class BRoutenpunkt {
		/*package*/ int position = -1;
		/*package*/ BHaltepunkt haltepunkt;
		/*package*/ boolean realStop = true;
	}

	protected static class BFahrzeitprofil {
		/*package*/ Id id = null;
		/*package*/ List<BFahrzeitprofilpunkt> profilpunkte = new ArrayList<BFahrzeitprofilpunkt>();
	}

	protected static class BFahrzeitprofilpunkt {
		/*package*/ double fahrzeit = 0;
		/*package*/ double wartezeit = 0;
		/*package*/ BRoutenpunkt routenpunkt = null;
	}

	protected static class BFahrt {
		/*package*/ double departureTime = Time.UNDEFINED_TIME;
		/*package*/ boolean published = true;
		/*package*/ BFahrzeitprofil fahrzeitprofil = null;
	}


	public static void main(final String[] args) throws SAXException, ParserConfigurationException, IOException {
		// TODO [MR] remove after testing
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = builder.createTransitSchedule();
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);
		TransitScheduleReaderBerta reader = new TransitScheduleReaderBerta(schedule, transformation);
		reader.setLocalDtdDirectory("../thesis-data/examples/berta/");
//		reader.readFile("../thesis-data/examples/berta/Bus145.xml");
		reader.readDirectory("../thesis-data/examples/berta/sample/");

		log.info("writing schedule.xml");
		new TransitScheduleWriterV1(schedule).write("../thesis-data/examples/berta/schedule.xml");

//		log.info("creating routing network.xml");
//		new TransitRouter(schedule); // writes out "wrappedNetwork.xml" for debugging
//		new CreatePseudoNetwork().run();// writes out "pseudoNetwork.xml" for debugging
	}

}
