package playground.toronto.analysis.handlers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class DwellTimeHandler implements VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler,
	PersonEntersVehicleEventHandler{

	private Vehicles vehicles;
	private HashMap<Id, Double> waitingVehicles;
	private HashMap<Id, Integer> boardings;
	private HashMap<Id, HashSet<Tuple<Integer, Double>>> XYbyVehType;
	
	public DwellTimeHandler(Vehicles veh){
		this.vehicles = veh;
		this.waitingVehicles = new HashMap<Id, Double>();
		this.boardings = new HashMap<Id, Integer>();
		this.XYbyVehType = new HashMap<Id, HashSet<Tuple<Integer,Double>>>();
	}
	
	public static void main(String[] args){
		String eventsFile = args[0];
		String vehiclesFile = args[1];
		String outputChart = args[2];
		
		Vehicles vehiclePopulation = VehicleUtils.createVehiclesContainer();
		
		new VehicleReaderV1(vehiclePopulation).readFile(vehiclesFile);
		DwellTimeHandler dwth = new DwellTimeHandler(vehiclePopulation);
		
		EventsManager em = EventsUtils.createEventsManager();
		em.addHandler(dwth);
	
		MatsimEventsReader reader = new MatsimEventsReader(em);
		reader.readFile(eventsFile);
		
		System.out.println("Done reading events.");
		
		dwth.getChart().saveAsPng(outputChart, 1024, 768);
		
	}
	
	
	public XYScatterChart getChart(){
		XYScatterChart chart = new XYScatterChart("Dwell times by vehicle type", "# of boardings", "dwell time (sec)");
		for (Entry<Id, HashSet<Tuple<Integer, Double>>> e : this.XYbyVehType.entrySet()){
			double[] xs = new double[e.getValue().size()];
			double[] ys = new double[e.getValue().size()];
			int i = 0;
			for (Tuple<Integer, Double> t : e.getValue()){
				xs[i] = t.getFirst();
				ys[i] = t.getSecond();
				i++;
			}
			
			chart.addSeries(e.getKey().toString(), xs, ys);
		}
		return chart;
	}
	
	private Id getVehicleType(Id vid){
		return this.vehicles.getVehicles().get(vid).getType().getId();
	}
	
	@Override
	public void reset(int iteration) {
		this.waitingVehicles = new HashMap<Id, Double>();
		this.boardings = new HashMap<Id, Integer>();
		this.XYbyVehType = new HashMap<Id, HashSet<Tuple<Integer,Double>>>();
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.waitingVehicles.containsKey(event.getVehicleId()))
			this.boardings.put(event.getVehicleId(), this.boardings.get(event.getVehicleId()) + 1);
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		Id vid = event.getVehicleId();
		double dwellTime = event.getTime() - this.waitingVehicles.get(vid);
		int load = this.boardings.get(vid);
		Tuple<Integer, Double> tup = new Tuple<Integer, Double>(load, dwellTime);
		
		Id vtype = getVehicleType(vid);
		if (!this.XYbyVehType.containsKey(vtype)) this.XYbyVehType.put(vtype, new HashSet<Tuple<Integer,Double>>());
		this.XYbyVehType.get(vtype).add(tup);
		
		this.waitingVehicles.remove(event.getVehicleId());
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		this.waitingVehicles.put(event.getVehicleId(), event.getTime());
		this.boardings.put(event.getVehicleId(), 0);
	}

}
