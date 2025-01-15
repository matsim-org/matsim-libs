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

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@CommandLine.Command(
	name = "impact"
)
@CommandSpec(requireRunDirectory = true,
	requires = {"legs.csv", "vehicles.xml", "trips.csv"},
	dependsOn = {
		@Dependency(value = AirPollutionAnalysis.class, files = "emissions_per_network_mode.csv")
	},
	produces = {"total_%s.csv"}
)
public class ImpactAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(ImpactAnalysis.class);

	private static final Integer DAYS_PER_YEAR = 365;
	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(ImpactAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(ImpactAnalysis.class);

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
		Map<String, Integer> modeCountsByDistance = new HashMap<>();
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

					// mode count by network_mode > mode
					modeCounts.put(mode, modeCounts.getOrDefault(mode, 0) + 1);

					// mode count by distance
					String distanceMode = distance >= 50000 ? mode + "_more_than_fifty" : mode + "_less_than_fifty";
					modeCountsByDistance.put(distanceMode, modeCountsByDistance.getOrDefault(distanceMode, 0) + 1);

					// travel time
					String travelTimeStr = travelTime.get(i);
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

		travelTimeMap.replaceAll((k, v) -> v * DAYS_PER_YEAR / 1000000);
		travelAndWaitingTimeMap.replaceAll((k, v) -> v * DAYS_PER_YEAR / 1000000);
		traveledDistanceMap.replaceAll((k, v) -> v * DAYS_PER_YEAR / 1000000);

		HashMap<String, Mode> modeMap = new HashMap<>();

		modeCounts.forEach((mode, count) -> {
			Mode m = new Mode();
			m.setCount(count);
			modeMap.put(mode, m);
		});

		modeCountsByDistance.forEach((mode, count) -> {

			if (!modeMap.containsKey(mode.split("_")[0])) {
				Mode m = new Mode();
				modeMap.put(mode.split("_")[0], m);
			}

			Mode m = modeMap.get(mode.split("_")[0]);
			if (mode.contains("less_than_fifty")) {
				m.setCountByDistanceLessThanFifty(count);
			} else if (mode.contains("more_than_fifty")) {
				m.setCountByDistanceMoreThanFifty(count);
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
			m.emissons = new Emissons();

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
			int countByDistanceLessThanFiftyTmp = m.getCountByDistanceLessThanFifty();
			int countByDistanceMoreThanFiftyTmp = m.getCountByDistanceMoreThanFifty();
			double travelTimeTmp = m.getTravelTime();
			double traveledDistanceTmp = m.getTraveledDistance();
			double travelAndWaitingTimeTmp = m.getTravelAndWaitingTime();
			double NOx = m.getEmissons().getNOx();
			double CO2_TOTAL = m.getEmissons().getCO2_TOTAL();
			double CO = m.getEmissons().getCO();
			double HC = m.getEmissons().getHC();
			double PM = m.getEmissons().getPM();
			double SO2 = m.getEmissons().getSO2();

			ArrayList<String> values = new ArrayList<>();
			ArrayList<String> units = new ArrayList<>();
			ArrayList<String> descriptions = new ArrayList<>();

			values.add(String.valueOf(countTmp));
			values.add(String.valueOf(countByDistanceLessThanFiftyTmp));
			values.add(String.valueOf(countByDistanceMoreThanFiftyTmp));
			values.add(String.format("%.2f", travelTimeTmp));
			values.add(String.format("%.2f", traveledDistanceTmp));
			values.add(String.format("%.2f", travelAndWaitingTimeTmp));
			values.add(String.format("%.2f", NOx));
			values.add(String.format("%.2f", CO2_TOTAL));
			values.add(String.format("%.2f", CO));
			values.add(String.format("%.2f", HC));
			values.add(String.format("%.2f", PM));
			values.add(String.format("%.2f", SO2));

			units.add("Vehicle / Day");
			units.add("Vehicle / Day");
			units.add("Vehicle / Day");
			units.add("Mio. Hours / Year");
			units.add("Mio. Meters / Year");
			units.add("Mio. Hours / Year");
			units.add("g / Day");
			units.add("g / Day");
			units.add("g / Day");
			units.add("g / Day");
			units.add("g / Day");
			units.add("g / Day");

			descriptions.add("Average vehicle loads");
			descriptions.add("Number of trips ≤ 50 km");
			descriptions.add("Number of trips > 50 km");
			descriptions.add("Average travel time");
			descriptions.add("Average travel distance");
			descriptions.add("Average travel and waiting time");
			descriptions.add("NOx");
			descriptions.add("CO2_Total");
			descriptions.add("CO");
			descriptions.add("HC");
			descriptions.add("PM");
			descriptions.add("SO2");


			StringColumn.create("first_column", descriptions);
			StringColumn.create("second_column", values);
			StringColumn.create("third_column", units);

			Table.create("Total Result")
				.addColumns(StringColumn.create("description", descriptions), StringColumn.create("value", values), StringColumn.create("unit", units))
				.write()
				.csv(output.getPath("total_%s.csv", mode)
					.toFile());
		});

		return 0;
	}

//	private Config prepareConfig() {
//		Config config = ConfigUtils.loadConfig(ApplicationUtils.matchInput("config.xml", input.getRunDirectory()).toAbsolutePath().toString());
//		config.vehicles().setVehiclesFile(ApplicationUtils.matchInput("vehicles", input.getRunDirectory()).toAbsolutePath().toString());
//		config.network().setInputFile(ApplicationUtils.matchInput("network", input.getRunDirectory()).toAbsolutePath().toString());
//		config.transit().setTransitScheduleFile(ApplicationUtils.matchInput("transitSchedule", input.getRunDirectory()).toAbsolutePath().toString());
//		config.transit().setVehiclesFile(ApplicationUtils.matchInput("transitVehicles", input.getRunDirectory()).toAbsolutePath().toString());
//		config.plans().setInputFile(null);
//		config.facilities().setInputFile(null);
//		config.eventsManager().setNumberOfThreads(null);
//		config.eventsManager().setEstimatedNumberOfEvents(null);
//		config.global().setNumberOfThreads(1);
//		return config;
//	}

	private static class Mode {
		private int count; // count
		private int countByDistanceLessThanFifty; // count
		private int countByDistanceMoreThanFifty; // count
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

		public int getCountByDistanceLessThanFifty() {
			return countByDistanceLessThanFifty;
		}

		public void setCountByDistanceLessThanFifty(int countByDistanceLessThanFifty) {
			this.countByDistanceLessThanFifty = countByDistanceLessThanFifty;
		}

		public int getCountByDistanceMoreThanFifty() {
			return countByDistanceMoreThanFifty;
		}

		public void setCountByDistanceMoreThanFifty(int countByDistanceMoreThanFifty) {
			this.countByDistanceMoreThanFifty = countByDistanceMoreThanFifty;
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
				", countByDistanceLessThanFifty=" + countByDistanceLessThanFifty +
				", countByDistanceMoreThanFifty=" + countByDistanceMoreThanFifty +
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
