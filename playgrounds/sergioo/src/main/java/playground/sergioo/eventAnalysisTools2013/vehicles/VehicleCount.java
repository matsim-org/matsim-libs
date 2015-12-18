package playground.sergioo.eventAnalysisTools2013.vehicles;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class VehicleCount implements VehicleEntersTrafficEventHandler, PersonArrivalEventHandler  {

	private static Integer[] numCars;
	private static Integer[] numCarsCh;
	private static Integer[] numCarsTo;
	private static Integer[] numBuses;
	private static Integer[] numTrucks;
	private static Integer[] numDCars;
	private static Integer[] numDCarsCh;
	private static Integer[] numDCarsTo;
	private static Integer[] numDBuses;
	private static Integer[] numDTrucks;
	private int timeInterval;
	private Collection<Id<Person>> carAgents = new ArrayList<Id<Person>>();
	private Collection<Id<Person>> touAgents = new ArrayList<Id<Person>>();
	private Collection<Id<Person>> chAgents = new ArrayList<Id<Person>>();
	
	public VehicleCount(int totalTime, int timeInterval) {
		int numSlots = totalTime/timeInterval;
		this.timeInterval = timeInterval;
		numCars = new Integer[numSlots];
		numCarsCh = new Integer[numSlots];
		numCarsTo = new Integer[numSlots];
		numBuses = new Integer[numSlots];
		numTrucks = new Integer[numSlots];
		numDCars = new Integer[numSlots];
		numDCarsCh = new Integer[numSlots];
		numDCarsTo = new Integer[numSlots];
		numDBuses = new Integer[numSlots];
		numDTrucks = new Integer[numSlots];
		for(int i=0; i<totalTime; i+=timeInterval) {
			numDCars[i/timeInterval] = 0;
			numDCarsCh[i/timeInterval] = 0;
			numDCarsTo[i/timeInterval] = 0;
			numDBuses[i/timeInterval] = 0;
			numDTrucks[i/timeInterval] = 0;
		}
	}

	@Override
	public void reset(int iteration) {
		
	}

	/*@Override
	public void handleEvent(LinkEnterEvent event) {
		int slot = (int)event.getTime()/timeInterval;
		if(slot==numCars.length)
			slot--;
		if(event.getDriverId().toString().startsWith("pt")) {
			Integer numBus = numBuses[slot];
			if(numBus==null)
				numBus = 0;
			numBuses[slot].put(event.getLinkId(), numBus+1);
		}
		else if(event.getDriverId().toString().startsWith("stTG")) {
			Integer numTruck = numTrucks[slot];
			if(numTruck==null)
				numTruck = 0;
			numTrucks[slot].put(event.getLinkId(), numTruck+1);
		}
		else if(event.getDriverId().toString().startsWith("stTC")) {
			Integer numCarCh = numCarsCh[slot];
			if(numCarCh==null)
				numCarCh = 0;
			numCarsCh[slot].put(event.getLinkId(), numCarCh+1);
		}
		else if(event.getDriverId().toString().startsWith("stTT")) {
			Integer numCarTo = numCarsTo[slot];
			if(numCarTo==null)
				numCarTo = 0;
			numCarsTo[slot].put(event.getLinkId(), numCarTo+1);
		}
		else {
			Integer numCar = numCars[slot];
			if(numCar==null)
				numCar = 0;
			numCars[slot].put(event.getLinkId(), numCar+1);
		}
	}*/

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		int slot = (int)event.getTime()/timeInterval;
		if(slot==numCars.length)
			slot--;
		if(event.getPersonId().toString().startsWith("pt")) {
			Integer numBus = numBuses[slot];
			if(numBus==null)
				numBus = slot==0?0:numBuses[slot-1]==null?0:numBuses[slot-1];
			numBuses[slot]=numBus-1;
		}
		else if(event.getPersonId().toString().startsWith("stTG")) {
			Integer numTruck = numTrucks[slot];
			if(numTruck==null)
				numTruck = slot==0?0:numTrucks[slot-1]==null?0:numTrucks[slot-1];
			numTrucks[slot]=numTruck-1;
		}
		else if(event.getPersonId().toString().startsWith("stTC")) {
			if(chAgents.contains(event.getPersonId())) {
				Integer numCarCh = numCarsCh[slot];
				if(numCarCh==null)
					numCarCh = slot==0?0:numCarsCh[slot-1]==null?0:numCarsCh[slot-1];
				numCarsCh[slot]=numCarCh-1;
				chAgents.remove(event.getPersonId());
			}
		}
		else if(event.getPersonId().toString().startsWith("stTT")) {
			if(touAgents.contains(event.getPersonId())) {
				Integer numCarTo = numCarsTo[slot];
				if(numCarTo==null)
					numCarTo = slot==0?0:numCarsTo[slot-1]==null?0:numCarsTo[slot-1];
				numCarsTo[slot]=numCarTo-1;
				touAgents.remove(event.getPersonId());
			}
		}
		else {
			if(carAgents.contains(event.getPersonId())) {
				Integer numCar = numCars[slot];
				if(numCar==null)
					numCar = slot==0?0:numCars[slot-1]==null?0:numCars[slot-1];
				numCars[slot]=numCar-1;
				carAgents.remove(event.getPersonId());
			}
		}
	}
	
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		int slot = (int)event.getTime()/timeInterval;
		if(slot==numCars.length)
			slot--;
		if(event.getPersonId().toString().startsWith("pt")) {
			Integer numBus = numBuses[slot];
			if(numBus==null)
				numBus = slot==0?0:numBuses[slot-1]==null?0:numBuses[slot-1];
			numBuses[slot]=numBus+1;
			numDBuses[slot]++;
		}
		else if(event.getPersonId().toString().startsWith("stTG")) {
			Integer numTruck = numTrucks[slot];
			if(numTruck==null)
				numTruck = slot==0?0:numTrucks[slot-1]==null?0:numTrucks[slot-1];
			numTrucks[slot]=numTruck+1;
			numDTrucks[slot]++;
		}
		else if(event.getPersonId().toString().startsWith("stTC")) {
			Integer numCarCh = numCarsCh[slot];
			if(numCarCh==null)
				numCarCh = slot==0?0:numCarsCh[slot-1]==null?0:numCarsCh[slot-1];
			numCarsCh[slot]=numCarCh+1;
			numDCarsCh[slot]++;
			chAgents.add(event.getPersonId());
		}
		else if(event.getPersonId().toString().startsWith("stTT")) {
			Integer numCarTo = numCarsTo[slot];
			if(numCarTo==null)
				numCarTo = slot==0?0:numCarsTo[slot-1]==null?0:numCarsTo[slot-1];
			numCarsTo[slot]=numCarTo+1;
			numDCarsTo[slot]++;
			touAgents.add(event.getPersonId());
		}
		else {
			Integer numCar = numCars[slot];
			if(numCar==null)
				numCar = slot==0?0:numCars[slot-1]==null?0:numCars[slot-1];
			numCars[slot]=numCar+1;
			numDCars[slot]++;
			carAgents.add(event.getPersonId());
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(args[0]);
		EventsManager events = EventsUtils.createEventsManager();
		int totalTime = new Integer(args[3]), timeInterval = new Integer(args[4]);
		events.addHandler(new VehicleCount(totalTime, timeInterval));
		new MatsimEventsReader(events).readFile(args[1]);
		/*PrintWriter writer = new PrintWriter(args[2]);
		writer.println("time, link id, number of cars, number of cars check, number of cars tourism, number of buses, number of trucks");
		for(int i=0; i<totalTime; i+=timeInterval) {
			int slot = i/timeInterval;
			for(Link link:scenario.getNetwork().getLinks().values()) {
				Integer numCar = numCars[slot].get(link.getId());
				Integer numCarCh = numCarsCh[slot].get(link.getId());
				Integer numCarTo = numCarsTo[slot].get(link.getId());
				Integer numBus = numBuses[slot].get(link.getId());
				Integer numTruck = numTrucks[slot].get(link.getId());
				writer.println(i+","+link.getId()+","+(numCar==null?0:numCar*4)+","+(numCarCh==null?0:numCarCh*4)+","+(numCarTo==null?0:numCarTo*4)+","+(numBus==null?0:numBus)+","+(numTruck==null?0:numTruck*4));
			}
		}
		writer.close();*/
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for(int i=0; i<totalTime; i+=timeInterval) {
			int slot = i/timeInterval;
			int allArray = 0, checkArray = 0, touristArray = 0, carArray = 0, truckArray = 0, busArray=0;
			//for(Link link:scenario.getNetwork().getLinks().values()) {
				checkArray += numCarsCh[slot]==null?0:numCarsCh[slot]*4;
				touristArray += numCarsTo[slot]==null?0:numCarsTo[slot]*4;
				carArray += numCars[slot]==null?0:numCars[slot]*4;
				truckArray += numTrucks[slot]==null?0:numTrucks[slot]*4;
				busArray += numBuses[slot]==null?0:numBuses[slot];
			//}
			allArray += checkArray+touristArray+carArray+truckArray+busArray;
			dataset.addValue(checkArray, "Check", Integer.toString(slot));
			dataset.addValue(touristArray, "Tourist", Integer.toString(slot));
			dataset.addValue(carArray, "Car", Integer.toString(slot));
			dataset.addValue(truckArray, "Truck", Integer.toString(slot));
			dataset.addValue(busArray, "Bus", Integer.toString(slot));
			//dataset.addValue(allArray, "All", Integer.toString(slot));
		}
		JFreeChart chart = ChartFactory.createLineChart( "Number of vehicles 25%", "time", "number", dataset, PlotOrientation.VERTICAL, true, false, false);
		JFrame frameTou = new JFrame();
		frameTou.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frameTou.add(new ChartPanel(chart));
		frameTou.pack();
		frameTou.setVisible(true);
		DefaultCategoryDataset datasetD = new DefaultCategoryDataset();
		for(int i=0; i<totalTime; i+=timeInterval) {
			int slot = i/timeInterval;
			int allArray = 0, checkArray = 0, touristArray = 0, carArray = 0, truckArray = 0, busArray=0;
			//for(Link link:scenario.getNetwork().getLinks().values()) {
				checkArray += numDCarsCh[slot]==null?0:numDCarsCh[slot]*4;
				touristArray += numDCarsTo[slot]==null?0:numDCarsTo[slot]*4;
				carArray += numDCars[slot]==null?0:numDCars[slot]*4;
				truckArray += numDTrucks[slot]==null?0:numDTrucks[slot]*4;
				busArray += numDBuses[slot]==null?0:numDBuses[slot];
			//}
			allArray += checkArray+touristArray+carArray+truckArray+busArray;
			datasetD.addValue(checkArray, "Check", Integer.toString(slot));
			datasetD.addValue(touristArray, "Tourist", Integer.toString(slot));
			datasetD.addValue(carArray, "Car", Integer.toString(slot));
			datasetD.addValue(truckArray, "Truck", Integer.toString(slot));
			datasetD.addValue(busArray, "Bus", Integer.toString(slot));
			//dataset.addValue(allArray, "All", Integer.toString(slot));
		}
		JFreeChart chartD = ChartFactory.createLineChart( "Number of departures 25%", "time", "number", datasetD, PlotOrientation.VERTICAL, true, false, false);
		JFrame frameTouD = new JFrame();
		frameTouD.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frameTouD.add(new ChartPanel(chartD));
		frameTouD.pack();
		frameTouD.setVisible(true);
	}

}
