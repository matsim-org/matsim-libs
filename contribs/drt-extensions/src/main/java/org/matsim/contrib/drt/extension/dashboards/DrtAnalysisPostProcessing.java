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
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import picocli.CommandLine;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
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
	produces = {"kpi.csv", "stops.shp", "trips_per_stop.csv", "od.csv"},
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

	@CommandLine.Option(names = "--stop-file", description = "URL to drt stop file")
	private URL stopFile;

	public static void main(String[] args) {
		new DrtAnalysisPostProcessing().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		List<TransitStopFacility> stops = readTransitStops(stopFile);

		// TODO: if there is no stop file, a pseudo-stop file needs to be generated from the legs

		writeStopsShp(stops, output.getPath("stops.shp"));

		Map<String, TransitStopFacility> byLink = stops.stream().collect(Collectors.toMap(s -> s.getLinkId().toString(), Function.identity()));

		Path legPath = ApplicationUtils.matchInput("drt_legs_" + drtMode + ".csv", input.getRunDirectory());

		Table legs = Table.read().csv(CsvReadOptions.builder(legPath.toFile()).separator(';')
			.columnTypesPartial(Map.of(
				"personId", ColumnType.TEXT, "vehicleId", ColumnType.TEXT,
				"toLinkId", ColumnType.TEXT, "fromLinkId", ColumnType.TEXT
			)).build());

		writeTripsPerStop(stops, legs, output.getPath("trips_per_stop.csv"));
		writeOD(byLink, legs, output.getPath("od.csv"));

		return 0;
	}

	private void writeStopsShp(Collection<TransitStopFacility> stops, Path path) throws IOException, SchemaException {

		Map<String, Object> map = new HashMap<>();
		map.put("url", path.toUri().toURL());

		FileDataStoreFactorySpi factory = new ShapefileDataStoreFactory();
		ShapefileDataStore shp = (ShapefileDataStore) factory.createNewDataStore(map);

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
