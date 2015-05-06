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

package playground.wdoering.grips.areaselector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.evacuation.control.algorithms.PolygonalCircleApproximation;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;

import playground.wdoering.grips.areaselector.MyMapViewer.SelectionMode;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

public class ShapeToStreetSnapperThreadWrapper implements Runnable {

	private Polygon p;
	private GeoPosition c0;
	private GeoPosition c1;
	private String targetS;
	private ShapeToStreetSnapper snapper;
	private Scenario sc;
	private GeoPosition center;
	private MyMapViewer mapViewer;
	private final String net;
	private final EvacuationAreaSelector evacuationAreaSelector;
	private List<GeoPosition> geoPolygon;
	private SelectionMode selectionMode = SelectionMode.POLYGON;

	public ShapeToStreetSnapperThreadWrapper(String osm, EvacuationAreaSelector evacuationAreaSelector) {
		this.net = osm;
		
		//TODO HACK to enable saveBtn - class should fire action events instead (see AbstractButton.java) 
		this.evacuationAreaSelector = evacuationAreaSelector;
		
		
		init();
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
		
		CoordinateReferenceSystem sourceCRS = MGC.getCRS("EPSG:4326");
		//		CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:3395");
		CoordinateReferenceSystem targetCRS = MGC.getCRS(this.targetS);
		MathTransform transform = null;
		try {
			transform = CRS.findMathTransform(sourceCRS, targetCRS,true);
		} catch (FactoryException e) {
			throw new RuntimeException(e);
		}
		
		if (selectionMode.equals(SelectionMode.CIRCLE))
		{
			
			Coordinate c0 = new Coordinate(this.c0.getLongitude(),this.c0.getLatitude());
			Coordinate c1 = new Coordinate(this.c1.getLongitude(),this.c1.getLatitude());
			PolygonalCircleApproximation.transform(c0,transform);
			PolygonalCircleApproximation.transform(c1,transform);
			
			p = PolygonalCircleApproximation.getPolygonFromGeoCoords(c0, c1);
			
			//snapper is deactivated for now!
			p = this.snapper.run(p);
			
			
		}
		else if (selectionMode.equals(SelectionMode.POLYGON))
		{
			
			GeometryFactory geofac = new GeometryFactory();
			
			
			//transform to coords
			Coordinate [] coords = new Coordinate[geoPolygon.size()+1];
			for (int i = 0; i < geoPolygon.size(); i++)
			{
				coords[i] = new Coordinate(geoPolygon.get(i).getLongitude(),geoPolygon.get(i).getLatitude());
				PolygonalCircleApproximation.transform(coords[i],transform);
			}			
			
			coords[geoPolygon.size()] = new Coordinate(coords[0]);
			
			LinearRing ls = geofac.createLinearRing(coords);
			p = geofac.createPolygon(ls,null);
			
		}
		
		try {
			p = (Polygon) PolygonalCircleApproximation.transform(p, transform.inverse());
		} catch (NoninvertibleTransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		this.setPolygon(p);
		this.mapViewer.repaint();
		this.evacuationAreaSelector.setSaveButtonEnabled(true);
		
	}

	
	private synchronized void setPolygon(Polygon p) {
		this.p = p;
	}
	
	/**
	 * sets the geoposition polygon
	 * @param polygon
	 */
	public synchronized void setGeoPolygon(List<GeoPosition> polygon)
	{
		this.geoPolygon = polygon;
	}
	
	public synchronized Polygon getPolygon() {
		return this.p;
	}
	
	public synchronized void setCoordinates(GeoPosition c0, GeoPosition c1) {
		this.evacuationAreaSelector.setSaveButtonEnabled(false);
		this.p = null;
		this.c0 = c0;
		this.c1 = c1;
		
	}
	
	public synchronized void reset() {
		this.evacuationAreaSelector.setSaveButtonEnabled(false);
		this.p = null;
	}

	public GeoPosition getNetworkCenter() {
		return this.center;
	}

	public void setView(MyMapViewer myMapViewer) {
		this.mapViewer = myMapViewer;
		
	}
	
	public synchronized void savePolygon(String dest) {
		if (!dest.endsWith("shp")) {
			dest = dest +".shp";
		}
		
		CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:4326");

		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(targetCRS);
		b.setName("EvacuationArea");
		b.add("location", MultiPolygon.class);
		b.add("name", String.class);
		SimpleFeatureBuilder factory = new SimpleFeatureBuilder(b.buildFeatureType());
		
		try {
			MultiPolygon mp = new GeometryFactory(new PrecisionModel(2)).createMultiPolygon(new Polygon[]{this.p});
			SimpleFeature f = factory.buildFeature(null, new Object[]{mp, "EvacuationArea"});
			Collection<SimpleFeature> fts = new ArrayList<SimpleFeature>();
			fts.add(f);
			ShapeFileWriter.writeGeometries(fts, dest);
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	public synchronized void setSelectionMode(SelectionMode selectionMode)
	{
		this.selectionMode = selectionMode;
	}
}
