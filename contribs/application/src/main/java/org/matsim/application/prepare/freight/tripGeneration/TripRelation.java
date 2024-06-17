package org.matsim.application.prepare.freight.tripGeneration;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The object TripRelation corresponds to each entry (row) in the ketten-2010 table. The information will be read
 * from the table and stored as the TripRelation. The data can then be used by different tools to generate MATSim
 * plans file.
 */
public class TripRelation {

	private static final Logger log = LogManager.getLogger(TripRelation.class);

	public static final String column_originCell = "Quellzelle";
	public static final String column_originCell_MainRun = "QuellzelleHL";
	public static final String column_destinationCell_MainRun = "ZielzelleHL";
	public static final String column_destinationCell = "Zielzelle";
	public static final String column_originTerminal = "Quellterminal";
	public static final String column_destinationTerminal = "Zielterminal";
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
	private final String originCell;
	/**
	 * Start location of the main run; Also the destination of the pre-run (when applicable)
	 */
	private final String originCellMainRun;
	/**
	 * Destination of the main run; Also the starting location of the post-run (when applicable)
	 */
	private final String destinationCellMainRun;
	/**
	 * Destination of the full trip relation
	 */
	private final String destinationCell;
	/**
	 * Start location of the main run, if it is combined traffic; Also the destination of the pre-run (when applicable)
	 */
	private final String originTerminal;
	/**
	 * Destination of the main run, if it is combined traffic; Also the starting location of the post-run (when applicable)
	 */
	private final String destinationTerminal;
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
	private double tonsPerYearPreRun;
	private double tonsPerYearMainRun;
	private double tonsPerYearPostRun;
	private double tonKMPerYearPreRun;
	private double tonKMPerYearMainRun;
	private double tonKMPerYearPostRun;

	public static class Builder {
		private String originCell;
		private String originCellMainRun;
		private String destinationCellMainRun;
		private String destinationCell;

		private String originTerminal;
		private String destinationTerminal;

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
			this.originCell = value;
			return this;
		}

