package org.matsim.application.prepare.freight.tripGeneration;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.LanduseOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Type description //TODO
 * // TODO Typo 2021--> 2016
 */
public class DefaultLocationCalculator implements FreightAgentGenerator.LocationCalculator {
    private final static Logger logger = LogManager.getLogger(DefaultLocationCalculator.class);
    private final Random rnd = new Random(5678);
    private final LanduseOptions landUse;
    private final Network network;
    private final Map<String, List<Id<Link>>> mapping = new HashMap<>();
    private final ShpOptions shp;
	private final String externalLookupTablePath; // Used if you want to use another lookupTable
    private static final String lookUpTablePath = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/" +
            "scenarios/countries/de/german-wide-freight/v2/processed-data/complete-lookup-table.csv"; // This one is now fixed

	/**
	 * The {@code DefaultLocationCalcluator} is used to convert between NUTS-regions and links using
	 * {@link DefaultLocationCalculator#getVerkehrszelleOfLink(Id)} and {@link DefaultLocationCalculator#getLocationOnNetwork(String)}.
	 */
    public DefaultLocationCalculator(Network network, Path shpFilePath, LanduseOptions landUse) throws IOException {
        this.shp = new ShpOptions(shpFilePath, "EPSG:4326", StandardCharsets.ISO_8859_1);
        // Reading shapefile from URL may not work properly, therefore users may need to download the shape file to the local directory
        this.landUse = landUse;
        this.network = network;
		this.externalLookupTablePath = "";
        prepareMapping();
    }

	/**
	 * The {@code DefaultLocationCalcluator} is used to convert between NUTS-regions and links using
	 * {@link DefaultLocationCalculator#getVerkehrszelleOfLink(Id)} and {@link DefaultLocationCalculator#getLocationOnNetwork(String)}.
	 * This constructor allows to use external lookup tables.
	 */
	public DefaultLocationCalculator(Network network, Path shpFilePath, String lookupTablePathString, LanduseOptions landUse) throws IOException {
		this.shp = new ShpOptions(shpFilePath, "EPSG:4326", StandardCharsets.ISO_8859_1);
		// Reading shapefile from URL may not work properly, therefore users may need to download the shape file to the local directory
		this.landUse = landUse;
		this.network = network;
		this.externalLookupTablePath = lookupTablePathString;
		prepareMapping();
	}

	/**
	 * Initiates the {@link DefaultLocationCalculator#mapping} attribute which is a {@code Map<String, List<Id<Link>>>} object.
	 * The key is a String of the NUTS-region-ID and the List contains all Links in this NUTS-region.
	 * @throws IOException
	 */
    private void prepareMapping() throws IOException {
		//This is needed to add ability to external lookupTables
		String lookUpTablePath;
		if(Objects.equals(externalLookupTablePath, "")){
			lookUpTablePath = DefaultLocationCalculator.lookUpTablePath;
		} else {
			lookUpTablePath = this.externalLookupTablePath;
		}

        logger.info("Reading NUTS shape files...");
        Set<String> relevantNutsIds = new HashSet<>();
        try (CSVParser parser = CSVParser.parse(URI.create(lookUpTablePath).toURL(), StandardCharsets.UTF_8,
			CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter(';').setHeader().setSkipHeaderRecord(true).build())) {
            for (CSVRecord record : parser) {
                if (!record.get(3).isEmpty()) {
                    relevantNutsIds.add(record.get(3));
                }
            }
        }

		// network CRS: EPSG:25832
		ShpOptions.Index index = shp.createIndex("EPSG:25832", "NUTS_ID",
			ft -> relevantNutsIds.contains(Objects.toString(ft.getAttribute("NUTS_ID"))));

        logger.info("Reading land use data...");
        ShpOptions.Index landIndex = landUse.getIndex("EPSG:25832"); //TODO

        logger.info("Processing shape network and shapefile...");
        List<Link> links = network.getLinks().values().stream().filter(l -> l.getAllowedModes().contains("car"))
                .collect(Collectors.toList());
        Map<String, List<Link>> nutsToLinksMapping = new HashMap<>();
        Map<String, List<Link>> filteredNutsToLinksMapping = new HashMap<>();
        for (Link link : links) {
            String nutsId = index.query(link.getToNode().getCoord());
            if (nutsId != null) {
                nutsToLinksMapping.computeIfAbsent(nutsId, l -> new ArrayList<>()).add(link);
                if (landIndex != null) {
                    if (!landIndex.contains(link.getToNode().getCoord())) {
                        continue;
                    }
                    filteredNutsToLinksMapping.computeIfAbsent(nutsId, l -> new ArrayList<>()).add(link);
                }
            }
        }

        // When filtered links list is not empty, then we use filtered links list. Otherwise, we use the full link lists in the NUTS region.
        for (String nutsId : filteredNutsToLinksMapping.keySet()) {
            nutsToLinksMapping.put(nutsId, filteredNutsToLinksMapping.get(nutsId));
        }
        logger.info("Network and shapefile processing complete!");

        logger.info("Computing mapping between Verkehrszelle and departure location...");
        try (CSVParser parser = CSVParser.parse(URI.create(lookUpTablePath).toURL(), StandardCharsets.UTF_8,
			CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter(';').setHeader().setSkipHeaderRecord(true).build())) {
            for (CSVRecord record : parser) {
                String verkehrszelle = record.get(0);
                String nuts2021 = record.get(3);
                if (!nuts2021.isEmpty() && nutsToLinksMapping.get(nuts2021) != null) {
                    mapping.put(verkehrszelle, nutsToLinksMapping.get(nuts2021).stream().map(Identifiable::getId).collect(Collectors.toList()));
                    continue;
                }
				// Add a backup link from the backup coords in the lookupTable, so that the program can continue to work
                Coord backupCoord = new Coord(Double.parseDouble(record.get(5)), Double.parseDouble(record.get(6)));
                CoordinateTransformation ct = new GeotoolsTransformation("EPSG:4326", "EPSG:25832");
                Coord transformedCoord = ct.transform(backupCoord);
                Link backupLink = NetworkUtils.getNearestLink(network, transformedCoord);
                assert backupLink != null;
                mapping.put(verkehrszelle, List.of(backupLink.getId()));
            }
        }
        logger.info("Location generator is successfully created!");
    }

	/**
	 * @param verkehrszelle The id of a NUTS-region
	 * @return The id of a random link inside the given NUTS-region.
	 */
    @Override
    public Id<Link> getLocationOnNetwork(String verkehrszelle) {
        int size = mapping.get(verkehrszelle).size();
        return mapping.get(verkehrszelle).get(rnd.nextInt(size));
    }

	/**
	 * @param linkId The id of a link.
	 * @return The id of the NUTS region, that the link lies inside.
	 * @throws IllegalArgumentException when a non-existing linkId is given/link is outside every NUTS-region.
	 */
	@Override
	public String getVerkehrszelleOfLink(Id<Link> linkId) {
		for (Map.Entry<String, List<Id<Link>>> entry : mapping.entrySet()) {
			if (entry.getValue().contains(linkId)) {
				return entry.getKey();
			}
		}
		throw new IllegalArgumentException("Link " + linkId + " not found in mapping");
	}
}
