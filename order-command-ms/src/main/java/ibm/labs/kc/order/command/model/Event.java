package ibm.labs.kc.order.command.model;

public interface Event {

    public long getTimestampMillis();

    public void setTimestampMillis(long timestampMillis);

    public String getType();

    public void setType(String type);

    public void setVersion(String version);

    public String getVersion();
}