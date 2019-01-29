package org.matsim.contrib.eventsBasedPTRouter.stopStopTimes;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * <B>When StopStopTimes are generated externally, e.g. by a regression.</B>
 * <p></p>
 * See constructor for input format.
 * </P>
 */
public class StopStopTimePreCalcSerializable implements StopStopTime, Serializable {

    private final Map<String, Map<String, Map<Integer, Tuple<Double, Double>>>> stopStopTimes = new HashMap<>(5000);
    private final Scenario scenario;

    public boolean isLogarithmic() {
        return logarithmic;
    }

    private final boolean logarithmic;
    private int nfeCounter = 0;
    private int aiobCounter = 0;
    private int errorCounter = 0;

    //Constructors


    /**
     * Populates a StopStopTime object with stop to stop travel times and their variances
     * recorded at particular times.
     * Needs at minimum one record per stop-to-stop combination.
     * Times at which records are recorded needn't be regular intervals, because a TreeMap is used.
     *
     * @param inputFile   path to the tab-separated file.Format is fromStopId (String), toStopId (String), time of record (seconds, double),
     *                    travelTime (seconds, double), travelTimeVariance (seconds**2, double). No headings or row numbers.
     * @param logarithmic if times are recorded as logarithms (normally distributed residuals)
     */
    public StopStopTimePreCalcSerializable(String inputFile, Scenario scenario, boolean logarithmic) {
        this.logarithmic = logarithmic;
        this.scenario = scenario;
        BufferedReader reader = IOUtils.getBufferedReader(inputFile);
        String txt = "";
        while (true) {
            try {
                txt = reader.readLine();
                if (txt == null)
                    break;
                String[] split = txt.split("\t");
//                get the map from this stop id
                Map<String, Map<Integer, Tuple<Double, Double>>> toMap = stopStopTimes.get(split[0]);
                if (toMap == null) {
                    toMap = new HashMap<>();
                    stopStopTimes.put(split[0], toMap);
                }

                Map<Integer, Tuple<Double, Double>> timeData = toMap.get(split[1]);
                if (timeData == null) {
                    timeData = new TreeMap<>();
                    toMap.put(split[1], timeData);
                }

                timeData.put(Integer.parseInt(split[2]), new Tuple<>(Double.parseDouble(split[3]), Double.parseDouble(split[4])));

            } catch (IOException e) {
                e.printStackTrace();
            } catch (ArrayIndexOutOfBoundsException e) {
                if (aiobCounter < 10) {
                    System.err.println("Seems one of the lines in the StopStopTime input file is missing a value. Skipping it.");
                } else {
                    System.err.println("Seems one of the lines in the StopStopTime input file is missing a value. Skipping further warnings...");

                }
                aiobCounter++;
            } catch (NumberFormatException e) {
                if (nfeCounter < 10) {
                    System.err.println("Some values in the StopStopTime input file are of the wrong type. Skipping it.");
                } else {
                    System.err.println("Some values in the StopStopTime input file are of the wrong type. Skipping further warnings...");
                }
                nfeCounter++;
            }
        }
        System.out.println("\n\n*************************************************************************\n\n");
        System.out.println("STOPSTOPTIMES LOADED, filling in the blanks from the schedule and network");
        System.out.println("\n\n*************************************************************************\n\n");

        for (TransitLine transitLine : scenario.getTransitSchedule().getTransitLines().values()) {
            for (TransitRoute route : transitLine.getRoutes().values()) {
                TRANSITSTOPS:
                for (int s = 0; s < route.getStops().size() - 1; s++) {
                    String origin = route.getStops().get(s).getStopFacility().getId().toString();
                    String destination = route.getStops().get(s + 1).getStopFacility().getId().toString();
                    Map<String, Map<Integer, Tuple<Double, Double>>> toMap = stopStopTimes.get(origin);
                    if (toMap == null) {
                        toMap = new HashMap<>();
                        stopStopTimes.put(origin, toMap);
                    }

                    Map<Integer, Tuple<Double, Double>> timeData = toMap.get(destination);
                    if (timeData == null) {
                        timeData = new TreeMap<>();
                        toMap.put(destination, timeData);

                        try {
                            List<TransitRouteStop> stops;
                            stops = scenario.getTransitSchedule().getTransitLines().get(transitLine.getId()).getRoutes().get(route.getId()).getStops();
                            if (stops == null)
                                throw new NullPointerException();
                            Link fromLink = null;
                            Link toLink = null;
                            for (TransitRouteStop tss : stops) {
                                if (tss.getStopFacility().getId().toString().equals(origin)) {
                                    fromLink = scenario.getNetwork().getLinks().get(tss.getStopFacility().getLinkId());
                                }
                                if (tss.getStopFacility().getId().toString().equals(destination)) {
                                    toLink = scenario.getNetwork().getLinks().get(tss.getStopFacility().getLinkId());
                                }
                            }
                            if (fromLink == null || toLink == null)
                                throw new NullPointerException();
                            NetworkRoute networkRoute = scenario.getTransitSchedule().getTransitLines().get(transitLine.getId())
                                    .getRoutes().get(route.getId()).getRoute();
                            NetworkRoute subRoute = networkRoute.getSubRoute(fromLink.getId(), toLink.getId());
                            List<Id<Link>> linkIds = new ArrayList<>();
                            linkIds.addAll(subRoute.getLinkIds());
                            linkIds.add(toLink.getId());
                            double freeSpeedTravelTime = 0;
                            for (Id<Link> id : linkIds) {
                                Link link = scenario.getNetwork().getLinks().get(id);
                                freeSpeedTravelTime += link.getLength() / link.getFreespeed();
                            }
                            if (logarithmic)
                                timeData.put(0, new Tuple<Double, Double>(Math.log(freeSpeedTravelTime), 0.005));
                            else
                                timeData.put(0, new Tuple<Double, Double>(freeSpeedTravelTime, 0.0));

                        } catch (NullPointerException ne) {
                            System.err.printf("Couldnt create STOP-STOP entry for for from: %s, to: %s, route: %s, line: %s\n",
                                    origin, destination, route.getId().toString(), transitLine.getId().toString());
                            continue TRANSITSTOPS;
                        }

                    } else {
                        //value already exists (assuming singular stop-stop network routes)
                        continue TRANSITSTOPS;
                    }

                }
            }
        }
    }


