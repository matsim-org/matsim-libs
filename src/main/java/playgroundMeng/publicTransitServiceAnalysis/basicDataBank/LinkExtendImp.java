package playgroundMeng.publicTransitServiceAnalysis.basicDataBank;

import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.HashedMap;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;

import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.TransitStopFacilityExtendImp;
import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.TransitStopFacilityExtendImp.RouteStopInfo;
import playgroundMeng.publicTransitServiceAnalysis.run.PtAccessabilityConfig;

public class LinkExtendImp implements Link {

	private PtAccessabilityConfig analysisConfig;
	private Link link;
	private Id<Link> linkId;
	private Map<Map<Id<TransitStopFacility>, RouteStopInfo>, Double> ptInfos = new HashedMap();
	private Map<Integer, Double> time2Score;
	private boolean findGrid = false;

	public LinkExtendImp(Link link) {
		this.link = link;
		this.analysisConfig = PtAccessabilityConfig.getInstance();
	}

	public void addPtInfos(Id<TransitStopFacility> transitStopId, RouteStopInfo routeStopInfo, Double distance) {
		Map<Id<TransitStopFacility>, RouteStopInfo> transitId2RouteStopInfo = new HashedMap();
		transitId2RouteStopInfo.put(transitStopId, routeStopInfo);
		this.ptInfos.put(transitId2RouteStopInfo, distance);
	}

	public void addStopsInfo(TransitStopFacilityExtendImp transitStopFacilityExtendImp) throws Exception {

		for (RouteStopInfo routeStopInfo : transitStopFacilityExtendImp.getRouteStopInfoMap().values()) {
			String mode = routeStopInfo.getTransportMode();
			if (!this.analysisConfig.getModeDistance().containsKey(mode)) {
				throw new RuntimeException(mode + " is not defined in config");
			} else {
				double configDis = this.analysisConfig.getModeDistance().get(mode);
				double dis = this.getDistance2Stops(transitStopFacilityExtendImp);
				if (dis <= configDis) {
					this.addPtInfos(transitStopFacilityExtendImp.getId(), routeStopInfo, dis);
				}
			}
		}

	}

	public double getDistance2Stops(TransitStopFacilityExtendImp transitStopFacilityExtendImp) {
		Coord stopCoord = transitStopFacilityExtendImp.getCoord();
		Coord linkCoord = this.getCoord();
		double x1 = stopCoord.getX();
		double y1 = stopCoord.getY();
		double x2 = linkCoord.getX();
		double y2 = linkCoord.getY();
		return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
	}

	public Map<Map<Id<TransitStopFacility>, RouteStopInfo>, Double> getPtInfos() {
		return ptInfos;
	}

	public void setTime2Score(int x, Double score) {
		Map<Integer, Double> time2Score = new HashedMap();
		time2Score.put(x, score);
		this.time2Score = time2Score;
	}

	public Map<Integer, Double> getTime2Score() {
		return time2Score;
	}

	public boolean isFindGrid() {
		return findGrid;
	}

	public void setFindGrid(boolean findGrid) {
		this.findGrid = findGrid;
	}

	// override methods below
	@Override
	public Coord getCoord() {
		// TODO Auto-generated method stub
		return this.link.getCoord();
	}

	@Override
	public Attributes getAttributes() {
		// TODO Auto-generated method stub
		return this.link.getAttributes();
	}

	@Override
	public Id<Link> getId() {
		if (this.link == null) {
			return this.linkId;
		} else {
			return this.link.getId();
		}
	}

	@Override
	public boolean setFromNode(Node node) {
		// TODO Auto-generated method stub
		return this.link.setFromNode(node);
	}

	@Override
	public boolean setToNode(Node node) {
		// TODO Auto-generated method stub
		return this.link.setToNode(node);
	}

	@Override
	public Node getToNode() {
		// TODO Auto-generated method stub
		return this.link.getToNode();
	}

	@Override
	public Node getFromNode() {
		// TODO Auto-generated method stub
		return this.link.getFromNode();
	}

	@Override
	public double getLength() {
		// TODO Auto-generated method stub
		return this.link.getLength();
	}

	@Override
	public double getNumberOfLanes() {
		// TODO Auto-generated method stub
		return this.link.getNumberOfLanes();
	}

	@Override
	public double getNumberOfLanes(double time) {
		// TODO Auto-generated method stub
		return this.link.getNumberOfLanes(time);
	}

	@Override
	public double getFreespeed() {
		// TODO Auto-generated method stub
		return this.link.getFreespeed();
	}

	@Override
	public double getFreespeed(double time) {
		// TODO Auto-generated method stub
		return this.link.getFreespeed(time);
	}

	@Override
	public double getCapacity() {
		// TODO Auto-generated method stub
		return this.link.getCapacity();
	}

	@Override
	public double getCapacity(double time) {
		// TODO Auto-generated method stub
		return this.link.getCapacity();
	}

	@Override
	public void setFreespeed(double freespeed) {
		// TODO Auto-generated method stub
		this.link.setFreespeed(freespeed);
	}

	@Override
	public void setLength(double length) {
		// TODO Auto-generated method stub
		this.link.setLength(length);
	}

	@Override
	public void setNumberOfLanes(double lanes) {
		// TODO Auto-generated method stub
		this.link.setNumberOfLanes(lanes);
	}

	@Override
	public void setCapacity(double capacity) {
		// TODO Auto-generated method stub
		this.link.setCapacity(capacity);
	}

	@Override
	public void setAllowedModes(Set<String> modes) {
		// TODO Auto-generated method stub
		this.link.setAllowedModes(modes);
	}

	@Override
	public Set<String> getAllowedModes() {
		// TODO Auto-generated method stub
		return this.link.getAllowedModes();
	}

	@Override
	public double getFlowCapacityPerSec() {
		// TODO Auto-generated method stub
		return this.link.getFlowCapacityPerSec();
	}

	@Override
	public double getFlowCapacityPerSec(double time) {
		// TODO Auto-generated method stub
		return this.link.getFlowCapacityPerSec(time);
	}

}
