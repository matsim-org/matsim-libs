package org.matsim.contrib.drt.extension.operations.eshifts.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;
import org.matsim.contrib.evrp.ChargingTask;
import org.matsim.contrib.evrp.ETask;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeOverTask;

import java.util.Map;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STOP;

/**
 * @author nkuehnel / MOIA
 */
public class EDrtShiftChangeoverTaskImpl extends DefaultStayTask implements ShiftChangeOverTask, ETask {

	public static final DrtTaskType TYPE = new DrtTaskType("SHIFT_CHANGEOVER", STOP);

    private final DrtShift shift;
    private final double consumedEnergy;
    private final ChargingTask chargingTask;
    private final OperationFacility facility;

	private final DrtStopTask delegate;


	public EDrtShiftChangeoverTaskImpl(double beginTime, double endTime, Link link,
                                       DrtShift shift, double consumedEnergy,
                                       ChargingTask chargingTask, OperationFacility facility) {
		super(TYPE, beginTime, endTime, link);
		this.delegate = new DefaultDrtStopTask(beginTime, endTime, link);
		this.shift = shift;
        this.consumedEnergy = consumedEnergy;
        this.chargingTask = chargingTask;
        this.facility = facility;
    }

    @Override
    public DrtShift getShift() {
        return shift;
    }

    @Override
    public double getTotalEnergy() {
        return consumedEnergy;
    }

    public ChargingTask getChargingTask() {
        return chargingTask;
    }

    @Override
    public OperationFacility getFacility() {
        return facility;
    }

	@Override
	public Map<Id<Request>, AcceptedDrtRequest> getDropoffRequests() {
		return delegate.getDropoffRequests();
	}

	@Override
	public Map<Id<Request>, AcceptedDrtRequest> getPickupRequests() {
		return delegate.getPickupRequests();
	}

	@Override
	public void addDropoffRequest(AcceptedDrtRequest request) {
		delegate.addDropoffRequest(request);
	}

	@Override
	public void addPickupRequest(AcceptedDrtRequest request) {
		delegate.addPickupRequest(request);
	}

	@Override
	public void removePickupRequest(Id<Request> requestId) {
		delegate.removePickupRequest(requestId);
	}

	@Override
	public void removeDropoffRequest(Id<Request> requestId) {
		delegate.removeDropoffRequest(requestId);
	}
}
