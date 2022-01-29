package lsp.shipment;

import demand.UtilityFunction;
import lsp.LogisticsSolutionElement;
import lsp.functions.LSPInfo;
import lsp.resources.LSPResource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.TimeWindow;

import java.util.ArrayList;

public class ShipmentUtils{
	private ShipmentUtils(){} // do not instantiate

	public static class LSPShipmentBuilder{

		private final Id<LSPShipment> id;
		private Id<Link> fromLinkId;
		private Id<Link> toLinkId;
		private TimeWindow startTimeWindow;
		private TimeWindow endTimeWindow;
		private int capacityDemand;
		private double deliveryServiceTime;
		private double pickupServiceTime;
		private final ArrayList<Requirement> requirements;
		private final ArrayList<UtilityFunction> utilityFunctions;
		private final ArrayList<LSPInfo> infos;

		public static LSPShipmentBuilder newInstance( Id<LSPShipment> id ){
			return new LSPShipmentBuilder(id);
		}

		private LSPShipmentBuilder( Id<LSPShipment> id ){
			this.requirements = new ArrayList<>();
			this.utilityFunctions = new ArrayList<>();
			this.infos = new ArrayList<>();
			this.id = id;
		}

		public LSPShipmentBuilder setFromLinkId( Id<Link> fromLinkId ){
			this.fromLinkId = fromLinkId;
			return this;
		}

		public LSPShipmentBuilder setToLinkId( Id<Link> toLinkId ){
			this.toLinkId = toLinkId;
			return this;
		}

		public LSPShipmentBuilder setStartTimeWindow( TimeWindow startTimeWindow ){
			this.startTimeWindow = startTimeWindow;
			return this;
		}

		public LSPShipmentBuilder setEndTimeWindow( TimeWindow endTimeWindow ){
			this.endTimeWindow = endTimeWindow;
			return this;
		}

		public LSPShipmentBuilder setCapacityDemand( int capacityDemand ){
			this.capacityDemand = capacityDemand;
			return this;
		}

		public LSPShipmentBuilder setDeliveryServiceTime( double serviceTime ){
			this.deliveryServiceTime = serviceTime;
			return this;
		}
		public LSPShipmentBuilder setPickupServiceTime( double serviceTime ){
			this.pickupServiceTime = serviceTime;
			return this;
		}

		public LSPShipmentBuilder addRequirement( Requirement requirement ) {
			requirements.add(requirement);
			return this;
		}

		public LSPShipmentBuilder addUtilityFunction( UtilityFunction utilityFunction ) {
			utilityFunctions.add(utilityFunction);
			return this;
		}

		public LSPShipmentBuilder addInfo( LSPInfo info ) {
			infos.add(info);
			return this;
		}

		public LSPShipment build(){
			return new LSPShipmentImpl(this);
		}

		// --- Getters ---

		public Id<LSPShipment> getId() {
			return id;
		}

		public Id<Link> getFromLinkId() {
			return fromLinkId;
		}

		public Id<Link> getToLinkId() {
			return toLinkId;
		}

		public TimeWindow getStartTimeWindow() {
			return startTimeWindow;
		}

		public TimeWindow getEndTimeWindow() {
			return endTimeWindow;
		}

		public int getCapacityDemand() {
			return capacityDemand;
		}

		public double getDeliveryServiceTime() {
			return deliveryServiceTime;
		}
		public double getPickupServiceTime(){
			return pickupServiceTime;
		}

		public ArrayList<Requirement> getRequirements() {
			return requirements;
		}

		public ArrayList<UtilityFunction> getUtilityFunctions() {
			return utilityFunctions;
		}

		public ArrayList<LSPInfo> getInfos() {
			return infos;
		}

	}

	public static class LoggedShipmentHandleBuilder {
		private double startTime;
		private double endTime;
		private LogisticsSolutionElement element;
		private Id<LSPResource> resourceId;
		private Id<Link> linkId;

		private LoggedShipmentHandleBuilder(){
		}

		public static LoggedShipmentHandleBuilder newInstance(){
			return new LoggedShipmentHandleBuilder();
		}

