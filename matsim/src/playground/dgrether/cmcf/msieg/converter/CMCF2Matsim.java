package playground.dgrether.cmcf.msieg.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;




public class CMCF2Matsim {
	private Document _confXML, _plansXML, _networkXML;
	private String /*_oldGraph, _cmcfSol,*/ _newCfg, _newPath, _newOutDir, _newNetwork;
	private Random rand;
	private HashMap<String, MEdge> _edges;
	private HashSet<MPath> _plans;
	private Set<String> _unusedNodes;
	//private Map<String, Boolean> _usedNode;
	
	protected class MPath{
		String from, to, path;
		double flow;
	}
	protected class MEdge{
		String from, to;
		public MEdge(String f, String t) { this.from=f; this.to=t; }
	}
	
	public CMCF2Matsim(String cfg, String suffix){

		_confXML = getDocumentFromFile(new File(cfg));
		
		for(Iterator<Element> it = _confXML.getRootElement().getChildren().iterator(); it.hasNext(); ){
			Element e = it.next();
			//search children holding network and plans file
			
			if(e.getAttributeValue("name").equals("network")){
				//get network-filename
				for(Iterator<Element> it2 = e.getChildren().iterator(); it2.hasNext(); ){
					Element e2 = it2.next();
					if(e2.getAttributeValue("name").equals("inputNetworkFile")){
						this._networkXML = getDocumentFromFile( new File(e2.getAttributeValue("value")) );
						this._newNetwork = e2.getAttributeValue("value");
						this._newNetwork = this._newNetwork.substring(0, _newNetwork.indexOf(".xml")) + "_reduced.xml";
						
						_edges = new HashMap<String, MEdge>();
						for(Iterator<Element> it3 = this._networkXML.getRootElement().getChild("links").getChildren().iterator(); it3.hasNext(); )
						{	Element link = it3.next();
							_edges.put(link.getAttributeValue("id"), new MEdge(link.getAttributeValue("from"), link.getAttributeValue("to")) );
						}
						this._unusedNodes = new HashSet<String>();
						for(Iterator<Element> it3 = this._networkXML.getRootElement().getChild("nodes").getChildren().iterator(); it3.hasNext(); )
						{	Element node = it3.next();
							this._unusedNodes.add(node.getAttributeValue("id"));
						}
						break;
					}
				}
			}
			
			if(e.getAttributeValue("name").equals("plans")){
				//get plans-filename
				for(Iterator<Element> it2 = e.getChildren().iterator(); it2.hasNext(); ){
					Element e2 = it2.next();
					if(e2.getAttributeValue("name").equals("inputPlansFile")){
						_newPath = e2.getAttributeValue("value");
						_plansXML = getDocumentFromFile( new File(_newPath) );
						_newPath = _newPath.substring(0, _newPath.indexOf(".xml")) + suffix + ".xml";
						break;
					}
				}
			}
			
			if(e.getAttributeValue("name").equals("controler")){
				//get output directory
				for(Iterator<Element> it3 = e.getChildren().iterator(); it3.hasNext(); ){
					Element e3 = it3.next();
					if(e3.getAttributeValue("name").equals("outputDirectory")) {
						_newOutDir = e3.getAttributeValue("value") + suffix;
						//System.out.println(_newOutDir);
						break;
					}
				}
			}

			if(e.getAttributeValue("name").equals("global")){
				// create random number generator with specified seed
				for(Iterator<Element> it3 = e.getChildren().iterator(); it3.hasNext(); ){
					Element e3 = it3.next();
					if(e3.getAttributeValue("name").equals("randomSeed")) {
						rand = new Random( new Long( e3.getAttributeValue("value") ) );
						//System.out.println( e3.getAttributeValue("value") );
						break;
					}
				}
				if(rand==null) rand = new Random();
			}
			
			//are we done?
			if((_networkXML != null) && (_plansXML != null) && (_edges != null) && (_newOutDir != null) && (rand != null))
				break;
		}
		
		//set path to new configuration file
		_newCfg = cfg.substring(0, cfg.indexOf(".xml")) + suffix + ".xml";
	}

	
	/**
	 * help method to get an Document from the given XML-File,
	 * any exception causes the program to abort
	 */
	public static Document getDocumentFromFile(File file){
		SAXBuilder parser = new SAXBuilder();
		parser.setValidation(false);
		Document doc = null;
		try {
			doc = parser.build(file);
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(doc==null){
			System.out.println(">!> An Error occured when parsing file: "+file+"\n\tAborting...");
			System.exit(1);
		}
		return doc;
	}
	
	
	public int writePaths(double reroutingProbability){
		/********************
		 * Steps of method:
		 * Load the solutionfile and extract paths into a set of MPaths
		 * 
		 * Go through XML-File and iterate over every person:
		 * 	Look if there is a MPath which corresponds to the plan of that person
		 *  If yes then change the route-Tag of these person and decrement the flow by 1.
		 *  
		 * Write the output plans XML file.
		 */
		int readpersons = 0;
		int plansChanged = 0;
		System.out.println("Processing persons...");
		//HashSet<MPath> paths = this.extractPathsFromCMCF(solutionFile);
		for(Iterator<Element> it = _plansXML.getRootElement().getChildren().iterator(); it.hasNext(); ){
			Element person = it.next();
			readpersons++;
			if (readpersons % 500 == 0) {
				System.out.println("Read " + readpersons + " number of persons.");
			}
			
			String edgeStart = null, edgeEnd=null;
			//extract start and target of that person:
			boolean first = true;
			for(Iterator<Element> it2 = person.getChild("plan").getChildren().iterator(); it2.hasNext(); ) {	
				Element e = it2.next();
				if(e.getName().equals("act")){
				  if (first) {
						edgeStart = e.getAttributeValue("link");
				    first = false;
				  }
				  else {
						edgeEnd = e.getAttributeValue("link");
				  }
//					if(e.getAttributeValue("type").equals("h"))
//						edgeStart = e.getAttributeValue("link");
//					else if(e.getAttributeValue("type").equals("w"))
//						edgeEnd = e.getAttributeValue("link");
				}
			}
			
			this._unusedNodes.remove(_edges.get(edgeStart).from);
			this._unusedNodes.remove(_edges.get(edgeEnd).to);

			// check if person is rerouted
			if(rand.nextDouble() > reroutingProbability){
				StringTokenizer st = new StringTokenizer(
						person.getChild("plan").getChild("leg").getChild("route").getText());
				while(st.hasMoreTokens())
					this._unusedNodes.remove(st.nextToken());
				continue;
			}

			// iterate over paths, until an MPath is found.
			for(MPath p: _plans){
				if( Math.round(p.flow) < 1 ) continue;
				if( _edges.get(edgeStart).to.equals(p.from) && _edges.get(edgeEnd).from.equals(p.to))
				{ 	//then it is the right path, we can reroute the agent:
					//we have to change the route tag of that person and
					//transform the edge based path to a node based path.
					String route = p.from;
					this._unusedNodes.remove(p.from);
					StringTokenizer st = new StringTokenizer(p.path);
					while(st.hasMoreTokens()) {
						String edgeId = st.nextToken();
						route = route + " " + _edges.get(edgeId).to;
						this._unusedNodes.remove(_edges.get(edgeId).to);
					}
					person.getChild("plan").getChild("leg").getChild("route").setText(route);
					person.getChild("plan").getChild("leg").getChild("route").removeAttribute("trav_time");
					person.getChild("plan").getChild("leg").getChild("route").removeAttribute("dist");
					person.getChild("plan").getChild("leg").removeAttribute("trav_time");
					person.getChild("plan").getChild("leg").removeAttribute("arr_time");
					p.flow--;
					plansChanged++;
					break;
				}
			}
		} //end of outer for
		
		System.out.println("Start to write the xml document...");
		
		//now write xml Document:
		try {
		    XMLOutputter outputter = new XMLOutputter();//("  ", true);
		    FileWriter writer = new FileWriter(_newPath);
		    outputter.output(_plansXML, writer);
		    writer.close();
		} catch (java.io.IOException e) {
		    e.printStackTrace();
		}
		return plansChanged;
	}
	

	protected void extractPathsFromCMCF(String solutionFile){
		_plans = new HashSet<MPath>();
		try {
			BufferedReader in = new BufferedReader(new FileReader(solutionFile));
			String line = null;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				/***
				 * Example of a line which has to be extracted:
				 * Flow 7.5 on path 1: 2 -> 12 (15000, 2): 7 16
				 */
				if(line.startsWith("Flow")){
					line = line.substring(5);
					MPath p = new MPath();
					p.flow = new Double(line.substring(0, line.indexOf(' ')));
					line = line.substring(line.indexOf(':')+2);
					p.from = line.substring(0, line.indexOf(" ->"));
					line = line.substring(line.indexOf("-> ")+3);
					p.to = line.substring(0,line.indexOf(" ("));
					p.path = line.substring(line.indexOf("): ")+3);
					_plans.add(p);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeReducedNetwork(){
		int countLinks = 0, countNodes = 0, removedLinks=0, removedNodes=0;
		for(Iterator<Element> it3 = this._networkXML.getRootElement().getChild("links").getChildren().iterator(); it3.hasNext(); )
		{	Element link = it3.next(); countLinks++;
			
			//if(!usedLink.containsKey(link.getAttributeValue("id"))){
			if(this._unusedNodes.contains( link.getAttributeValue("from") ) ||
					this._unusedNodes.contains( link.getAttributeValue("to") ) ) {
				removedLinks++;
				link.setName("unusedLink");
			}
		}
		for(Iterator<Element> it3 = this._networkXML.getRootElement().getChild("nodes").getChildren().iterator(); it3.hasNext(); )
		{	Element node = it3.next(); countNodes++;
			if( this._unusedNodes.contains(node.getAttributeValue("id"))){
				removedNodes++;
				node.setName("unusedNode");
			}
		}
		
		this._networkXML.getRootElement().getChild("links").removeChildren("unusedLink");
		this._networkXML.getRootElement().getChild("nodes").removeChildren("unusedNode");
		
		try {
		    XMLOutputter outputter = new XMLOutputter();//("  ", true);
		    FileWriter writer = new FileWriter(this._newNetwork);
		    outputter.output(this._networkXML, writer);
		    writer.close();
		} catch (java.io.IOException e) {
		    e.printStackTrace();
		}
		
		System.out.println(" Reduced Network written to "+this._newNetwork+" and has "
				+(countNodes-removedNodes)+" / "+countNodes+" nodes with "
				+(countLinks-removedLinks)+" / "+countLinks+" links");
	}
	
	public void writeConfig(){
		for(Iterator<Element> it = _confXML.getRootElement().getChildren().iterator(); it.hasNext(); ){
			Element e = it.next();
			//search children holding network and plans file
			//change them to the new files
			
			if(e.getAttributeValue("name").equals("plans")){
				//set plans-filename
				for(Iterator<Element> it2 = e.getChildren().iterator(); it2.hasNext(); ){
					Element e2 = it2.next();
					if(e2.getAttributeValue("name").equals("inputPlansFile")){
						e2.setAttribute("value", _newPath);
						break;
					}
				}
			}
			
			if(e.getAttributeValue("name").equals("controler")){
				//set output directory
				for(Iterator<Element> it3 = e.getChildren().iterator(); it3.hasNext(); ){
					Element e3 = it3.next();
					if(e3.getAttributeValue("name").equals("outputDirectory")) {
						e3.setAttribute("value", _newOutDir);
						break;
					}
				}
			}
		}
		
		//write new config file
		try {
		    XMLOutputter outputter = new XMLOutputter();//("  ", true);
		    FileWriter writer = new FileWriter(_newCfg);
		    outputter.output(_confXML, writer);
		    writer.close();
		} catch (java.io.IOException e) {
		    e.printStackTrace();
		}
	}


	public static void main(String[] args) {
//		String config = "/Volumes/data/work/vspSvn/studies/schweiz-ivtch/cmcf/configCmcfPlansOneAct.xml";
//		String solution = "/Volumes/data/work/vspSvn/studies/schweiz-ivtch/cmcf/plans/solution";
//		String config = "/Volumes/data/work/vspSvn/studies/dgrether/cmcf/daganzoConfig.xml";
//		String solution = "/Volumes/data/work/vspSvn/studies/dgrether/cmcf/solution_daganzo_SO";
//		String config = "/Volumes/data/work/vspSvn/studies/schweiz-ivtch/cmcf/configCmcfPlansOneAct.xml";
//		String solution = "/Volumes/data/work/vspSvn/studies/schweiz-ivtch/cmcf/plans/flowG001.cmcf";

//		String[] args2 = {config, solution};
//		args = args2;
		
		if(args.length < 2)
		{	System.out.println(	" Usage: java CMCF2Matsim CONFIG.xml SOLUTION [Probability] [Suffix]\n" +
								" The given config file is being read and searched for plans file.\n" +
								" After that, the routing specified in given solution is converted and stored in a new plans file.\n" +
								" An agent is rerouted with the given probability (default: 1.0)\n" +
								" If specified, suffix is appended before file extension to new filenames (default: _cmcf).");
			System.exit(0);
		}
		
		double prob = 1.0;
		if(args.length > 2)
			try{
				prob = new Double(args[2]).doubleValue();
			} catch (NumberFormatException nfe) {
				System.out.println(" Couldn't convert probability, setting 1.0");
			}
			
		String suf = "_newcmcf";
		if(args.length>3)
			suf=args[3];
		
		File cfg = new File(args[0]);
		if(!cfg.exists())
		{	System.out.println("File '"+args[0]+"' not found, aborting.");
			System.exit(1);
		}
		
		System.out.print( " Reading configuration file '"+args[0]+"'\t");
		CMCF2Matsim my = new CMCF2Matsim(args[0], suf);
		System.out.print( "[DONE]\n Reading solution file '"+args[1]+"'\t");
		my.extractPathsFromCMCF(args[1]);
		System.out.print( "[DONE]\n Writing new plans file, ");
		if(prob != 1)
			System.out.print("rerouting probability "+prob+", ");
		System.out.println( my.writePaths(prob)+" agents rerouted\t[DONE]");
		//my.writeReducedNetwork();
		my.writeConfig();
		System.out.println(" Start MATSIM now with configuration file '"+my._newCfg+"'\n");
		System.out.println(" >>> Don't drink and drive. <<<");
	}
}
