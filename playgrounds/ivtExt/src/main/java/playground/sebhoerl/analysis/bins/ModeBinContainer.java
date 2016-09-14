package playground.sebhoerl.analysis.bins;

import java.util.ArrayList;
import java.util.List;

public class ModeBinContainer<Data> extends BinContainer<ModeBin<Data>, Data> {
    final private ArrayList<String> modes = new ArrayList<String>();
    
    public ModeBinContainer(final List<String> modes, final BinDataFactory<Data> factory) {
        super(modes.size(), new BinFactory<ModeBin<Data>, Data>() {
            @Override
            public ModeBin<Data> createBin(int index) {
                return new ModeBin<Data>(modes.get(index), factory.createData());
            }
        });
        
        this.modes.addAll(modes);
    }
    
    public int getIndexByMode(String mode) {
        return modes.indexOf(mode);
    }
    
    public ModeBin<Data> getBinByMode(String mode) {
        return getBinByIndex(getIndexByMode(mode));
    }
}
