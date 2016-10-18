package playground.sebhoerl.avtaxi.config;

import org.apache.log4j.chainsaw.Main;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import playground.sebhoerl.avtaxi.data.AVOperator;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class AVConfigReader extends MatsimXmlParser {
    final private AVConfig config;

    final static String PARAM = "param";
    final static String AV = "av";
    final static String OPERATOR = "operator";
    final static String TIMING = "timing";
    final static String DISPATCHER = "dispatcher";

    enum State {
        MAIN, OPERATOR, TIMING, DISPATCHER
    }

    private Stack<State> state = new Stack<State>();
    private Stack<ConfigGroup> configs = new Stack<>();

    private AVOperatorConfig operatorConfig;


    public AVConfigReader(AVConfig config) {
        this.config = config;

        state.push(State.MAIN);
        configs.push(config);
    }

    @Override
    public void startTag(String name, Attributes atts, Stack<String> context) {
        if (state.peek().equals(State.MAIN)) {
            switch (name) {
                case OPERATOR:
                    operatorConfig = new AVOperatorConfig(atts.getValue("id"), config);
                    state.push(State.OPERATOR);
                    configs.push(operatorConfig);
                    break;
                case TIMING:
                    state.push(State.TIMING);
                    configs.push(config.getTimingParameters());
                    break;
            }
        }

        if (state.peek().equals(State.OPERATOR)) {
            switch (name) {
                case TIMING:
                    state.push(State.TIMING);
                    configs.push(operatorConfig.getTimingParameters());
                    break;
                case DISPATCHER:
                    state.push(State.DISPATCHER);
                    configs.push(operatorConfig.createDispatcherConfig(atts.getValue("name")));
                    break;
            }
        }

        if (name.equals(PARAM)) {
            configs.peek().addParam(atts.getValue("name"), atts.getValue("value"));
        }
    }

    @Override
    public void endTag(String name, String content, Stack<String> context) {
        if (!name.equals("param")) {
            state.pop();
            configs.pop();
        }
    }
}
