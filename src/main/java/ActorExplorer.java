import akka.actor.AbstractActor;
import akka.http.javadsl.model.ws.TextMessage;
import akka.japi.pf.ReceiveBuilder;

import java.util.Map;

public class ActorExplorer extends AbstractActor {
    private Map<String, >

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(FindMessage.class, msg -> {

                })
                .match(TestMessage.class, msg -> {

                }).
        build();
    }
}
