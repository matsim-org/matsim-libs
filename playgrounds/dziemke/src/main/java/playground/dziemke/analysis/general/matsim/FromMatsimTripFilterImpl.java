package playground.dziemke.analysis.general.matsim;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;
import playground.dziemke.analysis.general.Trip;
import playground.dziemke.analysis.general.TripFilter;
import playground.dziemke.utils.ShapeReader;

import java.util.*;

/**
 * @author gthunig on 04.04.2017.
 */
public class FromMatsimTripFilterImpl implements TripFilter {
    public static final Logger log = Logger.getLogger(FromMatsimTripFilterImpl.class);

    // Parameters
    private boolean onlyAnalyzeTripsWithMode;
    private List<String> mode;

    private boolean onlyAnalyzeTripInteriorOfArea; // formerly results labelled as "int"
    private boolean onlyAnalyzeTripsStartingOrEndingInArea; // formerly results labelled as "ber" (Berlin-based) <----------
    private int areaId;

    private boolean onlyAnalyzeTripsInDistanceRange; // "dist"; usually varied for analysis // <----------
    private double minDistance_km = -1;
    private double maxDistance_km = -1;

    private boolean onlyAnalyzeTripsWithActivityTypeBeforeTrip;
    private String activityTypeBeforeTrip;
    private boolean onlyAnalyzeTripsWithActivityTypeAfterTrip;
    private String activityTypeAfterTrip;

    private boolean onlyAnalyzeTripsDoneByPeopleInAgeRange; // "age"; this requires setting a CEMDAP file
    private int minAge = -1; // typically "x0"
    private int maxAge = -1; // typically "x9"; highest number usually chosen is 119

    private Network network;
    private Geometry areaGeometry;

    public void activateModeChoice(String... mode) {
        onlyAnalyzeTripsWithMode = true;
        this.mode = Arrays.asList(mode);
    }

    public void activateInt(Network network, String areaShapeFile) {
        assignNetwork(network);
        assignAreGeometry(areaShapeFile);
        this.onlyAnalyzeTripInteriorOfArea = true;
    }

    public void activateStartsOrEndsIn(Network network, String areaShapeFile, int areaId) {
        this.areaId = areaId;
        assignNetwork(network);
        assignAreGeometry(areaShapeFile);
        onlyAnalyzeTripsStartingOrEndingInArea = true;
    }

    private void assignNetwork(Network network) {
        this.network = network;
    }

    private void assignAreGeometry(String areaShapeFile) {
        Map<Integer, Geometry> zoneGeometries = ShapeReader.read(areaShapeFile, "NR");
        areaGeometry = zoneGeometries.get(areaId);
    }

    public void activateDist(double minDistance_km, double maxDistance_km) {
        onlyAnalyzeTripsInDistanceRange = true;
        this.minDistance_km = minDistance_km;
        this.maxDistance_km = maxDistance_km;
    }

    public void activateCertainActBefore(String activityTypeBeforeTrip) {
        onlyAnalyzeTripsWithActivityTypeBeforeTrip = true;
        this.activityTypeBeforeTrip = activityTypeBeforeTrip;
    }

    public void activateCertainActAfter(String activityTypeAfterTrip) {
        onlyAnalyzeTripsWithActivityTypeAfterTrip = true;
        this.activityTypeAfterTrip = activityTypeAfterTrip;
    }

    public void activateAge(int minAge, int maxAge) {
        onlyAnalyzeTripsDoneByPeopleInAgeRange = true;
        this.minAge = minAge;
        this.maxAge = maxAge;
    }

