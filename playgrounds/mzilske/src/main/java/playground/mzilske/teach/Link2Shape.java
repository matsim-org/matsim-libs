package playground.mzilske.teach;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape;

public class Link2Shape {
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("input/network.xml");
		config.plans().setInputFile("input/plans.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		String crs = "PROJCS[\"ETRS89_UTM_Zone_33\",GEOGCS[\"GCS_ETRS89\",DATUM[\"D_ETRS89\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",3500000.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",15.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]";
		XY2Links xy2Links = new XY2Links((NetworkImpl) scenario.getNetwork());
		xy2Links.run(scenario.getPopulation());
		Links2ESRIShape.main(new String[]{"../../matsim/input/network.xml","input/networkline.shp","input/networkpoly.shp", crs});
		SelectedPlans2ESRIShape plans2Shape = new SelectedPlans2ESRIShape(scenario.getPopulation(), scenario.getNetwork(), MGC.getCRS(crs), "input");
		plans2Shape.setWriteActs(true);
		plans2Shape.setWriteLegs(false);
		plans2Shape.write();
		
		config.plans().setInputFile("output/output_plans.xml.gz");
		scenario = ScenarioUtils.loadScenario(config);
		plans2Shape = new SelectedPlans2ESRIShape(scenario.getPopulation(), scenario.getNetwork(), MGC.getCRS(crs), "output");
		plans2Shape.setWriteActs(false);
		plans2Shape.setWriteLegs(true);
		plans2Shape.write();
		
	}

}
