package playground.dhosse.bachelorarbeit;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.geotools.feature.IllegalAttributeException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.utils.LeastCostPathTree;
import org.matsim.vehicles.Vehicle;
import org.opengis.feature.Feature;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
* @author dhosse
* 
*/

public class Grid {
	
	private static QuadTree<Cell> cells;
	
	private double minX = Double.POSITIVE_INFINITY;
	private double maxX = 0;
	private double minY = Double.POSITIVE_INFINITY;
	private double maxY = 0;
	
	protected Logger log = Logger.getLogger(Grid.class);
	
	private int index=0;
	
	private FeatureType featureType;
	private GeometryFactory gF = new GeometryFactory();
	
	/**
	 * Creates a grid for the comparison of two networks
	 * 
	 */
	
	public Grid(){
	}
	
	public void gridComparison(double dim,Network net1, Network net2){
		
		this.init(net1, net2, dim);
		
		log.info("start comparing");
		
		double step = dim / 2;
		
		traverseNetwork(net1,step,1);
		traverseNetwork(net2,step,2);
		
		log.info("done comparing");
		
	}
	
	public void traverseNetwork(Network net, double step, int it){
		for(Link link : net.getLinks().values()){
			
			double nextX = link.getFromNode().getCoord().getX();
			
			double nextY = link.getFromNode().getCoord().getY();
			
			while(nextX!=link.getToNode().getCoord().getX()&&nextY!=link.getToNode().getCoord().getY()){

				Cell cell = this.getCells().get(nextX, nextY);
				
				if(cell.wasTraversed()<it&&cell.contains(nextX, nextY)){
					if(cell.wasTraversed()==0){
						if(it==1){
							cell.setTraversed(1);
						}
						else{
							cell.setTraversed(2);
						}
					}
					else if(cell.wasTraversed()==1&&it==2){
						cell.setTraversed(3);
					}
				}
				else{
					double diffx = link.getToNode().getCoord().getX()-nextX;
					double diffy = link.getToNode().getCoord().getY()-nextY;
					double betrag = Math.sqrt(Math.pow(diffx, 2) + Math.pow(diffy, 2));
					double x = nextX;
					double y = nextY;
					nextX = x + step*(diffx/betrag);
					nextY = y + step*(diffy/betrag);
					if(Math.sqrt(Math.pow(nextX-link.getFromNode().getCoord().getX(), 2)+Math.pow(nextY-link.getFromNode().getCoord().getY(), 2))>
					Math.sqrt(Math.pow(link.getToNode().getCoord().getX()-link.getFromNode().getCoord().getX(), 2)+
							Math.pow(link.getToNode().getCoord().getY()-link.getFromNode().getCoord().getY(), 2))){
						break;
					}
				}
			}
		}
	}
	
//	public void generateSHPExport(String dname, String dname2){
//		
//		log.info("writing to shape file.");
//		initFeatureType();
//		Collection<Feature> features = createFeatures(this);
//		ShapeFileWriter.writeGeometries(features, "output/grid");
//		log.info("done writing shape file.");
//		int i=0;
//		Links2ESRIShape.main(new String[]{dname,"input/shp/networkline"+i+".shp","input/shp/networkpoly"+i+".shp", });
//		i++;
//		Links2ESRIShape.main(new String[]{dname2,"input/shp/networkline"+i+".shp","input/shp/networkpoly"+i+".shp", });
//		
//	}
	
//	private Collection<Feature> createFeatures(Grid grid){
//		Collection<Feature> features = new ArrayList<Feature>();
//		for(Cell cell : grid.getCells().values()){
//			Feature feature = getFeature(cell);
//			features.add(feature);
//		}
//		return features;
//	}
		
//	private void initFeatureType() {
//		AttributeType[] attribs = new AttributeType[] {
//		AttributeTypeFactory.newAttributeType("Polygon", Polygon.class),
//		AttributeTypeFactory.newAttributeType("ID", String.class),
//		AttributeTypeFactory.newAttributeType("area", Double.class),
//		AttributeTypeFactory.newAttributeType("traversed", String.class)
//		};
//		try {
//			this.featureType = FeatureTypeBuilder.newFeatureType(attribs, "link");
//			} catch (FactoryRegistryException e) {
//			e.printStackTrace();
//			} catch (SchemaException e) {
//			e.printStackTrace();
//			}
//	}
	
//	private Feature getFeature(final Cell cell){
//		
//		CoordinateList list = new CoordinateList();
//		list.add(new Coordinate(cell.getLeft(),cell.getBottom()),true);
//		list.add(new Coordinate(cell.getRight(),cell.getBottom()),true);
//		list.add(new Coordinate(cell.getRight(),cell.getTop()),true);
//		list.add(new Coordinate(cell.getLeft(),cell.getTop()),true);
//		list.add(new Coordinate(cell.getLeft(),cell.getBottom()),true);
//		
//		CoordinateArraySequence cAs = new CoordinateArraySequence(list.toCoordinateArray());
//
//		LinearRing shell = new LinearRing(cAs, new GeometryFactory());
//			
//		Object [] attribs = new Object []{
//				this.gF.createPolygon(shell,null),
//				Integer.toString(cell.getId()),
//				cell.getArea(),
//				Integer.toString(cell.wasTraversed())
//		};
//		try {
//			return this.featureType.create(attribs);
//			} catch (IllegalAttributeException e) {
//			throw new RuntimeException(e);
//			}			
//	}

	
	public QuadTree<Cell> getCells(){
		return Grid.cells;
	}
	
