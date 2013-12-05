package josmMatsimPlugin;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPresetMenu;
import org.openstreetmap.josm.gui.tagging.TaggingPresetReader;
import org.openstreetmap.josm.gui.tagging.TaggingPresetSeparator;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.xml.sax.SAXException;

/**
 * This is the main class for the MATSim plugin.
 * 
 * @author nkuehnel
 * 
 */
public class MATSimPlugin extends Plugin {
    private MATSimAction Action;
    protected static MATSimToggleDialog toggleDialog;

    public MATSimPlugin(PluginInformation info) throws IOException,
	    SAXException {
	super(info);
	Action = new MATSimAction();
	Main.main.menu.toolsMenu.add(Action);

	Reader reader = new InputStreamReader(this
		.getPluginResourceClassLoader().getResourceAsStream(
			"resources/matsimPreset.xml"));

	Collection<TaggingPreset> tps;
	try {
	    tps = TaggingPresetReader.readAll(reader, true);
	} catch (SAXException e) {
	    e.printStackTrace();
	    tps = Collections.EMPTY_LIST;
	}

	for (TaggingPreset tp : tps) {
	    if (!(tp instanceof TaggingPresetSeparator)) {
		Main.toolbar.register(tp);
	    }
	}
	

	AutoCompletionManager.cachePresets(tps);
	HashMap<TaggingPresetMenu, JMenu> submenus = new HashMap<TaggingPresetMenu, JMenu>();
	for (final TaggingPreset p : tps) {
	    JMenu m = p.group != null ? submenus.get(p.group)
		    : Main.main.menu.presetsMenu;
	    if (p instanceof TaggingPresetSeparator) {
		m.add(new JSeparator());
	    } else if (p instanceof TaggingPresetMenu) {
		JMenu submenu = new JMenu(p);
		submenu.setText(p.getLocaleName());
		((TaggingPresetMenu) p).menu = submenu;
		submenus.put((TaggingPresetMenu) p, submenu);
		m.add(submenu);
	    } else {
		JMenuItem mi = new JMenuItem(p);
		mi.setText(p.getLocaleName());
		m.add(mi);
	    }
	}

    }

    /**
     * Called when the JOSM map frame is created or destroyed.
     */
    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
	if (oldFrame == null && newFrame != null) { // map frame added
	    toggleDialog = new MATSimToggleDialog();
	    Main.map.addToggleDialog(toggleDialog);
	    MapView.addLayerChangeListener(toggleDialog);

//	    URL url = getPluginResourceClassLoader().getResource(
//		    "resources/matsimStyle.mapcss");
//
//	    MapCSSStyleSource style = new MapCSSStyleSource(url.getPath(),
//		    "matsim_style", "MATSim style");
//	    
//	    if (!MapPaintStyles.getStyles().getStyleSources().contains(style)) {
//		MapPaintStyles.addStyle(style);
//	    }
	    
	    
	}
    }

    @Override
    public PreferenceSetting getPreferenceSetting() {
	return null;
    }
}
