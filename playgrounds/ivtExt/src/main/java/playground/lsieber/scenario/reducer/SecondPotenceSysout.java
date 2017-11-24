package playground.lsieber.scenario.reducer;

public class SecondPotenceSysout {

    int counter;
    int nextMsg;
    String string;

    public SecondPotenceSysout(String string) {
        counter = 0;
        nextMsg = 1;
        this.string = string;
    }

    public void ifPotenceOf2() {
        counter++;
        if (counter % nextMsg == 0) {
            nextMsg *= 2;
            System.out.println(string + counter + ". ");
        }
    }

}
