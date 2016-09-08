package playground.sebhoerl.agentfsm.instruction;

public class AdvanceInstruction implements Instruction {
    private String destinationStateId;
    
    public AdvanceInstruction(String destinationStateId) {
        this.destinationStateId = destinationStateId;
    }
    
    public String getDestinationStateId() {
        return destinationStateId;
    }
}
