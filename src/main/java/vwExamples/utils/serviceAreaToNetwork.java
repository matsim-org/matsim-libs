package vwExamples.utils;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.contrib.networkEditor.*;
import org.matsim.contrib.networkEditor.utils.GeometryTools;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;

import peoplemover.preparation.ExtractPeopleMoverNetwork;

public class serviceAreaToNetwork {
	
	//This class assigns a new mode to network links that are within a certain geographical area and are already used by car 
	//Rectangle for Braunschweig
	//Bottom right
//	private final static double easternLimit =  10.639120;
//	private final static double southernLimit =  52.191426;
//	
//	//Top left
//	private final static double westernLimit =  10.395657;
//	private final static double northernLimit =  52.355276;

	//Rectangle for Braunschweig
	
	
//Rectangle for Wolfsburg
private final static double easternLimit =  10.903587;
private final static double southernLimit =  52.316199;

private final static double westernLimit =  10.641626;
private final static double northernLimit =  52.497724;	
//Rectangle for Wolfsburg
	
	
	
	static String drtTag = "drt";

	//Initialize network object
	private Network network = NetworkUtils.createNetwork();
	//Set source for the initial network file
	
	static File inputNetworkFile = new File("D:\\Axer\\MatsimDataStore\\WOB_BS_DRT\\WOB\\input\\network\\vw219.output_network.xml.gz");
	static String networkfolder = inputNetworkFile.getParent();
	
	String outputNetworkFile = networkfolder+"/../network/network_area_wob_withDRT_links.xml.gz";
	
	//Initialize corner coordinates for service area definition
	private Coord topLeft;
	private Coord bottomRight;
	
	//Function to transform WGS84 Coordinates to EPSG:25832
	private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");
	
	//Main function creates the class and runs it!
	public static void main(String[] args) {
		new serviceAreaToNetwork().run();
	}
	
	
	
	private void run() {
		topLeft = ct.transform(new Coord(westernLimit,northernLimit));
		bottomRight = ct.transform(new Coord(easternLimit,southernLimit));
		new MatsimNetworkReader(network).readFile(inputNetworkFile.toString());
		
		Coordinate p1 = GeometryTools.MATSimCoordToCoordinate(topLeft);
		Coordinate p2 = GeometryTools.MATSimCoordToCoordinate(bottomRight);
		
		int i = 0;
		for (Link l : network.getLinks().values())
		{
			

			
			
			if (isServiceAreaLink(l,p1,p2))
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



//	private boolean somePartOfLinkIsInBox(Link l) {
//		if (coordIsInBox(l.getFromNode().getCoord())&&coordIsInBox(l.getToNode().getCoord())&&coordIsInBox(l.getCoord())) return true;
//		else 
//			return false;
//	}

//	private boolean coordIsInBox(Coord c) {
//		if ((c.getX()<=bottomRight.getX()&&c.getX()>=topLeft.getX())&&(c.getY()>=bottomRight.getY()&&c.getY()<=topLeft.getY()) )
//			return true;
//		
//		else return false;
//	}
	
	private boolean isServiceAreaLink(Link l, Coordinate p1, Coordinate p2) {
		Coordinate start = new Coordinate(l.getFromNode().getCoord().getX(), l.getFromNode().getCoord().getY());
		Coordinate end = new Coordinate(l.getToNode().getCoord().getX(), l.getToNode().getCoord().getY()); 
		LineSegment lineSegment = new LineSegment(start, end);
		
		GeometryFactory f = new GeometryFactory();
		
		//1. Link needs to be in geographical area

		
		
		if ( GeometryTools.getRectangle(p1, p2).getEnvelope().intersects(lineSegment.toGeometry(f)))
		{
			//2. Link needs to be already available for car
			if (l.getAllowedModes().contains("car")) 
			{
				return true;
				
			} else return false;
			
		} else return false;
		
		
	}
	
	
	

	
}

