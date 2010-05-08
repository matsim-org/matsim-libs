package playground.christoph.replanning;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.mobsim.queuesim.SimulationTimer;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.network.MyLinkImpl;

/*
 * The VehicleCounts are updated after all Replanning Actions within a 
 * simulated TimeStep, so all Agents plan their Routes based on the same
 * counts.
 * 
 * Each Agent creates a Replanning Event that contains the replanned Leg.
 * This Leg contains the new Route that is used to update the Vehicle Counts.
 * Doing this is an additional effort (basically calculating the travel time
 * a second time to identify the TimeSlots), but at the moment its is the
 * option with the smallest changes in the other parts of the Framework.
 * 
 * Updating the Counts only once per SimStep could leed to Problems if to many 
 * Agents plan their Routes in the same TimeStep (for example an Evacuation 
 * Scenario). One possible Solution would be an iterative Approach within the
 * TimeStep like "replan 10k Agents, update Counts, replan another 10k Agents, ...".    
 */
public class TravelTimeEstimator implements TravelTime, Cloneable{

	private static final Logger log = Logger.getLogger(TravelTimeEstimator.class);

	private double flowCapacityFactor = Gbl.getConfig().simulation().getFlowCapFactor();
	private double storageCapacityFactor = Gbl.getConfig().simulation().getStorageCapFactor();
	private double vehicleLength = 7.5;
	
	private int travelTimeBinSize;
	private int numSlots;

	private TravelTimeEstimatorHandlerAndListener handlerAndListener;
	
	private boolean useMyLinkImpls = true;
	/*
	 * We could instead use MyLinkImpls and add the Array to each single Link.
	 * Doing this would avoid the lookups in the HashMap. Use floats because
	 * they should use only half the memory size that doubles would need.
	 */
	private Map<Id, float[]> linkVehicleCounts;	// LinkId
	private Map<Id, int[]> linkEnterCounts;	// LinkId
	private Map<Id, int[]> linkLeaveCounts;	// LinkId

	public TravelTimeEstimator(TravelTimeEstimatorHandlerAndListener handlerAndListener)
	{
		this.handlerAndListener = handlerAndListener;
		this.travelTimeBinSize = handlerAndListener.getTravelTimeBinSize();
		this.numSlots = handlerAndListener.getNumSlots();
	}
	
	// implements TravelTime
	public double getLinkTravelTime(Link link, double time)
	{
		return calcTravelTime(link, time);
	}

	private int getTimeSlotIndex(double time)
	{
		int slice = ((int) time) / this.travelTimeBinSize;
		if (slice >= this.numSlots)
			slice = this.numSlots - 1;
		return slice;
	}

	private double calcTravelTime(Link link, double time)
	{
		int timeSlotIndex = this.getTimeSlotIndex(time);

		float[] vehicleCounts;
		int[] enterCounts;
		int[] leaveCounts;
	
		if (this.useMyLinkImpls)
		{
			MyLinkImpl myLink = (MyLinkImpl)link;
			vehicleCounts = myLink.getLinkVehicleCounts();
			enterCounts = myLink.getLinkEnterCounts();
			leaveCounts = myLink.getLinkLeaveCounts();
		}
		else
		{
			vehicleCounts = linkVehicleCounts.get(link.getId());
			enterCounts = linkEnterCounts.get(link.getId());
			leaveCounts = linkLeaveCounts.get(link.getId());			
		}
		
		float vehiclesCount = vehicleCounts[timeSlotIndex];
		int enterCount = enterCounts[timeSlotIndex];
		int leaveCount = leaveCounts[timeSlotIndex];
		
		double flow = (enterCount + leaveCount) / 2;
		
//		int veh;
//		// Do we use SubNetworks?
//		if (link instanceof SubLink)
//		{
//			Link parentLink = ((SubLink) link).getParentLink();
//			veh = ((MyLinkImpl) parentLink).getVehiclesCount();
//		} 
//		else
//		{
//			veh = ((MyLinkImpl) link).getVehiclesCount();
//		}
//		
//		vehiclesCount = veh;
		
		// calculate and return
//		return calcBPRTravelTime(link, flow, time);
		double bpr = calcBPRTravelTime(link, flow, time);
		double fundamental = getFundamentalTravelTime(link, time, vehiclesCount, flow);
		
		return fundamental;
	}