	public void calculateTravelTime(Network net){
//		
		LeastCostPathTree lctp = new LeastCostPathTree(new TravelTimeCalculator(net, new TravelTimeCalculatorConfigGroup()), new TravelDisutility() {
			
			@Override
			public double getLinkTravelDisutility(Link link, double time,
					Person person, Vehicle vehicle) {
				return 0;
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return 0;
			}
		}){

			@SuppressWarnings("unused")
			public double getLinkGeneralizedTravelCost(Link link, double time) {
				return 0;
			}
			
		};
		log.info("start calculating free speed travel time");
		for(Link link : net.getLinks().values()){
			lctp.calculate(net, link.getFromNode(), 0);
		}
		log.info("done calculating");
		for(Node node : net.getNodes().values()){
			System.out.println(lctp.getTree().get(node.getId()).getTime());
		}
	}
	
	/**
	 * Initializes the grid by filling it with cells
	 * @param net1
	 * @param net2
	 * @param dim The dimension (height, width) of a single grid cell
	 */
	public void init(Network net1,Network net2, double dim){
		
		for(Link link : net1.getLinks().values()){
			Coord fromCoord = link.getFromNode().getCoord();
			Coord toCoord = link.getToNode().getCoord();
			if(fromCoord.getX()<minX){
				if(toCoord.getX()<fromCoord.getX()){
					minX = toCoord.getX();
				}
				else{
					minX = fromCoord.getX();
				}
			}
			if(fromCoord.getX()>maxX){
				if(toCoord.getX()>fromCoord.getX()){
					maxX = toCoord.getX();
				}
				else{
					maxX = fromCoord.getX();
				}
			}
			if(fromCoord.getY()<minY){
				if(toCoord.getY()<fromCoord.getY()){
					minY = toCoord.getY();
				}
				else{
					minY = fromCoord.getY();
				}
			}
			if(fromCoord.getY()>maxY){
				if(toCoord.getY()>fromCoord.getY()){
					maxY = toCoord.getY();
				}
				else{
					maxY = fromCoord.getY();
				}
			}
		}
		for(Link link : net2.getLinks().values()){
			Coord fromCoord = link.getFromNode().getCoord();
			Coord toCoord = link.getToNode().getCoord();
			if(fromCoord.getX()<minX){
				if(toCoord.getX()<fromCoord.getX()){
					minX = toCoord.getX();
				}
				else{
					minX = fromCoord.getX();
				}
			}
			if(fromCoord.getX()>maxX){
				if(toCoord.getX()>fromCoord.getX()){
					maxX = toCoord.getX();
				}
				else{
					maxX = fromCoord.getX();
				}
			}
			if(fromCoord.getY()<minY){
				if(toCoord.getY()<fromCoord.getY()){
					minY = toCoord.getY();
				}
				else{
					minY = fromCoord.getY();
				}
			}
			if(fromCoord.getY()>maxY){
				if(toCoord.getY()>fromCoord.getY()){
					maxY = toCoord.getY();
				}
				else{
					maxY = fromCoord.getY();
				}
			}
		}
		
		System.out.println("Range: x: (" +  minX + "," + maxX + ")," +
				"y: (" + minY + "," + maxY + ")" );
		
		cells = new QuadTree<Cell>(minX, minY, maxX, maxY);
				
		double y = maxY;
		double x = minX;
		while(y>minY){
			while(x<maxX){
				double ybottom = y-dim;
				double xright = x+dim;
				double xleft= x;
				x=xright;
				Cell c = new Cell(xleft,xright,y,ybottom,index);
				this.index++;
				cells.put((c.getRight()+c.getLeft())/2, (c.getTop()+c.getBottom())/2, c);
			}
			x=minX;
			y-=dim;
		}
		
		System.out.println("created " + index + " cells");
	}
	
	static class Cell
	{
		private double xleft;
		private double xright;
		private double ytop;
		private double ybottom;
		private int traversed;
		
		private int id=0;
		
		public Cell(double left,double right,double top,double bottom,int id){
			this.xleft = left;
			this.xright = right;
			this.ytop = top;
			this.ybottom = bottom;
			this.id = id;
			this.traversed = 0;
		}
		
		public double getLeft(){
			return this.xleft;
		}
		
		public double getRight(){
			return this.xright;
		}
		
		public double getTop(){
			return this.ytop;
		}
		
		public double getBottom(){
			return this.ybottom;
		}
		
		public int getId(){
			return this.id;
		}
		
		public double getArea(){
			return((this.xright-this.xleft)*(this.ytop-this.ybottom));
		}
		
		public int wasTraversed(){
			return this.traversed;
		}
		
		public void setTraversed(int traversed){
			this.traversed = traversed;
		}
		
		public boolean contains(double x, double y){
			if(x<=this.getRight()&&
					x>=this.getLeft()&&
					y<=this.getTop()&&
					y>=this.getBottom()){
				return true;
			}
			else{
				return false;
			}
		}
	}
	
}
