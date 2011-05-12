package playground.msieg.cmcf;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * This class is for converting a MATSIM Network into a file format,
 * that CMCF is able to handle (right now, XML-format)
 * @author msieg
 *
 */
public class CMCFNetworkWriter {

	private final String netFile;
	private final MatsimNetworkReader netReader;
	private final NetworkImpl netLayer;
	private String netName;

	/**
	 * @param netFile path to the file which should be converted
	 */
	public CMCFNetworkWriter(String netFile) {
		super();
		this.netFile = netFile;
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.netLayer = scenario.getNetwork();
		this.netReader = new MatsimNetworkReader(scenario);
		this.netName = "unspecified";
	}

	public void read() throws UncheckedIOException {
		this.netReader.parse(this.netFile);
		this.netLayer.connect();
		this.netName = this.netLayer.getName();
	}


	public void setNetName(String netName) {
		this.netName = netName;
	}

	/**
	 * Converts file and prints out to console, equivalent to call convert(null)
	 */
	public void convert(){
		this.convert(null);
	}


	/**
	 * Converts the network into CMCF format and writes it to the specified Writer.
	 * Make sure you have called read() before calling this function
	 * @param out the Writer where to write the output, or null
	 */
	public void convert(Writer out){
		log("<basegraph>\n", out);
		this.convertHeader(out, (byte)1);
		this.convertNodes(out, (byte)1);
		this.convertLinks(out, (byte)1);
		log("</basegraph>", out);
	}

	/**
	 * Make something like:
	 *       <header>
	 *               <name>1678683210</name>
 	 *               <date>1226498697127</date>
 	 *               <author>msieg</author>
 	 *               <creator>TNTP2CMCF</creator>
 	 *       </header>
	 * @param out
	 * @param tabs
	 */
	private void convertHeader(Writer out, byte tabs){
		String tab="";
		while(tabs-- > 0)
			tab += '\t';
		log(tab+"<header>\n", out);
		log(tab+"\t<name>"+this.netName+"</name>\n", out);
		log(tab+"\t<date>"+System.currentTimeMillis()+"</date>\n", out);
		log(tab+"\t<creator>"+this.getClass().getSimpleName()+"</creator>\n", out);
		log(tab+"</header>\n", out);
	}

	/**
	 * Make something like:
	 *       <nodes>
	 *               <node id="8" x="22" y="22"/>
 	 *               <node id="1" x="22" y="22"/>
 	 *       </nodes>
	 * @param out
	 * @param tabs
	 */
	private void convertNodes(Writer out, byte tabs){
		String tab="";
		while(tabs-- > 0)
			tab += '\t';
		log(tab+"<nodes>\n", out);
		for (Map.Entry<Id, Node> entry: this.netLayer.getNodes().entrySet()){
			log(tab+"\t<node id=\""+entry.getKey()+"\" " +
					"x=\""+entry.getValue().getCoord().getX()+"\" " +
					"y=\""+entry.getValue().getCoord().getY()+"\" />\n", out);
		}
		log(tab+"</nodes>\n", out);
	}

	/**
	 * Make something like:
	 *       <edges>
	 *  				<edge id="65">
	 *                       <from>21</from>
	 *                       <to>22</to>
	 *                       <length>2</length>
	 *                       <capacity>5229.910063</capacity>
	 *                       <lpfdata>
	 *                               <alpha>2</alpha>
	 *                               <beta>0.15</beta>
	 *                               <gamma>5230.910063</gamma>
	 *                       </lpfdata>
	 *               </edge>
	 *       </edges>
	 * @param out
	 * @param tabs
	 */
	private void convertLinks(Writer out, byte tabs){
		String tab="";
		while(tabs-- > 0)
			tab += '\t';
		log(tab+"<edges>", out);
		for (Link l: this.netLayer.getLinks().values()){
			log(tab+"\t<edge id=\""+l.getId()+"\">\n", out);
			log(tab+"\t\t<from>"+l.getFromNode().getId()+"</from>\n", out);
			log(tab+"\t\t<to>"+l.getToNode().getId()+"</to>\n", out);
			//now read link parameters
			double 	length = l.getLength(),
					capacity = length/7.5 * l.getNumberOfLanes(),
					alpha = ((LinkImpl) l).getFreespeedTravelTime(),
					beta = 2.0,
					gamma = capacity + 1;
			//and write them
			log(tab+"\t\t<length>"+length+"</length>\n", out);
			log(tab+"\t\t<capacity>"+capacity+"</capacity>\n", out);
			log(tab+"\t\t<lpfdata>\n", out);
			log(tab+"\t\t\t<alpha>"+alpha+"</alpha>\n", out);
			log(tab+"\t\t\t<beta>"+beta+"</beta>\n", out);
			log(tab+"\t\t\t<gamma>"+gamma+"</gamma>\n", out);
			log(tab+"\t\t</lpfdata>\n", out);
			log(tab+"\t</edge>\n", out);
		}
		log(tab+"</edges>\n", out);
	}

	private void log(String s, Writer out){
		if(out == null){
			System.out.print(s);
		}
		else{
			try {
				out.write(s);
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length==0){
			System.out.println("Usage: java CMCFNetworkWriter [matsimNetworkFile] [outputFile]\n" +
					"\t second argument is optional if not given, then output is written to console.");
		}
		CMCFNetworkWriter cnw = new CMCFNetworkWriter(args[0]);

		//read input file
		try{
			cnw.read();
		}catch(UncheckedIOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}

		//write output file
		Writer out=null;
		if(args.length>1){
			System.out.print(" Trying to access output file '"+args[1]+"' ... ");
			try {
				out = new FileWriter(args[1]);
				System.out.println(" [DONE]");
			} catch (IOException e) {
				e.printStackTrace();
				out = null;
				System.out.println(" Sorry, but access denied, writing output to console.");
			}
		}

		//do it
		cnw.convert(out);
	}
}