		public LoggedShipmentHandleBuilder setStartTime(double startTime){
			this.startTime = startTime;
			return this;
		}

		public LoggedShipmentHandleBuilder setEndTime(double endTime){
			this.endTime = endTime;
			return this;
		}

		public LoggedShipmentHandleBuilder setLogisticsSolutionElement(LogisticsSolutionElement element){
			this.element = element;
			return this;
		}

		public LoggedShipmentHandleBuilder setResourceId(Id<LSPResource> resourceId){
			this.resourceId = resourceId;
			return this;
		}

		public LoggedShipmentHandleBuilder setLinkId(Id<Link> linkId){
			this.linkId = linkId;
			return this;
		}

		public ShipmentPlanElement build(){
			return new LoggedShipmentHandle(this);
		}

		// --- Getters --- //

		public double getStartTime() {
			return startTime;
		}

		public double getEndTime() {
			return endTime;
		}

		public LogisticsSolutionElement getElement() {
			return element;
		}

		public Id<LSPResource> getResourceId() {
			return resourceId;
		}

		public Id<Link> getLinkId() {
			return linkId;
		}
	}

	public static class LoggedShipmentLoadBuilder {
		private double startTime;
		private double endTime;
		private LogisticsSolutionElement element;
		private Id<LSPResource> resourceId;
		private Id<Carrier> carrierId;
		private Id<Link> linkId;

		private LoggedShipmentLoadBuilder(){
		}

		public static LoggedShipmentLoadBuilder newInstance(){
			return new LoggedShipmentLoadBuilder();
		}

		public LoggedShipmentLoadBuilder setStartTime(double startTime){
			this.startTime = startTime;
			return this;
		}

		public LoggedShipmentLoadBuilder setEndTime(double endTime){
			this.endTime = endTime;
			return this;
		}

		public LoggedShipmentLoadBuilder setLogisticsSolutionElement(LogisticsSolutionElement element){
			this.element = element;
			return this;
		}

		public LoggedShipmentLoadBuilder setResourceId(Id<LSPResource> resourceId){
			this.resourceId = resourceId;
			return this;
		}

		public LoggedShipmentLoadBuilder setLinkId(Id<Link> linkId){
			this.linkId = linkId;
			return this;
		}

		public LoggedShipmentLoadBuilder setCarrierId(Id<Carrier> carrierId){
			this.carrierId = carrierId;
			return this;
		}

		public LoggedShipmentLoad build(){
			return new LoggedShipmentLoad(this);
		}

		// --- Getters --- //

		public double getStartTime() {
			return startTime;
		}

		public double getEndTime() {
			return endTime;
		}

		public LogisticsSolutionElement getElement() {
			return element;
		}

		public Id<LSPResource> getResourceId() {
			return resourceId;
		}

		public Id<Carrier> getCarrierId() {
			return carrierId;
		}

		public Id<Link> getLinkId() {
			return linkId;
		}
	}

	public static class LoggedShipmentTransportBuilder {
		private double startTime;
		private LogisticsSolutionElement element;
		private Id<LSPResource> resourceId;
		private Id<Link> fromLinkId;
		private Id<Link> toLinkId;
		private Id<Carrier> carrierId;

		private LoggedShipmentTransportBuilder(){
		}

		public static LoggedShipmentTransportBuilder newInstance(){
			return new LoggedShipmentTransportBuilder();
		}

		public LoggedShipmentTransportBuilder setStartTime(double startTime){
			this.startTime = startTime;
			return this;
		}

		public LoggedShipmentTransportBuilder setLogisticsSolutionElement(LogisticsSolutionElement element){
			this.element = element;
			return this;
		}

		public LoggedShipmentTransportBuilder setResourceId(Id<LSPResource> resourceId){
			this.resourceId = resourceId;
			return this;
		}

		public LoggedShipmentTransportBuilder setFromLinkId(Id<Link> fromLinkId){
			this.fromLinkId = fromLinkId;
			return this;
		}

		public LoggedShipmentTransportBuilder setToLinkId(Id<Link> toLinkId){
			this.toLinkId = toLinkId;
			return this;
		}

