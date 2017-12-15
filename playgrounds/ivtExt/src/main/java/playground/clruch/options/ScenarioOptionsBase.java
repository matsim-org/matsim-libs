/**
 * 
 */
package playground.clruch.options;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

//TODO comment this file
/** @author Claudio Ruch */
public enum ScenarioOptionsBase {
    ;

    private static final String OPTIONSFILENAME = "IDSCOptions.properties";
    /* package */ static final String FULLCONFIGIDENTIFIER = "fullConfig";
    /* package */ static final String SIMUCONFIGIDENTIFIER = "simuConfig";
    /* package */ static final String REFFERENCEFRAMEIDENTIFIER = "ReferenceFrame";
    /* package */ static final String LOCATIONSPECIDENTIFIER = "LocationSpec";
    /* package */ static final String VIRTUALNETWORKNAMEIDENTIFIER = "virtualNetwork";
    /* package */ static final String TRAVELDATAFILENAME = "travelDataFileName";
    /* package */ static final String MINIMUMFLEETSIZEFILENAME = "minimumFleetSizeFileName";
    /* package */ static final String PERFORMANCEFLEETSIZEFILENAME = "performanceFleetSizeFileName";
    /* package */ static final String DTTRAVELDATAIDENTIFIER = "dtTravelData";
    /* package */ static final String CALCPERFFLAGID = "calculatePerformanceFleetSize";
    /* package */ static final String NUMVNODESIDENTIFIER = "numVirtualNodes";
    /* package */ static final String COMPLETEGRAPHIDENTIFIER = "completeGraph";
    /* package */ static final String NETWORKUPDATEDNAMEIDENTIFIER = "NetworkUpdateName";
    /* package */ static final String POPULATIONUPDATEDNAMEIDENTIFIER = "PopulationUpdateName";
    /* package */ static final String NETWORKCUTTERIDENTIFIER = "networkCutter";
    /* package */ static final String POPULATIONCUTTERIDENTIFIER = "populationCutter";
    /* package */ static final String SHAPEFILEIDENTIFIER = "shapeFile";
    /* package */ static final String LINKMODESIDENTIFIER = "linkModes";
    /* package */ static final String MAXPOPULATIONSIZEIDENTIFIER = "maxPopulationSize";
    /* package */ static final String REMOVEFREIGHTIDENTIFIER = "populationeliminateFreight";
    /* package */ static final String REMOVEWALKINGIDENTIFIER = "populationeliminateWalking";
    /* package */ static final String CHANGEMODETOAVIDENTIFIER = "populationchangeModeToAV";
    /* package */ static final String VIRTUALNETWORKcREATORIDENTIFIER = "virtualNetworkCreator";
    /* package */ static final String NETWORKCLEANERIDENTIFIER = "networkCleaner";
    /* package */ static final String NETWORKREMOVESHORTLINKS = "networkRemoveShortLinks";

    public static Properties getDefault() {
        Properties returnP = new Properties();
        returnP.setProperty(FULLCONFIGIDENTIFIER, "av_config_full.xml");
        returnP.setProperty(SIMUCONFIGIDENTIFIER, "av_config.xml");
        returnP.setProperty(MAXPOPULATIONSIZEIDENTIFIER, "2000");
        returnP.setProperty(NUMVNODESIDENTIFIER, "10");
        returnP.setProperty(DTTRAVELDATAIDENTIFIER, "3600");
        returnP.setProperty(COMPLETEGRAPHIDENTIFIER, "true");
        returnP.setProperty(REMOVEFREIGHTIDENTIFIER, "false");
        returnP.setProperty(REMOVEWALKINGIDENTIFIER, "false");
        returnP.setProperty(CHANGEMODETOAVIDENTIFIER, "true");
        returnP.setProperty("waitForClients", "false");
        returnP.setProperty(VIRTUALNETWORKNAMEIDENTIFIER, "virtualNetwork");
        returnP.setProperty(TRAVELDATAFILENAME, "travelData");
        returnP.setProperty(CALCPERFFLAGID, "true");
        returnP.setProperty(MINIMUMFLEETSIZEFILENAME, "minimumFleetSizeCalculator");
        returnP.setProperty(PERFORMANCEFLEETSIZEFILENAME, "performanceFleetSizeCalculator");
        returnP.setProperty(NETWORKUPDATEDNAMEIDENTIFIER, "networkPrepared");
        returnP.setProperty(POPULATIONUPDATEDNAMEIDENTIFIER, "populationPrepared");
        returnP.setProperty(NETWORKCUTTERIDENTIFIER, "NONE");
        returnP.setProperty(SHAPEFILEIDENTIFIER, "AbsoluteShapeFileName");
        returnP.setProperty(LINKMODESIDENTIFIER, "all");
        returnP.setProperty(POPULATIONCUTTERIDENTIFIER, "NETWORKBASED");
        returnP.setProperty(VIRTUALNETWORKcREATORIDENTIFIER, "KMEANS");
        returnP.setProperty(NETWORKCLEANERIDENTIFIER, "true");
        returnP.setProperty(NETWORKREMOVESHORTLINKS, "true");
        return returnP;
    }

    public static void saveDefault() throws IOException {
        saveProperties(getDefault(), OPTIONSFILENAME);
    }

    private static void saveProperties(Properties prop, String filename) throws IOException {
        File defaultFile = new File(filename);
        try (FileOutputStream ostream = new FileOutputStream(defaultFile)) {
            prop.store(ostream,
                    "This is a default config file that needs to be modified. \n "
                            + "The parameters ReferenceFrame and LocationSpec need to be set in order to simulate,"
                            + " e.g., ReferenceFrame=SIOUXFALLS, LocationSpec=SIOUXFALLS_CITY ");
        }
    }

    /** @param workingDirectory with simulation data an dan IDSC.Options.properties file
     * @return Properties object with default options and any options found in folder
     * @throws IOException */
    /* package */ static Properties load(File workingDirectory) throws IOException {
        System.out.println("working in directory \n" + workingDirectory.getCanonicalFile());

        Properties simOptions = new Properties(getDefault());
        File simOptionsFile = new File(workingDirectory, OPTIONSFILENAME);
        if (simOptionsFile.exists()) {
            simOptions.load(new FileInputStream(simOptionsFile));
        } else
            ScenarioOptionsBase.saveDefault();

        return simOptions;
    }

    public static String getOptionsFileName() {
        return OPTIONSFILENAME;
    }

}
