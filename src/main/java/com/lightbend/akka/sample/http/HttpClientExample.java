package com.lightbend.akka.sample.http;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;

public class HttpClientExample {
	
   static SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    static void log(String mesg) {
    	System.out.println(sdfDate.format(new Date()) + ": " + mesg);
    }
    
    public static String makeRequest(ActorSystem system, ActorMaterializer materializer, String taskDetail) {
		final CompletionStage<HttpResponse> responseFuture =
				  Http.get(system)
				      .singleRequest(HttpRequest.create("http://localhost:9090/token?scopes=" +taskDetail));
				

				log("Akka. make the request for " + taskDetail);
				responseFuture.thenAccept(resp -> {
					Jackson.unmarshaller(String.class).unmarshal(resp.entity(), materializer).thenApplyAsync(str -> {
						log("Akka. Got the response: " + str);
						return str;
					});
				});
				
				return taskDetail;
    }
    
    public static Callable<String> akkaBased(ActorSystem system, ActorMaterializer materializer, AtomicInteger taskNo) {
    	return new Callable<String>() {
			public String call() {
				return makeRequest(system, materializer, "task-" +taskNo.getAndIncrement());
			}
		};
    }
    
    public static Callable<String> jerseyBased(Client client, AtomicInteger taskNo) {
    	return new Callable<String>() {
			public String call() {
				
				//client.target("http://localhost:9090/token?scopes="+"task-" +taskNo.getAndIncrement())
		        //.request().async().get();
				
				log("Jersey. make the request for task: " + taskNo);
				String resp = client.target("http://localhost:9090/token?scopes="+"task-" +taskNo.getAndIncrement())
				        .request()
				        .get().readEntity(String.class);
				log("Jersey. Got the response: " + resp);
				return resp;
			}
		};
    }

	public static void main(String...args) throws InterruptedException, ExecutionException {
		
		Client client = ClientBuilder.newClient().register(JacksonJsonProvider.class);
		final ActorSystem system = ActorSystem.create();
		
		final ActorMaterializer materializer = ActorMaterializer.create(system);
		
		ExecutorService service = Executors.newFixedThreadPool(5);
		
		List<Callable<String>> tasks = new ArrayList<>();
		final AtomicInteger taskNo = new AtomicInteger(0);
		for (int i=0; i<10; i++) {
			tasks.add(akkaBased(system, materializer, taskNo));
			//tasks.add(jerseyBased(client, taskNo));
			
			// Verdict. Jersey is sync thread based, waits for thread availability from the pool. So not all 10 requests can go at one shot.
			// Akka runs requests its own on async methods and it is not limited by the executor thread pool availability.
		}

		service.invokeAll(tasks);
		
		service.awaitTermination(15, TimeUnit.SECONDS);
		service.shutdown();

	}
	
}
