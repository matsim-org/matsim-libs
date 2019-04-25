package org.matsim.contrib.carsharing.control.listeners;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.manager.demand.AgentRentals;
import org.matsim.contrib.carsharing.manager.demand.DemandHandler;
import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

/**
 * 
 * @author balac
 */
public class CarsharingListener implements IterationEndsListener {

	@Inject
	private MatsimServices controler;
	@Inject
	private DemandHandler demandHandler;
	@Inject
	private CarsharingSupplyInterface carsharingSupply;

	ArrayList<Integer> rentalsPerIteration = new ArrayList<>();

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		int numberOfRentals = 0;

		Map<Id<Person>, AgentRentals> agentRentalsMap = demandHandler.getAgentRentalsMap();
		final BufferedWriter outLink = IOUtils.getBufferedWriter(
				this.controler.getControlerIO().getIterationFilename(event.getIteration(), "CS.txt"));
		try {
			outLink.write(
					"personID,carsharingType,startTime,endTIme,startLink,pickupLink,dropoffLink,endLink,startCoordX,startCoordY,"
							+ "pickupCoordX,pickupCoordY,dropoffCoordX,dropoffCoordY,endCoordX,endCoordY,distance,"
							+ "inVehicleTime,accessTime,egressTime,vehicleID," + "companyID,vehicleType");
			outLink.newLine();

			for (Id<Person> personId : agentRentalsMap.keySet()) {

				for (RentalInfo i : agentRentalsMap.get(personId).getArr()) {
					CSVehicle vehicle = this.carsharingSupply.getAllVehicles().get(i.getVehId().toString());
					numberOfRentals++;
					outLink.write(
							personId + "," + i.toString() + "," + vehicle.getCompanyId() + "," + vehicle.getType());
					outLink.newLine();
				}

			}
			rentalsPerIteration.add(numberOfRentals);

			outLink.flush();
			outLink.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (event.getIteration() == controler.getConfig().controler().getLastIteration()) {
			final BufferedWriter outLinkStats = IOUtils
					.getBufferedWriter(this.controler.getControlerIO().getOutputFilename("CS.txt"));
			try {
				outLinkStats.write("iteration,numberOfRentals");

				outLinkStats.newLine();
				int k = 0;
				for (Integer i : rentalsPerIteration) {
					outLinkStats.write(k + "," + i);
					k++;
					outLinkStats.newLine();
				}

				outLinkStats.flush();
				outLinkStats.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}
}
