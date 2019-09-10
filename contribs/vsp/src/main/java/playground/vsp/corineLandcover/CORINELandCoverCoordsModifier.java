/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.vsp.corineLandcover;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.vsp.openberlinscenario.cemdap.output.Cemdap2MatsimUtils;
import playground.vsp.openberlinscenario.cemdap.output.CemdapOutput2MatsimPlansConverter;

/**
 * Created by amit on 24.10.17.
 * <li>
 * - takes matsim plans, CORINE shape file and zonal shape file as input
 * </li>
 * <li>
 * - check for every activity location, if they are inside the correct zone based on CORINE landcover activity type (home/other)
 * </li>
 * - if not, reassign the coordinate.
 * </li>
 *
 * <p>
 * There are some assumptions about the activity types:
 * <li>
 * -first activity is always home
 * </li>
 * <li>
 * -all home activities are located at the same coord if sameHomeActivity is set to true.
 * </li>
 * </p>
 */

public class CORINELandCoverCoordsModifier {

    private static final Logger LOG = Logger.getLogger(CORINELandCoverCoordsModifier.class);
    private static final Coord fakeCoord = new Coord(-1, -1);

    private final CorineLandCoverData corineLandCoverData;
    private final Population population;

    private final Map<String, Geometry> zoneFeatures = new HashMap<>();

    private final boolean sameHomeActivity;
    private final String homeActivityPrefix;

    public CORINELandCoverCoordsModifier(String matsimPlans, Map<String, String> shapeFileToFeatureKey, String CORINELandCoverFile,
                                         boolean simplifyGeoms, boolean combiningGeoms, boolean sameHomeActivity, String homeActivityPrefix) {
        this.corineLandCoverData = new CorineLandCoverData(CORINELandCoverFile, simplifyGeoms, combiningGeoms);
        LOG.info("Loading population from plans file " + matsimPlans);
        this.population = getPopulation(matsimPlans);

        for (String shapeFile : shapeFileToFeatureKey.keySet()) {
            String key = shapeFileToFeatureKey.get(shapeFile);
            LOG.info("Processing zone file " + shapeFile + " with feature key " + key);
            Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
            for (SimpleFeature feature : features) {
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                String shapeId = Cemdap2MatsimUtils.removeLeadingZeroFromString((String) feature.getAttribute(key));

                if (zoneFeatures.get(shapeId) != null) { // union geoms corresponding to same zone id.
                    zoneFeatures.put(shapeId,
                            GeometryUtils.combine(Arrays.asList(geometry, zoneFeatures.get(shapeId))));
                } else {
                    zoneFeatures.put(shapeId, geometry);
                }
            }
        }

        this.sameHomeActivity = sameHomeActivity;
        if (this.sameHomeActivity) LOG.info("Home activities for a person will be at the same location.");
        
        this.homeActivityPrefix = homeActivityPrefix;
    }

    public static void main(String[] args) {

        String corineLandCoverFile = "/Users/amit/Documents/gitlab/mercator-nemo/data/original_files/shapeFiles/CORINE_landcover_nrw/corine_nrw_src_clc12.shp"; //<- e.g. Berlin brandenburg file, NRW
        String zoneFile = "/Users/amit/Documents/gitlab/mercator-nemo/data/original_files/shapeFiles/sourceShape_NRW/dvg2gem_nw.shp";
        String zoneIdTag = "KN";

        String spatialRefinementShapeFile = "/Users/amit/Documents/gitlab/mercator-nemo/data/original_files/shapeFiles/plzBasedPopulation/plz-gebiete_Ruhrgebiet/plz-gebiete_Ruhrgebiet_withPopulation.shp";
        String featureKeySpatialRefinement = "plz";

        String matsimPlans = "/Users/amit/Documents/gitlab/mercator-nemo/data/matsim_input/zz_archive/plans/2018_jan_24/mergedPlansFiles/plans_1pct_fullChoiceSet.xml.gz";
        String outPlans = "/Users/amit/Documents/gitlab/mercator-nemo/data/matsim_input/zz_archive/plans/2018_jan_24/mergedPlansFiles/plans_1pct_fullChoiceSet_coordsAssigned.xml.gz";

        boolean simplifyGeom = true;
        boolean combiningGeoms = false;
        boolean sameHomeActivity = true;
        String homeActivityPrefix = "home";

        if (args.length > 0) {
            corineLandCoverFile = args[0];
            zoneFile = args[1];
            zoneIdTag = args[2];
            spatialRefinementShapeFile = args[3];
            featureKeySpatialRefinement = args[4];
            matsimPlans = args[5];
            simplifyGeom = Boolean.valueOf(args[6]);
            combiningGeoms = Boolean.valueOf(args[7]);
            sameHomeActivity = Boolean.valueOf(args[8]);
            homeActivityPrefix = args[9];
            outPlans = args[10];
        }

        Map<String, String> shapeFileToFeatureKey = new HashMap<>();
        shapeFileToFeatureKey.put(zoneFile, zoneIdTag);
        shapeFileToFeatureKey.put(spatialRefinementShapeFile, featureKeySpatialRefinement);

        CORINELandCoverCoordsModifier plansFilterForCORINELandCover = new CORINELandCoverCoordsModifier(matsimPlans,
                shapeFileToFeatureKey,
                corineLandCoverFile,
                simplifyGeom,
                combiningGeoms,
                sameHomeActivity,
                homeActivityPrefix);
        plansFilterForCORINELandCover.process();
        plansFilterForCORINELandCover.writePlans(outPlans);
    }

