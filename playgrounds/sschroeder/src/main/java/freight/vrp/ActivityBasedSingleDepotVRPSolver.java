package freight.vrp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.mzilske.freight.carrier.Tour;
import playground.mzilske.freight.carrier.TourBuilder;
import vrp.algorithms.ruinAndRecreate.RuinAndRecreate;
import vrp.algorithms.ruinAndRecreate.api.RuinAndRecreateListener;
import vrp.algorithms.ruinAndRecreate.factories.RuinAndRecreateFactory;
import vrp.api.SingleDepotVRP;
import vrp.basics.SingleDepotInitialSolutionFactoryImpl;
import vrp.basics.TourActivity;

public class ActivityBasedSingleDepotVRPSolver {
	
	private SingleDepotVRP vrp;
	
	private Collection<Tour> solution;
	
	private RuinAndRecreateFactory rrFactory;
	
	private List<RuinAndRecreateListener> listeners = new ArrayList<RuinAndRecreateListener>();

	public ActivityBasedSingleDepotVRPSolver(SingleDepotVRP vrp, RuinAndRecreateFactory rrFactory) {
		super();
		this.vrp = vrp;
	}
	
	public void run(){
		RuinAndRecreate algo = createAlgo(vrp);
		algo.run();
		solution = makeTours(algo.getSolution());
	}
	
	public Collection<Tour> getSolution(){
		return solution;
	}
	
	private Collection<Tour> makeTours(Collection<vrp.basics.Tour> solution) {
		Collection<Tour> tours = new ArrayList<Tour>();
		for(vrp.basics.Tour t : solution){
			TourBuilder tourBuilder = new TourBuilder();
			tourBuilder.scheduleStart(makeId(t.getActivities().getFirst().getLocation().getId()));
			for(TourActivity act : t.getActivities()){
				tourBuilder.scheduleGeneralActivity(act.getCustomer().getId(), makeId(act.getLocation().getId()), 
						act.getEarliestArrTime(), act.getLatestArrTime(), act.getServiceTime());
			}
			tourBuilder.scheduleEnd(makeId(t.getActivities().getLast().getLocation().getId()));
			tours.add(tourBuilder.build());
		}
		return tours;
	}

	private Id makeId(String id) {
		return new IdImpl(id);
	}

	private RuinAndRecreate createAlgo(SingleDepotVRP vrp) {
		for(RuinAndRecreateListener l : listeners){
			rrFactory.addRuinAndRecreateListener(l);
		}
		return rrFactory.createAlgorithm(vrp, new SingleDepotInitialSolutionFactoryImpl().createInitialSolution(vrp),vrp.getVehicleType().capacity);
	}

	public void registerListener(RuinAndRecreateListener listener) {
		listeners.add(listener);
	}
	
	

}
