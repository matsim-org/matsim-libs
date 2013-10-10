package josmMatsimPlugin;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * This is the main class for the MATSim plugin.
 * @author nkuehnel
 * 
 */
public class MATSimPlugin extends Plugin{
    
	private MATSimAction Action;
	
    public MATSimPlugin(PluginInformation info) {
        super(info);
        Action = new MATSimAction();
        Main.main.menu.toolsMenu.add(Action);
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
