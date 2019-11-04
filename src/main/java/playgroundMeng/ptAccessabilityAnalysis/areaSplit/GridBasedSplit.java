package playgroundMeng.ptAccessabilityAnalysis.areaSplit;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.analysis.zonal.DrtGridUtils;
import org.matsim.core.network.NetworkUtils;

import com.google.inject.Inject;

import playgroundMeng.ptAccessabilityAnalysis.linksCategoryAnalysis.LinkExtendImp;
import playgroundMeng.ptAccessabilityAnalysis.run.ConsoleProgressBar;
import playgroundMeng.ptAccessabilityAnalysis.run.GridShapeFileWriter;
import playgroundMeng.ptAccessabilityAnalysis.run.PtAccessabilityConfig;
import playgroundMeng.ptAccessabilityAnalysis.stopInfoCellector.RouteStopInfoCollector;
import playgroundMeng.ptAccessabilityAnalysis.stopInfoCellector.TransitStopFacilityExtendImp;

public class GridBasedSplit implements AreaSplit {
	private static final Logger logger = Logger.getLogger(GridBasedSplit.class);
	
	private Map<String, List<TransitStopFacilityExtendImp>> num2Stops = new HashedMap();
	private Map<String, List<LinkExtendImp>> num2Link = new HashedMap();
	private Map<String, Geometry> num2Polygon = new HashedMap();
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
		logger.info("beginn to divide the area into grid");
		
		if(this.ptAccessabilityConfig.getAnalysisNetworkFile() != null) {
			Network network2 = NetworkUtils.readNetwork(this.ptAccessabilityConfig.getAnalysisNetworkFile());
			this.num2Polygon = DrtGridUtils.createGridFromNetwork(network2, this.ptAccessabilityConfig.getAnalysisGridSlice());
		} else {
			this.num2Polygon = DrtGridUtils.createGridFromNetwork(this.network, this.ptAccessabilityConfig.getAnalysisGridSlice());
		}
		new GridShapeFileWriter(this.num2Polygon,this.ptAccessabilityConfig.getOutputDirectory()).write();
		this.divideLinksMap();
		this.divideStopsMap();		
	}
	
	private void divideLinksMap(){
		logger.info("beginn to divide Links into Grid");
		Geometry geometry = null;
		GeometryFactory gf = new GeometryFactory();
		
		String LinkDividedProgress = "LinkDividedProgress";
		int a = 0;
		int total = this.network.getLinks().size();
		
		for (Link link : this.network.getLinks().values()) {
			LinkExtendImp linkExtendImp = new LinkExtendImp(link,ptAccessabilityConfig);
				this.linkExtendImps.put(link.getId(), linkExtendImp);
				
				for(String string: this.num2Polygon.keySet()) {
					geometry = this.num2Polygon.get(string);
					boolean bo = geometry.contains(gf.createPoint(new Coordinate(link.getCoord().getX(),link.getCoord().getY())));
					if(!this.num2Link.containsKey(string)) {
						this.num2Link.put(string,  new LinkedList<LinkExtendImp>());
					} 
					if(bo){
						this.num2Link.get(string).add(linkExtendImp);
						break;
					}
				}
				a++;
				if(a%(total/10) == 0) {
					ConsoleProgressBar.progressPercentage(a, total, LinkDividedProgress,logger);
				} else if (a == total) {
					ConsoleProgressBar.progressPercentage(a, total, LinkDividedProgress,logger);
				}
		}
	}
	private void divideStopsMap() {
		int remain = 0;
		int total = this.num2Link.keySet().size();
		String stopDividedProgress = "StopDividedProgress";
		
		for(String string : this.num2Link.keySet()) {
				
		LinkedList<Double> xLinkedList = new LinkedList<Double>();
		LinkedList<Double> yLinkedList = new LinkedList<Double>();
				
		if(!this.num2Link.get(string).isEmpty()) {
			for(Link l : this.num2Link.get(string)) {
				xLinkedList.add(l.getCoord().getX());
				yLinkedList.add(l.getCoord().getY());
				}
				double maxDistance = Collections.max(ptAccessabilityConfig.getModeDistance().values());
				double minx = Collections.min(xLinkedList)-maxDistance;  double miny = Collections.min(yLinkedList)-maxDistance;
				double maxx = Collections.max(xLinkedList)+maxDistance;  double maxy = Collections.max(yLinkedList)+maxDistance;
						
				for(TransitStopFacilityExtendImp transitStopFacilityExtendImp: routeStopInfoCollector.getTransitStopFacilities().values()) {
					if(transitStopFacilityExtendImp.getCoord().getX() > minx && transitStopFacilityExtendImp.getCoord().getX() < maxx
							&& transitStopFacilityExtendImp.getCoord().getY() > miny && transitStopFacilityExtendImp.getCoord().getY() < maxy) {
						if(!this.num2Stops.containsKey(string)) {
							this.num2Stops.put(string,  new LinkedList<TransitStopFacilityExtendImp>());
						} 
					this.num2Stops.get(string).add(transitStopFacilityExtendImp);
					}
				}
			}
			remain++;
			if(remain % (total/10) == 0) {
				ConsoleProgressBar.progressPercentage(remain, total, stopDividedProgress, logger);
			}  else if (remain == total) {
				ConsoleProgressBar.progressPercentage(remain, total, stopDividedProgress,logger);
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
	public Map<String, Geometry> getNum2Polygon() {
		return num2Polygon;
	}


	
	
	
	

}
