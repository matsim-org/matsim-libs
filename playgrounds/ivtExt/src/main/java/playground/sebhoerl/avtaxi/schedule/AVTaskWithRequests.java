package playground.sebhoerl.avtaxi.schedule;

import java.util.Collection;
import java.util.List;

import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public interface AVTaskWithRequests {
	Collection<AVRequest> getRequests();
	void addRequest(AVRequest request);
}
