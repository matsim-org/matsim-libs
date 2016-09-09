package playground.sebhoerl.avtaxi.schedule;

import java.util.List;

import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.schedule.TaxiTask;

public interface AVTaxiTaskWithMultipleRequests extends TaxiTask {
	List<TaxiRequest> getRequests();
	void disconnectFromRequests();
	void addRequest(TaxiRequest request);
}
