import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.Query;
import akka.http.javadsl.server.AllDirectives;
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

import java.io.IOException;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;

import static config.Config.*;

public class Server  extends AllDirectives {

    public static void main(String[] args) throws IOException {
        System.out.println(ON_START);
        ActorSystem system = ActorSystem.create(SYSTEM_NAME);
        ActorRef explorer = system.actorOf(Props.create(ActorExplorer.class));

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = createFlow(http, explorer, materializer);//вызов метода которому передаем Http, ActorSystem и ActorMaterializer;
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(
                routeFlow,
                ConnectHttp.toHost(HOST, PORT),
                materializer
        );
        System.out.println(SERVER_START_MESSAGE);
        System.in.read();
        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done
    }

    private static Flow<HttpRequest, HttpResponse, NotUsed> createFlow(Http http, ActorRef explorer, ActorMaterializer materializer) {
        Flow.of(HttpRequest.class).map(h -> {
            Query q = h.getUri().query();
            String url = q.get(ULR_PARAMETER).get();
            Integer count = Integer.valueOf(q.get(COUNT_PARAMETER).get());
            return new TestMessage(url, count);
        }).mapAsync(MAX_STREAMS, msg ->
            Patterns.ask(explorer, new FindMessage(msg.getUrl()), TIMEOUT)
                    .thenCompose()
        )
    }
}
