package playground.sebhoerl.remote_exec.local;

public class LocalConfiguration {
    private String scenarioPath;
    private String outputPath;
    private boolean createDirectoryStructure = true;

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
}
