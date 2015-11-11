package org.matsim.hybrid;

import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;

@javax.annotation.Generated("by gRPC proto compiler")
public class MATSimInterfaceServiceGrpc {

  private MATSimInterfaceServiceGrpc() {}

  public static final String SERVICE_NAME = "org.matsim.hybrid.MATSimInterfaceService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.Extern2MATSim,
      org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed> METHOD_REQ_EXTERN2MATSIM =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "org.matsim.hybrid.MATSimInterfaceService", "reqExtern2MATSim"),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.Extern2MATSim.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.AgentsStuck,
      org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed> METHOD_REQ_AGENT_STUCK =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "org.matsim.hybrid.MATSimInterfaceService", "reqAgentStuck"),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.AgentsStuck.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.ExternalConnect,
      org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed> METHOD_REQ_EXTERNAL_CONNECT =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "org.matsim.hybrid.MATSimInterfaceService", "reqExternalConnect"),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternalConnect.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.ExternSimStepFinished,
      org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived> METHOD_REQ_EXTERN_SIM_STEP_FINISHED =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "org.matsim.hybrid.MATSimInterfaceService", "reqExternSimStepFinished"),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternSimStepFinished.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents,
      org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed> METHOD_REQ_MAXIMUM_NUMBER_OF_AGENTS =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "org.matsim.hybrid.MATSimInterfaceService", "reqMaximumNumberOfAgents"),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed.getDefaultInstance()));
  @io.grpc.ExperimentalApi
  public static final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories,
      org.matsim.hybrid.MATSimInterface.MATSim2ExternTrajectoriesReceived> METHOD_REQ_SEND_TRAJECTORIES =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "org.matsim.hybrid.MATSimInterfaceService", "reqSendTrajectories"),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.MATSim2ExternTrajectoriesReceived.getDefaultInstance()));

  public static MATSimInterfaceServiceStub newStub(io.grpc.Channel channel) {
    return new MATSimInterfaceServiceStub(channel);
  }

  public static MATSimInterfaceServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new MATSimInterfaceServiceBlockingStub(channel);
  }

  public static MATSimInterfaceServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new MATSimInterfaceServiceFutureStub(channel);
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

  public static class MATSimInterfaceServiceStub extends io.grpc.stub.AbstractStub<MATSimInterfaceServiceStub>
      implements MATSimInterfaceService {
    private MATSimInterfaceServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private MATSimInterfaceServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MATSimInterfaceServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new MATSimInterfaceServiceStub(channel, callOptions);
    }

    @java.lang.Override
    public void reqExtern2MATSim(org.matsim.hybrid.MATSimInterface.Extern2MATSim request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_REQ_EXTERN2MATSIM, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void reqAgentStuck(org.matsim.hybrid.MATSimInterface.AgentsStuck request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_REQ_AGENT_STUCK, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void reqExternalConnect(org.matsim.hybrid.MATSimInterface.ExternalConnect request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_REQ_EXTERNAL_CONNECT, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void reqExternSimStepFinished(org.matsim.hybrid.MATSimInterface.ExternSimStepFinished request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_REQ_EXTERN_SIM_STEP_FINISHED, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void reqMaximumNumberOfAgents(org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_REQ_MAXIMUM_NUMBER_OF_AGENTS, getCallOptions()), request, responseObserver);
    }

    @java.lang.Override
    public void reqSendTrajectories(org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MATSim2ExternTrajectoriesReceived> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_REQ_SEND_TRAJECTORIES, getCallOptions()), request, responseObserver);
    }
  }

  public static class MATSimInterfaceServiceBlockingStub extends io.grpc.stub.AbstractStub<MATSimInterfaceServiceBlockingStub>
      implements MATSimInterfaceServiceBlockingClient {
    private MATSimInterfaceServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private MATSimInterfaceServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MATSimInterfaceServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new MATSimInterfaceServiceBlockingStub(channel, callOptions);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed reqExtern2MATSim(org.matsim.hybrid.MATSimInterface.Extern2MATSim request) {
      return blockingUnaryCall(
          getChannel().newCall(METHOD_REQ_EXTERN2MATSIM, getCallOptions()), request);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed reqAgentStuck(org.matsim.hybrid.MATSimInterface.AgentsStuck request) {
      return blockingUnaryCall(
          getChannel().newCall(METHOD_REQ_AGENT_STUCK, getCallOptions()), request);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed reqExternalConnect(org.matsim.hybrid.MATSimInterface.ExternalConnect request) {
      return blockingUnaryCall(
          getChannel().newCall(METHOD_REQ_EXTERNAL_CONNECT, getCallOptions()), request);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived reqExternSimStepFinished(org.matsim.hybrid.MATSimInterface.ExternSimStepFinished request) {
      return blockingUnaryCall(
          getChannel().newCall(METHOD_REQ_EXTERN_SIM_STEP_FINISHED, getCallOptions()), request);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed reqMaximumNumberOfAgents(org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents request) {
      return blockingUnaryCall(
          getChannel().newCall(METHOD_REQ_MAXIMUM_NUMBER_OF_AGENTS, getCallOptions()), request);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.MATSim2ExternTrajectoriesReceived reqSendTrajectories(org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories request) {
      return blockingUnaryCall(
          getChannel().newCall(METHOD_REQ_SEND_TRAJECTORIES, getCallOptions()), request);
    }
  }

  public static class MATSimInterfaceServiceFutureStub extends io.grpc.stub.AbstractStub<MATSimInterfaceServiceFutureStub>
      implements MATSimInterfaceServiceFutureClient {
    private MATSimInterfaceServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private MATSimInterfaceServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MATSimInterfaceServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new MATSimInterfaceServiceFutureStub(channel, callOptions);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed> reqExtern2MATSim(
        org.matsim.hybrid.MATSimInterface.Extern2MATSim request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_REQ_EXTERN2MATSIM, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed> reqAgentStuck(
        org.matsim.hybrid.MATSimInterface.AgentsStuck request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_REQ_AGENT_STUCK, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed> reqExternalConnect(
        org.matsim.hybrid.MATSimInterface.ExternalConnect request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_REQ_EXTERNAL_CONNECT, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived> reqExternSimStepFinished(
        org.matsim.hybrid.MATSimInterface.ExternSimStepFinished request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_REQ_EXTERN_SIM_STEP_FINISHED, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed> reqMaximumNumberOfAgents(
        org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_REQ_MAXIMUM_NUMBER_OF_AGENTS, getCallOptions()), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.MATSim2ExternTrajectoriesReceived> reqSendTrajectories(
        org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_REQ_SEND_TRAJECTORIES, getCallOptions()), request);
    }
  }

  public static io.grpc.ServerServiceDefinition bindService(
      final MATSimInterfaceService serviceImpl) {
    return io.grpc.ServerServiceDefinition.builder(SERVICE_NAME)
      .addMethod(
        METHOD_REQ_EXTERN2MATSIM,
        asyncUnaryCall(
          new io.grpc.stub.ServerCalls.UnaryMethod<
              org.matsim.hybrid.MATSimInterface.Extern2MATSim,
              org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed>() {
            @java.lang.Override
            public void invoke(
                org.matsim.hybrid.MATSimInterface.Extern2MATSim request,
                io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed> responseObserver) {
              serviceImpl.reqExtern2MATSim(request, responseObserver);
            }
          }))
      .addMethod(
        METHOD_REQ_AGENT_STUCK,
        asyncUnaryCall(
          new io.grpc.stub.ServerCalls.UnaryMethod<
              org.matsim.hybrid.MATSimInterface.AgentsStuck,
              org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed>() {
            @java.lang.Override
            public void invoke(
                org.matsim.hybrid.MATSimInterface.AgentsStuck request,
                io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed> responseObserver) {
              serviceImpl.reqAgentStuck(request, responseObserver);
            }
          }))
      .addMethod(
        METHOD_REQ_EXTERNAL_CONNECT,
        asyncUnaryCall(
          new io.grpc.stub.ServerCalls.UnaryMethod<
              org.matsim.hybrid.MATSimInterface.ExternalConnect,
              org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed>() {
            @java.lang.Override
            public void invoke(
                org.matsim.hybrid.MATSimInterface.ExternalConnect request,
                io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed> responseObserver) {
              serviceImpl.reqExternalConnect(request, responseObserver);
            }
          }))
      .addMethod(
        METHOD_REQ_EXTERN_SIM_STEP_FINISHED,
        asyncUnaryCall(
          new io.grpc.stub.ServerCalls.UnaryMethod<
              org.matsim.hybrid.MATSimInterface.ExternSimStepFinished,
              org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived>() {
            @java.lang.Override
            public void invoke(
                org.matsim.hybrid.MATSimInterface.ExternSimStepFinished request,
                io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived> responseObserver) {
              serviceImpl.reqExternSimStepFinished(request, responseObserver);
            }
          }))
      .addMethod(
        METHOD_REQ_MAXIMUM_NUMBER_OF_AGENTS,
        asyncUnaryCall(
          new io.grpc.stub.ServerCalls.UnaryMethod<
              org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents,
              org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed>() {
            @java.lang.Override
            public void invoke(
                org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents request,
                io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed> responseObserver) {
              serviceImpl.reqMaximumNumberOfAgents(request, responseObserver);
            }
          }))
      .addMethod(
        METHOD_REQ_SEND_TRAJECTORIES,
        asyncUnaryCall(
          new io.grpc.stub.ServerCalls.UnaryMethod<
              org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories,
              org.matsim.hybrid.MATSimInterface.MATSim2ExternTrajectoriesReceived>() {
            @java.lang.Override
            public void invoke(
                org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories request,
                io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MATSim2ExternTrajectoriesReceived> responseObserver) {
              serviceImpl.reqSendTrajectories(request, responseObserver);
            }
          })).build();
  }
}
