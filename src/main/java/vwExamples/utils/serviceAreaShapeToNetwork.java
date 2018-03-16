package vwExamples.utils;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.matsim.contrib.networkEditor.utils.GeometryTools;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;


public class serviceAreaShapeToNetwork {
	Set<String> zones = new HashSet<>();
	Map<String, Geometry> zoneMap = new HashMap<>();
	File networkFile = new File("D:\\Axer\\MatsimDataStore\\Berlin_DRT\\input\\network\\be_251.output_network.xml.gz");
	File shapeFile = new File("D:\\Axer\\MatsimDataStore\\Berlin_DRT\\input\\shapes\\Prognoseraum_EPSG_31468.shp");
	String networkfolder = null;
	String outputNetworkFile = null;
	static String drtTag = "drt";
	Network network = NetworkUtils.createNetwork();
	String shapeFeature = "SCHLUESSEL";
	String zoneList[] = {"0101"};
	double bufferRange = 500;


	public serviceAreaShapeToNetwork() { 
	this.networkfolder = networkFile.getParent();
	this.outputNetworkFile = networkfolder+"/../network/modifiedNetwork.xml.gz";
	readShape(this.shapeFile,this.shapeFeature);
	}
	
	//Main function creates the class and runs it!
	public static void main(String[] args) {
		//Run constructor and initialize shape file 
		serviceAreaShapeToNetwork serviceArea = new serviceAreaShapeToNetwork();
		serviceArea.assignServiceAreatoNetwork();
	}
	
	
	
	private void assignServiceAreatoNetwork() {
		//Load Network
		new MatsimNetworkReader(this.network).readFile(networkFile.toString());
				
		int i = 0;
		for (Link l : this.network.getLinks().values())
		{

			if (isServiceAreaLink(l,this.zoneList))
			{ 
				Set<String> modes = new HashSet<>();
				modes.addAll(l.getAllowedModes());
				modes.add(drtTag);
				l.setAllowedModes(modes);
				i++;
			}
		}
		System.out.println("Touched "+i+" Links within total network");
		new NetworkWriter(network).write(outputNetworkFile);
		
	}
	
	
	public void readShape(File shapeFile, String featureKeyInShapeFile) {
		Collection <SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile.toString());
		for (SimpleFeature feature : features) {
			String id =  feature.getAttribute(featureKeyInShapeFile).toString();
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			this.zones.add(id);
			this.zoneMap.put(id, geometry);
		}
	}
	
	
	private boolean isServiceAreaLink(Link l, String[] zoneList) {
		//Construct a LineSegment from link coordinates
		Coordinate start = new Coordinate(l.getFromNode().getCoord().getX(), l.getFromNode().getCoord().getY());
		Coordinate end = new Coordinate(l.getToNode().getCoord().getX(), l.getToNode().getCoord().getY()); 
		LineSegment lineSegment = new LineSegment(start, end);
		
		GeometryFactory f = new GeometryFactory();
		
		//1. Link needs to be in geographical area
		
		for (String z : zoneList)
		{
			//Get geometry for zone 
			Geometry zone = zoneMap.get(z);
			
			if (zone.buffer(this.bufferRange).intersects(lineSegment.toGeometry(f)))
			{
				//2. Link needs to be already available for car
				if (l.getAllowedModes().contains("car")) 
				{
					return true;
					
				} else return false;
				
			} else return false;
		}
		
		return false;
		
	}
	
	
	

	
}

