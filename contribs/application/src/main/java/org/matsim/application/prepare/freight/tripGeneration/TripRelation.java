package org.matsim.application.prepare.freight.tripGeneration;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The object TripRelation corresponds to each entry (row) in the ketten-2010 table. The information will be read
 * from the table and stored as the TripRelation. The data can then be used by different tools to generate MATSim
 * plans file.
 */
public class TripRelation {
	public static final String column_originCell = "Quellzelle";
	public static final String column_originCell_MainRun = "QuellzelleHL";
	public static final String column_destinationCell_MainRun = "ZielzelleHL";
	public static final String column_destinationCell = "Zielzelle";
	public static final String column_mode_PreRun = "ModeVL";
	public static final String column_mode_MainRun = "ModeHL";
	public static final String column_mode_PostRun = "ModeNL";
	public static final String column_goodsType_PreRun = "GütergruppeVL";
	public static final String column_goodsType_MainRun = "GütergruppeHL";
	public static final String column_goodsType_PostRun = "GütergruppeNL";
	public static final String column_tones_PreRun = "TonnenVL";
	public static final String column_tones_MainRun = "TonnenHL";
	public static final String column_tones_PostRun = "TonnenNL";
	public static final String column_tonKM_PreRun = "TkmVL";
	public static final String column_tonKM_MainRun = "TkmHL";
	public static final String column_tonKM_PostRun = "TkmNL";
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

	private final String goodsTypePreRun;
	private final String goodsTypeMainRun;
	private final String goodsTypePostRun;
	private final double tonsPerYearPreRun;
	private final double tonsPerYearMainRun;
	private final double tonsPerYearPostRun;
	private final double tonKMPerYearPreRun;
	private final double tonKMPerYearMainRun;
	private final double tonKMPerYearPostRun;

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

		private String goodsTypePreRun;
		private String goodsTypeMainRun;
		private String goodsTypePostRun;
		private double tonsPerYearPreRun;
		private double tonsPerYearMainRun;
		private double tonsPerYearPostRun;
		private double tonKMPerYearPreRun;
		private double tonKMPerYearMainRun;
		private double tonKMPerYearPostRun;

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

		public Builder goodsTypePreRun(String value) {
			this.goodsTypePreRun = value;
			return this;
		}

		public Builder goodsTypeMainRun(String value) {
			this.goodsTypeMainRun = value;
			return this;
		}

		public Builder goodsTypePostRun(String value) {
			this.goodsTypePostRun = value;
			return this;
		}

		public Builder tonsPerYearPreRun(double value) {
			this.tonsPerYearPreRun = value;
			return this;
		}

		public Builder tonsPerYearMainRun(double value) {
			this.tonsPerYearMainRun = value;
			return this;
		}

		public Builder tonsPerYearPostRun(double value) {
			this.tonsPerYearPostRun = value;
			return this;
		}
		public Builder tonKMPerYearPreRun(double value) {
			this.tonKMPerYearPreRun = value;
			return this;
		}

		public Builder tonKMPerYearMainRun(double value) {
			this.tonKMPerYearMainRun = value;
			return this;
		}

		public Builder tonKMPerYearPostRun(double value) {
			this.tonKMPerYearPostRun = value;
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

		this.goodsTypePreRun = builder.goodsTypePreRun;
		this.goodsTypeMainRun = builder.goodsTypeMainRun;
		this.goodsTypePostRun = builder.goodsTypePostRun;

		this.tonsPerYearPreRun = builder.tonsPerYearPreRun;
		this.tonsPerYearMainRun = builder.tonsPerYearMainRun;
		this.tonsPerYearPostRun = builder.tonsPerYearPostRun;

		this.tonKMPerYearPreRun = builder.tonKMPerYearPreRun;
		this.tonKMPerYearMainRun = builder.tonKMPerYearMainRun;
		this.tonKMPerYearPostRun = builder.tonKMPerYearPostRun;
	}

	public String getOriginalOriginCell() {
		return originalCell;
	}

	public String getOriginalCellMainRun() {
		return originalCellMainRun;
	}

	public String getDestinationCellMainRun() {
		return destinationCellMainRun;
	}

	public String getFinalDestinationCell() {
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

	public String getGoodsTypePreRun() {
		return goodsTypePreRun;
	}

	public String getGoodsTypeMainRun() {
		return goodsTypeMainRun;
	}

	public String getGoodsTypePostRun() {
		return goodsTypePostRun;
	}

	public double getTonsPerYearPreRun() {
		return tonsPerYearPreRun;
	}
	public double getTonsPerYearMainRun() {
		return tonsPerYearMainRun;
	}
	public double getTonsPerYearPostRun() {
		return tonsPerYearPostRun;
	}
	public double getTonKMPerYearPreRun() {
		return tonKMPerYearPreRun;
	}

	public double getTonKMPerYearMainRun() {
		return tonKMPerYearMainRun;
	}

	public double getTonKMPerYearPostRun() {
		return tonKMPerYearPostRun;
	}

	public static List<TripRelation> readTripRelations(String pathToKettenData) throws IOException {
		List<TripRelation> tripRelations = new ArrayList<>();
		CSVParser parser = CSVParser.parse(URI.create(pathToKettenData).toURL(), StandardCharsets.ISO_8859_1,
			CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader());
		for (CSVRecord record : parser) {
			Builder builder = new Builder();
			// Read locations
			builder.originalCell(record.get(column_originCell)).originalCellMainRun(record.get(column_originCell_MainRun)).
				destinationCellMainRun(record.get(column_destinationCell_MainRun)).destinationCell(record.get(column_destinationCell));
			// Read trips
			builder.modePreRun(record.get(column_mode_PreRun)).modeMainRun(record.get(column_mode_MainRun)).modePostRun(record.get(
				column_mode_PostRun));

			// Read goods type and tons
			builder.goodsTypePreRun(record.get(column_goodsType_PreRun)).goodsTypeMainRun(record.get(column_goodsType_MainRun)).goodsTypePostRun(
				record.get(column_goodsType_PostRun));
			builder.tonsPerYearPreRun(Double.parseDouble(record.get(column_tones_PreRun))).tonsPerYearMainRun(
				Double.parseDouble(record.get(column_tones_MainRun))).tonsPerYearPostRun(Double.parseDouble(record.get(
				column_tones_PostRun)));
			builder.tonKMPerYearPreRun(Double.parseDouble(record.get(column_tonKM_PreRun))).tonKMPerYearMainRun(
				Double.parseDouble(record.get(column_tonKM_MainRun))).tonKMPerYearPostRun(Double.parseDouble(record.get(
				column_tonKM_PostRun)));
			//TODO we have not read the data "Verkehrsart" (konventioneller Verkehr or KV/Container/RoLa). Should we do this?

			// Build trip relation and add to list
			tripRelations.add(builder.build());
		}

		return tripRelations;
	}

	public static TripRelation readTripRelation(CSVRecord record) {
		Builder builder = new Builder();
		// Read locations
		builder.originalCell(record.get(column_originCell)).originalCellMainRun(record.get(column_originCell_MainRun)).
			destinationCellMainRun(record.get(column_destinationCell_MainRun)).destinationCell(record.get(column_destinationCell));
		// Read trips
		builder.modePreRun(record.get(column_mode_PreRun)).modeMainRun(record.get(column_mode_MainRun)).modePostRun(record.get(column_mode_PostRun));

		// Read goods type and tons
		builder.goodsTypeMainRun(record.get(column_goodsType_MainRun)).tonsPerYearMainRun(Double.parseDouble(record.get(column_tones_MainRun)));

		// Build trip relation and add to list
		return builder.build();
	}

}
