package josmMatsimPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.StyleKeys;
import org.openstreetmap.josm.gui.mappaint.StyleSource;

public class MATSimStyleSource extends StyleSource implements StyleKeys {

    public MATSimStyleSource(String url, String name, String title) {
	super(url, name, title);
	// TODO Auto-generated constructor stub
    }

    @Override
    public void apply(MultiCascade arg0, OsmPrimitive arg1, double arg2,
	    OsmPrimitive arg3, boolean arg4) {
	// TODO Auto-generated method stub

    }

    @Override
    public InputStream getSourceInputStream() throws IOException {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public void loadStyleSource() {
	// TODO Auto-generated method stub

    }

}
