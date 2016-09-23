package contrib.baseline.build;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.io.IOUtils;

public class BuildConfig extends ReflectiveConfigGroup  {
	static final public String GROUP_NAME = "build";
	
	private String outputDirectory;
	private String workingDirectory;
	private String svnDirectory;
	
	private Population population = Population.POPULATION_2015;
	private Scaling scaling = Scaling.SCALING_1;
	private Scenario scenario = Scenario.SWITZERLAND;
	
	private boolean hashScenario = false;
	
	private String repository = "https://repos.ivt.ethz.ch/svn/ivt/studies/trunk/baseline2010";
	
	public enum Scaling {
		SCALING_100,
		SCALING_10,
		SCALING_1
	}
	
	public enum Population {
		POPULATION_2015,
		POPULATION_2030
	}
	
	public enum Scenario {
		SWITZERLAND,
		ZURICH
	}
	
	public BuildConfig() {
		super(GROUP_NAME);
	}
	
    @StringSetter( "outputDirectory" )
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
    
    @StringGetter( "outputDirectory" )
    public String getOutputDirectory() {
        return outputDirectory;
    }
    
    @StringSetter( "workingDirectory" )
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
    
    @StringGetter( "workingDirectory" )
    public String getWorkingDirectory() {
        return workingDirectory;
    }
    
    @StringSetter( "svnDirectory" )
    public void setSVNDirectory(String svnDirectory) {
        this.svnDirectory = svnDirectory;
    }
    
    @StringGetter( "svnDirectory" )
    public String getSVNDirectory() {
        return svnDirectory;
    }
    
    @StringSetter( "repository" )
    public void setRepository(String repository) {
        this.repository = repository;
    }
    
    @StringGetter( "repository" )
    public String getRepository() {
        return repository;
    }
    
    @StringSetter( "population" )
    public void setPopulation(Population population) {
        this.population = population;
    }
    
    @StringGetter( "population" )
    public Population getPopulation() {
        return population;
    }
    
    @StringSetter( "scaling" )
    public void setScaling(Scaling scaling) {
        this.scaling = scaling;
    }
    
    @StringGetter( "scaling" )
    public Scaling getScaling() {
        return scaling;
    }
    
    @StringSetter( "scenario" )
    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }
    
    @StringGetter( "scenario" )
    public Scenario getScenario() {
        return scenario;
    }
    
    @StringSetter( "hashScenario" )
    public void setHashScenario(boolean hashScenario) {
        this.hashScenario = hashScenario;
    }
    
    @StringGetter( "hashScenario" )
    public boolean getHashScenario() {
        return hashScenario;
    }
    
    static public BuildConfig loadConfig(String path) {
    	Config config = new Config();
    	config.addModule(new BuildConfig());
    	new ConfigReader(config).parse(IOUtils.getUrlFromFileOrResource(path));
    	return (BuildConfig) config.getModule(GROUP_NAME);
    }
    
    static public void saveConfig(String path, BuildConfig buildConfig) {
    	Config config = new Config();
    	config.addModule(buildConfig);
    	new ConfigWriter(config).write(path);
    }
}
