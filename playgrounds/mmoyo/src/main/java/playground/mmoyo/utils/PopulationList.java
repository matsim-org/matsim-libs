package playground.mmoyo.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.TreeSet;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import playground.mmoyo.io.TextFileWriter;

/**Reads only agents ids for analysis*/
public class PopulationList  extends MatsimXmlParser {
	private final String strPerson = "person";
	private final String strId = "id";
	private List<String> agentStrId_List = new ArrayList<String>();
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (strPerson.equals(name)) {
			agentStrId_List.add(atts.getValue(strId));
		}
	}
	
	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		
	}
	
	/**saves id's in text file */
	public void SavePersonsIds(final String outFile){
		final String NR= "\n";
		StringBuffer sBuff = new StringBuffer();
		for(String strId : agentStrId_List){
			sBuff.append(strId + NR);
		}
		new TextFileWriter().write(sBuff.toString(),outFile,false);
		System.out.println("Num of agents: " +  agentStrId_List.size());
	}

	/**sorted ids*/
	public void SortAndListPersons(){
		TreeSet<String> treeSet = new TreeSet<String>(agentStrId_List);
		for (String strId : treeSet){
			System.out.println(strId);
		}
		System.out.println("Number of persons: " + treeSet.size());
	}
	
	public static void main(String[] args) {
		String popFilePath;
		if (args.length>0){
			popFilePath = args[0];
		}else{
			popFilePath = "../../";
		}

		PopulationList populationListOnlyId = new PopulationList(); 
		populationListOnlyId.parse(popFilePath);
		//populationListOnlyId.SavePersonsIds("../../input/randomizedPtRouter/analysis/fragmentedPlansIds.txt");
		System.out.println("Num of agents: " +  populationListOnlyId.agentStrId_List.size());
		//populationListOnlyId.SortAndListPersons();
	}
	
}