		public LoggedShipmentTransportBuilder setCarrierId(Id<Carrier> carrierId){
			this.carrierId = carrierId;
			return this;
		}

		public LoggedShipmentTransport build(){
			return new LoggedShipmentTransport(this);
		}

		// --- Getters --- //
		public double getStartTime() {
			return startTime;
		}

		public LogisticsSolutionElement getElement() {
			return element;
		}

		public Id<LSPResource> getResourceId() {
			return resourceId;
		}

		public Id<Link> getFromLinkId() {
			return fromLinkId;
		}

		public Id<Link> getToLinkId() {
			return toLinkId;
		}

		public Id<Carrier> getCarrierId() {
			return carrierId;
		}
	}

	public static class ScheduledShipmentUnloadBuilder{
		double startTime;
		double endTime;
		LogisticsSolutionElement element;
		Id<LSPResource> resourceId;
		Id<Carrier> carrierId;
		Id<Link> linkId;
		CarrierService carrierService;

		private ScheduledShipmentUnloadBuilder(){
		}

		public static ScheduledShipmentUnloadBuilder newInstance(){
			return new ScheduledShipmentUnloadBuilder();
		}

		public ScheduledShipmentUnloadBuilder setStartTime( double startTime ){
			this.startTime = startTime;
			return this;
		}

		public ScheduledShipmentUnloadBuilder setEndTime( double endTime ){
			this.endTime = endTime;
			return this;
		}

		public ScheduledShipmentUnloadBuilder setLogisticsSolutionElement( LogisticsSolutionElement element ){
			this.element = element;
			return this;
		}

		public ScheduledShipmentUnloadBuilder setResourceId( Id<LSPResource> resourceId ){
			this.resourceId = resourceId;
			return this;
		}

		public ScheduledShipmentUnloadBuilder setCarrierId( Id<Carrier> carrierId ){
			this.carrierId = carrierId;
			return this;
		}

		public ScheduledShipmentUnloadBuilder setLinkId( Id<Link> linkId ){
			this.linkId = linkId;
			return this;
		}

		public ScheduledShipmentUnloadBuilder setCarrierService( CarrierService carrierService ){
			this.carrierService = carrierService;
			return this;
		}

		public ScheduledShipmentUnload build(){
			return new ScheduledShipmentUnload(this);
		}
	}

	public static class ScheduledShipmentTransportBuilder{
		double startTime;
		double endTime;
		LogisticsSolutionElement element;
		Id<LSPResource> resourceId;
		Id<Carrier> carrierId;
		Id<Link> fromLinkId;
		Id<Link> toLinkId;
		CarrierService carrierService;

		private ScheduledShipmentTransportBuilder(){
		}

		public static ScheduledShipmentTransportBuilder newInstance(){
			return new ScheduledShipmentTransportBuilder();
		}

		public ScheduledShipmentTransportBuilder setStartTime( double startTime ){
			this.startTime = startTime;
			return this;
		}

		public ScheduledShipmentTransportBuilder setEndTime( double endTime ){
			this.endTime = endTime;
			return this;
		}

		public ScheduledShipmentTransportBuilder setLogisticsSolutionElement( LogisticsSolutionElement element ){
			this.element = element;
			return this;
		}

		public ScheduledShipmentTransportBuilder setResourceId( Id<LSPResource> resourceId ){
			this.resourceId = resourceId;
			return this;
		}

		public ScheduledShipmentTransportBuilder setCarrierId( Id<Carrier> carrierId ){
			this.carrierId = carrierId;
			return this;
		}

		public ScheduledShipmentTransportBuilder setFromLinkId( Id<Link> fromLinkId ){
			this.fromLinkId = fromLinkId;
			return this;
		}

		public ScheduledShipmentTransportBuilder setToLinkId( Id<Link> toLinkId ){
			this.toLinkId = toLinkId;
			return this;
		}

		public ScheduledShipmentTransportBuilder setCarrierService( CarrierService carrierService ){
			this.carrierService = carrierService;
			return this;
		}

		public ScheduledShipmentTransport build(){
			return new ScheduledShipmentTransport(this);
		}
	}

