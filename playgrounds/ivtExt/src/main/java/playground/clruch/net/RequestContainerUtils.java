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
        RequestStatus requestStatus = taxiTrail.interp(now).getValue().requestStatus;
        if (requestStatus != RequestStatus.EMPTY) {
            if (findSubmissionTime(now) >= 0 && (propagateTo(now, RequestStatus.DROPOFF) >= 0 || propagateTo(now, RequestStatus.CANCELLED) >= 0))
                return true;
        }
        return false;
    }

    private int findSubmissionTime(int now) {
        // System.out.println("Trying to find submissionTime from Time: " + now);
        int submissionTime = propagateTo(now, RequestStatus.REQUESTED);
        if (submissionTime >= 0)
            return submissionTime;
        else {
            submissionTime = propagateTo(now, RequestStatus.PICKUP);
            if (submissionTime >= 0)
                return submissionTime;
<<<<<<< HEAD
            else {
//                System.err.println("WARN Could not find submissionTime.");
            }
=======
            // System.err.println("WARN Could not find submissionTime.");
>>>>>>> master
        }
        return -1; // TODO -1 seems to be a reoccurring magic const in this file
    }

    private Coord getCoordAt(int now, RequestStatus requestedStatus) {
        RequestStatus requestStatus = taxiTrail.interp(now).getValue().requestStatus;

        // Check requestStatus and propagate to TaxiStamp with desired values
        if (requestStatus != RequestStatus.EMPTY && requestStatus != RequestStatus.CANCELLED) {
            now = propagateTo(now, requestedStatus);
            if (now >= 0) {
                // System.out.println("INFO Found gps data for requested Status: " + requestedStatus.toString());
                return taxiTrail.interp(now).getValue().gps;
            }
        }
        // TODO look into returning type Optional<Coord>, and instead of null simply "return Optional.empty();"
        // this can prevent certain mistakes in the application layer
        return null;
    }

    private int propagateTo(int now, RequestStatus requestedStatus) {
        // Check requestStatus and propagate to TaxiStamp with desired values
        RequestStatus requestStatus = taxiTrail.interp(now).getValue().requestStatus;
        // System.out.println("Trying to find: " + requestedStatus.toString() + " from " + requestStatus.toString());

        if (requestStatus != RequestStatus.EMPTY) {
            if (requestedStatus.compareTo(requestStatus) > 0) {
                if (Objects.nonNull(requestStatus = taxiTrail.getNextEntry(now).getValue().requestStatus)) {
                    int nextTimeStep = taxiTrail.getNextEntry(now).getKey();
                    return propagateTo(nextTimeStep, requestedStatus);
<<<<<<< HEAD
                } else {
//                	 System.err.println("WARN getNextEntry is null");
                }              
=======
                }
                else 
                    return -1; 
                // System.err.println("WARN getNextEntry is null");
>>>>>>> master
            } else if (requestedStatus.compareTo(requestStatus) < 0) {
                if (Objects.nonNull(requestStatus = taxiTrail.getLastEntry(now).getValue().requestStatus)) {
                    int nextTimeStep = taxiTrail.getLastEntry(now).getKey();
                    return propagateTo(nextTimeStep, requestedStatus);
<<<<<<< HEAD
                } else {
//                	System.err.println("WARN getLastEntry is null");
                }    
            } else if (requestedStatus == requestStatus) {
//            	System.out.println("INFO Found requestStatus: " + requestedStatus.toString());
            }         
            return now;
        }
//        System.err.println("WARN Couldn't find requested Status, returning -1");
=======
                }
                else
                    return -1;
                // System.err.println("WARN getLastEntry is null");
            } else if (requestedStatus == requestStatus)
                // System.out.println("INFO Found requestStatus: " + requestedStatus.toString());
                return now;
        }
        // System.err.println("WARN Couldn't find requested Status, returning -1");
>>>>>>> master
        return -1;
    }

    public RequestContainer populate(int now, int requestIndex, QuadTree<Link> qt, MatsimStaticDatabase db) {
        // Handle requestIndex & submissionTime
        int submissionTime = -1;

        RequestStatus lastRequest = taxiTrail.getLastEntry(now).getValue().requestStatus;
        if (lastRequest == RequestStatus.EMPTY) {
            requestIndex++;
            taxiTrail.setRequestIndex(now, requestIndex);
            submissionTime = now;
        } else {
            taxiTrail.setRequestIndex(now, taxiTrail.getLastEntry(now).getValue().requestIndex);
            submissionTime = findSubmissionTime(now);
        }
        // System.out.println("INFO SubmissionTime set to: " + submissionTime);

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
        }
        rc.requestIndex = taxiTrail.interp(now).getValue().requestIndex;
        rc.requestStatus = taxiTrail.interp(now).getValue().requestStatus;
        rc.submissionTime = submissionTime;
        return rc;
    }
}
