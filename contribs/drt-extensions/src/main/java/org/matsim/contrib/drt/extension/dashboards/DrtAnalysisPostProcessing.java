package org.matsim.contrib.drt.extension.dashboards;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.geotools.api.data.FileDataStoreFactorySpi;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.GeoFileReader;
import org.matsim.core.utils.gis.GeoFileWriter;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import picocli.CommandLine;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@CommandLine.Command(
	name = "drt-post-process",
	description = "Creates additional files for drt dashboards."
)
@CommandSpec(
	requireRunDirectory = true,
	produces = {"supply_kpi.csv", "demand_kpi.csv", "stops.shp", "trips_per_stop.csv", "od.csv", "serviceArea.shp"},
	group = "drt"
)
public final class DrtAnalysisPostProcessing implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(DrtAnalysisPostProcessing.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(DrtAnalysisPostProcessing.class);

	@CommandLine.Mixin
	private CrsOptions crs;

	@CommandLine.Option(names = "--drt-mode", required = true, description = "Name of the drt mode to analyze.")
	private String drtMode;

	@CommandLine.Option(names = "--stops-file", description = "URL to drt stops file")
	private URL stopsFile;

	@CommandLine.Option(names = "--area-file", description = "URL to drt service area file")
	private URL areaFile;

	public static void main(String[] args) {
		new DrtAnalysisPostProcessing().execute(args);
	}

	private static Table prepareSupplyKPITable(Table vehicleStats) {
		//only use last row (last iteration) and certain columns
		Table tableSupplyKPI = vehicleStats.dropRange(vehicleStats.rowCount() - 1)
			.selectColumns("vehicles", "totalDistance", "emptyRatio", "totalPassengerDistanceTraveled", "d_p/d_t", "l_det", "totalServiceDuration");

		//important to know: divide function creates a copy of the column object, setName() does not!, so call divide first in order to avoid verbose code

		tableSupplyKPI.replaceColumn("totalServiceDuration", ((DoubleColumn) tableSupplyKPI
			.column("totalServiceDuration"))
			.divide(3600)
			.round()
			.setName("Total service hours"));

		tableSupplyKPI.replaceColumn("totalDistance", ((DoubleColumn) tableSupplyKPI
			.column("totalDistance"))
			.divide(1000)
			.round()
			.setName("Total vehicle mileage [km]"));

		tableSupplyKPI.replaceColumn("totalPassengerDistanceTraveled", ((DoubleColumn) tableSupplyKPI
			.column("totalPassengerDistanceTraveled"))
			.divide(1000)
			.round()
			.setName("Total pax distance [km]"));

		tableSupplyKPI.column("d_p/d_t").setName("Occupancy rate [pax-km/v-km]");
		tableSupplyKPI.column("l_det").setName("Detour ratio");
		tableSupplyKPI.column("emptyRatio").setName("Empty ratio");

		return tableSupplyKPI;
	}

	private static void printDemandKPICSV(Table customerStats, Table tableSupplyKPI, Path output) throws IOException {
		double rides = getLastDoubleValue(customerStats, "rides");
		double rejections = getLastDoubleValue(customerStats, "rejections");
		double requests = rides + rejections;
		double pax = getLastDoubleValue(customerStats, "rides_pax");

		DecimalFormat df = new DecimalFormat("#,###.##", new DecimalFormatSymbols(Locale.US));

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output), CSVFormat.DEFAULT)) {
			csv.printRecord("Info", "value");

			csv.printRecord("Handled Requests", requests);
			csv.printRecord("Passengers (Pax)", pax);
			csv.printRecord("Avg Group Size", df.format((getLastDoubleValue(customerStats, "groupSize_mean"))));
			csv.printRecord("Pax per veh", df.format(pax / ((DoubleColumn) tableSupplyKPI.column("vehicles")).get(0)));
			csv.printRecord("Pax per veh-h", df.format(pax / ((DoubleColumn) tableSupplyKPI.column("total service hours")).get(0)));
			csv.printRecord("Pax per veh-km", df.format(pax / ((DoubleColumn) tableSupplyKPI.column("total vehicle mileage [km]")).get(0)));
			csv.printRecord("Rejections", rejections);
			csv.printRecord("Rejection rate", getLastDoubleValue(customerStats, "rejectionRate"));
			csv.printRecord("Avg. total travel time", LocalTime.ofSecondOfDay(getLastDoubleValue(customerStats, "totalTravelTime_mean").longValue()));
			csv.printRecord("Avg. in-vehicle time", LocalTime.ofSecondOfDay(getLastDoubleValue(customerStats, "inVehicleTravelTime_mean").longValue()));
			csv.printRecord("Avg. wait time", LocalTime.ofSecondOfDay(getLastDoubleValue(customerStats, "wait_average").longValue()));
			csv.printRecord("95th percentile wait time", LocalTime.ofSecondOfDay(getLastDoubleValue(customerStats, "wait_p95").longValue()));
			csv.printRecord("Avg. ride distance [km]", df.format((getLastDoubleValue(customerStats, "distance_m_mean")) / 1000.));
			csv.printRecord("Avg. direct distance [km]", df.format((getLastDoubleValue(customerStats, "directDistance_m_mean")) / 1000.));
			csv.printRecord("Avg. fare [MoneyUnit]", df.format(getLastDoubleValue(customerStats, "fareAllReferences_mean")));
		}
	}

	private static Double getLastDoubleValue(Table table, String columnName) {
		//the value can be null in case we are reading the value "NaN" within a DoubleColumn (this happens e.g. for 'wait_average' in customer stats when there is 0 rides)
		if (table.column(columnName).get(table.rowCount() - 1) == null) {
			return Double.NaN;
		} else {
			return ((DoubleColumn) table.column(columnName)).get(table.rowCount() - 1);
		}
	}

	@Override
	public Integer call() throws Exception {

		Path legPath = ApplicationUtils.matchInput("drt_legs_" + drtMode + ".csv", input.getRunDirectory());
		Table legs = Table.read().csv(CsvReadOptions.builder(legPath.toFile())
			.separator(';')
			.columnTypesPartial(Map.of(
				"personId", ColumnType.TEXT, "vehicleId", ColumnType.TEXT,
				"toLinkId", ColumnType.TEXT, "fromLinkId", ColumnType.TEXT
			)).build());

		//read vehicle stats
		Path vehicleStatsPath = ApplicationUtils.matchInput("drt_vehicle_stats_" + drtMode + ".csv", input.getRunDirectory());
		Table vehicleStats = Table.read().csv(CsvReadOptions.builder(vehicleStatsPath.toFile())
			.columnTypes(columnHeader -> columnHeader.equals("runId") ? ColumnType.STRING : ColumnType.DOUBLE)
			.separator(';').build());

		//read customer stats
		Path customerStatsPath = ApplicationUtils.matchInput("drt_customer_stats_" + drtMode + ".csv", input.getRunDirectory());
		Table customerStats = Table.read().csv(CsvReadOptions.builder(customerStatsPath.toFile())
				.columnTypes(columnHeader -> columnHeader.equals("runId") ? ColumnType.STRING : ColumnType.DOUBLE)
			.separator(';').build());

		Table tableSupplyKPI = Table.create("supplyKPI");
		if (stopsFile != null) {
			List<TransitStopFacility> stops = readTransitStops(stopsFile);
			writeStopsShp(stops, output.getPath("stops.shp"));
			Map<String, TransitStopFacility> byLink = stops.stream().collect(Collectors.toMap(s -> s.getLinkId().toString(), Function.identity()));

			//needs to be a DoubleColumn because transposing later forces us to have the same column type for all (new) value columns
			tableSupplyKPI.addColumns(DoubleColumn.create("Number of stops", new Integer[]{stops.size()}));

			writeTripsPerStop(stops, legs, output.getPath("trips_per_stop.csv"));
			writeOD(byLink, legs, output.getPath("od.csv"));
		}

		if (areaFile != null) {
			// create copy of shp file so that it is accessible for sim wrapper
			Collection<SimpleFeature> allFeatures = GeoFileReader.getAllFeatures(areaFile);
			//do not convert coordinates! all MATSim output should be in the same CRS. if input was not in correct CRS, simulation would have crashed...
			GeoFileWriter.writeGeometries(allFeatures, output.getPath("serviceArea.shp").toString());
			//needs to be a DoubleColumn because transposing later forces us to have the same column type for all (new) value columns
			tableSupplyKPI.addColumns(DoubleColumn.create("Number of areas", new Integer[]{allFeatures.size()}));
		}

		tableSupplyKPI.addColumns(prepareSupplyKPITable(vehicleStats).columnArray());

		printDemandKPICSV(customerStats, tableSupplyKPI, output.getPath("demand_kpi.csv"));

		tableSupplyKPI = tableSupplyKPI.transpose(true, false);
		tableSupplyKPI.column(0).setName("info");
		tableSupplyKPI.column(1).setName("value");
		tableSupplyKPI.replaceColumn(0, tableSupplyKPI.stringColumn(0).capitalize());

		tableSupplyKPI.write().csv(output.getPath("supply_kpi.csv").toFile());

		return 0;
	}

	private void writeStopsShp(Collection<TransitStopFacility> stops, Path path) throws IOException, SchemaException {

		Map<String, Object> map = new HashMap<>();
		map.put("url", path.toUri().toURL());

		FileDataStoreFactorySpi factory = new ShapefileDataStoreFactory();
		ShapefileDataStore shp = (ShapefileDataStore) factory.createNewDataStore(map);

		// re-project to default crs (lat/lon)
		SimpleFeatureType featureType = DataUtilities.createType("stop", "the_geom:Point:srid=4326,id:String,linkId:String,name:String");
		shp.createSchema(featureType);

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(crs.getInputCRS(), "EPSG:4326");

		List<SimpleFeature> features = new ArrayList<>();

		GeometryFactory f = JTSFactoryFinder.getGeometryFactory();
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);

		// Build transit stop features
		for (TransitStopFacility stop : stops) {

			Coord coord = ct.transform(stop.getCoord());

			Point point = f.createPoint(new Coordinate(coord.getX(), coord.getY()));
			featureBuilder.add(point);
			featureBuilder.add(stop.getId().toString());
			featureBuilder.add(stop.getLinkId().toString());
			featureBuilder.add(stop.getName());

			features.add(featureBuilder.buildFeature(stop.getId().toString()));
		}

		try (Transaction transaction = new DefaultTransaction("create")) {
			String typeName = shp.getTypeNames()[0];
			SimpleFeatureStore featureSource = (SimpleFeatureStore) shp.getFeatureSource(typeName);
			featureSource.setTransaction(transaction);
			featureSource.addFeatures(new ListFeatureCollection(featureType, features));
			transaction.commit();
		}
	}

	private void writeTripsPerStop(List<TransitStopFacility> stops, Table legs, Path path) throws IOException {

		Object2IntMap<String> departures = new Object2IntOpenHashMap<>();
		Object2IntMap<String> arrivals = new Object2IntOpenHashMap<>();

		for (Row leg : legs) {
			departures.mergeInt(leg.getString("fromLinkId"), 1, Integer::sum);
			arrivals.mergeInt(leg.getString("toLinkId"), 1, Integer::sum);
		}

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(path), CSVFormat.DEFAULT)) {

			csv.printRecord("stop_id", "departures", "arrivals");

			for (TransitStopFacility stop : stops) {
				csv.printRecord(stop.getId(), departures.getInt(stop.getLinkId().toString()), arrivals.getInt(stop.getLinkId().toString()));
			}
		}
	}

	private void writeOD(Map<String, TransitStopFacility> stops, Table legs, Path path) throws IOException {

		Object2IntMap<OD> trips = new Object2IntOpenHashMap<>();

		for (Row leg : legs) {
			trips.mergeInt(new OD(leg.getString("fromLinkId"), leg.getString("toLinkId")), 1, Integer::sum);
		}

		Comparator<Object2IntMap.Entry<OD>> cmp = Comparator.comparingInt(Object2IntMap.Entry::getIntValue);

		// Output only the top 200 relations, to avoid to large data
		List<Object2IntMap.Entry<OD>> entries = trips.object2IntEntrySet().stream()
			.sorted(cmp.reversed()).limit(200).toList();

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(path), CSVFormat.DEFAULT)) {

			csv.printRecord("from_stop", "to_stop", "trips");

			for (Object2IntMap.Entry<OD> e : entries) {
				OD od = e.getKey();

				if (e.getIntValue() > 0)
					csv.printRecord(stops.get(od.origin).getLinkId(), stops.get(od.destination).getLinkId(), e.getIntValue());
			}
		}
	}

	private List<TransitStopFacility> readTransitStops(URL stopFile) {

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readURL(stopFile);


		return new ArrayList<>(scenario.getTransitSchedule().getFacilities().values());
	}

	private record OD(String origin, String destination) {
	}

}
