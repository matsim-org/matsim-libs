package playground.gregor.grips.config;

import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.core.config.Module;

public class GripsConfigModule extends Module {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "grips";

	public static final String NETWORK_FILE_NAME = "networkFile";

	public static final String EVACUATION_AREA_FILE_NAME = "evacuationAreaFile";

	public static final String POPULATION_FILE_NAME = "populationFile";

	public static final String OUTPUT_DIR = "outputDir";

	//Do we really want to define it in the GripsConfigModule?
	@Deprecated
	public static final String SAMPLE_SIZE = "sampleSize";

	private String networkFileName;

	private String evacuationAreaFileName;

	private String populationFileName;

	private String outputDir;

	//Do we really want to define it in the GripsConfigModule?
	@Deprecated
	private double sampleSize = 1;



	public GripsConfigModule(String name) {
		super(name);
	}

	public GripsConfigModule(Module grips) {
		super(GROUP_NAME);
		for (Entry<String, String> e : grips.getParams().entrySet()) {
			addParam(e.getKey(), e.getValue());
		}
	}

	@Override
	public void addParam(String param_name, String value) {
		if (param_name.equals(NETWORK_FILE_NAME)) {
			setNetworkFileName(value);
		} else if (param_name.equals(EVACUATION_AREA_FILE_NAME)) {
			setEvacuationAreaFileName(value);
		} else if (param_name.equals(POPULATION_FILE_NAME)){
			setPopulationFileName(value);
		} else if (param_name.equals(OUTPUT_DIR)){
			setOutputDir(value);
		} else if (param_name.equals(SAMPLE_SIZE)){
			setSampleSize(value);
		}else {
			throw new IllegalArgumentException(param_name);
		}
	}

	public String getOutputDir() {
		return this.outputDir;
	}

	public void setOutputDir(String outputDir) {
		this.outputDir = outputDir;
	}

	@Override
	public String getValue(String param_name) {
		if (param_name.equals(NETWORK_FILE_NAME)) {
			return getNetworkFileName();
		} else if (param_name.equals(EVACUATION_AREA_FILE_NAME)) {
			return getEvacuationAreaFileName();
		} else if (param_name.equals(POPULATION_FILE_NAME)){
			return getPopulationFileName();
		} else if (param_name.equals(OUTPUT_DIR)){
			return getOutputDir();
		}else if (param_name.equals(SAMPLE_SIZE)){
			return Double.toString(getSampleSize());
		}else {
			throw new IllegalArgumentException(param_name);
		}
	}

	@Override
	public Map<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(NETWORK_FILE_NAME, getValue(NETWORK_FILE_NAME));
		map.put(EVACUATION_AREA_FILE_NAME, getValue(EVACUATION_AREA_FILE_NAME));
		map.put(POPULATION_FILE_NAME, getValue(POPULATION_FILE_NAME));
		map.put(OUTPUT_DIR, getValue(OUTPUT_DIR));
		map.put(SAMPLE_SIZE, getValue(SAMPLE_SIZE));
		return map;
	}

	public String getNetworkFileName() {
		return this.networkFileName;
	}

	public void setNetworkFileName(String name) {
		this.networkFileName = name;
	}

	public String getEvacuationAreaFileName() {
		return this.evacuationAreaFileName;
	}

	public void setEvacuationAreaFileName(String evacuationAreaFileName) {
		this.evacuationAreaFileName = evacuationAreaFileName;
	}

	public String getPopulationFileName() {
		return this.populationFileName;
	}

	public void setPopulationFileName(String populationFileName) {
		this.populationFileName = populationFileName;
	}

	//Do we really want to define it in the GripsConfigModule?
	@Deprecated
	public double getSampleSize() {
		return this.sampleSize;
	}

	//Do we really want to define it in the GripsConfigModule?
	@Deprecated
	public void setSampleSize(String sampleSize) {
		this.sampleSize = Double.parseDouble(sampleSize);
	}
}
