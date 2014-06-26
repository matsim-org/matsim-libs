package playground.southafrica.population.census2011.nelsonMandelaBay;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.southafrica.population.census2011.attributeConverters.CoordConverter;
import playground.southafrica.population.census2011.containers.MainDwellingType2011;
import playground.southafrica.population.utilities.ComprehensivePopulationReader;
import playground.southafrica.utilities.Header;

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
public class GtiHomeRelocator {
	private final static Logger LOG = Logger.getLogger(GtiHomeRelocator.class);
	private ComprehensivePopulationReader cpr;
	private Map<MainDwellingType2011, QuadTree<Coord>> dwellingMap = new HashMap<MainDwellingType2011, QuadTree<Coord>>();
	private double[] qtExtent;
	private int householdsChanged = 0;
	private int personsChanged = 0;

	public static void main(String[] args) {
		Header.printHeader(GtiHomeRelocator.class.toString(), args);
		
		String populationFolder = args[0];
		String areaShapefile = args[1];
		String gtiShapefile = args[2];
		
		GtiHomeRelocator ghl = new GtiHomeRelocator();
		ghl.setupQuadTreeExtentFromStudyArea(areaShapefile);
		ghl.parseGtiPointsToQuadTrees(gtiShapefile);
		ghl.runGtiHomeRelocator(populationFolder);
		
		/* Write population and household attributes to file. These are the
		 * only two elements that have been edited. */
		PopulationWriter pw = new PopulationWriter(ghl.cpr.getScenario().getPopulation());
		pw.write(populationFolder + (populationFolder.endsWith("/") ? "" : "/") + "Population_Gti.xml.gz");
		
		ObjectAttributesXmlWriter oaw = new ObjectAttributesXmlWriter(ghl.cpr.getScenario().getHouseholds().getHouseholdAttributes());
		oaw.putAttributeConverter(CoordImpl.class, new CoordConverter());
		oaw.writeFile(populationFolder + (populationFolder.endsWith("/") ? "" : "/") + "HouseholdAttributes_Gti.xml.gz");
		
		Header.printFooter();
	}
	
	public GtiHomeRelocator() {
		this.cpr = new ComprehensivePopulationReader();
	}
	
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
	
	
	public void parseGtiResidentialBuildings(String areaShapefile, String gtiShapefile){
		
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
	
	
	public void parseGtiPointsToQuadTrees(String gtiShapefile){
		LOG.info("Reading GTI point features...");
		if(qtExtent.length == 0){
			throw new RuntimeException("The QuadTree extent must first be established. Run setupQuadTreeExtentFromStudyArea(String) method first!");
		}
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_SA_Albers");
		
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(gtiShapefile);
		Collection<SimpleFeature> features = sfr.getFeatureSet();

		LOG.info("Parsing GTI point features to QuadTrees...");
		Counter counter = new Counter("   features # ");
		
		for(SimpleFeature feature : features){
			Geometry geo = (Geometry) feature.getDefaultGeometry();
			Object os = feature.getAttribute("S_LU_CODE");
			Object ot = feature.getAttribute("T_LU_CODE");

			String secondaryLanduseCode = null;
			String tertiaryLanduseCode = null;
			if(geo instanceof Point && os != null && os instanceof String){
				Point ps = (Point)geo;
				secondaryLanduseCode = (String)os;
				
				/* Filter buildings to residential. */
				if(secondaryLanduseCode.startsWith("7.")){
					if(ot != null && ot instanceof String){
						tertiaryLanduseCode = (String)ot;
					}
					MainDwellingType2011 dwellingType = getMainDwellingTypeFromGti(secondaryLanduseCode, tertiaryLanduseCode);
					
					/* Add the building to the appropriate QuadTree, creating 
					 * the QuadTree if it doesn't already exist. */
					if(!dwellingMap.containsKey(dwellingType)){
						QuadTree<Coord> qt = new QuadTree<Coord>(qtExtent[0], qtExtent[1], qtExtent[2], qtExtent[3]);
						dwellingMap.put(dwellingType, qt);
					}
					Coord c = new CoordImpl(ps.getX(), ps.getY());
					Coord cc = ct.transform(c);
					dwellingMap.get(dwellingType).put(cc.getX(), cc.getY(), cc);
				}
				
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
	}
	
	
	public void runGtiHomeRelocator(String populationfolder){
		cpr.parse(populationfolder);
		
		LOG.info("Running the GTI home relocator: (" + cpr.getScenario().getHouseholds().getHouseholds().size() + " household)");
		Counter counter = new Counter("   households # ");
		for(Id hhid : cpr.getScenario().getHouseholds().getHouseholds().keySet()){
			Coord homeCoord = (Coord) cpr.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(hhid.toString(), "homeCoord");
			
			MainDwellingType2011 dwellingType = MainDwellingType2011.valueOf((String) cpr.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(hhid.toString(), "mainDwellingType"));
			MainDwellingType2011 simpleType = this.getMainDwellingTypeFromCensus(dwellingType);
			
			/* Check if an appropriate QuadTree exists for the simple dwelling type. */
			QuadTree<Coord> qt = dwellingMap.get(simpleType);
			if(qt != null && qt.size() > 0){
				Coord closestBuilding = qt.get(homeCoord.getX(), homeCoord.getY());
				
				/* We do not check how close the closest building is from the 
				 * original home coordinate. */
				
				/* Adapt the home coordinate of the household. */
				cpr.getScenario().getHouseholds().getHouseholdAttributes().putAttribute(hhid.toString(), "homeCoord", closestBuilding);
				householdsChanged++;
				
				/* Adapt the 'home' activity location of all the household members. */
				for(Id id : cpr.getScenario().getHouseholds().getHouseholds().get(hhid).getMemberIds()){
					Person p = cpr.getScenario().getPopulation().getPersons().get(id);
					boolean adaptHome = false;
					for(Plan plan : p.getPlans()){
						for(PlanElement pe : plan.getPlanElements()){
							if(pe instanceof Activity){
								ActivityImpl activity = (ActivityImpl)pe;
								if(activity.getType().equalsIgnoreCase("home")){
									activity.setCoord(closestBuilding);
									adaptHome = true;
								}
							}
						}
					}
					if(adaptHome){ personsChanged++; }
				}
				
				/* For certain building types, it is assumed free standing, 
				 * and should be removed from the QuadTree since it is already 
				 * "occupied" by a family/household. */
				switch (simpleType) {
				case FormalHouse:
				case Informal:
				case Townhouse:
				case TraditionalDwelling:
					qt.remove(closestBuilding.getX(), closestBuilding.getY(), closestBuilding);
				default:
					/* Keep the building in the QuadTree as it may be occupied by
					 * multiple households. */
					break;
				}
			}
			counter.incCounter();
		}
		counter.printCounter();
		
		LOG.info("Results from running GtiHomeRelocator:");
		LOG.info("   Households relocated: " + householdsChanged);
		LOG.info("      Persons relocated: " + personsChanged);
	}

}
