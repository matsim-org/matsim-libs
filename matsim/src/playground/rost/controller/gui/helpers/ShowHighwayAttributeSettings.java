package playground.rost.controller.gui.helpers;

import java.awt.GridLayout;
import java.io.IOException;

import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.xml.bind.JAXBException;

import playground.rost.osm2matconverter.HighwayAttributeMapping;
import playground.rost.util.PathTracker;

public class ShowHighwayAttributeSettings extends JInternalFrame {

	public ShowHighwayAttributeSettings()
	{
		super("Highway Attribute Settings", true, true, true, true);
		HighwayAttributeMapping hAMap = null;
		try {
			hAMap = HighwayAttributeMapping.readXMLFile(PathTracker.resolve("highwayMappingDefault"));
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.setLayout(new GridLayout(0,2));
		for(String str : hAMap.highwayMapping.keySet())
		{
			JLabel key = new JLabel(str);
			this.add(key);
			JLabel value = new JLabel(hAMap.highwayMapping.get(str).getWidth().toString());
			this.add(value);
		}
		this.setSize(300, 400);
		this.setVisible(true);
		
		
	}
}
