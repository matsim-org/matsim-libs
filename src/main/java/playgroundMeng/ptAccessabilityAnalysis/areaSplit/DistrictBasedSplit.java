package playgroundMeng.ptAccessabilityAnalysis.areaSplit;


import java.util.Collection;
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
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.google.inject.Inject;


import playgroundMeng.ptAccessabilityAnalysis.linksCategoryAnalysis.LinkExtendImp;
import playgroundMeng.ptAccessabilityAnalysis.run.PtAccessabilityConfig;
import playgroundMeng.ptAccessabilityAnalysis.stopInfoCellector.RouteStopInfoCollector;
import playgroundMeng.ptAccessabilityAnalysis.stopInfoCellector.TransitStopFacilityExtendImp;




public class DistrictBasedSplit implements AreaSplit {
	private static final Logger logger = Logger.getLogger(DistrictBasedSplit.class);
	private Map<String, List<TransitStopFacilityExtendImp>> district2Stops = new HashedMap();
	private Map<String, List<LinkExtendImp>> district2Link = new HashedMap();
	private Map<Id<Link>, LinkExtendImp> linkExtendImps = new HashedMap();
	private Collection<SimpleFeature> simpleFeatures = null;
	
	Network network;
	Population population;
	RouteStopInfoCollector routeStopInfoCollector;
	PtAccessabilityConfig ptAccessabilityConfig;
	
	@Inject
	public DistrictBasedSplit(Network network,Population population,RouteStopInfoCollector routeStopInfoCollector,PtAccessabilityConfig ptAccessabilityConfig) {
		// filter the Links
		this.network = network;
		this.population = population;
		this.routeStopInfoCollector = routeStopInfoCollector;
		this.ptAccessabilityConfig = ptAccessabilityConfig;
		
		this.creatLinkExtendImps(network.getLinks().values());
		this.readShapeFile();
		this.filterLinkId();
		for(String string: district2Link.keySet()) {
			this.district2Stops.put(string, new LinkedList<TransitStopFacilityExtendImp>());
		}
		// filter the stops
		logger.info("beginn to filter stops");
		this.divideStopsForDistrict();
		
	}
	
	private void readShapeFile() {
		ShapeFileReader shapeFileReader = new ShapeFileReader();
		this.simpleFeatures = shapeFileReader.getAllFeatures(ptAccessabilityConfig.getShapeFile());
	}
	
	private void filterLinkId(){
		Geometry geometry = null;
		GeometryFactory gf = new GeometryFactory();
		int a = 1;
		for(Link link : network.getLinks().values()) {
			for(SimpleFeature simpleFeature : this.simpleFeatures) {
				if(!this.district2Link.keySet().contains(simpleFeature.getAttribute("NAME").toString())) {
					this.district2Link.put(simpleFeature.getAttribute("NAME").toString(), new LinkedList<LinkExtendImp>());
				}
				geometry = (Geometry) simpleFeature.getDefaultGeometry();
				boolean bo = geometry.contains(gf.createPoint(new Coordinate(link.getCoord().getX(),link.getCoord().getY())));
				if(bo){
					this.district2Link.get(simpleFeature.getAttribute("NAME").toString()).add(this.linkExtendImps.get(link.getId()));
					break;
				}
			}
			if(a%1000 == 0) {
				logger.info("beginn to filter the "+a+"th link");
			}
			a++;
		}
	}
	private void creatLinkExtendImps(Collection<? extends Link> links) {
		for (Link link : links) {
			LinkExtendImp linkExtendImp = new LinkExtendImp(link,ptAccessabilityConfig);
				this.linkExtendImps.put(link.getId(), linkExtendImp);
		}
	}
	
	private void divideStopsForDistrict() {
		for(String string: this.district2Stops.keySet()) {
			
			LinkedList<Double> xLinkedList = new LinkedList<Double>();
			LinkedList<Double> yLinkedList = new LinkedList<Double>();
			
			if(!this.district2Link.get(string).isEmpty()) {
				for(Link l : this.district2Link.get(string)) {
					xLinkedList.add(l.getCoord().getX());
					yLinkedList.add(l.getCoord().getY());
				}
				double maxDistance = Collections.max(ptAccessabilityConfig.getModeDistance().values());
				double minx = Collections.min(xLinkedList)-maxDistance;  double miny = Collections.min(yLinkedList)-maxDistance;
				double maxx = Collections.max(xLinkedList)+maxDistance;  double maxy = Collections.max(yLinkedList)+maxDistance;
					
				for(TransitStopFacilityExtendImp transitStopFacilityExtendImp: routeStopInfoCollector.getTransitStopFacilities().values()) {
					if(transitStopFacilityExtendImp.getCoord().getX() > minx && transitStopFacilityExtendImp.getCoord().getX() < maxx
							&& transitStopFacilityExtendImp.getCoord().getY() > miny && transitStopFacilityExtendImp.getCoord().getY() < maxy) {
						this.district2Stops.get(string).add(transitStopFacilityExtendImp);
					}
				}
				logger.info(district2Link.get(string).size()+" links in the "+string+" area");
				logger.info(district2Stops.get(string).size()+" Stops in the "+string+" area");
		}
	}
}

	@Override
	public Map<String, List<LinkExtendImp>> getLinksClassification() {
		return this.district2Link;
	}

	@Override
	public Map<String, List<TransitStopFacilityExtendImp>> getStopsClassification() {
		return this.district2Stops;
	}



}
