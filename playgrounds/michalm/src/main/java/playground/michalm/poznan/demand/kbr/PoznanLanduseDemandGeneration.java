/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.michalm.poznan.demand.kbr;

import static playground.michalm.poznan.demand.kbr.PoznanLanduseDemandGeneration.ActivityType.*;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.util.random.*;
import org.matsim.contrib.zone.*;
import org.matsim.contrib.zone.util.SubzoneUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.matrices.Matrix;

import com.vividsolutions.jts.geom.*;

import playground.michalm.demand.*;
import playground.michalm.demand.DefaultActivityCreator.*;
import playground.michalm.util.matrices.MatrixUtils;
import playground.michalm.util.visum.VisumMatrixReader;

public class PoznanLanduseDemandGeneration {
	static enum ActivityType {
		HOME, WORK, EDUCATION, SHOPPING, OTHER, NON_HOME;
	}

	static enum ActivityPair {
		D_I(HOME, OTHER), //
		D_N(HOME, EDUCATION), //
		D_P(HOME, WORK), //
		D_Z(HOME, SHOPPING), //
		I_D(OTHER, HOME), //
		N_D(EDUCATION, HOME), //
		P_D(WORK, HOME), //
		Z_D(SHOPPING, HOME), //
		NZD(NON_HOME, NON_HOME);//

		final ActivityType from, to;

		private ActivityPair(ActivityType from, ActivityType to) {
			this.from = from;
			this.to = to;
		}
	}

	private static class ZoneLanduseValidation {
		private final boolean residential;
		private final boolean industrial;

		private ZoneLanduseValidation(boolean residential, boolean industrial) {
			this.residential = residential;
			this.industrial = industrial;
		}
	}

	private static final double PEOPLE_PER_VEHICLE = 1.2;
	private static final double RESIDENTIAL_TO_INDUSTRIAL_WORK = 0.33;
	private static final double RESIDENTIAL_TO_SHOP_SHOPPING = 0.1;

	public class LanduseLocationGeneratorStrategy implements GeometryProvider, PointAcceptor {
		@Override
		public Geometry getGeometry(Zone zone, String actType) {
			ActivityType activityType = ActivityType.valueOf(actType);

			if (isActivityConstrained(activityType)) {
				Polygon polygon = selectionTable.select(zone.getId(), activityType);

				if (polygon != null) {
					return polygon; // randomly selected subzone
				}
			}

			return zone.getMultiPolygon(); // whole zone
		}

		@Override
		public boolean acceptPoint(Zone zone, String actType, Point point) {
			ActivityType activityType = ActivityType.valueOf(actType);

			if (isActivityConstrained(activityType)) {
				if (selectionTable.contains(zone.getId(), activityType)) {
					return true;
				}

				for (Polygon p : forestByZone.get(zone.getId())) {
					if (p.contains(point)) {
						return false; // inside forest
					}
				}
			}

			return true;
		}
	}

	private Scenario scenario;
	private Map<Id<Zone>, Zone> zones;
	private Map<Id<Zone>, ZoneLanduseValidation> zoneLanduseValidation;
	private EnumMap<ActivityPair, Double> prtCoeffs;

	private String transportMode;

	private Map<Id<Zone>, List<Polygon>> forestByZone;
	private Map<Id<Zone>, List<Polygon>> industrialByZone;
	private Map<Id<Zone>, List<Polygon>> residentialByZone;
	private Map<Id<Zone>, List<Polygon>> schoolByZone;
	private Map<Id<Zone>, List<Polygon>> shopByZone;

	private ODDemandGenerator dg;

	private final EnumSet<ActivityType> constrainedActivities = EnumSet.of(HOME, WORK, EDUCATION, SHOPPING);

	private WeightedRandomSelectionTable<Id<Zone>, ActivityType, Polygon> selectionTable;

	public void generate(String inputDir, String plansFile, String transportMode) {
		this.transportMode = transportMode;

		String networkFile = inputDir + "Matsim_2013_06/network-cleaned-extra-lanes.xml";
		String zonesXmlFile = inputDir + "Matsim_2013_06/zones.xml";
		String zonesShpFile = inputDir + "Osm_2013_06/zones.SHP";

		String forestShpFile = inputDir + "Osm_2013_06/forests.SHP";
		String industrialShpFile = inputDir + "Osm_2013_06/industrial.SHP";
		String residentialShpFile = inputDir + "Osm_2013_06/residential.SHP";
		String schoolShpFile = inputDir + "Osm_2013_06/school.SHP";
		String shopShpFile = inputDir + "Osm_2013_06/shop.SHP";

		String zonesWithLanduseFile = inputDir + "Osm_2013_06/zones_with_correct_landuse";

		String put2PrtRatiosFile = inputDir + "Visum_2012/PuT_PrT_ratios";
		String odMatrixFilePrefix = inputDir + "Visum_2012/odMatricesByType/";

		int randomSeed = RandomUtils.DEFAULT_SEED;
		RandomUtils.reset(randomSeed);

		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		zones = Zones.readZones(zonesXmlFile, zonesShpFile);

		readValidatedZones(zonesWithLanduseFile);

		forestByZone = getLandusePolygonsByZone(forestShpFile);
		industrialByZone = getLandusePolygonsByZone(industrialShpFile);
		residentialByZone = getLandusePolygonsByZone(residentialShpFile);
		schoolByZone = getLandusePolygonsByZone(schoolShpFile);
		shopByZone = getLandusePolygonsByZone(shopShpFile);

		initSelection();

		prtCoeffs = readPrtCoeffs(put2PrtRatiosFile);

		generateAll(odMatrixFilePrefix);

		dg.write(plansFile);
		// dg.writeTaxiCustomers(taxiFile);
	}

