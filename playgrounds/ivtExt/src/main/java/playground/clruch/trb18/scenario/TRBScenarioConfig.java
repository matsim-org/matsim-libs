package playground.clruch.trb18.scenario;

public class TRBScenarioConfig {
    public double centerX = 2683518.0;
    public double centerY = 1246836.0;
    public double radius = 1000.0;
    public boolean removeBackgroundTraffic = true;
    public boolean allowMultimodalPlans = true;
    public double avShare = 1.0;
    public boolean allowModeChoice = false;

    public String populationOutputPath = "trb_population.xml.gz";
    public String populationAttributesOutputPath = "trb_population_attributes.xml.gz";
    public String fullNetworkOutputPath = "trb_full_network.xml.gz";
    public String filteredNetworkOutputPath = "trb_filtered_network.xml.gz";
    public String populationInputPath = "output_plans.xml.gz";
    public String populationAttributesInputPath = "population_attributes.xml.gz";
    public String networkInputPath = "mmNetwork.xml.gz";

    public String avConfigPath = "av.xml";

    public String configInputPath = "defaultIVTConfig.xml";
    public String configOutputPath = "trb_config.xml";

    public int numberOfVirtualNodes = 40;
    public String virtualNetworkOutputDirectory = "virtualNetwork";
    public String virtualNetworkFileName = "virtualNetwork";
    public int dtTravelData = 300;
    public String travelDataFileName = "travelData";

    public long maximumNumberOfAgents = 200;//Long.MAX_VALUE;

//    public String shapefileInputPath = "stadtkreis/Stadtkreis.shp";
    public String shapefileInputPath = null;
}
