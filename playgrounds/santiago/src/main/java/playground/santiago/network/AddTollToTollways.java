package playground.santiago.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.roadpricing.RoadPricingWriterXMLv1;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import playground.santiago.SantiagoScenarioConstants;



public class AddTollToTollways {

	private static final Logger log = Logger.getLogger(AddTollToTollways.class);
	String netFile;	
	String gantriesFile;
	String schemeName;
	String outFile;
	String crs = SantiagoScenarioConstants.toCRS;
	Collection<SimpleFeature> networkFeatures;
	Collection<SimpleFeature> gantriesFeatures;
	HashMap<Id<Link>,ArrayList<String>> pricedLinksInfo;
	double dayStartTime = 0;
	double dayEndTime = 24*60*60;
	
	//Providing a new constructor to use this class in the CreateCordonScheme class.
	public AddTollToTollways(String gantriesFile, String netFile, String schemeName){
		this.gantriesFile = gantriesFile;
		this.netFile = netFile;
		this.schemeName = schemeName;
		//do not use the writeScheme method if the class is called from outside.
	}
	
	public AddTollToTollways(){
		this.gantriesFile = "../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/toll/gantriesWithFares/gantriesAndFares2012.shp";
		this.netFile = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/network/network_merged_cl.xml.gz";
		this.schemeName = "gantries"; //default name if the class is called from inside.
		this.outFile = "../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/toll/" + schemeName + ".xml";
	}
	

	public void createNetworkFeatures (){
		

		Network net = NetworkUtils.createNetwork();		
		new MatsimNetworkReader(net).readFile(netFile);
		
		PolylineFeatureFactory linkFactory = new PolylineFeatureFactory.Builder().
				setCrs(MGC.getCRS(crs)).
				setName("link").
				addAttribute("ID", String.class).
				create();
		
		this.networkFeatures = new ArrayList<SimpleFeature>();		
		
		for(Link link : net.getLinks().values()){	
			
			if(link.getAllowedModes().contains(TransportMode.pt)){
				continue;
				
			}else{

				Coordinate fromNodeCoordinate = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
				Coordinate toNodeCoordinate = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
				Coordinate linkCoordinate = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
				
				SimpleFeature ft = linkFactory.createPolyline(new Coordinate [] {fromNodeCoordinate, linkCoordinate, toNodeCoordinate},
						new Object [] {link.getId().toString()}, null);
				networkFeatures.add(ft);
				
				
			}
		}
		
	}
	
	
	public void collectInformation() {
	
		this.pricedLinksInfo = new HashMap<>();
		ShapeFileReader shapeReader = new ShapeFileReader();
		shapeReader.readFileAndInitialize(gantriesFile);
		this.gantriesFeatures = shapeReader.getFeatureSet();
		for (SimpleFeature gantrie : gantriesFeatures) {			
			for (SimpleFeature link : networkFeatures){
				if(((Geometry) gantrie.getDefaultGeometry()).crosses((Geometry) link.getDefaultGeometry())){					
					ArrayList<String>faresInfo = new ArrayList<>();
					for (int i=5; i<=25; ++i){					
						faresInfo.add((String) gantrie.getAttribute(i));
					}
					pricedLinksInfo.put(Id.createLinkId((String) link.getAttribute("ID")), faresInfo );					
					}
				}
			}
			
		}
		

