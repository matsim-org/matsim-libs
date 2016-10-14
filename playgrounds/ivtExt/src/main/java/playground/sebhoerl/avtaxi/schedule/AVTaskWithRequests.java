package playground.sebhoerl.avtaxi.schedule;

import java.util.Collection;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public interface AVTaskWithRequests {
	Collection<AVRequest> getRequests();
	void addRequest(AVRequest request);
}
