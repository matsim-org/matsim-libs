package playground.jbischoff.taxi.berlin.supply;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import playground.michalm.taxi.data.TaxiRank;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class BerlinTaxiRankCreator
{
    private static final Logger log = Logger.getLogger(BerlinTaxiRankCreator.class);

    private final static String NETWORKFILE = "C:/local_jb/data/network/berlin_brb.xml.gz";
    private final static String RANKFILE = "C:/local_jb/data/network/taxiranks_greaterberlin-1.csv";
    private final static String OUTPUTFILE = "C:/local_jb/data/network/berlin_ranks.xml";
    private final static String OUTPUTFILETXT = "C:/local_jb/data/network/berlin_ranks.csv";
    

    public static void main(String[] args)
    {
        BerlinTaxiRankCreator btrc = new BerlinTaxiRankCreator();
        Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(sc.getNetwork()).readFile(NETWORKFILE);
        List<TaxiRank> rankList = btrc.read(sc.getNetwork(), RANKFILE);
        btrc.writeRanks(rankList, OUTPUTFILE);
        btrc.WriteRanksText(rankList, OUTPUTFILETXT);
        //		new Links2ESRIShape(sc.getNetwork(), OUTPUTSHP, ,
        //				
        //					).write();;
    }


    public List<TaxiRank> read(Network network, String rankFile)
    {
        RankReader rr = new RankReader(network);
        TabularFileParserConfig config = new TabularFileParserConfig();
        log.info("parsing " + rankFile);
        config.setDelimiterTags(new String[] { "\t" });
        config.setFileName(rankFile);
        new TabularFileParser().parse(config, rr);
        log.info("done. (parsing " + rankFile + ")");
        return rr.getRanks();

    }


    public void writeRanks(List<TaxiRank> rankList, String outputFile)
    {

        try {
            FileWriter fw = new FileWriter(new File(outputFile));
            fw.append("<?xml version=\"1.0\" ?>\n<!DOCTYPE ranks SYSTEM \"http://matsim.org/files/dtd/taxi_ranks_v1.dtd\">\n<ranks>\n");
            for (TaxiRank rank : rankList) {
                fw.append("<rank id=\"" + rank.getId().toString() + "\" name=\"" + rank.getName()
                        + "\" link=\"" + rank.getLink().getId().toString() + "\">\n");
                fw.append("</rank>\n");
            }

            fw.append("</ranks>");
            fw.flush();
            fw.close();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public void WriteRanksText(List<TaxiRank> rankList, String outputFile)
    {
        try {
            FileWriter fw = new FileWriter(new File(outputFile));
            fw.append("id\tname\tlink\tx\ty\n");

            for (TaxiRank rank : rankList) {
                fw.append(rank.getId().toString() + "\t" + rank.getName() + "\t"
                        + rank.getLink().getId().toString() + "\t"
                        + rank.getLink().getCoord().getX() + "\t"
                        + rank.getLink().getCoord().getY() + "\n");

            }

            fw.flush();
            fw.close();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}


class RankReader
    implements TabularFileHandler
{
    private final static int RANK_CAPACITY = 100;

    private Network network;
    private List<TaxiRank> ranks = new ArrayList<TaxiRank>();
    private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
            TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);


    RankReader(Network network)
    {
        this.network = (Network)network;
    }


    @Override
    public void startRow(String[] row)
    {
        Link link = NetworkUtils.getNearestRightEntryLink(network, stringtoCoord(row[2], row[1]));
        String name = row[4];
        Id<TaxiRank> id = Id.create(row[5], TaxiRank.class);
        if (id.equals("21"))
            link = network.getLinks().get(Id.create("-35954", Link.class));
        //Exception for Tegel Airport
        TaxiRank rank = new TaxiRank(id, name, link, RANK_CAPACITY);
        ranks.add(rank);
    }


    Coord stringtoCoord(String x, String y)
    {
        String xcoordString = x.substring(2);
        double xc = 13. + Double.parseDouble("0." + xcoordString);
        String ycoordString = y.substring(2);
        double yc = 52. + Double.parseDouble("0." + ycoordString);
        Coord coord = new Coord(xc, yc);
        Coord trans = ct.transform(coord);
        //		System.out.println("Read x"+ x + " Read y "+ y + " coord read "+ coord + " transformed "+trans );
        return trans;
    }


    public List<TaxiRank> getRanks()
    {
        return ranks;
    }

}
