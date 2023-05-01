package ch.sbb.matsim.contrib.railsim.prototype.supply.infrastructure;

import ch.sbb.matsim.contrib.railsim.prototype.supply.DepotInfo;
import ch.sbb.matsim.contrib.railsim.prototype.supply.InfrastructureRepository;
import ch.sbb.matsim.contrib.railsim.prototype.supply.RailsimSupplyConfigGroup;
import ch.sbb.matsim.contrib.railsim.prototype.supply.SectionPartInfo;
import ch.sbb.matsim.contrib.railsim.prototype.supply.SectionSegmentInfo;
import ch.sbb.matsim.contrib.railsim.prototype.supply.StopInfo;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;

/**
 * Default implementation of the infrastructure repository
 *
 * @author Merlin Unterfinger
 */
public class DefaultInfrastructureRepository implements InfrastructureRepository {

	private final int stopCapacity;
	private final double stopSpeedLimit;
	private final double stopLinkLength;
	private final int depotCapacity;
	private final int depotInOutCapacity;
	private final double depotLinkLength;
	private final double depotSpeedLimit;
	private final double depotOffset;
	private final int routeTrainCapacity;
	private final double routeSpeedLimit;
	private final double routeEuclideanDistanceFactor;

	public DefaultInfrastructureRepository(Scenario scenario) {
		var config = ConfigUtils.addOrGetModule(scenario.getConfig(), RailsimSupplyConfigGroup.class);
		stopCapacity = config.getStopTrainCapacity();
		stopSpeedLimit = config.getStopSpeedLimit();
		stopLinkLength = config.getStopLinkLength();
		routeTrainCapacity = config.getRouteTrainCapacity();
		routeSpeedLimit = config.getRouteSpeedLimit();
		routeEuclideanDistanceFactor = config.getRouteEuclideanDistanceFactor();
		depotCapacity = config.getDepotTrainCapacity();
		depotInOutCapacity = config.getDepotInOutCapacity();
		depotLinkLength = config.getDepotLinkLength();
		depotSpeedLimit = config.getDepotSpeedLimit();
		depotOffset = config.getDepotOffset();
	}

	@Override
	public StopInfo getStop(String stopId, double x, double y) {
		var stopInfo = new StopInfo(stopId, new Coord(x, y), stopLinkLength);
		InfrastructureRepository.addRailsimAttributes(stopInfo, stopCapacity, stopSpeedLimit, 0.);
		return stopInfo;
	}

	@Override
	public DepotInfo getDepot(StopInfo stopInfo) {
		var depotInfo = new DepotInfo(stopInfo.getId(), new Coord(stopInfo.getCoord().getX(), stopInfo.getCoord().getY() - depotOffset), depotLinkLength, depotOffset, depotOffset, depotCapacity);
		// add in link attributes
		InfrastructureRepository.addRailsimAttributes(depotInfo, depotCapacity, depotInOutCapacity, depotSpeedLimit, 0.);
		return depotInfo;
	}

	@Override
	public SectionPartInfo getSectionPart(StopInfo fromStop, StopInfo toStop) {
		final double length = routeEuclideanDistanceFactor * NetworkUtils.getEuclideanDistance(fromStop.getCoord(), toStop.getCoord());
		var sectionPartInfo = new SectionPartInfo(fromStop.getId(), toStop.getId());
		// add one segment for th section
		var sectionSegmentInfo = new SectionSegmentInfo(fromStop.getCoord(), toStop.getCoord(), length);
		sectionPartInfo.addSegment(sectionSegmentInfo);
		// add link attributes
		InfrastructureRepository.addRailsimAttributes(sectionSegmentInfo, routeTrainCapacity, routeSpeedLimit, 0.);
		return sectionPartInfo;
	}
}
