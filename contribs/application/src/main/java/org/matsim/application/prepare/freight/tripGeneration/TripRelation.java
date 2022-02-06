package org.matsim.application.prepare.freight.tripGeneration;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The object TripRelation corresponds to each entry (row) in the ketten-2010 table. The information will be readed
 * from the table and stored as the TripRelation. The data can then be used by different tools to generate MATSim
 * plans file.
 */
public class TripRelation {
    /**
     * Start location of the full trip relation
     */
    private final String originalCell;
    /**
     * Start location of the main run; Also the destination of the pre-run (when applicable)
     */
    private final String originalCellMainRun;
    /**
     * Destination of the main run; Also the starting location of the post-run (when applicable)
     */
    private final String destinationCellMainRun;
    /**
     * Destination of the full trip relation
     */
    private final String destinationCell;

    /**
     * Mode encoding: 0 --> unknown/not applicable; 1 --> railway; 2 --> road; 3 --> container ship; Therefore "2" is what we are mainly looking at
     */
    private final String modePreRun;
    /**
     * Mode encoding: 0 --> unknown/not applicable; 1 --> railway; 2 --> road; 3 --> container ship; Therefore "2" is what we are mainly looking at
     */
    private final String modeMainRun;
    /**
     * Mode encoding: 0 --> unknown/not applicable; 1 --> railway; 2 --> road; 3 --> container ship; Therefore "2" is what we are mainly looking at
     */
    private final String modePostRun;

    private final String goodsType;
    private final double tonsPerYear;

    // TODO Additional data (currently, we don't have the lookup table for those data)
    // private final String originalTerminal; // Starting terminal for the main run (also the destination for the pre-run)
    // private final String destinationTerminal; // Destination terminal for main run (also the starting terminal for the post-run)

    public static class Builder {
        private String originalCell;
        private String originalCellMainRun;
        private String destinationCellMainRun;
        private String destinationCell;

        private String modePreRun;
        private String modeMainRun;
        private String modePostRun;

        private String goodsType;
        private double tonsPerYear;

        public Builder originalCell(String value) {
            this.originalCell = value;
            return this;
        }

        public Builder originalCellMainRun(String value) {
            this.originalCellMainRun = value;
            return this;
        }

        public Builder destinationCellMainRun(String value) {
            this.destinationCellMainRun = value;
            return this;
        }

        public Builder destinationCell(String value) {
            this.destinationCell = value;
            return this;
        }

        public Builder modePreRun(String value) {
            this.modePreRun = value;
            return this;
        }

        public Builder modeMainRun(String value) {
            this.modeMainRun = value;
            return this;
        }

        public Builder modePostRun(String value) {
            this.modePostRun = value;
            return this;
        }

        public Builder goodsType(String value) {
            this.goodsType = value;
            return this;
        }

        public Builder tonsPerYear(double value) {
            this.tonsPerYear = value;
            return this;
        }

        public TripRelation build() {
            return new TripRelation(this);
        }
    }

    private TripRelation(Builder builder) {
        this.originalCell = builder.originalCell;
        this.originalCellMainRun = builder.originalCellMainRun;
        this.destinationCellMainRun = builder.destinationCellMainRun;
        this.destinationCell = builder.destinationCell;

        this.modePreRun = builder.modePreRun;
        this.modeMainRun = builder.modeMainRun;
        this.modePostRun = builder.modePostRun;

        this.goodsType = builder.goodsType;
        this.tonsPerYear = builder.tonsPerYear;
    }

    public String getOriginalCell() {
        return originalCell;
    }

    public String getOriginalCellMainRun() {
        return originalCellMainRun;
    }

    public String getDestinationCellMainRun() {
        return destinationCellMainRun;
    }

    public String getDestinationCell() {
        return destinationCell;
    }

    public String getModePreRun() {
        return modePreRun;
    }

    public String getModeMainRun() {
        return modeMainRun;
    }

    public String getModePostRun() {
        return modePostRun;
    }

    public String getGoodsType() {
        return goodsType;
    }

    public double getTonsPerYear() {
        return tonsPerYear;
    }


    public static List<TripRelation> readTripRelations(String pathToKettenData) throws IOException {
        List<TripRelation> tripRelations = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(URI.create(pathToKettenData).toURL(), StandardCharsets.ISO_8859_1,
                CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                Builder builder = new Builder();
                // Read locations
                builder.originalCell(record.get(0)).originalCellMainRun(record.get(2)).
                        destinationCellMainRun(record.get(3)).destinationCell(record.get(1));
                // Read trips
                builder.modePreRun(record.get(6)).modeMainRun(record.get(7)).modePostRun(record.get(8));

                // Read goods type and tons
                builder.goodsType(record.get(10)).tonsPerYear(Double.parseDouble(record.get(16)));

                // Build trip relation and add to list
                tripRelations.add(builder.build());
            }
        }
        return tripRelations;
    }

    public static TripRelation readTripRelation(CSVRecord record) {
        Builder builder = new Builder();
        // Read locations
        builder.originalCell(record.get(0)).originalCellMainRun(record.get(2)).
                destinationCellMainRun(record.get(3)).destinationCell(record.get(1));
        // Read trips
        builder.modePreRun(record.get(6)).modeMainRun(record.get(7)).modePostRun(record.get(8));

        // Read goods type and tons
        builder.goodsType(record.get(10)).tonsPerYear(Double.parseDouble(record.get(16)));

        // Build trip relation and add to list
        return builder.build();
    }

}
