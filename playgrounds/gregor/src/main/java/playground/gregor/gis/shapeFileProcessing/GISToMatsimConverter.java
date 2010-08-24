/* *********************************************************************** *
 * project: org.matsim.*
 * ShapeMatsimConverter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.gis.shapeFileProcessing;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Converter class for street network shape files as provided by DLR in Last Mile project 
 * @author laemmel
 *
 */
public class GISToMatsimConverter {

	private static String polygonFile = null;
	private static String linestringFile = null;
	HashMap<String,FeatureSource> features = new  HashMap<String,FeatureSource>();
	
	FeatureSource polygons = null;
	FeatureSource linestrings = null;
	private Envelope envelope;
	private CoordinateReferenceSystem crs;
	static final double CATCH_RADIUS = 0.5;
	public static final double CAPACITY_COEF = 1.33;
	
	public GISToMatsimConverter(){
//		this("./padang/padang_streets.shp","./padang/vd10_streetnetwork_padang_v0.5_utm47s.shp");//
//		this("./padang/padang_streets.shp","./padang/converter/d_ls.shp");
//		this("./padang/converter/p_all.shp", "./padang/converter/d_ls.shp");
		this("./padang/converter/p_all.shp", "./padang/converter/ls_all.shp");
//		this("./padang/testcase1/simple/simpleIV.shp", "./padang/testcase1/simpleline/simplelineIV.shp" );
//		this("./padang/test4poly.shp", "./padang/test8line.shp");
//		this("./padang/testcase1/padang/padangTeilStreets_testII.shp", "./padang/testcase1/padang/padangTeil_testIV.shp");
	}
	
	public GISToMatsimConverter(final String polyFile, final String lineFile){
		polygonFile = polyFile;
		linestringFile = lineFile;
	}

	public void run(){
		try {
			readData();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		try {
			processData();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void processData() throws Exception {
		GraphGenerator gg = new GraphGenerator(features.get(linestringFile));
		Collection<Feature> graph =  gg.createGraph();
		ShapeFileWriter.writeGeometries(graph, "./padang/converter/d_ls.shp");
//		ShapeFileWriter.writeGeometries(graph, "./padang/testPadangLine.shp");
		
//		features.get(linestringFile);
		
//		PolygonGeneratorII polyGen = new PolygonGeneratorII(features.get(linestringFile) ,features.get(polygonFile));
		PolygonGeneratorII polyGen = new PolygonGeneratorII(graph,features.get(polygonFile));
		Collection<Feature> polyGraph = polyGen.generatePolygons();

//		ShapeFileWriter.writeGeometries(polyGraph, "./padang/testSimpleControlPoly2.shp");
//		ShapeFileWriter.writeGeometries(polyGraph, "./padang/testSimplePoly4.shp");
		ShapeFileWriter.writeGeometries(polyGraph, "./padang/converter/d_p.shp");

//		ShapeFileWriter.writeGeometries(polyGraph, "./padang/testDebugPadangPoints.shp");
//		NetworkGenerator ng = new NetworkGenerator(graph,this.envelope);
//		NetworkLayer network = ng.generateFromGraph();



//		network = new NetworkLayer();
//		NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(network);
//		nr.readFile("./networks/padang_net.xml");
//		NetworkWriter nw = new NetworkWriter(network,"./padang/debug_net.xml");
//		NetworkWriter nw = new NetworkWriter(network,"./padang/testcase2.xml");
		
//		nw.write();
//		NetworkToGraph ntg = new NetworkToGraph(network,this.crs);
//		Collection<Feature> netGraph = ntg.generateFromNet();
//		ShapeFileWriter.writeGeometries(netGraph, "./padang/testcase2_net.shp");

	}

	private void readData() throws Exception{
//		if (polygonFile != null){
			features.put(polygonFile, ShapeFileReader.readDataFile(polygonFile));			
			this.envelope = features.get(polygonFile).getBounds();
			this.crs = features.get(polygonFile).getSchema().getDefaultGeometry().getCoordinateSystem();
//		}
//		if (linestringFile != null){
			features.put(linestringFile, ShapeFileReader.readDataFile(linestringFile));			
//		}		
		
	}
	
	public static void main(String [] args){
		GISToMatsimConverter converter = new GISToMatsimConverter();
		converter.run();
	}
}
