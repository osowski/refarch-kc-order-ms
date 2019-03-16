package ibm.labs.kc.order.query.action;

import java.util.Collection;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import ibm.labs.kc.order.query.model.Order;
import ibm.labs.kc.order.query.model.events.Event;
import ibm.labs.kc.order.query.model.events.EventListener;
import ibm.labs.kc.order.query.model.events.OrderEvent;
import ibm.labs.kc.order.query.model.Cancellation;
import ibm.labs.kc.order.query.model.ContainerAssignment;
import ibm.labs.kc.order.query.model.Rejection;
import ibm.labs.kc.order.query.model.VoyageAssignment;
import ibm.labs.kc.order.query.model.events.AssignContainerEvent;
import ibm.labs.kc.order.query.model.events.AssignOrderEvent;
import ibm.labs.kc.order.query.model.events.CancelOrderEvent;
import ibm.labs.kc.order.query.model.events.ContainerDeliveredEvent;
import ibm.labs.kc.order.query.model.events.ContainerOffShipEvent;
import ibm.labs.kc.order.query.model.events.ContainerOnShipEvent;
import ibm.labs.kc.order.query.model.events.CreateOrderEvent;
import ibm.labs.kc.order.query.model.events.OrderCompletedEvent;
import ibm.labs.kc.order.query.model.events.RejectOrderEvent;
import ibm.labs.kc.order.query.model.events.UpdateOrderEvent;

@Path("orders")
public class OrderActionService implements EventListener{
	
	static final Logger logger = LoggerFactory.getLogger(OrderActionService.class);

    private OrderActionDAO orderActionDAO;

    public OrderActionService() {
    	orderActionDAO = OrderActionDAOImpl.instance();
    }
	
	@GET
    @Path("orderHistory/{orderId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Order history by order ID", description = "")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Order history found", content = @Content(mediaType = "application/json")) })
    public Response getOrderHistory(@PathParam("orderId") String orderId) {
        logger.info("OrderActionService.getOrderHistory(" + orderId + ")");
        Collection<OrderAction> orderAction = orderActionDAO.getOrderStatus(orderId);
        return Response.ok(orderAction, MediaType.APPLICATION_JSON).build();
    }

