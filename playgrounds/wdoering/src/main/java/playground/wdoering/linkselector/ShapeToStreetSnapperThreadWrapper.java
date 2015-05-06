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

package playground.wdoering.linkselector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.geotools.referencing.CRS;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.evacuation.control.algorithms.PolygonalCircleApproximation;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactory;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.misc.Time;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;

import playground.wdoering.debugvisualization.model.DataPoint;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Polygon;

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
	private HashMap<Integer, DataPoint> networkNodes;
	private Map<Id<org.matsim.api.core.v01.network.Link>, ? extends org.matsim.api.core.v01.network.Link>  networkLinks;

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
		
		GeotoolsTransformation ct = new GeotoolsTransformation("EPSG:4326", c.global().getCoordinateSystem());
		OsmNetworkReader reader = new OsmNetworkReader(this.sc.getNetwork(), ct, true);
		reader.setKeepPaths(true);
		reader.parse(this.net);
		
		Envelope e = new Envelope();
		
		for (Node node : this.sc.getNetwork().getNodes().values())
		{
			e.expandToInclude(MGC.coord2Coordinate(node.getCoord()));
		}
		
		//TODO / FIXME
//		NetworkImpl netw = (NetworkImpl) this.sc.getNetwork();
//		netw.getNearestLink(coord)
//		Link l = null;
//		QuadTree<Link> qtree = new QuadTree<Link>(e.getMinX(),1,1,1);
//		qtree.put(1, 1, l);
//		qtree.put(2, 2, l);
		
		//qtree.get^
		
		Coord centerC = new CoordImpl((e.getMaxX()+e.getMinX())/2, (e.getMaxY()+e.getMinY())/2);
		CoordinateTransformation ct2 =  new GeotoolsTransformation(c.global().getCoordinateSystem(),"EPSG:4326");
		centerC = ct2.transform(centerC);
		this.center = new GeoPosition(centerC.getY(),centerC.getX());
		
		this.snapper = new ShapeToStreetSnapper(this.sc);
	}

	@Override
	public void run()
	{
		CoordinateReferenceSystem sourceCRS = MGC.getCRS("EPSG:4326");
//		CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:3395");
		CoordinateReferenceSystem targetCRS = MGC.getCRS(this.targetS);
		MathTransform transform = null;
		try {
			transform = CRS.findMathTransform(sourceCRS, targetCRS,true);
		} catch (FactoryException e) {
			throw new RuntimeException(e);
		}
		Coordinate c0 = new Coordinate(this.c0.getLongitude(),this.c0.getLatitude());
		Coordinate c1 = new Coordinate(this.c1.getLongitude(),this.c1.getLatitude());
		PolygonalCircleApproximation.transform(c0,transform);
		PolygonalCircleApproximation.transform(c1,transform);
		
		Polygon p = PolygonalCircleApproximation.getPolygonFromGeoCoords(c0, c1);
		
		p = this.snapper.run(p);
		
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
	
	public synchronized void saveRoadClosures(String fileName, HashMap<Id, String> roadClosures)
	{
		if (roadClosures.size()>0)
		{
			
			//check if network links are set
			if (networkLinks==null)
				networkLinks = sc.getNetwork().getLinks();

			//create change event
			Collection<NetworkChangeEvent> evs = new ArrayList<NetworkChangeEvent>();
			NetworkChangeEventFactory fac = new NetworkChangeEventFactoryImpl();
			
			Iterator it = roadClosures.entrySet().iterator();
		    while (it.hasNext())
		    {
		        Map.Entry pairs = (Map.Entry)it.next();
		        
		        Id currentId = (Id)pairs.getKey();
		        String timeString = (String)pairs.getValue();
		        
		        double time = Time.parseTime(timeString);
		        NetworkChangeEvent ev = fac.createNetworkChangeEvent(time);
		        ev.setFreespeedChange(new ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 0));
		        
		        ev.addLink(networkLinks.get(currentId));
		        evs.add(ev);
		        
		    }
		    
		    
		    NetworkChangeEventsWriter writer = new NetworkChangeEventsWriter();
		    writer.write(fileName + ".xml", evs);		
		}
		
		
	}
	
//	public synchronized void savePolygon(String dest) {
//		if (!dest.endsWith("shp")) {
//			dest = dest +".shp";
//		}
//		
//		CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:4326");
//		AttributeType p = DefaultAttributeTypeFactory.newAttributeType(
//				"MultiPolygon", MultiPolygon.class, true, null, null, targetCRS);
//		AttributeType t = AttributeTypeFactory.newAttributeType(
//				"name", String.class);
//		try {
//			FeatureType ft = FeatureTypeFactory.newFeatureType(new AttributeType[] { p, t }, "EvacuationArea");
//			MultiPolygon mp = new GeometryFactory(new PrecisionModel(2)).createMultiPolygon(new Polygon[]{this.p});
//			Feature f = ft.create(new Object[]{mp,"EvacuationArea"});
//			Collection<Feature> fts = new ArrayList<Feature>();
//			fts.add(f);
//			ShapeFileWriter.writeGeometries(fts, dest);
//		} catch (FactoryRegistryException e) {
//			e.printStackTrace();
//		} catch (SchemaException e) {
//			e.printStackTrace();
//		} catch (IllegalAttributeException e) {
//			e.printStackTrace();
//		}
//	}
	
	public Collection<Node> getNearestNodes(Coord coord)
	{
		Collection<Node> nodes = ((NetworkImpl)sc.getNetwork()).getNearestNodes(coord, 1);
		return nodes;
	}
	
	

	public HashMap<Id[], Coord[]> getNetworkLinks()
	{
		
		if (this.sc!=null)
		{
			GeotoolsTransformation ct = new GeotoolsTransformation(sc.getConfig().global().getCoordinateSystem(),"EPSG:4326");
			HashMap<Id[], Coord[]> links = new HashMap<Id[], Coord[]>();
			
//			Map<Id, ? extends org.matsim.api.core.v01.network.Node> networkNodes = sc.getNetwork().getNodes();
			networkLinks = sc.getNetwork().getLinks();
			
//			sc.getNetwork().getNodes();
			
//			Coord coord = null;
//			LinkImpl x = ((NetworkImpl)sc.getNetwork()).getNearestLink(coord);
			
			for (Link link: networkLinks.values())
			{
				Coord[] fromToCoord =  {ct.transform(link.getFromNode().getCoord()), ct.transform(link.getToNode().getCoord())};
				
				Id fromID = link.getFromNode().getId();
				Id toID = link.getToNode().getId();
				
				Id[] ids = {fromID , toID, link.getId()};
				
				links.put(ids, fromToCoord);
				
			}

			return links;
			
		}
		return null;
	}

	
}
