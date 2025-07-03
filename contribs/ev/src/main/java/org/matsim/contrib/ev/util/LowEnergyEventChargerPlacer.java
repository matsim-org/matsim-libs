package org.matsim.contrib.ev.util;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.infrastructure.*;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class LowEnergyEventChargerPlacer {
    public static void main(int designIteration, int[] plugs) throws IOException {

        // ------------------ CONFIGURABLE PARAMETERS ------------------
        int allowedNewChargers = 1;                     // Number of new chargers to place per iteration
        int plugPower = 1000;                           // Power per plug in kW
        double speedLimit = KM_PER_H_TO_M_PER_S(70.0); 		// Speed limit for links where to allow charging station placement
        String crsSim = "EPSG:NUMBER_CRS_SIMULATION";                    // CRS used in simulation
        String crsGeo = "EPSG:NUMBER_CRS_SHAPEFILE";                    // Geographic CRS for shape interpretation
        String networkInputPath = "output/TruckEvExample/ITER0/output_network.xml.gz"; // Input network file
        String zonesShapefile = "path/to/shapefile"; // Shapefile for area aggregation
        String clcShapefile = "path/to/clc_shapefile"; // Shapefile for Corine Land Cover, can be removed
        String scenarioBasePath = "contribs/ev/test/input/org/matsim/contrib/ev/TruckEvexample/RunEvExample";
        //----------------------------------------------------------------
        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(crsSim, crsGeo);

        // Load MATSim network
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkInputPath);

        // Filter network: trucks only and min speed >= 70 km/h
        NetworkConfigGroup networkCfg = new NetworkConfigGroup();
        networkCfg.setInputFile(networkInputPath);
        networkCfg.setTimeVariantNetwork(false);
        NetworkFilterManager nfm = new NetworkFilterManager(network, networkCfg);
        nfm.addLinkFilter(l -> l.getAllowedModes().contains(TransportMode.truck)
                && l.getFreespeed() >= speedLimit);
        Network filteredNet = nfm.applyFilters();

        // Load zones and CLC data
        Collection<SimpleFeature> clcFeatures = ShapeFileReader.getAllFeatures(clcShapefile); // Can be commented out if no CLC
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(zonesShapefile);

        for (int plugCount : plugs) {
            String specificScenario = plugCount + "PLUGS";
            boolean firstIteration = designIteration == 1;

			// Load previous chargers if applicable
            ChargingInfrastructureSpecification chargersOld = ChargingInfrastructureUtils.createChargingInfrastructureSpecification();
            List<Coord> oldChargerCoords = new ArrayList<>();
            if (!firstIteration) {
                new ChargerReader(chargersOld).readFile(scenarioBasePath + "/ITER" + (designIteration - 1) + "/ITER" + (designIteration - 1) + "_" + specificScenario + ".xml.gz");
                for (ChargerSpecification spec : chargersOld.getChargerSpecifications().values()) {
                    oldChargerCoords.add(network.getLinks().get(spec.getLinkId()).getCoord());
                }
            }

			// Map used zones to avoid duplicating chargers
			HashMap<Geometry, Coord> oldZones = new HashMap<>();
            features.forEach(f -> {
                Geometry geom = (Geometry) f.getDefaultGeometry();
                oldChargerCoords.forEach(coord -> {
                    if (geom.contains(MGC.coord2Point(coord))) {
                        oldZones.put(geom, coord);
                    }
                });
            });

			// Read LowEnergyEvent file and extract coordinates
			File inputFile = new File("output/TruckEvExample/ITER" + (designIteration - 1) + "/lowEnergyEvents.txt");
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            String[] headers = br.readLine().split("\t");
            int iXCoord = Arrays.asList(headers).indexOf("xCoord");
            int iYCoord = Arrays.asList(headers).indexOf("yCoord");

            List<Coord> LEECoordList = br.lines().map(line -> {
                String[] strArray = line.split("\t");
                return new Coord(Double.parseDouble(strArray[iXCoord]), Double.parseDouble(strArray[iYCoord]));
            }).toList();

			// Track zone-level LEE aggregation
			HashMap<String, List<Coord>> LEEperZone = new HashMap<>();
            HashMap<Geometry, Integer> oldCoordsZonesLEEs = new HashMap<>();
            features.forEach(f -> {
                Geometry geometry = (Geometry) f.getDefaultGeometry();
                if (!firstIteration) {
                    oldChargerCoords.forEach(coord -> {
                        if (geometry.contains(MGC.coord2Point(coord))) {
                            oldCoordsZonesLEEs.put(geometry, 0);
                        }
                    });
                }
            });

            for (Coord coord : LEECoordList) {
                features.forEach(f -> {
                    Geometry geometry = (Geometry) f.getDefaultGeometry();
                    if (!oldZones.containsKey(geometry) && geometry.contains(MGC.coord2Point(coord))) {
                        if (!firstIteration && oldCoordsZonesLEEs.containsKey(geometry)) {
                            oldCoordsZonesLEEs.computeIfPresent(geometry, (k, v) -> v + 1);
                        } else {
                            LEEperZone.computeIfAbsent(f.getID(), k -> new ArrayList<>()).add(coord);
                        }
                    }
                });
            }

			// Determine average location per zone
            HashMap<Coord, Integer> nrMissingPerArea = new HashMap<>();
            LEEperZone.forEach((zoneId, coords) -> {
                double avgX = coords.stream().mapToDouble(Coord::getX).average().orElse(0.0);
                double avgY = coords.stream().mapToDouble(Coord::getY).average().orElse(0.0);
                Coord uniqueCoord = new Coord(avgX, avgY);
                nrMissingPerArea.put(uniqueCoord, coords.size());
            });

			// Sort zones by LEE count
			Map<Id<Charger>, ChargerSpecification> chargers = new HashMap<>();
            Map<Coord, Integer> sorted = nrMissingPerArea.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(allowedNewChargers)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

			// This can be removed if no CLC
            ArrayList<String> clc_info = new ArrayList<>();
            GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
            int k = 1;
            for (Map.Entry<Coord, Integer> entry : sorted.entrySet()) {
                Link l = NetworkUtils.getNearestLink(filteredNet, entry.getKey());
                shapeFactory.setNumPoints(64);
				assert l != null;
				shapeFactory.setCentre(new Coordinate(l.getCoord().getX(), l.getCoord().getY()));
                shapeFactory.setWidth(2000);
                Polygon circle = shapeFactory.createCircle();

                Arrays.stream(circle.getCoordinates()).parallel().forEach(c -> {
                    List<SimpleFeature> match = clcFeatures.stream().filter(f -> {
                        Geometry geom = (Geometry) f.getDefaultGeometry();
                        return geom.contains(MGC.coord2Point(ct.transform(MGC.coordinate2Coord(c))));
                    }).toList();
                    match.forEach(f -> clc_info.add(f.getAttribute("Code_18").toString()));
                });

                Id<Charger> chargerID = Id.create("ITER" + designIteration + "_Charger_max" + plugCount + "_" + k++, Charger.class);
                chargers.put(chargerID, ImmutableChargerSpecification.newBuilder()
                        .id(chargerID)
                        .plugPower(plugPower * EvUnits.W_PER_kW)
                        .plugCount(plugCount)
                        .linkId(l.getId())
                        .chargerType("truck")
                        .build());
            }

            if (!firstIteration) {
                chargersOld.getChargerSpecifications().forEach((id, spec) -> {
                    chargers.put(id, ImmutableChargerSpecification.newBuilder()
                            .id(id)
                            .linkId(spec.getLinkId())
                            .chargerType(spec.getChargerType())
                            .plugCount(plugCount)
                            .plugPower(plugPower * EvUnits.W_PER_kW).build());
                });
            }

            new File(scenarioBasePath + "/ITER" + designIteration).mkdirs();
            new ChargerWriter(chargers.values().stream()).write(scenarioBasePath + "/ITER" + designIteration + "/ITER" + designIteration + "_" + specificScenario + ".xml.gz");

			// This can be removed if no CLC
            BufferedWriter bw = new BufferedWriter(new FileWriter(scenarioBasePath + "/clc_info_iter" + designIteration + "_" + plugCount + "plugs.txt"));
            bw.write("clc_code\n");
            for (String s : clc_info) {
                bw.write(s + "\n");
            }
            bw.close();

            System.out.println("Chargers have been placed in " + chargers.size() + " zones.");
            System.out.println("Total installed power: " + chargers.size() * plugCount * plugPower + " kW");
        }
    }
	public static Double KM_PER_H_TO_M_PER_S(Double KM_PER_H){
		return KM_PER_H/3.6;
	}
}
