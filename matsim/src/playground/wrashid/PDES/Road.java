package playground.wrashid.PDES;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;





public class Road extends SimUnit {
	private int belongsToMessageExecutorThreadId=0;
	public static HashMap<String, Road> allRoads;
	private Link link;
	private LinkedList<Double> gap; // see enterRequest for a detailed
	// description of variable 'gap'
	//private LinkedList<Vehicle> interestedInEnteringRoad = new LinkedList<Vehicle>();
	private double timeOfLastEnteringVehicle = Double.MIN_VALUE;
	private double timeOfLastLeavingVehicle = Double.MIN_VALUE;

	// the inverseFlowCapacity is simple the inverse
	// of the capacity meaning, the minimal time between two cars
	// entering/leaving the road
	private double inverseInFlowCapacity = 0;
	private double inverseOutFlowCapacity = 0;

	// also we keep track of the number of cars on the road, there is a problem:
	// if we schedule a car, that it may enter the road at time x, then must
	// kept track of the number of those cars, so that we can decide in
	// enterRequest
	// how much space we have
	private int noOfCarsPromisedToEnterRoad = 0;

	// how many cars can be parked on the street

	private long maxNumberOfCarsOnRoad = 0;

	// the time it takes for a gap to get to the back of the road
	private double gapTravelTime = 0;

	private LinkedList<Vehicle> carsOnTheRoad = new LinkedList<Vehicle>();
	private LinkedList<Double> earliestDepartureTimeOfCar = new LinkedList<Double>();

	
	private LinkedList<DeadlockPreventionMessage> deadlockPreventionMessages= new LinkedList<DeadlockPreventionMessage>();
	private PriorityQueue<DeadlockPreventionMessage> testDeadlockPreventionMessages= new PriorityQueue<DeadlockPreventionMessage>();
	
	Lock lock=new ReentrantLock();
	//public ConcurrentLinkedQueue<Message> waitingOnLock=new ConcurrentLinkedQueue<Message>();
	private LinkedList<Message> waitingOnLock=new LinkedList<Message>();
	//FairLock lock=new FairLock();
	
	// private double oldestUnusedGapTime=Double.MIN_VALUE;

	public Road(Scheduler scheduler, Link link) {
		super(scheduler);
		this.link = link;
		// random cut
		belongsToMessageExecutorThreadId = new Random().nextInt(SimulationParameters.numberOfMessageExecutorThreads)+1;
		
		/*
		 // two processor cut
		if (getXCoordinate()>680237){
			belongsToMessageExecutorThreadId = 1;
		} else {
			belongsToMessageExecutorThreadId = 2;
		}
		*/
		/*
		// 4 processor cut
		if (getXCoordinate()>=695638){
			belongsToMessageExecutorThreadId = 1;
		} else if (getXCoordinate()<=651666) {
			belongsToMessageExecutorThreadId = 2;
		} else if (getXCoordinate()>651666 && getXCoordinate()<680237){
			belongsToMessageExecutorThreadId = 3;
		} else if (getXCoordinate()>=680237 && getXCoordinate()<695638){
			belongsToMessageExecutorThreadId = 4;
		}
		*/
		
		maxNumberOfCarsOnRoad = Math.round(link.getLength()
				* link.getLanesAsInt(SimulationParameters.linkCapacityPeriod)
				* SimulationParameters.storageCapacityFactor
				/ SimulationParameters.carSize);

		// this is an assumption made her: a road must at least have the space
		// capacity to park one car
		// so that the backward propagation of gaps can work
		if (maxNumberOfCarsOnRoad == 0) {
			maxNumberOfCarsOnRoad = 1;
		}

		double maxInverseInFlowCapacity = 3600 / (SimulationParameters.minimumInFlowCapacity * SimulationParameters.flowCapacityFactor * link.getLanesAsInt(SimulationParameters.linkCapacityPeriod));

		inverseOutFlowCapacity = 1 / (link
				.getFlowCapacity(SimulationParameters.linkCapacityPeriod) * SimulationParameters.flowCapacityFactor);

		if (inverseOutFlowCapacity > maxInverseInFlowCapacity) {
			inverseInFlowCapacity = maxInverseInFlowCapacity;
		} else {
			inverseInFlowCapacity = inverseOutFlowCapacity;
		}

		gapTravelTime = link.getLength() / SimulationParameters.gapTravelSpeed;

		// gap must be initialized to null because of the application logic
		gap = null;

		if (link.getId().toString().equalsIgnoreCase("110915")) {
			//System.out.println("sdfa");
		}
		
		if (scheduler.minInverseInOutflowCapacity>inverseOutFlowCapacity){
			scheduler.minInverseInOutflowCapacity=inverseOutFlowCapacity;
		}
		if (scheduler.minInverseInOutflowCapacity>inverseInFlowCapacity){
			scheduler.minInverseInOutflowCapacity=inverseInFlowCapacity;
		}
		
	}
	


