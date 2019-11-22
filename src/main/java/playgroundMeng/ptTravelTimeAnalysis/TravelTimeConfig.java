package playgroundMeng.ptTravelTimeAnalysis;

public class TravelTimeConfig {
	
	private String configFile = null;
	private String networkFile = null;
	private String transitFile = null;
	private String eventFile =  null;
	private String outputDirectory = null;
	private String analysisNetworkFile = null;
	
	int numOfThread = 1;
	int timeSlice = 900;
	int gridSlice = 1000;
	
	private int beginnTime = 0;
	private int EndTime = 0;
	
	public int getGridSlice() {
		return gridSlice;
	}
	
	public void setGridSlice(int gridSlice) {
		this.gridSlice = gridSlice;
	}
	
	public void setBeginnTime(int beginnTime) {
		this.beginnTime = beginnTime;
	}
	
	public void setEndTime(int endTime) {
		EndTime = endTime;
	}
	
	public int getBeginnTime() {
		return beginnTime;
	}
	
	public int getEndTime() {
		return EndTime;
	}
	
	public void setTimeSlice(int timeSlice) {
		this.timeSlice = timeSlice;
	}
	
	public int getTimeSlice() {
		return timeSlice;
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

	public String getTransitFile() {
		return transitFile;
	}

	public void setTransitFile(String transitFile) {
		this.transitFile = transitFile;
	}

	public String getEventFile() {
		return eventFile;
	}

	public void setEventFile(String eventFile) {
		this.eventFile = eventFile;
	}

	public String getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public int getNumOfThread() {
		return numOfThread;
	}

	public void setNumOfThread(int numOfThread) {
		this.numOfThread = numOfThread;
	}

	public void setAnalysisNetworkFile(String analysisNetworkFile) {
		this.analysisNetworkFile = analysisNetworkFile;
	}
	public String getAnalysisNetworkFile() {
		return analysisNetworkFile;
	}
	


}
