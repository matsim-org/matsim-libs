package playground.santiago.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class Plans2Shape {
	
	
	private String svnWorkingDir = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/";
	private String santiagoNetwork = svnWorkingDir + "network/network_merged_cl.xml.gz";
	
	
	private String runsWorkingDir = "../../../runs-svn/santiago/baseCase10pct/";
	
	private String originalLocationPlans = runsWorkingDir + "outputOfOriginalLocations/ITERS/it.0/0.plans.xml.gz";		
	private String randomizedLocationPlans = runsWorkingDir + "outputOfStep0_24/ITERS/it.0/0.plans.xml.gz";
	
	private String outputDir = "../../../baseCaseAnalysis/10pct/3_otherThings/shapesFiles/";

	
	public static void main(String[] args) {

		Plans2Shape ps = new Plans2Shape();
		ps.run();	

	}
	
	private void run(){
		writeShapeFile(santiagoNetwork,originalLocationPlans,"original");
		writeShapeFile(santiagoNetwork,randomizedLocationPlans,"randomized");
	}
	
	
	private void writeShapeFile(String networkFile, String plansFile, String conditionLocations){
		
        Config originalConfig = ConfigUtils.createConfig();
        originalConfig.network().setInputFile(networkFile);
        Scenario originalScenario = ScenarioUtils.loadScenario(originalConfig);
        PopulationReader originalReader = new PopulationReader(originalScenario);
        originalReader.readFile(plansFile);
        Population originalPopulation = originalScenario.getPopulation();
        
        Network network = originalScenario.getNetwork();       
        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:32719");
        
        String outputOriginalDir = outputDir + conditionLocations + "Locations";
        
        SantiagoSelectedPlans2ESRIShape originalShp = new SantiagoSelectedPlans2ESRIShape(originalPopulation,network,crs,outputOriginalDir);
        
        originalShp.setWriteActs(true);
        originalShp.setWriteLegs(false);
        originalShp.setOutputSample(0.1);
        originalShp.write();
	}

}
