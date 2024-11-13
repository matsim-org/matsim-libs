package org.matsim.application.prepare.network;

import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.commons.lang3.mutable.MutableObject;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.avro.AvroNetwork;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@CommandLine.Command(name = "network-avro", description = "Create avro representation of a network.")
@CommandSpec(requireNetwork = true, produces = "network.avro")
public class CreateAvroNetwork implements MATSimAppCommand {

	private static final Set<String> FIELD_RESERVED = Set.of("name", "type", "doc", "default", "aliases");

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(CreateAvroNetwork.class);
	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(CreateAvroNetwork.class);
	@CommandLine.Mixin
	private ShpOptions shp;
	@CommandLine.Mixin
	private CrsOptions crs = new CrsOptions(null, "EPSG:4326");

	@CommandLine.Option(names = "--match-id", description = "Pattern to filter links by id")
	private String matchId;

	@CommandLine.Option(names = "--mode-filter", split = ",", defaultValue = "car,bike,truck,freight,drt",
		description = "Only keep links if they have one of the specified modes. Specify 'none' to disable.")
	private Set<String> modes;

	@CommandLine.Option(names = "--precision", description = "Number of decimals places", defaultValue = "6")
	private int precision;

	@CommandLine.Option(names = "--with-properties", description = "Convert custom node and link attributes as well.")
	private boolean withProperties;

	@CommandLine.Option(names = "--filter-properties", description = "Only include listed properties in the output.")
	private Set<String> filterProperties;

	public static void main(String[] args) {
		new CreateAvroNetwork().execute(args);
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

		// Strings that could have been added in the list, due to command line parsing
		modes.removeIf(m -> m.isBlank() || m.equals("none") || m.equals("\"") || m.equals("\"\""));

		// At least one of the specified modes needs to be contained
		if (!modes.isEmpty()) {
			filter = filter.and(link -> modes.stream().anyMatch(m -> link.getAllowedModes().contains(m)));
		}

		GenericData.Record avro = convert(network, TransformationFactory.getCoordinateTransformation(networkCrs, this.crs.getTargetCRS()), filter);

		avro.put("crs", crs.getTargetCRS());

		write(avro.getSchema(), avro, output.getPath().toFile());

		return 0;
	}

	/**
	 * Converts the given network to a GeoJson representation.
	 *
	 * @param network the network to convert
	 * @param ct      the coordinate transformation to use
	 * @param filter  the filter to apply to the links
	 * @return created record
	 */
	private GenericData.Record convert(Network network, CoordinateTransformation ct, Predicate<Link> filter) {

		// Node attributes
		List<Float> nodeCoords = new ArrayList<>();
		Object2IntMap<CharSequence> nodeIds = new Object2IntLinkedOpenHashMap<>();

		Map<String, AttributeColumn> nodeAttributes = getAttributeColumns(network.getNodes().values());

		for (Node node : network.getNodes().values()) {

			List<Link> relevant = new ArrayList<>();
			relevant.addAll(node.getInLinks().values());
			relevant.addAll(node.getOutLinks().values());

			// Skip nodes without relevant links
			if (relevant.stream().noneMatch(filter))
				continue;

			Coord from = ct.transform(node.getCoord());
			double xCoord = round(from.getX());
			double yCoord = round(from.getY());
			nodeCoords.add((float) xCoord);
			nodeCoords.add((float) yCoord);

			String nodeId = node.getId().toString();
			nodeIds.put(nodeId, nodeIds.size());

			collectAttributes(nodeAttributes, node);
		}

		// Link attributes
		FloatList lengths = new FloatArrayList();
		FloatList freeSpeeds = new FloatArrayList();
		FloatList capacities = new FloatArrayList();
		FloatList permLanes = new FloatArrayList();
		List<CharSequence> ids = new ArrayList<>();
		IntList froms = new IntArrayList();
		IntList tos = new IntArrayList();
		IntList allowedModes = new IntArrayList();

		Map<String, AttributeColumn> linkAttributes = getAttributeColumns(network.getLinks().values());

		Object2IntMap<CharSequence> modeMapping = new Object2IntLinkedOpenHashMap<>();

		for (Link link : network.getLinks().values()) {
			if (!filter.test(link))
				continue;

			lengths.add((float) link.getLength());
			freeSpeeds.add((float) link.getFreespeed());
			capacities.add((float) link.getCapacity());
			permLanes.add((float) link.getNumberOfLanes());

			String m = String.join(",", link.getAllowedModes());
			allowedModes.add(modeMapping.computeIfAbsent(m, k -> modeMapping.size()));

			ids.add(link.getId().toString());
			froms.add(nodeIds.getOrDefault(link.getFromNode().getId().toString(), -1));
			tos.add(nodeIds.getOrDefault(link.getToNode().getId().toString(), -1));

			collectAttributes(linkAttributes, link);
		}

		// Create the Avro record
		Schema baseSchema = AvroNetwork.getClassSchema();

		// Field objects needs to be copied
		List<Schema.Field> fields = baseSchema.getFields()
			.stream()
			.map(f -> new Schema.Field(f.name(), f.schema(), f.doc(), f.defaultVal(), f.order()))
			.collect(Collectors.toList());

		createFields(fields, nodeAttributes, "node_");
		createFields(fields, linkAttributes, "link_");

		// Copy of the original schema
		Schema schema = Schema.createRecord(baseSchema.getName(), baseSchema.getDoc(),
			baseSchema.getNamespace(), baseSchema.isError(), fields);

		GenericData.Record avro = new GenericData.Record(schema);

		// Set the node attributes
		avro.put("nodeId", nodeIds.keySet().stream().toList());
		avro.put("nodeCoordinates", nodeCoords);
		avro.put("nodeAttributes", createAttributeList(nodeAttributes, "nodeId"));
		avro.put("linkAttributes", createAttributeList(linkAttributes,
			"linkId", "length", "freespeed", "capacity", "permlanes", "allowedModes"));

		// Set the mapping which assigns an integer to each possible allowed mode entry
		avro.put("modes", modeMapping.keySet().stream().toList());

		// Set the link attributes
		avro.put("linkId", ids);
		avro.put("length", lengths);
		avro.put("freespeed", freeSpeeds);
		avro.put("capacity", capacities);
		avro.put("permlanes", permLanes);
		avro.put("allowedModes", allowedModes);

		avro.put("from", froms);
		avro.put("to", tos);

		// Put the additional entry columns
		for (Map.Entry<String, AttributeColumn> e : Iterables.concat(nodeAttributes.entrySet(), linkAttributes.entrySet())) {
			avro.put(e.getValue().name.getValue(), e.getValue().values);
		}

		return avro;
	}

