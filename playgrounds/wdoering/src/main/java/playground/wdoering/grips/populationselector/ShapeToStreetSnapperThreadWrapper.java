/* *********************************************************************** *
 * project: org.matsim.*
 * ShapeToStreetSnapperThreadWrapper.java
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

package playground.wdoering.grips.populationselector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.table.DefaultTableModel;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.geotools.referencing.CRS;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.grips.algorithms.PolygonalCircleApproximation;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

public class ShapeToStreetSnapperThreadWrapper implements Runnable {

	private Polygon p;
	private Polygon areaPolygon;
	
	private HashMap<Integer, Polygon> polygons;

	
	private GeoPosition c0;
	private GeoPosition c1;
	private String targetS;
	private ShapeToStreetSnapper snapper;
	private Scenario sc;
	private GeoPosition center;
	private MyMapViewer mapViewer;
	private final String net;
	private final PopulationAreaSelector populationAreaSelector;
	private MathTransform transform;
	private int currentPolyIndex;
	

	public ShapeToStreetSnapperThreadWrapper(String osm, PopulationAreaSelector evacuationAreaSelector) {
		this.net = osm;
		
		//TODO HACK to enable saveBtn - class should fire action events instead (see AbstractButton.java) 
		this.populationAreaSelector = evacuationAreaSelector;
		
		init();
	}
	
	public ShapeToStreetSnapperThreadWrapper(String osm, PopulationAreaSelector evacuationAreaSelector, String ShapeFileString) {
		readShapeFile(ShapeFileString);
		this.net = osm;
		this.populationAreaSelector = evacuationAreaSelector;
		init();
	}
	
	public synchronized HashMap<Integer, Polygon> getPolygons()
	{
		return polygons;
	}
	
	public void readShapeFile(String shapeFileString)
	{
			ShapeFileReader shapeFileReader = new ShapeFileReader();
			shapeFileReader.readFileAndInitialize(shapeFileString);
	
//			shapeFileReader.g
			
			ArrayList<Geometry> geometries = new ArrayList<Geometry>();
			for (Feature ft : shapeFileReader.getFeatureSet())
			{
				Geometry geo = ft.getDefaultGeometry();
				//System.out.println(ft.getFeatureType());
				geometries.add(geo);
			}
			
			int j = 0;			
			Coordinate [] coords = geometries.get(0).getCoordinates();
			coords[coords.length-1] = coords[0];
			
			GeometryFactory geofac = new GeometryFactory();
			
			LinearRing shell = geofac.createLinearRing(coords);
			areaPolygon = geofac.createPolygon(shell, null);		
			
	}

	
	private void init() {
//		String net = "/Users/laemmel/svn/shared-svn/studies/countries/de/hh/hafen_fest_evacuation/GDIToMATSimData/map.osm";
		
		Config c = ConfigUtils.createConfig();
		c.global().setCoordinateSystem("EPSG:3395");
		
		this.targetS = c.global().getCoordinateSystem();
		this.sc = ScenarioUtils.createScenario(c);
		
		CoordinateTransformation ct =  new GeotoolsTransformation("EPSG:4326", c.global().getCoordinateSystem());
		OsmNetworkReader reader = new OsmNetworkReader(this.sc.getNetwork(), ct, true);
		reader.setKeepPaths(true);
		reader.parse(this.net);
		
		Envelope e = new Envelope();
		for (Node node : this.sc.getNetwork().getNodes().values()) {
			e.expandToInclude(MGC.coord2Coordinate(node.getCoord()));
		}
		Coord centerC = new CoordImpl((e.getMaxX()+e.getMinX())/2, (e.getMaxY()+e.getMinY())/2);
		CoordinateTransformation ct2 =  new GeotoolsTransformation(c.global().getCoordinateSystem(),"EPSG:4326");
		centerC = ct2.transform(centerC);
		this.center = new GeoPosition(centerC.getY(),centerC.getX());
		
		this.snapper = new ShapeToStreetSnapper(this.sc);
		
		
	}

	@Override
	public void run() {
		
		if (transform==null)
			getTransform();
		
		if (polygons==null)
			polygons = new HashMap<Integer, Polygon>();
		
		Coordinate c0 = new Coordinate(this.c0.getLongitude(),this.c0.getLatitude());
		Coordinate c1 = new Coordinate(this.c1.getLongitude(),this.c1.getLatitude());
		PolygonalCircleApproximation.transform(c0,transform);
		PolygonalCircleApproximation.transform(c1,transform);
		
		Polygon p = PolygonalCircleApproximation.getPolygonFromGeoCoords(c0, c1);
		
		p = this.snapper.run(p);
		
		if ((p!=null) && (!p.isEmpty()))
		{
			
			try {
				p = (Polygon) PolygonalCircleApproximation.transform(p, transform.inverse());
			} catch (NoninvertibleTransformException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			this.polygons.put(currentPolyIndex, p);
			this.populationAreaSelector.addNewArea(this.currentPolyIndex);

		}
		
//		if (lassoPolygon!=null)
//		{
//			this.setPolygon(p);
//		}
		
		this.mapViewer.repaint();
		this.populationAreaSelector.setSaveButtonEnabled(true);
		
	}

	private void getTransform()
	{
		CoordinateReferenceSystem sourceCRS = MGC.getCRS("EPSG:4326");
//		CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:3395");
		CoordinateReferenceSystem targetCRS = MGC.getCRS(this.targetS);
		transform = null;
		try {
			transform = CRS.findMathTransform(sourceCRS, targetCRS,true);
		} catch (FactoryException e) {
			throw new RuntimeException(e);
		}
	}

	public Polygon getAreaPolygon()
	{
		return areaPolygon;
	}
	
	private synchronized void setPolygon(Polygon p) {
		this.p = p;
	}
	
	public synchronized Polygon getPolygon() {
		return this.p;
	}
	
	public synchronized void setIndexAndCoordinates(int index, GeoPosition c0, GeoPosition c1) {
		this.currentPolyIndex = index;
		this.populationAreaSelector.setSaveButtonEnabled(false);
		this.p = null;
		this.c0 = c0;
		this.c1 = c1;
		
	}
	
	public synchronized void reset() {
		this.populationAreaSelector.setSaveButtonEnabled(false);
		this.p = null;
	}

	public GeoPosition getNetworkCenter() {
		return this.center;
	}

	public void setView(MyMapViewer myMapViewer) {
		this.mapViewer = myMapViewer;
		
	}
	
	public synchronized void savePolygon(String dest)
	{
		if (this.polygons.size()==0)
			return;
		
//		Polygon[] polygonArray = new Polygon[polygons.size()];
//		for (int i = 0; i < polygons.size(); i++)
//			polygonArray[i] = polygons.get(i);
		
		if (!dest.endsWith("shp")) {
			dest = dest +".shp";
		}
		
		CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:4326");
		AttributeType p = DefaultAttributeTypeFactory.newAttributeType(
				"MultiPolygon", MultiPolygon.class, true, null, null, targetCRS);
		AttributeType t = AttributeTypeFactory.newAttributeType(
				"persons", Long.class);
		try
		{
			
			Collection<Feature> fts = new ArrayList<Feature>();
//			for (int i = 0; i < polygons.size(); i++)
			
			
			for (Map.Entry<Integer, Polygon> entry : polygons.entrySet())
			{
			    int id = entry.getKey();
			    Polygon currentPolygon = entry.getValue();

				
				DefaultTableModel defModel = (DefaultTableModel)populationAreaSelector.getAreaTable().getModel();
				
				int pop = 23;
				for (int j = 0; j < defModel.getRowCount(); j++)
				{
					if ((Integer) populationAreaSelector.getAreaTable().getModel().getValueAt(j, 0) == id)
						pop = Integer.valueOf((String)populationAreaSelector.getAreaTable().getModel().getValueAt(j, 1));
				}
				
				FeatureType ft = FeatureTypeFactory.newFeatureType(new AttributeType[] { p, t }, "EvacuationArea");
				MultiPolygon mp = new GeometryFactory(new PrecisionModel(2)).createMultiPolygon(new Polygon[]{currentPolygon});
				Feature f = ft.create(new Object[]{mp,pop});
				fts.add(f);
			}
			
			
//			FeatureType ft = FeatureTypeFactory.newFeatureType(new AttributeType[] { p, t }, "EvacuationArea");
//			MultiPolygon mp = new GeometryFactory(new PrecisionModel(2)).createMultiPolygon(polygonArray);
//			Feature f = ft.create(new Object[]{mp,"EvacuationArea"});
			
			
			
			ShapeFileWriter.writeGeometries(fts, dest);
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}
	}

//	public void setLasso(ArrayList<GeoPosition> newLine)
//	{
//		System.out.println("elem:" + newLine.size());
//		if (newLine.size()<3)
//			return;
//		
//		int length = newLine.size();
//		int step = (int)(length*0.1);
//		
//		System.out.println("step:"+step);
//		
//		if (step<3)
//			step = 1;
//		
//		if (transform==null)
//			getTransform();
//		
//		GeometryFactory geofac = new GeometryFactory();
//		
//		Coordinate [] coords = new Coordinate[(length/step)+1];
//		
//		int idx = 0;
//		
//		for (int i = 0; i < newLine.size(); i+=step)
//		{
//			GeoPosition currentGeoPos = newLine.get(i);
//			Coordinate currentPoint = new Coordinate(currentGeoPos.getLongitude(),currentGeoPos.getLatitude());
//			PolygonalCircleApproximation.transform(currentPoint,transform);
//			coords[idx] = currentPoint; 
//			idx++;
//		}
//		
//		coords[coords.length-1] = coords[0];
//		
//		System.out.println("length of new polygon: " + coords.length + " | idx:" + idx);
//		
//		LinearRing ls = geofac.createLinearRing(coords);
//		lassoPolygon = geofac.createPolygon(ls,null);		
//		
////		Coordinate c = new Coordinate(tmpX+c0.x,tmpY+c0.y);
////		coords[idx++]=c;
////		}
////		coords[idx] = coords[0];
////		
//		
//	}
}
