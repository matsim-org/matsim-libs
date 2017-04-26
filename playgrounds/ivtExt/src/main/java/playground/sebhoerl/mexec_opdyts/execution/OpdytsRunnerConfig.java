package playground.sebhoerl.mexec_opdyts.execution;

public class OpdytsRunnerConfig {
    private long maximumNumberOfIterations = 100;
    private long maximumNumberOfTransitions = 1000;
    private long matsimStepsPerTransition = 10;

    private long candidatePoolSize = 10;

    private boolean interpolate = true;
    private boolean includeCurrentBest = false;

    private String simulationPrefix = "opdyts_";
    private String cachePath = null;

    private String logPath = null;

    private double selfTunerInertia;

    public long getMaximumNumberOfIterations() {
        return maximumNumberOfIterations;
    }

    public void setMaximumNumberOfIterations(long maximumNumberOfIterations) {
        this.maximumNumberOfIterations = maximumNumberOfIterations;
    }

    public long getMaximumNumberOfTransitions() {
        return maximumNumberOfTransitions;
    }

    public void setMaximumNumberOfTransitions(long maximumNumberOfTransitions) {
        this.maximumNumberOfTransitions = maximumNumberOfTransitions;
    }

    public long getMatsimStepsPerTransition() {
        return matsimStepsPerTransition;
    }

    public void setMatsimStepsPerTransition(long matsimStepsPerTransition) {
        this.matsimStepsPerTransition = matsimStepsPerTransition;
    }

    public long getCandidatePoolSize() {
        return candidatePoolSize;
    }

    public void setCandidatePoolSize(long candidatePoolSize) {
        this.candidatePoolSize = candidatePoolSize;
    }

    public boolean isInterpolate() {
        return interpolate;
    }

    public void setInterpolate(boolean interpolate) {
        this.interpolate = interpolate;
    }

    public boolean isIncludeCurrentBest() {
        return includeCurrentBest;
    }

    public void setIncludeCurrentBest(boolean includeCurrentBest) {
        this.includeCurrentBest = includeCurrentBest;
    }

    public String getSimulationPrefix() {
        return simulationPrefix;
    }

    public void setSimulationPrefix(String simulationPrefix) {
        this.simulationPrefix = simulationPrefix;
    }

    public double getSelfTunerInertia() {
        return selfTunerInertia;
    }

    public void setSelfTunerInertia(double selfTunerInertia) {
        this.selfTunerInertia = selfTunerInertia;
    }

    public String getCachePath() {
        return cachePath;
    }

    public void setCachePath(String cachePath) {
        this.cachePath = cachePath;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }
}