	@Override
	public void handle(Event event) {
		String orderID;
        Optional<OrderActionInfo> oqo;
        try {
            OrderEvent orderEvent = (OrderEvent) event;
            System.out.println("@@@@ in handle order action" + new Gson().toJson(orderEvent));
            switch (orderEvent.getType()) {
            case OrderEvent.TYPE_CREATED:
                synchronized (orderActionDAO) {
                    Order o1 = ((CreateOrderEvent) orderEvent).getPayload();
                    long timestampMillis = ((CreateOrderEvent) orderEvent).getTimestampMillis();
                    String action = ((CreateOrderEvent) orderEvent).getType();
                    OrderActionInfo orderActionItem = OrderActionInfo.newFromOrder(o1);
                    OrderAction orderAction = OrderAction.newFromOrder(orderActionItem, timestampMillis, action);
                    orderActionDAO.add(orderAction);
                    orderActionDAO.orderHistory(orderAction);
                }
                break;
            case OrderEvent.TYPE_UPDATED:
                synchronized (orderActionDAO) {
                    Order o2 = ((UpdateOrderEvent) orderEvent).getPayload();
                    long timestampMillis = ((UpdateOrderEvent) orderEvent).getTimestampMillis();
                    String action = ((UpdateOrderEvent) orderEvent).getType();
                    orderID = o2.getOrderID();
                    oqo = orderActionDAO.getById(orderID);
                    if (oqo.isPresent()) {
                    	OrderActionInfo orderActionItem = oqo.get();
                    	orderActionItem.update(o2);
                    	OrderAction cqo = OrderAction.newFromOrder(orderActionItem, timestampMillis, action);
                    	orderActionDAO.update(cqo);
                    	orderActionDAO.orderHistory(cqo);
                    } else {
                        throw new IllegalStateException("Cannot update - Unknown order Id " + orderID);
                    }
                }
                break;
            case OrderEvent.TYPE_ASSIGNED:
                synchronized (orderActionDAO) {
                    VoyageAssignment voyageAssignment = ((AssignOrderEvent) orderEvent).getPayload();
                    long timestampMillis = ((AssignOrderEvent) orderEvent).getTimestampMillis();
                    String action = ((AssignOrderEvent) orderEvent).getType();
                    orderID = voyageAssignment.getOrderID();
                    oqo = orderActionDAO.getById(orderID);
                    if (oqo.isPresent()) {
                    	OrderActionInfo orderActionItem = oqo.get();
                    	orderActionItem.assign(voyageAssignment);
                    	OrderAction orderAction = OrderAction.newFromOrder(orderActionItem, timestampMillis, action);
                        orderActionDAO.update(orderAction);
                        orderActionDAO.orderHistory(orderAction);
                    } else {
                        throw new IllegalStateException("Cannot update - Unknown order Id " + orderID);
                    }
                }
                break;
            case OrderEvent.TYPE_REJECTED:
                synchronized (orderActionDAO) {
                    Rejection rejection = ((RejectOrderEvent) orderEvent).getPayload();
                    long timestampMillis = ((RejectOrderEvent) orderEvent).getTimestampMillis();
                    String action = ((RejectOrderEvent) orderEvent).getType();
                    orderID = rejection.getOrderID();
                    oqo = orderActionDAO.getById(orderID);
                    if (oqo.isPresent()) {
                    	OrderActionInfo orderActionItem = oqo.get();
                    	orderActionItem.reject(rejection);
                    	OrderAction orderAction = OrderAction.newFromOrder(orderActionItem, timestampMillis, action);
                    	orderActionDAO.update(orderAction);
                    	orderActionDAO.orderHistory(orderAction);
                    } else {
                        throw new IllegalStateException("Cannot update - Unknown order Id " + orderID);
                    }
                }
                break;
            case OrderEvent.TYPE_CONTAINER_ALLOCATED:
                synchronized (orderActionDAO) {
                	ContainerAssignment container = ((AssignContainerEvent) orderEvent).getPayload();
                	long timestampMillis = ((AssignContainerEvent) orderEvent).getTimestampMillis();
                	String action = ((AssignContainerEvent) orderEvent).getType();
                    orderID = container.getOrderID();
                    oqo = orderActionDAO.getById(orderID);
                    if (oqo.isPresent()) {
                    	OrderActionInfo orderActionItem = oqo.get();
                    	orderActionItem.assignContainer(container);
                    	OrderAction orderAction = OrderAction.newFromOrder(orderActionItem, timestampMillis, action);
                    	orderActionDAO.update(orderAction);
                    	orderActionDAO.orderHistory(orderAction);
                    } else {
                        throw new IllegalStateException("Cannot update - Unknown order Id " + orderID);
                    }
                }
                break;
            case OrderEvent.TYPE_CONTAINER_ON_SHIP:
                synchronized (orderActionDAO) {
                	ContainerAssignment container = ((ContainerOnShipEvent) orderEvent).getPayload();
                	long timestampMillis = ((ContainerOnShipEvent) orderEvent).getTimestampMillis();
                	String action = ((ContainerOnShipEvent) orderEvent).getType();
                    orderID = container.getOrderID();
                    oqo = orderActionDAO.getById(orderID);
                    if (oqo.isPresent()) {
                    	OrderActionInfo orderActionItem = oqo.get();
                    	orderActionItem.containerOnShip(container);
                    	OrderAction orderAction = OrderAction.newFromOrder(orderActionItem, timestampMillis, action);
                    	orderActionDAO.update(orderAction);
                    	orderActionDAO.orderHistory(orderAction);
                    } else {
                        throw new IllegalStateException("Cannot update - Unknown order Id " + orderID);
                    }
                }
                break;
            case OrderEvent.TYPE_CONTAINER_OFF_SHIP:
                synchronized (orderActionDAO) {
                	ContainerAssignment container = ((ContainerOffShipEvent) orderEvent).getPayload();
                	long timestampMillis = ((ContainerOffShipEvent) orderEvent).getTimestampMillis();
                	String action = ((ContainerOffShipEvent) orderEvent).getType();
                    orderID = container.getOrderID();
                    oqo = orderActionDAO.getById(orderID);
                    if (oqo.isPresent()) {
                    	OrderActionInfo orderActionItem = oqo.get();
                    	orderActionItem.containerOffShip(container);
                    	OrderAction orderAction = OrderAction.newFromOrder(orderActionItem, timestampMillis, action);
                    	orderActionDAO.update(orderAction);
                    	orderActionDAO.orderHistory(orderAction);
                    } else {
                        throw new IllegalStateException("Cannot update - Unknown order Id " + orderID);
                    }
                }
                break;
            case OrderEvent.TYPE_CONTAINER_DELIVERED:
                synchronized (orderActionDAO) {
                	ContainerAssignment container = ((ContainerDeliveredEvent) orderEvent).getPayload();
                	long timestampMillis = ((ContainerDeliveredEvent) orderEvent).getTimestampMillis();
                	String action = ((ContainerDeliveredEvent) orderEvent).getType();
                    orderID = container.getOrderID();
                    oqo = orderActionDAO.getById(orderID);
                    if (oqo.isPresent()) {
                    	OrderActionInfo orderActionItem = oqo.get();
                    	orderActionItem.containerDelivered(container);
                    	OrderAction orderAction = OrderAction.newFromOrder(orderActionItem, timestampMillis, action);
                    	orderActionDAO.update(orderAction);
                    	orderActionDAO.orderHistory(orderAction);
                    } else {
                        throw new IllegalStateException("Cannot update - Unknown order Id " + orderID);
                    }
                }
                break;
            case OrderEvent.TYPE_CANCELLED:
                synchronized (orderActionDAO) {
                    Cancellation cancellation = ((CancelOrderEvent) orderEvent).getPayload();
                    long timestampMillis = ((CancelOrderEvent) orderEvent).getTimestampMillis();
                    String action = ((CancelOrderEvent) orderEvent).getType();
                    orderID = cancellation.getOrderID();
                    oqo = orderActionDAO.getById(orderID);
                    if (oqo.isPresent()) {
                    	OrderActionInfo orderActionItem = oqo.get();
                    	orderActionItem.cancel(cancellation);
                    	OrderAction orderAction = OrderAction.newFromOrder(orderActionItem, timestampMillis, action);
                    	orderActionDAO.update(orderAction);
                    	orderActionDAO.orderHistory(orderAction);
                    } else {
                        throw new IllegalStateException("Cannot update - Unknown order Id " + orderID);
                    }
                }
                break;
            case OrderEvent.TYPE_COMPLETED:
                synchronized (orderActionDAO) {
                    Order order = ((OrderCompletedEvent) orderEvent).getPayload();
                    long timestampMillis = ((OrderCompletedEvent) orderEvent).getTimestampMillis();
                    String action = ((OrderCompletedEvent) orderEvent).getType();
                    orderID = order.getOrderID();
                    oqo = orderActionDAO.getById(orderID);
                    if (oqo.isPresent()) {
                    	OrderActionInfo orderActionItem = oqo.get();
                    	orderActionItem.orderCompleted(order);
                    	OrderAction orderAction = OrderAction.newFromOrder(orderActionItem, timestampMillis, action);
                    	orderActionDAO.update(orderAction);
                    	orderActionDAO.orderHistory(orderAction);
                    } else {
                        throw new IllegalStateException("Cannot update - Unknown order Id " + orderID);
                    }
                }
                break;
            default:
                logger.warn("Unknown event type: " + orderEvent);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
		
	}

}