	private List<String> createAttributeList(Map<String, AttributeColumn> attr, String... baseAttributes) {
		List<String> result = Arrays.stream(baseAttributes).collect(Collectors.toList());
		attr.values().stream().map(AttributeColumn::name).map(MutableObject::getValue)
			.forEach(result::add);

		return result;
	}

	private <T extends Attributable> Map<String, AttributeColumn> getAttributeColumns(Collection<T> entities) {

		if (!withProperties)
			return Collections.emptyMap();

		Map<String, AttributeColumn> map = new LinkedHashMap<>();

		for (T entity : entities) {
			Attributes attr = entity.getAttributes();
			for (Map.Entry<String, Object> e : attr.getAsMap().entrySet()) {
				String key = e.getKey();
				if (filterProperties != null && !filterProperties.contains(key))
					continue;

				Object value = e.getValue();
				if (value instanceof Number) {
					map.computeIfAbsent(key, k -> new AttributeColumn(new MutableObject<>(k), ColumnType.NUMBER, new ArrayList<>()));
				} else if (value instanceof Boolean) {
					map.computeIfAbsent(key, k -> new AttributeColumn(new MutableObject<>(k), ColumnType.BOOLEAN, new ArrayList<>()));
				} else {
					map.computeIfAbsent(key, k -> new AttributeColumn(new MutableObject<>(k), ColumnType.STRING, new ArrayList<>()));
				}
			}
		}

		return map;
	}

	private void collectAttributes(Map<String, AttributeColumn> attributes, Attributable el) {
		attributes.forEach((key, column) -> {
			Object value = el.getAttributes().getAttribute(key);
			if (column.type == ColumnType.NUMBER && value instanceof Number n)
				value = n.floatValue();
			else if (column.type == ColumnType.BOOLEAN && value instanceof Boolean b)
				value = b;
			else if (column.type == ColumnType.STRING && value != null)
				value = value.toString();
			else if (column.type == ColumnType.NUMBER)
				value = Float.NaN;
			else if (column.type == ColumnType.BOOLEAN)
				value = false;
			else if (column.type == ColumnType.STRING)
				value = "";


			// Null is not allowed for any of these entries
			column.values.add(value);
		});
	}

	/**
	 * Add fields to the schema.
	 */
	private void createFields(List<Schema.Field> fields, Map<String, AttributeColumn> attr, String prefix) {
		for (Map.Entry<String, AttributeColumn> e : attr.entrySet()) {
			MutableObject<String> name = e.getValue().name;

			while (fields.stream().anyMatch(f -> f.name().equals(name.getValue())) || FIELD_RESERVED.contains(name.getValue()))
				name.setValue(prefix + name.getValue());

			String key = name.getValue();
			Schema field = switch (e.getValue().type) {
				case NUMBER -> Schema.createArray(Schema.create(Schema.Type.FLOAT));
				case BOOLEAN -> Schema.createArray(Schema.create(Schema.Type.BOOLEAN));
				case STRING -> Schema.createArray(Schema.create(Schema.Type.STRING));
			};

			fields.add(new Schema.Field(key, field, e.getValue().name.getValue()));
		}
	}

	/**
	 * Write the avro network given as generic data to file output.
	 */
	private void write(Schema schema, GenericData.Record avroNetwork, File output) {
		GenericDatumWriter<Object> datumWriter = new GenericDatumWriter<>();
		try (DataFileWriter<Object> dataFileWriter = new DataFileWriter<>(datumWriter)) {
			dataFileWriter.setCodec(CodecFactory.deflateCodec(9));
			dataFileWriter.create(schema, IOUtils.getOutputStream(IOUtils.getFileUrl(output.toString()), false));
			dataFileWriter.append(avroNetwork);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Round to desired precision.
	 */
	private double round(double x) {
		BigDecimal d = BigDecimal.valueOf(x).setScale(precision, RoundingMode.HALF_UP);
		return d.doubleValue();
	}

	private enum ColumnType {
		NUMBER, STRING, BOOLEAN
	}

	private record AttributeColumn(MutableObject<String> name, ColumnType type, List<Object> values) {
	}

}
