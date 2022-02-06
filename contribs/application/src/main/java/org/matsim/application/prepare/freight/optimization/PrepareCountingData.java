package org.matsim.application.prepare.freight.optimization;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import picocli.CommandLine;

import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(
        name = "prepare-freight-count",
        description = "Prepare the freight traffic count data",
        showDefaultValues = true
)
public class PrepareCountingData implements MATSimAppCommand {
    @CommandLine.Option(names = "--data", description = "Path to the raw data", required = true)
    private Path rawDataPath;

    @CommandLine.Option(names = "--network", description = "Path to desired network file", required = true)
    private Path networkPath;

    @CommandLine.Option(names = "--output", description = "Path to the output folder", required = true)
    private Path outputFolder;

    @CommandLine.Mixin
    private CrsOptions crs = new CrsOptions(); // Current setup: input EPSG:25832, target EPSG:5677

    public static void main(String[] args) {
        new PrepareCountingData().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        Network network = NetworkUtils.readNetwork(networkPath.toString());

        Counts counts = new Counts();
        counts.setName("BASt Automatische Zählstellen 2019");
        counts.setYear(2019);
        List<Id<Link>> processed = new ArrayList<>();

        String tsvOutputPath = outputFolder.toString() + "/freight-count-data.tsv";
        CSVPrinter tsvWriter = new CSVPrinter(new FileWriter(tsvOutputPath), CSVFormat.TDF);
        tsvWriter.printRecord("nearest_link_id", "total_count", "count_direction_1",
                "count_direction_2", "road_name", "road_type", "link_to_node_x", "link_to_node_y",
                "station_x", "station_y");

        try (CSVParser parser = new CSVParser(Files.newBufferedReader(rawDataPath, StandardCharsets.ISO_8859_1),
                CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                String totalCountString = record.get(37).replace(".", "");
                if (!totalCountString.equals("") && !totalCountString.equals("0")) {
                    String countDirection1String = record.get(38).replace(".", "");
                    String countDirection2String = record.get(39).replace(".", "");
                    String xString = record.get(156).replace(".", "");
                    String yString = record.get(157).replace(".", "");
                    String roadName = record.get(2);
                    String roadType = record.get(5);

                    int totalCount = Integer.parseInt(totalCountString);
                    int count1 = Integer.parseInt(countDirection1String);
                    int count2 = Integer.parseInt(countDirection2String);
                    Coord originalCoord = new Coord(Double.parseDouble(xString), Double.parseDouble(yString));
                    Coord coord = crs.getTransformation().transform(originalCoord);

                    Link link = NetworkUtils.getNearestLink(network, coord);
                    assert link != null;
                    double distance = CoordUtils.distancePointLinesegment
                            (link.getFromNode().getCoord(), link.getToNode().getCoord(), coord);

                    if (distance > 1000 || processed.contains(link.getId())) {
                        continue;
                    }

                    processed.add(link.getId());
                    Count count = counts.createAndAddCount(link.getId(), roadName);
                    double hourlyValue = Math.floor(totalCount / 24.0 + 0.5);
                    for (int i = 1; i < 25; i++) {
                        count.createVolume(i, hourlyValue);
                    }

                    List<String> outputRow = new ArrayList<>();
                    outputRow.add(link.getId().toString());
                    outputRow.add(Integer.toString(totalCount));
                    outputRow.add(Integer.toString(count1));
                    outputRow.add(Integer.toString(count2));
                    outputRow.add(roadName);
                    outputRow.add(roadType);
                    outputRow.add(Double.toString(link.getToNode().getCoord().getX()));
                    outputRow.add(Double.toString(link.getToNode().getCoord().getY()));
                    outputRow.add(Double.toString(coord.getX()));
                    outputRow.add(Double.toString(coord.getY()));

                    tsvWriter.printRecord(outputRow);
                }
            }
        }
        tsvWriter.close();
        CountsWriter writer = new CountsWriter(counts);
        writer.write(outputFolder.toString() + "/count.xml");

        return 0;
    }
}
