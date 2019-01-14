package org.matsim.contrib.emissions.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.analysis.spatial.Grid;
import org.matsim.contrib.analysis.spatial.HexagonalGrid;
import org.matsim.contrib.analysis.spatial.SpatialInterpolation;
import org.matsim.contrib.analysis.spatial.SquareGrid;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.types.Pollutant;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class may be used to collect emission events of some events file and assign those emissions to a grid structure.
 * Additionally those emissions are divided into time bins
 */
public class EmissionGridAnalyzer {

    private final double binSize;
    private final double smoothingRadius;
    private final double countScaleFactor;

    private static GeometryFactory factory = new GeometryFactory();
    private final GridType gridType;
    private final double gridSize;
    private final Network network;

    private EmissionGridAnalyzer(final double binSize, final double gridSize, final double smoothingRadius,
                                 final double countScaleFactor, final GridType gridType, final Network network) {
        this.binSize = binSize;
        this.network = network;
        this.gridType = gridType;
        this.gridSize = gridSize;
        this.smoothingRadius = smoothingRadius;
        this.countScaleFactor = countScaleFactor;
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

        timeBinsWithEmissions.getTimeBins().forEach(bin -> {
            Grid<Map<Pollutant, Double>> grid = writeAllLinksToGrid(bin.getValue());
            result.getTimeBin(bin.getStartTime()).setValue(grid);
        });
        return result;
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
     * @param eventsFile Path to the events file e.g. '/path/to/events.xml.gz
     * @param jsonFile Path to the output file e.g. '/path/to/emissions.json
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
        eventsReader.readFile(eventsFile);
        return handler.getTimeBins();
    }

    private Grid<Map<Pollutant, Double>> writeAllLinksToGrid(Map<Id<Link>, EmissionsByPollutant> linksWithEmissions) {

        Grid<Map<Pollutant, Double>> grid = createGrid();

        linksWithEmissions.forEach((id, emission) -> {
            Link link = network.getLinks().get(id);
            if (network.getLinks().containsKey(id))
                processLink(link, emission, grid);
        });
        return grid;
    }

    private void processLink(Link link, EmissionsByPollutant emissions, Grid<Map<Pollutant, Double>> grid) {

        grid.getCells().forEach(cell -> {
            double weight = SpatialInterpolation.calculateWeightFromLine(
                    transformToCoordinate(link.getFromNode()), transformToCoordinate(link.getToNode()),
                    cell.getCoordinate(), smoothingRadius);
            processCell(cell, emissions, weight);
        });
    }

    private void processCell(Grid.Cell<Map<Pollutant, Double>> cell, EmissionsByPollutant emissions, double weight) {

        // merge both maps from cell and linkemissions and sum up values, while the link emissions are multiplied by
        // the cell weight
        Map<Pollutant, Double> newValues = Stream.concat(cell.getValue().entrySet().stream(), emissions.getEmissions().entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (cellValue, linkValue) -> cellValue + linkValue * weight * countScaleFactor));
        cell.setValue(newValues);
    }

    private Geometry getBoundsFromNetwork() {
        double[] box = NetworkUtils.getBoundingBox(network.getNodes().values());
        return factory.createPolygon(new Coordinate[]{
                new Coordinate(box[0], box[1]),
                new Coordinate(box[2], box[1]),
                new Coordinate(box[2], box[3]),
                new Coordinate(box[0], box[3]),
                new Coordinate(box[0], box[1])});
    }

    private Grid<Map<Pollutant, Double>> createGrid() {

        Geometry bounds = getBoundsFromNetwork();
        if (gridType == GridType.Hexagonal)
            return new HexagonalGrid<>(gridSize, HashMap::new, bounds);
        else
            return new SquareGrid<>(gridSize, HashMap::new, bounds);
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
        private GridType gridType = GridType.Square;

        /**
         * Sets the duration of a time bin
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
         * @param type The grid type. Currently either Square or Hexagonal
         * @return {@link org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer.Builder}
         */
        public Builder withGridType(GridType type) {
            this.gridType = type;
            return this;
        }

        /**
         * Sets the horizontal distance between grid cells.
         * @param size The horizontal distance between grid cells. Should conform to the units used by the supplied network.
         * @return {@link org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer.Builder}
         */
        public Builder withGridSize(double size) {
            gridSize = size;
            return this;
        }

        /**
         * Sets the smoothing radius for the spatial interpolation of pollution over grid cells
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
         * @param network a network
         * @return {@link org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer.Builder}
         */
        public Builder withNetwork(Network network) {
            this.network = network;
            return this;
        }

        /**
         * Builds a new Instance of {@link org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer}
         * @return {@link org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer}
         * @throws java.lang.IllegalArgumentException if binSize, gridSize, smoothingRadius are <= 0, and if
         * network was not set
         */
        public EmissionGridAnalyzer build() {

            if (!isValid())
                throw new IllegalArgumentException("binSize, gridSize, smoothingRadius must be set and greater 0, Also network must be set");

            return new EmissionGridAnalyzer(binSize, gridSize, smoothingRadius, countScaleFactor, gridType, network);
        }

        private boolean isValid() {
            return binSize > 0 && gridSize > 0 && smoothingRadius > 0 && network != null;
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
