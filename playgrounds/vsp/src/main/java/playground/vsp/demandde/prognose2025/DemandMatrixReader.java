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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.opengis.feature.simple.SimpleFeature;

import playground.vsp.demandde.pendlermatrix.TripFlowSink;
import playground.vsp.demandde.pendlermatrix.Zone;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;


public class DemandMatrixReader {

	private static final Logger log = Logger.getLogger(DemandMatrixReader.class);

	// PV Matrix enthält keine Richtungs-Info für den Pendlerverkehr --> Pendlerstatistik
//	private static final String PV_MATRIX = "../../shared-svn/studies/countries/de/prognose_2025/orig/pv-matrizen/2004_nuts_102r6x6.csv";

	private static final String GV_MATRIX = "../../shared-svn/studies/countries/de/prognose_2025/orig/gv-matrizen/gv-matrix-2004.csv";

	private Map<Integer, Zone> zones = new HashMap<Integer, Zone>();

	private TripFlowSink flowSink;

	private String shapeFile;

	public DemandMatrixReader(String shapeFile) {
		this.shapeFile = shapeFile;
	}

	public void run() {
		//		readNodes();
		readShape();
//		readMatrix(PV_MATRIX);
		readMatrix(GV_MATRIX);
		flowSink.complete();
	}

	private void readShape() {
		Collection<SimpleFeature> landkreise = ShapeFileReader.getAllFeatures(this.shapeFile);
		for (SimpleFeature landkreis : landkreise) {
			Integer gemeindeschluessel = Integer.parseInt((String) landkreis.getAttribute("gemeindesc"));
			Geometry geo = (Geometry) landkreis.getDefaultGeometry();
			Point point = getRandomPointInFeature(new Random(), geo);
			Coordinate coordinate = point.getCoordinate();
			Double xcoordinate = coordinate.x;
			Double ycoordinate = coordinate.y;
			Coord coord = new CoordImpl(xcoordinate.toString(), ycoordinate.toString());
			Zone zone = new Zone(gemeindeschluessel, 1, 1, coord);
			zones.put(gemeindeschluessel, zone);
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
				Integer quelle = null ;
				Integer ziel = 0;
				// car market share for commuter work/education trips (taken from "Regionaler Nahverkehrsplan-Fortschreibung, MVV 2007)
				double carMarketShare = 0.67;
				// scale factor, since Pendlermatrix only considers "sozialversicherungspflichtige Arbeitnehmer" (taken from GuthEtAl2005)
				double scaleFactor = 1.29;

				if (filename.equals(GV_MATRIX)){
					try {
						int idx = 0 ;
						quelle = Integer.parseInt(row[idx]); idx++ ;
						ziel = Integer.parseInt(row[idx]); idx++ ;
						
						Verkehrsmittel vm = Verkehrsmittel.values()[idx] ; idx++ ;
						Guetergruppe gg = Guetergruppe.values()[idx] ; idx++ ;
						idx++ ; // Seehafenhinterlandverkehr
						double tons = Double.parseDouble(row[idx]) ;
						
					} catch ( Exception ee ) {
						System.err.println("we are trying to read quelle: " + quelle ) ;
						System.exit(-1) ;
					}
				}
				else{
					System.err.println("ATTENTION: check filename!") ;
				}
			}

		});
	}

	private void process(int quelle, int ziel, int workPt, int educationPt, int workCar, int educationCar) {
		Zone source = zones.get(quelle);
		Zone sink = zones.get(ziel);
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
			flowSink.process(zones.get(quelle), zones.get(ziel), scaledCarQuantity, TransportMode.car, "pvWork", 0.0);
		}
		if (scaledPtQuantity != 0){
			log.info(quelle + "->" + ziel + ": " + scaledPtQuantity + " pt trips");
			flowSink.process(zones.get(quelle), zones.get(ziel), scaledPtQuantity, TransportMode.pt, "pvWork", 0.0);
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
