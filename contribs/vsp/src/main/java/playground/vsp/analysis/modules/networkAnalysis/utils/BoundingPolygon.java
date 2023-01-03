package playground.vsp.analysis.modules.networkAnalysis.utils;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

/**
 * This class creates a bounding polygon around a given network.
 * The method starts with an initial bounding box. Coordinates are placed in a certain
 * step size on the edge of the box. Then, the nearest node of each coordinate is searched
 * to get the corners of the resulting polygon.
 * 
 * @author dhosse
 *
 */

public class BoundingPolygon {
	
	private Network network;
	private double stepSize;
	
	private List<Coordinate> coordinates;
	
	/**
	 * 
	 * Creates a new bounding polygon around a given network file.
	 * 
	 * @param network The network you want to bound.
	 * @param stepSize The distance between the initial coordinates on the edge of the bounding box.
	 */
	public BoundingPolygon(final Network network,double stepSize){
		
		this.network = network;
		this.stepSize = stepSize;
		
		this.createPolygon();
		
	}
	
	/**
	 * Return the created BoundingPolygon as geometry.
	 * 
	 * @return <code>com.vividsolutions.jts.geom.Polygon</code>.
	 */
	public Geometry returnPolygon(){
		
		Coordinate[] ring = new Coordinate[this.coordinates.size()];
		for(int i = 0;i<ring.length;i++)
			ring[i] = this.coordinates.get(i);
		
		LinearRing shell = new GeometryFactory().createLinearRing(ring);
		Polygon polygon = new GeometryFactory().createPolygon(shell, null);
		
		return polygon;
		
	}
	
	/**
	 * Creates the bounding polygon for the given network.
	 */
	private void createPolygon(){
		
		if(this.network != null && this.stepSize > 0){

			//that list stores all the nodes that are corners of the bounding polygon 
			List<Node> outerNodes = new ArrayList<Node>();
		
			//creates an initial bounding box around the network
			double[] corners = createInitialBoundingBox();
			
			//sets coordinates on the edge of the bounding box
			this.coordinates = createInitialCoordinates(corners);
			
			List<Coordinate> coords = new ArrayList<Coordinate>();
			
			for(Coordinate coord : coordinates){
			
				//search the nearest node from the current coordinate
				Node node = NetworkUtils.getNearestNode(((Network)this.network),MGC.coordinate2Coord(coord));
				
				//if the nearest node is not yet a corner of the bounding polygon, add it to the collection
				//and add the node's coordinate to the collection of the corners
				if(!outerNodes.contains(node)){
					outerNodes.add(node);
					coords.add(new Coordinate(MGC.coord2Coordinate(node.getCoord())));
				}
			
			}

			//replace the coordinates on the edge of the bounding box by the
			//coordinates of the corners nodes
			this.coordinates = coords;
			
			//add the first coordinate at the end of the collection to close the envelope
			this.coordinates.add(this.coordinates.get(0));
		
		}
	}
	
	/**
	 * 
	 * @param corners The coordinates of the corners of the bounding box.
	 * @return Coordinates on the edge of the bounding box.
	 */
	private List<Coordinate> createInitialCoordinates(double[] corners) {
		
		List<Coordinate> coords = new ArrayList<Coordinate>();
		
		//create coordinates at lower edge of the bbox
		for( double x = corners[0] ; x <= corners[2] ; x += this.stepSize ){
			double y=corners[1];
			coords.add(new Coordinate(x, y));
		}
		
		//create coordinates at right edge of the bbox
		for( double y = corners[1] ; y <= corners[3] ; y += this.stepSize ){
			double x = corners[2];
				coords.add(new Coordinate(x, y));
		}
		
		//create coordinates at upper edge of the bbox
		for( double x = corners[2] ; x >= corners[0] ; x -= this.stepSize ){
			double y=corners[3];
			coords.add(new Coordinate(x, y));
		}
		
		//create coordinates at left edge of the bbox
		for( double y = corners[3] ; y >= corners[1] ; y -= this.stepSize ){
			double x = corners[0];
				coords.add(new Coordinate(x, y));
		}
		
		return coords;
		
	}

	private double[] createInitialBoundingBox(){
		return NetworkUtils.getBoundingBox(this.network.getNodes().values());
	}
	
	public double getStepSize(){
		return this.stepSize;
	}

}