	private void readValidatedZones(String validatedZonesFile) {
		zoneLanduseValidation = new HashMap<>();

		try (Scanner scanner = new Scanner(new File(validatedZonesFile))) {
			scanner.nextLine();// skip the header

			while (scanner.hasNext()) {
				Id<Zone> zoneId = Id.create(scanner.next(), Zone.class);
				boolean residential = scanner.nextInt() != 0;
				boolean industrial = scanner.nextInt() != 0;

				zoneLanduseValidation.put(zoneId, new ZoneLanduseValidation(residential, industrial));
			}

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<Id<Zone>, List<Polygon>> getLandusePolygonsByZone(String shpFile) {
		System.out.println("getLanduseByZone() for: " + shpFile);
		return SubzoneUtils.extractSubzonePolygons(zones, ShapeFileReader.getAllFeatures(shpFile));
	}

	private void initSelection() {
		selectionTable = WeightedRandomSelectionTable.createWithArrayTable(zoneLanduseValidation.keySet(),
				constrainedActivities);

		for (Entry<Id<Zone>, ZoneLanduseValidation> e : zoneLanduseValidation.entrySet()) {
			Id<Zone> zoneId = e.getKey();
			ZoneLanduseValidation validation = e.getValue();
			Zone zone = zones.get(zoneId);

			List<Polygon> industrialPolygons = validation.industrial ? //
					industrialByZone.get(zoneId) : //
					Zones.getPolygons(zone);

			for (Polygon p : industrialPolygons) {
				selectionTable.add(zoneId, WORK, p, p.getArea());
			}

			Iterable<Polygon> residentialPolygons = validation.residential ? //
					residentialByZone.get(zoneId) : //
					Zones.getPolygons(zone);

			for (Polygon p : residentialPolygons) {
				double area = p.getArea();
				selectionTable.add(zoneId, HOME, p, area);
				selectionTable.add(zoneId, WORK, p, RESIDENTIAL_TO_INDUSTRIAL_WORK * area);
				selectionTable.add(zoneId, SHOPPING, p, RESIDENTIAL_TO_SHOP_SHOPPING * area);
			}

			for (Polygon p : schoolByZone.get(zoneId)) {
				selectionTable.add(zoneId, EDUCATION, p, p.getArea());
			}

			for (Polygon p : shopByZone.get(zoneId)) {
				selectionTable.add(zoneId, SHOPPING, p, p.getArea());
			}
		}
	}

	static EnumMap<ActivityPair, Double> readPrtCoeffs(String put2PrtRatiosFile) {
		EnumMap<ActivityPair, Double> prtCoeffs = new EnumMap<>(ActivityPair.class);

		try (Scanner scanner = new Scanner(new File(put2PrtRatiosFile))) {
			while (scanner.hasNext()) {
				ActivityPair pair = ActivityPair.valueOf(scanner.next());
				double putShare = scanner.nextDouble();
				double prtShare = scanner.nextDouble();
				double coeff = prtShare / (putShare + prtShare) / PEOPLE_PER_VEHICLE;

				if (prtCoeffs.put(pair, coeff) != null) {
					scanner.close();
					throw new RuntimeException("Pair respecified: " + pair);
				}
			}

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		if (prtCoeffs.size() != ActivityPair.values().length) {
			throw new RuntimeException("Not all pairs have been specified");
		}

		return prtCoeffs;
	}

	private void generateAll(String odMatrixFilePrefix) {
		LanduseLocationGeneratorStrategy strategy = new LanduseLocationGeneratorStrategy();
		ActivityCreator ac = new DefaultActivityCreator(scenario, strategy, strategy);
		PersonCreator pc = new DefaultPersonCreator(scenario);
		dg = new ODDemandGenerator(scenario, zones, false, ac, pc);

		for (ActivityPair ap : ActivityPair.values()) {
			generate(odMatrixFilePrefix, ap);
		}
	}

	private void generate(String odMatrixFilePrefix, ActivityPair actPair) {
		double flowCoeff = prtCoeffs.get(actPair);
		String actTypeFrom = actPair.from.name();
		String actTypeTo = actPair.to.name();

		for (int i = 0; i < 24; i++) {
			String odMatrixFile = odMatrixFilePrefix + actPair.name() + "_" + i + "-" + (i + 1) + ".gz";
			System.out.println("Generation for " + odMatrixFile);

			double[][] visumODMatrix = VisumMatrixReader.readMatrix(odMatrixFile);
			Matrix odMatrix = MatrixUtils.createSparseMatrix("m" + i, zones.keySet(), visumODMatrix);
			dg.generateSinglePeriod(odMatrix, actTypeFrom, actTypeTo, transportMode, i * 3600, 3600, flowCoeff);
		}
	}

	private boolean isActivityConstrained(ActivityType activityType) {
		return constrainedActivities.contains(activityType);
	}

	public static void main(String[] args) {
		String inputDir = "d:/GoogleDrive/Poznan/";
		String plansFile = "d:/PP-rad/poznan/test/plans.xml.gz";
		String transportMode = TransportMode.car;
		new PoznanLanduseDemandGeneration().generate(inputDir, plansFile, transportMode);
	}
}
