package playground.clruch.export;

import java.io.File;
import java.util.Map;
import java.util.NavigableMap;

/**
 * Created by Claudio on 1/26/2017.
 */
abstract class AbstractEventXML<Type> {
    public abstract void generate(Map<String, NavigableMap<Double, Type>> waitStepFctn, File file);

}
