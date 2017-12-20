// code by andya
package playground.clruch.io.fleet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.QuadTree;

import playground.clruch.dispatcher.core.RequestStatus;
import playground.clruch.net.LinkSpeedUtils;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.RequestContainer;

/** @author Andreas Aumiller */
public class RequestContainerUtils {

    private final TaxiTrail taxiTrail;
    private final LinkSpeedUtils lsUtils;

    public RequestContainerUtils(TaxiTrail taxiTrail, LinkSpeedUtils lsUtils) {
        this.taxiTrail = taxiTrail;
        this.lsUtils = lsUtils;
    }

    public boolean isValidRequest(int now, boolean includeCancelled) {
        // System.out.println("Checking if request is valid at time: " + now);
        if (includeCancelled) {
            if (findSubmissionTime(now) > 0 && (propagateTo(now, now, RequestStatus.DROPOFF) > 0 || propagateTo(now, now, RequestStatus.CANCELLED) == 0))
                return true;
        } else {
            if (findSubmissionTime(now) > 0 && propagateTo(now, now, RequestStatus.DROPOFF) > 0 && propagateTo(now, now, RequestStatus.CANCELLED) < 0) {
                // System.out.println("Found request starting at " + findSubmissionTime(now) + " and ending at " + propagateTo(now, now, RequestStatus.DROPOFF)
                // + " in total taking " + (propagateTo(now, now, RequestStatus.DROPOFF) - findSubmissionTime(now)) + " seconds");
                return true;
            }
        }
        return false;
    }

    private int findSubmissionTime(int now) {
        // System.out.println("Trying to find submissionTime from Time: " + now);
        int submissionTime = propagateTo(now, now, RequestStatus.REQUESTED);
        if (submissionTime < 0)
            submissionTime = propagateTo(now, now, RequestStatus.PICKUP);
        // System.err.println("WARN Could not find submissionTime.");
        return submissionTime;
    }

    public List<RequestStatus> dumpRequestTrail(int now) {
        List<RequestStatus> requestTrail = new ArrayList<RequestStatus>();
        int requestStart = findSubmissionTime(now);
        int requestEnd = propagateTo(now, now, RequestStatus.DROPOFF);
        now = requestStart;
        while (now <= requestEnd) {
            requestTrail.add(taxiTrail.interp(now).getValue().requestStatus);
            if (Objects.nonNull(taxiTrail.getNextEntry(now)))
                now = taxiTrail.getNextEntry(now).getKey();
            else
                break;
        }
        return requestTrail;
    }

    private Optional<Coord> getCoordAt(int now, RequestStatus requestedStatus) {
        // Check requestStatus and propagate to TaxiStamp with desired values
        Optional<Coord> coord = Optional.empty();
        int requestedTime = propagateTo(now, now, requestedStatus);
        if (requestedTime > 0) {
            // System.out.println("INFO Found gps data for requested Status: " + requestedStatus.toString());
            coord = Optional.ofNullable(taxiTrail.interp(requestedTime).getValue().gps);
        }
        return coord;
    }

    private int propagateTo(int now, int lastTimeStep, RequestStatus requestedStatus) {
        // Check requestStatus and propagate to TaxiStamp with desired values
        RequestStatus requestStatus = taxiTrail.interp(now).getValue().requestStatus;

        // System.out.println("Trying to find: " + requestedStatus.toString() + " from " + requestStatus.toString());
        if (requestStatus != RequestStatus.EMPTY && requestStatus != RequestStatus.CANCELLED) {
            if (requestedStatus.compareTo(requestStatus) > 0) {
                if (Objects.nonNull(taxiTrail.getNextEntry(now))) {
                    int nextTimeStep = taxiTrail.getNextEntry(now).getKey();
                    if (lastTimeStep != nextTimeStep && taxiTrail.interp(nextTimeStep).getValue().requestStatus.compareTo(requestStatus) >= 0) {
                        lastTimeStep = now;
                        return propagateTo(nextTimeStep, lastTimeStep, requestedStatus);
                    }
                }
                return -1;
            } else if (requestedStatus.compareTo(requestStatus) < 0) {
                if (Objects.nonNull(taxiTrail.getLastEntry(now))) {
                    int nextTimeStep = taxiTrail.getLastEntry(now).getKey();
                    if (lastTimeStep != nextTimeStep && taxiTrail.interp(nextTimeStep).getValue().requestStatus.compareTo(requestStatus) <= 0) {
                        lastTimeStep = now;
                        return propagateTo(nextTimeStep, lastTimeStep, requestedStatus);
                    }
                }
                return -1;
            } else if (requestedStatus == requestStatus)
                return now;
        } else if (requestStatus == RequestStatus.CANCELLED)
            return 0;
        return -1;

    }

    public RequestContainer populate(int now, int requestIndex, boolean newRequest, QuadTree<Link> qt, MatsimStaticDatabase db) {
        // Handle requestIndex & submissionTime
        int submissionTime = -1;

        RequestStatus nowRequest = taxiTrail.interp(now).getValue().requestStatus;
        RequestStatus lastRequest = taxiTrail.getLastEntry(now).getValue().requestStatus;

        if (newRequest) {
            // requestIndex++;
            System.out.println("Adding new Request: " + requestIndex + " [ " + lastRequest.name() + " / " + nowRequest.name() + " ]");
            taxiTrail.setRequestIndex(now, requestIndex);
            // System.out.println("Processing requestIndex: " + requestIndex);
            submissionTime = taxiTrail.interp(now).getKey();
            // System.out.println("Submission time is: " + submissionTime);
        } else {
            taxiTrail.setRequestIndex(now, taxiTrail.getLastEntry(now).getValue().requestIndex);
            submissionTime = findSubmissionTime(now);
        }

        // Populate RequestContainer
        RequestContainer rc = new RequestContainer();
        try {
            Optional<Coord> from = getCoordAt(now, RequestStatus.PICKUP);
            if (from.isPresent()) {
                rc.fromLinkIndex = lsUtils.getLinkfromCoord(from.get());
            }
            Optional<Coord> to = getCoordAt(now, RequestStatus.DROPOFF);
            if (to.isPresent()) {
                rc.toLinkIndex = lsUtils.getLinkfromCoord(to.get());
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
