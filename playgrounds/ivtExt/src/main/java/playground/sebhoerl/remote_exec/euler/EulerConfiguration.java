package playground.sebhoerl.remote_exec.euler;

public class EulerConfiguration {
    private String scenarioPath;
    private String outputPath;
    private boolean createDirectoryStructure = true;
    private String jobPrefix = "matsim_";

    public String getScenarioPath() {
        return scenarioPath;
    }

    public void setScenarioPath(String scenarioPath) {
        this.scenarioPath = scenarioPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public boolean getCreateDirectoryStructure() {
        return createDirectoryStructure;
    }

    public void setCreateDirectoryStructure(boolean createDirectoryStructure) {
        this.createDirectoryStructure = createDirectoryStructure;
    }

    public String getJobPrefix() {
        return jobPrefix;
    }

    public void setJobPrefix(String jobPrefix) {
        this.jobPrefix = jobPrefix;
    }
}
