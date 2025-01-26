package org.matsim.application.analysis.impact;

import it.unimi.dsi.fastutil.objects.Object2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.CommandSpec;
import org.matsim.application.Dependency;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.emissions.AirPollutionAnalysis;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.SampleOptions;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;


@CommandLine.Command(
	name = "impact"
)
@CommandSpec(requireRunDirectory = true,
	requires = {"legs.csv", "vehicles.xml", "trips.csv"},
	dependsOn = {
		@Dependency(value = AirPollutionAnalysis.class, files = "emissions_per_network_mode.csv")
	},
	produces = {"general_%s.csv", "emissions_%s.csv"}
)
public class ImpactAnalysis implements MATSimAppCommand {

	/**
	 * Locale used for formatting numbers.
	 */
	private static final Locale L = Locale.US;

	private static final Integer DAYS_PER_YEAR_PKW = 334; // Also used for non-pkw.
	private static final Integer DAYS_PER_YEAR_LKW = 302;
	private static final Integer ONE_MILLION = 1000000;
	private static final Integer METERS_PER_KILOMETER = 1000;

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(ImpactAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(ImpactAnalysis.class);

	@CommandLine.Mixin
	private SampleOptions sample;

	@CommandLine.Option(names = {"--modes"}, description = "Mode(s) to analyze", split = ",")
	private Set<String> modeArgs;

	/**
	 * Formats the value to a string with 2 decimal places and divides it by 1000000 if it is greater than 1000000 for gram to ton conversion
	 * or divides it by 1000 if it is greater than 1000 for kilogram to ton conversion.
	 *
	 * @param value emission value
	 * @return formatted emission value
	 */
	private static String formatValue(double value) {
		if (value >= 1000000) {
			return String.format(L, "%.2f", value / 1000000);
		} else if (value >= 1000) {
			return String.format(L, "%.2f", value / 1000);
		} else {
			return String.format(L, "%.2f", value);
		}
	}

	/**
	 * Formats the unit to kg / Day or t / Day if the value is greater than 1000 or 1000000, otherwise g / Day.
	 *
	 * @param value emission value
	 * @return formatted emission unit
	 */
	private static String formatUnit(double value) {
		if (value >= 1000000) {
			return "t / Day";
		} else if (value >= 1000) {
			return "kg / Day";
		} else {
			return "g / Day";
		}
	}

	@Override
	public Integer call() throws Exception {

		Table legs = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(input.getPath("legs.csv")))
			.columnTypesPartial(Map.of("dep_time", ColumnType.STRING, "trav_time", ColumnType.STRING))
			.sample(false)
			.separator(CsvOptions.detectDelimiter(input.getPath("legs.csv"))).build());

		StringColumn departureTime = legs.stringColumn("dep_time");

		// Use network mode, or the trip mode if missing
		StringColumn modes = legs.stringColumn("network_mode")
			.set(legs.stringColumn("network_mode").isMissing(),
				legs.stringColumn("mode"));

		IntColumn traveledDistance = legs.intColumn("distance");

		StringColumn travelTime = legs.stringColumn("trav_time");

		Object2DoubleMap<String> modeCounts = new Object2DoubleLinkedOpenHashMap<>();
		Object2DoubleMap<String> vehicleOperatingTimeByDistance = new Object2DoubleLinkedOpenHashMap<>();
		Object2DoubleMap<String> travelTimeMap =  new Object2DoubleLinkedOpenHashMap<>();
		Object2DoubleMap<String>traveledDistanceMap =  new Object2DoubleLinkedOpenHashMap<>();

