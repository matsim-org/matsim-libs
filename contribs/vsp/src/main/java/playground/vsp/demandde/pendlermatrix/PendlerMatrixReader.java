/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.vsp.demandde.pendlermatrix;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.Facility;
import org.opengis.feature.simple.SimpleFeature;


public class PendlerMatrixReader {

	private static final Logger log = LogManager.getLogger(PendlerMatrixReader.class);

	private static final String PV_EINPENDLERMATRIX = "../../detailedEval/eingangsdaten/Pendlermatrizen/EinpendlerMUC_843_062004.csv";

	private static final String PV_AUSPENDLERMATRIX = "../../detailedEval/eingangsdaten/Pendlermatrizen/AuspendlerMUC_843_062004.csv";

	//	private static final String NODES = "../../shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2004/strasse/knoten_wgs84.csv";

	private final Map<Integer, ActivityFacility> facilities = new HashMap<Integer, ActivityFacility>();

	private TripFlowSink flowSink;

	private final String shapeFile;

	private final Scenario sc;

	public PendlerMatrixReader(String shapeFile) {
		this.shapeFile = shapeFile;
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}

	public void run() {
		//		readNodes();
		readShape();
		readMatrix(PV_EINPENDLERMATRIX);
		readMatrix(PV_AUSPENDLERMATRIX);
		this.flowSink.complete();
	}

	private void readShape() {
		Collection<SimpleFeature> landkreise = ShapeFileReader.getAllFeatures(this.shapeFile);
		ActivityFacilitiesFactory factory = ((MutableScenario)this.sc).getActivityFacilities().getFactory();
		for (SimpleFeature landkreis : landkreise) {
			Integer gemeindeschluessel = Integer.parseInt((String) landkreis.getAttribute("gemeindesc"));
			Geometry geo = (Geometry) landkreis.getDefaultGeometry();
			Point point = getRandomPointInFeature(new Random(), geo);
			Coordinate coordinate = point.getCoordinate();
			Double xcoordinate = coordinate.x;
			Double ycoordinate = coordinate.y;
			Coord coord = new Coord(Double.parseDouble(xcoordinate.toString()), Double.parseDouble(ycoordinate.toString()));
			ActivityFacility facility = factory.createActivityFacility(Id.create(gemeindeschluessel, ActivityFacility.class), coord);
			{
				ActivityOption option = factory.createActivityOption("work");
				option.setCapacity(1.);
				facility.addActivityOption(option);
			}
			{
				ActivityOption option = factory.createActivityOption("home");
				option.setCapacity(1.);
				facility.addActivityOption(option);
			}
			this.facilities.put(gemeindeschluessel, facility);
		}
	}

	private static Point getRandomPointInFeature(Random rnd, Geometry g) {
		Point p = null;
		double x, y;
		do {
			x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
			y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!g.contains(p));
		return p;
	}

	//// leaves some of the municipalities empty...
	//
	//	private void readNodes() {
	//		final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);
	//		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
	//		tabFileParserConfig.setFileName(NODES);
	//		tabFileParserConfig.setDelimiterTags(new String[] {";"});
	//		try {
	//			new TabularFileParser().parse(tabFileParserConfig,
	//					new TabularFileHandler() {
	//				@Override
	//				public void startRow(String[] row) {
	//					if (row[0].startsWith("Knoten")) {
	//						return;
	//					}
	//					int zone = Integer.parseInt(row[5]);
	//					double x = Double.parseDouble(row[2]);
	//					double y = Double.parseDouble(row[3]);
	//					Zone zone1 = new Zone(zone, 1, 1, coordinateTransformation.transform(new CoordImpl(x,y)));
	//					zones.put(zone, zone1);
	//				}
	//
	//			});
	//		} catch (IOException e) {
	//			throw new RuntimeException(e);
	//		}
	//	}

