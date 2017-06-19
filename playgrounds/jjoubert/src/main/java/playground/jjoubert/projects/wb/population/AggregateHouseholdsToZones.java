/* *********************************************************************** *
 * project: org.matsim.*
 * AggregatePopulationToZones.java
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

/**
 * 
 */
package playground.jjoubert.projects.wb.population;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import playground.jjoubert.projects.wb.freight.ConvertGridToPolygons;
import playground.southafrica.population.census2011.attributeConverters.CoordConverter;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.SouthAfricaInflationCorrector;

/**
 * This class parses the synthetic population and links them to a hexagonal
 * zone as created from...
 * 
 * @author jwjoubert
 */
public class AggregateHouseholdsToZones {
	final private static Logger LOG = Logger.getLogger(AggregateHouseholdsToZones.class);
	final private static String TREASURY_CRS = TransformationFactory.WGS84_SA_Albers;
	final private static GeometryFactory GF = new GeometryFactory();
	
	private static QuadTree<Geometry> cellQT;
	private static QuadTree<String> idQT;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AggregateHouseholdsToZones.class.toString(), args);
		run(args);
		Header.printFooter();
	}
	
	public static void run(String[] args){
		String households = args[0];
		String householdAttributes = args[1];
		String grid = args[2];
		String output = args[3];
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new HouseholdsReaderV10(sc.getHouseholds()).readFile(households);
		
		ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(
				sc.getHouseholds().getHouseholdAttributes());
		oar.putAttributeConverter(Coord.class, new CoordConverter());
		oar.readFile(householdAttributes);
		
		/* Parse the grid */
		parseQuadTreeFromGridFile(grid);
		
		/* Process each household. */
		LOG.info("Processing households...");
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
				TREASURY_CRS, TransformationFactory.WGS84);
		int ignoredHouseholds = 0;
		Counter counter = new Counter("  households # ");
		try{
			bw.write("hhId,hhLon,hhLat,hhX,hhY,zoneId,income,members");
			bw.newLine();
			
			for(Id<Household> hid : sc.getHouseholds().getHouseholds().keySet()){
				Household hh = sc.getHouseholds().getHouseholds().get(hid);
				int numberOfHouseholdMembers = hh.getMemberIds().size();
				double householdIncome = hh.getIncome().getIncome();
				double householdIncome2017 = SouthAfricaInflationCorrector.convert(householdIncome, 2014, 2017);
				
				/* Get and check the home coordinate. */
				Coord homeCoord = (Coord) sc.getHouseholds().getHouseholdAttributes().getAttribute(hid.toString(), "homeCoord");
				Coord homeCoordWgs = ct.transform(homeCoord);
				Point p = GF.createPoint(new Coordinate(homeCoordWgs.getX(), homeCoordWgs.getY()));
				Geometry cell = cellQT.getClosest(p.getX(), p.getY());
				if(cell.covers(p)){
					/* The household is in the cell. */
					String cellId = idQT.getClosest(p.getX(), p.getY());
					bw.write(String.format("%s,%.6f,%.6f,%.0f,%.0f,%s,%.0f,%d\n",
							hid.toString(),
							homeCoordWgs.getX(), homeCoordWgs.getY(),
							homeCoord.getX(), homeCoord.getY(),
							cellId, householdIncome2017, numberOfHouseholdMembers
							));
				} else{
					/* Ignore the household. */
					ignoredHouseholds++;
				}
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		counter.printCounter();
		LOG.info("Done processing. Total of " + ignoredHouseholds + " households ignored (without cell).");
	}
	
	/**
	 * Reads a grid file that was created using {@link ConvertGridToPolygons}
	 * and builds a {@link QuadTree} of cells, each cell being of type 
	 * {@link Geometry}. The coordinates of the points read should be of the
	 * unprojected WGS84 decimal degrees.
	 * 
	 * @param grid
	 * @return
	 */
	private static void parseQuadTreeFromGridFile(String grid){
		LOG.info("Parsing grid cells...");
		LOG.info("Calculate QuadTree extent...");
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		Counter counter = new Counter("  lines # ");
		BufferedReader br = IOUtils.getBufferedReader(grid);
		try{
			String line = br.readLine();
			while((line = br.readLine()) != null){
				String[] sa  = line.split(",");
				double x = Double.parseDouble(sa[2]);
				double y = Double.parseDouble(sa[3]);
				minX = Math.min(minX, x);
				maxX = Math.max(maxX, x);
				minY = Math.min(minY, y);
				maxY = Math.max(maxY, y);
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + grid);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + grid);
			}
		}
		counter.printCounter();
		
		cellQT = new QuadTree<Geometry>(minX, minY, maxX, maxY);
		idQT = new QuadTree<String>(minX, minY, maxX, maxY);
		LOG.info("Populating the QuadTree...");
		counter = new Counter("  grid cells # ");
		br = IOUtils.getBufferedReader(grid);
		try{
			String line = br.readLine();
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				String id = sa[0];
				Coordinate c1 = buildCoordinateFromLine(line);
				Coordinate c2 = buildCoordinateFromLine(br.readLine());
				Coordinate c3 = buildCoordinateFromLine(br.readLine());
				Coordinate c4 = buildCoordinateFromLine(br.readLine());
				Coordinate c5 = buildCoordinateFromLine(br.readLine());
				Coordinate c6 = buildCoordinateFromLine(br.readLine());
				Coordinate c7 = buildCoordinateFromLine(br.readLine());
				Coordinate[] ca = {c1, c2, c3, c4, c5, c6, c7};
				Geometry cell = GF.createPolygon(ca);
				cellQT.put(cell.getCentroid().getX(), cell.getCentroid().getY(), cell);
				idQT.put(cell.getCentroid().getX(), cell.getCentroid().getY(), id);
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + grid);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + grid);
			}
		}
		counter.printCounter();
	}
	
	private static Coordinate buildCoordinateFromLine(String line){
		String[] sa  = line.split(",");
		double x = Double.parseDouble(sa[2]);
		double y = Double.parseDouble(sa[3]);
		Coordinate c = new Coordinate(x, y);
		return c;
	}

}
