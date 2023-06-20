package org.matsim.contrib.drt.extension.dashboards;

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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
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
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import picocli.CommandLine;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
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

		Path legPath = ApplicationUtils.matchInput("drt_legs_" + drtMode + ".csv", input.getRunDirectory());

		Map<Id<Link>, List<TransitStopFacility>> byLink = stops.stream().collect(Collectors.groupingBy(Facility::getLinkId, Collectors.toList()));

		Table legs = Table.read().csv(CsvReadOptions.builder(legPath.toFile()).separator(';')
			.columnTypesPartial(Map.of(
				"personId", ColumnType.TEXT, "vehicleId", ColumnType.TEXT,
				"toLinkId", ColumnType.TEXT, "fromLinkId", ColumnType.TEXT
			)).build());

		writeTripsPerStop(byLink, legs, output.getPath("trips_per_stop.csv"));
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

	private void writeTripsPerStop(Map<Id<Link>, List<TransitStopFacility>> stops, Table legs, Path path) {

	}

	private void writeOD(Map<Id<Link>, List<TransitStopFacility>> stops, Table legs, Path path) {
	}

	private List<TransitStopFacility> readTransitStops(URL stopFile) {

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readURL(stopFile);

		return new ArrayList<>(scenario.getTransitSchedule().getFacilities().values());
	}

}
