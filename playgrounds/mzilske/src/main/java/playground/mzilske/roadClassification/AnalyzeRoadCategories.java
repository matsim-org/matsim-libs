package playground.mzilske.roadClassification;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import playground.mzilske.cdr.BerlinRunUncongested3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by michaelzilske on 08/10/15.
 */
public class AnalyzeRoadCategories {

    final static String BERLIN_PATH = "/Users/michaelzilske/shared-svn/studies/countries/de/berlin/";

    public static void main(String[] args) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        ObjectAttributes linkAttributes = new ObjectAttributes();
        new MatsimNetworkReader(scenario).readFile(BERLIN_PATH + "counts/iv_counts/network.xml.gz");
        Set<RoadCategoryClusterableLink> rcs = new HashSet<>();
        for (Link link : scenario.getNetwork().getLinks().values()) {
            RoadCategoryClusterableLink rc = new RoadCategoryClusterableLink(link);
            rcs.add(rc);
        }
        List<CentroidCluster<RoadCategoryClusterableLink>> cluster = new KMeansPlusPlusClusterer<RoadCategoryClusterableLink>(5).cluster(rcs);
        int i = 0;
        for (CentroidCluster<RoadCategoryClusterableLink> roadCategoryCentroidCluster : cluster) {
            for (RoadCategoryClusterableLink roadCategoryClusterableLink : roadCategoryCentroidCluster.getPoints()) {
                linkAttributes.putAttribute(roadCategoryClusterableLink.link.getId().toString(), "roadCategory", i);
            }
            System.out.println(roadCategoryCentroidCluster.getCenter());
            ++i;
        }
        new ObjectAttributesXmlWriter(linkAttributes).writeFile("output/berlin/road-categories/road-categories.xml");
    }

    private static class RoadCategoryClusterableLink implements Clusterable {

        private Link link;

        public RoadCategoryClusterableLink(Link link) {
            this.link = link;
        }

        @Override
        public double[] getPoint() {
            return new double[] {link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes()};
        }
    }
}
