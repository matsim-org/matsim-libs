package playground.dgrether.cmcf.msieg.converter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.jdom.DataConversionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;



public class Matsim2CMCF {
	private String _pathGra, _pathPlan;
	private Document _confXML, _graphXML, _plansXML;
	private HashMap<String, MEdge> _edgesPerID;
	private class MEdge{
		String from, to;
		public MEdge(String f, String t) { this.from=f; this.to=t; }
	}
	
	
	public void writeDemand(){
		/******************
		 * To optimize running time of CMCF, first the demands are stored in a HashMap,
		 * with the 'from -> to' String used as an ID and an Integer counting the occurence
		 */
		HashMap<String, Integer> demands = new HashMap<String, Integer>();

		for(Iterator<Element> it = _plansXML.getRootElement().getChildren().iterator(); it.hasNext(); )
		{	Element person = it.next();
			String s = " -> ";
			// search start and target node and append to s
			boolean first = true;
			for(Iterator<Element> it2 = person.getChild("plan").getChildren().iterator(); it2.hasNext(); )
			{	Element e = it2.next();
				if(e.getName().equals("act")){
					if (first) {
						s = _edgesPerID.get(e.getAttributeValue("link")).to + s;						
						first = false;
					}
					else {
						s = s + _edgesPerID.get(e.getAttributeValue("link")).from;						
					}
//					if(e.getAttributeValue("type").equals("h"))
//						s = _edgesPerID.get(e.getAttributeValue("link")).to + s;
//					else if(e.getAttributeValue("type").equals("w"))
//						s = s + _edgesPerID.get(e.getAttributeValue("link")).from;
				}
			}
			if(s.substring(0, s.indexOf(" -> ")).trim().equals(
					s.substring(s.indexOf(" -> ")+4).trim()) )
				continue;
			//update demand hash table
			if(demands.containsKey(s)) {
				Integer i = demands.get(s);
				demands.remove(s);
				demands.put(s, i+1);
			}
			else
				demands.put(s, 1);
		}

		//now demands are stored in demands and we can write file:
		FileWriter fstream;
		try {
			fstream = new FileWriter(_pathPlan, false);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("BEGIN DEMANDGRAPH\n");
			out.write("NAME "+_pathPlan+"\n");
			out.write("DATE "+System.currentTimeMillis()+"\n");
			out.write("AUTHOR msieg\n");
			out.write("CREATOR Matsim2CMCF\n");
			out.write("INPUT "+_pathGra+"\n");
			out.newLine();

			out.write("DEMANDS\n# ID: from -> to (demand)\n");
			int id = 1;
			for(String s: demands.keySet() )
				out.write( (id++) +": " + s + " ("+ demands.get(s) + ")\n");
			out.write("END");
			//Close the output stream
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void writeGraph() throws DataConversionException{
	    FileWriter fstream;
		try {
			fstream = new FileWriter(_pathGra, false);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("BEGIN BASEGRAPH\n");
			out.write("NAME "+_pathGra+"\n");
			out.write("DATE "+System.currentTimeMillis()+"\n");
			out.write("AUTHOR msieg\n");
			out.write("CREATOR Matsim2CMCF\n");
			out.newLine();
			
			//now go through graph.xml and write nodes & edges
			out.write("NODES\n# ID: (x y)\n");
			for(Iterator<Element> it = _graphXML.getRootElement().getChild("nodes").getChildren().iterator(); it.hasNext(); )
			{	Element node = it.next();
				out.write( node.getAttributeValue("id") + ": ("
							+ node.getAttributeValue("x") + " "
							+ node.getAttributeValue("y") + ")\n"
				);
			}
			out.newLine();
			
			out.write("EDGES\n# ID: from -> to (length capacity) {alpha beta gamma}\n");
			for(Iterator<Element> it = _graphXML.getRootElement().getChild("links").getChildren().iterator(); it.hasNext(); )
			{	Element link = it.next();
				
				double  length = link.getAttribute("length").getDoubleValue(),
				     lanes = link.getAttribute("permlanes").getDoubleValue(),
						capacity = length * lanes / 7.5,
						alpha = length / link.getAttribute("freespeed").getDoubleValue(),
						beta = 2.0,
						gamma = link.getAttribute("capacity").getDoubleValue() + 1;
				if(gamma < alpha) {
					//CMCF doesnot accept some inputs
					gamma=alpha;
					System.out.print('.');
				}
				out.write( link.getAttributeValue("id") + ": "
							+ link.getAttributeValue("from") + " -> "
							+ link.getAttributeValue("to") + " ("
							+ length + " " + capacity + ") {"
							+ alpha + " " + beta + " " + gamma +"}\n"
				);
				_edgesPerID.put(link.getAttributeValue("id"), new MEdge(link.getAttributeValue("from"), link.getAttributeValue("to")) );
			}
			
			out.write("END");
	        //Close the output stream
	        out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	public Matsim2CMCF(File config, String suffix){

		_confXML = getDocumentFromFile(config);
		//_configRoot = _configXML.getRootElement();
		
		for(Iterator<Element> it = _confXML.getRootElement().getChildren().iterator(); it.hasNext(); ){
			Element e = it.next();
			//search children holding network and plans file
			
			if(e.getAttributeValue("name").equals("network")){
				//get network-filename
				for(Iterator<Element> it2 = e.getChildren().iterator(); it2.hasNext(); ){
					Element e2 = it2.next();
					if(e2.getAttributeValue("name").equals("inputNetworkFile")){
						_pathGra = e2.getAttributeValue("value");
						_graphXML = getDocumentFromFile( new File(_pathGra) );
						break;
					}
				}
			}
			
			if(e.getAttributeValue("name").equals("plans")){
				//get plans-filename
				for(Iterator<Element> it2 = e.getChildren().iterator(); it2.hasNext(); ){
					Element e2 = it2.next();
					if(e2.getAttributeValue("name").equals("inputPlansFile")){
						_pathPlan = e2.getAttributeValue("value");
						_plansXML = getDocumentFromFile( new File(_pathPlan) );
						break;
					}
				}
			}
			//are we done?
			if(_plansXML != null && _graphXML != null)
				break;
		}
		
		_pathGra = _pathGra.substring(0, _pathGra.indexOf(".xml")) + suffix + ".gra";
		_pathPlan = _pathPlan.substring(0, _pathPlan.indexOf(".xml")) + suffix + ".dem";
		//_edgesPerID = createHashTableOfEdges();
		_edgesPerID = new HashMap<String, MEdge>();
	}

	
	/**
	 * help method to get an Document from the given XML-File,
	 * any exception causes the program to abort
	 */
	public static Document getDocumentFromFile(File file){
		SAXBuilder parser = new SAXBuilder();
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

	
	public static void main(String[] args) {
//		String[] args2 = {"/Volumes/data/work/vspSvn/studies/schweiz-ivtch/cmcf/configCmcfPlansOneAct.xml"};
		String[] args2 = {"/Volumes/data/work/vspSvn/studies/dgrether/cmcf/daganzoConfig.xml"};
		
		args = args2;
		
		if(args.length == 0)
		{	System.out.println(	" Usage: java Matsim2CMCF CONFIG.xml [Suffix]\n" +
								" The given config file is being read and searched for the graph and plans file.\n" +
								" After these have been read, they are converted to CMCF format and saved.\n" +
								" If specified, suffix is appended before file extension to new filenames.");
			System.exit(0);
		}
		
		String suf = "_cmcf";
		if(args.length>1)
			suf=args[1];
		File cfg = new File(args[0]);
		if(!cfg.exists())
		{	System.out.println("File '"+args[0]+"' not found, aborting.");
			System.exit(1);
		}
		
		System.out.print( " Reading configuration file '"+args[0]+"' \t");
		Matsim2CMCF my = new Matsim2CMCF(cfg, suf);
		System.out.print( "[DONE]\n Writing graph file...\t");
		try {
			my.writeGraph();
		} catch (DataConversionException e) {
			e.printStackTrace();
		}
		System.out.print( "[DONE]\n Writing demand file...\t");
		my.writeDemand();
		System.out.println( "[DONE]\n Output files can be found in the same directory as input.\n Start now CMCF and write solution into a file.");
	}
}
