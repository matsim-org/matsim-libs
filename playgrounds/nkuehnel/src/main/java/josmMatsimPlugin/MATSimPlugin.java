package josmMatsimPlugin;

import java.util.ArrayList;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * This is the main class for the MATSim plugin.
 * 
 */
public class MATSimPlugin extends Plugin{
    
	private MATSimExportAction exportAction;
	
    public MATSimPlugin(PluginInformation info) {
        super(info);
        exportAction = new MATSimExportAction();
        Main.main.menu.toolsMenu.add(exportAction);
        System.out.println(getPluginDir());
    }
    
    /**
     * Called when the JOSM map frame is created or destroyed. 
     */
    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {             
        if (oldFrame == null && newFrame != null) { // map frame added
        	
        	
        }
    }

    @Override
    public PreferenceSetting getPreferenceSetting() {
		return null;
    	
    }
}
