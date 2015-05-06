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
public class ExternInterfaceServiceGrpc {

  private static final io.grpc.stub.Method<org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace,
      org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed> METHOD_REQ_MATSIM2EXTERN_HAS_SPACE =
      io.grpc.stub.Method.create(
          io.grpc.MethodType.UNARY, "reqMATSim2ExternHasSpace",
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace.PARSER),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed.PARSER));
  private static final io.grpc.stub.Method<org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent,
      org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed> METHOD_REQ_MATSIM2EXTERN_PUT_AGENT =
      io.grpc.stub.Method.create(
          io.grpc.MethodType.UNARY, "reqMATSim2ExternPutAgent",
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent.PARSER),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed.PARSER));
  private static final io.grpc.stub.Method<org.matsim.hybrid.MATSimInterface.ExternDoSimStep,
      org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived> METHOD_REQ_EXTERN_DO_SIM_STEP =
      io.grpc.stub.Method.create(
          io.grpc.MethodType.UNARY, "reqExternDoSimStep",
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternDoSimStep.PARSER),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived.PARSER));
  private static final io.grpc.stub.Method<org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim,
      org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed> METHOD_REQ_EXTERN_ON_PREPARE_SIM =
      io.grpc.stub.Method.create(
          io.grpc.MethodType.UNARY, "reqExternOnPrepareSim",
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim.PARSER),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed.PARSER));
  private static final io.grpc.stub.Method<org.matsim.hybrid.MATSimInterface.ExternAfterSim,
      org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed> METHOD_REQ_EXTERN_AFTER_SIM =
      io.grpc.stub.Method.create(
          io.grpc.MethodType.UNARY, "reqExternAfterSim",
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternAfterSim.PARSER),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed.PARSER));
  private static final io.grpc.stub.Method<org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents,
      org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed> METHOD_REQ_MAXIMUM_NUMBER_OF_AGENTS =
      io.grpc.stub.Method.create(
          io.grpc.MethodType.UNARY, "reqMaximumNumberOfAgents",
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents.PARSER),
          io.grpc.protobuf.ProtoUtils.marshaller(org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed.PARSER));

  public static ExternInterfaceServiceStub newStub(io.grpc.Channel channel) {
    return new ExternInterfaceServiceStub(channel, CONFIG);
  }

  public static ExternInterfaceServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new ExternInterfaceServiceBlockingStub(channel, CONFIG);
  }

  public static ExternInterfaceServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new ExternInterfaceServiceFutureStub(channel, CONFIG);
  }

  public static final ExternInterfaceServiceServiceDescriptor CONFIG =
      new ExternInterfaceServiceServiceDescriptor();

  @javax.annotation.concurrent.Immutable
  public static class ExternInterfaceServiceServiceDescriptor extends
      io.grpc.stub.AbstractServiceDescriptor<ExternInterfaceServiceServiceDescriptor> {
    public final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace,
        org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed> reqMATSim2ExternHasSpace;
    public final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent,
        org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed> reqMATSim2ExternPutAgent;
    public final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.ExternDoSimStep,
        org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived> reqExternDoSimStep;
    public final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim,
        org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed> reqExternOnPrepareSim;
    public final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.ExternAfterSim,
        org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed> reqExternAfterSim;
    public final io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents,
        org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed> reqMaximumNumberOfAgents;

    private ExternInterfaceServiceServiceDescriptor() {
      reqMATSim2ExternHasSpace = createMethodDescriptor(
          "org.matsim.hybrid.ExternInterfaceService", METHOD_REQ_MATSIM2EXTERN_HAS_SPACE);
      reqMATSim2ExternPutAgent = createMethodDescriptor(
          "org.matsim.hybrid.ExternInterfaceService", METHOD_REQ_MATSIM2EXTERN_PUT_AGENT);
      reqExternDoSimStep = createMethodDescriptor(
          "org.matsim.hybrid.ExternInterfaceService", METHOD_REQ_EXTERN_DO_SIM_STEP);
      reqExternOnPrepareSim = createMethodDescriptor(
          "org.matsim.hybrid.ExternInterfaceService", METHOD_REQ_EXTERN_ON_PREPARE_SIM);
      reqExternAfterSim = createMethodDescriptor(
          "org.matsim.hybrid.ExternInterfaceService", METHOD_REQ_EXTERN_AFTER_SIM);
      reqMaximumNumberOfAgents = createMethodDescriptor(
          "org.matsim.hybrid.ExternInterfaceService", METHOD_REQ_MAXIMUM_NUMBER_OF_AGENTS);
    }

    @SuppressWarnings("unchecked")
    private ExternInterfaceServiceServiceDescriptor(
        java.util.Map<java.lang.String, io.grpc.MethodDescriptor<?, ?>> methodMap) {
      reqMATSim2ExternHasSpace = (io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace,
          org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed>) methodMap.get(
          CONFIG.reqMATSim2ExternHasSpace.getName());
      reqMATSim2ExternPutAgent = (io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent,
          org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed>) methodMap.get(
          CONFIG.reqMATSim2ExternPutAgent.getName());
      reqExternDoSimStep = (io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.ExternDoSimStep,
          org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived>) methodMap.get(
          CONFIG.reqExternDoSimStep.getName());
      reqExternOnPrepareSim = (io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim,
          org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed>) methodMap.get(
          CONFIG.reqExternOnPrepareSim.getName());
      reqExternAfterSim = (io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.ExternAfterSim,
          org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed>) methodMap.get(
          CONFIG.reqExternAfterSim.getName());
      reqMaximumNumberOfAgents = (io.grpc.MethodDescriptor<org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents,
          org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed>) methodMap.get(
          CONFIG.reqMaximumNumberOfAgents.getName());
    }

    @java.lang.Override
    protected ExternInterfaceServiceServiceDescriptor build(
        java.util.Map<java.lang.String, io.grpc.MethodDescriptor<?, ?>> methodMap) {
      return new ExternInterfaceServiceServiceDescriptor(methodMap);
    }

    @java.lang.Override
    public com.google.common.collect.ImmutableList<io.grpc.MethodDescriptor<?, ?>> methods() {
      return com.google.common.collect.ImmutableList.<io.grpc.MethodDescriptor<?, ?>>of(
          reqMATSim2ExternHasSpace,
          reqMATSim2ExternPutAgent,
          reqExternDoSimStep,
          reqExternOnPrepareSim,
          reqExternAfterSim,
          reqMaximumNumberOfAgents);
    }
  }

  public static interface ExternInterfaceService {

    public void reqMATSim2ExternHasSpace(org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed> responseObserver);

    public void reqMATSim2ExternPutAgent(org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed> responseObserver);

    public void reqExternDoSimStep(org.matsim.hybrid.MATSimInterface.ExternDoSimStep request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived> responseObserver);

    public void reqExternOnPrepareSim(org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed> responseObserver);

    public void reqExternAfterSim(org.matsim.hybrid.MATSimInterface.ExternAfterSim request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed> responseObserver);

    public void reqMaximumNumberOfAgents(org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed> responseObserver);
  }

  public static interface ExternInterfaceServiceBlockingClient {

    public org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed reqMATSim2ExternHasSpace(org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace request);

    public org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed reqMATSim2ExternPutAgent(org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent request);

    public org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived reqExternDoSimStep(org.matsim.hybrid.MATSimInterface.ExternDoSimStep request);

    public org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed reqExternOnPrepareSim(org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim request);

    public org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed reqExternAfterSim(org.matsim.hybrid.MATSimInterface.ExternAfterSim request);

    public org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed reqMaximumNumberOfAgents(org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents request);
  }

  public static interface ExternInterfaceServiceFutureClient {

    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed> reqMATSim2ExternHasSpace(
        org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace request);

    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed> reqMATSim2ExternPutAgent(
        org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent request);

    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived> reqExternDoSimStep(
        org.matsim.hybrid.MATSimInterface.ExternDoSimStep request);

    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed> reqExternOnPrepareSim(
        org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim request);

    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed> reqExternAfterSim(
        org.matsim.hybrid.MATSimInterface.ExternAfterSim request);

    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed> reqMaximumNumberOfAgents(
        org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents request);
  }

  public static class ExternInterfaceServiceStub extends
      io.grpc.stub.AbstractStub<ExternInterfaceServiceStub, ExternInterfaceServiceServiceDescriptor>
      implements ExternInterfaceService {
    private ExternInterfaceServiceStub(io.grpc.Channel channel,
        ExternInterfaceServiceServiceDescriptor config) {
      super(channel, config);
    }

    @java.lang.Override
    protected ExternInterfaceServiceStub build(io.grpc.Channel channel,
        ExternInterfaceServiceServiceDescriptor config) {
      return new ExternInterfaceServiceStub(channel, config);
    }

    @java.lang.Override
    public void reqMATSim2ExternHasSpace(org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed> responseObserver) {
      asyncUnaryCall(
          channel.newCall(config.reqMATSim2ExternHasSpace), request, responseObserver);
    }

    @java.lang.Override
    public void reqMATSim2ExternPutAgent(org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed> responseObserver) {
      asyncUnaryCall(
          channel.newCall(config.reqMATSim2ExternPutAgent), request, responseObserver);
    }

    @java.lang.Override
    public void reqExternDoSimStep(org.matsim.hybrid.MATSimInterface.ExternDoSimStep request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived> responseObserver) {
      asyncUnaryCall(
          channel.newCall(config.reqExternDoSimStep), request, responseObserver);
    }

    @java.lang.Override
    public void reqExternOnPrepareSim(org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed> responseObserver) {
      asyncUnaryCall(
          channel.newCall(config.reqExternOnPrepareSim), request, responseObserver);
    }

    @java.lang.Override
    public void reqExternAfterSim(org.matsim.hybrid.MATSimInterface.ExternAfterSim request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed> responseObserver) {
      asyncUnaryCall(
          channel.newCall(config.reqExternAfterSim), request, responseObserver);
    }

    @java.lang.Override
    public void reqMaximumNumberOfAgents(org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents request,
        io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed> responseObserver) {
      asyncUnaryCall(
          channel.newCall(config.reqMaximumNumberOfAgents), request, responseObserver);
    }
  }

  public static class ExternInterfaceServiceBlockingStub extends
      io.grpc.stub.AbstractStub<ExternInterfaceServiceBlockingStub, ExternInterfaceServiceServiceDescriptor>
      implements ExternInterfaceServiceBlockingClient {
    private ExternInterfaceServiceBlockingStub(io.grpc.Channel channel,
        ExternInterfaceServiceServiceDescriptor config) {
      super(channel, config);
    }

    @java.lang.Override
    protected ExternInterfaceServiceBlockingStub build(io.grpc.Channel channel,
        ExternInterfaceServiceServiceDescriptor config) {
      return new ExternInterfaceServiceBlockingStub(channel, config);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed reqMATSim2ExternHasSpace(org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace request) {
      return blockingUnaryCall(
          channel.newCall(config.reqMATSim2ExternHasSpace), request);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed reqMATSim2ExternPutAgent(org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent request) {
      return blockingUnaryCall(
          channel.newCall(config.reqMATSim2ExternPutAgent), request);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived reqExternDoSimStep(org.matsim.hybrid.MATSimInterface.ExternDoSimStep request) {
      return blockingUnaryCall(
          channel.newCall(config.reqExternDoSimStep), request);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed reqExternOnPrepareSim(org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim request) {
      return blockingUnaryCall(
          channel.newCall(config.reqExternOnPrepareSim), request);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed reqExternAfterSim(org.matsim.hybrid.MATSimInterface.ExternAfterSim request) {
      return blockingUnaryCall(
          channel.newCall(config.reqExternAfterSim), request);
    }

    @java.lang.Override
    public org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed reqMaximumNumberOfAgents(org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents request) {
      return blockingUnaryCall(
          channel.newCall(config.reqMaximumNumberOfAgents), request);
    }
  }

  public static class ExternInterfaceServiceFutureStub extends
      io.grpc.stub.AbstractStub<ExternInterfaceServiceFutureStub, ExternInterfaceServiceServiceDescriptor>
      implements ExternInterfaceServiceFutureClient {
    private ExternInterfaceServiceFutureStub(io.grpc.Channel channel,
        ExternInterfaceServiceServiceDescriptor config) {
      super(channel, config);
    }

    @java.lang.Override
    protected ExternInterfaceServiceFutureStub build(io.grpc.Channel channel,
        ExternInterfaceServiceServiceDescriptor config) {
      return new ExternInterfaceServiceFutureStub(channel, config);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed> reqMATSim2ExternHasSpace(
        org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace request) {
      return unaryFutureCall(
          channel.newCall(config.reqMATSim2ExternHasSpace), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed> reqMATSim2ExternPutAgent(
        org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent request) {
      return unaryFutureCall(
          channel.newCall(config.reqMATSim2ExternPutAgent), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived> reqExternDoSimStep(
        org.matsim.hybrid.MATSimInterface.ExternDoSimStep request) {
      return unaryFutureCall(
          channel.newCall(config.reqExternDoSimStep), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed> reqExternOnPrepareSim(
        org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim request) {
      return unaryFutureCall(
          channel.newCall(config.reqExternOnPrepareSim), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed> reqExternAfterSim(
        org.matsim.hybrid.MATSimInterface.ExternAfterSim request) {
      return unaryFutureCall(
          channel.newCall(config.reqExternAfterSim), request);
    }

    @java.lang.Override
    public com.google.common.util.concurrent.ListenableFuture<org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgentsConfirmed> reqMaximumNumberOfAgents(
        org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents request) {
      return unaryFutureCall(
          channel.newCall(config.reqMaximumNumberOfAgents), request);
    }
  }

  public static io.grpc.ServerServiceDefinition bindService(
      final ExternInterfaceService serviceImpl) {
    return io.grpc.ServerServiceDefinition.builder("org.matsim.hybrid.ExternInterfaceService")
      .addMethod(createMethodDefinition(
          METHOD_REQ_MATSIM2EXTERN_HAS_SPACE,
          asyncUnaryRequestCall(
            new io.grpc.stub.ServerCalls.UnaryRequestMethod<
                org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace,
                org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed>() {
              @java.lang.Override
              public void invoke(
                  org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace request,
                  io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed> responseObserver) {
                serviceImpl.reqMATSim2ExternHasSpace(request, responseObserver);
              }
            })))
      .addMethod(createMethodDefinition(
          METHOD_REQ_MATSIM2EXTERN_PUT_AGENT,
          asyncUnaryRequestCall(
            new io.grpc.stub.ServerCalls.UnaryRequestMethod<
                org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent,
                org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed>() {
              @java.lang.Override
              public void invoke(
                  org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent request,
                  io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed> responseObserver) {
                serviceImpl.reqMATSim2ExternPutAgent(request, responseObserver);
              }
            })))
      .addMethod(createMethodDefinition(
          METHOD_REQ_EXTERN_DO_SIM_STEP,
          asyncUnaryRequestCall(
            new io.grpc.stub.ServerCalls.UnaryRequestMethod<
                org.matsim.hybrid.MATSimInterface.ExternDoSimStep,
                org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived>() {
              @java.lang.Override
              public void invoke(
                  org.matsim.hybrid.MATSimInterface.ExternDoSimStep request,
                  io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived> responseObserver) {
                serviceImpl.reqExternDoSimStep(request, responseObserver);
              }
            })))
      .addMethod(createMethodDefinition(
          METHOD_REQ_EXTERN_ON_PREPARE_SIM,
          asyncUnaryRequestCall(
            new io.grpc.stub.ServerCalls.UnaryRequestMethod<
                org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim,
                org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed>() {
              @java.lang.Override
              public void invoke(
                  org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim request,
                  io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed> responseObserver) {
                serviceImpl.reqExternOnPrepareSim(request, responseObserver);
              }
            })))
      .addMethod(createMethodDefinition(
          METHOD_REQ_EXTERN_AFTER_SIM,
          asyncUnaryRequestCall(
            new io.grpc.stub.ServerCalls.UnaryRequestMethod<
                org.matsim.hybrid.MATSimInterface.ExternAfterSim,
                org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed>() {
              @java.lang.Override
              public void invoke(
                  org.matsim.hybrid.MATSimInterface.ExternAfterSim request,
                  io.grpc.stub.StreamObserver<org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed> responseObserver) {
                serviceImpl.reqExternAfterSim(request, responseObserver);
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
            }))).build();
  }
}