	@Override
	public void finalize() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleMessage(Message m) {
		// TODO Auto-generated method stub

	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub

	}

	 public void leaveRoad(Vehicle vehicle) {
		//System.out.println("vehicleId:"+vehicle.getOwnerPerson().getId().toString() + ";linkId:"+this.getLink().getId().toString());
		
		 if (carsOnTheRoad.isEmpty()){
			 System.out.println();
		 }
		 
		assert(carsOnTheRoad.getFirst()==vehicle):"road:"+link.getId()+  " - " + this + " - " + lock; // TODO: uncomment this, and find out, why it produces a problem with test6
	 	
		
		
		carsOnTheRoad.removeFirst();
		earliestDepartureTimeOfCar.removeFirst();
		timeOfLastLeavingVehicle = MessageExecutor.getSimTime();

		if (link.getId().toString().equalsIgnoreCase("110915")) {
			//System.out.println("leave road: " + Scheduler.simTime);
		}

		// the next car waiting for entering the road should now be alloted a
		// time for entering the road
		if (deadlockPreventionMessages.size() > 0) {
			
			assert(deadlockPreventionMessages.getFirst()==testDeadlockPreventionMessages.peek()):"inconsistency";
			assert(deadlockPreventionMessages.size()==testDeadlockPreventionMessages.size()):"inconsistency";
			
			DeadlockPreventionMessage m=null;
			Vehicle nextVehicle=null;
			try{
				m=deadlockPreventionMessages.poll();
				testDeadlockPreventionMessages.poll();
				nextVehicle = m.vehicle;
			} catch (Exception e){
				System.out.println("road:"+link.getId()+  " - " + this + "  -  " + "vehicle " + nextVehicle.getOwnerPerson().getId() + " - " + lock);
			}

			
			scheduler.unschedule(m);
			
			double nextAvailableTimeForEnteringStreet = Math.max(
					timeOfLastEnteringVehicle + inverseInFlowCapacity,
					MessageExecutor.getSimTime() + gapTravelTime);

			noOfCarsPromisedToEnterRoad++;

			nextVehicle.scheduleEnterRoadMessage(
					nextAvailableTimeForEnteringStreet, this);
		} else {
			if (gap != null) {
				// as long as the road is not full once, there is no need to
				// keep track of the gaps
				gap.add(MessageExecutor.getSimTime() + gapTravelTime);

				// if no one is interested in entering this road (precondition)
				// and there are no cars on the road, then reset gap
				// (this is required, for enterRequest to function properly)
				if (carsOnTheRoad.size() == 0) {
					gap = null;
				}
			}
		}

		// tell the car behind the fist car (which is the first car now), when
		// it reaches the end of the read
		if (carsOnTheRoad.size() > 0) {
			Vehicle nextVehicle = carsOnTheRoad.getFirst();
			double nextAvailableTimeForLeavingStreet = Math.max(
					earliestDepartureTimeOfCar.getFirst(),
					timeOfLastLeavingVehicle + inverseOutFlowCapacity);
			nextVehicle.scheduleEndRoadMessage(
					nextAvailableTimeForLeavingStreet, this);
		}

	}

	// returns the time, when the car reaches the end of the road
	// TODO: instead of returning the scheduling time, just schedule messages
	// here...
	public void enterRoad(Vehicle vehicle) {

		// vehicle.leavePreviousRoad();

		if (link.getId().toString().equalsIgnoreCase("110915")) {
			//System.out.println("enter Road:" + Scheduler.simTime);
		}

		double nextAvailableTimeForLeavingStreet = Double.MIN_VALUE;
		nextAvailableTimeForLeavingStreet = MessageExecutor.getSimTime()
				+ link.getLength()
				/ link.getFreespeed(SimulationParameters.linkCapacityPeriod);

		noOfCarsPromisedToEnterRoad--;
		carsOnTheRoad.add(vehicle);

		// need to remove this assertion because of deadlock prevention
		//assert maxNumberOfCarsOnRoad >= carsOnTheRoad.size() : "There are more cars on the road, than its capacity!";
		earliestDepartureTimeOfCar.add(nextAvailableTimeForLeavingStreet);

		// if we are in the front of the queue, then we can just drive with free
		// speed
		// to the front and have to have at least inverseFlowCapacity
		// time-distance to the
		// previous car
		if (carsOnTheRoad.size() == 1) {
			nextAvailableTimeForLeavingStreet = Math.max(
					nextAvailableTimeForLeavingStreet, timeOfLastLeavingVehicle
							+ inverseOutFlowCapacity);
			vehicle.scheduleEndRoadMessage(nextAvailableTimeForLeavingStreet,
					this);
		} else {
			// this car is not the front car in the street queue
			// when the cars infront of the current car leave the street and
			// this car becomes the
			// front car, it will be waken up...
		}

	}

