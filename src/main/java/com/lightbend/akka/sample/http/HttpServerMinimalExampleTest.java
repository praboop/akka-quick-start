package com.lightbend.akka.sample.http;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

public class HttpServerMinimalExampleTest extends AllDirectives {

	public static final String TOKEN_URI = "token";

	public static void main(String[] args) throws Exception {
		// boot up server using the route as defined below
		ActorSystem system = ActorSystem.create("routes");
		Integer httpPort = 9090;

		final Http http = Http.get(system);
		final ActorMaterializer materializer = ActorMaterializer.create(system);

		// In order to access all directives we need an instance where the routes are
		// define.
		HttpServerMinimalExampleTest app = new HttpServerMinimalExampleTest();

		final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.testSimpleJsonRoutes().flow(system,
				materializer);
		final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
				ConnectHttp.toHost("localhost", httpPort), materializer);

		System.out.println("Server online at http://localhost:" + httpPort + "/\nPress RETURN to stop...");
		System.in.read(); // let it run until user presses return

		binding.thenCompose(ServerBinding::unbind) // trigger unbinding from the port
				.thenAccept(unbound -> system.terminate()); // and shutdown when done
	}

	private Route getHelloRoute() {
		return concat(path("hello", () -> get(() -> complete("<h1>Say hello to akka-http</h1>"))));
	}

	private Route testSimpleJsonRoutes() {
		Function<String, Route> routeTokenGet = scopes -> get(() -> complete(StatusCodes.OK,
				"Scopes are " + scopes + ". Status Code: " + StatusCodes.OK, Jackson.<String>marshaller()));
		Function<Token, Route> routeTokenPost = token -> post(() -> complete(StatusCodes.OK,
				"Token are " + token + ". Status Code: " + StatusCodes.OK, Jackson.<String>marshaller()));

		return concat(
				// GET with parameter scope
				parameter("scopes",
						scopesParamValue -> path(TOKEN_URI, () -> routeTokenGet.apply(scopesParamValue)).seal()),
				// POST with Json body of Token class
				path(TOKEN_URI, () -> entity(Jackson.unmarshaller(Token.class), routeTokenPost::apply).seal()));

	}
}