package playgroundMeng.publicTransitServiceAnalysis.gridAnalysis;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtGridUtils;
import org.matsim.core.network.NetworkUtils;

import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.GridImp;
import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.LinkExtendImp;
import playgroundMeng.publicTransitServiceAnalysis.infoCollector.EventsReader;
import playgroundMeng.publicTransitServiceAnalysis.infoCollector.RouteStopInfoCollector;
import playgroundMeng.publicTransitServiceAnalysis.others.ConsoleProgressBar;
import playgroundMeng.publicTransitServiceAnalysis.others.PtAccessabilityConfig;

public class GridCreator {
	private static final Logger logger = Logger.getLogger(GridCreator.class);
	private static GridCreator gridCreator = null;
	private Map<String, Geometry> num2Polygon = new HashedMap();
	private Map<String, GridImp> num2Grid = new HashedMap();

	private final Network network;
	private List<LinkExtendImp> linkExtendImps = new LinkedList<LinkExtendImp>();
	private final PtAccessabilityConfig ptAccessabilityConfig;

	private GridCreator() {
		this.ptAccessabilityConfig = PtAccessabilityConfig.getInstance();
		this.network = ptAccessabilityConfig.getNetwork();
		this.setLinkExtendImps();
		logger.info("beginn to create grids");

		if (this.ptAccessabilityConfig.getAnalysisNetworkFile() != null) {
			Network network2 = NetworkUtils.readNetwork(this.ptAccessabilityConfig.getAnalysisNetworkFile());
			this.num2Polygon = DrtGridUtils.createGridFromNetwork(network2,
					this.ptAccessabilityConfig.getAnalysisGridSlice());
		} else {
			this.num2Polygon = DrtGridUtils.createGridFromNetwork(this.network,
					this.ptAccessabilityConfig.getAnalysisGridSlice());
		}
		int remain = 0;
		int total = this.num2Polygon.keySet().size();
		String caculateRatioProgress = "DivideIntoGridProgress";

		for (String string : this.num2Polygon.keySet()) {
			this.num2Grid.put(string,
					new GridImp(num2Polygon.get(string), ptAccessabilityConfig.getAnalysisTimeSlice()));
			InfrastructureIntoGridDivider.divideLinksIntoGrid(this.num2Grid.get(string), linkExtendImps);
			InfrastructureIntoGridDivider.divideStopsIntoGrid(this.num2Grid.get(string),
					RouteStopInfoCollector.getInstance().getTransitStopFacilities().values());
			TripsIntoGridDivider.divideTripsIntoGrid(this.num2Grid.get(string), EventsReader.getInstance().getTrips());

			remain++;
			if (remain % (total / 10) == 0) {
				ConsoleProgressBar.progressPercentage(remain, total, caculateRatioProgress, logger);
			} else if (remain == total) {
				ConsoleProgressBar.progressPercentage(remain, total, caculateRatioProgress, logger);
			}
		}
	}

	public Map<String, Geometry> getSubGeometriesByRadom(Map<String, Geometry> map, int count) {
		Map<String, Geometry> subMap = new HashedMap();
		Random random = new Random();
		int backSum = 0;
		if (map.size() >= count) {
			backSum = count;
		} else {
			backSum = map.size();
		}
		for (int i = 0; i < backSum; i++) {

			int target = random.nextInt(map.size());
			int a = 0;
			for (String string : map.keySet()) {
				if (a == target) {
					subMap.put(string, map.get(string));
				}
				a++;
			}
		}
		return subMap;
	}

	public static GridCreator getInstacne() {
		if (gridCreator == null) {
			gridCreator = new GridCreator();
		}
		return gridCreator;

	}

	public void setLinkExtendImps() {
		for (Link link : network.getLinks().values()) {
			LinkExtendImp linkExtendImp = new LinkExtendImp(link);
			this.linkExtendImps.add(linkExtendImp);
		}
	}

	public Map<String, GridImp> getNum2Grid() {
		return num2Grid;
	}

	public Map<String, Geometry> getNum2Polygon() {
		return num2Polygon;
	}

}
