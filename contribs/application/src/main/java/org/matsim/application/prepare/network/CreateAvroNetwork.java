package org.matsim.application.prepare.network;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
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
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;


@CommandLine.Command(name = "network-avro", description = "Create avro representation of a network.")
@CommandSpec(requireNetwork = true, produces = "network.avro")
public class CreateAvroNetwork implements MATSimAppCommand {
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


		AvroNetwork avro = new AvroNetwork();
		avro.setCrs(networkCrs);

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

//		AvroNetwork.getClassSchema().addProp();

		convert(avro, network, TransformationFactory.getCoordinateTransformation(networkCrs, this.crs.getTargetCRS()), filter);

		writeAvro(avro, output.getPath().toFile());

		return 0;
	}

	/**
	 * Converts the given network to a GeoJson representation.
	 *
	 * @param avro    the AvroNetwork to write to
	 * @param network the network to convert
	 * @param ct      the coordinate transformation to use
	 * @param filter  the filter to apply to the links
	 */
	private void convert(AvroNetwork avro, Network network, CoordinateTransformation ct, Predicate<Link> filter) {

		// Node attributes
		List<Float> nodeCoords = new ArrayList<>();
		Object2IntMap<CharSequence> nodeIds = new Object2IntLinkedOpenHashMap<>();
		Set<CharSequence> nodeAttributes = new LinkedHashSet<>();

		for (Node node : network.getNodes().values()) {
			Coord from = ct.transform(node.getCoord());
			double xCoord = round(from.getX());
			double yCoord = round(from.getY());
			nodeCoords.add((float) xCoord);
			nodeCoords.add((float) yCoord);

			String nodeId = node.getId().toString();
			nodeIds.put(nodeId, nodeIds.size());
			nodeAttributes.addAll(node.getAttributes().getAsMap().keySet());
		}

		// Set the node attributes
		avro.setNodeCoordinates(nodeCoords);
		avro.setNodeIds(nodeIds.keySet().stream().toList());
		avro.setNodeAttributes(nodeAttributes.stream().toList());


		// Link attributes
		FloatList lengths = new FloatArrayList();
		FloatList freeSpeeds = new FloatArrayList();
		FloatList capacities = new FloatArrayList();
		FloatList permLanes = new FloatArrayList();
		List<CharSequence> ids = new ArrayList<>();
		IntList froms = new IntArrayList();
		IntList tos = new IntArrayList();
		IntList allowedModes = new IntArrayList();

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
		}

		// Set the mapping which assigns an integer to each possible allowed mode entry
		avro.setModes(modeMapping.keySet().stream().toList());

		// Set the link attributes
		avro.setLength(lengths);
		avro.setFreespeed(freeSpeeds);
		avro.setCapacity(capacities);
		avro.setPermlanes(permLanes);
		avro.setAllowedModes(allowedModes);

		avro.setLinkIds(ids);
		avro.setFrom(froms);
		avro.setTo(tos);
	}

	/**
	 * Writes the given data to the given file.
	 *
	 * @param avroNetwork the data to write
	 * @param output      the file to write to
	 */
	private void writeAvro(AvroNetwork avroNetwork, File output) {
		DatumWriter<AvroNetwork> datumWriter = new SpecificDatumWriter<>(AvroNetwork.class);
		try (DataFileWriter<AvroNetwork> dataFileWriter = new DataFileWriter<>(datumWriter)) {
			dataFileWriter.setCodec(CodecFactory.deflateCodec(9));
			dataFileWriter.create(avroNetwork.getSchema(), IOUtils.getOutputStream(IOUtils.getFileUrl(output.toString()), false));
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

}