		for (int i = 0; i < legs.rowCount(); i++) {
			String time = departureTime.get(i);
			Integer distance = traveledDistance.get(i);
			try {
				LocalTime depTime = LocalTime.parse(time);
				if (depTime.isBefore(LocalTime.of(23, 59, 59))) {
					String mode = modes.get(i);

					if (modeArgs != null && !modeArgs.contains(mode)) {
						continue;
					}

					// mode count by network_mode > mode
					modeCounts.mergeDouble(mode, sample.getUpscaleFactor(), Double::sum);

					// travel time
					String travelTimeStr = travelTime.get(i);
					LocalTime localTime = LocalTime.parse(travelTimeStr);
					Duration duration = Duration.between(LocalTime.MIDNIGHT, localTime);
					double travelTimeConverted = sample.getUpscaleFactor() * (duration.toSeconds() / 3600.0);

					// mode count by distance
					String distanceMode = distance >= 50000 ? mode + "_more_than_fifty" : mode + "_less_than_fifty";
					vehicleOperatingTimeByDistance.mergeDouble(distanceMode, travelTimeConverted, Double::sum);

					travelTimeMap.merge(mode, travelTimeConverted, Double::sum);
					traveledDistanceMap.merge(mode, sample.getUpscaleFactor() * distance, Double::sum);
				}
			} catch (Exception ignored) {
			}
		}

		travelTimeMap.replaceAll((k, v) -> {
			if ("freight".equals(k) || TransportMode.truck.equals(k)) {
				return v * DAYS_PER_YEAR_LKW / ONE_MILLION;
			} else {
				return v * DAYS_PER_YEAR_PKW / ONE_MILLION;
			}
		});

		traveledDistanceMap.replaceAll((k, v) -> {
			if ("freight".equals(k) || TransportMode.truck.equals(k)) {
				return v * DAYS_PER_YEAR_LKW / (ONE_MILLION * METERS_PER_KILOMETER);
			} else {
				return v * DAYS_PER_YEAR_PKW / (ONE_MILLION * METERS_PER_KILOMETER);
			}
		});

		vehicleOperatingTimeByDistance.replaceAll((k, v) -> {
			if ("freight".equals(k) || TransportMode.truck.equals(k)) {
				return v * DAYS_PER_YEAR_LKW / (ONE_MILLION);
			} else {
				return v * DAYS_PER_YEAR_PKW / (ONE_MILLION);
			}
		});

		Map<String, Mode> modeMap = new HashMap<>();

		modeCounts.forEach((mode, count) -> {
			Mode m = new Mode();
			m.setCount(count);
			modeMap.put(mode, m);
		});

		vehicleOperatingTimeByDistance.forEach((mode, time) -> {

			if (!modeMap.containsKey(mode.split("_")[0])) {
				Mode m = new Mode();
				modeMap.put(mode.split("_")[0], m);
			}

			Mode m = modeMap.get(mode.split("_")[0]);
			if (mode.contains("less_than_fifty")) {
				m.setVehicleOperatingTimeByDistanceLessThanFifty(time);
			} else if (mode.contains("more_than_fifty")) {
				m.setVehicleOperatingTimeByDistanceMoreThanFifty(time);
			}
		});

		travelTimeMap.forEach((mode, time) -> {
			if (!modeMap.containsKey(mode)) {
				Mode m = new Mode();
				modeMap.put(mode, m);
			}
			Mode m = modeMap.get(mode);
			m.setTravelTime(time);
		});

		traveledDistanceMap.forEach((mode, distance) -> {

			if (!modeMap.containsKey(mode)) {
				Mode m = new Mode();
				modeMap.put(mode, m);
			}

			Mode m = modeMap.get(mode);
			m.setTraveledDistance(distance);
		});

