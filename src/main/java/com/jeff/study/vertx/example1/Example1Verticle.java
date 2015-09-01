package com.jeff.study.vertx.example1;

import com.jeff.study.vertx.util.ExampleRunner;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;

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
		
		router.route("/moeuser/*").handler(context -> {
			context.response().putHeader("content-type", "text/plain").end("Hello World, lamda handler");
		});
		
		router.route("/chainMe").handler(context -> {
			context.response().setChunked(true).write("First, ");
			context.vertx().setPeriodic(3000, tid -> {context.next();});
		});
		router.route("/chainMe").handler(context -> {
			context.response().write("Second, ");
			context.vertx().setPeriodic(2000, tid -> {context.next();});
		});
		router.route("/chainMe").handler(context -> {
			context.response().end("Third. ");
		});
		
		vertx.createHttpServer().requestHandler(router::accept).listen(8989);
	}
	
	

}
