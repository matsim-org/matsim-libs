package playground.vsp.matsimToPostgres;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

public class PostgresExporterConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "postgresExporter" ;

    private static final String DB_PARAM_FILE = "dbParamFile";
    private static final String OVERWRITE_RUN = "overwriteRun";

    public String dbParamFile;
    public enum OverwriteRunSettings { failIfRunIdExists, overwriteExistingRunId }
    private OverwriteRunSettings overwriteRunSettings = OverwriteRunSettings.failIfRunIdExists ;

    private PostgresExporterConfigGroup.InitialDataSettings initialDataSettings;

    public PostgresExporterConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(DB_PARAM_FILE, "xml-File containing database host, database, user, password, port" );
        comments.put(OVERWRITE_RUN, "Possible values: failIfRunIdExists, overwriteExistingRunId" );

        return comments;
    }

    @StringGetter(DB_PARAM_FILE)
    public String getDbParamFile(){
        return this.dbParamFile;
    }

    @StringSetter(DB_PARAM_FILE)
    public void setDbParamFile(String dbParamFile){
        this.dbParamFile = dbParamFile;
    }

    @StringGetter(OVERWRITE_RUN)
    public OverwriteRunSettings getOverwriteRun(){
        return this.overwriteRunSettings;
    }

    @StringSetter(OVERWRITE_RUN)
    public void setOverwriteRun (OverwriteRunSettings overwriteRunSettings){
        this.overwriteRunSettings = overwriteRunSettings;
    }


    @Override
    public ConfigGroup createParameterSet(String type) {
        if (PostgresExporterConfigGroup.InitialDataSettings.TYPE.equals(type)) {
            return new PostgresExporterConfigGroup.InitialDataSettings();
        } else {
            throw new IllegalArgumentException("Unsupported parameterset-type: " + type);
        }
    }


    public void addParameterSet(ConfigGroup cfg) {
        if (cfg instanceof PostgresExporterConfigGroup.InitialDataSettings) {
            this.setInitialDataSettings((InitialDataSettings)cfg);
        } else {
            throw new IllegalArgumentException("Unsupported parameterset: " + cfg.getClass().getName());
        }
    }


    public PostgresExporterConfigGroup.InitialDataSettings setInitialDataSettings(PostgresExporterConfigGroup.InitialDataSettings cfg) {
        PostgresExporterConfigGroup.InitialDataSettings old = getInitialDataSettings();
        this.initialDataSettings = cfg;
        super.addParameterSet(cfg);
        return old;
    }


    public PostgresExporterConfigGroup.InitialDataSettings getInitialDataSettings() {
        return this.initialDataSettings;
    }


    public static class InitialDataSettings extends ReflectiveConfigGroup {
        public static final String TYPE = "initialData";

        private static final String IMPORT_MODE = "importMode";
        private static final String AGGREGATION_AREAS_FILE = "aggregationAreasFile";

        public enum InitialImportModeSettings { importOverwrite, importIfNotExist, NoImport }
        private PostgresExporterConfigGroup.InitialDataSettings.InitialImportModeSettings modeSettings = PostgresExporterConfigGroup.InitialDataSettings.InitialImportModeSettings.importIfNotExist ;
        public String aggregationAreasFile;

        public InitialDataSettings() {
            super(TYPE);
        }

        @Override
        public Map<String, String> getComments() {
            Map<String, String> comments = super.getComments();
            comments.put(IMPORT_MODE, "Possible values: importOverwrite, importIfNotExist, NoImport");
            comments.put(AGGREGATION_AREAS_FILE, "csv-File containing geo data of aggregation areas such as counties, communities, hexagons etc...");

            return comments;
        }


        @StringGetter(IMPORT_MODE)
        public InitialImportModeSettings getInitialImportModeSettings(){
            return this.modeSettings;
        }

        @StringSetter(IMPORT_MODE)
        public void setInitialImportModeSettings(InitialImportModeSettings modeSettings){
            this.modeSettings = modeSettings;
        }

        @StringGetter(AGGREGATION_AREAS_FILE)
        public String getAggregationAreasFile(){
            return this.aggregationAreasFile;
        }

        @StringSetter(AGGREGATION_AREAS_FILE)
        public void setAggregationAreasFile(String aggregationAreasFile){
            this.aggregationAreasFile = aggregationAreasFile;
        }

    }






}
