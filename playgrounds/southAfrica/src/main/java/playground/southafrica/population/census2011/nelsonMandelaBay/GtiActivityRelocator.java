package playground.southafrica.population.census2011.nelsonMandelaBay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.southafrica.population.census2011.attributeConverters.CoordConverter;
import playground.southafrica.population.census2011.containers.MainDwellingType2011;
import playground.southafrica.population.utilities.ComprehensivePopulationReader;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.RandomPermutation;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * Class to change the randomly generated household locations of a population 
 * to the location of an appropriate building as derived from the 
 * GeoTerraImage data.
 * 
 * @see <a href=>GeoTerraImage Buildings<a>
 *
 * @author jwjoubert
 */
public class GtiActivityRelocator {
	private final static Logger LOG = Logger.getLogger(GtiActivityRelocator.class);
	private ComprehensivePopulationReader cpr;
	private Map<MainDwellingType2011, QuadTree<ActivityFacility>> dwellingMap = new TreeMap<MainDwellingType2011, QuadTree<ActivityFacility>>();
	private Map<GtiActivities, QuadTree<ActivityFacility>> facilityMap = new TreeMap<GtiActivityRelocator.GtiActivities, QuadTree<ActivityFacility>>();
	private double[] qtExtent;
	
	private enum GtiActivities{Home, Education, PreSchoolEducation, Primary, Secondary, Tertiary, Work, Shopping, Leisure, Other};
	private enum SurveyActivities{h, w, e1, e2, e3, s, l, o};

	public static void main(String[] args) {
		Header.printHeader(GtiActivityRelocator.class.toString(), args);
		
		String populationFolder = args[0];
		String areaShapefile = args[1];
		String gtiShapefile = args[2];
		
		GtiActivityRelocator gar = new GtiActivityRelocator();
		gar.setupQuadTreeExtentFromStudyArea(areaShapefile);
		gar.parseGtiPointsToQuadTrees(gtiShapefile);
		gar.runGtiActivityRelocator(populationFolder);
		
		/* Write population and household attributes to file. These are the
		 * only two elements that have been edited. */
		PopulationWriter pw = new PopulationWriter(gar.cpr.getScenario().getPopulation());
		pw.write(populationFolder + (populationFolder.endsWith("/") ? "" : "/") + "population_Gti.xml.gz");
		
		ObjectAttributesXmlWriter oaw = new ObjectAttributesXmlWriter(gar.cpr.getScenario().getHouseholds().getHouseholdAttributes());
		oaw.putAttributeConverter(Coord.class, new CoordConverter());
		oaw.writeFile(populationFolder + (populationFolder.endsWith("/") ? "" : "/") + "householdAttributes_Gti.xml.gz");
		
		Header.printFooter();
	}
	
