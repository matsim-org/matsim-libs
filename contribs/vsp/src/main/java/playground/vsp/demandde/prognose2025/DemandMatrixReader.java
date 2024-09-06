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

package playground.vsp.demandde.prognose2025;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.gis.Zone;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.GeoFileReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.Facility;

import playground.vsp.demandde.pendlermatrix.TripFlowSink;


public class DemandMatrixReader {

	private static final Logger log = LogManager.getLogger(DemandMatrixReader.class);

	// PV Matrix enthält keine Richtungs-Info für den Pendlerverkehr --> Pendlerstatistik
	//	private static final String PV_MATRIX = "../../shared-svn/studies/countries/de/prognose_2025/orig/pv-matrizen/2004_nuts_102r6x6.csv";

	private static final String GV_MATRIX = "../../shared-svn/studies/countries/de/prognose_2025/orig/gv-matrizen/gv-matrix-2004.csv";

	private static final String ANBINDUNG = "../../shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2004/strasse/anbindung.csv" ;
	private static final String NODES = "../../shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2004/strasse/knoten_wgs84.csv" ;

	private final Map<Integer, ActivityFacility> facilities = new HashMap<Integer, ActivityFacility>();

	private TripFlowSink flowSink;

	private final String shapeFile;

	private final Scenario sc ;

	private final Map<Id<Zone>, NodesAndDistances> zoneToConnectorsMap = new HashMap<>();

	class NodesAndDistances {
		private final Map<Id<Node>,Double> nodesAndDistances = new HashMap<>();
		Map<Id<Node>,Double> getNodesAndDistances() {
			return this.nodesAndDistances ;
		}
	}

	public DemandMatrixReader(String shapeFile) {
		this.shapeFile = shapeFile;
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}

	public void run() {
		readNodes();
		readShape();
		readAnbindungen();
		//		readMatrix(PV_MATRIX);
		readMatrix(GV_MATRIX);
		this.flowSink.complete();
	}

	private void readAnbindungen() {
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(ANBINDUNG);
		tabFileParserConfig.setDelimiterTags(new String[]{";"});
		try {
			new TabularFileParser().parse(tabFileParserConfig,
					new TabularFileHandler() {

				@Override
				public void startRow(String[] row) {
					if ( row[0].startsWith("Zonennummer") ) {
						return ;
					}
					int pvgv = Integer.parseInt(row[1]);
					if ( pvgv==1 ) { // "1" means "Anbindung f. PV"
						return ;
					}
					Id<Zone> zoneId = Id.create(row[1], Zone.class);
					int anzahl = Integer.parseInt(row[2]);
					NodesAndDistances nodeToDistanceMap = new NodesAndDistances();
					for ( int ii=0 ; ii<anzahl ; ii++ ) {
						Id<Node> nodeId = Id.create( row[3+2*ii], Node.class);
						double distance = Double.parseDouble(row[3+2*ii+1]);
						nodeToDistanceMap.getNodesAndDistances().put( nodeId, distance);
					}
					DemandMatrixReader.this.zoneToConnectorsMap.put( zoneId, nodeToDistanceMap );
				}
			});
		} catch ( Exception e ) {
			throw new RuntimeException(e);
		}
	}

	private void readNodes() {
		final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(NODES);
		tabFileParserConfig.setDelimiterTags(new String[] {";"});
		try {
			new TabularFileParser().parse(tabFileParserConfig,
					new TabularFileHandler() {
				@Override
				public void startRow(String[] row) {
					if (row[0].startsWith("Knoten")) {
						return;
					}
					double x = Double.parseDouble(row[2]);
					double y = Double.parseDouble(row[3]);
					Node node = DemandMatrixReader.this.sc.getNetwork().getFactory().createNode(Id.create(row[5], Node.class),
							coordinateTransformation.transform(new Coord(x, y)));
					DemandMatrixReader.this.sc.getNetwork().addNode(node);
				}

			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void readShape() {
		Collection<SimpleFeature> landkreise = GeoFileReader.getAllFeatures(this.shapeFile);
		final ActivityFacilitiesFactory factory = this.sc.getActivityFacilities().getFactory();
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

	enum Verkehrsmittel { railConv, railComb, LKW, ship }
	enum Guetergruppe { gg0, gg1, gg2, gg3, gg4, gg5, gg6, gg7, gg8, gg9 }

	private void readMatrix(final String filename) {

		System.out.println("======================" + "\n"
				+ "Start reading " + filename + "\n"
				+ "======================" + "\n");
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {";"});
		new TabularFileParser().parse(tabFileParserConfig,
				new TabularFileHandler() {

			@Override
			public void startRow(String[] row) {
				if (row[0].startsWith("#")) {
					return;
				}
				if (filename.equals(GV_MATRIX)){
					try {
						int idx = 0 ;
						Id<Zone> quellZonenId = Id.create(row[idx], Zone.class); idx++ ;
						Id<Zone> zielZonenId = Id.create(row[idx], Zone.class); idx++ ;

						Verkehrsmittel vm = Verkehrsmittel.values()[idx] ; idx++ ;
						Guetergruppe gg = Guetergruppe.values()[idx] ; idx++ ;
						idx++ ; // Seehafenhinterlandverkehr
						double tons = Double.parseDouble(row[idx]);

						process( quellZonenId, zielZonenId, vm, gg, tons);

					} catch ( Exception ee ) {
						throw new RuntimeException(ee);
					}
				}
				else{
					System.err.println("ATTENTION: check filename!");
				}
			}

		});
	}

	private void process(Id<Zone> quellZonenId, Id<Zone> zielZonenId, Verkehrsmittel vm, Guetergruppe gg, double tons) {
		getCoordFromZone(quellZonenId);
	}

	private Coord getCoordFromZone(Id<Zone> zoneId) {
		NodesAndDistances connectors = this.zoneToConnectorsMap.get(zoneId);
		Node node = null ;
		for ( Id<Node> nodeId : connectors.getNodesAndDistances().keySet() ) {
			node = this.sc.getNetwork().getNodes().get( nodeId);
			break ;
		}
		return node.getCoord();
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
		int carQuantity = workCar + educationCar ;
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
		int scaled = (int) (quantityOut * 0.1 );
		return scaled;
	}

	public void setFlowSink(TripFlowSink flowSink) {
		this.flowSink = flowSink;
	}

}
