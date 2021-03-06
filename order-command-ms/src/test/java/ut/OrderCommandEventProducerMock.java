package ut;

import ibm.gse.orderms.domain.events.EventBase;
import ibm.gse.orderms.domain.events.EventEmitter;
import ibm.gse.orderms.domain.events.command.OrderCommandEvent;
import ibm.gse.orderms.domain.model.order.ShippingOrder;
import ibm.gse.orderms.infrastructure.repository.ShippingOrderRepository;


/**
 * Use this mockup class so data is saved to repo without going out to messaging layer
 *
 */
public class OrderCommandEventProducerMock implements EventEmitter{
	
	public OrderCommandEventProducerMock() {}
	
	public boolean eventEmitted = false;
	public OrderCommandEvent emittedEvent = null;
	public ShippingOrderRepository repo = null;
	
	public OrderCommandEventProducerMock(ShippingOrderRepository repo) {
		this.repo = repo;
	}
	
	@Override
	public void emit(EventBase event) throws Exception {
		this.eventEmitted = true;
		this.emittedEvent = (OrderCommandEvent)event;
		ShippingOrder shippingOrder = new ShippingOrder(emittedEvent.getPayload());
        switch (emittedEvent.getType()) {
		case OrderCommandEvent.ORDER_CREATED_TYPE:
			// Move to pending state to allow update
			shippingOrder.setStatus(ShippingOrder.PENDING_STATUS);
        	repo.addOrUpdateNewShippingOrder(shippingOrder);			
            break;
        case OrderCommandEvent.UPDATED_ORDER_TYPE:
        	repo.updateShippingOrder(shippingOrder);			
	           
        	break;
        }	
	}

	@Override
	public void safeClose() {
		//this.eventEmitted = false;
		//this.emittedEvent = null;	
	}

	public EventBase getEventEmitted() {
		return emittedEvent;
	}
	
	String expectedValue = "";
	
	public void defineEqualAssertion(String key, String value) {
		expectedValue = value;
	}
}
