package org.matsim.application.prepare.freight.dataProcessing;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.utils.geometry.geotools.MGC;
import picocli.CommandLine;

import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenerateLookupTable implements MATSimAppCommand {

    @CommandLine.Option(names = "--input", description = "input region list (Verkehrszellen)", required = true)
    private Path input;

    @CommandLine.Option(names = "--germany", description = "german lookup table", required = true)
    private Path germanTable;

    @CommandLine.Option(names = "--international", description = "international lookup table", required = true)
    private Path internationalTable;

    @CommandLine.Option(names = "--output", description = "output lookup table", required = true)
    private Path output;

    @CommandLine.Option(names = "--shp-germany", description = "the NUTS3 shape file (2006 version) for germany", required = true)
    private Path german2006ShapeFilePath;

    @CommandLine.Option(names = "--shp-2021", description = "NUTS shape file 2021 version", required = true)
    private Path shapeFile2021Path;

    public static void main(String[] args) {
        new GenerateLookupTable().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        ShpOptions german2006shp = new ShpOptions(german2006ShapeFilePath, "EPSG:5677", StandardCharsets.UTF_8);
        ShpOptions nuts2021shp = new ShpOptions(shapeFile2021Path, "EPSG:4326", StandardCharsets.UTF_8);
        Map<String, TrafficCellCoreData> coreDataLookupTable = new HashMap<>();

        // Read german lookup table
        Map<String, String> german2006To2021Transformation =
                new GermanNutsTransformation(german2006shp, nuts2021shp).getNuts2006To2021Mapping();
        try (CSVParser parser = new CSVParser(Files.newBufferedReader(germanTable, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                String verkerhszelle = record.get(0);
                String name = record.get(1);
                String nutsId2006 = record.get(3);
                String nutsId2021 = german2006To2021Transformation.get(nutsId2006);
                coreDataLookupTable.put(verkerhszelle, new TrafficCellCoreData(verkerhszelle, name, nutsId2006, nutsId2021));
            }
        }

        // Read international lookup table
        try (CSVParser parser = new CSVParser(Files.newBufferedReader(internationalTable, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                String verkerhszelle = record.get(0);
                String name = record.get(1);
                String nutsId2006 = record.get(2);
                String nutsId2021 = record.get(3);
                coreDataLookupTable.put(verkerhszelle, new TrafficCellCoreData(verkerhszelle, name, nutsId2006, nutsId2021));
            }
        }

        // Read verkehrszellen.csv and then writeout lookup table
        List<SimpleFeature> featuresNuts2021 = nuts2021shp.readFeatures();
        CSVPrinter tsvWriter = new CSVPrinter(new FileWriter(output.toString()), CSVFormat.TDF);
        tsvWriter.printRecord("verkehrszelle", "name", "NUTS_2006", "NUTS_2021", "NUTS_2021_name", "coord_x", "coord_y");
        try (CSVParser parser = new CSVParser(Files.newBufferedReader(input, StandardCharsets.ISO_8859_1),
                CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
            List<String[]> incompleteCellLists = new ArrayList<>();
            for (CSVRecord record : parser) {
                String verkerhszelle = record.get(0);
                String name = record.get(1);
                if (!coreDataLookupTable.containsKey(verkerhszelle)) {
                    incompleteCellLists.add(new String[]{verkerhszelle, name});
                } else {
                    TrafficCellCoreData coreData = coreDataLookupTable.get(verkerhszelle);
                    if (coreData.getNuts2021().equals("")) {
                        incompleteCellLists.add(new String[]{verkerhszelle, name});
                    } else {
                        String nuts2006 = coreData.getNuts2006();
                        String nuts2021 = coreData.getNuts2021();
                        String nuts2021Name = getNutsName(featuresNuts2021, nuts2021);
                        Coord coord = getBackupCoord(featuresNuts2021, nuts2021);
                        tsvWriter.printRecord(verkerhszelle, name, nuts2006, nuts2021, nuts2021Name, coord.getX(), coord.getY());
                    }
                }
            }

            for (String[] verkerhszelleAndName : incompleteCellLists) {
                String verkehrszelle = verkerhszelleAndName[0];
                String name = verkerhszelleAndName[1];
                String nuts2006 = coreDataLookupTable.getOrDefault(verkehrszelle, new TrafficCellCoreData(verkehrszelle, name)).getNuts2006();
                tsvWriter.printRecord(verkehrszelle, name, nuts2006);
            }
        }

        tsvWriter.close();
        return 0;
    }

    private Coord getBackupCoord(List<SimpleFeature> featuresNuts2021, String nuts2021) {
        for (SimpleFeature feature : featuresNuts2021) {
            if (nuts2021.equals(feature.getAttribute("NUTS_ID").toString())) {
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                return MGC.point2Coord(geometry.getCentroid());
            }
        }
        System.err.println("Warning: unable to find NUTS region for " + nuts2021);
        return new Coord(0, 0);
    }

    private String getNutsName(List<SimpleFeature> featuresNuts2021, String nuts2021) {
        for (SimpleFeature feature : featuresNuts2021) {
            if (nuts2021.equals(feature.getAttribute("NUTS_ID").toString())) {
                return feature.getAttribute("NUTS_NAME").toString();
            }
        }
        System.err.println("Warning: unable to find NUTS region for " + nuts2021);
        return "unknown";
    }

    private static class TrafficCellCoreData {
        private final String verkehrszelle;
        private final String name;
        private final String nuts2006;
        private final String nuts2021;

        TrafficCellCoreData(String verkehrszelle, String name, String nuts2006, String nuts2021) {
            this.verkehrszelle = verkehrszelle;
            this.name = name;
            this.nuts2006 = nuts2006;
            this.nuts2021 = nuts2021;
        }

        TrafficCellCoreData(String verkehrszelle, String name) {
            this.verkehrszelle = verkehrszelle;
            this.name = name;
            this.nuts2006 = "";
            this.nuts2021 = "";
        }

        public String getName() {
            return name;
        }

        public String getVerkehrszelle() {
            return verkehrszelle;
        }

        public String getNuts2006() {
            return nuts2006;
        }

        public String getNuts2021() {
            return nuts2021;
        }
    }
}
