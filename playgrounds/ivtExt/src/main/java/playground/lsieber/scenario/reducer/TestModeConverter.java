package playground.lsieber.scenario.reducer;

import java.io.IOException;

public class TestModeConverter {

    /** @author Lukas Sieber
     * Hint: if Out of Memory error appears give the program more Memory with the -Xmx8192m argument in the Run Configurations VM paart
     */
    public static void main(String[] args) throws IOException {
        // TODO Auto-generated constructor stub
        ModeConverterImpl modeConverter = new ModeConverterImpl();
        modeConverter.ConvertPtToAV();
        System.out.println("got it");
    }
    

    

}
