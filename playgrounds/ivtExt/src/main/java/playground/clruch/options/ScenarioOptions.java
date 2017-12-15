package playground.clruch.options;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import playground.clruch.data.LocationSpec;
import playground.clruch.data.ReferenceFrame;
import playground.lsieber.networkshapecutter.LinkModes;
import playground.lsieber.networkshapecutter.NetworkCutters;
import playground.lsieber.networkshapecutter.PopulationCutters;
import playground.lsieber.networkshapecutter.VirtualNetworkCreators;

/** @author Claudio Ruch */
// TODO make sure only PropertiesExt visible in Server, Viewer, Preparer.
public class ScenarioOptions {

    private final Properties properties;

    public static ScenarioOptions load(File workingDirectory) throws IOException {
        Properties properties = ScenarioOptionsBase.load(workingDirectory);
        return new ScenarioOptions(properties);
    }

    private ScenarioOptions(Properties properties) {
        this.properties = properties;
    }

    // specific access functions ==============================================
    public String getSimulationConfigName() {
        return getString(ScenarioOptionsBase.SIMUCONFIGIDENTIFIER);
    }

    public String getPreparerConfigName() {
        return getString(ScenarioOptionsBase.FULLCONFIGIDENTIFIER);
    }

    public String getVirtualNetworkName() {
        return getString(ScenarioOptionsBase.VIRTUALNETWORKNAMEIDENTIFIER);
    }

    public int getNumVirtualNodes() {
        return getInt(ScenarioOptionsBase.NUMVNODESIDENTIFIER);
    }

    public boolean isCompleteGraph() {
        return getBoolean(ScenarioOptionsBase.COMPLETEGRAPHIDENTIFIER);
    }

    public String getTravelDataName() {
        return getString(ScenarioOptionsBase.TRAVELDATAFILENAME);
    }

    public String getMinFleetName() {
        return getString(ScenarioOptionsBase.MINIMUMFLEETSIZEFILENAME);
    }

    public String getPerformFleetName() {
        return getString(ScenarioOptionsBase.PERFORMANCEFLEETSIZEFILENAME);
    }

    public ReferenceFrame getReferenceFrame() {
        return ReferenceFrame.fromString(properties.getProperty(ScenarioOptionsBase.REFFERENCEFRAMEIDENTIFIER));
    }

    public LocationSpec getLocationSpec() {
        return LocationSpec.fromString(properties.getProperty(ScenarioOptionsBase.LOCATIONSPECIDENTIFIER));
    }

    public int getdtTravelData() {
        return getInt(ScenarioOptionsBase.DTTRAVELDATAIDENTIFIER);
    }

    public boolean calculatePerfFleetSize() {
        return getBoolean(ScenarioOptionsBase.CALCPERFFLAGID);
    }

    public String getPreparedNetworkName() {
        return getString(ScenarioOptionsBase.NETWORKUPDATEDNAMEIDENTIFIER);
    }

    public String getPreparedPopulationName() {
        return getString(ScenarioOptionsBase.POPULATIONUPDATEDNAMEIDENTIFIER);
    }

    public NetworkCutters getNetworkCutter() {
        return NetworkCutters.valueOf(getString(ScenarioOptionsBase.NETWORKCUTTERIDENTIFIER));
    }

    public PopulationCutters getPopulationCutter() {
        return PopulationCutters.valueOf(getString(ScenarioOptionsBase.POPULATIONCUTTERIDENTIFIER));
    }

    public VirtualNetworkCreators getVirtualNetworkCreator() {
        return VirtualNetworkCreators.valueOf(getString(ScenarioOptionsBase.VIRTUALNETWORKcREATORIDENTIFIER));
    }

    public File getShapeFile() {
        File shapeFile = new File(getString(ScenarioOptionsBase.SHAPEFILEIDENTIFIER));
        if (shapeFile.exists()) {
            return shapeFile;
        } else {
            return null;
        }
    }

    public LinkModes getLinkModes() {
        return new LinkModes(getString(ScenarioOptionsBase.LINKMODESIDENTIFIER));
    }

    public int getMaxPopulationSize() {
        return getInt(ScenarioOptionsBase.MAXPOPULATIONSIZEIDENTIFIER);
    }

    public boolean eliminateFreight() {
        return getBoolean(ScenarioOptionsBase.REMOVEFREIGHTIDENTIFIER);
    }

    public boolean eliminateWalking() {
        return getBoolean(ScenarioOptionsBase.REMOVEWALKINGIDENTIFIER);
    }

    public boolean changeModeToAV() {
        return getBoolean(ScenarioOptionsBase.CHANGEMODETOAVIDENTIFIER);
    }

    public boolean cleanNetwork(){
        return getBoolean(ScenarioOptionsBase.NETWORKCLEANERIDENTIFIER);
    }
    
    public boolean removeShortLinks(){
        return getBoolean(ScenarioOptionsBase.NETWORKREMOVESHORTLINKS);
    }
    
    // base access functions ==================================================
    public String getString(String key) {
        return properties.getProperty(key);
    }

    public boolean getBoolean(String key) {
        return Boolean.valueOf(properties.getProperty(key));
    }

    public int getInt(String key) {
        return Integer.valueOf(properties.getProperty(key));
    }

    @Deprecated // TODO this is not useful ad functionality inherent to Properties class
    public String getString(String key, String standard) {
        if (!properties.containsKey(key)) {
            return standard;
        }
        return properties.getProperty(key);
    }

    @Deprecated // TODO this is not useful ad functionality inherent to Properties class
    public boolean getBoolean(String key, boolean standard) {
        if (!properties.containsKey(key)) {
            return standard;
        }
        return Boolean.valueOf(properties.getProperty(key));
    }
}