	private void readMatrix(final String filename) {

		LogManager.getLogger(this.getClass()).warn("this method may read double entries in the Pendlermatrix (such as Nuernberg) twice. " +
						"If this may be a problem, you need to check.  kai, apr'11");

		System.out.println("======================" + "\n"
						+ "Start reading " + filename + "\n"
						+ "======================" + "\n");
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {","});
		new TabularFileParser().parse(tabFileParserConfig,
						new TabularFileHandler() {

			@Override
			public void startRow(String[] row) {
				if (row[0].startsWith("#")) {
					return;
				}
				Integer quelle = null;
				Integer ziel = 0;
				// car market share for commuter work/education trips (taken from "Regionaler Nahverkehrsplan-Fortschreibung, MVV 2007)
				double carMarketShare = 0.67;
				// scale factor, since Pendlermatrix only considers "sozialversicherungspflichtige Arbeitnehmer" (taken from GuthEtAl2005)
				double scaleFactor = 1.29;

				if (filename.equals(PV_EINPENDLERMATRIX)){
					try {
						quelle = Integer.parseInt(row[2]);
						ziel = 9162;

						int totalTrips = (int) (scaleFactor * Integer.parseInt(row[4]));
						int workPt = (int) ((1 - carMarketShare) * totalTrips);
						int educationPt = 0;
						int workCar = (int) (carMarketShare * totalTrips);
						int educationCar = 0;
						String label = row[3];
						if ( !label.contains("brige ") && !quelle.equals(ziel)) {
							process(quelle, ziel, workPt, educationPt, workCar, educationCar);
						} else {
							System.out.println( " uebrige? : " + label);
						}
					} catch ( Exception ee) {
						System.err.println("we are trying to read quelle: " + quelle);
						//						System.exit(-1);
					}
				}
				else if (filename.equals(PV_AUSPENDLERMATRIX)){
					try {
						quelle = 9162;
						ziel = Integer.parseInt(row[2]);

						int totalTrips = (int) (scaleFactor * Integer.parseInt(row[4]));
						int workPt = (int) ((1 - carMarketShare) * totalTrips);
						int educationPt = 0;
						int workCar = (int) (carMarketShare * totalTrips);
						int educationCar = 0;
						String label = row[3];
						if ( !label.contains("brige ") && !quelle.equals(ziel)) {
							process(quelle, ziel, workPt, educationPt, workCar, educationCar);
						} else {
							System.out.println( " uebrige? : " + label);
						}
					} catch ( Exception ee) {
						System.err.println("we are trying to read quelle: " + quelle);
						//						System.exit(-1);
					}
				}
				else{
					System.err.println("ATTENTION: check filename!");
				}
			}

		});
	}

	private void process(int quelle, int ziel, int workPt, int educationPt, int workCar, int educationCar) {
		Facility source = this.facilities.get(quelle);
		Facility sink = this.facilities.get(ziel);
		if (source == null) {
			log.error("Unknown source: " + quelle);
			return;
		}
		if (sink == null) {
			log.error("Unknown sink: " + ziel);
			return;
		}
		int carQuantity = workCar + educationCar;
		int ptQuantity = workPt + educationPt;
		int scaledCarQuantity = scale(carQuantity);
		int scaledPtQuantity = scale(ptQuantity);

		if (scaledCarQuantity != 0) {
			log.info(quelle + "->" + ziel + ": " + scaledCarQuantity + " car trips");
			this.flowSink.process(this.facilities.get(quelle), this.facilities.get(ziel), scaledCarQuantity, TransportMode.car, "pvWork", 0.0);
		}
		if (scaledPtQuantity != 0){
			log.info(quelle + "->" + ziel + ": " + scaledPtQuantity + " pt trips");
			this.flowSink.process(this.facilities.get(quelle), this.facilities.get(ziel), scaledPtQuantity, TransportMode.pt, "pvWork", 0.0);
		}
	}

	private int scale(int quantityOut) {
		int scaled = (int) (quantityOut * 0.1);
		return scaled;
	}

	public void setFlowSink(TripFlowSink flowSink) {
		this.flowSink = flowSink;
	}

}