	public static class ScheduledShipmentLoadBuilder{
		double startTime;
		double endTime;
		LogisticsSolutionElement element;
		Id<LSPResource> resourceId;
		Id<Carrier> carrierId;
		Id<Link> linkId;
		CarrierService carrierService;

		private ScheduledShipmentLoadBuilder(){
		}

		public static ScheduledShipmentLoadBuilder newInstance(){
			return new ScheduledShipmentLoadBuilder();
		}

		public ScheduledShipmentLoadBuilder setStartTime( double startTime ){
			this.startTime = startTime;
			return this;
		}

		public ScheduledShipmentLoadBuilder setEndTime( double endTime ){
			this.endTime = endTime;
			return this;
		}

		public ScheduledShipmentLoadBuilder setLogisticsSolutionElement( LogisticsSolutionElement element ){
			this.element = element;
			return this;
		}

		public ScheduledShipmentLoadBuilder setResourceId( Id<LSPResource> resourceId ){
			this.resourceId = resourceId;
			return this;
		}

		public ScheduledShipmentLoadBuilder setLinkId( Id<Link> linkId ){
			this.linkId = linkId;
			return this;
		}

		public ScheduledShipmentLoadBuilder setCarrierId( Id<Carrier> carrierId ){
			this.carrierId = carrierId;
			return this;
		}

		public ScheduledShipmentLoadBuilder setCarrierService( CarrierService carrierService ){
			this.carrierService = carrierService;
			return this;
		}

		public ScheduledShipmentLoad build(){
			return new ScheduledShipmentLoad(this);
		}
	}

	public static class ScheduledShipmentHandleBuilder{
		double startTime;
		double endTime;
		LogisticsSolutionElement element;
		Id<LSPResource> resourceId;
		private Id<Link> linkId;

		private ScheduledShipmentHandleBuilder(){
		}

		public static ScheduledShipmentHandleBuilder newInstance(){
			return new ScheduledShipmentHandleBuilder();
		}

		public ScheduledShipmentHandleBuilder setStartTime( double startTime ){
			this.startTime = startTime;
			return this;
		}

		public ScheduledShipmentHandleBuilder setEndTime( double endTime ){
			this.endTime = endTime;
			return this;
		}

		public ScheduledShipmentHandleBuilder setLogisticsSolutionElement( LogisticsSolutionElement element ){
			this.element = element;
			return this;
		}

		public ScheduledShipmentHandleBuilder setResourceId( Id<LSPResource> resourceId ){
			this.resourceId = resourceId;
			return this;
		}

		public ScheduledShipmentHandleBuilder setLinkId( Id<Link> linkId ){
			this.linkId = linkId;
			return this;
		}

		public ScheduledShipmentHandle build(){
			return new ScheduledShipmentHandle(this);
		}
	}

	public static class LoggedShipmentUnloadBuilder{
		double startTime;
		double endTime;
		LogisticsSolutionElement element;
		Id<LSPResource> resourceId;
		Id<Carrier> carrierId;
		Id<Link> linkId;

		private LoggedShipmentUnloadBuilder(){
		}

		public static LoggedShipmentUnloadBuilder newInstance(){
			return new LoggedShipmentUnloadBuilder();
		}

		public LoggedShipmentUnloadBuilder setStartTime( double startTime ){
			this.startTime = startTime;
			return this;
		}

		public LoggedShipmentUnloadBuilder setEndTime( double endTime ){
			this.endTime = endTime;
			return this;
		}

		public LoggedShipmentUnloadBuilder setLogisticsSolutionElement( LogisticsSolutionElement element ){
			this.element = element;
			return this;
		}

		public LoggedShipmentUnloadBuilder setResourceId( Id<LSPResource> resourceId ){
			this.resourceId = resourceId;
			return this;
		}

		public LoggedShipmentUnloadBuilder setLinkId( Id<Link> linkId ){
			this.linkId = linkId;
			return this;
		}

		public LoggedShipmentUnloadBuilder setCarrierId( Id<Carrier> carrierId ){
			this.carrierId = carrierId;
			return this;
		}

		public LoggedShipmentUnload build(){
			return new LoggedShipmentUnload(this);
		}
	}
}
