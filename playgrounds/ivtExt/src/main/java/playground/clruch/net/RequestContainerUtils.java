package playground.clruch.net;

import java.util.Map.Entry;
import java.util.Objects;

import org.matsim.api.core.v01.Coord;

import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.dispatcher.core.RequestStatus;
import playground.clruch.io.fleet.TaxiStamp;
import playground.clruch.io.fleet.TaxiTrail;

/** @author Andreas Aumiller */
public class RequestContainerUtils {

    private final TaxiTrail taxiTrail;

    public RequestContainerUtils(TaxiTrail taxiTrail) {
        this.taxiTrail = taxiTrail;
    }

    public int findSubmissionTime(int now) {
        int submissionTime = propagateTo(now, RequestStatus.REQUESTED);
        if (submissionTime >= 0)
            return submissionTime;
        else
            return propagateTo(now, RequestStatus.PICKUP);
    }

    public boolean isNewRequest(int now, int requestIndex, TaxiTrail taxiTrail) {
        RequestStatus requestStatus = taxiTrail.interp(now).getValue().requestStatus;
        RequestStatus lastRequest = taxiTrail.getLastEntry(now).getValue().requestStatus;
        if (requestStatus != RequestStatus.EMPTY) {
            if (lastRequest == RequestStatus.EMPTY) {
                requestIndex++;
                taxiTrail.setRequestIndex(now, requestIndex);
                return true;
            } else {
                taxiTrail.setRequestIndex(now, taxiTrail.getLastEntry(now).getValue().requestIndex);
                return false;
            }
        }
        return false;
    }

    public Coord getCoordAt(int now, RequestStatus requestedStatus) {
        RequestStatus requestStatus = taxiTrail.interp(now).getValue().requestStatus;

        // Check requestStatus and propagate to TaxiStamp with desired values
        if (requestStatus != RequestStatus.EMPTY && requestStatus != RequestStatus.CANCELLED) {
            now = propagateTo(now, requestedStatus);
            if (now >= 0)
                return taxiTrail.interp(now).getValue().gps;
        }
        return null;
    }

    private int propagateTo(int now, RequestStatus requestedStatus) {
        // Check requestStatus and propagate to TaxiStamp with desired values
        RequestStatus requestStatus = taxiTrail.interp(now).getValue().requestStatus;
        System.out.println("Trying to find: " + requestedStatus.toString() + " from " + requestStatus.toString());
        
        if (requestStatus != RequestStatus.EMPTY && requestStatus != RequestStatus.CANCELLED) {
            if (requestedStatus.compareTo(requestStatus) > 0) {
                if (Objects.nonNull(requestStatus = taxiTrail.getNextEntry(now).getValue().requestStatus)) {
                    int nextTimeStep = taxiTrail.getNextEntry(now).getKey();
                    return propagateTo(nextTimeStep, requestedStatus);
                }
                else
                    System.err.println("getNextEntry is null");
            } else if (requestedStatus.compareTo(requestStatus) < 0) {
                if (Objects.nonNull(requestStatus = taxiTrail.getLastEntry(now).getValue().requestStatus)) {
                    int nextTimeStep = taxiTrail.getLastEntry(now).getKey();
                    return propagateTo(nextTimeStep, requestedStatus);
                }
                else
                    System.err.println("getLastEntry is null");
            } else if (requestedStatus == requestStatus)
                return now;
        }
        System.err.println("Couldn't find requested Status, returning -1");
        return -1;
    }

    public RequestStatus parseRequestStatus(int now) {
        Entry<Integer, TaxiStamp> nowEntry = taxiTrail.interp(now);
        Entry<Integer, TaxiStamp> lastEntry = taxiTrail.getLastEntry(now);

        if (Objects.nonNull(lastEntry)) {
            AVStatus nowState = nowEntry.getValue().avStatus;
            AVStatus lastState = lastEntry.getValue().avStatus;

            // Check change of AVStatus from the vehicle and map corresponding requestStatus
            switch (nowState) {
            case DRIVETOCUSTOMER:
                switch (lastState) {
                case STAY:
                case REBALANCEDRIVE:
                    return RequestStatus.REQUESTED;
                case DRIVETOCUSTOMER:
                    return RequestStatus.ONTHEWAY;
                default:
                    break;
                }
            case DRIVEWITHCUSTOMER:
                switch (lastState) {
                case STAY:
                case REBALANCEDRIVE:
                case DRIVETOCUSTOMER:
                    return RequestStatus.PICKUP;
                case DRIVEWITHCUSTOMER:
                    return RequestStatus.DRIVING;
                default:
                    break;
                }
            case STAY:
            case REBALANCEDRIVE:
                switch (lastState) {
                case DRIVETOCUSTOMER:
                    return RequestStatus.CANCELLED;
                case DRIVEWITHCUSTOMER:
                    return RequestStatus.DROPOFF;
                default:
                    break;
                }
            default:
                break;
            }
            return RequestStatus.EMPTY;
        }
        return RequestStatus.EMPTY;
    }
}
