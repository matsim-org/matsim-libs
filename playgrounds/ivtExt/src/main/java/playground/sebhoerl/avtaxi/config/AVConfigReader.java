package playground.sebhoerl.avtaxi.config;

import java.util.Stack;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

public class AVConfigReader extends MatsimXmlParser {
    final private AVConfig config;

    final static String PARAM = "param";
    final static String AV = "av";
    final static String OPERATOR = "operator";
    final static String TIMING = "timing";
    final static String DISPATCHER = "dispatcher";
    final static String GENERATOR = "generator";
    final static String PRICING = "pricing";

    enum State {
        MAIN, OPERATOR, TIMING, DISPATCHER, GENERATOR, PRICING
    }

    private Stack<State> state = new Stack<>();
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
                    operatorConfig = config.createOperatorConfig(atts.getValue("id"));
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
                    configs.push(operatorConfig.createTimingParameters());
                    break;
                case DISPATCHER:
                    state.push(State.DISPATCHER);
                    configs.push(operatorConfig.createDispatcherConfig(atts.getValue("strategy")));
                    break;
                case GENERATOR:
                    state.push(State.GENERATOR);
                    configs.push(operatorConfig.createGeneratorConfig(atts.getValue("strategy")));
                    break;
                case PRICING:
                    state.push(State.PRICING);
                    configs.push(operatorConfig.createPriceStructureConfig());
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
