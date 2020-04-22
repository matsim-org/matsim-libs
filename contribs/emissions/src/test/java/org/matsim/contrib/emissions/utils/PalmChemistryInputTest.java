package org.matsim.contrib.emissions.utils;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.emissions.Pollutant;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PalmChemistryInputTest {

    private static final Logger logger = Logger.getLogger(PalmChemistryInputTest.class);
    private static Map<String, Pollutant> pollutantMapping;

    @BeforeClass
    public static void setUpclass() {
        // have some mapping to the same value of the Enum, so that the below code works for both reads, from the original
        // file and the newly written one, which prints the enum strings
        pollutantMapping = new HashMap<>();
        pollutantMapping.put("NO", Pollutant.NOx);
        pollutantMapping.put("NOx", Pollutant.NOx);
        pollutantMapping.put("NO2", Pollutant.NO2);
        pollutantMapping.put("PM25", Pollutant.PM2_5);
        pollutantMapping.put("PM2_5", Pollutant.PM2_5);
        pollutantMapping.put("PM10", Pollutant.PM_non_exhaust);
        pollutantMapping.put("PM_non_exhaust", Pollutant.PM_non_exhaust);
        pollutantMapping.put("CO", Pollutant.CO);
        pollutantMapping.put("CO2", Pollutant.CO2_TOTAL);
        pollutantMapping.put("CO2_TOTAL", Pollutant.CO2_TOTAL);
        pollutantMapping.put("CH4", Pollutant.CH4);
        pollutantMapping.put("SO2", Pollutant.SO2);
        pollutantMapping.put("NH3", Pollutant.NH3);
    }

    private static PalmChemistryInput readFromFile(Path path) {

        try (NetcdfFile file = NetcdfFile.open(path.toString())) {

            List<Integer> times = toIntList(file.findVariable("time"));
            List<Double> x = toDoubleArray(file.findVariable("x"));
            List<Double> y = toDoubleArray(file.findVariable("y"));
            List<String> emissionNames = toStringArray(file.findVariable("emission_name"));
            List<String> timestamps = toStringArray(file.findVariable("timestamp"));

            Variable emissionValues = file.findVariable("emission_values");

            Dimension zDimension = new Dimension("z", 1);
            emissionValues = emissionValues.reduce(Collections.singletonList(zDimension)); // remove z dimension, since it is not used

            // use one second as time bin size
            var chemistryInput = new PalmChemistryInput(1, 10);

            for (int ti = 0; ti < times.size(); ti++) {

                logger.info("Reading from Netcdf file. Timestep is: " + timestamps.get(ti));
                var currentTimeStep = times.get(ti);

                for (int xi = 0; xi < x.size(); xi++) {
                    for (int yi = 0; yi < y.size(); yi++) {

                        Map<Pollutant, Double> pollutionMap = new HashMap<>();
                        Array pollution = emissionValues.read(new int[]{ti, yi, xi, 0}, new int[]{1, 1, 1, emissionNames.size()});
                        float[] values = (float[]) pollution.copyTo1DJavaArray();

                        // write the different pollutants into a map
                        for (int ei = 0; ei < values.length; ei++) {
                            if (values[ei] > 0 && pollutantMapping.containsKey(emissionNames.get(ei))) {
                                double doubleValue = values[ei];
                                pollutionMap.put(pollutantMapping.get(emissionNames.get(ei)), doubleValue);
                            }
                        }
                        var coord = new Coord(x.get(xi), y.get(yi));
                        chemistryInput.addPollution(currentTimeStep, coord, pollutionMap);
                    }
                }
            }
            return chemistryInput;
        } catch (IOException | InvalidRangeException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static List<Integer> toIntList(Variable oneDimensionalVariable) throws IOException {

        if (oneDimensionalVariable.getRank() != 1 || oneDimensionalVariable.getDataType() != DataType.INT)
            throw new IllegalArgumentException("only 1 dimensional variables in this method");

        int[] values = (int[]) oneDimensionalVariable.read().copyTo1DJavaArray();
        return Arrays.stream(values).boxed().collect(Collectors.toList());
    }

    private static List<Double> toDoubleArray(Variable oneDimensionalVariable) throws IOException {

        if (oneDimensionalVariable.getRank() != 1 || oneDimensionalVariable.getDataType() != DataType.DOUBLE)
            throw new IllegalArgumentException("only 1 dimensional variables in this method");

        double[] values = (double[]) oneDimensionalVariable.read().copyTo1DJavaArray();
        return Arrays.stream(values).boxed().collect(Collectors.toList());
    }

    private static List<String> toStringArray(Variable oneDimensionalVariable) throws IOException {

        if (oneDimensionalVariable.getRank() != 2 || oneDimensionalVariable.getDataType() != DataType.CHAR)
            throw new IllegalArgumentException("only 1 dimensional variables in this method");

        ArrayChar stringArray = (ArrayChar) oneDimensionalVariable.read();
        List<String> result = new ArrayList<>();
        for (String s : stringArray) {
            result.add(s);
        }
        return result;
    }

    @Test
    public void testWritingLogic() {

        Path path = Paths.get("C:\\Users\\Janekdererste\\repos\\shared-svn\\projects\\mosaik-2\\data\\emission-driver-input\\erp_itm_chemistry.nc");
        var expectedChemistryInput = readFromFile(path);

        var filePath = Paths.get("C:\\Users\\Janekdererste\\Desktop\\test-netcdf.nc");
        // actually test the writing to a file
        expectedChemistryInput.writeToFile(filePath);

        // now read it in again and compare the two chemistry inputs
        var actualChemistryInput = readFromFile(filePath);

        var expectedIterator = expectedChemistryInput.getData().getTimeBins().iterator();
        var actualIterator = actualChemistryInput.getData().getTimeBins().iterator();

        // test both iterators to make sure both have the exact same number of items
        while (expectedIterator.hasNext() || actualIterator.hasNext()) {

            var expectedBin = expectedIterator.next();
            var actualBin = actualIterator.next();

            logger.info("Testing equality of time bin: " + expectedBin.getStartTime());
            assertEquals(expectedBin.getStartTime(), actualBin.getStartTime(), 0.00001);

            for (var entry : expectedBin.getValue().entrySet()) {

                var expectedCell = entry.getValue();
                var actualCell = actualBin.getValue().get(entry.getKey());

                for (var expectedPollution : expectedCell.getEmissions().entrySet()) {
                    assertTrue(expectedPollution.getKey().toString() + "was not in the actual cell",
                            actualCell.getEmissions().containsKey(expectedPollution.getKey()));

                    var actualPollution = actualCell.getEmissions().get(expectedPollution.getKey());
                    assertEquals(expectedPollution.getValue(), actualPollution, 0.00001);
                }
            }
        }
    }
}