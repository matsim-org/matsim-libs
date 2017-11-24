package playground.lsieber.networkshapecutter;

public abstract class PopulationCutter implements PopulationCutterInterface {
    protected String cutInfo = null;

    @Override
    public void printCutSummary() {
        System.out.println(cutInfo); // TODO this will produce an exeption, fill thet info while cutting!
    }

    @Override
    public void checkNetworkConsistency() {
        // TODO @Lukas Create check on Network Consistency
    }
}
