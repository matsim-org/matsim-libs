package org.matsim.application.prepare.network;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import picocli.CommandLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;


/**
 * Note that {@link CreateAvroNetwork} offers a more efficient way to store network data, which also loads much faster in the browser.
 */
@CommandLine.Command(name = "network-geojson", description = "Create geojson representation of a network.")
@CommandSpec(requireNetwork = true, produces = "network.geojson")
public class CreateGeoJsonNetwork implements MATSimAppCommand {

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(CreateGeoJsonNetwork.class);
	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(CreateGeoJsonNetwork.class);
	@CommandLine.Mixin
	private ShpOptions shp;
	@CommandLine.Mixin
	private CrsOptions crs = new CrsOptions(null, "EPSG:4326");

	@CommandLine.Option(names = "--match-id", description = "Pattern to filter links by id")
	private String matchId;

	@CommandLine.Option(names = "--mode-filter", split = ",", defaultValue = "car,freight,drt",
		description = "Only keep links if they have one of the specified modes. Specify 'none' to disable.")
	private Set<String> modes;

	@CommandLine.Option(names = "--precision", description = "Number of decimals places", defaultValue = "6")
	private int precision;

	@CommandLine.Option(names = "--with-properties", description = "Put network attributes as properties into the geojson.")
	private boolean withProperties;

	@CommandLine.Option(names = "--filter-properties", description = "Only include listed properties in the output.")
	private Set<String> filterProperties;

	public static void main(String[] args) {
		new CreateGeoJsonNetwork().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Network network = input.getNetwork();

		String networkCrs = ProjectionUtils.getCRS(input.getNetwork());
		if (crs.getInputCRS() != null)
			networkCrs = crs.getInputCRS();

		if (networkCrs == null) {
			throw new IllegalArgumentException("Network coordinate system is neither in the xml nor given as option.");
		}

		ObjectMapper mapper = new ObjectMapper();

		ObjectNode json = mapper.createObjectNode();

		json.put("type", "FeatureCollection");

		// Default CRS assumed to be 4326
		if (!crs.getTargetCRS().equalsIgnoreCase("epsg:4326")) {
			ObjectNode crs = json.putObject("crs");
			putCrs(crs, networkCrs);
		}

		Predicate<Link> filter = link -> true;

		if (shp.isDefined()) {
			Geometry geom = shp.getGeometry();
			CoordinateTransformation ct = shp.createTransformation(networkCrs);

			filter = link -> geom.contains(MGC.coord2Point(ct.transform(link.getFromNode().getCoord()))) ||
				geom.contains(MGC.coord2Point(ct.transform(link.getToNode().getCoord())));
		}

		if (matchId != null) {
			Pattern p = Pattern.compile(matchId);
			filter = filter.and(link -> p.matcher(link.getId().toString()).matches());
		}

		// At least one of the specified modes needs to be contained
		if (!modes.isEmpty() && !modes.equals(Set.of("none"))) {
			filter = filter.and(link -> modes.stream().anyMatch(m -> link.getAllowedModes().contains(m)));
		}

		convert(json, network, TransformationFactory.getCoordinateTransformation(networkCrs, this.crs.getTargetCRS()), filter);

		mapper.writerFor(JsonNode.class).writeValue(output.getPath().toFile(), json);

		return 0;
	}

	private void putCrs(ObjectNode crs, String networkCrs) {
		crs.put("type", "name");
		ObjectNode prop = crs.putObject("properties");
		prop.put("name", networkCrs);
	}

	private void convert(ObjectNode json, Network network, CoordinateTransformation ct, Predicate<Link> filter) {

		ArrayNode ft = json.putArray("features");

		for (Link link : network.getLinks().values()) {

			if (!filter.test(link))
				continue;

			ObjectNode obj = ft.addObject();

			obj.put("id", link.getId().toString());
			obj.put("type", "Feature");

			ObjectNode geom = obj.putObject("geometry");

			geom.put("type", "LineString");
			ArrayNode coords = geom.putArray("coordinates");

			Coord from = ct.transform(link.getFromNode().getCoord());
			Coord to = ct.transform(link.getToNode().getCoord());

			ArrayNode f = coords.addArray();
			f.add(round(from.getX()));
			f.add(round(from.getY()));

			ArrayNode t = coords.addArray();
			t.add(round(to.getX()));
			t.add(round(to.getY()));

			if (withProperties) {
				ObjectNode prop = obj.putObject("properties");

				for (Map.Entry<String, Object> e : link.getAttributes().getAsMap().entrySet()) {
					Object value = e.getValue();

					// Skip properties that are not defined
					if (filterProperties != null && !filterProperties.isEmpty() && !filterProperties.contains(e.getKey()))
						continue;

					if (value instanceof String || value instanceof Number || value instanceof Boolean)
						prop.putPOJO(e.getKey(), value);
				}
			}
		}
	}

	/**
	 * Round to desired precision.
	 */
	private double round(double x) {
		BigDecimal d = BigDecimal.valueOf(x).setScale(precision, RoundingMode.HALF_UP);
		return d.doubleValue();
	}

}
