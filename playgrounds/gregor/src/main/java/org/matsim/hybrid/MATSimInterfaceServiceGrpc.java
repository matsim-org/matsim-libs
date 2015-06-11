package org.matsim.hybrid;

import static io.grpc.stub.Calls.createMethodDescriptor;
import static io.grpc.stub.Calls.asyncUnaryCall;
import static io.grpc.stub.Calls.asyncServerStreamingCall;
import static io.grpc.stub.Calls.asyncClientStreamingCall;
import static io.grpc.stub.Calls.duplexStreamingCall;
import static io.grpc.stub.Calls.blockingUnaryCall;
import static io.grpc.stub.Calls.blockingServerStreamingCall;
import static io.grpc.stub.Calls.unaryFutureCall;
import static io.grpc.stub.ServerCalls.createMethodDefinition;
import static io.grpc.stub.ServerCalls.asyncUnaryRequestCall;
import static io.grpc.stub.ServerCalls.asyncStreamingRequestCall;

@javax.annotation.Generated("by gRPC proto compiler")
public class MATSimInterfaceServiceGrpc {

  private static final io.grpc.stub.Method<org.matsim.hybrid.MATSimInterface.Extern2MATSim,
      org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed> METHOD_REQ_EXTERN2MATSIM =
      io.grpc.stub.Method.create(
          io.grpc.MethodType.UNARY, "reqExtern2MATSim",
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.Extern2MATSim.PARSER),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed.PARSER));
  private static final io.grpc.stub.Method<org.matsim.hybrid.MATSimInterface.AgentsStuck,
      org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed> METHOD_REQ_AGENT_STUCK =
      io.grpc.stub.Method.create(
          io.grpc.MethodType.UNARY, "reqAgentStuck",
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.AgentsStuck.PARSER),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed.PARSER));
  private static final io.grpc.stub.Method<org.matsim.hybrid.MATSimInterface.ExternalConnect,
      org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed> METHOD_REQ_EXTERNAL_CONNECT =
      io.grpc.stub.Method.create(
          io.grpc.MethodType.UNARY, "reqExternalConnect",
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternalConnect.PARSER),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed.PARSER));
  private static final io.grpc.stub.Method<org.matsim.hybrid.MATSimInterface.ExternSimStepFinished,
      org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived> METHOD_REQ_EXTERN_SIM_STEP_FINISHED =
      io.grpc.stub.Method.create(
          io.grpc.MethodType.UNARY, "reqExternSimStepFinished",
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternSimStepFinished.PARSER),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived.PARSER));
  private static final io.grpc.stub.Method<org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents,
      org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed> METHOD_REQ_MAXIMUM_NUMBER_OF_AGENTS =
      io.grpc.stub.Method.create(
          io.grpc.MethodType.UNARY, "reqMaximumNumberOfAgents",
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents.PARSER),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed.PARSER));
  private static final io.grpc.stub.Method<org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories,
      org.matsim.hybrid.MATSimInterface.MATSim2ExternTrajectoriesReceived> METHOD_REQ_SEND_TRAJECTORIES =
      io.grpc.stub.Method.create(
          io.grpc.MethodType.UNARY, "reqSendTrajectories",
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories.PARSER),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.MATSim2ExternTrajectoriesReceived.PARSER));

  public static MATSimInterfaceServiceStub newStub(io.grpc.Channel channel) {
    return new MATSimInterfaceServiceStub(channel, CONFIG);
  }

  public static MATSimInterfaceServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new MATSimInterfaceServiceBlockingStub(channel, CONFIG);
  }

  public static MATSimInterfaceServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new MATSimInterfaceServiceFutureStub(channel, CONFIG);
  }

  public static final MATSimInterfaceServiceServiceDescriptor CONFIG =
      new MATSimInterfaceServiceServiceDescriptor();

  @javax.annotation.concurrent.Immutable
  public static class MATSimInterfaceServiceServiceDescriptor extends
      io.grpc.stub.AbstractServiceDescriptor<MATSimInterfaceServiceServiceDescriptor> {
    public final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.Extern2MATSim,
        org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed> reqExtern2MATSim;
    public final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.AgentsStuck,
        org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed> reqAgentStuck;
    public final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.ExternalConnect,
        org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed> reqExternalConnect;
    public final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.ExternSimStepFinished,
        org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived> reqExternSimStepFinished;
    public final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents,
        org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed> reqMaximumNumberOfAgents;
    public final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories,
        org.matsim.hybrid.MATSimInterface.MATSim2ExternTrajectoriesReceived> reqSendTrajectories;

    private MATSimInterfaceServiceServiceDescriptor() {
      reqExtern2MATSim = createMethodDescriptor(
          "org.matsim.hybrid.MATSimInterfaceService", METHOD_REQ_EXTERN2MATSIM);
      reqAgentStuck = createMethodDescriptor(
          "org.matsim.hybrid.MATSimInterfaceService", METHOD_REQ_AGENT_STUCK);
      reqExternalConnect = createMethodDescriptor(
          "org.matsim.hybrid.MATSimInterfaceService", METHOD_REQ_EXTERNAL_CONNECT);
      reqExternSimStepFinished = createMethodDescriptor(
          "org.matsim.hybrid.MATSimInterfaceService", METHOD_REQ_EXTERN_SIM_STEP_FINISHED);
      reqMaximumNumberOfAgents = createMethodDescriptor(
          "org.matsim.hybrid.MATSimInterfaceService", METHOD_REQ_MAXIMUM_NUMBER_OF_AGENTS);
      reqSendTrajectories = createMethodDescriptor(
          "org.matsim.hybrid.MATSimInterfaceService", METHOD_REQ_SEND_TRAJECTORIES);
    }

    @SuppressWarnings("unchecked")
    private MATSimInterfaceServiceServiceDescriptor(
        java.util.Map<java.lang.String, io.grpc.MethodDescriptor<?, ?>> methodMap) {
      reqExtern2MATSim = (io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.Extern2MATSim,
          org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed>) methodMap.get(
          CONFIG.reqExtern2MATSim.getName());
      reqAgentStuck = (io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.AgentsStuck,
          org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed>) methodMap.get(
          CONFIG.reqAgentStuck.getName());
      reqExternalConnect = (io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.ExternalConnect,
          org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed>) methodMap.get(
          CONFIG.reqExternalConnect.getName());
      reqExternSimStepFinished = (io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.ExternSimStepFinished,
          org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived>) methodMap.get(
          CONFIG.reqExternSimStepFinished.getName());
      reqMaximumNumberOfAgents = (io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents,
          org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed>) methodMap.get(
          CONFIG.reqMaximumNumberOfAgents.getName());
      reqSendTrajectories = (io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories,
          org.matsim.hybrid.MATSimInterface.MATSim2ExternTrajectoriesReceived>) methodMap.get(
          CONFIG.reqSendTrajectories.getName());
    }

    @java.lang.Override
    protected MATSimInterfaceServiceServiceDescriptor build(
        java.util.Map<java.lang.String, io.grpc.MethodDescriptor<?, ?>> methodMap) {
      return new MATSimInterfaceServiceServiceDescriptor(methodMap);
    }

    @java.lang.Override
    public com.google.common.collect.ImmutableList<io.grpc.MethodDescriptor<?, ?>> methods() {
      return com.google.common.collect.ImmutableList.<io.grpc.MethodDescriptor<?, ?>>of(
          reqExtern2MATSim,
          reqAgentStuck,
          reqExternalConnect,
          reqExternSimStepFinished,
          reqMaximumNumberOfAgents,
          reqSendTrajectories);
    }
  }

  public static interface MATSimInterfaceService {

    public void reqExtern2MATSim(org.matsim.hybrid.MATSimInterface.Extern2MATSim request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed> responseObserver);

    public void reqAgentStuck(org.matsim.hybrid.MATSimInterface.AgentsStuck request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed> responseObserver);

    public void reqExternalConnect(org.matsim.hybrid.MATSimInterface.ExternalConnect request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed> responseObserver);

    public void reqExternSimStepFinished(org.matsim.hybrid.MATSimInterface.ExternSimStepFinished request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived> responseObserver);

    public void reqMaximumNumberOfAgents(org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed> responseObserver);

    public void reqSendTrajectories(org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MATSim2ExternTrajectoriesReceived> responseObserver);
  }

  public static interface MATSimInterfaceServiceBlockingClient {

    public org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed reqExtern2MATSim(org.matsim.hybrid.MATSimInterface.Extern2MATSim request);

    public org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed reqAgentStuck(org.matsim.hybrid.MATSimInterface.AgentsStuck request);

    public org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed reqExternalConnect(org.matsim.hybrid.MATSimInterface.ExternalConnect request);

    public org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived reqExternSimStepFinished(org.matsim.hybrid.MATSimInterface.ExternSimStepFinished request);

    public org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed reqMaximumNumberOfAgents(org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents request);

    public org.matsim.hybrid.MATSimInterface.MATSim2ExternTrajectoriesReceived reqSendTrajectories(org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories request);
  }

  public static interface MATSimInterfaceServiceFutureClient {

    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed> reqExtern2MATSim(
        org.matsim.hybrid.MATSimInterface.Extern2MATSim request);

    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed> reqAgentStuck(
        org.matsim.hybrid.MATSimInterface.AgentsStuck request);

    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed> reqExternalConnect(
        org.matsim.hybrid.MATSimInterface.ExternalConnect request);

    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived> reqExternSimStepFinished(
        org.matsim.hybrid.MATSimInterface.ExternSimStepFinished request);

    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed> reqMaximumNumberOfAgents(
        org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents request);

    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.MATSim2ExternTrajectoriesReceived> reqSendTrajectories(
        org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories request);
  }

  public static class MATSimInterfaceServiceStub extends
      io.grpc.stub.AbstractStub<MATSimInterfaceServiceStub, MATSimInterfaceServiceServiceDescriptor>
      implements MATSimInterfaceService {
    private MATSimInterfaceServiceStub(io.grpc.Channel channel,
        MATSimInterfaceServiceServiceDescriptor config) {
      super(channel, config);
    }

    @java.lang.Override
    protected MATSimInterfaceServiceStub build(io.grpc.Channel channel,
        MATSimInterfaceServiceServiceDescriptor config) {
      return new MATSimInterfaceServiceStub(channel, config);
    }

    @java.lang.Override
    public void reqExtern2MATSim(org.matsim.hybrid.MATSimInterface.Extern2MATSim request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed> responseObserver) {
      asyncUnaryCall(
          channel.newCall(config.reqExtern2MATSim), request, responseObserver);
    }

    @java.lang.Override
    public void reqAgentStuck(org.matsim.hybrid.MATSimInterface.AgentsStuck request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed> responseObserver) {
      asyncUnaryCall(
          channel.newCall(config.reqAgentStuck), request, responseObserver);
    }

    @java.lang.Override
    public void reqExternalConnect(org.matsim.hybrid.MATSimInterface.ExternalConnect request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed> responseObserver) {
      asyncUnaryCall(
          channel.newCall(config.reqExternalConnect), request, responseObserver);
    }

    @java.lang.Override
    public void reqExternSimStepFinished(org.matsim.hybrid.MATSimInterface.ExternSimStepFinished request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived> responseObserver) {
      asyncUnaryCall(
          channel.newCall(config.reqExternSimStepFinished), request, responseObserver);
    }

    @java.lang.Override
    public void reqMaximumNumberOfAgents(org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed> responseObserver) {
      asyncUnaryCall(
          channel.newCall(config.reqMaximumNumberOfAgents), request, responseObserver);
    }

    @java.lang.Override
    public void reqSendTrajectories(org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MATSim2ExternTrajectoriesReceived> responseObserver) {
      asyncUnaryCall(
          channel.newCall(config.reqSendTrajectories), request, responseObserver);
    }
  }

  public static class MATSimInterfaceServiceBlockingStub extends
      io.grpc.stub.AbstractStub<MATSimInterfaceServiceBlockingStub, MATSimInterfaceServiceServiceDescriptor>
      implements MATSimInterfaceServiceBlockingClient {
    private MATSimInterfaceServiceBlockingStub(io.grpc.Channel channel,
        MATSimInterfaceServiceServiceDescriptor config) {
      super(channel, config);
    }

    @java.lang.Override
    protected MATSimInterfaceServiceBlockingStub build(io.grpc.Channel channel,
        MATSimInterfaceServiceServiceDescriptor config) {
      return new MATSimInterfaceServiceBlockingStub(channel, config);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed reqExtern2MATSim(org.matsim.hybrid.MATSimInterface.Extern2MATSim request) {
      return blockingUnaryCall(
          channel.newCall(config.reqExtern2MATSim), request);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed reqAgentStuck(org.matsim.hybrid.MATSimInterface.AgentsStuck request) {
      return blockingUnaryCall(
          channel.newCall(config.reqAgentStuck), request);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed reqExternalConnect(org.matsim.hybrid.MATSimInterface.ExternalConnect request) {
      return blockingUnaryCall(
          channel.newCall(config.reqExternalConnect), request);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived reqExternSimStepFinished(org.matsim.hybrid.MATSimInterface.ExternSimStepFinished request) {
      return blockingUnaryCall(
          channel.newCall(config.reqExternSimStepFinished), request);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed reqMaximumNumberOfAgents(org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents request) {
      return blockingUnaryCall(
          channel.newCall(config.reqMaximumNumberOfAgents), request);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.MATSim2ExternTrajectoriesReceived reqSendTrajectories(org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories request) {
      return blockingUnaryCall(
          channel.newCall(config.reqSendTrajectories), request);
    }
  }

  public static class MATSimInterfaceServiceFutureStub extends
      io.grpc.stub.AbstractStub<MATSimInterfaceServiceFutureStub, MATSimInterfaceServiceServiceDescriptor>
      implements MATSimInterfaceServiceFutureClient {
    private MATSimInterfaceServiceFutureStub(io.grpc.Channel channel,
        MATSimInterfaceServiceServiceDescriptor config) {
      super(channel, config);
    }

    @java.lang.Override
    protected MATSimInterfaceServiceFutureStub build(io.grpc.Channel channel,
        MATSimInterfaceServiceServiceDescriptor config) {
      return new MATSimInterfaceServiceFutureStub(channel, config);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed> reqExtern2MATSim(
        org.matsim.hybrid.MATSimInterface.Extern2MATSim request) {
      return unaryFutureCall(
          channel.newCall(config.reqExtern2MATSim), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed> reqAgentStuck(
        org.matsim.hybrid.MATSimInterface.AgentsStuck request) {
      return unaryFutureCall(
          channel.newCall(config.reqAgentStuck), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed> reqExternalConnect(
        org.matsim.hybrid.MATSimInterface.ExternalConnect request) {
      return unaryFutureCall(
          channel.newCall(config.reqExternalConnect), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived> reqExternSimStepFinished(
        org.matsim.hybrid.MATSimInterface.ExternSimStepFinished request) {
      return unaryFutureCall(
          channel.newCall(config.reqExternSimStepFinished), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed> reqMaximumNumberOfAgents(
        org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents request) {
      return unaryFutureCall(
          channel.newCall(config.reqMaximumNumberOfAgents), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.MATSim2ExternTrajectoriesReceived> reqSendTrajectories(
        org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories request) {
      return unaryFutureCall(
          channel.newCall(config.reqSendTrajectories), request);
    }
  }

  public static io.grpc.ServerServiceDefinition bindService(
      final MATSimInterfaceService serviceImpl) {
    return io.grpc.ServerServiceDefinition.builder("org.matsim.hybrid.MATSimInterfaceService")
      .addMethod(createMethodDefinition(
          METHOD_REQ_EXTERN2MATSIM,
          asyncUnaryRequestCall(
            new io.grpc.stub.ServerCalls.UnaryRequestMethod<
                org.matsim.hybrid.MATSimInterface.Extern2MATSim,
                org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed>() {
              @java.lang.Override
              public void invoke(
                  org.matsim.hybrid.MATSimInterface.Extern2MATSim request,
                  io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed> responseObserver) {
                serviceImpl.reqExtern2MATSim(request, responseObserver);
              }
            })))
      .addMethod(createMethodDefinition(
          METHOD_REQ_AGENT_STUCK,
          asyncUnaryRequestCall(
            new io.grpc.stub.ServerCalls.UnaryRequestMethod<
                org.matsim.hybrid.MATSimInterface.AgentsStuck,
                org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed>() {
              @java.lang.Override
              public void invoke(
                  org.matsim.hybrid.MATSimInterface.AgentsStuck request,
                  io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed> responseObserver) {
                serviceImpl.reqAgentStuck(request, responseObserver);
              }
            })))
      .addMethod(createMethodDefinition(
          METHOD_REQ_EXTERNAL_CONNECT,
          asyncUnaryRequestCall(
            new io.grpc.stub.ServerCalls.UnaryRequestMethod<
                org.matsim.hybrid.MATSimInterface.ExternalConnect,
                org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed>() {
              @java.lang.Override
              public void invoke(
                  org.matsim.hybrid.MATSimInterface.ExternalConnect request,
                  io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed> responseObserver) {
                serviceImpl.reqExternalConnect(request, responseObserver);
              }
            })))
      .addMethod(createMethodDefinition(
          METHOD_REQ_EXTERN_SIM_STEP_FINISHED,
          asyncUnaryRequestCall(
            new io.grpc.stub.ServerCalls.UnaryRequestMethod<
                org.matsim.hybrid.MATSimInterface.ExternSimStepFinished,
                org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived>() {
              @java.lang.Override
              public void invoke(
                  org.matsim.hybrid.MATSimInterface.ExternSimStepFinished request,
                  io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived> responseObserver) {
                serviceImpl.reqExternSimStepFinished(request, responseObserver);
              }
            })))
      .addMethod(createMethodDefinition(
          METHOD_REQ_MAXIMUM_NUMBER_OF_AGENTS,
          asyncUnaryRequestCall(
            new io.grpc.stub.ServerCalls.UnaryRequestMethod<
                org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents,
                org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed>() {
              @java.lang.Override
              public void invoke(
                  org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents request,
                  io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed> responseObserver) {
                serviceImpl.reqMaximumNumberOfAgents(request, responseObserver);
              }
            })))
      .addMethod(createMethodDefinition(
          METHOD_REQ_SEND_TRAJECTORIES,
          asyncUnaryRequestCall(
            new io.grpc.stub.ServerCalls.UnaryRequestMethod<
                org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories,
                org.matsim.hybrid.MATSimInterface.MATSim2ExternTrajectoriesReceived>() {
              @java.lang.Override
              public void invoke(
                  org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories request,
                  io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MATSim2ExternTrajectoriesReceived> responseObserver) {
                serviceImpl.reqSendTrajectories(request, responseObserver);
              }
            }))).build();
  }
}
