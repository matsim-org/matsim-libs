/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package ch.sbb.matsim.analysis.skims;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorCore.TravelInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Various static methods and a data structure to calculate average adaption times, connection frequency and connection shares based on the rooftop-algorithm from Niek Guis (ca. 2015).
 * <p>
 * For more details, see {@link PTSkimMatrices}.
 *
 * @author mrieser / Simunto
 */
public class RooftopUtils {

    public static List<ODConnection> sortAndFilterConnections(List<ODConnection> connections, double maxDepartureTime) {
        connections.sort((c1, c2) -> Double.compare((c1.departureTime - c1.accessTime), (c2.departureTime - c2.accessTime)));

        // step forward through all connections and figure out which can be ignore because the earlier one is better
        List<ODConnection> filteredConnections1 = new ArrayList<>(connections.size());
        ODConnection earlierConnection = null;
        for (ODConnection connection : connections) {
            if (earlierConnection == null) {
                filteredConnections1.add(connection);
                earlierConnection = connection;
            } else {
                double timeDiff = (connection.departureTime - connection.accessTime) - (earlierConnection.departureTime - earlierConnection.accessTime);
                if (earlierConnection.totalTravelTime() + timeDiff > connection.totalTravelTime() + 0.5) { // +0.5 to catch numerical instabilities in double calculations
                    // connection is better to earlierConnection, use it
                    filteredConnections1.add(connection);
                    earlierConnection = connection;
                }
            }
        }

        // now step backwards through the remaining connections and figure out which can be ignored because the later one is better
        List<ODConnection> filteredConnections = new ArrayList<>();
        ODConnection laterConnection = null;

        for (int i = filteredConnections1.size() - 1; i >= 0; i--) {
            ODConnection connection = filteredConnections1.get(i);
            if (laterConnection == null) {
                filteredConnections.add(connection);
                laterConnection = connection;
            } else {
                double timeDiff = (laterConnection.departureTime - laterConnection.accessTime) - (connection.departureTime - connection.accessTime);
                if (laterConnection.totalTravelTime() + timeDiff > connection.totalTravelTime() + 0.5) { // +0.5 to catch numerical instabilities in double calculations
                    // connection is better to laterConnection, use it
                    if (connection.departureTime - connection.accessTime > maxDepartureTime) {
                        // there should only be one connection after maxDepartureTime
                        filteredConnections.set(0, connection);
                    } else {
                        filteredConnections.add(connection);
                    }
                    laterConnection = connection;
                }
            }
        }

        Collections.reverse(filteredConnections);
        // now the filtered connections are in ascending departure time order

        return filteredConnections;
    }

    public static double calcAverageAdaptionTime(List<ODConnection> connections, double minDepartureTime, double maxDepartureTime) {
        ODConnection prevConnection = null;
        double sum = 0;
        for (ODConnection connection : connections) {
            if (prevConnection != null) {
                double depTime1 = prevConnection.departureTime - prevConnection.accessTime;
                double depTime2 = connection.departureTime - connection.accessTime;
                if (depTime2 > minDepartureTime && depTime1 < maxDepartureTime) {
                    double travelTime1 = prevConnection.totalTravelTime();
                    double travelTime2 = connection.totalTravelTime();
                    double deltaStart = 0;
                    double deltaEnd = 0;
                    if (depTime1 < minDepartureTime) {
                        // shift connection1 to minDepartureTime
                        deltaStart = minDepartureTime - depTime1;
                        depTime1 = minDepartureTime;
                        travelTime1 += deltaStart;
                    }
                    if (depTime2 > maxDepartureTime) {
                        // shift connection2 to maxDepartureTime;
                        deltaEnd = depTime2 - maxDepartureTime;
                        depTime2 = maxDepartureTime;
                        travelTime2 += deltaEnd;
                    }
                    double deltaTravelTime = travelTime2 - travelTime1;
                    double zenith = ((depTime1 + deltaTravelTime) + depTime2) / 2;

                    if (zenith < minDepartureTime) {
                        sum += (depTime2 - minDepartureTime) * (depTime2 - minDepartureTime) / 2;
                    } else if (zenith > maxDepartureTime) {
                        sum += (maxDepartureTime - depTime1) * (maxDepartureTime - depTime1) / 2;
                    } else {
                        sum += (zenith - depTime1) * (zenith - depTime1) / 2;
                        sum += deltaStart * (zenith - depTime1);
                        sum += (depTime2 - zenith) * (depTime2 - zenith) / 2;
                        sum += deltaEnd * (depTime2 - zenith);
                    }
                }
            } else {
                // there is no previous connection
                double depTime = connection.departureTime - connection.accessTime;
                if (depTime >= minDepartureTime && depTime < maxDepartureTime) {
                    // calculate the first triangle
                    sum += (depTime - minDepartureTime) * (depTime - minDepartureTime) / 2;
                }
            }
            prevConnection = connection;
        }
        if (connections.size() == 1) {
            // in this case, the above loop did nothing except setting prevConnection
            double depTime = prevConnection.departureTime - prevConnection.accessTime;
            if (depTime < minDepartureTime) {
                double delta = minDepartureTime - depTime;
                sum = (3600 + delta) * (3600 + delta) / 2 - (delta * delta / 2);
            } else if (depTime > maxDepartureTime) {
                double delta = depTime - maxDepartureTime;
                sum = (3600 + delta) * (3600 + delta) / 2 - (delta * delta / 2);
            } else {
                sum += (maxDepartureTime - depTime) * (maxDepartureTime - depTime) / 2;
            }
        } else if (prevConnection != null) {
            double depTime = prevConnection.departureTime - prevConnection.accessTime;
            if (depTime < maxDepartureTime) {
                // there is no departure after maxDepartureTime, so we're missing the final part
                sum += (maxDepartureTime - depTime) * (maxDepartureTime - depTime) / 2;
            }
        }
        return sum / (maxDepartureTime - minDepartureTime);
    }

