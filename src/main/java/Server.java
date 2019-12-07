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
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import javafx.util.Pair;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import scala.compat.java8.FutureConverters;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

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
            return new Pair<>(url, count);
        }).mapAsync(MAX_STREAMS, msg ->
            Patterns.ask(explorer, new FindMessage(msg.getKey()), TIMEOUT)
                .thenCompose(answer ->
                            answer.getClass() == TestMessage.class ?
                            CompletableFuture.completedFuture(answer)
                            : Source
                                .from(Collections.singletonList(msg))
                                .toMat(testSink(), Keep.right())
                                .run(materializer)
                                .thenCompose(sum -> CompletableFuture(new TestMessage(msg.getKey(),  sum / msg.getValue()))));



        );

    }

    private static Sink<Long> testSink() {
        Flow.<Pair<String, Integer>>create()
                .mapConcat(msg -> Collections.nCopies(msg.getValue(), msg.getKey()))
                .mapAsync(MAX_STREAMS, url -> {
                    Long zeroTime = System.nanoTime();
                    AsyncHttpClient client = Dsl.asyncHttpClient();

                    return client
                            .prepareGet(url)
                            .execute()
                            .toCompletableFuture()
                            .thenCompose(response)
                })

    }
}
