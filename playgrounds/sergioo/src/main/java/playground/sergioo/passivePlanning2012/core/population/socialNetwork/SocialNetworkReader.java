package playground.sergioo.passivePlanning2012.core.population.socialNetwork;

import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.sergioo.passivePlanning2012.core.scenario.ScenarioSocialNetwork;

public class SocialNetworkReader extends MatsimXmlParser {

	private final static String RELATION = "relation";
	private final static String SOCIAL_NETWORK = "social_network";
	
	private final SocialNetwork socialNetwork;
	private final Scenario scenario;

	public SocialNetworkReader(final Scenario scenario) {
		super();
		this.scenario = scenario;
		this.socialNetwork = ((ScenarioSocialNetwork)scenario).getSocialNetwork();
	}
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (RELATION.equals(name)) {
			startRelation(atts);
		} else if (SOCIAL_NETWORK.equals(name)) {
			startSocialNetwork(atts);
		}
	}
	private void startSocialNetwork(Attributes atts) {
		if (atts.getValue("desc") != null) {
			this.socialNetwork.setDescription(atts.getValue("desc"));
		}
	}
	private void startRelation(Attributes atts) {
		String type = atts.getValue("type");
		if(type==null)
			socialNetwork.relate(Id.create(atts.getValue("id_ego"), Person.class), Id.create(atts.getValue("id_alter"), Person.class));
		else
			socialNetwork.relate(Id.create(atts.getValue("id_ego"), Person.class), Id.create(atts.getValue("id_alter"), Person.class), type);
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		
	}

}
