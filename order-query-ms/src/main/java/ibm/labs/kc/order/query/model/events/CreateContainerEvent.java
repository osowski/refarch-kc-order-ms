package ibm.labs.kc.order.query.model.events;

import ibm.labs.kc.order.query.model.Container;

public class CreateContainerEvent extends ContainerEvent {
	
	private Container payload;
	
	public CreateContainerEvent(long timestampMillis, String version, Container payload) {
        super(timestampMillis, ContainerEvent.TYPE_CREATED, version);
        this.payload = payload;
    }

    public CreateContainerEvent() {
    }

	public Container getPayload() {
		return payload;
	}

	public void setPayload(Container payload) {
		this.payload = payload;
	}

}
