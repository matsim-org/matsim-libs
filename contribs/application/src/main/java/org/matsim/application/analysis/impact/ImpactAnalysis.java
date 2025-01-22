package org.matsim.application.analysis.impact;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.CommandSpec;
import org.matsim.application.Dependency;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.emissions.AirPollutionAnalysis;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// --modes

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

	private static final Logger log = LogManager.getLogger(ImpactAnalysis.class);
	private static final Integer DAYS_PER_YEAR_PKW = 334; // Also used for non-pkw.
	private static final Integer DAYS_PER_YEAR_LKW = 302;
	private static final Integer ONE_MILLION = 1000000;
	private static final Integer METERS_PER_KILOMETER = 1000;
	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(ImpactAnalysis.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(ImpactAnalysis.class);
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
			return String.format("%.2f", value / 1000000);
		} else if (value >= 1000) {
			return String.format("%.2f", value / 1000);
		} else {
			return String.format("%.2f", value);
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
			.sample(false)
			.separator(CsvOptions.detectDelimiter(input.getPath("legs.csv"))).build());

		StringColumn departureTime = legs.stringColumn("dep_time");
		StringColumn modes = legs.stringColumn("network_mode")
			.set(legs.stringColumn("network_mode").isMissing(),
				legs.stringColumn("mode"));

		IntColumn traveledDistance = legs.intColumn("distance");

		StringColumn travelTime = legs.stringColumn("trav_time");
		TimeColumn waitingTime = legs.timeColumn("wait_time");

		Map<String, Integer> modeCounts = new HashMap<>();
		Map<String, Double> vehicleOperatingTimeByDistance = new HashMap<>();
		Map<String, Double> travelTimeMap = new HashMap<>();
		Map<String, Double> travelAndWaitingTimeMap = new HashMap<>();
		Map<String, Double> traveledDistanceMap = new HashMap<>();

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
					modeCounts.put(mode, modeCounts.getOrDefault(mode, 0) + 1);

					// travel time
					String travelTimeStr = travelTime.get(i);
					LocalTime localTime = LocalTime.parse(time);
					Duration duration = Duration.between(LocalTime.MIDNIGHT, localTime);
					double travelTimeConverted = duration.toSeconds() / 3600.0;

					// mode count by distance
					String distanceMode = distance >= 50000 ? mode + "_more_than_fifty" : mode + "_less_than_fifty";
					vehicleOperatingTimeByDistance.put(distanceMode, vehicleOperatingTimeByDistance.getOrDefault(distanceMode, 0.) + travelTimeConverted);


					String waitingTimeStr = String.valueOf(waitingTime.get(i));

					LocalTime travelTimeLocal = LocalTime.parse(travelTimeStr);
					LocalTime waitingTimeLocal = LocalTime.parse(waitingTimeStr);

					travelTimeMap.put(mode, travelTimeMap.getOrDefault(mode, 0.) + travelTimeLocal.getHour() + travelTimeLocal.getMinute() / 60.0);
					travelAndWaitingTimeMap.put(mode, travelAndWaitingTimeMap.getOrDefault(mode, 0.) + travelTimeLocal.getHour() + travelTimeLocal.getMinute() / 60.0 + waitingTimeLocal.getHour() + waitingTimeLocal.getMinute() / 60.0);

					traveledDistanceMap.put(mode, traveledDistanceMap.getOrDefault(mode, 0.) + distance);
				}
			} catch (Exception ignored) {
			}
		}

		travelTimeMap.replaceAll((k, v) -> {
			if ("freight".equals(k)) {
				return v * DAYS_PER_YEAR_LKW / ONE_MILLION;
			} else {
				return v * DAYS_PER_YEAR_PKW / ONE_MILLION;
			}
		});

		travelAndWaitingTimeMap.replaceAll((k, v) -> {
			if ("freight".equals(k)) {
				return v * DAYS_PER_YEAR_LKW / ONE_MILLION;
			} else {
				return v * DAYS_PER_YEAR_PKW / ONE_MILLION;
			}
		});

		traveledDistanceMap.replaceAll((k, v) -> {
			if ("freight".equals(k)) {
				return v * DAYS_PER_YEAR_LKW / (ONE_MILLION * METERS_PER_KILOMETER);
			} else {
				return v * DAYS_PER_YEAR_PKW / (ONE_MILLION * METERS_PER_KILOMETER);
			}
		});

		vehicleOperatingTimeByDistance.replaceAll((k, v) -> {
			if ("freight".equals(k)) {
				return v * DAYS_PER_YEAR_LKW / (ONE_MILLION);
			} else {
				return v * DAYS_PER_YEAR_PKW / (ONE_MILLION);
			}
		});

		HashMap<String, Mode> modeMap = new HashMap<>();

		modeCounts.forEach((mode, count) -> {
			Mode m = new Mode();
			m.setCount(count);
			modeMap.put(mode, m);
		});

		vehicleOperatingTimeByDistance.forEach((mode, count) -> {

			if (!modeMap.containsKey(mode.split("_")[0])) {
				Mode m = new Mode();
				modeMap.put(mode.split("_")[0], m);
			}

			Mode m = modeMap.get(mode.split("_")[0]);
			if (mode.contains("less_than_fifty")) {
				m.setVehicleOperatingTimeByDistanceLessThanFifty(count);
			} else if (mode.contains("more_than_fifty")) {
				m.setVehicleOperatingTimeByDistanceLessMoreFifty(count);
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

		travelAndWaitingTimeMap.forEach((mode, time) -> {

			if (!modeMap.containsKey(mode)) {
				Mode m = new Mode();
				modeMap.put(mode, m);
			}

			Mode m = modeMap.get(mode);
			m.setTravelAndWaitingTime(time);
		});

		traveledDistanceMap.forEach((mode, distance) -> {

			if (!modeMap.containsKey(mode)) {
				Mode m = new Mode();
				modeMap.put(mode, m);
			}

			Mode m = modeMap.get(mode);
			m.setTraveledDistance(distance);
		});

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

			if (m.getEmissons() == null) {
				m.setEmissons(new Emissons());
			}

			switch (pollutant) {
				case "NOx" -> m.getEmissons().setNOx(value);
				case "CO2_TOTAL" -> m.getEmissons().setCO2_TOTAL(value);
				case "CO" -> m.getEmissons().setCO(value);
				case "HC" -> m.getEmissons().setHC(value);
				case "PM" -> m.getEmissons().setPM(value);
				case "SO2" -> m.getEmissons().setSO2(value);
			}
		}

		modeMap.forEach((mode, m) -> {
			int countTmp = m.getCount();
			double countByDistanceLessThanFiftyTmp = m.getVehicleOperatingTimeByDistanceLessThanFifty();
			double countByDistanceMoreThanFiftyTmp = m.getVehicleOperatingTimeByDistanceLessMoreFifty();
			double travelTimeTmp = m.getTravelTime();
			double traveledDistanceTmp = m.getTraveledDistance();
			double travelAndWaitingTimeTmp = m.getTravelAndWaitingTime();
			double NOx = m.getEmissons().getNOx();
			double CO2_TOTAL = m.getEmissons().getCO2_TOTAL();
			double CO = m.getEmissons().getCO();
			double HC = m.getEmissons().getHC();
			double PM = m.getEmissons().getPM();
			double SO2 = m.getEmissons().getSO2();

			// Other Data
			ArrayList<String> generalValues = new ArrayList<>();
			ArrayList<String> generalUnits = new ArrayList<>();
			ArrayList<String> generalDescriptions = new ArrayList<>();

			generalValues.add(String.valueOf(countTmp));
			generalValues.add(String.format("%.2f", countByDistanceLessThanFiftyTmp));
			generalValues.add(String.format("%.2f", countByDistanceMoreThanFiftyTmp));
			generalValues.add(String.format("%.2f", travelTimeTmp));
			generalValues.add(String.format("%.2f", traveledDistanceTmp));
			// generalValues.add(String.format("%.2f", travelAndWaitingTimeTmp));

			generalUnits.add("Vehicle / Day");
			generalUnits.add("Mio. Hours / Year");
			generalUnits.add("Mio. Hours / Year");
			generalUnits.add("Mio. Hours / Year");
			generalUnits.add("Mio. Kilometers / Year");
			// generalUnits.add("Mio. Hours / Year");

			generalDescriptions.add("Average Vehicle Loads");
			generalDescriptions.add("Vehicle Operating Times (≤ 50 km)");
			generalDescriptions.add("Vehicle Operating Times (> 50 km)");
			generalDescriptions.add("Vehicle Operating Times");
			generalDescriptions.add("Travel Distance");
			// generalDescriptions.add("Travel And Waiting Time");

			// Emission Data
			ArrayList<String> emissionsValues = new ArrayList<>();
			ArrayList<String> emissionsUnits = new ArrayList<>();
			ArrayList<String> emissionsDescriptions = new ArrayList<>();

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

			Table generalTable = Table.create("General Data")
				.addColumns(
					StringColumn.create("Description", generalDescriptions),
					StringColumn.create("Value", generalValues),
					StringColumn.create("Unit", generalUnits)
				);

			Table emissionsTable = Table.create("Emissions Data")
				.addColumns(
					StringColumn.create("Description", emissionsDescriptions),
					StringColumn.create("Value", emissionsValues),
					StringColumn.create("Unit", emissionsUnits)
				);

			generalTable.write().csv(output.getPath("general_%s.csv", mode).toFile());
			emissionsTable.write().csv(output.getPath("emissions_%s.csv", mode).toFile());
		});


		return 0;
	}

	private static class Mode {
		private int count; // count
		private double vehicleOperatingTimeByDistanceLessThanFifty; // count
		private double vehicleOperatingTimeByDistanceLessMoreFifty; // count
		private double travelTime; // mio. hours / year
		private double traveledDistance; // mio. meters / year
		private double travelAndWaitingTime; // mio. hours / year
		private Emissons emissons;

		public Emissons getEmissons() {
			return emissons;
		}

		public void setEmissons(Emissons emissons) {
			this.emissons = emissons;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public double getVehicleOperatingTimeByDistanceLessThanFifty() {
			return vehicleOperatingTimeByDistanceLessThanFifty;
		}

		public void setVehicleOperatingTimeByDistanceLessThanFifty(double vehicleOperatingTimeByDistanceLessThanFifty) {
			this.vehicleOperatingTimeByDistanceLessThanFifty = vehicleOperatingTimeByDistanceLessThanFifty;
		}

		public double getVehicleOperatingTimeByDistanceLessMoreFifty() {
			return vehicleOperatingTimeByDistanceLessMoreFifty;
		}

		public void setVehicleOperatingTimeByDistanceLessMoreFifty(double vehicleOperatingTimeByDistanceLessMoreFifty) {
			this.vehicleOperatingTimeByDistanceLessMoreFifty = vehicleOperatingTimeByDistanceLessMoreFifty;
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

		public double getTravelAndWaitingTime() {
			return travelAndWaitingTime;
		}

		public void setTravelAndWaitingTime(double travelAndWaitingTime) {
			this.travelAndWaitingTime = travelAndWaitingTime;
		}

		@Override
		public String toString() {
			return "Mode{" +
				"count=" + count +
				", countByDistanceLessThanFifty=" + vehicleOperatingTimeByDistanceLessThanFifty +
				", countByDistanceMoreThanFifty=" + vehicleOperatingTimeByDistanceLessMoreFifty +
				", travelTime=" + travelTime +
				", traveledDistance=" + traveledDistance +
				", travelAndWaitingTime=" + travelAndWaitingTime +
				", emissons=" + emissons +
				'}';
		}
	}

	private class Emissons {
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
			return "Emissons{" +
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
