package playgroundMeng.ptTravelTimeAnalysis;

import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtGridUtils;
import org.matsim.core.network.NetworkUtils;

import com.google.inject.Inject;

import playgroundMeng.ptAccessabilityAnalysis.run.GridShapeFileWriter;
import playgroundMeng.ptAccessabilityAnalysis.run.PtAccessabilityConfig;

public class GridRatioAnalysis {
	@Inject
	RatioCaculator ratioCaculator;
	@Inject
	Network network;
	@Inject
	TravelTimeConfig travelTimeConfig;
	
	private Map<String, Geometry> num2Polygon = new HashedMap();
	
	public GridRatioAnalysis() {
		if(this.travelTimeConfig.getAnalysisNetworkFile() != null) {
			Network network2 = NetworkUtils.readNetwork(this.travelTimeConfig.getAnalysisNetworkFile());
			this.num2Polygon = DrtGridUtils.createGridFromNetwork(network2, this.travelTimeConfig.getGridSlice());
		} else {
			this.num2Polygon = DrtGridUtils.createGridFromNetwork(network, this.travelTimeConfig.getGridSlice());
		}
	}
	
	
}