		public Builder originCellMainRun(String value) {
			this.originCellMainRun = value;
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

		public void originTerminal(String value) {
			this.originTerminal = value;
		}

		public void destinationTerminal(String value) {
			this.destinationTerminal = value;
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
		this.originCell = builder.originCell;
		this.originCellMainRun = builder.originCellMainRun;
		this.destinationCellMainRun = builder.destinationCellMainRun;
		this.destinationCell = builder.destinationCell;

		this.originTerminal = builder.originTerminal;
		this.destinationTerminal = builder.destinationTerminal;

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

	public String getOriginCell() {
		return originCell;
	}

	public String getOriginCellMainRun() {
		return originCellMainRun;
	}

	public String getDestinationCellMainRun() {
		return destinationCellMainRun;
	}

	public String getDestinationCell() {
		return destinationCell;
	}

	public String getOriginTerminal() {
		return originTerminal;
	}

	public String getDestinationTerminal() {
		return destinationTerminal;
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

	/**
	 * Reads a CSV file and turns every entry into a {@link TripRelation}
	 * @param pathToKettenData URI-path to ketten.csv file (for local files start with "file:/")
	 * @return A List of {@link TripRelation}
	 * @throws IOException
	 */
	public static List<TripRelation> readTripRelations(String pathToKettenData) throws IOException {
		List<TripRelation> tripRelations = new ArrayList<>();
		CSVParser parser = CSVParser.parse(URI.create(pathToKettenData).toURL(), StandardCharsets.ISO_8859_1,
			CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter(';').setHeader().setSkipHeaderRecord(true).build());
		for (CSVRecord record : parser) {
			Builder builder = new Builder();

			// Read locations
			builder.originalCell(record.get(column_originCell)).originCellMainRun(record.get(column_originCell_MainRun)).
				destinationCellMainRun(record.get(column_destinationCell_MainRun)).destinationCell(record.get(column_destinationCell));

			// Read Terminals
			builder.originTerminal(record.get(column_originTerminal));
			builder.destinationTerminal(record.get(column_destinationTerminal));

			// Read trips
			builder.modePreRun(record.get(column_mode_PreRun)).modeMainRun(record.get(column_mode_MainRun)).modePostRun(record.get(
				column_mode_PostRun));

			// Read the goods type and tons
			builder.goodsTypePreRun(record.get(column_goodsType_PreRun)).goodsTypeMainRun(record.get(column_goodsType_MainRun)).goodsTypePostRun(
				record.get(column_goodsType_PostRun));
			builder.tonsPerYearPreRun(Double.parseDouble(record.get(column_tones_PreRun))).tonsPerYearMainRun(
				Double.parseDouble(record.get(column_tones_MainRun))).tonsPerYearPostRun(Double.parseDouble(record.get(
				column_tones_PostRun)));
			builder.tonKMPerYearPreRun(Double.parseDouble(record.get(column_tonKM_PreRun))).tonKMPerYearMainRun(
				Double.parseDouble(record.get(column_tonKM_MainRun))).tonKMPerYearPostRun(Double.parseDouble(record.get(
				column_tonKM_PostRun)));
			//TODO we have not read the data "Verkehrsart" (konventioneller Verkehr or KV/Container/RoLa). Should we do this?
			assert (Objects.equals(builder.goodsTypePreRun, builder.goodsTypeMainRun) && Objects.equals(builder.goodsTypeMainRun,
                    builder.goodsTypePostRun));
			// Build trip relation and add to list
			tripRelations.add(builder.build());
		}

		return tripRelations;
	}

	/**
	 * Combines trip relations with the same characteristics (mode, O/D cells, goodsType) to one trip relation with the sum of the tons values
	 *
	 * @param tripRelations List of trip relations
	 */
	public static void combineSimilarEntries(List<TripRelation> tripRelations) {
		double sumTonsMainRun = tripRelations.stream().mapToDouble(TripRelation::getTonsPerYearMainRun).sum();
		int numberOfEntries = tripRelations.size();
		int numberCombined = 0;

		List<String> checkedOriginCells = new ArrayList<>();
		for (int i = 0; i < tripRelations.size(); i++) {

			if (i % 100000 == 0) {
				log.info("Processing: " + i + " out of " + tripRelations.size() + " entries have been processed");
			}
			TripRelation tripRelation = tripRelations.get(i);
			if (checkedOriginCells.contains(tripRelation.getOriginCell()))
				continue;
			List<TripRelation> tripRelationsFromThisOrigin = new ArrayList<>(tripRelations.stream().filter(
                    singeTripRelation -> singeTripRelation.getOriginCell().equals(tripRelation.getOriginCell())).toList());
			log.info("Processing " + tripRelationsFromThisOrigin.size() + " entries from origin cell " + tripRelation.getOriginCell());
			for (int j = 0; j < tripRelationsFromThisOrigin.size(); j++) {
				TripRelation oneTripRelationFromOrigin = tripRelationsFromThisOrigin.get(j);
				for (int k = j + 1; k < tripRelationsFromThisOrigin.size(); k++) {
					TripRelation tripRelationOfOriginToCompare = tripRelationsFromThisOrigin.get(k);
					if (oneTripRelationFromOrigin.isSimilarTo(tripRelationOfOriginToCompare)) {
						oneTripRelationFromOrigin.combineTonsValuesFotBothRelations(tripRelationOfOriginToCompare);
						tripRelations.remove(tripRelationOfOriginToCompare);
						tripRelationsFromThisOrigin.remove(tripRelationOfOriginToCompare);
						numberCombined++;
						k--;
					}
				}
			}
			checkedOriginCells.add(tripRelationsFromThisOrigin.get(0).getOriginCell());
		}
		double sumTonsMainRunAfterCombining = tripRelations.stream().mapToDouble(TripRelation::getTonsPerYearMainRun).sum();
		assert (sumTonsMainRun == sumTonsMainRunAfterCombining);
		int numberOfEntriesAfterCombining = tripRelations.size();
		log.info("Combined " + numberCombined + " entries");
		log.info("Number of entries before combining: " + numberOfEntries + " and after combining: " + numberOfEntriesAfterCombining);
		log.info("Sum of tons before combining: " + sumTonsMainRun + " and after combining: " + sumTonsMainRunAfterCombining);
	}

	/**
	 * Reads a {@link CSVRecord} object and turns it into a {@link TripRelation} object. <br>
	 * <b>Warning:</b> Only sets the following attributes: {@link TripRelation#column_originCell}, {@link TripRelation#column_originCell_MainRun},
	 * {@link TripRelation#column_destinationCell_MainRun}, {@link TripRelation#column_destinationCell},  {@link TripRelation#column_mode_PreRun},
	 * {@link TripRelation#column_mode_MainRun},  {@link TripRelation#column_mode_PostRun},  {@link TripRelation#column_goodsType_MainRun},  {@link TripRelation#column_tones_MainRun} <br>
	 * All other attributes are set to {@code null} or {@code 0}!
	 * @param record {@link CSVRecord} object containing the entry of the ketten.csv
	 * @return A List of {@link TripRelation}
	 * @throws IOException
	 */
	public static TripRelation readTripRelation(CSVRecord record) {
		Builder builder = new Builder();
		// Read locations
		builder.originalCell(record.get(column_originCell)).originCellMainRun(record.get(column_originCell_MainRun)).
			destinationCellMainRun(record.get(column_destinationCell_MainRun)).destinationCell(record.get(column_destinationCell));
		// Read trips
		builder.modePreRun(record.get(column_mode_PreRun)).modeMainRun(record.get(column_mode_MainRun)).modePostRun(record.get(column_mode_PostRun));

		// Read goods type and tons
		builder.goodsTypeMainRun(record.get(column_goodsType_MainRun)).tonsPerYearMainRun(Double.parseDouble(record.get(column_tones_MainRun)));

		// Build trip relation and add to list
		return builder.build();
	}


	/**
	 * Combines the tons values of both trip relations
	 *
	 * @param tripRelationOfOriginToCompare
	 */
	private void combineTonsValuesFotBothRelations(TripRelation tripRelationOfOriginToCompare) {
		this.tonsPerYearPreRun = this.tonsPerYearPreRun + tripRelationOfOriginToCompare.tonsPerYearPreRun;
		this.tonsPerYearMainRun = this.tonsPerYearMainRun + tripRelationOfOriginToCompare.tonsPerYearMainRun;
		this.tonsPerYearPostRun = this.tonsPerYearPostRun + tripRelationOfOriginToCompare.tonsPerYearPostRun;
		this.tonKMPerYearPreRun = this.tonKMPerYearPreRun + tripRelationOfOriginToCompare.tonKMPerYearPreRun;
		this.tonKMPerYearMainRun = this.tonKMPerYearMainRun + tripRelationOfOriginToCompare.tonKMPerYearMainRun;
		this.tonKMPerYearPostRun = this.tonKMPerYearPostRun + tripRelationOfOriginToCompare.tonKMPerYearPostRun;
	}

	/**
	 * Checks if the trip relations have the same characteristics (mode, O/D cells/terminals, goodsType) and only differ in the tons values
	 *
	 * @param tripRelationOfOriginToCompare
	 * @return
	 */
	private boolean isSimilarTo(TripRelation tripRelationOfOriginToCompare) {
		return
			this.destinationCell.equals(tripRelationOfOriginToCompare.destinationCell)
			&& this.destinationCellMainRun.equals(tripRelationOfOriginToCompare.destinationCellMainRun)
			&& this.originCell.equals(tripRelationOfOriginToCompare.originCell)
			&& this.originCellMainRun.equals(tripRelationOfOriginToCompare.originCellMainRun)
			&& this.originTerminal.equals(tripRelationOfOriginToCompare.originTerminal)
			&& this.destinationTerminal.equals(tripRelationOfOriginToCompare.destinationTerminal)
			&& this.modePreRun.equals(tripRelationOfOriginToCompare.modePreRun)
			&& this.modeMainRun.equals(tripRelationOfOriginToCompare.modeMainRun)
			&& this.modePostRun.equals(tripRelationOfOriginToCompare.modePostRun)
			&& this.goodsTypeMainRun.equals(tripRelationOfOriginToCompare.goodsTypeMainRun);
	}
}
