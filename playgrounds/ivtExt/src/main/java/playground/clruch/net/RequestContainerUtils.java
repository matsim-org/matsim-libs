package playground.clruch.net;

import java.util.Map.Entry;
import java.util.Objects;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.QuadTree;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.dispatcher.core.RequestStatus;
import playground.clruch.io.fleet.TaxiStamp;
import playground.clruch.io.fleet.TaxiTrail;

/** @author Andreas Aumiller */
public class RequestContainerUtils {

    private TaxiTrail taxiTrail;

    public RequestContainerUtils(TaxiTrail taxiTrail) {
        this.taxiTrail = taxiTrail;
    }

    private int findSubmissionTime(int now) {
        System.out.println("Trying to find submissionTime from Time: " + now);
        int submissionTime = propagateTo(now, RequestStatus.REQUESTED);
        if (submissionTime >= 0)
            return submissionTime;
        else {
            submissionTime = propagateTo(now, RequestStatus.PICKUP);
            if (submissionTime >= 0)
                return submissionTime;
            else
                System.err.println("Could not find submissionTime.");
        }
        return -1;
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
                    System.out.println("propagating forward further...");
                    return propagateTo(nextTimeStep, requestedStatus);
                } else
                    System.err.println("getNextEntry is null");
            } else if (requestedStatus.compareTo(requestStatus) < 0) {
                if (Objects.nonNull(requestStatus = taxiTrail.getLastEntry(now).getValue().requestStatus)) {
                    int nextTimeStep = taxiTrail.getLastEntry(now).getKey();
                    System.out.println("propagating backward further...");
                    return propagateTo(nextTimeStep, requestedStatus);
                } else
                    System.err.println("getLastEntry is null");
            } else if (requestedStatus == requestStatus)
                System.out.println("Found requestStatus: " + requestedStatus.toString());
            return now;
        }
        System.err.println("Couldn't find requested Status, returning -1");
        return -1;
    }

    public RequestContainer populate(int now, int requestIndex, QuadTree<Link> qt, MatsimStaticDatabase db) {
        // Handle requestIndex & submissionTime
        int submissionTime = -1;
        RequestStatus requestStatus = taxiTrail.interp(now).getValue().requestStatus;
        RequestStatus lastRequest = taxiTrail.getLastEntry(now).getValue().requestStatus;
        if (requestStatus != RequestStatus.EMPTY) {
            if (lastRequest == RequestStatus.EMPTY) {
                requestIndex++;
                taxiTrail.setRequestIndex(now, requestIndex);
                submissionTime = now;
            } else {
                taxiTrail.setRequestIndex(now, taxiTrail.getLastEntry(now).getValue().requestIndex);
                submissionTime = findSubmissionTime(now);
            }
            System.out.println("SubmissionTime set to: " + submissionTime);
        }

        // Populate RequestContainer
        RequestContainer rc = new RequestContainer();
        try {
            Coord from = getCoordAt(now, RequestStatus.PICKUP);
            if (Objects.nonNull(from)) {
                from = db.referenceFrame.coords_fromWGS84.transform(from);
                rc.fromLinkIndex = db.getLinkIndex(qt.getClosest(from.getX(), from.getY()));
            }
            Coord to = getCoordAt(now, RequestStatus.DROPOFF);
            if (Objects.nonNull(to)) {
                to = db.referenceFrame.coords_fromWGS84.transform(to);
                rc.fromLinkIndex = db.getLinkIndex(qt.getClosest(to.getX(), to.getY()));
            }
        } catch (Exception exception) {
            System.err.println("failed to get from/to Coords at time: " + now);
        }
        rc.requestIndex = taxiTrail.interp(now).getValue().requestIndex;
        rc.requestStatus = taxiTrail.interp(now).getValue().requestStatus;
        rc.submissionTime = submissionTime;
        return rc;
    }

    public RequestStatus parseRequestStatus(int now) {
        if (now != 0) {
            Entry<Integer, TaxiStamp> nowEntry = taxiTrail.interp(now);
            Entry<Integer, TaxiStamp> lastEntry = taxiTrail.getLastEntry(now);
            // GlobalAssert.that(Objects.nonNull(lastEntry));

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
        return RequestStatus.EMPTY;
    }
}