	public GtiActivityRelocator() {
		this.cpr = new ComprehensivePopulationReader();
	}
	
	
	/**
	 * Finds the extent of a given shapefile by considering the corner points
	 * of the geometry's envelope.
	 * 
	 * @param areaShapefile
	 */
	public void setupQuadTreeExtentFromStudyArea(String areaShapefile){
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(areaShapefile);
		Collection<SimpleFeature> features = sfr.getFeatureSet();
		
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		
		for(SimpleFeature feature : features){
			Coordinate[] envelope = ((Geometry) feature.getDefaultGeometry()).getEnvelope().getCoordinates();
			minX = Math.min(minX, envelope[0].x);
			maxX = Math.max(maxX, envelope[2].x);
			minY = Math.min(minY, envelope[0].y);
			maxY = Math.max(maxY, envelope[2].y);
		}
		
		double[] qte = {minX, minY, maxX, maxY};
		this.qtExtent = qte;
	}
	
	
	/**
	 * An encompassing method to parse all the point features from the 
	 * GeoTerraImage Building shapefile. An {@link ActivityFacility} is created
	 * for each building, and added to an appropriate {@link QuadTree}. 
	 * Residential buildings are separately grouped. This class is responsible
	 * to categorise all buildings in a way that they will be considered once
	 * activity's are relocated.
	 * 
	 * @param gtiShapefile
	 * @see <a>GeoTerraImage Buildings product</a>
	 */
	public void parseGtiPointsToQuadTrees(String gtiShapefile){
		LOG.info("Reading GTI point features...");
		if(qtExtent.length == 0){
			throw new RuntimeException("The QuadTree extent must first be established. Run setupQuadTreeExtentFromStudyArea(String) method first!");
		}
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84_SA_Albers");
		
		/* Set up facility infrastructure */
		ActivityFacilities facilities = cpr.getScenario().getActivityFacilities();
		facilities.getFactory();
		int facilityId = 0;
		
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(gtiShapefile);
		Collection<SimpleFeature> features = sfr.getFeatureSet();
	
		LOG.info("Parsing GTI point features to QuadTrees...");
		Counter counter = new Counter("   features # ");
		
		for(SimpleFeature feature : features){
			Geometry geo = (Geometry) feature.getDefaultGeometry();
			Object om = feature.getAttribute("M_LU_CODE");
			Object os = feature.getAttribute("S_LU_CODE");
			Object ot = feature.getAttribute("T_LU_CODE");
	
			Integer mainLanduseCode = null;
			String secondaryLanduseCode = null;
			String tertiaryLanduseCode = null;
			if(geo instanceof Point && os != null && 
					om instanceof String &&
					os instanceof String){
				/* Get the point and its associated coordinates. */
				Point ps = (Point)geo;
				Coord c = new Coord(ps.getX(), ps.getY());
				Coord cc = ct.transform(c);
				
				/* Establish a facility for the point. */
				ActivityFacility facility = facilities.getFactory().createActivityFacility(Id.create(facilityId++, ActivityFacility.class), cc);
	
				mainLanduseCode = Integer.parseInt((String)om);
				secondaryLanduseCode = (String)os;
				List<GtiActivities> list = new ArrayList<GtiActivities>();
				switch (mainLanduseCode) {
				case 1: /* Agriculture */
					list.add(GtiActivities.Work);
					list.add(GtiActivities.Other);
					break;
				case 2: /* Forestry */
					list.add(GtiActivities.Work);
					break;
				case 3: /* Conservation */
					list.add(GtiActivities.Work);
					break;
				case 4: /* Mining */
					list.add(GtiActivities.Work);
					break;
				case 5: /* Transport */
					list.add(GtiActivities.Work);
					break;
				case 6: /* Utilities and infrastructure */
					list.add(GtiActivities.Work);
					break;
				case 7:
					/* TODO Not so sure how to add residential buildings as work
					 * locations as well. All of them? 
					 * TODO Is there a way to filter housing to middle class, 
					 * i.e. those that can afford domestic workers? */
					list.add(GtiActivities.Work);
	
					/* Parse the housing stock. */
					if(ot != null && ot instanceof String){
						tertiaryLanduseCode = (String)ot;
					}
					MainDwellingType2011 dwellingType = getMainDwellingTypeFromGti(secondaryLanduseCode, tertiaryLanduseCode);
	
					/* Add the building to the appropriate QuadTree, creating 
					 * the QuadTree if it doesn't already exist. */
					if(!dwellingMap.containsKey(dwellingType)){
						QuadTree<ActivityFacility> qt = new QuadTree<ActivityFacility>(qtExtent[0], qtExtent[1], qtExtent[2], qtExtent[3]);
						dwellingMap.put(dwellingType, qt);
					}
					
					/* Set the facility's object attribute to reflect dwelling type. */
					facilities.getFacilityAttributes().putAttribute(facility.getId().toString(), "dwellingType", dwellingType.toString());
					facility.addActivityOption(facilities.getFactory().createActivityOption("Home"));
					
					dwellingMap.get(dwellingType).put(cc.getX(), cc.getY(), facility);
					break;
				case 8: /* Community service */
					list.add(GtiActivities.Work);
					list.add(GtiActivities.Other);
					break;
				case 9: /* Health care */
					list.add(GtiActivities.Other);
					break;
				case 10: /* Education */
					/* Schools are a place of work. */
					list.add(GtiActivities.Work);
					list.add(GtiActivities.Education);
					
					/* But schools are mainly a place of education. */
					int secondaryClassCode = Integer.parseInt( 
							secondaryLanduseCode.substring(secondaryLanduseCode.indexOf(".")+1,	secondaryLanduseCode.length()) );
					switch (secondaryClassCode) {
					case 1:
						list.add(GtiActivities.PreSchoolEducation); break;
					case 2:
						list.add(GtiActivities.Primary); break;
					case 3:
						list.add(GtiActivities.Secondary); break;
					case 4:
						list.add(GtiActivities.Tertiary); break;
					case 5:
					case 6:
						list.add(GtiActivities.Other); break;
					default:
						break;
					}; 
					break;
				case 11: /* Commercial */
					list.add(GtiActivities.Work);
					list.add(GtiActivities.Shopping);
					list.add(GtiActivities.Leisure);
					list.add(GtiActivities.Other);
					break;
				case 12: /* Industrial */
					list.add(GtiActivities.Work);
					break;
				case 13: /* Recreation and leisure */
					list.add(GtiActivities.Work);
					list.add(GtiActivities.Leisure);
					break;
				case 14: /* Tourism */
					list.add(GtiActivities.Work);
					list.add(GtiActivities.Leisure);
					break;
				case 15: /* Institutions */
					list.add(GtiActivities.Work);
					list.add(GtiActivities.Other);
					break;
				case 16: /* Other */
					list.add(GtiActivities.Work);
					list.add(GtiActivities.Other);
					/* Ignore */
					break;
				default:
					break;
				}
				
				/* Now add the point to all the relevant QuadTrees. */
				for(GtiActivities activity : list){
					/* First add the facility options. */
					facility.addActivityOption(facilities.getFactory().createActivityOption(activity.toString()));
					
					if(!facilityMap.containsKey(activity)){
						facilityMap.put(activity, new QuadTree<ActivityFacility>(qtExtent[0], qtExtent[1], qtExtent[2], qtExtent[3]));
					}
					facilityMap.get(activity).put(cc.getX(), cc.getY(), facility);
				}
				facilities.addActivityFacility(facility);
			} else{
				/* Ignore the feature. */
				LOG.warn("  Problematic feature:");
				LOG.warn("  |_     Geometry type: " + geo.getGeometryType());
				LOG.warn("  |_ Landuse code type: " + os.getClass().toString());
				LOG.warn("  |");
				LOG.warn("  Feature will be ignored.");
			}
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("Done parsing GTI point features.");
		
		LOG.info("Residential building type summary:");
		for(MainDwellingType2011 type : dwellingMap.keySet()){
			LOG.info("   " + type.toString() + ": " + dwellingMap.get(type).size());
		}
		LOG.info("Other activity type summary:");
		for(GtiActivities activity: facilityMap.keySet()){
			LOG.info("   " + activity.toString() + ": " + facilityMap.get(activity).size());
		}
	}

	
	/**
	 * There is not a one-to-one match between the dwelling descriptions and 
	 * classifications provided in Census 2011, and that which is provided in
	 * the GTI Building data set. This class makes the best possible assignment
	 * to link the synthetic population's main dwelling type (as assigned from
	 * Census 2011 data, to an actual building.
	 * 
	 * @param secondaryClass as described in the GTI Building data set's land-use 
	 * 			classification document.
	 * @return an approximate main dwelling type as described in Census 2011. 
	 */
	public MainDwellingType2011 getMainDwellingTypeFromGti(String secondaryClass, String teriaryClass){
		int secondaryClassCode = Integer.parseInt( secondaryClass.substring(secondaryClass.indexOf(".")+1, secondaryClass.length()) );
		
		switch (secondaryClassCode) {
		case 1:
			return MainDwellingType2011.FormalHouse;
		case 7:
			return MainDwellingType2011.Other;
		case 8:
			return MainDwellingType2011.TraditionalDwelling;
		}
		
		String tmp = teriaryClass.replaceFirst("7.", "");
		int teriartyClassCode = Integer.parseInt( tmp.substring(tmp.indexOf(".")+1, tmp.length()) );
		switch (secondaryClassCode) {
		case 2:
			switch (teriartyClassCode) {
			case 1: /* Informal */
			case 2: /* Transitional (Unknown) */
			case 3: /* Backyard (formal or informal) */
				return MainDwellingType2011.Informal;
			}
		case 3:
			switch (teriartyClassCode) {
			case 1: /* Flats */
				return MainDwellingType2011.Apartment;
			case 2: /* Hostels */
			case 3: /* Retirement village */
			case 4: /* Townhouse */
			case 5: /* Duet */
			case 6: /* Student residencies */
			case 7: /* Children's homes */
			case 8: /* Correctional services */
				return MainDwellingType2011.Cluster;
			}
		case 4:
			switch (teriartyClassCode) {
			case 1: /* Estate gate */
				return MainDwellingType2011.NotApplicable;
			case 2: /* Estate housing */
				return MainDwellingType2011.FormalHouse;
			}
		case 5:
			switch (teriartyClassCode) {
			case 1: /* Security village gate */
				return MainDwellingType2011.NotApplicable;
			case 2: /* Security village housing */
				return MainDwellingType2011.Townhouse;
			}
		case 6:
			switch (teriartyClassCode) {
			case 1: /* Smallholdings */
				return MainDwellingType2011.FormalHouse;
			case 2: /* Farmsteads */
				return MainDwellingType2011.FormalHouse;
			}
		}
		
		LOG.error("Could return a main dwelling type:");
		LOG.error("   Secondaty code: " + secondaryClass);
		LOG.error("     Teriary code: " + teriaryClass);
		LOG.error("Returning 'Other' as main dwelling type.");
		return MainDwellingType2011.Other;
	}
	
	
	/**
	 * To simplify the number of main dwelling types specified in Census 2011,
	 * and to align with the GTI housing types, this class simplifies the Census
	 * 2011 main dwelling types to fewer possibilities.
	 * 
	 * @param type
	 * @return
	 */
	public MainDwellingType2011 getMainDwellingTypeFromCensus(MainDwellingType2011 type){
		switch (type) {
		case FormalHouse:
			return MainDwellingType2011.FormalHouse;
		case TraditionalDwelling:
			return MainDwellingType2011.TraditionalDwelling;
		case Apartment:
		case Cluster:
		case Townhouse:
		case SemiDetachedHouse:
			return MainDwellingType2011.Cluster;
		case BackyardFormal:
		case BackyardInformal:
		case Informal:
			return MainDwellingType2011.Informal;
		case CaravanTent:
		case Other:
		case Unknown:
			return MainDwellingType2011.Other;
		default:
			return MainDwellingType2011.Other;	
		}		
	}
	
	
	/**
	 * Class to relocate each {@link Activity} of each {@link Person}'s 
	 * {@link Plan} to an actual facility. 
	 * 
	 * @param populationFolder
	 */
	public void runGtiActivityRelocator(String populationFolder){
		cpr.parse(populationFolder);
		
		LOG.info("Running the GTI activity relocator: (" + cpr.getScenario().getHouseholds().getHouseholds().size() + " households)");
		Counter counter = new Counter("   households # ");
		for(Id hhid : cpr.getScenario().getHouseholds().getHouseholds().keySet()){
			/* First update the household's home coordinate. */			
			Coord homeCoord = (Coord) cpr.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(hhid.toString(), "homeCoord");
			
			MainDwellingType2011 dwellingType = MainDwellingType2011.valueOf((String) cpr.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(hhid.toString(), "mainDwellingType"));
			MainDwellingType2011 simpleType = this.getMainDwellingTypeFromCensus(dwellingType);
			
			/* Check if an appropriate QuadTree exists for the simple dwelling type. */
			QuadTree<ActivityFacility> qt = dwellingMap.get(simpleType);
			if(qt != null && qt.size() > 0){
				ActivityFacility residence = getGtiFacility(homeCoord, qt, 20);
				
				/* Adapt the home coordinate of the household. */
				cpr.getScenario().getHouseholds().getHouseholdAttributes().putAttribute(hhid.toString(), "homeCoord", residence.getCoord());
				
				/* Adapt the activity locations of all the household members. */
				for(Id id : cpr.getScenario().getHouseholds().getHouseholds().get(hhid).getMemberIds()){
					Person p = cpr.getScenario().getPopulation().getPersons().get(id);
					for(Plan plan : p.getPlans()){
						for(PlanElement pe : plan.getPlanElements()){
							if(pe instanceof Activity){
								ActivityImpl activity = (ActivityImpl)pe;
								SurveyActivities activityType = SurveyActivities.valueOf(activity.getType());
								ActivityFacility facility = null;
								switch (activityType) {
								case h:
									facility = residence;
									break;
								case w:
									facility = getGtiFacility(activity.getCoord(), facilityMap.get(GtiActivities.Work), 20);
									break;
								case e1:
									int age = (Integer) cpr.getScenario().getPopulation().getPersonAttributes().getAttribute(p.getId().toString(), "age");
									if(age < 6){
										facility = getGtiFacility(activity.getCoord(), facilityMap.get(GtiActivities.PreSchoolEducation), 20);
									} else if(age <= 13){
										facility = getGtiFacility(activity.getCoord(), facilityMap.get(GtiActivities.Primary), 20);
									} else{
										facility = getGtiFacility(activity.getCoord(), facilityMap.get(GtiActivities.Secondary), 20);
									}
									break;
								case e2:
									facility = getGtiFacility(activity.getCoord(), facilityMap.get(GtiActivities.Tertiary), 20);
									break;
								case e3:
									facility = getGtiFacility(activity.getCoord(), facilityMap.get(GtiActivities.Education), 20);
									break;
								case s:
									facility = getGtiFacility(activity.getCoord(), facilityMap.get(GtiActivities.Shopping), 20);
									break;
								case l:
									facility = getGtiFacility(activity.getCoord(), facilityMap.get(GtiActivities.Leisure), 20);
									break;
								case o:
									facility = getGtiFacility(activity.getCoord(), facilityMap.get(GtiActivities.Other), 20);
									break;
								default:
									break;
								}
								activity.setCoord(facility.getCoord());
								activity.setFacilityId(facility.getId());
								cpr.getScenario().getActivityFacilities().getFacilityAttributes().putAttribute(facility.getId().toString(), "isUsed", true);
							}
						}
					}
				}
				
				/* For certain residential building types, it is assumed free 
				 * standing, and should be removed from the QuadTree since it is 
				 * already "occupied" by a family/household. */
				switch (simpleType) {
				case FormalHouse:
				case Informal:
				case Townhouse:
				case TraditionalDwelling:
					qt.remove(residence.getCoord().getX(), residence.getCoord().getY(), residence);
				default:
					/* Keep the building in the QuadTree as it may be occupied by
					 * multiple households. */
					break;
				}
			}
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("Done relocating activities.");
	}
	
	
	/**
	 * The appropriate {@link QuadTree} of GTI buildings is identified, and 
	 * building is sampled. The sampling occurs as follows: first a shortlist 
	 * of the 20 nearest facilities (of the correct type) is constructed. 
	 * Secondly, a facility is randomly sampled from the shortlist.
	 * 
	 * @param c the original {@link Coord}inate of the activity;
	 * @param qt the appropriate {@link QuadTree} containing buildings that can
	 * accommodate the specific activity type;
	 * @param number of nearest facilities to consider in the shortlist.
	 * @return a randomly sampled facility picked from the shortlist of nearest
	 * facilities.
	 */
	private ActivityFacility getGtiFacility(Coord c, QuadTree<ActivityFacility> qt, int number){
		List<Tuple<ActivityFacility, Double>> list = new ArrayList<Tuple<ActivityFacility,Double>>();
		List<Tuple<ActivityFacility, Double>> tuples = new ArrayList<Tuple<ActivityFacility,Double>>();
		
		/* Quickly scan distance in QuadTree to limit the ranking later-on. */ 
		Collection<ActivityFacility> facilitiesToRank = null;
		if(qt.values().size() > number){
		 /* Start the search radius with the distance to the closest facility. */
			ActivityFacility closestFacility = qt.getClosest(c.getX(), c.getY());
			
			double radius = CoordUtils.calcEuclideanDistance(c, closestFacility.getCoord() );
			Collection<ActivityFacility> facilities = qt.getDisk(c.getX(), c.getY(), radius);
			while(facilities.size() < number){
				/* Double the radius. If the radius happens to be zero (0), 
				 * then you stand the chance of running into an infinite loop.
				 * Hence, add a minimum of 1m to move on. */
				radius += Math.max(radius, 1.0);
				facilities = qt.getDisk(c.getX(), c.getY(), radius);
			}
			facilitiesToRank = facilities;
		} else{
			facilitiesToRank = qt.values();
		}
		
		/* Rank the facilities based on distance. */
		for(ActivityFacility facility : facilitiesToRank){
			double d = CoordUtils.calcEuclideanDistance(c, facility.getCoord());
			Tuple<ActivityFacility, Double> thisTuple = new Tuple<ActivityFacility, Double>(facility, d);
			if(tuples.size() == 0){
				tuples.add(thisTuple);
			} else{
				int index = 0;
				boolean found = false;
				while(!found && index < tuples.size()){
					if(d <= tuples.get(index).getSecond()){
						found = true;
					} else{
						index++;
					}
				}
				if(found){
					tuples.add(index, thisTuple);
				} else{
					tuples.add(thisTuple);
				}
			}
		}
		
		/* Add the number of plans requested, or the  number of the plans in 
		 * the QuadTree, whichever is less, to the results, and return. */
		for(int i = 0; i < Math.min(number, tuples.size()); i++){
			list.add(tuples.get(i));
		}
		
		/* Pick a random facility from the list. */
		return list.get(RandomPermutation.getRandomPermutation(list.size())[0]-1).getFirst();
	}
	
}
