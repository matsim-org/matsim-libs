package org.matsim.contrib.analysis.vsp.traveltimedistance;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.geotools.MGC;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class TravelTimeValidationRunnerPreAnalysis {
    private final Network network;
    private final int trips;
    private Geometry keyAreaGeometry;
    private final TravelTimeDistanceValidator validator;
    private final String outputFolder;

    /**
     * Proportion of trips will be chosen within the key area (when key area shape file is provided).
     * */
    private double insideTripProportion = 0.6;
    /**
     * Proportion of trips will be chosen that cross the border of the key area (when key area shape file is provided)
     * */
    private double crossBorderTripProportion = 0.3;

    private final Random rnd = new Random(1234);

    public TravelTimeValidationRunnerPreAnalysis(Network network, int trips, String outputFolder, TravelTimeDistanceValidator validator, Geometry keyAreaGeometry) {
        this.network = network;
        this.trips = trips;
        this.outputFolder = outputFolder;
        this.validator = validator;
        this.keyAreaGeometry = keyAreaGeometry;
    }

    public void setKeyAreaGeometry(Geometry keyAreaGeometry) {
        this.keyAreaGeometry = keyAreaGeometry;
    }

    /**
     * Set the proportion of the inside trips to analyze.
     * Note that insideTripProportion + crossBorderTripProportion should be less than or equal to 1
     * The rest proportion will be outside trips
     * */
    public void setInsideTripProportion(double insideTripProportion) {
        this.insideTripProportion = insideTripProportion;
    }

    /**
     * Set the proportion of the cross border trips to analyze.
     * Note that insideTripProportion + crossBorderTripProportion should be less than or equal to 1
     * The rest proportion will be outside trips
     * */
    public void setCrossBorderTripProportion(double crossBorderTripProportion){
        this.crossBorderTripProportion = crossBorderTripProportion;
    }

    public void run() throws IOException, InterruptedException {
        List<Link> links = network.getLinks().values().stream().
                filter(l -> l.getAllowedModes().contains(TransportMode.car)).
                collect(Collectors.toList());
        int numOfLinks = links.size();

        List<Link> linksInsideShp = new ArrayList<>();
        List<Link> outsideLinks = new ArrayList<>();

        if (keyAreaGeometry != null) {
            for (Link link : links) {
                if (MGC.coord2Point(link.getToNode().getCoord()).within(keyAreaGeometry)) {
                    linksInsideShp.add(link);
                }
            }
            outsideLinks.addAll(links);
            outsideLinks.removeAll(linksInsideShp);
        }

        // Create router
        FreeSpeedTravelTime travelTime = new FreeSpeedTravelTime();
        LeastCostPathCalculatorFactory fastAStarLandmarksFactory = new SpeedyALTFactory();
        OnlyTimeDependentTravelDisutilityFactory disutilityFactory = new OnlyTimeDependentTravelDisutilityFactory();
        TravelDisutility travelDisutility = disutilityFactory.createTravelDisutility(travelTime);
        LeastCostPathCalculator router = fastAStarLandmarksFactory.createPathCalculator(network, travelDisutility,
                travelTime);

        // Choose random trips to validate
        CSVPrinter csvWriter = new CSVPrinter(new FileWriter(outputFolder + "/pre-analysis-results.csv"), CSVFormat.DEFAULT);
        csvWriter.printRecord("trip_number", "trip_category", "from_x", "from_y", "to_x", "to_y", "simulated_travel_time", "validated_travel_time");
        int counter = 0;

        Link fromLink;
        Link toLink;
        String tripType;
        while (counter < trips) {
            if (!linksInsideShp.isEmpty()) {
                int numOfLinksInsideShp = linksInsideShp.size();
                int numOfOutsideLinks = outsideLinks.size();

                if (counter < insideTripProportion * trips) {
                    fromLink = linksInsideShp.get(rnd.nextInt(numOfLinksInsideShp));
                    toLink = linksInsideShp.get(rnd.nextInt(numOfLinksInsideShp));
                    tripType = "inside";
                } else if (counter < (insideTripProportion + crossBorderTripProportion) * trips) {
                    fromLink = linksInsideShp.get(rnd.nextInt(numOfLinksInsideShp));
                    toLink = outsideLinks.get(rnd.nextInt(numOfOutsideLinks));
                    tripType = "cross-border";
                } else {
                    fromLink = outsideLinks.get(rnd.nextInt(numOfOutsideLinks));
                    toLink = outsideLinks.get(rnd.nextInt(numOfOutsideLinks));
                    tripType = "outside";
                }
            } else {
                fromLink = links.get(rnd.nextInt(numOfLinks));
                toLink = links.get(rnd.nextInt(numOfLinks));
                tripType = "unknown";
            }

            if (!fromLink.getToNode().getId().equals(toLink.getToNode().getId())) {
                String detailedFile = outputFolder + "/detailed-record/trip" + counter + ".json.gz";
                Coord fromCorrd = fromLink.getToNode().getCoord();
                Coord toCoord = toLink.getToNode().getCoord();
                double validatedTravelTime = validator.getTravelTime
                        (fromCorrd, toCoord, 1, detailedFile).getFirst();
                counter++;
                Thread.sleep(100);
                if (validatedTravelTime < 60) {
                    continue;
                }
                double simulatedTravelTime = router.calcLeastCostPath
                        (fromLink.getToNode(), toLink.getToNode(), 0, null, null).travelTime;
                csvWriter.printRecord(Integer.toString(counter), tripType, Double.toString(fromCorrd.getX()),
                        Double.toString(fromCorrd.getY()), Double.toString(toCoord.getX()),
                        Double.toString(toCoord.getY()), Double.toString(simulatedTravelTime),
                        Double.toString(validatedTravelTime));
            }
        }
        csvWriter.close();

    }
}
