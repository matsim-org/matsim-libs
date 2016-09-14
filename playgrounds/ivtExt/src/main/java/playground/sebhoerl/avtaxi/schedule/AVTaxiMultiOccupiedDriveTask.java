package playground.sebhoerl.avtaxi.schedule;

import java.util.LinkedList;
import java.util.List;

import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DriveTaskImpl;
import org.matsim.contrib.taxi.data.TaxiRequest;

public class AVTaxiMultiOccupiedDriveTask extends DriveTaskImpl implements AVTaxiTaskWithMultipleRequests, AVTaxiMultiTask {
	private final List<TaxiRequest> requests = new LinkedList<>();
	
	public AVTaxiMultiOccupiedDriveTask(VrpPathWithTravelData path) {
		super(path);
	}

	@Override
	public TaxiTaskType getTaxiTaskType() {
		return TaxiTaskType.OCCUPIED_DRIVE;
	}

	@Override
	public List<TaxiRequest> getRequests() {
		return requests;
	}

	@Override
	public void disconnectFromRequests() {
		for (TaxiRequest request : requests) request.setOccupiedDriveTask(null);
	}

	@Override
	public void addRequest(TaxiRequest request) {
		requests.add(request);
	}

	@Override
	public AVTaxiMultiTaskType getMultiTaxiTaskType() {
		return AVTaxiMultiTaskType.MULTI_OCCUPIED_DRIVE;
	}

}
