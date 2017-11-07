package playground.clruch.net;

import java.util.Objects;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.QuadTree;

import playground.clruch.dispatcher.core.RequestStatus;
import playground.clruch.io.fleet.TaxiTrail;

/** @author Andreas Aumiller */
public class RequestContainerUtils {

    private TaxiTrail taxiTrail;

    public RequestContainerUtils(TaxiTrail taxiTrail) {
        this.taxiTrail = taxiTrail;
    }

    public boolean isValidRequest(int now) {
        // System.out.println("Checking if request is valid at time: " + now);
        if (findSubmissionTime(now) >= 0 && (propagateTo(now, RequestStatus.DROPOFF) > 0 || propagateTo(now, RequestStatus.CANCELLED) == 0))
            return true;
        return false;
    }

    private int findSubmissionTime(int now) {
        // System.out.println("Trying to find submissionTime from Time: " + now);
        int submissionTime = propagateTo(now, RequestStatus.REQUESTED);
        if (submissionTime < 0)
            submissionTime = propagateTo(now, RequestStatus.PICKUP);
        // System.err.println("WARN Could not find submissionTime.");
        return submissionTime;
    }

    private Coord getCoordAt(int now, RequestStatus requestedStatus) {
        // Check requestStatus and propagate to TaxiStamp with desired values
        int requestedTime = propagateTo(now, requestedStatus);
        if (requestedTime > 0) {
            // System.out.println("INFO Found gps data for requested Status: " + requestedStatus.toString());
            return taxiTrail.interp(requestedTime).getValue().gps;
            // } else if (requestedTime == 0) {
            // return null;
        }
        return null;
    }

    private int propagateTo(int now, RequestStatus requestedStatus) {
        // Check requestStatus and propagate to TaxiStamp with desired values
        RequestStatus requestStatus = taxiTrail.interp(now).getValue().requestStatus;
        // System.out.println("Trying to find: " + requestedStatus.toString() + " from " + requestStatus.toString());
        if (requestStatus != RequestStatus.EMPTY && requestStatus != RequestStatus.CANCELLED) {
            if (requestedStatus.compareTo(requestStatus) > 0) {
                if (Objects.nonNull(taxiTrail.getNextEntry(now))) {
                    int nextTimeStep = taxiTrail.getNextEntry(now).getKey();
                    return propagateTo(nextTimeStep, requestedStatus);
                } else {
                    // System.err.println("WARN getNextEntry is null");
                    return -1;
                }
            } else if (requestedStatus.compareTo(requestStatus) < 0) {
                if (Objects.nonNull(taxiTrail.getLastEntry(now))) {
                    int nextTimeStep = taxiTrail.getLastEntry(now).getKey();
                    return propagateTo(nextTimeStep, requestedStatus);
                } else {
                    // System.err.println("WARN getLastEntry is null");
                    return -1;
                }
            } else if (requestedStatus == requestStatus)
                // System.out.println("INFO Found requestStatus: " + requestedStatus.toString());
                return now;
        } else if (requestStatus == RequestStatus.CANCELLED)
            return 0;
        // System.err.println("WARN Couldn't find requested Status, returning -1");
        return -1;

    }

    public RequestContainer populate(int now, int requestIndex, QuadTree<Link> qt, MatsimStaticDatabase db) {
        // Handle requestIndex & submissionTime
        int submissionTime = -1;

        RequestStatus lastRequest = taxiTrail.getLastEntry(now).getValue().requestStatus;
        if (lastRequest == RequestStatus.EMPTY) {
            taxiTrail.setRequestIndex(now, requestIndex);
            submissionTime = taxiTrail.interp(now).getKey();
        } else {
            taxiTrail.setRequestIndex(now, taxiTrail.getLastEntry(now).getValue().requestIndex);
            submissionTime = findSubmissionTime(now);
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
                rc.toLinkIndex = db.getLinkIndex(qt.getClosest(to.getX(), to.getY()));
            }
        } catch (Exception exception) {
            System.err.println("WARN failed to get from/to Coords at time: " + now);
            System.err.println("WARN " + exception.toString());
        }
        rc.requestIndex = taxiTrail.interp(now).getValue().requestIndex;
        rc.requestStatus = taxiTrail.interp(now).getValue().requestStatus;
        rc.submissionTime = submissionTime;
        return rc;
    }
}
