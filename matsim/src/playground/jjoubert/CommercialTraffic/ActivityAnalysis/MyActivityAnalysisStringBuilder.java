package playground.jjoubert.CommercialTraffic.ActivityAnalysis;

import java.util.ArrayList;
import java.util.List;

import playground.jjoubert.Utilities.MyStringBuilder;

public class MyActivityAnalysisStringBuilder extends MyStringBuilder{
	private String version;
	private String threshold;
	private String sample;
	private String studyAreaName;
	private String prefix;
	
	public MyActivityAnalysisStringBuilder(String root, String version, String threshold, String sample, String studyAreaName){
		super(root);
		this.prefix = root + studyAreaName + "/" + version + "/" + threshold 
							+ "/Sample" + sample + "/Activities/" + studyAreaName + "_";
		this.version = version;
		this.threshold = threshold;
		this.sample = sample;
		this.studyAreaName = studyAreaName;		
	}
	
	public String getGapInputMinor(){
		return prefix +"MinorLocations.txt";
	}
	
	public String getGapInputMajor(){
		return prefix + "MajorLocations.txt";
	}
	
	public String getGapOutputMinor(){
		return prefix + "MinorGapStats.txt";
	}
	
	public String getGapOutputMajor(){
		return prefix + "MajorGapStats.txt";
	}

	public String getGapShapefilename() {
		return super.getGapShapefilename(studyAreaName);
	}

	/**
	 * @return {@code ROOT + /DigiCore/Signals.txt}
	 */
	public String getSignalFilename(){
		return super.getRoot() + "/DigiCore/Signals.txt";
	}

	public String getXmlFoldername() {
		return super.getRoot() + "/DigiCore/XML/" + version + "/" + threshold + "/Sample" + sample + "/";
	}

	public String getOutputFoldername() {
		return super.getRoot() + "/" + studyAreaName + "/" + version + "/" + threshold + "/Sample" + sample + "/Activities/";
	}

	public List<String> getActivityOutputList() {
		List<String> result = new ArrayList<String>(4);
		result.add(prefix + "VehicleStats.txt");
		result.add(prefix + "ChainStats.txt");
		result.add(prefix + "MinorLocations.txt");
		result.add(prefix + "MajorLocations.txt");
		return result;
	}


}
