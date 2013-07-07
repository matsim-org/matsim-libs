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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.facilities.ActivityFacilitiesFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.opengis.feature.simple.SimpleFeature;

import playground.vsp.demandde.pendlermatrix.TripFlowSink;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;


public class DemandMatrixReader {

	private static final Logger log = Logger.getLogger(DemandMatrixReader.class);

	// PV Matrix enthält keine Richtungs-Info für den Pendlerverkehr --> Pendlerstatistik
	//	private static final String PV_MATRIX = "../../shared-svn/studies/countries/de/prognose_2025/orig/pv-matrizen/2004_nuts_102r6x6.csv";

	private static final String GV_MATRIX = "../../shared-svn/studies/countries/de/prognose_2025/orig/gv-matrizen/gv-matrix-2004.csv";

	private static final String ANBINDUNG = "../../shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2004/strasse/anbindung.csv" ;
	private static final String NODES = "../../shared-svn/studies/countries/de/prognose_2025/orig/netze/netz-2004/strasse/knoten_wgs84.csv" ;

	private Map<Integer, ActivityFacility> facilities = new HashMap<Integer, ActivityFacility>();

	private TripFlowSink flowSink;

	private String shapeFile;

	private Scenario sc ;

	private Map<Id, NodesAndDistances> zoneToConnectorsMap = new HashMap<Id,NodesAndDistances>() ;
	
	class NodesAndDistances {
		private Map<Id,Double> nodesAndDistances = new HashMap<Id,Double>() ;
		Map<Id,Double> getNodesAndDistances() {
			return nodesAndDistances ;
		}
	}

	public DemandMatrixReader(String shapeFile) {
		this.shapeFile = shapeFile;
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig()) ;
	}

	public void run() {
		readNodes();
		readShape();
		readAnbindungen() ;
		//		readMatrix(PV_MATRIX);
		readMatrix(GV_MATRIX);
		flowSink.complete();
	}

	private void readAnbindungen() {
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig() ;
		tabFileParserConfig.setFileName(ANBINDUNG) ;
		tabFileParserConfig.setDelimiterTags(new String[]{";"}) ;
		try {
			new TabularFileParser().parse(tabFileParserConfig, 
					new TabularFileHandler() {

				@Override
				public void startRow(String[] row) {
					if ( row[0].startsWith("Zonennummer") ) {
						return ;
					}
					int pvgv = Integer.parseInt(row[1]) ;
					if ( pvgv==1 ) { // "1" means "Anbindung f. PV"
						return ; 
					}
					Id zoneId = sc.createId(row[1]);
					int anzahl = Integer.parseInt(row[2]) ;
					NodesAndDistances nodeToDistanceMap = new NodesAndDistances() ;
					for ( int ii=0 ; ii<anzahl ; ii++ ) {
						Id nodeId = sc.createId( row[3+2*ii] ) ;
						double distance = Double.parseDouble(row[3+2*ii+1]) ;
						nodeToDistanceMap.getNodesAndDistances().put( nodeId, distance ) ;
					}
					zoneToConnectorsMap.put( zoneId, nodeToDistanceMap );
				}
			});
		} catch ( Exception e ) {
			throw new RuntimeException(e) ;
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
					Node node = sc.getNetwork().getFactory().createNode(sc.createId(row[5]), 
							coordinateTransformation.transform(sc.createCoord(x, y) ) ) ;
					sc.getNetwork().addNode(node) ;
				}

			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void readShape() {
		Collection<SimpleFeature> landkreise = ShapeFileReader.getAllFeatures(this.shapeFile);
		final ActivityFacilitiesFactory factory = ((ScenarioImpl)sc).getActivityFacilities().getFactory();
		for (SimpleFeature landkreis : landkreise) {
			Integer gemeindeschluessel = Integer.parseInt((String) landkreis.getAttribute("gemeindesc"));
			Geometry geo = (Geometry) landkreis.getDefaultGeometry();
			Point point = getRandomPointInFeature(new Random(), geo);
			Coordinate coordinate = point.getCoordinate();
			Double xcoordinate = coordinate.x;
			Double ycoordinate = coordinate.y;
			Coord coord = new CoordImpl(xcoordinate.toString(), ycoordinate.toString());
			ActivityFacility facility = factory.createActivityFacility(new IdImpl(gemeindeschluessel), coord) ;
			{
				ActivityOption option = factory.createActivityOption("work", facility ) ;
				option.setCapacity(1.) ;
				facility.addActivityOption(option) ;
			}
			{
				ActivityOption option = factory.createActivityOption("home", facility ) ;
				option.setCapacity(1.) ;
				facility.addActivityOption(option) ;
			}
			facilities.put(gemeindeschluessel, facility);
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
						Id quellZonenId = sc.createId(row[idx]); idx++ ;
						Id zielZonenId = sc.createId(row[idx]); idx++ ;

						Verkehrsmittel vm = Verkehrsmittel.values()[idx] ; idx++ ;
						Guetergruppe gg = Guetergruppe.values()[idx] ; idx++ ;
						idx++ ; // Seehafenhinterlandverkehr
						double tons = Double.parseDouble(row[idx]) ;
						
						process( quellZonenId, zielZonenId, vm, gg, tons ) ;

					} catch ( Exception ee ) {
						throw new RuntimeException(ee) ;
					}
				}
				else{
					System.err.println("ATTENTION: check filename!") ;
				}
			}

		});
	}

	private void process(Id quellZonenId, Id zielZonenId, Verkehrsmittel vm, Guetergruppe gg, double tons) {
		getCoordFromZone(quellZonenId);
	}

	private Coord getCoordFromZone(Id zoneId) {
		NodesAndDistances connectors = this.zoneToConnectorsMap.get(zoneId) ;
		Node node = null ;
		for ( Id nodeId : connectors.getNodesAndDistances().keySet() ) {
			node = sc.getNetwork().getNodes().get( nodeId ) ;
			break ;
		}
		return node.getCoord() ;
	}

	private void process(int quelle, int ziel, int workPt, int educationPt, int workCar, int educationCar) {
		Facility source = facilities.get(quelle);
		Facility sink = facilities.get(ziel);
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
			flowSink.process(facilities.get(quelle), facilities.get(ziel), scaledCarQuantity, TransportMode.car, "pvWork", 0.0);
		}
		if (scaledPtQuantity != 0){
			log.info(quelle + "->" + ziel + ": " + scaledPtQuantity + " pt trips");
			flowSink.process(facilities.get(quelle), facilities.get(ziel), scaledPtQuantity, TransportMode.pt, "pvWork", 0.0);
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
