package org.matsim.contrib.drt.extension.operations.operationFacilities;

import org.matsim.contrib.common.util.reservation.AbstractReservationManager;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

/**
 * @author nkuehnel / MOIA
 */
public class OperationFacilityReservationManager extends AbstractReservationManager<OperationFacility, DvrpVehicle> implements IterationStartsListener {

    @Override
    protected int getCapacity(OperationFacility resource) {
        return resource.getCapacity();
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        super.cleanReservations();
    }
}
