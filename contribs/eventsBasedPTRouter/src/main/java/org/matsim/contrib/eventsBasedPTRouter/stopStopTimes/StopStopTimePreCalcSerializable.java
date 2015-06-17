package org.matsim.contrib.eventsBasedPTRouter.stopStopTimes;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * <B>When StopStopTimes are generated externally, e.g. by a regression.</B>
 * <p/>
 * See constructor for input format.
 * </P>
 */
public class StopStopTimePreCalcSerializable implements StopStopTime, Serializable {

    private final Map<String, Map<String, Map<Integer, Tuple<Double, Double>>>> stopStopTimes = new HashMap<>();
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
     * @param inputFile: path to the tab-separated file.Format is fromStopId (String), toStopId (String), time of record (seconds, double),
     *                   travelTime (seconds, double), travelTimeVariance (seconds**2, double). No headings or row numbers.
     */
    public StopStopTimePreCalcSerializable(String inputFile) {
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
                Tuple<Double, Double> timeVar = timeData.get(Integer.parseInt(split[2]));
                if (timeVar == null) {
                    timeVar = new Tuple<>(Double.parseDouble(split[3]), Double.parseDouble(split[4]));
                }
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
    }

    /**
     * for testing purposes only
     */
    StopStopTimePreCalcSerializable() {

    }



    public double getStopStopTime(Id stopOId, Id stopDId, double time) {
        Map<String, Map<Integer, Tuple<Double, Double>>> toMap = stopStopTimes.get(stopOId.toString());

        if (toMap == null) {
            return errorValue(stopOId,stopDId);
        }

        TreeMap<Integer, Tuple<Double, Double>> timedObservations = (TreeMap<Integer, Tuple<Double, Double>>) toMap.get(stopDId.toString());

        if (timedObservations == null) {
            return errorValue(stopOId,stopDId);
        }

        Map.Entry<Integer, Tuple<Double, Double>> ceilingEntry = timedObservations.ceilingEntry((int) time);
        Map.Entry<Integer, Tuple<Double, Double>> floorEntry = timedObservations.floorEntry((int) time);
        if(ceilingEntry == null){
            if(floorEntry == null){
                return errorValue(stopOId,stopDId);
            }else{
                return floorEntry.getValue().getFirst();
            }
        }else{
            if(floorEntry == null){
                return ceilingEntry.getValue().getFirst();
            }else{
                // I have both, so can interpolate a travel time
                double x1 = floorEntry.getKey();
                double x2 = ceilingEntry.getKey();
                double y1 = floorEntry.getValue().getFirst();
                double y2 = ceilingEntry.getValue().getFirst();
                double m = (y2-y1)/(x2 -x1);
                double x = time - x1;
                return y1 + m * x;
            }
        }
    }

    private double errorValue(Id stopOId, Id stopDId) {
            if (errorCounter < 10) {
                System.err.println("No StopStop data for origin stop " + stopOId.toString()+ ", destination stop "+stopDId.toString()+". Returning 3600 seconds...");
            } else {
                System.err.println("No StopStop data for origin stop " + stopOId.toString() + ". Skipping further warnings...");
            }
            errorCounter++;
            return 3600;
    }

    public double getStopStopTimeVariance(Id stopOId, Id stopDId, double time) {
        Map<String, Map<Integer, Tuple<Double, Double>>> toMap = stopStopTimes.get(stopOId.toString());

        if (toMap == null) {
            return errorValue(stopOId,stopDId);
        }

        TreeMap<Integer, Tuple<Double, Double>> timedObservations = (TreeMap<Integer, Tuple<Double, Double>>) toMap.get(stopDId.toString());

        if (timedObservations == null) {
            return errorValue(stopOId,stopDId);
        }

        Map.Entry<Integer, Tuple<Double, Double>> ceilingEntry = timedObservations.ceilingEntry((int) time);
        Map.Entry<Integer, Tuple<Double, Double>> floorEntry = timedObservations.floorEntry((int) time);
        if(ceilingEntry == null){
            if(floorEntry == null){
                return errorValue(stopOId,stopDId);
            }else{
                return floorEntry.getValue().getSecond();
            }
        }else{
            if(floorEntry == null){
                return ceilingEntry.getValue().getSecond();
            }else{
                // I have both, so can interpolate a travel time
                double x1 = floorEntry.getKey();
                double x2 = ceilingEntry.getKey();
                double y1 = floorEntry.getValue().getSecond();
                double y2 = ceilingEntry.getValue().getSecond();
                double m = (y2-y1)/(x2 -x1);
                double x = time - x1;
                return y1 + m * x;
            }
        }
    }


}
