package playground.jjoubert.projects.capeTownFreight;

import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.southafrica.utilities.Header;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

/**
 * Class to read in a MATSim {@link Population} and extract all those 
 * {@link Person}s with at least one {@link Activity} inside a given 
 * geographical area.
 *
 * @author jwjoubert
 */
public class ExtractCapeTownVehiclesFromPopulation {
	private final static Logger LOG = Logger.getLogger(ExtractCapeTownVehiclesFromPopulation.class);

	/**
	 * Implementation of the population extractor.
	 * 
	 * @param args The method needs the following arguments, and in the
	 * specific order:
	 * <ol>
	 * 	<li> population input file;
	 * 	<li> population attribute file for input population;
	 * 	<li> path to the shapefile describing the study area of interest;
	 * 	<li> population output file; and
	 * 	<li> population attribute file for the output population.
	 * </ol>
	 */
	public static void main(String[] args) {
		Header.printHeader(ExtractCapeTownVehiclesFromPopulation.class.toString(), args);
		
		String inputPopulationFile = args[0];
		String inputAttributeFile = args[1];
		String shapefile = args[2];
		String outputPopulationfile = args[3];
		String outputAttributeFile = args[4];
		
		/* Read in the population file. */
		Scenario scInput = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scInput).parse(inputPopulationFile);
		/* Read in the population attributes. */
		new ObjectAttributesXmlReader(scInput.getPopulation().getPersonAttributes()).parse(inputAttributeFile);
		LOG.info("Total number in population before extraction: " + scInput.getPopulation().getPersons().size());
		
		
		/* Read in the shapefile. */
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> features = sfr.getFeatureSet();
		if(features.size() > 1){
			LOG.warn("Multiple features in given shapefile. Number of features: " + features.size());
		}
		/* Get the first geometry. */		
		MultiPolygon city = null;
		Iterator<SimpleFeature> iterator = features.iterator();
		SimpleFeature sf = iterator.next();
		if(sf.getDefaultGeometry() instanceof MultiPolygon){
			LOG.info("Great! Geometry is MultiPolygon.");
			city = (MultiPolygon)sf.getDefaultGeometry();
		}
		
		Counter counter = new Counter("  persons # ");
		Scenario scOutput = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		GeometryFactory gf = new GeometryFactory();
		for(Person person : scInput.getPopulation().getPersons().values()){
			boolean inCapeTown = false;
			Iterator<PlanElement> elements = person.getSelectedPlan().getPlanElements().iterator();
			while(elements.hasNext() && !inCapeTown){
				PlanElement pe = elements.next();
				if(pe instanceof Activity){
					Activity act = (Activity)pe;
					Point p = gf.createPoint(new Coordinate(act.getCoord().getX(), act.getCoord().getY()));
					if(city.covers(p)){
						inCapeTown = true;
//						LOG.info("    --> in Cape Town!");
					}
				}
			}

			if(inCapeTown){
				scOutput.getPopulation().addPerson(person);
				scOutput.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), "subpopulation", 
						scInput.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), "subpopulation"));
			}
			counter.incCounter();
		}
		counter.printCounter();
		
		/* Write the population to file. */
		LOG.info("Total number in population after extraction: " + scOutput.getPopulation().getPersons().size());
		new PopulationWriter(scOutput.getPopulation()).write(outputPopulationfile);
		new ObjectAttributesXmlWriter(scOutput.getPopulation().getPersonAttributes()).writeFile(outputAttributeFile);
		
		Header.printFooter();
	}

}
