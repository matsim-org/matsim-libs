package playground.clruch.io.fleet;

import java.util.Map.Entry;
import java.util.Objects;

import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.dispatcher.core.RequestStatus;

public enum RequestStatusParser {
    ;

    public static RequestStatus parseRequestStatus(int now, TaxiTrail taxiTrail) {
        if (now != 0) {
            Entry<Integer, TaxiStamp> nowEntry = taxiTrail.interp(now);
            Entry<Integer, TaxiStamp> lastEntry = taxiTrail.getLastEntry(now);
            // GlobalAssert.that(Objects.nonNull(lastEntry));

            if (Objects.nonNull(lastEntry)) {
                AVStatus nowState = nowEntry.getValue().avStatus;
                AVStatus lastState = lastEntry.getValue().avStatus;
                return parse(nowState, lastState);
            }
        }
        return RequestStatus.EMPTY;
    }

    // 2nd constructor that can be called when both AVStatus are known for logging reasons
    public static RequestStatus parseRequestStatus(AVStatus nowState, AVStatus lastState) {
        return parse(nowState, lastState);
    }

    private static RequestStatus parse(AVStatus nowState, AVStatus lastState) {
        // Check change of AVStatus from the vehicle and map corresponding requestStatus
        switch (nowState) {
        case DRIVETOCUSTOMER:
            switch (lastState) {
            case STAY:
            case REBALANCEDRIVE:
            case OFFSERVICE:
            case DRIVEWITHCUSTOMER:
                return RequestStatus.REQUESTED;
            case DRIVETOCUSTOMER:
                return RequestStatus.PICKUPDRIVE;
            default:
                break;
            }
        case DRIVEWITHCUSTOMER:
            switch (lastState) {
            case STAY:
            case REBALANCEDRIVE:
            case DRIVETOCUSTOMER:
            case OFFSERVICE:
                return RequestStatus.PICKUP;
            case DRIVEWITHCUSTOMER:
                return RequestStatus.DRIVING;
            default:
                break;
            }
        case STAY:
        case REBALANCEDRIVE:
        case OFFSERVICE:
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
}
