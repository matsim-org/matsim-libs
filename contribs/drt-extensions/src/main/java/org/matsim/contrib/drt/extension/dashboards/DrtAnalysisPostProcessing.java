package org.matsim.contrib.drt.extension.dashboards;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureStore;
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
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
			.columnTypesPartial(Map.of("vehicles", ColumnType.DOUBLE,
				"totalDistance", ColumnType.DOUBLE,
				"emptyRatio", ColumnType.DOUBLE,
				"totalServiceDuration", ColumnType.DOUBLE,
				"d_p/d_t", ColumnType.DOUBLE,
				"totalPassengerDistanceTraveled", ColumnType.DOUBLE))
			.separator(';').build());

		//read customer stats
		Path customerStatsPath = ApplicationUtils.matchInput("drt_customer_stats_" + drtMode + ".csv", input.getRunDirectory());
		Table customerStats = Table.read().csv(CsvReadOptions.builder(customerStatsPath.toFile())
			.columnTypesPartial(Map.of(
				"rides", ColumnType.DOUBLE,
				"wait_average", ColumnType.DOUBLE,
				"wait_p95", ColumnType.DOUBLE,
				"inVehicleTravelTime_mean", ColumnType.DOUBLE,
				"distance_m_mean", ColumnType.DOUBLE,
				"directDistance_m_mean", ColumnType.DOUBLE,
				"totalTravelTime_mean", ColumnType.DOUBLE,
				"fareAllReferences_mean", ColumnType.DOUBLE,
				"rejections", ColumnType.DOUBLE,
				"rejectionRate", ColumnType.DOUBLE))
			.separator(';').build());

		Table tableSupplyKPI = Table.create("supplyKPI");
		if(stopsFile != null){
			List<TransitStopFacility> stops = readTransitStops(stopsFile);
			writeStopsShp(stops, output.getPath("stops.shp"));
			Map<String, TransitStopFacility> byLink = stops.stream().collect(Collectors.toMap(s -> s.getLinkId().toString(), Function.identity()));

			//needs to be a DoubleColumn because transposing later forces us to have the same column type for all (new) value columns
			tableSupplyKPI.addColumns(DoubleColumn.create("number of stops", new Integer[]{stops.size()}));

			writeTripsPerStop(stops, legs, output.getPath("trips_per_stop.csv"));
			writeOD(byLink, legs, output.getPath("od.csv"));
		}

		if(areaFile != null){
			//TODO discuss whether this is our preferred practice, in general. (input file might not be accessible for simwrapper (web, different partition, ...)
			Collection<SimpleFeature> allFeatures = ShapeFileReader.getAllFeatures(areaFile);
			//do not convert coordinates! all MATSim output should be in the same CRS. if input was not in correct CRS, simulation would have crashed...
			ShapeFileWriter.writeGeometries(allFeatures,output.getPath("serviceArea.shp").toString());
			//needs to be a DoubleColumn because transposing later forces us to have the same column type for all (new) value columns
			tableSupplyKPI.addColumns(DoubleColumn.create("number of areas", new Integer[]{allFeatures.size()}));
		}

		tableSupplyKPI.addColumns(prepareSupplyKPITable(vehicleStats).columnArray());
		Table tableDemandKPI = prepareDemandKPITable(customerStats, tableSupplyKPI);

		//TODO format to 3 decimal points only

		tableSupplyKPI = tableSupplyKPI.transpose(true, false);
		tableSupplyKPI.column(0).setName("info");
		tableSupplyKPI.column(1).setName("value");
		tableSupplyKPI.write().csv(output.getPath("supply_kpi.csv").toFile());

		tableDemandKPI = tableDemandKPI.transpose(true, false);
		tableDemandKPI.column(0).setName("info");
		tableDemandKPI.column(1).setName("value");
		tableDemandKPI.write().csv(output.getPath("demand_kpi.csv").toFile());

		return 0;
	}

	private static Table prepareSupplyKPITable(Table vehicleStats) {
		//only use last row (last iteration) and certain columns
		Table tableSupplyKPI = vehicleStats.dropRange(vehicleStats.rowCount() - 1)
			.selectColumns("vehicles", "totalDistance", "emptyRatio", "totalPassengerDistanceTraveled", "d_p/d_t", "totalServiceDuration" );

		//important to know: divide function creates a copy of the column object, setName() does not!, so call divide first in order to avoid verbose code

		tableSupplyKPI.replaceColumn("totalServiceDuration",  ((DoubleColumn)tableSupplyKPI
			.column("totalServiceDuration"))
			.divide(3600)
			.setName("total service hours"));

		tableSupplyKPI.replaceColumn("totalDistance", ((DoubleColumn)tableSupplyKPI
			.column("totalDistance"))
			.divide(1000)
			.setName("total fleet mileage [km]"));

		tableSupplyKPI.replaceColumn("totalPassengerDistanceTraveled",  ((DoubleColumn)tableSupplyKPI
			.column("totalPassengerDistanceTraveled"))
			.divide(1000)
			.setName("total passenger km"));

		return tableSupplyKPI;
	}

	private static Table prepareDemandKPITable(Table customerStats, Table tableSupplyKPI) {
		//only use last row (last iteration) and certain columns
		Table tableDemandKPI = customerStats.dropRange(customerStats.rowCount() - 1)
			.selectColumns("rides", "rejections", "rejectionRate", "totalTravelTime_mean", "inVehicleTravelTime_mean", "wait_average", "wait_p95",
				"distance_m_mean", "directDistance_m_mean",  "fareAllReferences_mean");

		tableDemandKPI.replaceColumn("totalTravelTime_mean",  ((DoubleColumn)tableDemandKPI
			.column("totalTravelTime_mean"))
			.divide(60)
			.setName("Avg. total travel time [min]"));

		tableDemandKPI.replaceColumn("inVehicleTravelTime_mean",  ((DoubleColumn)tableDemandKPI
			.column("inVehicleTravelTime_mean"))
			.divide(60)
			.setName("Avg. in-vehicle time [min]"));

		tableDemandKPI.replaceColumn("wait_average",  ((DoubleColumn)tableDemandKPI
			.column("wait_average"))
			.divide(60)
			.setName("Avg. wait time [min]"));

		tableDemandKPI.replaceColumn("wait_p95", ((DoubleColumn)tableDemandKPI
			.column("wait_p95"))
			.divide(60)
			.setName("95th percentile wait time [min]"));

		tableDemandKPI.replaceColumn("distance_m_mean", ((DoubleColumn)tableDemandKPI
			.column("distance_m_mean"))
			.divide(1000)
			.setName("Avg. driven distance [km]"));

		tableDemandKPI.replaceColumn("directDistance_m_mean", ((DoubleColumn)tableDemandKPI
			.column("directDistance_m_mean"))
			.divide(1000)
			.setName("Avg. direct distance [km]"));

		tableDemandKPI.column("fareAllReferences_mean")
			.setName("Avg. fare [MoneyUnit]");

		//compute rides per vehicle-hour
		DoubleColumn ridesPerVeh = ((DoubleColumn) tableDemandKPI
			.column("rides")
			.copy())
			.divide((DoubleColumn) tableSupplyKPI.column("vehicles"))
			.setName("rides per veh");

		DoubleColumn ridesPerVehH = ((DoubleColumn) tableDemandKPI
			.column("rides")
			.copy())
			.divide((DoubleColumn) tableSupplyKPI.column("total service hours"))
			.setName("rides per veh-h");

		DoubleColumn ridesPerVehKm = ((DoubleColumn) tableDemandKPI
			.column("rides")
			.copy())
			.divide((DoubleColumn) tableSupplyKPI.column("total fleet mileage [km]"))
			.setName("rides per veh-km");

		tableDemandKPI.addColumns(ridesPerVeh, ridesPerVehH, ridesPerVehKm);

		return tableDemandKPI;
	}

	private void writeStopsShp(Collection<TransitStopFacility> stops, Path path) throws IOException, SchemaException {

		Map<String, Object> map = new HashMap<>();
		map.put("url", path.toUri().toURL());

		FileDataStoreFactorySpi factory = new ShapefileDataStoreFactory();
		ShapefileDataStore shp = (ShapefileDataStore) factory.createNewDataStore(map);

		//TODO all the MATSim output should be in the same CRS! So, do not transform stop coordinates!! (Simwrapper automatically picks up the crs from the resulting .prj file and does conversion itself)
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

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(path), CSVFormat.DEFAULT)) {

			csv.printRecord("from_stop", "to_stop", "trips");

			for (Object2IntMap.Entry<OD> e : trips.object2IntEntrySet()) {
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