    /**
     * calculates the share each connection covers based on minimizing (travelTime + adaptionTime)
     */
    public static Map<ODConnection, Double> calcConnectionShares(List<ODConnection> connections, double minDepartureTime, double maxDepartureTime) {
        Map<ODConnection, Double> shares = new HashMap<>();

        ODConnection prevConnection = null;
        for (ODConnection connection : connections) {
            if (prevConnection != null) {
                double depTime1 = prevConnection.departureTime - prevConnection.accessTime;
                double depTime2 = connection.departureTime - connection.accessTime;
                if (depTime2 > minDepartureTime && depTime1 < maxDepartureTime) {
                    double travelTime1 = prevConnection.totalTravelTime();
                    double travelTime2 = connection.totalTravelTime();
                    if (depTime1 < minDepartureTime) {
                        // shift connection1 to minDepartureTime
                        double delta = minDepartureTime - depTime1;
                        depTime1 = minDepartureTime;
                        travelTime1 += delta;
                    }
                    if (depTime2 > maxDepartureTime) {
                        // shift connection2 to maxDepartureTime;
                        double delta = depTime2 - maxDepartureTime;
                        depTime2 = maxDepartureTime;
                        travelTime2 += delta;
                    }
                    double deltaTravelTime = travelTime2 - travelTime1;
                    double zenith = ((depTime1 + deltaTravelTime) + depTime2) / 2;

                    double share1 = (zenith - depTime1) / 3600;
                    double share2 = (depTime2 - zenith) / 3600;

                    if (share1 < 0) {
                        // this can happen if zenith if before minDepTime
                        share2 += share1;
                        share1 = 0;
                    }
                    if (share2 < 0) {
                        // this can happen if zenith is after maxDepTime
                        share1 += share2;
                        share2 = 0;
                    }

                    final double fShare1 = share1; // variables must be final to be used in lambda expression below
                    final double fShare2 = share2;

                    shares.compute(prevConnection, (c, oldVal) -> (oldVal == null ? fShare1 : (oldVal + fShare1)));
                    shares.compute(connection, (c, oldVal) -> (oldVal == null ? fShare2 : (oldVal + fShare2)));
                }
            } else {
                // there is no previous connection
                double depTime = connection.departureTime - connection.accessTime;
                if (depTime >= minDepartureTime && depTime < maxDepartureTime) {
                    // calculate the first triangle
                    double share = (depTime - minDepartureTime) / 3600;
                    shares.compute(connection, (c, oldVal) -> (oldVal == null ? share : (oldVal + share)));
                }
            }
            prevConnection = connection;
        }
        if (connections.size() == 1) {
            shares.put(prevConnection, 1.0);
        } else if (prevConnection != null) {
            double depTime = prevConnection.departureTime - prevConnection.accessTime;
            if (depTime < maxDepartureTime) {
                // there is no departure after maxDepartureTime, so we're still missing the final part
                double share = (maxDepartureTime - depTime) / 3600;
                shares.compute(prevConnection, (c, oldVal) -> (oldVal == null ? share : (oldVal + share)));
            }
        }
        return shares;
    }

    public static class ODConnection {

        public final double departureTime;
        public final double travelTime;
        public final double accessTime;
        public final double egressTime;
        public final double transferCount;
        public final TravelInfo travelInfo;

        public ODConnection(double departureTime, double travelTime, double accessTime, double egressTime, double transferCount, TravelInfo info) {
            this.departureTime = departureTime;
            this.travelTime = travelTime;
            this.accessTime = accessTime;
            this.egressTime = egressTime;
            this.transferCount = transferCount;
            this.travelInfo = info;
        }

        public double totalTravelTime() {
            return this.accessTime + this.travelTime + this.egressTime;
        }
    }
}