	// calculate "real" travel time on the link and return it
	/*
	 * rho ... Density
	 * flow ... Flow
	 */
	private double getFundamentalTravelTime(Link link, double time, double vehicles, double binFlow)
	{	
		// Adding the vehicle itself to the count.
//		vehicles++;
		
		QueueNetwork queueNetwork = this.handlerAndListener.getQueueNetwork();
		
		double length = link.getLength();		
		double freeSpeed = link.getFreespeed(time);
		double freeSpeedTravelTime = length / freeSpeed;
		
		if (length == 0.0) return 0.0;
		
//		double rhoMax = length * this.storageCapacityFactor / this.vehicleLength;
//		double flowMax = link.getCapacity(time) * SimulationTimer.getSimTickTime() * flowCapacityFactor / 36000;
//		double rhoMax = queueNetwork.getLinks().get(link.getId()).getSpaceCap();
		double vehMax = queueNetwork.getQueueLinks().get(link.getId()).getStorageCapacity();
		double rhoMax = vehMax / length;
		
		double flowMax = queueNetwork.getQueueLinks().get(link.getId()).getSimulatedFlowCapacity();
		double rhoFreeSpeed = flowMax / freeSpeed;
		
		double flow = binFlow / this.travelTimeBinSize;
		double rho = vehicles / length;
				
		// If we can Travel at FreeSpeed
		if (rho < rhoFreeSpeed) return freeSpeedTravelTime;
		
		// We ensure that we have at least a small velocity
		if (rho > rhoMax) return length / 0.01;
		
		// Linear Approach...
		double deltaV = (freeSpeed - 0.1) / (rhoMax - rhoFreeSpeed);
		double vL = freeSpeed - deltaV * (rho - rhoFreeSpeed);

		double travelTimeL = length / vL;

		// S-Shaped Approach
		double rhoWidth = (rhoMax - rhoFreeSpeed);
		double dRho = rhoFreeSpeed + (rhoWidth)/2;
		
//		dRho = dRho * 0.40;
		
		double vS = freeSpeed * (1 - 1 / (1 + Math.exp((-rho + dRho)/(rhoWidth * 0.1)) ) );
		double travelTimeS = length / vS;
		
		return travelTimeS;
	}
	
	private double calcBPRTravelTime(Link link, double flow, double time)
	{	
		/*
		 * After reaching the end of a Link a vehicles has typically to
		 * wait for some time until the next Link can be entered.
		 * Maybe a Function f(#vehicles on outlinks, etc.) would produce
		 * better Results?
		 */
		double linkLeaveTime = 0.0;
			
		/*
		 * We don't allow negative vehicles counts. Doing this ensures that we
		 * don't get TravelTimes that are shorter than the FreeSpeedTravelTime.
		 */
//		if (vehicles < 0.0) vehicles = 0.0;

		/*
		 * Calc TravelTime based on the vehiclesCount. We increase the count by
		 * one so we calculate the TravelTime that the Vehicles would have.
		 */
//		vehicles++;
		
		double freeSpeed = link.getFreespeed(time);
		double length = link.getLength();

		// TravelTime in empty Network -> FreeSpeedTravelTime
		double t0 = length / freeSpeed;

		flow = flow * 25;
//		flow = flow * 30;
		
		double qa = flow * 3600 / this.travelTimeBinSize; // vehicles / hour (?)
		double ca = link.getCapacity(time) * SimulationTimer.getSimTickTime() * flowCapacityFactor;
//		double ca = link.getCapacity(time) * SimulationTimer.getSimTickTime() * Gbl.getConfig().simulation().getFlowCapFactor();
		
		// Model Parameters
		double alpha = 0.15;
		double beta = 4;
//		double alpha = 0.20;
//		double beta = 5;

		double travelTime = t0 * (1 + alpha * Math.pow((qa / ca), beta));
		
		return travelTime + linkLeaveTime;
	}
	
	public TravelTimeEstimator clone()
	{
		TravelTimeEstimator clone = new TravelTimeEstimator(this.handlerAndListener);
		
		return clone;
	}
}