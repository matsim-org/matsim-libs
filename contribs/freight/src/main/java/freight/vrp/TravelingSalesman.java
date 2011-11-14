package freight.vrp;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.TourBuilder;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreate;
import org.matsim.contrib.freight.vrp.algorithms.rr.api.RuinAndRecreateListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.factories.RuinAndRecreateWithTimeWindowsFactory;
import org.matsim.contrib.freight.vrp.api.SingleDepotVRP;
import org.matsim.contrib.freight.vrp.basics.SingleDepotInitialSolutionFactoryImpl;
import org.matsim.contrib.freight.vrp.basics.TourActivity;
import org.matsim.core.basic.v01.IdImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TravelingSalesman {
	
	private SingleDepotVRP singleDepotVRP;
	
	private Collection<Tour> solution;
	
	private List<RuinAndRecreateListener> listeners = new ArrayList<RuinAndRecreateListener>();
	
	public TravelingSalesman(SingleDepotVRP singleDepotVRP) {
		super();
		this.singleDepotVRP = singleDepotVRP;
	}
	
	public void run(){
		RuinAndRecreate algo = createAlgo(singleDepotVRP);
		algo.run();
		solution = makeTours(algo.getSolution());
	}
	
	public Collection<Tour> getSolution(){
		return solution;
	}
	
	private Collection<Tour> makeTours(Collection<org.matsim.contrib.freight.vrp.basics.Tour> solution) {
		Collection<Tour> tours = new ArrayList<Tour>();
		for(org.matsim.contrib.freight.vrp.basics.Tour t : solution){
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
		RuinAndRecreateWithTimeWindowsFactory factory = new RuinAndRecreateWithTimeWindowsFactory(5,50);
		for(RuinAndRecreateListener l : listeners){
			factory.addRuinAndRecreateListener(l);
		}
		return factory.createAlgorithm(vrp, new SingleDepotInitialSolutionFactoryImpl().createInitialSolution(vrp),vrp.getVehicleType().capacity);
	}

	public void registerListener(RuinAndRecreateListener listener) {
		listeners.add(listener);
	}
	
	

}
