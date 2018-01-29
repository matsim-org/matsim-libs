package vwExamples.utils;

import java.util.Collection;
import java.util.zip.GZIPInputStream;

import org.junit.Assert;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.gis.matsim2esri.plans.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class matsimPlan2Shape {
	public static void main(String[] args) {

		String outputDir = "D:/Axer/Shapes/";

		String outShp = outputDir + "acts.shp";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(network).readFile("D:/Axer/MatsimDataStore/BaseCases/VW205/vw205.1.0.output_network_mod.xml.gz");;

		Population population = scenario.getPopulation();
		new PopulationReader(scenario).readFile("C:/Users/VWBIDGN/Downloads/vw205.1.0/vw205.1.0.output_plans.xml.gz");

		CoordinateReferenceSystem crs = MGC.getCRS("EPSG:25832");
		SelectedPlans2ESRIShape sp = new SelectedPlans2ESRIShape(population, network, crs, outputDir);
		sp.setOutputSample(0.05);
		sp.setActBlurFactor(100);
		sp.setWriteActs(true);
		sp.setWriteLegs(false);
		sp.write();

		Collection<SimpleFeature> writtenFeatures = ShapeFileReader.getAllFeatures(outShp);
//		Assert.assertEquals(2235, writtenFeatures.size());
	}

}
