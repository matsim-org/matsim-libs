package playground.dgrether.koehlerstrehlersignal.solutionconverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossingNode;
import playground.dgrether.koehlerstrehlersignal.data.DgStreet;
import playground.dgrether.koehlerstrehlersignal.data.TtPath;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;

/**
 * Class to parse the BTU format for the path distribution.
 * The paths are saved in a DgCommodities object.
 * 
 * Note: BTU paths don't contain the first link of the MATSim route,
 * because they were converted to the end node of the MATSim links.
 * 
 * @author tthunig
 */
public class KS2015RouteXMLParser extends MatsimXmlParser{

	private static final Logger log = Logger.getLogger(KS2015RouteXMLParser.class);
	
	private final static String COMMODITY = "commodity";
	private final static String ID = "id";
	private final static String PATH = "path";
	private final static String FLOW = "flow";
	private final static String EDGE = "edge";
	private final static String FROMNODE = "source";
	private final static String TONODE = "drain";
	
	private DgCommodities comsWithRoutes = new DgCommodities();
	
	private Id<DgCommodity> currentCommodityId = null;
	private Id<DgCrossingNode> currentFromNodeId = null;
	private Id<DgCrossingNode> currentToNodeId = null;
	private double currentFlow = 0.0;
	private List<Id<DgStreet>> currentPath = new ArrayList<>();
	
	private DgIdConverter idConverter;
	
	public KS2015RouteXMLParser(DgIdConverter idConverter){
		super();
		this.idConverter = idConverter;
		this.setValidating(false);
	}
	
//	public void readFile(final String filename) {
//		this.setValidating(false);
//		readFile(filename);
//		log.info("Read " + this.comsWithRoutes.getNumberOfCommodites() + " routes");
//	}
	
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		// create a commodity for the current path and empty the fields for the new one
		if (name.equals(PATH)){
			// check whether commodity already exists
			if (!this.comsWithRoutes.getCommodities().containsKey(this.currentCommodityId)){
				this.comsWithRoutes.addCommodity(new DgCommodity(this.currentCommodityId, 
						this.currentFromNodeId, this.currentToNodeId, 0.0));
			}
			
			// create the path id
			Id<TtPath> pathId = this.idConverter.convertPathInfo2PathId(this.currentPath, 
					this.currentFromNodeId, this.currentToNodeId);
			
			// add the path to the commodity paths
			DgCommodity comWithRoute = this.comsWithRoutes.getCommodities().get(this.currentCommodityId);
			comWithRoute.addPath(pathId, this.currentPath, this.currentFlow);
		}
	}

	@Override
	public void startTag(String elementName, Attributes atts, Stack<String> context) {
		// save the commodity information
		if (elementName.equals(COMMODITY)){
			this.currentCommodityId = Id.create(atts.getValue(ID), DgCommodity.class);
			this.currentFromNodeId = Id.create(atts.getValue(FROMNODE), DgCrossingNode.class);
			this.currentToNodeId = Id.create(atts.getValue(TONODE), DgCrossingNode.class);
		}
		// save the flow value of the path
		if (elementName.equals(PATH)){
			this.currentPath = new ArrayList<>();
			this.currentFlow = Double.parseDouble(atts.getValue(FLOW));
		}
		// add the street to the path
		if (elementName.equals(EDGE)){
			this.currentPath.add(Id.create(atts.getValue(ID), DgStreet.class));
		}
	}
	
	/**
	 * Getter for the DgCommodities object that contains all parsed 
	 * commodities with paths
	 * 
	 * Note: BTU paths don't contain the first link of the MATSim route,
	 * because they were converted to the end node of the MATSim links.
	 * 
	 * @return the parsed commodities with the BTU paths
	 */
	public DgCommodities getComsWithRoutes(){
		return this.comsWithRoutes;
	}

}
