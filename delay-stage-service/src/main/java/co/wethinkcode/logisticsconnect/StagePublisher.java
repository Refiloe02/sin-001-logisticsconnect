package co.wethinkcode.logisticsconnect;

public interface StagePublisher {

    void publish(StageChange change) throws Exception;
}
