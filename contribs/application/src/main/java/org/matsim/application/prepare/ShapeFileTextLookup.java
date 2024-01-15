package org.matsim.application.prepare;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.text.similarity.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.ShpOptions;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;


@CommandLine.Command(
        name = "shapefile-text-lookup",
        description = "Match column from csv with features in shape file and write them as columns.",
        showDefaultValues = true
)
public class ShapeFileTextLookup implements MATSimAppCommand {

    private static final Logger log = LogManager.getLogger(ShapeFileTextLookup.class);

    @CommandLine.Parameters(arity = "1", description = "Input csv")
    private Path input;

    @CommandLine.Option(names = "--output", description = "Output file name", required = true)
    private Path output;

    @CommandLine.Option(names = "--csv-column", description = "Name of the csv column to match", required = true)
    private String csvColumn;

    @CommandLine.Option(names = "--shp-column", description = "Name of the shp feature to match", required = true)
    private String shpColumn;

    @CommandLine.Mixin
    private ShpOptions shp = new ShpOptions();

    @CommandLine.Mixin
    private CsvOptions csvOptions = new CsvOptions();


    public static void main(String[] args) {
        System.exit(new CommandLine(new ShapeFileTextLookup()).execute(args));
    }

    @Override
    public Integer call() throws Exception {

        if (!Files.exists(input)) {
            log.error("Input {} does not exists.", input);
            return 1;
        }

        if (!shp.isDefined()) {
            throw new IllegalArgumentException("Shape file must be given!");
        }

        List<SimpleFeature> features = shp.readFeatures();

        Map<String, SimpleFeature> map = new HashMap<>();

        for (SimpleFeature ft : features) {
            String name = cleanString((String) ft.getAttribute(shpColumn));
            if (map.containsKey(name))
                log.warn("Entry {} is duplicated", name);

            map.put(name, ft);
        }


        EditDistance<Integer> score = new LongestCommonSubsequenceDistance();

        List<String> attributes = new ArrayList<>();
        for (AttributeDescriptor attr : features.get(0).getType().getAttributeDescriptors()) {

            String name = attr.getLocalName();

            if (name.contains("geom"))
                continue;

            attributes.add(attr.getLocalName());
        }


        try (CSVParser reader = csvOptions.createParser(input); CSVPrinter writer = csvOptions.createPrinter(output)) {

            List<String> newHeader = new ArrayList<>();

            newHeader.addAll(reader.getHeaderNames());
            newHeader.addAll(attributes);

            writer.printRecord(newHeader);

            for (CSVRecord record : reader) {

                String toMatch = cleanString(record.get(csvColumn));

                SimpleFeature bestMatch = null;
                double minDist = Integer.MAX_VALUE;

                for (Map.Entry<String, SimpleFeature> e : map.entrySet()) {
                    double dist = score.apply(e.getKey(), toMatch);
                    if (dist < minDist) {
                        minDist = dist;
                        bestMatch = e.getValue();
                    }
                }

                List<Object> row = new ArrayList<>();
                record.iterator().forEachRemaining(row::add);

                SimpleFeature finalBestMatch = bestMatch;
                attributes.forEach(attr -> row.add(finalBestMatch.getAttribute(attr)));
                writer.printRecord(row);
            }
        }

        return 0;
    }

    private String cleanString(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z]", "")
                .replace("region", "")
                .replace("landeshauptstadt", "")
                .replace("hansestadt", "")
                .replace("stadt", "")
                .replace("kreisfreie", "")
                .replace("kreis", "");
    }

}
