/**
 * 
 */
package playground.tschlenther.parkingSearch.memoryBased;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.parking.parkingsearch.DynAgent.agentLogic.ParkingAgentLogic;
import org.matsim.contrib.parking.parkingsearch.manager.FacilityBasedParkingManager;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.WalkLegFactory;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.routing.ParkingRouter;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;

import playground.tschlenther.parkingSearch.Benenson.BenensonParkingAgentLogic;
import playground.tschlenther.parkingSearch.Benenson.BenensonParkingSearchLogic;

/**
 * @author Work
 *
 */
public class MemoryBasedParkingAgentFactory implements AgentFactory {

	
	@Inject
	WalkLegFactory walkLegFactory;
	@Inject
	ParkingSearchManager parkingManager;

	@Inject
	EventsManager events;
	@Inject
	ParkingRouter parkingRouter;
	@Inject
	Network network;
	@Inject
	VehicleTeleportationLogic teleportationLogic;
	
	private final QSim qsim;
	
	/**
	 * 
	 */
	@Inject
	public MemoryBasedParkingAgentFactory(QSim qsim) {
		this.qsim = qsim;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.mobsim.qsim.agents.AgentFactory#createMobsimAgentFromPerson(org.matsim.api.core.v01.population.Person)
	 */
	@Override
	public MobsimAgent createMobsimAgentFromPerson(Person p) {
		ParkingSearchLogic parkingLogic  = new DistanceMemoryBasedParkingSearchLogic(network);
		ParkingAgentLogic agentLogic = new MemoryBasedParkingAgentLogic(p.getSelectedPlan(), parkingManager, walkLegFactory,
				parkingRouter, events, parkingLogic,  ((QSim) qsim).getSimTimer(),teleportationLogic );
		Id<Link> startLinkId = ((Activity) p.getSelectedPlan().getPlanElements().get(0)).getLinkId();
		if (startLinkId == null) {
			throw new NullPointerException(" No start link found. Should not happen.");
		}
		DynAgent agent = new DynAgent(p.getId(), startLinkId, events, agentLogic);
		return agent;
	}

}