	public RoadPricingSchemeImpl createGantriesFile() {
		RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
		scheme.setName(schemeName);
		scheme.setType(scheme.TOLL_TYPE_LINK);
		scheme.setDescription("No description available");
	
		for (Map.Entry<Id<Link>,ArrayList<String>> entry : pricedLinksInfo.entrySet()){
			
			scheme.addLink(entry.getKey());			
			ArrayList<String>faresInfo = entry.getValue();			
			String TBFP = faresInfo.get(0); //"tarifa base fuera punta"
			String TBP = faresInfo.get(1); //"tarifa base punta"
			String TS = faresInfo.get(2); //"tarifa de saturación"

			
				//Only TBFP
			if (TBP.equals("-")&&TS.equals("-")){			
				scheme.addLinkCost(entry.getKey(), dayStartTime, dayEndTime, Double.parseDouble(TBFP));
				
				//TBFP + TB	
			} else if (!TBP.equals("-")&&TS.equals("-")){
				ArrayList<Double> orderedTimes =new ArrayList<>();
								
				int i=1;
				while(!faresInfo.get(2*i+1).equals("-")){
					
					String[]startTime = faresInfo.get(2*i+1).split(":");
					String[]endTime = faresInfo.get(2*i+2).split(":");
					double secondsStart = Double.parseDouble(startTime[0])*60*60+Double.parseDouble(startTime[1])*60+Double.parseDouble(startTime[2]);					
					double secondsEnd = Double.parseDouble(endTime[0])*60*60+Double.parseDouble(endTime[1])*60+Double.parseDouble(endTime[2]);
					orderedTimes.add(secondsStart);
					orderedTimes.add(secondsEnd);					
					++i;
					if (i>6) break;
				}
				
				int size = orderedTimes.size();
				scheme.addLinkCost(entry.getKey(), dayStartTime, orderedTimes.get(0)-1, Double.parseDouble(TBFP));
				

				for (int j=0; j<size; j+=2){
					
					scheme.addLinkCost(entry.getKey(),orderedTimes.get(j),orderedTimes.get(j+1)-1, Double.parseDouble(TBP));
					
					//not general, only use this with the original gantries shape file.
					//TODO: check this.						
					if(j+1==1&&size>2){						
						scheme.addLinkCost(entry.getKey(),orderedTimes.get(j+1),orderedTimes.get(j+2)-1, Double.parseDouble(TBFP));
					}
					
				}
				scheme.addLinkCost(entry.getKey(),orderedTimes.get(size-1), dayEndTime, Double.parseDouble(TBFP));
				
				}else{
					
					TreeMap<Double,String> orderedTimes = new TreeMap<>();
					
					int j=1; //index related with the "base-punta" periods
					while(!faresInfo.get(2*j+1).equals("-")){
						String[]startTime = faresInfo.get(2*j+1).split(":");
						String[]endTime = faresInfo.get(2*j+2).split(":");
						double secondsStart = Double.parseDouble(startTime[0])*60*60+Double.parseDouble(startTime[1])*60+Double.parseDouble(startTime[2]);					
						double secondsEnd = Double.parseDouble(endTime[0])*60*60+Double.parseDouble(endTime[1])*60+Double.parseDouble(endTime[2]);
						orderedTimes.put(secondsStart, TBP);
						orderedTimes.put(secondsEnd-1, TBP);
						++j;
						if (j>6) break;
					}
					
					int k=1; //index related with the "saturación" periods.
					
					while(!faresInfo.get(2*k+13).equals("-")){
						String[]startTime = faresInfo.get(2*k+13).split(":");
						String[]endTime = faresInfo.get(2*k+14).split(":");
						double secondsStart = Double.parseDouble(startTime[0])*60*60+Double.parseDouble(startTime[1])*60+Double.parseDouble(startTime[2]);					
						double secondsEnd = Double.parseDouble(endTime[0])*60*60+Double.parseDouble(endTime[1])*60+Double.parseDouble(endTime[2]);
						orderedTimes.put(secondsStart, TS);
						orderedTimes.put(secondsEnd-1, TS);
						++k;
						if (k>3) break;
					}
					
					ArrayList<Double> keys = new ArrayList<>();
					ArrayList<String> values = new ArrayList<>();
					
					for (Map.Entry<Double,String> time : orderedTimes.entrySet()){
						keys.add(time.getKey());
						values.add(time.getValue());
						
					}
					
					scheme.addLinkCost(entry.getKey(), dayStartTime, keys.get(0)-1, Double.parseDouble(TBFP));

					for (int i=0; i<keys.size(); i+=2){
						scheme.addLinkCost(entry.getKey(),keys.get(i),keys.get(i+1), Double.parseDouble(values.get(i)));
						
						
						if((i+2)<keys.size()){
							
						if (!((keys.get(i+1)+1)==keys.get(i+2))){							
							scheme.addLinkCost(entry.getKey(),keys.get(i+1)+1,keys.get(i+2)-1, Double.parseDouble(TBFP));
							
						}					
						
						
						
					}
					}
					scheme.addLinkCost(entry.getKey(), keys.get(keys.size()-1)+1 , dayEndTime, Double.parseDouble(TBFP));

					
					
					
				}

		}

		return scheme;
	}
	
	private void writeScheme (RoadPricingScheme scheme){
	
		RoadPricingWriterXMLv1 rpw = new RoadPricingWriterXMLv1(scheme);
		rpw.writeFile(outFile);
	}


	
	
	private void run() {

		createNetworkFeatures();
		collectInformation();
		RoadPricingScheme linksWithGantries = createGantriesFile();
		writeScheme(linksWithGantries);
		
	}
	
	public static void main(String[] args) {
		AddTollToTollways adtt = new AddTollToTollways();
		adtt.run();
	}
	
	



}