	// gives back the time, when the car can enter the road
	// it returns -1, if there is no space in the street and the car will be
	// handled later
	// => TODO: remove the return value. Scheduling the car etc. should be done
	// by the vehicle
	public void enterRequest(Vehicle vehicle) {
		double nextAvailableTimeForEnteringStreet = Double.MIN_VALUE;
		
		// attention: do not use in multi thread solution (might return false result)
		SimulationParameters.sumXCoordinate+=getXCoordinate();
		SimulationParameters.noOfCars++;
		
		if (getXCoordinate()>680237){
			SimulationParameters.sumXCoordinateRight+=getXCoordinate();
			SimulationParameters.noOfCarsRight++;
		} else {
			SimulationParameters.sumXCoordinateLeft+=getXCoordinate();
			SimulationParameters.noOfCarsLeft++;
		}
		
		
		
		// assert maxNumberOfCarsOnRoad >= carsOnTheRoad.size() : "There are
		// more cars on the road, than its capacity!";
		// This assert has been commented out for deadlock prevention:
		// If a car waits too long, it is alloud to enter the road.
		//assert maxNumberOfCarsOnRoad >= carsOnTheRoad.size()
			//	+ noOfCarsPromisedToEnterRoad : "You promised too many cars, that they can enter the street!";

		if (link.getId().toString().equalsIgnoreCase("110915")) {
			//System.out.print("enterRequest");
		}

		if (carsOnTheRoad.size() + noOfCarsPromisedToEnterRoad < maxNumberOfCarsOnRoad) {

			if (link.getId().toString().equalsIgnoreCase("110915")) {
				//System.out.println("normal");
			}

			// - check, if the gap needs to be considered for entering the road
			// - we can find out, the time since when we have a free road for
			// entrance for sure:
			//   

			// the gap queue will only be empty in the beginning
			double arrivalTimeOfGap = Double.MIN_VALUE;
			// if the road has been full recently then find out, when the next
			// gap arrives
			if (gap != null && gap.size() > 0) {
				arrivalTimeOfGap = gap.remove();
			}

			noOfCarsPromisedToEnterRoad++;
			nextAvailableTimeForEnteringStreet = Math.max(Math.max(
					timeOfLastEnteringVehicle + inverseInFlowCapacity,
					MessageExecutor.getSimTime()), arrivalTimeOfGap);

			timeOfLastEnteringVehicle = nextAvailableTimeForEnteringStreet;
			vehicle.scheduleEnterRoadMessage(
					nextAvailableTimeForEnteringStreet, this);
		} else {

			if (link.getId().toString().equalsIgnoreCase("110915")) {
				//System.out.println("road full: " + Scheduler.simTime);
			}

			// at the moment, the road is full and no gap is available
			// => put this car into the interestedInEnteringRoad LinkedList
			// When cars leave the road, a gap slot will eventually be alloted
			// to this car

			// - if the road was empty then create a new queue else empty the
			// old queue
			// As long as the gap is null, the road is not full (and there is no
			// reason to keep track of the gaps => see leaveRoad)
			// But when the road gets full once, we need to start keeping track
			// of the gaps
			// Once the road is empty again, gap is reset to null (see
			// leaveRoad).
			//   
			// The gap variable in only needed for the situation, where the
			// street has been full recently, but the interestedInEnteringRoad
			// is
			// is empty and a new car arrives (or a few). So, if the street is
			// long, it takes time for the gap to come back.
			//
			// As long as interestedInEnteringRoad is not empty, newly generated
			// gaps get used by the new cars (see leaveRoad)
			if (gap == null) {
				gap = new LinkedList<Double>();
			} else {
				gap.clear();
			}

			//interestedInEnteringRoad.add(vehicle);
			
			DeadlockPreventionMessage dpm=null;
			
			// the first car interested in entering a road has to wait 'stuckTime'
			// the car behind has to wait an additional stuckTime (this logic was introduced to adhere the C++ implementation)
			if (deadlockPreventionMessages.size()>0){
				assert(deadlockPreventionMessages. getLast().messageArrivalTime +SimulationParameters.stuckTime>=MessageExecutor.getSimTime());
				//if (deadlockPreventionMessages. getLast().messageArrivalTime +SimulationParameters.stuckTime<MessageExecutor.getSimTime()){
				//	System.out.println();	
				//}
				dpm=vehicle.scheduleDeadlockPreventionMessage(deadlockPreventionMessages.getLast().messageArrivalTime +SimulationParameters.stuckTime, this);
				deadlockPreventionMessages.add(dpm);
				testDeadlockPreventionMessages.add(dpm);
				
			} else {
				dpm=vehicle.scheduleDeadlockPreventionMessage(MessageExecutor.getSimTime()+SimulationParameters.stuckTime, this);
				deadlockPreventionMessages.add(dpm);
				testDeadlockPreventionMessages.add(dpm);
			}
			assert(deadlockPreventionMessages.getFirst()==testDeadlockPreventionMessages.peek()):"inconsistency";
			assert(deadlockPreventionMessages.size()==testDeadlockPreventionMessages.size()):"inconsistency";
			assert(testDeadlockPreventionMessages.contains(dpm)):"inconsistency";
			assert(deadlockPreventionMessages.contains(dpm)):"inconsistency";
			
		}
	}

