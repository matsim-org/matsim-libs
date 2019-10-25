package playgroundMeng.ptAccessabilityAnalysis.areaSplit;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;

import com.google.inject.Inject;

import playgroundMeng.ptAccessabilityAnalysis.linksCategoryAnalysis.LinkExtendImp;
import playgroundMeng.ptAccessabilityAnalysis.run.GridShapeFileWriter;
import playgroundMeng.ptAccessabilityAnalysis.run.PtAccessabilityConfig;
import playgroundMeng.ptAccessabilityAnalysis.stopInfoCellector.RouteStopInfoCollector;
import playgroundMeng.ptAccessabilityAnalysis.stopInfoCellector.TransitStopFacilityExtendImp;

public class GridBasedSplit implements AreaSplit {
	private static final Logger logger = Logger.getLogger(GridBasedSplit.class);
	
	private Map<String, List<TransitStopFacilityExtendImp>> num2Stops = new HashedMap();
	private Map<String, List<LinkExtendImp>> num2Link = new HashedMap();
	private Map<String, Polygon> num2Polygon = new HashedMap();
	private Map<Id<Link>, LinkExtendImp> linkExtendImps = new HashedMap();
	
	Network network;
	Population population;
	RouteStopInfoCollector routeStopInfoCollector;
	PtAccessabilityConfig ptAccessabilityConfig;
	double xInterval;
	double yInterval;
	double minx; double maxx;
	double miny; double maxy;
	
	@Inject
	public GridBasedSplit(Network network,Population population,RouteStopInfoCollector routeStopInfoCollector,PtAccessabilityConfig ptAccessabilityConfig) {
		this.network = network;
		this.population = population;
		this.routeStopInfoCollector = routeStopInfoCollector;
		this.ptAccessabilityConfig = ptAccessabilityConfig;
		
		minx = this.ptAccessabilityConfig.getMinx();  miny = this.ptAccessabilityConfig.getMiny();
		maxx = this.ptAccessabilityConfig.getMaxx();  maxy = this.ptAccessabilityConfig.getMaxy();
		int num = ptAccessabilityConfig.getAnalysisGridSlice();
		xInterval = (maxx-minx)/num;
		yInterval = (maxy-miny)/num;
		
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
		for(int a=1; a <= num; a++) {
			for(int b= 1; b<= num; b++) {
				Coordinate[] coords  =
						   new Coordinate[] {new Coordinate((a-1)*xInterval+minx, (b-1)*yInterval+miny), 
								   				new Coordinate((a)*xInterval+minx, (b-1)*yInterval+miny),
								   					new Coordinate((a)*xInterval+minx, (b)*yInterval+miny),
								   						new Coordinate((a-1)*xInterval+minx, (b)*yInterval+miny), 
								   							new Coordinate((a-1)*xInterval+minx, (b-1)*yInterval+miny)};
				
				LinearRing ring = geometryFactory.createLinearRing( coords );
				LinearRing holes[] = null; // use LinearRing[] to represent holes
				Polygon polygon = geometryFactory.createPolygon(ring, holes );
				
				Map<Integer, Integer> coordMap = new HashedMap();
				coordMap.put(a, b);
				this.num2Polygon.put(coordMap.toString(), polygon);
				this.num2Link.put(coordMap.toString(), new LinkedList<LinkExtendImp>());

				this.num2Stops.put(coordMap.toString(), new LinkedList<TransitStopFacilityExtendImp>());
			}
		}
		this.divideLinksMap();
		this.divideStopsMap();
		new GridShapeFileWriter(this.num2Polygon,this.ptAccessabilityConfig.getOutputDirectory()).write();
	}
	
	private void divideLinksMap(){
		Geometry geometry = null;
		GeometryFactory gf = new GeometryFactory();
		for (Link link : this.network.getLinks().values()) {
			LinkExtendImp linkExtendImp = new LinkExtendImp(link,ptAccessabilityConfig);
				this.linkExtendImps.put(link.getId(), linkExtendImp);
				
				for(String string: this.num2Polygon.keySet()) {
					geometry = (Geometry) this.num2Polygon.get(string);
					boolean bo = geometry.contains(gf.createPoint(new Coordinate(link.getCoord().getX(),link.getCoord().getY())));
					if(bo){
						this.num2Link.get(string).add(linkExtendImp);
						break;
					}
				}
		}		
		for(String string: this.num2Link.keySet()) {
			logger.info(num2Link.get(string).size()+" Links in the "+string+" area");
		}
	}
	private void divideStopsMap() {
		int num = ptAccessabilityConfig.getAnalysisGridSlice();
		for(int a=1; a <= num; a++) {
			for(int b= 1; b<= num; b++) {
				Map<Integer, Integer> coordMap = new HashedMap();
				coordMap.put(a, b);
				LinkedList<Double> xLinkedList = new LinkedList<Double>();
				LinkedList<Double> yLinkedList = new LinkedList<Double>();
				
				if(!this.num2Link.get(coordMap.toString()).isEmpty()) {
					for(Link l : this.num2Link.get(coordMap.toString())) {
						xLinkedList.add(l.getCoord().getX());
						yLinkedList.add(l.getCoord().getY());
					}
					double maxDistance = Collections.max(ptAccessabilityConfig.getModeDistance().values());
					double minx = Collections.min(xLinkedList)-maxDistance;  double miny = Collections.min(yLinkedList)-maxDistance;
					double maxx = Collections.max(xLinkedList)+maxDistance;  double maxy = Collections.max(yLinkedList)+maxDistance;
						
					for(TransitStopFacilityExtendImp transitStopFacilityExtendImp: routeStopInfoCollector.getTransitStopFacilities().values()) {
						if(transitStopFacilityExtendImp.getCoord().getX() > minx && transitStopFacilityExtendImp.getCoord().getX() < maxx
								&& transitStopFacilityExtendImp.getCoord().getY() > miny && transitStopFacilityExtendImp.getCoord().getY() < maxy) {
							this.num2Stops.get(coordMap.toString()).add(transitStopFacilityExtendImp);
						}
					}
					logger.info(num2Stops.get(coordMap.toString()).size()+" Stops in the "+a+","+b+" area");
				}
			}
			
		}
	}

	@Override
	public Map<String, List<LinkExtendImp>> getLinksClassification() {
		// TODO Auto-generated method stub
		return this.num2Link;
	}

	@Override
	public Map<String, List<TransitStopFacilityExtendImp>> getStopsClassification() {
		// TODO Auto-generated method stub
		return this.num2Stops;
	}
	public double getMaxx() {
		return maxx;
	}
	public double getMaxy() {
		return maxy;
	}
	public double getMinx() {
		return minx;
	}
	public double getMiny() {
		return miny;
	}
	public double getxInterval() {
		return xInterval;
	}
	public double getyInterval() {
		return yInterval;
	}
	public Map<String, Polygon> getNum2Polygon() {
		return num2Polygon;
	}


	
	
	
	

}
