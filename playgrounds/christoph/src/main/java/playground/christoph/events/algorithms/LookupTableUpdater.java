package playground.christoph.events.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.mobsim.queuesim.SimulationTimer;

import playground.christoph.router.util.LookupTable;

/*
 * Until now, the updating Methods have been called from
 * the ReplanningQueueSimulation.
 * Now we now try to do this event based...
 * 
 * Attention: The LinkVehiclesCounter has to be added to the
 * QueueSimulation before the LookupTableUpdater is added!
 * 
 * When cloning LookupTables for using them in Multi-Thread
 * Simulations typically its sufficient to add only the parent
 * LookupTable. All clones should use the same instance of
 * the Data Structure that is used to implement the LookupTable.
 */
public class LookupTableUpdater implements SimulationAfterSimStepListener, SimulationInitializedListener{
	
	/*
	 * We can handle different Replanner Arrays.
	 * For Example the LeaveLinkReplanner could use
	 * another Replanner than the ActEndReplanner.
	 */
	private List<LookupTable> lookupTables;
	
	public LookupTableUpdater()
	{
		this.lookupTables = new ArrayList<LookupTable>();
	}
	
	public void addLookupTable(LookupTable lookupTable)
	{
		this.lookupTables.add(lookupTable);
	}
	
	public void removeLookupTable(LookupTable lookupTable)
	{
		this.lookupTables.remove(lookupTable);
	}
	
	/*
	 * Using LookupTables for the LinktravelTimes may speed up the WithinDayReplanning.
	 */
	public void updateLinkLookupTables(double time)
	{	
		for (LookupTable lookupTable : lookupTables)
		{
			lookupTable.updateLookupTable(time);
		}
	}
	

	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e)
	{
//		System.out.println("LookupTableUpdater QueueSimulationAfterSimStepEvent " + e.getSimulationTime() + "-------------------------------------------------------------------------------");
		/*
		 *  We update our LookupTables for the next Timestep.
  		 * Update the LinkTravelTimes first because the LinkTravelCosts may use
		 * them already!
		 */
		updateLinkLookupTables(e.getSimulationTime() + SimulationTimer.getSimTickTime());
	}

	public void notifySimulationInitialized(SimulationInitializedEvent e)
	{
//		System.out.println("LookupTableUpdater QueueSimulationInitializedEvent-------------------------------------------------------------------------------");
		/*
		 *  We update our LookupTables for the next Timestep.
  		 * Update the LinkTravelTimes first because the LinkTravelCosts may use
		 * them already!
		 */
		updateLinkLookupTables(SimulationTimer.getSimStartTime());	
	}
}