    public void process() {
        LOG.info("Start processing, this may take a while ... ");
                
        int personCounter = 0;
        for (Person person : population.getPersons().values()) {
            Coord homeLocationCoord = null;
            String homeActivityName = null; // home or h or home_1 or home_2 etc

            if (personCounter%1000 == 0) {
            		LOG.info("Person #" + personCounter);
            }
            
            for (Plan plan : person.getPlans()) {
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {

                        Activity activity = (Activity) planElement;
                        Coord coord = activity.getCoord();
//                        String activityType = activity.getType().split("_")[0]; // could be home_10587 or home_1h
                        String activityType = activity.getType(); // could be home or home_1h

                        // during matsim plans generation, for home activities following fake coordinate is assigned.
                        if ( coord !=null && coord.equals(fakeCoord) ) {
                            String zoneId = (String) activity.getAttributes().getAttribute(
                                    CemdapOutput2MatsimPlansConverter.activityZoneId_attributeKey);

                            if (homeLocationCoord==null) {
                                if ( activityType.startsWith(this.homeActivityPrefix) ) {
                                    coord = getRandomCoord(LandCoverUtils.LandCoverActivityType.home, zoneId);

                                    homeLocationCoord = coord;
                                    homeActivityName = activityType;
                                } else {
                                    Log.warn("First activity is not a home activity...");
                                    coord = getRandomCoord(LandCoverUtils.LandCoverActivityType.other, zoneId);
                                }
                            } else if (activityType.equals(homeActivityName) && sameHomeActivity) {
                                // same home activity, just take the stored coord
                                coord = homeLocationCoord;

                            } else {
                                coord = getRandomCoord(LandCoverUtils.LandCoverActivityType.other, zoneId);
                            }

                            activity.setCoord(coord);
                            activity.setType(activityType);
                        }
                        else {
                            // a regular coord --> check and reassign coord if required.
                            if (homeLocationCoord==null) {
                                Point point = MGC.coord2Point(coord);
                                if (activityType.startsWith(this.homeActivityPrefix)) {
                                    if (! corineLandCoverData.isPointInsideLandCover(LandCoverUtils.LandCoverActivityType.home, point) ){
                                        coord = reassignCoord(point, LandCoverUtils.LandCoverActivityType.home);
                                    }
                                    homeLocationCoord = coord;
                                    homeActivityName = activityType;

                                } else {
                                    if (! corineLandCoverData.isPointInsideLandCover(LandCoverUtils.LandCoverActivityType.other, point) ){
                                        coord = reassignCoord(point, LandCoverUtils.LandCoverActivityType.other);
                                    }
                                }
                            } else if ( activityType.equals(homeActivityName) && sameHomeActivity) {
                                // same home activity, just take the stored coord
                                coord = homeLocationCoord;
                            } else {
                                Point point = MGC.coord2Point(coord);
                                if (! corineLandCoverData.isPointInsideLandCover(LandCoverUtils.LandCoverActivityType.other, point) ){
                                    coord = reassignCoord(point, LandCoverUtils.LandCoverActivityType.other);
                                }
                                activity.setCoord(coord);
                            }
                            activity.setCoord(coord);
                        }
                    }
                }
            }
            personCounter++;
        }
        LOG.info("Finished processing.");
    }

    public void writePlans(String outFile) {
        LOG.info("Writing resulting plans to " + outFile);
        new PopulationWriter(population).write(outFile);
    }

    /**
     * @param activityType
     * @param zoneId
     * @return a random coord if there was no coordinate assigned to activity already.
     */
    private Coord getRandomCoord(LandCoverUtils.LandCoverActivityType activityType, String zoneId) {
        Geometry zone = this.zoneFeatures.keySet()
                                         .stream()
                                         .filter(key -> key.equals(zoneId))
                                         .findFirst()
                                         .map(this.zoneFeatures::get)
                                         .orElse(null);
        return corineLandCoverData.getRandomCoord(zone, activityType);
    }

    /**
     * @param point
     * @param activityType
     * @return a random coord if there was already a coordinate assigned to activity, i.e.
     * already assigned coord is inside the zone (municipality/plz/lor) but not in the CORINE landcover zone for given activity type.
     */
    private Coord reassignCoord(Point point, LandCoverUtils.LandCoverActivityType activityType){
        Geometry zone = this.zoneFeatures.values()
                                         .stream()
                                         .filter(geometry -> geometry.contains(point))
                                         .findFirst()
                                         .orElse(null);
        if (zone == null) {
        		LOG.warn(point.toString() + " / " + activityType + " not reassigned (Activity coordinates are outside given shape zone.");
        		return MGC.point2Coord(point);
        } else {
            return corineLandCoverData.getRandomCoord(zone, activityType);
        }
    }

    private Population getPopulation(String plansFile) {
        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(plansFile);
        return ScenarioUtils.loadScenario(config).getPopulation();
    }
}
