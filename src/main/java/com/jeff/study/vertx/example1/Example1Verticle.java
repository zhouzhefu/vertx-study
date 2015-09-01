package com.jeff.study.vertx.example1;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

import com.jeff.study.vertx.util.ExampleRunner;

public class Example1Verticle extends AbstractVerticle {

	public Example1Verticle() {
	}

	public static void main(String[] args) {
		ExampleRunner.run(Example1Verticle.class);
	}

	@Override
	public void start() throws Exception {
		Router router = Router.router(vertx);
		
		/** the simplest server start without web
		vertx.createHttpServer().requestHandler(req -> {
			req.response().putHeader("content-type", "text/plain").end("Hello World! Man");
		}).listen(8989);
		*/
		
		//normal "hello world"
		router.route("/moeuser/*").handler(context -> {
			context.response().putHeader("content-type", "text/plain").end("Hello World, lamda handler");
		});
		
		//chained timer handlers set
		router.route("/chainMe").handler(context -> {
			context.response().setChunked(true).write("First, ");
			context.vertx().setTimer(3000, tid -> {context.next();});
		});
		router.route("/chainMe").handler(context -> {
			context.response().write("Second, ");
			context.vertx().setTimer(2000, tid -> {context.next();});
		});
		router.route("/chainMe").handler(context -> {
			System.out.println("3rd handler!");
			context.response().end("Third. ");
			
//			context.response().write("Third, ");
//			context.vertx().setTimer(500, tid -> {context.next();});
		});
		
		//RESTful path params
		router.post("/product/:producttype/:productid").handler(context -> {
			String productType = context.request().getParam("producttype");
			String productId = context.request().getParam("productid");
			System.out.println("POSTing productType: " + productType + " and productID: " + productId);
			context.response().setStatusCode(200).end("Succesful");
		});
		
		//route by headers, multi consumes() & produces() are in relationship of "&&"
		router.route().consumes("text/json").consumes("text/*").produces("application/json")
			.handler(context -> {
			System.out.println("Print me, Text/JSON successful!");
			context.response().setStatusCode(200).end();
		});
		
		//subrouter, can be access by /subroute/products/:id
		Router subRouter = Router.router(vertx);
		subRouter.get("/products/:id").handler(ctx -> {
			System.out.println("GET product: " + ctx.request().getParam("id"));
			ctx.response().setStatusCode(200).end();
		});
		subRouter.put("/products/:id").handler(ctx -> {
			System.out.println("PUT product: " + ctx.request().getParam("id"));
			ctx.response().setStatusCode(200).end();
		});
		
		router.mountSubRouter("/subroute", subRouter);
		
		//error handler
		Route r1 = router.post("/errorhub/r1").handler(ctx -> {
			throw new RuntimeException("r1 failure!");
		});
		Route r2 = router.get("/errorhub/r2").handler(ctx -> {
			ctx.fail(403);
		});
		Route r3 = router.route("/errorhub/*").failureHandler(failureCtx -> {
			System.out.println(failureCtx.statusCode());
			failureCtx.response().setStatusCode(failureCtx.statusCode()).end("Sorry not today!");
		});
		
		
		vertx.createHttpServer().requestHandler(router::accept).listen(8989);
	}
	
	

}
