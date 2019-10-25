package playgroundMeng.ptAccessabilityAnalysis.run;


import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;

import org.matsim.pt.transitSchedule.api.TransitSchedule;
import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class PtAccessabilityConfig {
	
	@Inject
	Network network;
	@Inject
	Population population;
	@Inject
	TransitSchedule transitSchedule;
	
	private List<String> analysisModes = null;
	private Map<String, Double> modeDistance = null;
	private Map<String, Double> modeScore = null;
	private List<String> realActivities = null;
	
	private String configFile = null;
	private String networkFile = null;
	private String planFile = null;
	private String shapeFile = null;
	private String transitFile = null;

	private String outputDirectory = "C:/Users/VW3RCOM/Desktop/ptAnalysisOutputFileTest/";
	private String eventFile =  "W:/08_Temporaere_Mitarbeiter/082_K-GGSN//0822_Praktikanten/Meng/vw243_cadON_ptSpeedAdj.0.1/vw243_cadON_ptSpeedAdj.0.1.output_events.xml.gz";
	
	private double analysisTimeSlice = 0;
	private int analysisGridSlice = 0;
	private double minx; private double maxx;
	private double miny; private double maxy;
	
	private double beginnTime = 0;
	private double EndTime = 0;
	
	private boolean writeStopInfo = true;
	private boolean writeLinksInfo = true;
	private boolean writeNetworkChangeEventForEachArea = true;
	private boolean considerActivities = true;
	private boolean writeArea2Time2Score = true;
	private boolean writeArea2Time2Activities = true;
	
	public void setMinx(double minx) {
		this.minx = minx;
	}
	public void setMiny(double miny) {
		this.miny = miny;
	}
	public void setMaxx(double maxx) {
		this.maxx = maxx;
	}
	public void setMaxy(double maxy) {
		this.maxy = maxy;
	}
	public double getMaxx() {
		return maxx;
	}
	public double getMaxy() {
		return maxy;
	}
	public double getMinx() {
		return minx;
	}
	public double getMiny() {
		return miny;
	}	
	public void setWriteArea2Time2Activities(boolean writeArea2Time2Activities) {
		this.writeArea2Time2Activities = writeArea2Time2Activities;
	}
	public boolean isWriteArea2Time2Activities() {
		return writeArea2Time2Activities;
	}
	
	public void setWriteArea2Time2Score(boolean writeArea2Time2Score) {
		this.writeArea2Time2Score = writeArea2Time2Score;
	}
	
	public boolean isWriteArea2Time2Score() {
		return writeArea2Time2Score;
	}
	
	public void setEventFile(String eventFile) {
		this.eventFile = eventFile;
	}
	public String getEventFile() {
		return eventFile;
	}

	public void setConsiderActivities(boolean considerActivities) {
		this.considerActivities = considerActivities;
	}
	public boolean isConsiderActivities() {
		return considerActivities;
	}
	public void setRealActivities(List<String> realActivities) {
		this.realActivities = realActivities;
	}
	public List<String> getRealActivities() {
		return realActivities;
	}
	public void setWriteNetworkChangeEventForEachArea(boolean writeNetworkChangeEventForEachArea) {
		this.writeNetworkChangeEventForEachArea = writeNetworkChangeEventForEachArea;
	}
	public boolean isWriteNetworkChangeEventForEachArea() {
		return writeNetworkChangeEventForEachArea;
	}
	
	public void setModeScore(Map<String, Double> modeScore) {
		this.modeScore = modeScore;
	}
	public Map<String, Double> getModeScore() {
		return modeScore;
	}
	
	public String getOutputDirectory() {
		return outputDirectory;
	}
	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}
	public List<String> getAnalysisModes() {
		return analysisModes;
	}
	public void setAnalysisModes(List<String> analysisModes) {
		this.analysisModes = analysisModes;
	}
	public Map<String, Double> getModeDistance() {
		return modeDistance;
	}
	public void setModeDistance(Map<String, Double> modeDistance) {
		this.modeDistance = modeDistance;
	}
	public String getConfigFile() {
		return configFile;
	}
	public void setConfigFile(String configFile) {
		this.configFile = configFile;
	}
	public String getNetworkFile() {
		return networkFile;
	}
	public void setNetworkFile(String networkFile) {
		this.networkFile = networkFile;
	}
	public String getPlanFile() {
		return planFile;
	}
	public void setPlanFile(String planFile) {
		this.planFile = planFile;
	}
	public String getShapeFile() {
		return shapeFile;
	}
	public void setShapeFile(String shapeFile) {
		this.shapeFile = shapeFile;
	}
	public String getTransitFile() {
		return transitFile;
	}
	public void setTransitFile(String transitFile) {
		this.transitFile = transitFile;
	}
	public double getAnalysisTimeSlice() {
		return analysisTimeSlice;
	}
	public void setAnalysisTimeSlice(double analysisTimeSlice) {
		this.analysisTimeSlice = analysisTimeSlice;
	}
	public int getAnalysisGridSlice() {
		return analysisGridSlice;
	}
	public void setAnalysisGridSlice(int analysisGridSlice) {
		this.analysisGridSlice = analysisGridSlice;
	}
	public double getBeginnTime() {
		return beginnTime;
	}
	public void setBeginnTime(double beginnTime) {
		this.beginnTime = beginnTime;
	}
	public double getEndTime() {
		return EndTime;
	}
	public void setEndTime(double endTime) {
		EndTime = endTime;
	}
	public Network getNetwork() {
		return network;
	}
	public Population getPopulation() {
		return population;
	}
	public TransitSchedule getTransitSchedule() {
		return transitSchedule;
	}
	public boolean isWriteStopInfo() {
		return writeStopInfo;
	}
	public void setIfWriteStopInfo(boolean ifWriteStopInfo) {
		this.writeStopInfo = ifWriteStopInfo;
	}
	public boolean isWriteLinksInfo() {
		return writeLinksInfo;
	}
	public void setIfWriteLinksInfo(boolean ifWriteLinksInfo) {
		this.writeLinksInfo = ifWriteLinksInfo;
	}
}
