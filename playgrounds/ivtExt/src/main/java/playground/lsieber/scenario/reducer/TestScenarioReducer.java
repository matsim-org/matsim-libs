package playground.lsieber.scenario.reducer;

import java.io.IOException;

public class TestScenarioReducer {

    public static void main(String[] args) throws IOException {
        ShapeScenarioReducer shapeScenarioReducer = new ShapeScenarioReducer();
        shapeScenarioReducer.run();
        shapeScenarioReducer.writeToXML();
    }

}
