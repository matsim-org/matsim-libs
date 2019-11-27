package playgroundMeng.publicTransitServiceAnalysis.others;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class PtAccessabilityConfig {

	private Network network;
	private TransitSchedule transitSchedule;

	private List<String> analysisModes = null;
	private Map<String, Double> modeDistance = null;
	private Map<String, Double> modeScore = null;

	private String configFile = null;
	private String networkFile = null;
	private String transitFile = null;
	private String outputDirectory = null;
	private String eventFile = null;
	private String analysisNetworkFile = null;

	private int analysisTimeSlice = 0;
	private int analysisGridSlice = 0;

	private double beginnTime = 0;
	private double EndTime = 0;

	private static PtAccessabilityConfig ptAccessabilityConfig = null;

	private PtAccessabilityConfig() {
	}

	public static PtAccessabilityConfig getInstance() {
		if (ptAccessabilityConfig == null)
			ptAccessabilityConfig = new PtAccessabilityConfig();
		return ptAccessabilityConfig;
	}
	
	public void setConfigFile(String configFile) {
		this.configFile = configFile;
	}
	
	public String getConfigFile() {
		return configFile;
	}

	public void setNetwork(Network network) {
		this.network = network;
	}

	public void setTransitSchedule(TransitSchedule transitSchedule) {
		this.transitSchedule = transitSchedule;
	}

	public void setAnalysisNetworkFile(String analysisNetworkFile) {
		this.analysisNetworkFile = analysisNetworkFile;
	}

	public String getAnalysisNetworkFile() {
		return analysisNetworkFile;
	}

	public void setEventFile(String eventFile) {
		this.eventFile = eventFile;
	}

	public String getEventFile() {
		return eventFile;
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

	public String getNetworkFile() {
		return networkFile;
	}

	public void setNetworkFile(String networkFile) {
		this.networkFile = networkFile;
	}

	public String getTransitFile() {
		return transitFile;
	}

	public void setTransitFile(String transitFile) {
		this.transitFile = transitFile;
	}

	public int getAnalysisTimeSlice() {
		return analysisTimeSlice;
	}

	public void setAnalysisTimeSlice(int analysisTimeSlice) {
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

	public TransitSchedule getTransitSchedule() {
		return transitSchedule;
	}

}