		modeMap.forEach((mode, m) -> {
			double countTmp = m.getCount();
			double countByDistanceLessThanFiftyTmp = m.getVehicleOperatingTimeByDistanceLessThanFifty();
			double countByDistanceMoreThanFiftyTmp = m.getVehicleOperatingTimeByDistanceMoreThanFifty();
			double travelTimeTmp = m.getTravelTime();
			double traveledDistanceTmp = m.getTraveledDistance();

			// Other Data
			List<String> generalValues = new ArrayList<>();
			List<String> generalUnits = new ArrayList<>();
			ArrayList<String> generalDescriptions = new ArrayList<>();

			generalValues.add(String.valueOf(countTmp));
			generalValues.add(String.format(L, "%.2f", countByDistanceLessThanFiftyTmp));
			generalValues.add(String.format(L, "%.2f", countByDistanceMoreThanFiftyTmp));
			generalValues.add(String.format(L, "%.2f", travelTimeTmp));
			generalValues.add(String.format(L, "%.2f", traveledDistanceTmp));

			generalUnits.add("Vehicle / Day");
			generalUnits.add("Mio. Hours / Year");
			generalUnits.add("Mio. Hours / Year");
			generalUnits.add("Mio. Hours / Year");
			generalUnits.add("Mio. Kilometers / Year");

			generalDescriptions.add("Vehicle Volume");
			generalDescriptions.add("Vehicle Operating Times (≤ 50 km)");
			generalDescriptions.add("Vehicle Operating Times (> 50 km)");
			generalDescriptions.add("Vehicle Operating Times");
			generalDescriptions.add("Travel Distance");


			Table generalTable = Table.create("General Data")
				.addColumns(
					StringColumn.create("Description", generalDescriptions),
					StringColumn.create("Value", generalValues),
					StringColumn.create("Unit", generalUnits)
				);

			generalTable.write().csv(output.getPath("general_%s.csv", mode).toFile());
		});

		writeEmissionsInfo(modeMap);

