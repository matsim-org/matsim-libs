package playground.toronto.transitfares;

import java.util.HashMap;
import java.util.Stack;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

/**
 * Loader for the fare lookup table, which matches zone-to-zone transfers, by agent
 * fare class, to a corresponding monetary fare.
 * @author pkucirek
 */

public class FareLookupTableXmlReader extends MatsimXmlParser{
	
	static public final String FARECLASS = "fareclass";
	static public final String DATAPOINT = "tuple";
	
	private String current = "";
		
	public HashMap<String, HashMap<Tuple<String,String>, Double>> farelookuptable;
	
	public FareLookupTableXmlReader(HashMap<String, HashMap<Tuple<String,String>, Double>> farelookuptable){
		this.farelookuptable = farelookuptable;
		this.setValidating(false);
	}
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if(name.equals(FARECLASS))
		{
			this.current = atts.getValue("name");
			this.farelookuptable.put(this.current, new HashMap<Tuple<String,String>, Double>());
		}
		else if(name.equals(DATAPOINT))
		{
			this.farelookuptable.get(current).put(
					new Tuple<String,String>(atts.getValue("fromZone"),atts.getValue("toZone")),
					Double.parseDouble(atts.getValue("fare")));
		}
	}
	
	@Override
	public void endTag(String name, String content, Stack<String> context) {	
	}
}