package co.wethinkcode.logisticsconnect;

import java.io.IOException;

/** Where transit-service learns a hub's current delay stage*/
public interface DelayStageSource {

    int stageFor(String hubId) throws IOException;

    String name();
}
