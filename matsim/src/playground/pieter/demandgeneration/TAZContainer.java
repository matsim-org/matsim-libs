package playground.pieter.demandgeneration;
/**code by Gregor Laemmel, Pieter Fourie. Class to find home TAZ from TAZ shapefile and point shapefile of home locs */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;

import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class TAZContainer {
	//TAZ object with all relevant methods to find if point is in a TAZ

	private Collection<Feature> polygons;
	private HashMap<String,MultiPolygon> polygonHashMap;
	private HashMap<String,Polygon> envelopeHashMap;
	private String TAZShapeFile;
	private GeometryFactory geofac;

	public TAZContainer(String shapeFile) {
		this.TAZShapeFile = shapeFile;
		FeatureSource featSrc = null;
		try {
			featSrc = ShapeFileReader.readDataFile(shapeFile);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		this.polygons = SAInitialDemandGenerator.getFeatures(featSrc);
		this.polygonHashMap = new HashMap<String, MultiPolygon>();
		this.envelopeHashMap = new HashMap<String, Polygon>();
		this.geofac = new GeometryFactory();
		indexPolygons();
	}

	private void indexPolygons() {
		//goes through the collection of polygons,
		for (Feature ft : this.polygons){
			Geometry geo = ft.getDefaultGeometry();
			//converts geometry to Multipolygon, if not already
			MultiPolygon multiPoly = null;
			if ( geo instanceof MultiPolygon ) {
				multiPoly = (MultiPolygon) geo;
			} else if (geo instanceof Polygon ) {
				multiPoly = this.geofac.createMultiPolygon(new Polygon[] {(Polygon) geo});
			} else {
				throw new RuntimeException("Feature does not contain a polygon/multipolygon!");
			}
//			String ID = ((Long)ft.getAttribute(1)).toString();
			String ID = String.valueOf(ft.getAttribute(1));
			this.polygonHashMap.put(ID, multiPoly);
			this.envelopeHashMap.put(ID,(Polygon)multiPoly.getEnvelope());
		}
	}

	 String findContainerID(Point point){
		//returns the container Multipolygon ID
		Collection<String> possibleCandidates = new ArrayList<String>();
		//first find possible candidates from envelopes
		Iterator hashMapIterator = this.envelopeHashMap.keySet().iterator();
		while (hashMapIterator.hasNext()){
			String possibleID = (String)hashMapIterator.next();
			Polygon possibleEnvelope = this.envelopeHashMap.get(possibleID);
			if(possibleEnvelope.contains(point))
				possibleCandidates.add(possibleID);
		}
		//check if there are any candidates, else the point isn't in any polygon
		if(possibleCandidates.isEmpty()){
			System.out.println("Point not in any polygon");
			return "3099";
		}
		//got list of candidates, now check their geometries
		for(String ID : possibleCandidates){
			if(this.polygonHashMap.get(ID).contains(point)){
				return ID;
			}
		}
		//if the method hasn't exited already, the point isn't in any polygon
		System.out.println("Point not in any polygon");
		return "3099";
	}

	public HashMap<String, MultiPolygon> getTAZHAshMap() {
		return this.polygonHashMap;
	}


}
