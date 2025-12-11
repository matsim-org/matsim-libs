package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.simwrapper.ComponentMixin;
import org.matsim.testcases.MatsimTestUtils;
import tech.tablesaw.plotly.components.Component;

import java.io.IOException;

/**
 * Test class for BackgroundLayer functionality in SimWrapper visualizations.
 */
public class BackgroundLayerTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private ObjectWriter writer;

	@BeforeEach
	public void setUp() throws Exception {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory()
				.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
				.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
				.setSerializationInclusion(JsonInclude.Include.NON_NULL)
				.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

		mapper.registerModule(new JavaTimeModule());
		mapper.addMixIn(Component.class, ComponentMixin.class);

		writer = mapper.writer();
	}

	@Test
	void gridMapWithBackgroundLayers() throws IOException {
		GridMap gridMap = new GridMap();
		gridMap.title = "Test Grid Map";
		gridMap.file = "data.csv";
		gridMap.projection = "EPSG:25832";

		// Add first background layer with all properties
		gridMap.addBackgroundLayer("stadtgrenze",
				new BackgroundLayer("shape-file-1.shp")
						.setFill("none")
						.setOpacity(1.0)
						.setBorderWidth(4)
						.setBorderColor("#ff0000")
						.setVisible(true)
						.setOnTop(false)
						.setLabel("city_name")
		);

		// Add second background layer with minimal properties
		gridMap.addBackgroundLayer("bezirke",
				new BackgroundLayer("geopackage-file-1.gpkg")
						.setFill("#4488ff")
						.setOpacity(0.3)
		);

		String yaml = writer.writeValueAsString(gridMap);

//		System.out.println("=== GridMap YAML Output ===");
//		System.out.println(yaml);
//		System.out.println("===========================");

		Assertions.assertThat(yaml)
				.contains("title: Test Grid Map")
				.contains("file: data.csv")
				.contains("projection: EPSG:25832")
				.contains("backgroundLayers:")
				.contains("stadtgrenze:")
				.contains("shapes: shape-file-1.shp")
				.contains("fill: none")
				.contains("opacity: 1.0")
				.contains("borderWidth: 4")
				.contains("borderColor: \"#ff0000\"")
				.contains("visible: true")
				.contains("onTop: false")
				.contains("label: city_name")
				.contains("bezirke:")
				.contains("shapes: geopackage-file-1.gpkg")
				.contains("fill: \"#4488ff\"")
				.contains("opacity: 0.3");
	}

	@Test
	void hexagonsWithBackgroundLayer() throws IOException {
		Hexagons hexagons = new Hexagons();
		hexagons.title = "Test Hexagons";
		hexagons.file = "trips.csv";
		hexagons.projection = "EPSG:25832";

		hexagons.addBackgroundLayer("study_area",
				new BackgroundLayer("kehlheim_shape.shp")
						.setFill("salmon")
						.setOpacity(0.6)
						.setBorderWidth(2)
						.setBorderColor("darkred")
						.setVisible(false)
						.setOnTop(true)
		);

		String yaml = writer.writeValueAsString(hexagons);

		Assertions.assertThat(yaml)
				.contains("title: Test Hexagons")
				.contains("backgroundLayers:")
				.contains("study_area:")
				.contains("shapes: kehlheim_shape.shp")
				.contains("fill: salmon")
				.contains("opacity: 0.6")
				.contains("borderWidth: 2")
				.contains("borderColor: darkred")
				.contains("visible: false")
				.contains("onTop: true");
	}

	@Test
	void xyTimeWithBackgroundLayer() throws IOException {
		XYTime xyTime = new XYTime();
		xyTime.title = "Test XY Time";
		xyTime.file = "points.csv";

		xyTime.addBackgroundLayer("base_map",
				new BackgroundLayer("geopackage-file-2.gpkg")
						.setFill("Viridis")
		);

		String yaml = writer.writeValueAsString(xyTime);

		Assertions.assertThat(yaml)
				.contains("title: Test XY Time")
				.contains("backgroundLayers:")
				.contains("base_map:")
				.contains("shapes: geopackage-file-2.gpkg")
				.contains("fill: Viridis");
	}

	@Test
	void linksWithBackgroundLayer() throws IOException {
		Links links = new Links();
		links.title = "Test Links";
		links.network = "network.xml.gz";
		links.datasets.csvFile = "volumes.csv";
		links.projection = "EPSG:31468";

		links.addBackgroundLayer("regions",
				new BackgroundLayer("shape-file-1.shp")
						.setFill("#00ff00")
						.setOpacity(0.25)
						.setLabel("region_name")
		);

		String yaml = writer.writeValueAsString(links);

		Assertions.assertThat(yaml)
				.contains("title: Test Links")
				.contains("backgroundLayers:")
				.contains("regions:")
				.contains("shapes: shape-file-1.shp");
	}

	@Test
	void mapPlotWithBackgroundLayer() throws IOException {
		MapPlot mapPlot = new MapPlot();
		mapPlot.title = "Test Map Plot";
		mapPlot.setShape("shape.geojson");

		mapPlot.addBackgroundLayer("layer1",
				new BackgroundLayer("shape-file-1.shp")
						.setFill("#123456")
		);

		String yaml = writer.writeValueAsString(mapPlot);

		Assertions.assertThat(yaml)
				.contains("title: Test Map Plot")
				.contains("backgroundLayers:")
				.contains("layer1:")
				.contains("shapes: shape-file-1.shp");
	}

	@Test
	void transitViewerWithBackgroundLayer() throws IOException {
		TransitViewer transitViewer = new TransitViewer();
		transitViewer.title = "Test Transit Viewer";
		transitViewer.network = "network.xml.gz";
		transitViewer.transitSchedule = "schedule.xml.gz";

		transitViewer.addBackgroundLayer("zones",
				new BackgroundLayer("geopackage-file-1.gpkg")
						.setFill("none")
						.setBorderWidth(3)
						.setBorderColor("blue")
		);

		String yaml = writer.writeValueAsString(transitViewer);

		Assertions.assertThat(yaml)
				.contains("title: Test Transit Viewer")
				.contains("backgroundLayers:")
				.contains("zones:");
	}

	@Test
	void flowMapWithBackgroundLayer() throws IOException {
		FlowMap flowMap = new FlowMap();
		flowMap.title = "Test Flow Map";

		flowMap.addBackgroundLayer("boundaries",
				new BackgroundLayer("shape-file-1.shp")
		);

		String yaml = writer.writeValueAsString(flowMap);

		Assertions.assertThat(yaml)
				.contains("title: Test Flow Map")
				.contains("backgroundLayers:")
				.contains("boundaries:");
	}

	@Test
	void vehiclesWithBackgroundLayer() throws IOException {
		Vehicles vehicles = new Vehicles();
		vehicles.title = "Test Vehicles";
		vehicles.drtTrips = "drt-vehicles.json";
		vehicles.projection = "EPSG:25832";

		vehicles.addBackgroundLayer("service_area",
				new BackgroundLayer("service-area.geojson")
						.setFill("#ffaa00")
						.setOpacity(0.4)
		);

		String yaml = writer.writeValueAsString(vehicles);

		Assertions.assertThat(yaml)
				.contains("title: Test Vehicles")
				.contains("backgroundLayers:")
				.contains("service_area:");
	}

	@Test
	void carrierViewerWithBackgroundLayer() throws IOException {
		CarrierViewer carrierViewer = new CarrierViewer();
		carrierViewer.title = "Test Carrier Viewer";
		carrierViewer.network = "network.xml.gz";
		carrierViewer.carriers = "carriers.xml.gz";

		carrierViewer.addBackgroundLayer("delivery_zones",
				new BackgroundLayer("geopackage-file-1.gpkg")
		);

		String yaml = writer.writeValueAsString(carrierViewer);

		Assertions.assertThat(yaml)
				.contains("title: Test Carrier Viewer")
				.contains("backgroundLayers:")
				.contains("delivery_zones:");
	}

	@Test
	void logisticViewerWithBackgroundLayer() throws IOException {
		LogisticViewer logisticViewer = new LogisticViewer();
		logisticViewer.title = "Test Logistic Viewer";
		logisticViewer.network = "network.xml.gz";
		logisticViewer.carriers = "carriers.xml.gz";
		logisticViewer.lsps = "lsps.xml.gz";

		logisticViewer.addBackgroundLayer("warehouses",
				new BackgroundLayer("warehouse-locations.geojson")
						.setFill("red")
						.setOnTop(true)
		);

		String yaml = writer.writeValueAsString(logisticViewer);

		Assertions.assertThat(yaml)
				.contains("title: Test Logistic Viewer")
				.contains("backgroundLayers:")
				.contains("warehouses:");
	}

	@Test
	void multipleBackgroundLayers() throws IOException {
		GridMap gridMap = new GridMap();
		gridMap.title = "Multiple Layers Test";
		gridMap.file = "data.csv";
		gridMap.projection = "EPSG:25832";

		// Add three layers as in the specification example
		gridMap.addBackgroundLayer("stadtgrenze",
				new BackgroundLayer("shape-file-1.shp")
						.setFill("none")
						.setOpacity(1.0)
						.setBorderWidth(4)
						.setBorderColor("#ff0000")
						.setVisible(true)
						.setOnTop(false)
						.setLabel("city_name")
		);

		gridMap.addBackgroundLayer("bezirke",
				new BackgroundLayer("geopackage-file-1.shp")
						.setFill("#4488ff")
						.setOpacity(0.3)
						.setBorderWidth(1)
						.setBorderColor("#0033aa")
						.setVisible(true)
						.setOnTop(false)
						.setLabel("district")
		);

		gridMap.addBackgroundLayer("studiengebiet",
				new BackgroundLayer("kehlheim_shape.shp")
						.setFill("salmon")
						.setOpacity(0.6)
						.setBorderWidth(2)
						.setBorderColor("darkred")
						.setVisible(false)
						.setOnTop(true)
						.setLabel("area_id")
		);

		String yaml = writer.writeValueAsString(gridMap);

		// Verify all three layers are present
		Assertions.assertThat(yaml)
				.contains("backgroundLayers:")
				.contains("stadtgrenze:")
				.contains("bezirke:")
				.contains("studiengebiet:");
	}

	@Test
	void backgroundLayerWithMinimalProperties() throws IOException {
		GridMap gridMap = new GridMap();
		gridMap.title = "Minimal Layer Test";
		gridMap.file = "data.csv";
		gridMap.projection = "EPSG:25832";

		// Only required property
		gridMap.addBackgroundLayer("simple",
				new BackgroundLayer("shape-file-2.shp")
		);

		String yaml = writer.writeValueAsString(gridMap);

		Assertions.assertThat(yaml)
				.contains("backgroundLayers:")
				.contains("simple:")
				.contains("shapes: shape-file-2.shp");

		// Verify optional properties are not present
		Assertions.assertThat(yaml)
				.doesNotContain("opacity:")
				.doesNotContain("borderWidth:")
				.doesNotContain("borderColor:")
				.doesNotContain("visible:")
				.doesNotContain("onTop:")
				.doesNotContain("label:");
	}
}
