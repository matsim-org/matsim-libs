/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.emissions.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.analysis.spatial.Grid;
import org.matsim.contrib.analysis.spatial.HexagonalGrid;
import org.matsim.contrib.analysis.spatial.SpatialInterpolation;
import org.matsim.contrib.analysis.spatial.SquareGrid;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.Tuple;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class may be used to collect emission events of some events file and assign those emissions to a grid structure.
 * Additionally, those emissions are divided into time bins
 */
public class EmissionGridAnalyzer {

    private static final Double minimumThreshold = 1e-6;
    private static final Logger logger = LogManager.getLogger(EmissionGridAnalyzer.class);

    private final double binSize;
    private final double smoothingRadius;
    private final double countScaleFactor;

    private static final GeometryFactory factory = new GeometryFactory();
    private final GridType gridType;
    private final double gridSize;
    private final Network network;
    private final PreparedGeometry bounds;
    private Iterator<TimeBinMap.TimeBin<Map<Id<Link>, EmissionsByPollutant>>> timeBins;

    private EmissionGridAnalyzer(final double binSize, final double gridSize, final double smoothingRadius,
                                 final double countScaleFactor, final GridType gridType, final Network network,
                                 final PreparedGeometry bounds) {
        this.binSize = binSize;
        this.network = network;
        this.gridType = gridType;
        this.gridSize = gridSize;
        this.smoothingRadius = smoothingRadius;
        this.countScaleFactor = countScaleFactor;
        this.bounds = bounds;
        this.timeBins = null;
    }

    /**
     * Processes the events file of the given path. The events file must already contain emission events. The emission
     * events will be divided into time bins, as configured by the binSize parameter in the constructor. Within each time
     * bin all the emission events for each link are collected and their emissions are summed up by pollutant. After
     * processing the events the impact of the emissions per link onto each grid cell is calculated by using a gaussian
     * blur.
     *
     * @param eventsFile Path to the events file e.g. '/path/to/events.xml.gz
     * @return TimeBinMap containing a grid which maps pollutants to values.
     */
    public TimeBinMap<Grid<Map<Pollutant, Double>>> process(String eventsFile) {

        TimeBinMap<Map<Id<Link>, EmissionsByPollutant>> timeBinsWithEmissions = processEventsFile(eventsFile);
        TimeBinMap<Grid<Map<Pollutant, Double>>> result = new TimeBinMap<>(binSize);

        logger.info("Starting grid computation...");

        for (var bin : timeBinsWithEmissions.getTimeBins()) {
            logger.info("creating grid for time bin with start time: {}", bin.getStartTime());
            Grid<Map<Pollutant, Double>> grid = writeAllLinksToGrid(bin.getValue());
            result.getTimeBin(bin.getStartTime()).setValue(grid);
        }

        return result;
    }


    /**
     * Process the events file of the given path, and set up an iterator that will be used for
     * returning the results one bin at a time; see processNextTimeBin().
     *
     * @param eventsFile /path/to/input/events-file.xml.gz
     */
    public void processTimeBinsWithEmissions(String eventsFile) {

        TimeBinMap<Map<Id<Link>, EmissionsByPollutant>> emissionsByPollutant = processEventsFile(eventsFile);

        logger.info("!! Event data ready for first time bin grid.");
        this.timeBins = emissionsByPollutant.getTimeBins().iterator();
    }

    /**
     * Whether or not there are more time bins to process
     *
     * @throws RuntimeException if processTimeBinsWithEmissions was not called before this method
     */
    public boolean hasNextTimeBin() {
        if (this.timeBins == null) throw new RuntimeException("Must call processTimeBinsWithEmissions() first.");

        return this.timeBins.hasNext();
    }