	public void giveBackPromisedSpaceToRoad() {
		noOfCarsPromisedToEnterRoad--;
	}
	
	public void incrementPromisedToEnterRoad(){
		noOfCarsPromisedToEnterRoad++;
	}

	public Link getLink() {
		return link;
	}



	public void setTimeOfLastEnteringVehicle(double timeOfLastEnteringVehicle) {
		this.timeOfLastEnteringVehicle = timeOfLastEnteringVehicle;
	}
	
	public void removeDeadlockPreventionMessage(DeadlockPreventionMessage dpMessage){
		// this causes a problem with test6, as it the message does not exist
		// TODO: first find out why this happens and then
		
		// TODO: current problem: two different messages (sent by different vehicles)
		// we are sure, that no one removed this message using this method, but rather some different
		// place in the code...
		//if (deadlockPreventionMessages.peek()!=dpMessage){
		//	DeadlockPreventionMessage dpm=deadlockPreventionMessages.peek();
		//	System.out.println();
		//}
		
		// TODO: uncomment assertion again, because this indicates, that there is some out of sync behaviour!!!!!!!!!!
		// Answer: if we use a PriorityQueue instead, the problem would be solved, but the implementation gets harder, because we
		// need the last element (that function is not available with PriorityQueue)
		//assert(deadlockPreventionMessages.peek()==dpMessage):"Inconsitency in logic!!! => this should only be invoked from the handler of this message";
		assert(testDeadlockPreventionMessages.contains(dpMessage)):"inconsistency";
		assert(deadlockPreventionMessages.contains(dpMessage)):"Something is really wrong here";
		
		
		//deadlockPreventionMessages.removeFirst();
		// the code line above was replaced, because of time causality, there could be a problem
		testDeadlockPreventionMessages.remove(dpMessage);
		deadlockPreventionMessages.remove(dpMessage);
	}
	
	/*
	public void removeFromInterestedInEnteringRoad(Vehicle vehicle){
		try{
			//interestedInEnteringRoad.removeFirst();
			// the code line above was replaced, because of time causality, there could be a problem
			interestedInEnteringRoad.remove(vehicle);
		} catch (Exception e){
			System.out.println("road:"+link.getId() + " - " + this + " - " + lock);
		}
	}
	*/

	synchronized public Message peekOfWaitingOnLock(){
		return waitingOnLock.peek();
	}
	
	synchronized public Message pollWaitingOnLock(){
		return waitingOnLock.poll();
	}
	
	synchronized public void addWaitingOnLock(Message m){
		waitingOnLock.add(m);
	}



	public int getBelongsToMessageExecutorThreadId() {
		return belongsToMessageExecutorThreadId;
	}


	public double getXCoordinate(){
		return (link.getFromNode().getCoord().getX()+link.getToNode().getCoord().getX())/2;
	}

	
}