		return 0;
	}

	private void writeEmissionsInfo(Map<String, Mode> modeMap) {

		String emissionPath = input.getPath(AirPollutionAnalysis.class, "emissions_per_network_mode.csv");

		Table emissions = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(emissionPath))
			.sample(false));

		StringColumn vehicleTypeColumn = emissions.stringColumn("vehicleType");
		StringColumn pollutantColumn = emissions.stringColumn("pollutant");
		DoubleColumn valueColumn = emissions.doubleColumn("value");

		for (int i = 0; i < emissions.rowCount(); i++) {

			String vehicleType = vehicleTypeColumn.get(i);

			String pollutant = pollutantColumn.get(i);
			double value = valueColumn.get(i);

			if (!modeMap.containsKey(vehicleType)) {
				Mode m = new Mode();
				modeMap.put(vehicleType, m);
			}

			Mode m = modeMap.get(vehicleType);

			if (m.getEmissions() == null) {
				m.setEmissions(new Emissions());
			}

			switch (pollutant) {
				case "NOx" -> m.getEmissions().setNOx(value);
				case "CO2_TOTAL" -> m.getEmissions().setCO2_TOTAL(value);
				case "CO" -> m.getEmissions().setCO(value);
				case "HC" -> m.getEmissions().setHC(value);
				case "PM" -> m.getEmissions().setPM(value);
				case "SO2" -> m.getEmissions().setSO2(value);
			}
		}

		modeMap.forEach((mode, m) -> {
			double NOx = m.getEmissions().getNOx();
			double CO2_TOTAL = m.getEmissions().getCO2_TOTAL();
			double CO = m.getEmissions().getCO();
			double HC = m.getEmissions().getHC();
			double PM = m.getEmissions().getPM();
			double SO2 = m.getEmissions().getSO2();

			// Emission Data
			List<String> emissionsValues = new ArrayList<>();
			List<String> emissionsUnits = new ArrayList<>();
			List<String> emissionsDescriptions = new ArrayList<>();

			emissionsValues.add(formatValue(NOx));
			emissionsValues.add(formatValue(CO2_TOTAL));
			emissionsValues.add(formatValue(CO));
			emissionsValues.add(formatValue(HC));
			emissionsValues.add(formatValue(PM));
			emissionsValues.add(formatValue(SO2));

			emissionsUnits.add(formatUnit(NOx));
			emissionsUnits.add(formatUnit(CO2_TOTAL));
			emissionsUnits.add(formatUnit(CO));
			emissionsUnits.add(formatUnit(HC));
			emissionsUnits.add(formatUnit(PM));
			emissionsUnits.add(formatUnit(SO2));

			emissionsDescriptions.add("Nitrogen Oxides (NOₓ)");
			emissionsDescriptions.add("Total Carbon Dioxide (CO₂)");
			emissionsDescriptions.add("Carbon Monoxide (CO)");
			emissionsDescriptions.add("Hydrocarbons (HC)");
			emissionsDescriptions.add("Particulate Matter (PM)");
			emissionsDescriptions.add("Sulfur Dioxide (SO₂)");


			Table emissionsTable = Table.create("Emissions Data")
				.addColumns(
					StringColumn.create("Description", emissionsDescriptions),
					StringColumn.create("Value", emissionsValues),
					StringColumn.create("Unit", emissionsUnits)
				);

			emissionsTable.write().csv(output.getPath("emissions_%s.csv", mode).toFile());
		});
	}

	private static class Mode {
		private double count; // count
		private double vehicleOperatingTimeByDistanceLessThanFifty; // count
		private double vehicleOperatingTimeByDistanceMoreThanFifty; // count
		private double travelTime; // mio. hours / year
		private double traveledDistance; // mio. meters / year
		private Emissions emissions;

		public Emissions getEmissions() {
			return emissions;
		}

		public void setEmissions(Emissions emissions) {
			this.emissions = emissions;
		}

		public double getCount() {
			return count;
		}

		public void setCount(double count) {
			this.count = count;
		}

		public double getVehicleOperatingTimeByDistanceLessThanFifty() {
			return vehicleOperatingTimeByDistanceLessThanFifty;
		}

		public void setVehicleOperatingTimeByDistanceLessThanFifty(double vehicleOperatingTimeByDistanceLessThanFifty) {
			this.vehicleOperatingTimeByDistanceLessThanFifty = vehicleOperatingTimeByDistanceLessThanFifty;
		}

		public double getVehicleOperatingTimeByDistanceMoreThanFifty() {
			return vehicleOperatingTimeByDistanceMoreThanFifty;
		}

		public void setVehicleOperatingTimeByDistanceMoreThanFifty(double vehicleOperatingTimeByDistanceMoreThanFifty) {
			this.vehicleOperatingTimeByDistanceMoreThanFifty = vehicleOperatingTimeByDistanceMoreThanFifty;
		}

		public double getTravelTime() {
			return travelTime;
		}

		public void setTravelTime(double travelTime) {
			this.travelTime = travelTime;
		}

		public double getTraveledDistance() {
			return traveledDistance;
		}

		public void setTraveledDistance(double traveledDistance) {
			this.traveledDistance = traveledDistance;
		}

		@Override
		public String toString() {
			return "Mode{" +
				"count=" + count +
				", countByDistanceLessThanFifty=" + vehicleOperatingTimeByDistanceLessThanFifty +
				", countByDistanceMoreThanFifty=" + vehicleOperatingTimeByDistanceMoreThanFifty +
				", travelTime=" + travelTime +
				", traveledDistance=" + traveledDistance +
				", emissons=" + emissions +
				'}';
		}
	}

	private final static class Emissions {
		private double NOx;
		private double CO2_TOTAL;
		private double CO;
		private double HC;
		private double PM;
		private double SO2;

		public double getNOx() {
			return NOx;
		}

		public void setNOx(double NOx) {
			this.NOx = NOx;
		}

		public double getCO2_TOTAL() {
			return CO2_TOTAL;
		}

		public void setCO2_TOTAL(double CO2_TOTAL) {
			this.CO2_TOTAL = CO2_TOTAL;
		}

		public double getCO() {
			return CO;
		}

		public void setCO(double CO) {
			this.CO = CO;
		}

		public double getHC() {
			return HC;
		}

		public void setHC(double HC) {
			this.HC = HC;
		}

		public double getPM() {
			return PM;
		}

		public void setPM(double PM) {
			this.PM = PM;
		}

		public double getSO2() {
			return SO2;
		}

		public void setSO2(double SO2) {
			this.SO2 = SO2;
		}

		@Override
		public String toString() {
			return "Emissions{" +
				"NOx=" + NOx +
				", CO2_TOTAL=" + CO2_TOTAL +
				", CO=" + CO +
				", HC=" + HC +
				", PM=" + PM +
				", SO2=" + SO2 +
				'}';
		}
	}
}