    public List<? extends Trip> filter(List<? extends Trip> tripMap) {
        List<FromMatsimTrip> trips = new LinkedList<>();
        boolean printedWarn1 = false;
        boolean printedWarn2 = false;

        for (Trip currentTrip : tripMap) {
            FromMatsimTrip trip = (FromMatsimTrip)currentTrip;
            // Choose if trip will be considered
            if (onlyAnalyzeTripInteriorOfArea || onlyAnalyzeTripsStartingOrEndingInArea) {
                // get coordinates of links
                Id<Link> departureLinkId = trip.getDepartureLinkId();
                Id<Link> arrivalLinkId = trip.getArrivalLinkId();
                //
                Link departureLink = network.getLinks().get(departureLinkId);
                Link arrivalLink = network.getLinks().get(arrivalLinkId);

                // TODO use coords of toNode instead of center coord of link
                double arrivalCoordX = arrivalLink.getCoord().getX();
                double arrivalCoordY = arrivalLink.getCoord().getY();
                double departureCoordX = departureLink.getCoord().getX();
                double departureCoordY = departureLink.getCoord().getY();

                // create points
                Point arrivalLocation = MGC.xy2Point(arrivalCoordX, arrivalCoordY);
                Point departureLocation = MGC.xy2Point(departureCoordX, departureCoordY);

                if (onlyAnalyzeTripsStartingOrEndingInArea) {
                    if (!areaGeometry.contains(arrivalLocation) && !areaGeometry.contains(departureLocation)) {
                        continue;
                    }
                }
                if (onlyAnalyzeTripInteriorOfArea) {
                    if (onlyAnalyzeTripsStartingOrEndingInArea && !printedWarn1) {
                        log.warn("onlyAnalyzeTripInteriorOfArea and onlyAnalyzeTripsStartingOrEndingInArea activated at the same time!");
                        printedWarn1 = true;
                    }
                    if (!areaGeometry.contains(arrivalLocation) || !areaGeometry.contains(departureLocation)) {
                        continue;
                    }
                }
            }

            if (onlyAnalyzeTripsWithMode) {
                if (!mode.contains(trip.getLegMode())) {
                    continue;
                }
            }
            if (onlyAnalyzeTripsInDistanceRange && (trip.getDistanceBeeline_m() / 1000.) > maxDistance_km) {
                continue;
            }
            if (onlyAnalyzeTripsInDistanceRange && (trip.getDistanceBeeline_m() / 1000.) < minDistance_km) {
                continue;
            }
            if (onlyAnalyzeTripsWithActivityTypeBeforeTrip && onlyAnalyzeTripsWithActivityTypeAfterTrip && !printedWarn2) {
                log.warn("onlyAnalyzeTripsWithActivityTypeBeforeTrip and onlyAnalyzeTripsWithActivityTypeAfterTrip activated at the same time."
                        + "This may lead to results that are hard to interpret: rather not use these options simultaneously.");
                printedWarn2 = true;
            }
            if (onlyAnalyzeTripsWithActivityTypeBeforeTrip) {
                if (!trip.getActivityTypeBeforeTrip().equals(activityTypeBeforeTrip)) {
                    continue;
                }
            }
            if (onlyAnalyzeTripsWithActivityTypeAfterTrip) {
                if (!trip.getActivityTypeAfterTrip().equals(activityTypeAfterTrip)) {
                    continue;
                }
            }

			/* Only trips that fullfill all checked criteria are added; otherwise that loop would have been "continued" already */
            trips.add(trip);
        }

        return trips;
    }

    public String adaptOutputDirectory(String outputDirectory) {
        if (onlyAnalyzeTripsWithMode) {
            outputDirectory = outputDirectory + "_" + mode;
        }
        if (onlyAnalyzeTripInteriorOfArea) {
            outputDirectory = outputDirectory + "_inside-" + areaId;
        }
        if (onlyAnalyzeTripsStartingOrEndingInArea) {
            outputDirectory = outputDirectory + "_soe-in-" + areaId;
        }
        if (onlyAnalyzeTripsInDistanceRange) {
            outputDirectory = outputDirectory + "_dist-" + minDistance_km + "-" + maxDistance_km;
        }
        if (onlyAnalyzeTripsWithActivityTypeBeforeTrip) {
            outputDirectory = outputDirectory + "_act-bef-" + activityTypeBeforeTrip;
        }
        if (onlyAnalyzeTripsWithActivityTypeAfterTrip) {
            outputDirectory = outputDirectory + "_act-aft-" + activityTypeAfterTrip;
        }
        if (onlyAnalyzeTripsDoneByPeopleInAgeRange) {
            outputDirectory = outputDirectory + "_age-" + minAge + "-" + maxAge;
        }
        return outputDirectory;
    }

}
