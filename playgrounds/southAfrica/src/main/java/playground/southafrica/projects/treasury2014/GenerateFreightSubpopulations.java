package playground.southafrica.projects.treasury2014;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.population.freight.InAreaPlanKeeper;
import playground.southafrica.utilities.Header;

/**
 * Class to automate the process of extracting area-specific freight 
 * subpopulations from the national freight population.
 *
 * @author jwjoubert
 */
public class GenerateFreightSubpopulations {
	private final static Logger LOG = Logger.getLogger(GenerateFreightSubpopulations.class);

	public static void main(String[] args) {
		Header.printHeader(GenerateFreightSubpopulations.class.toString(), args);
		run(args);
		Header.printFooter();
	}
	
	public static void run(String[] args){
		String inputFolder = args[0];
		String shapefileFolder = args[1];
		String outputFolder = args[2];
		
		for(StudyAreas area : StudyAreas.values()){
			LOG.info("------------------------------------------------------------------------------");
			LOG.info(" Extracting freight population specific to " + area.getName());
			LOG.info("------------------------------------------------------------------------------");
			String areaOutputFolder = outputFolder + (outputFolder.endsWith("/") ? "" : "/") + area.getName() + "/";
			
			String areaShapefile = shapefileFolder + (shapefileFolder.endsWith("/") ? "" : "/") + 
					area.getName() + "/zones/" +
					area.getName() + 
					(area == StudyAreas.GAUTENG ? "_PR2011_" : "_MN2011_") + 
					"SA-Albers.shp";
			
			Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			String inputFreightFile = inputFolder + (inputFolder.endsWith("/") ? "" : "/") + "com_010per_30_15.xml.gz";;
			String inputFreightAttributeFile = inputFolder + (inputFolder.endsWith("/") ? "" : "/") + "comAttr_010per_30_15.xml.gz";;
			new MatsimPopulationReader(sc).parse(inputFreightFile);
			new ObjectAttributesXmlReader(sc.getPopulation().getPersonAttributes()).parse(inputFreightAttributeFile);;
			
			sc = InAreaPlanKeeper.run(sc, areaShapefile, false);
			
			/* Change the facility IDs here with 'cn' prefix to reflect that it
			 * originate from a complex network. . */
			for(Id id : sc.getPopulation().getPersons().keySet()){
				for(Plan plan : sc.getPopulation().getPersons().get(id).getPlans()){
					for(PlanElement pe : plan.getPlanElements()){
						if(pe instanceof Activity){
							Activity activity = (Activity)pe;
							((ActivityImpl)activity).setFacilityId(Id.create("cn_" + activity.getFacilityId().toString(), ActivityFacility.class));
						}
					}
				}
			}
			
			/* Write the freight subpopulation to file. */
			String areaFreightFile = areaOutputFolder + "freight.xml.gz";
			String areaFreightAttributeFile = areaOutputFolder + "freightAttributes.xml.gz";
			new PopulationWriter(sc.getPopulation()).write(areaFreightFile);
			new ObjectAttributesXmlWriter(sc.getPopulation().getPersonAttributes()).writeFile(areaFreightAttributeFile);
			
			LOG.info("==> done with " + area.getName());
			sc = null;
		}
		
	}

}
