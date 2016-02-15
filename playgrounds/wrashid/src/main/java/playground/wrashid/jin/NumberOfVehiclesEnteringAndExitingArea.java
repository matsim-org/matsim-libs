package playground.wrashid.jin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.api.core.v01.events.handler.*;

public class NumberOfVehiclesEnteringAndExitingArea {

	public static void main(String[] args) {
		String networkFile = "c:/data/run24/output_network.xml.gz";
		String eventsFile = "c:/data/run24/50.events.xml.gz";
		EventsManager events = EventsUtils.createEventsManager();

		Network network = GeneralLib.readNetwork(networkFile);

		// AreaHandler handler = new AreaHandler(network);
		// events.addHandler(handler);

		IndividualUserHandler handler = new IndividualUserHandler(network);
		events.addHandler(handler);

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);

		reader.parse(eventsFile);

		// handler.printOutput("C:\\tmp\\New folder\\Area1_60SecBin.txt");
		handler.printOutput();
	}

	public static boolean isInArea(Coord coord) {
		// Coord circleCenter = new Coord(683243.7, 247459.2);
		// double radius = 700;

		 Coord circleCenter=new Coord(682922.588,247474.957);
		 double radius=298;

		if (GeneralLib.getDistance(coord, circleCenter) < radius) {
			return true;
		}

		return false;
	}

	private static class IndividualUserHandler
			implements LinkEnterEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler,
			VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
		private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler() ;
		
		HashMap<Id<Person>, ParkingAgentEvent> parkingAgentEvent = new HashMap<>();

		LinkedList<ParkingAgentEvent> completedEvents = new LinkedList<>();

		HashMap<Id<Person>, Double> areaEnterTime = new HashMap<>();

		private Network network;

		public IndividualUserHandler(Network network) {
			this.network = network;
		}

		public void printOutput() {
			System.out.println("agentId\tenterAreaTime\tactStartTime\tactEndTime\tleaveAreaTime");
			for (ParkingAgentEvent pae : completedEvents) {
				System.out.print(pae.personId);
				System.out.print("\t");
				System.out.print(pae.enterAreaTime);
				System.out.print("\t");
				System.out.print(pae.actStartTime);
				System.out.print("\t");
				System.out.print(pae.actEndTime);
				System.out.print("\t");
				System.out.print(pae.leaveAreaTime);
				System.out.println("\t");
			}
			for (ParkingAgentEvent pae : parkingAgentEvent.values()) {
				System.out.print(pae.personId);
				System.out.print("\t");
				System.out.print(pae.enterAreaTime);
				System.out.print("\t");
				System.out.print(pae.actStartTime);
				System.out.print("\t");
				System.out.print(pae.actEndTime);
				System.out.print("\t");
				System.out.print(pae.leaveAreaTime);
				System.out.println("\t");
			}
		}

		@Override
		public void reset(int iteration) {
			this.delegate.reset(iteration);
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			if (event.getLegMode().equals("car")) {
				Coord linkCoord = network.getLinks().get(event.getLinkId()).getCoord();
				if (isInArea(linkCoord)) {
					if (!parkingAgentEvent.containsKey(event.getPersonId())) {
						parkingAgentEvent.put(event.getPersonId(), new ParkingAgentEvent(event.getPersonId()));
					}

					ParkingAgentEvent pae = parkingAgentEvent.get(event.getPersonId());
					pae.actEndTime = event.getTime();
				}
			}
		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			if (event.getLegMode().equals("car")) {
				Coord linkCoord = network.getLinks().get(event.getLinkId()).getCoord();
				if (isInArea(linkCoord)) {

					if (parkingAgentEvent.containsKey(event.getPersonId())) {
						// multiple activities in same area => log/complete
						// previous act time
						ParkingAgentEvent pae = parkingAgentEvent.get(event.getPersonId());
						completedEvents.add(pae);
						parkingAgentEvent.remove(event.getPersonId());
						areaEnterTime.remove(event.getPersonId());
					}

					ParkingAgentEvent pae = new ParkingAgentEvent(event.getPersonId());
					if (areaEnterTime.containsKey(event.getPersonId())) {
						pae.enterAreaTime = areaEnterTime.get(event.getPersonId());
						areaEnterTime.remove(event.getPersonId());
					}
					pae.actStartTime = event.getTime();
					parkingAgentEvent.put(event.getPersonId(), pae);
				}
			}
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Id<Person> driverId = this.delegate.getDriverOfVehicle( event.getVehicleId() ) ;
			
			Coord linkCoord = network.getLinks().get(event.getLinkId()).getCoord();
			if (isInArea(linkCoord)) {
				if (!areaEnterTime.containsKey(driverId)) {
					areaEnterTime.put(driverId, event.getTime());
				}
			} else if (parkingAgentEvent.containsKey(driverId)) {
				// leaving area
				ParkingAgentEvent pae = parkingAgentEvent.get(driverId);
				pae.leaveAreaTime = event.getTime();
				completedEvents.add(pae);
				parkingAgentEvent.remove(driverId);
				areaEnterTime.remove(driverId);
			}
		}

		private static class ParkingAgentEvent {
			
			public ParkingAgentEvent(Id<Person> personId){
				this.personId = personId;
			}
			
			Id<Person> personId;
			double enterAreaTime = -1;
			double actStartTime = -1;
			double actEndTime = -1;
			double leaveAreaTime = -1;
		}

		private static class ThroughTrafficEvent {
			Id<Person> personId;
			double enterAreaTime;
			double leaveAreaTime;
		}

		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
			this.delegate.handleEvent(event);
		}
		@Override
		public void handleEvent(VehicleLeavesTrafficEvent event) {
			this.delegate.handleEvent(event);
		}

	}

	private static class AreaHandler implements ActivityEndEventHandler, LinkEnterEventHandler,
	VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
		private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler() ;

		int binSizeInSeconds = 60;
		int arraySize = 200 * 60 * 60 / binSizeInSeconds;
		double[] inFlow = new double[arraySize];
		double[] outFlow = new double[arraySize];

		private Network network;
		HashSet<Id> vehicleInArea = new HashSet<Id>();
		ArrayList<String> output = new ArrayList<String>();

		public AreaHandler(Network network) {
			this.network = network;
		}

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub

		}

		public void printOutput(String fileName) {
			int maxInFlowIndex = inFlow.length - 1;
			int maxOutFlowIndex = outFlow.length - 1;

			while (inFlow[maxInFlowIndex] != 0) {
				maxInFlowIndex--;
			}

			while (outFlow[maxOutFlowIndex] != 0) {
				maxOutFlowIndex--;
			}

			output.add("timeBin\tInfow\tOutflow");

			for (int i = 0; i <= Math.max(maxInFlowIndex, maxOutFlowIndex); i++) {
				output.add(i + "\t" + inFlow[i] + "\t" + outFlow[i]);
			}

			GeneralLib.writeList(output, fileName);
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Id<Person> driverId = delegate.getDriverOfVehicle( event.getVehicleId() ) ;
			
			Coord linkCoord = network.getLinks().get(event.getLinkId()).getCoord();

			// leaving area
			if (vehicleInArea.contains(driverId) && !isInArea(linkCoord)) {
				outFlow[(int) Math.round(event.getTime() / binSizeInSeconds)]++;
				// output.add(Math.round(event.getTime()) + "\t" + "-1");
			}

			// entering area
			if (!vehicleInArea.contains(driverId) && isInArea(linkCoord)) {
				inFlow[(int) Math.round(event.getTime() / binSizeInSeconds)]++;
				// output.add(Math.round(event.getTime()) + "\t" + "+1");
			}

			// update vehicleInArea
			if (isInArea(linkCoord)) {
				vehicleInArea.add(driverId);
			} else {
				vehicleInArea.remove(driverId);
			}
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			Coord linkCoord = network.getLinks().get(event.getLinkId()).getCoord();
			if (isInArea(linkCoord)) {
				vehicleInArea.add(event.getPersonId());
			} else {
				vehicleInArea.remove(event.getPersonId());
			}
		}
		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
			this.delegate.handleEvent(event);
		}
		@Override
		public void handleEvent(VehicleLeavesTrafficEvent event) {
			this.delegate.handleEvent(event);
		}
	}

}
