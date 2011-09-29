package city2000w;

import gis.arcgis.NutsRegionShapeReader;

import java.util.ArrayList;
import java.util.List;

import kid.GeotoolsTransformation;
import kid.KiDUtils;
import kid.filter.RegionFilter;
import kid.filter.SimpleFeatureFilter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.feature.simple.SimpleFeature;

import utils.NetworkFuser;
import utils.RegionSchema;

public class KarlsruheNetworkCreator {

	/**
	 * @param args
	 */
	
	public static class KarlsruheRegierungsBezirksFilter implements SimpleFeatureFilter{
		
		private List<String> regionNames = new ArrayList<String>();
		
		public KarlsruheRegierungsBezirksFilter(){
			init();
		}
		
		public boolean judge(SimpleFeature feature) {
			String regName = feature.getProperty(RegionSchema.REGION_NAME).getValue().toString();
			if(isInRegionList(regName)){
				return true;
			}
			else{
				return false;
			}
		}
		
		private boolean isInRegionList(String regName) {
			if(regionNames.contains((String)regName)){
				return true;
			}
			else{
				return false;
			}
			
		}

		private void init(){
			regionNames.add("DE121");
			regionNames.add("DE122");
			regionNames.add("DE123");
			regionNames.add("DE124");
			regionNames.add("DE125");
			regionNames.add("DE126");
			regionNames.add("DE127");
			regionNames.add("DE128");
			regionNames.add("DE129");
			regionNames.add("DE12A");
			regionNames.add("DE12B");
			regionNames.add("DE12C");
		}
		
	}
	
	public static void main(String[] args) {
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		String networkFilename = "networks/karlsruheRaw.xml";
		new MatsimNetworkReader(scenario).readFile(networkFilename);
			
		List<SimpleFeature> regions = new ArrayList<SimpleFeature>();
		
		GeotoolsTransformation trafo = GeotoolsTransformation.TransformationFactory.create(GeotoolsTransformation.TransformationFactory.WGS84_32N, GeotoolsTransformation.TransformationFactory.WGS84_33N);
		
		String directory = "/Volumes/projekte/2000-Watt-City/Daten/KiD/";
		NutsRegionShapeReader regionReader = new NutsRegionShapeReader(regions, new KarlsruheRegierungsBezirksFilter(), trafo);
		regionReader.read(directory + "regions_europe_wgsUtm32N.shp");
		
		NetworkFilterManager networkFilterMan = new NetworkFilterManager(scenario.getNetwork());
		networkFilterMan.addLinkFilter(new RegionFilter(regions));
		Network network = networkFilterMan.applyFilters();
		
		NetworkFuser fuser = new NetworkFuser(network);
		fuser.fuse();
		
		new NetworkWriter(network).write("networks/karlsruhe.xml");
	}

}
