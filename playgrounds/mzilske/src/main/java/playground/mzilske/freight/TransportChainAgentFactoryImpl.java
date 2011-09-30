package playground.mzilske.freight;

public class TransportChainAgentFactoryImpl implements TransportChainAgentFactory{

	@Override
	public TransportChainAgent createChainAgent(TransportChain chain) {
		return new TransportChainAgentImpl(chain);
	}

}
