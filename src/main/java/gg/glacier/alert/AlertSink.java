package gg.glacier.alert;

/** Anything that wants to react to alerts (Discord, database, webhooks, etc.). */
public interface AlertSink {
    void onAlert(AlertEvent event);
}
