package org.matsim.contrib.emissions.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.core.utils.collections.Tuple;
import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class PalmChemistryInput {

    private static final Logger logger = Logger.getLogger(PalmChemistryInput.class);

    private static final String TIME = "time";
    private static final String Z = "z";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String SPECIES = "nspecies";
    private static final String FIELD_LEN = "field_len";
    private static final String EMISSION_NAME = "emission_name";
    private static final String EMISSION_INDEX = "emission_index";
    private static final String TIMESTAMP = "timestamp";
    private static final String EMISSION_VALUES = "emission_values";

    private final TimeBinMap<Map<Coord, Cell>> data;
    private final double cellSize;

    // some housekeeping for creating indices when writing to netcdf file
    private final Set<Pollutant> observedPollutants = new HashSet<>();
    private double minX = Double.POSITIVE_INFINITY;
    private double maxX = Double.NEGATIVE_INFINITY;
    private double minY = Double.POSITIVE_INFINITY;
    private double maxY = Double.NEGATIVE_INFINITY;

    private Cell defaultCell = null;

    public PalmChemistryInput(double timeIntervalInSeconds, double cellSize) {
        this.cellSize = cellSize;
        this.data = new TimeBinMap<>(timeIntervalInSeconds);
    }

    public TimeBinMap<Map<Coord, Cell>> getData() {
        return data;
    }

    /**
     * Writes the data into a csv format is: x,y,time,pollutant1,pollutant2,....
     * <p>
     * This is meant for debugging. One can display rastered pollution in via for example
     *
     * @param file           output file path
     * @param chemistryInput dat which is written to the file
     */
    static void writeToCsv(Path file, PalmChemistryInput chemistryInput) {

        try (var writer = Files.newBufferedWriter(file)) {
            try (var printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

                // print header
                printer.print("x");
                printer.print("y");
                printer.print("time");

                for (Pollutant observedPollutant : chemistryInput.getObservedPollutants()) {
                    printer.print(observedPollutant);
                }
                printer.println();

                // print data
                for (var bin : chemistryInput.getData().getTimeBins()) {

                    var time = bin.getStartTime();

                    for (var coordCellEntry : bin.getValue().entrySet()) {

                        var coord = coordCellEntry.getKey();
                        printer.print(coord.getX());
                        printer.print(coord.getY());
                        printer.print(time);

                        for (Pollutant observedPollutant : chemistryInput.getObservedPollutants()) {
                            var value = coordCellEntry.getValue().getEmissions().get(observedPollutant);
                            if (value == null)
                                value = 0.0;

                            printer.print(value);
                        }
                        printer.println();
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<Pollutant> getObservedPollutants() {
        return observedPollutants;
    }

    public void writeToFile(Path file) {

        try (var writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, file.toString())) {

            writeDimensions(writer);
            writeVariables(writer);
            writeAttributes(writer);
            writeGlobalAttributes(writer);
            writer.create();

            writeData(writer);

        } catch (IOException | InvalidRangeException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeData(NetcdfFileWriter writer) throws IOException, InvalidRangeException {

        List<Pollutant> pollutantToIndex = new ArrayList<>(observedPollutants);

        var emissionIndex = new ArrayInt.D1(pollutantToIndex.size(), false);
        for (var i = 0; i < pollutantToIndex.size(); i++) {
            emissionIndex.set(i, i);
        }

        var emissionNames = new ArrayChar.D2(pollutantToIndex.size(), 64);
        for (var i = 0; i < pollutantToIndex.size(); i++) {
            emissionNames.setString(i, pollutantToIndex.get(i).toString());
        }

        var zValues = new ArrayDouble.D1(1);
        zValues.set(0, 1.0); // the original file sets this to 1 as well
        var xValues = writeDoubleArray(minX, maxX, cellSize, getNumberOfCellsInXDirection());
        var yValues = writeDoubleArray(minY, maxY, cellSize, getNumberOfCellsInYDirection());

        int numberOfConsecutiveTimeBins = (int) ((data.getEndTimeOfLastBin() - data.getStartTime()) / data.getBinSize());
        var times = new ArrayInt.D1(numberOfConsecutiveTimeBins, false);
        var timestamps = new ArrayChar.D2(numberOfConsecutiveTimeBins, 64);
        var emissionValues = new ArrayFloat.D5(numberOfConsecutiveTimeBins, 1, getNumberOfCellsInYDirection(), getNumberOfCellsInXDirection(), pollutantToIndex.size());

        for (var i = 0; i < numberOfConsecutiveTimeBins; i++) {

            var bin = getTimeBin(i, data.getBinSize());

            var timestamp = getTimestamp(bin.getStartTime());
            logger.info("writing timestep: " + timestamp);

            times.set(i, (int) bin.getStartTime());
            timestamps.setString(i, timestamp);

            for (var yi = 0; yi < yValues.getSize(); yi++) {
                for (var xi = 0; xi < xValues.getSize(); xi++) {

                    var x = xValues.get(xi);
                    var y = yValues.get(yi);
                    var cell = bin.getValue().getOrDefault(new Coord(x, y), getDefaultCell());

                    for (var p = 0; p < pollutantToIndex.size(); p++) {
                        var value = cell.getEmissions().getOrDefault(pollutantToIndex.get(p), 0.0).floatValue();
                        emissionValues.set(i, 0, yi, xi, p, value);
                    }
                }
            }
        }

        // still don't know why we need two of these indices
        writer.write(writer.findVariable(SPECIES), emissionIndex);
        writer.write(writer.findVariable(EMISSION_INDEX), emissionIndex);

        writer.write(writer.findVariable(EMISSION_NAME), emissionNames);
        writer.write(writer.findVariable(TIMESTAMP), timestamps);
        writer.write(writer.findVariable(TIME), times);
        writer.write(writer.findVariable(Z), zValues);
        writer.write(writer.findVariable(Y), yValues);
        writer.write(writer.findVariable(X), xValues);
        writer.write(writer.findVariable(EMISSION_VALUES), emissionValues);
    }

    private TimeBinMap.TimeBin<Map<Coord, Cell>> getTimeBin(int index, double timeBinSize) {

        var bin = data.getTimeBin(index * timeBinSize);

        if (!bin.hasValue()) {
            bin.setValue(Map.of());
        }

        return bin;
    }

    private Cell getDefaultCell() {

        if (defaultCell == null) {
            var zeroEmissions = observedPollutants.stream()
                    .map(pollutant -> Tuple.of(pollutant, 0.0))
                    .collect(Collectors.toMap(Tuple::getFirst, Tuple::getSecond));

            defaultCell = new Cell(zeroEmissions);
        }
        return defaultCell;
    }

    private String getTimestamp(double time) {
        var startTimeDuration = Duration.ofSeconds((long) time);
        return String.format("%02d:%02d:%02d", startTimeDuration.toHours(), startTimeDuration.toMinutesPart(), startTimeDuration.toSecondsPart());
    }

    private ArrayDouble.D1 writeDoubleArray(double min, double max, double intervallSize, int size) {
        var result = new ArrayDouble.D1(size);
        var i = 0;
        for (var v = min; v <= max; v += intervallSize) {
            result.set(i, v);
            i++;
        }
        return result;
    }

    private void writeDimensions(NetcdfFileWriter writer) {

        writer.addUnlimitedDimension(TIME);
        writer.addDimension(X, getNumberOfCellsInXDirection());
        writer.addDimension(Y, getNumberOfCellsInYDirection());
        writer.addDimension(Z, 1);
        writer.addDimension(SPECIES, observedPollutants.size());
        // this seems to be necessary to encode strings. I guess each string has 64 bits reserved. I also guess that this means strings may only be 64 bits long.
        writer.addDimension(FIELD_LEN, 64);
    }

    private void writeVariables(NetcdfFileWriter writer) {

        writer.addVariable(SPECIES, DataType.INT, SPECIES);
        writer.addVariable(EMISSION_NAME, DataType.CHAR,
                List.of(writer.findDimension(SPECIES), writer.findDimension(FIELD_LEN)));
        writer.addVariable(EMISSION_INDEX, DataType.FLOAT, SPECIES);
        writer.addVariable(TIMESTAMP, DataType.CHAR, List.of(
                writer.findDimension(TIME), writer.findDimension(FIELD_LEN)
        ));
        writer.addVariable(TIME, DataType.INT, TIME);
        writer.addVariable(Z, DataType.DOUBLE, Z);
        writer.addVariable(Y, DataType.DOUBLE, Y);
        writer.addVariable(X, DataType.DOUBLE, X);
        writer.addVariable(EMISSION_VALUES, DataType.FLOAT,
                // order of the dimensions is important, since access is simply index based
                List.of(writer.findDimension(TIME), writer.findDimension(Z), writer.findDimension(Y), writer.findDimension(X), writer.findDimension(SPECIES))
        );
    }

    private void writeAttributes(NetcdfFileWriter writer) {
        writer.findVariable(SPECIES).addAttribute(new Attribute("long_name", "nspecies"));
        writer.findVariable(EMISSION_NAME).addAttribute(new Attribute("long_name", "emission species name"));
        writer.findVariable(EMISSION_INDEX).addAttribute(new Attribute("long_name", "emission species index"));
        writer.findVariable(EMISSION_INDEX).addAttribute(new Attribute("_Fill_Value", -9999.9F));
        writer.findVariable(TIMESTAMP).addAttribute(new Attribute("long_name", "time stamp"));
        writer.findVariable(TIME).addAttribute(new Attribute("long_name", "time"));
        writer.findVariable(TIME).addAttribute(new Attribute("units", "s"));
        writer.findVariable(X).addAttribute(new Attribute("units", "m"));
        writer.findVariable(Y).addAttribute(new Attribute("units", "m"));
        writer.findVariable(Z).addAttribute(new Attribute("units", "m"));
        writer.findVariable(EMISSION_VALUES).addAttribute(new Attribute("long_name", "emission values"));
        writer.findVariable(EMISSION_VALUES).addAttribute(new Attribute("_Fill_Value", -999.9F));
        // unclear whether it is this unit in our case...
        writer.findVariable(EMISSION_VALUES).addAttribute(new Attribute("units", "g/m2/hour"));
    }

    private void writeGlobalAttributes(NetcdfFileWriter writer) {
        writer.addGlobalAttribute("description", "PALM Chemistry Data");
        writer.addGlobalAttribute("author", "VSP - TU Berlin");
        writer.addGlobalAttribute("lod", 2);
        writer.addGlobalAttribute("legacy_mode", "yes");
    }

    private void updateBounds(Coord coord) {

        if (coord.getX() > maxX) maxX = coord.getX();
        if (coord.getX() < minX) minX = coord.getX();
        if (coord.getY() > maxY) maxY = coord.getY();
        if (coord.getY() < minY) minY = coord.getY();
    }

    private void updateObservedPollutants(Set<Pollutant> pollutants) {
        observedPollutants.addAll(pollutants);
    }

    private int getNumberOfCellsInXDirection() {
        return (int) ((maxX - minX) / cellSize + 1);
    }

    private int getNumberOfCellsInYDirection() {
        return (int) ((maxY - minY) / cellSize + 1);
    }

    static class Cell {

        private final Map<Pollutant, Double> emissions;

        private Cell(Map<Pollutant, Double> emissions) {
            this.emissions = emissions;
        }

        private static Cell merge(Cell c1, Cell c2) {

            for (var valueByPollutant : c2.getEmissions().entrySet()) {
                c1.emissions.merge(valueByPollutant.getKey(), valueByPollutant.getValue(), Double::sum);
            }
            // I guess this is not pure at all, but will do the job...
            return c1;
        }

        public Map<Pollutant, Double> getEmissions() {
            return emissions;
        }

        private void addEmissions(Map<Pollutant, Double> pollution, int divideBy) {
            for (var pollutant : pollution.entrySet()) {
                emissions.merge(pollutant.getKey(), pollutant.getValue() / divideBy, Double::sum);
            }
        }
    }

    public void addPollution(double time, Coord coord, Map<Pollutant, Double> valuesByPollutant) {

        updateBounds(coord);
        updateObservedPollutants(valuesByPollutant.keySet());

        if (time > 3600) {
            var stopHere = 0;
        }

        var timeBin = data.getTimeBin(time);

        if (!timeBin.hasValue()) timeBin.setValue(new HashMap<>());

        var cells = timeBin.getValue();

        // make a defensive copy and feed an individual hash map into each cell.
        // probably this could be programmed faster, but I guess this does for now.
        cells.merge(coord, new Cell(new HashMap<>(valuesByPollutant)), Cell::merge);
    }
}
