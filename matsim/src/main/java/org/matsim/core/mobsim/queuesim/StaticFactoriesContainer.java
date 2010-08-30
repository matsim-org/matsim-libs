package org.matsim.core.mobsim.queuesim;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.ptproject.qsim.helpers.DefaultPersonDriverAgent;
import org.matsim.ptproject.qsim.helpers.QSimTimer;
import org.matsim.ptproject.qsim.helpers.QVehicleImpl;
import org.matsim.ptproject.qsim.interfaces.QVehicle;
import org.matsim.ptproject.qsim.interfaces.SimTimerI;
import org.matsim.vehicles.Vehicle;

public class StaticFactoriesContainer {

	static QVehicle createQueueVehicle(Vehicle vehicle) {
	//		return new QueueVehicle(basicVehicle);
			return new QVehicleImpl(vehicle);
		}

	static QVehicle createQueueVehicle(Vehicle vehicle, double sizeInEquivalents) {
	//		return new QueueVehicle(basicVehicle, sizeInEquivalents);
			return new QVehicleImpl(vehicle, sizeInEquivalents );
		}

	public static PersonDriverAgent createQueuePersonAgent(Person p, QueueSimulation simulation) {
//		return new QueuePersonAgent(p, simulation);
		return new DefaultPersonDriverAgent(p, simulation);
	}

	public static SimTimerI createSimulationTimer(double stepSize) {
//		return new SimulationTimer(stepSize);
		return new QSimTimer(stepSize);
	}

}
