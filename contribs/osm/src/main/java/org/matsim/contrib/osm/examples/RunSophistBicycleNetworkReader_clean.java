package org.matsim.contrib.osm.examples;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.osm.networkReader.OsmBicycleReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.utils.collections.Tuple;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.BiConsumer;

public class RunSophistBicycleNetworkReader_clean {

	private static final String inputFile = "C://Users/metz_so/Workspace/data/neukoelln.osm.pbf";
	//    private static final String inputFile = "C://Users/metz_so/IdeaProjects/data/berlin-260122.osm.pbf";
	private static final String outputFile = "C://Users/metz_so/Workspace/data/matsim-network_nk_bicycle_custom_simp.xml.gz";
	//private static final String outputFile = "C://Users/metz_so/IdeaProjects/data/matsim-network_berlin_bike_rules.xml.gz";

	private static final CoordinateTransformation ct =
		TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");

	private static final List<String> TAGS_TO_COPY = List.of(
//		"bicycle",
//		"cycleway",
//		"cycleway:left",
//		"cycleway:right",
//		"cycleway:both",
//		"surface",
//		"smoothness",
//		"oneway",
//		"oneway:bicycle"
	);

	// Option A: wir erzeugen erst "bike" und benennen danach auf "bicycle" um
	private static final String FROM_MODE = TransportMode.bike; // "bike"
	private static final String TO_MODE = "bicycle";

	public static void main(String[] args) {

		var classifier = new BicycleInfraClassifier();
		var tagCopy = new TagCopy(TAGS_TO_COPY, "osm:");
		var policy = new BicycleLinkPolicy(classifier, tagCopy);

		Network network = new OsmBicycleReader.Builder()
			.setCoordinateTransformation(ct)
			.setAfterLinkCreated(policy::apply)
			.build()
			.read(inputFile);

//		// 1) Mode umbenennen (allowedModes + Restriction-Attribut-Key)
//		renameMode(network, FROM_MODE, TO_MODE);
//
//		// 2) optional: simplify/clean
//		NetworkUtils.simplifyNetwork(network);
//		NetworkUtils.cleanNetwork(network, Set.of(TO_MODE)); // oder Set.of(FROM_MODE) wenn du nicht umbenennst

		NetworkUtils.cleanNetwork(network, Set.of(FROM_MODE)); // "bike"

		simplifyWithBikeInfra(network);

		//NetworkUtils.simplifyNetwork(network);
		renameMode(network, FROM_MODE, TO_MODE);
//		NetworkUtils.cleanNetwork(network, Set.of(TO_MODE));   // optional
//		NetworkUtils.simplifyNetwork(network);                // optional, wenn du ganz sicher sein willst

		new NetworkWriter(network).write(outputFile);
	}

	static void renameMode(Network network, String from, String to) {
		for (var link : network.getLinks().values()) {

			// allowedModes: bike -> bicycle
			var modes = new HashSet<>(link.getAllowedModes());
			if (modes.remove(from)) {
				modes.add(to);
				link.setAllowedModes(modes);
			}

			// Attribute-Key: "bike" -> "bicycle" (OsmBicycleReader setzt bicycle-restriction unter key="bike")
			Object val = link.getAttributes().getAttribute(from);
			if (val != null) {
				link.getAttributes().putAttribute(to, val);
				link.getAttributes().removeAttribute(from);
			}
		}
	}


	///  why wee need this?
	/// the default simplyier ignors the additional bicylce attributes
	private static final String BICYCLE_INFRA = "bicycle_infra";
	private static final String SURFACE = "surface";
	private static final String TYPE = "type";
	private static final String BICYCLE = "bicycle";
	private static final String SMOOTHNESS = "smoothness";

	static void simplifyWithBikeInfra(Network network) {

		BiPredicate<Link, Link> attrsMustMatch = (a, b) ->
			Objects.equals(a.getAttributes().getAttribute(BICYCLE_INFRA), b.getAttributes().getAttribute(BICYCLE_INFRA))
				&& Objects.equals(a.getAttributes().getAttribute(TYPE), b.getAttributes().getAttribute(TYPE))
				&& Objects.equals(a.getAttributes().getAttribute(SURFACE), b.getAttributes().getAttribute(SURFACE))
				&& Objects.equals(a.getAttributes().getAttribute(BICYCLE), b.getAttributes().getAttribute(BICYCLE))
				&& Objects.equals(a.getAttributes().getAttribute(SMOOTHNESS), b.getAttributes().getAttribute(SMOOTHNESS));

		var simplifier = NetworkSimplifier.createNetworkSimplifier(network);
		simplifier.setMergeLinkStats(false);

		// 1) Merge-Regel
		simplifier.registerIsMergeablePredicate(attrsMustMatch);

		// 2) Attribute auf neuen Link übertragen (da sie gleich sind, reicht von einem)
		simplifier.registerTransferAttributesConsumer((inOut, newLink) -> {
			Link in = inOut.getFirst();

			copyIfPresent(in, newLink, BICYCLE_INFRA);
			copyIfPresent(in, newLink, TYPE);
			copyIfPresent(in, newLink, SURFACE);
			copyIfPresent(in, newLink, BICYCLE);
			copyIfPresent(in, newLink, SMOOTHNESS);
		});

		simplifier.run(network);
	}

	private static void copyIfPresent(Link from, Link to, String key) {
		Object v = from.getAttributes().getAttribute(key);
		if (v != null) to.getAttributes().putAttribute(key, v);
	}


//	private static final String BICYCLE_INFRA = "bicycle_infra";
//
//	static void simplifyWithBikeInfra(Network network) {
//
//		BiPredicate<Link, Link> bikeInfraMustMatch = (inLink, outLink) -> {
//			Object a = inLink.getAttributes().getAttribute(BICYCLE_INFRA);
//			Object b = outLink.getAttributes().getAttribute(BICYCLE_INFRA);
//			return Objects.equals(a, b); // nur merge wenn gleich (inkl. beide null)
//		};
//
//		BiConsumer<Tuple<Link, Link>, Link> transferBikeInfra = (inOut, newLink) -> {
//			Object v = inOut.getFirst().getAttributes().getAttribute(BICYCLE_INFRA);
//			if (v != null) {
//				newLink.getAttributes().putAttribute(BICYCLE_INFRA, v);
//			} else {
//				// optional: explizit entfernen
//				// newLink.getAttributes().removeAttribute(BICYCLE_INFRA);
//			}
//		};
//
//		NetworkSimplifier simplifier = NetworkSimplifier.createNetworkSimplifier(network);
//
//		// Wichtig: false lassen, sonst merged er auch bei unterschiedlichen Standard-Stats
//		simplifier.setMergeLinkStats(false);
//
//		// Deine Regeln registrieren
//		simplifier.registerIsMergeablePredicate(bikeInfraMustMatch);
//		simplifier.registerTransferAttributesConsumer(transferBikeInfra);
//
//		// und dann normal laufen lassen
//		simplifier.run(network);
//	}


}
