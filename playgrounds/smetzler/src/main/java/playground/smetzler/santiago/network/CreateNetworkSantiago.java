package playground.smetzler.santiago.network;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;


public class CreateNetworkSantiago {

///Santiago network extraction:
	//Run configuration
		//Main class
			//org.openstreetmap.osmosis.core.Osmosis
		//Arguments
			//	--rb file="C:/Users/Ettan/10. Sem - Uni SS 14/VSP/Santiago/chile-latest.osm.pbf"
			//	--bounding-box top=-33.313 left=-70.841 bottom=-33.670 right=-70.459 completeWays=true 
			//	--tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link--used-node
			//	--wx "C:/Users/Ettan/10. Sem - Uni SS 14/VSP/Santiago/entireSantiagoPrimary.osm.pbf"

public static void main(String[] args) {
	
	//leider sind die Ergebnisse der WKT und des EPSG nicht ueberein
	//WKT: 
	//String PSAD = "PROJCS[\"PSAD56 / UTM zone 19S\",GEOGCS[\"PSAD56\",DATUM[\"D_Provisional_S_American_1956\",SPHEROID[\"International_1924\",6378388,297]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",-69],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",10000000],UNIT[\"Meter\",1]]";
	  String PSAD = "EPSG:24879";
	  
	  String inputOSM = "C:/Users/Ettan/10. Sem - Uni SS 14/VSP/Santiago/entireSantiagoSecondary.osm";
	  String outputXML = "C:/Users/Ettan/10. Sem - Uni SS 14/VSP/Santiago/entireSantiagoSecondary.xml";
    
      Config config = ConfigUtils.createConfig();
      Scenario sc = ScenarioUtils.createScenario(config);
      Network net = sc.getNetwork();
      CoordinateTransformation ct = 
      TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, PSAD);
      

      OsmNetworkReader onr = new OsmNetworkReader(net, ct);
      onr.parse(inputOSM); 
      new NetworkCleaner().run(net);
      
      new NetworkWriter(net).write(outputXML);
      
//   // simplifying the cleaned network
//      NetworkSimplifier simplifier = new NetworkSimplifier();
//      Set<Integer> nodeTypess2merge = new HashSet<Integer>();
//      nodeTypess2merge.add(new Integer(4));
//      nodeTypess2merge.add(new Integer(5));
//      simplifier.setNodesToMerge(nodeTypess2merge);
//      simplifier.run(net);
//      new NetworkWriter(net).write("merged-network_stockholm_clean_simple.xml");
      
   }


}