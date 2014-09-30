package playground.juliakern.responsibilityOffline;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;

import playground.juliakern.distribution.ResponsibilityEvent;

public class ResponsibilityEventWriter implements EventWriter, BasicEventHandler{

	private BufferedWriter out;
	
	public ResponsibilityEventWriter(String outPutPath) {
		super();
		try {
			this.out = new BufferedWriter(new FileWriter(outPutPath));
			this.out.write("<events> \n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(Event event) {
		try {
			this.out.append("\t<event ");
			Map<String, String> attr = event.getAttributes();
			for (Map.Entry<String, String> entry : attr.entrySet()) {
				this.out.append(entry.getKey());
				this.out.append("=\"");
				this.out.append(encodeAttributeValue(entry.getValue()));
				this.out.append("\" ");
			}
			this.out.append(" />\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void handleResponsibilityEvent(ResponsibilityEvent event){
		try {
			this.out.append("\t<event ");
//			Map<String, String> attr = event.getAttributes();
			Map<String, String> attr = event.getInformationMap();			
			
			for (Map.Entry<String, String> entry : attr.entrySet()) {
				this.out.append(entry.getKey());
				this.out.append("=\"");
				this.out.append(encodeAttributeValue(entry.getValue()));
				this.out.append("\" ");
			}
			this.out.append(" />\n");
			//this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private CharSequence encodeAttributeValue(final String attributeValue) {
		if (attributeValue == null) {
			return null;
		}
		int len = attributeValue.length();
		boolean encode = false;
		for (int pos = 0; pos < len; pos++) {
			char ch = attributeValue.charAt(pos);
			if (ch == '<') {
				encode = true;
				break;
			} else if (ch == '>') {
				encode = true;
				break;
			} else if (ch == '\"') {
				encode = true;
				break;
			} else if (ch == '&') {
				encode = true;
				break;
			}
		}
		if (encode) {
			StringBuffer bf = new StringBuffer();
			for (int pos = 0; pos < len; pos++) {
				char ch = attributeValue.charAt(pos);
				if (ch == '<') {
					bf.append("&lt;");
				} else if (ch == '>') {
					bf.append("&gt;");
				} else if (ch == '\"') {
					bf.append("&quot;");
				} else if (ch == '&') {
					bf.append("&amp;");
				} else {
					bf.append(ch);
				}
			}
			
			return bf.toString();
		}
		return attributeValue;

	}

	@Override
	public void closeFile() {
		if (this.out != null)
			try {
				this.out.write("</events>");
				// I added a "\n" to make it look nicer on the console.  Can't say if this may have unintended side
				// effects anywhere else.  kai, oct'12
				// fails signalsystems test (and presumably other tests in contrib/playground) since they compare
				// checksums of event files.  Removed that change again.  kai, oct'12
				this.out.close();
				this.out = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		
	}

}