    /**
     * Generate the emissions grid data for the next time bin. Requires that the events file has already
     * been processed by calling processTimeBinsWithEmissions(). This method should be called in a loop
     * to exhaust the time bin iterator.
     *
     * @return tuple containing the start time of the next time bin and the emissions grid for that time bin. Returns
     * null if there are no further time bins.
     */
    public Tuple<Double, String> processNextTimeBin() {
        if (this.timeBins == null) throw new RuntimeException("Must call processTimeBinsWithEmissions() first.");
        if (!this.timeBins.hasNext()) throw new RuntimeException("processNextTimeBin() was called too many times");

        TimeBinMap.TimeBin<Map<Id<Link>, EmissionsByPollutant>> nextBin = this.timeBins.next();
        logger.info("creating grid for time bin with start time: {}", nextBin.getStartTime());

        Grid<Map<Pollutant, Double>> grid = writeAllLinksToGrid(nextBin.getValue());

        ObjectMapper mapper = createObjectMapper();
        try {
            return Tuple.of(nextBin.getStartTime(), mapper.writeValueAsString(grid));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Works like {@link org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer#process(String)} but writes the
     * result into a Json-String
     *
     * @param eventsFile Path to the events file e.g. '/path/to/events.xml.gz
     * @return TimeBinMap containing a grid which maps pollutants to values as JSON-String
     */
    public String processToJsonString(String eventsFile) {

        ObjectMapper mapper = createObjectMapper();
        TimeBinMap<Grid<Map<Pollutant, Double>>> result = process(eventsFile);
        try {
            return mapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Works like {@link org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer#process(String)} but writes the
     * result into a JSON-File
     *
     * @param eventsFile Path to the events file e.g. '/path/to/events.xml.gz
     * @param jsonFile   Path to the output file e.g. '/path/to/emissions.json
     */
    public void processToJsonFile(String eventsFile, String jsonFile) {

        ObjectMapper mapper = createObjectMapper();
        TimeBinMap<Grid<Map<Pollutant, Double>>> result = process(eventsFile);

        try {
            mapper.writeValue(new File(jsonFile), result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TimeBinMap<Map<Id<Link>, EmissionsByPollutant>> processEventsFile(String eventsFile) {

        EventsManager eventsManager = EventsUtils.createEventsManager();
        EmissionEventsReader eventsReader = new EmissionEventsReader(eventsManager);
        EmissionsOnLinkEventHandler handler = new EmissionsOnLinkEventHandler(binSize);
        eventsManager.addHandler(handler);
        eventsManager.initProcessing();
        eventsReader.readFile(eventsFile);
        eventsManager.finishProcessing();
        return handler.getTimeBins();
    }

    private Grid<Map<Pollutant, Double>> writeAllLinksToGrid(Map<Id<Link>, EmissionsByPollutant> linksWithEmissions) {

        final var grid = createGrid();
        final var counter = new AtomicInteger();

        // using stream's forEach here, instead of for each loop, to parallelize processing
        linksWithEmissions.entrySet().parallelStream()
                .forEach(entry -> {
                    var count = counter.incrementAndGet();
                    if (count % 10000 == 0)
                        logger.info("processing: {}% done", count * 100 / linksWithEmissions.keySet().size());

                    if (network.getLinks().containsKey(entry.getKey()) && isWithinBounds(network.getLinks().get(entry.getKey()))) {
                        processLink(network.getLinks().get(entry.getKey()), entry.getValue(), grid);
                    }
                });

        grid.getCells().parallelStream()
                .forEach(cell -> removeTinyValuesFromResults(cell.getValue()));

        return grid;
    }

    private void processLink(Link link, EmissionsByPollutant emissions, Grid<Map<Pollutant, Double>> grid) {

        // create a clipping area to speed up calculation time
        // use 5*smoothing radius as longer distances result in a weighting of effectively 0
        Geometry clip = factory.createPoint(new Coordinate(link.getCoord().getX(), link.getCoord().getY())).buffer(smoothingRadius * 5);

        for (var cell : grid.getCells(clip)) {
            double normalizationFactor = grid.getCellArea() / (Math.PI * smoothingRadius * smoothingRadius);
            double weight = SpatialInterpolation.calculateWeightFromLine(
                    transformToCoordinate(link.getFromNode()), transformToCoordinate(link.getToNode()),
                    cell.getCoordinate(), smoothingRadius);
            processCell(cell, emissions, weight * normalizationFactor);
        }
    }

    private void processCell(Grid.Cell<Map<Pollutant, Double>> cell, EmissionsByPollutant emissions, double weight) {

        for (var entry : emissions.getEmissions().entrySet()) {
            var emissionValue = entry.getValue() * weight * countScaleFactor;
            cell.getValue().merge(entry.getKey(), emissionValue, Double::sum);
        }
    }

    private void removeTinyValuesFromResults(Map<Pollutant, Double> values) {
        values.entrySet().removeIf(entry -> entry.getValue() < minimumThreshold);
    }

    private boolean isWithinBounds(Link link) {
        Point linkCentroid = factory.createPoint(new Coordinate(link.getCoord().getX(), link.getCoord().getY()));
        return bounds.contains(linkCentroid);
    }

    private Grid<Map<Pollutant, Double>> createGrid() {

        if (gridType == GridType.Hexagonal)
            return new HexagonalGrid<>(gridSize, ConcurrentHashMap::new, bounds);
        else
            return new SquareGrid<>(gridSize, ConcurrentHashMap::new, bounds);
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper result = new ObjectMapper();
        result.addMixIn(Coordinate.class, CoordinateMixin.class);
        return result;
    }

    private Coordinate transformToCoordinate(Node node) {
        return new Coordinate(node.getCoord().getX(), node.getCoord().getY());
    }

    public enum GridType {Square, Hexagonal}

    /**
     * Builder to configure a new {@link org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer} instance
     */
    public static class Builder {
        private double binSize;
        private double gridSize;
        private double smoothingRadius = 1.0;
        private double countScaleFactor = 1.0;
        private Network network;
        private PreparedGeometry bounds;
        private GridType gridType = GridType.Square;

        /**
         * Sets the duration of a time bin
         *
         * @param size duration of a time bin. Usually MATSim uses seconds as time unit but the implementation doesn't
         *             really care.
         * @return {@link org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer.Builder}
         */
        public Builder withTimeBinSize(double size) {
            this.binSize = size;
            return this;
        }

        /**
         * Sets the used grid type. Default is Square
         *
         * @param type The grid type. Currently, either Square or Hexagonal
         * @return {@link org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer.Builder}
         */
        public Builder withGridType(GridType type) {
            this.gridType = type;
            return this;
        }

        /**
         * Sets the horizontal distance between grid cells.
         *
         * @param size The horizontal distance between grid cells. Should conform to the units used by the supplied network.
         * @return {@link org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer.Builder}
         */
        public Builder withGridSize(double size) {
            gridSize = size;
            return this;
        }

        /**
         * Sets the smoothing radius for the spatial interpolation of pollution over grid cells
         *
         * @param radius The radius where things are smoothed. Should be the same unit as the gridSize
         * @return {@link org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer.Builder}
         */
        public Builder withSmoothingRadius(double radius) {
            smoothingRadius = radius;
            return this;
        }

        /**
         * Sets the count scale factor by which the emission values for each link are multiplied
         *
         * @param factor count scale factor of the scenario
         * @return {@link org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer.Builder}
         */
        public Builder withCountScaleFactor(double factor) {
            this.countScaleFactor = factor;
            return this;
        }

        /**
         * MATSim network that was used for the simulation run
         *
         * @param network a network
         * @return {@link org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer.Builder}
         */
        public Builder withNetwork(Network network) {
            this.network = network;
            return this;
        }

        public Builder withBounds(Geometry bounds) {
            this.bounds = new PreparedGeometryFactory().create(bounds);
            return this;
        }

        public Builder withBounds(PreparedGeometry bounds) {
            this.bounds = bounds;
            return this;
        }

        /**
         * Builds a new Instance of {@link org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer}
         *
         * @return {@link org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer}
         * @throws java.lang.IllegalArgumentException if binSize, gridSize, smoothingRadius are <= 0, and if
         *                                            network was not set
         */
        public EmissionGridAnalyzer build() {

            if (!isValidParameters())
                throw new IllegalArgumentException("binSize, gridSize, smoothingRadius must be set and greater 0, Also network must be set");

            if (!isValidSmoothingRadiusToGridSizeRatio())
                throw new IllegalArgumentException("A smoothing radius smaller than the grid size may lead to artifacts.In fact: Smoothing radius should be much bigger than grid size!");

            if (bounds == null)
                bounds = new PreparedGeometryFactory().create(createBounds());
            return new EmissionGridAnalyzer(binSize, gridSize, smoothingRadius, countScaleFactor, gridType, network, bounds);
        }

        private boolean isValidParameters() {
            return binSize > 0 && gridSize > 0 && smoothingRadius > 0 && network != null;
        }

        private boolean isValidSmoothingRadiusToGridSizeRatio() {
            return smoothingRadius >= gridSize;
        }

        private Geometry createBounds() {

            double[] box = NetworkUtils.getBoundingBox(network.getNodes().values());
            return factory.createPolygon(new Coordinate[]{
                    new Coordinate(box[0], box[1]),
                    new Coordinate(box[2], box[1]),
                    new Coordinate(box[2], box[3]),
                    new Coordinate(box[0], box[3]),
                    new Coordinate(box[0], box[1])});
        }
    }


    /**
     * Mixin to suppress the printing of z-coordinates when grid is serialized to json
     */
    private static class CoordinateMixin {

        @JsonIgnore
        double z;
    }
}