    /**
     * Retrieves the interpolated travel time for a combination of stops at a particular time, or the first/last variance observation if one cannot be  interpolated.
     * <p></p>
     * Returns the error value of infinity for a compbination that doesn't appear in the map
     *
     * @param stopOId
     * @param stopDId
     * @param time
     * @return the travel time, or Double.POSITIVE_INFINITY
     */
    public double getStopStopTime(Id stopOId, Id stopDId, double time) {
        Map<String, Map<Integer, Tuple<Double, Double>>> toMap = stopStopTimes.get(stopOId.toString());

        if (toMap == null) {
            return errorValue(stopOId, stopDId);
        }

        TreeMap<Integer, Tuple<Double, Double>> timedObservations = (TreeMap<Integer, Tuple<Double, Double>>) toMap.get(stopDId.toString());

        if (timedObservations == null) {
            return errorValue(stopOId, stopDId);
        }

        Map.Entry<Integer, Tuple<Double, Double>> ceilingEntry = timedObservations.ceilingEntry((int) time);
        Map.Entry<Integer, Tuple<Double, Double>> floorEntry = timedObservations.floorEntry((int) time);
        if (ceilingEntry == null) {
            if (floorEntry == null) {
                return errorValue(stopOId, stopDId);
            } else {
                return floorEntry.getValue().getFirst();
            }
        } else {
            if (floorEntry == null) {
                return ceilingEntry.getValue().getFirst();
            } else {
                // I have both, so can interpolate a travel time
                double x1 = floorEntry.getKey();
                double x2 = ceilingEntry.getKey();
                double y1 = floorEntry.getValue().getFirst();
                double y2 = ceilingEntry.getValue().getFirst();
                double m = (y2 - y1) / (x2 - x1);
                double x = time - x1;
                return y1 + m * x;
            }
        }
    }

    private double errorValue(Id stopOId, Id stopDId) {
        if (errorCounter < 10) {
            System.err.println("No StopStop data for origin stop " + stopOId.toString() + ", destination stop " + stopDId.toString() + ". Returning estimation...");
        } else if (errorCounter == 10) {
            System.err.println("No StopStop data for origin stop " + stopOId.toString() + ". Skipping further warnings...");
        }
        errorCounter++;

        return Double.POSITIVE_INFINITY;
    }

    /**
     * Retrieves the interpolated travel time variance for a combination of stops at a particular time, or the first/last variance observation if one cannot be  interpolated.
     * <p></p>
     * Returns the error value of infinity for a compbination that doesn't appear in the map
     *
     * @param stopOId
     * @param stopDId
     * @param time    seconds till the start of the sim
     * @return the travel time, or Double.POSITIVE_INFINITY
     */
    public double getStopStopTimeVariance(Id stopOId, Id stopDId, double time) {
        Map<String, Map<Integer, Tuple<Double, Double>>> toMap = stopStopTimes.get(stopOId.toString());

        if (toMap == null) {
            return errorValue(stopOId, stopDId);
        }

        TreeMap<Integer, Tuple<Double, Double>> timedObservations = (TreeMap<Integer, Tuple<Double, Double>>) toMap.get(stopDId.toString());

        if (timedObservations == null) {
            return errorValue(stopOId, stopDId);
        }

        Map.Entry<Integer, Tuple<Double, Double>> ceilingEntry = timedObservations.ceilingEntry((int) time);
        Map.Entry<Integer, Tuple<Double, Double>> floorEntry = timedObservations.floorEntry((int) time);
        if (ceilingEntry == null) {
            if (floorEntry == null) {
                return 0;
            } else {
                return floorEntry.getValue().getSecond();
            }
        } else {
            if (floorEntry == null) {
                return ceilingEntry.getValue().getSecond();
            } else {
                // I have both, so can interpolate a travel time
                double x1 = floorEntry.getKey();
                double x2 = ceilingEntry.getKey();
                double y1 = floorEntry.getValue().getSecond();
                double y2 = ceilingEntry.getValue().getSecond();
                double m = (y2 - y1) / (x2 - x1);
                double x = time - x1;
                return y1 + m * x;
            }
        }
    }


}
