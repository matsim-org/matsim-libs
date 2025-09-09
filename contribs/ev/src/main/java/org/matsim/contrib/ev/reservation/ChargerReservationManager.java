package org.matsim.contrib.ev.reservation;

import org.matsim.contrib.common.util.reservation.AbstractReservationManager;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

/**
 * This class is a singleton service that keeps a list of reservations for
 * chargers. It can be used in combination with the
 * ReservationBasedChargingPriority to let vehicle pass on to charging only if
 * they have a proper reservation.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ChargerReservationManager extends AbstractReservationManager<ChargerSpecification, ElectricVehicle> implements IterationStartsListener {

	@Override
	protected int getCapacity(ChargerSpecification charger) {
		return charger.getPlugCount();
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		super.cleanReservations();
	}
}