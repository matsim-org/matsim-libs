package playground.clruch.net;

import java.util.Objects;
import java.util.Map.Entry;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.dispatcher.core.RequestStatus;
import playground.clruch.io.fleet.TaxiStamp;
import playground.clruch.io.fleet.TaxiTrail;

/** @author Andreas Aumiller */
public class RequestContainerUtils {

    private final TaxiTrail taxiTrail;
    private final RequestStatus requestStatus;
    private final int linkIndex;
    private int submissionTime;

    public RequestContainerUtils(int now, int linkIndex, TaxiTrail taxiTrail) {
        this.taxiTrail = taxiTrail;
        this.linkIndex = linkIndex;
        this.submissionTime = getSubmissionTime(now);
        this.requestStatus = mapRequestState(now);
    }

    public void populate(RequestContainer rc) {
        switch (requestStatus) {
        case REQUEST:
            rc.submissionTime = submissionTime;
        case PICKUP:
            rc.fromLinkIndex = linkIndex;
            rc.submissionTime = submissionTime;
        case DROPOFF:
            rc.toLinkIndex = linkIndex;
        case CANCELED:
        case FREE:
        default:
            break;
        }
        rc.requestStatus = requestStatus;
        return;
    }
    
    private int getSubmissionTime(int now) {
        return taxiTrail.interp(now).getKey();
    }

    private RequestStatus mapRequestState(int now) {
        Entry<Integer, TaxiStamp> nowEntry = taxiTrail.interp(now);
        Entry<Integer, TaxiStamp> lastEntry = taxiTrail.getLastEntry(now);
        GlobalAssert.that(Objects.nonNull(lastEntry));
        
        AVStatus nowState = nowEntry.getValue().avStatus;
        AVStatus lastState = lastEntry.getValue().avStatus;
        
        // Check change of AVStatus from the vehicle and map corresponding requestStatus
        switch (nowState) {
        case DRIVETOCUSTOMER:
            if (lastState == AVStatus.STAY || lastState == AVStatus.REBALANCEDRIVE)
                return RequestStatus.REQUEST;
        case DRIVEWITHCUSTOMER:
            if (lastState != AVStatus.OFFSERVICE || lastState != AVStatus.DRIVEWITHCUSTOMER) {
                if (lastState == AVStatus.DRIVETOCUSTOMER)
                    // Check if taxi was ordered or called from street
                    submissionTime = -1;
                return RequestStatus.PICKUP;
            }
        case STAY:
        case REBALANCEDRIVE:
            if (lastState == AVStatus.DRIVEWITHCUSTOMER)
                return RequestStatus.DROPOFF;
            else if (lastState == AVStatus.DRIVETOCUSTOMER)
                return RequestStatus.CANCELED;
        default:
            break;
        }
        return RequestStatus.FREE;
    }
}
