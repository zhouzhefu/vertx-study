package com.jeff.study.vertx.example1;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;

import java.util.Set;

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
		
		//file upload
		router.route().handler(BodyHandler.create().setUploadsDirectory("/Users/winniewang/Downloads"));
		router.post("/bodyhandler/uploads").handler(ctx -> {
			Set<FileUpload> uploads = ctx.fileUploads();
			uploads.stream().forEach(upload -> {
				System.out.println(upload.fileName());
				System.out.println(upload.uploadedFileName());
			});
			ctx.response().end("Upload done!");
		});
		
		router.route("/try/me").handler(ctx -> {
			ctx.response().end("try me ok!");
		});
		
		
		//handling session
		router.route().handler(CookieHandler.create()); //cookie support is the prerequisite of session support
		SessionStore store = LocalSessionStore.create(vertx);
		//only patterned "/sessioned/*" URL will manage session
		router.route().handler(SessionHandler.create(store));
		//this should be sessioned
		router.route("/sessioned/foo").handler(ctx -> {
			System.out.println("/sessioned/foo start...");
			ctx.session().put("sessionKey1", "SessionVal1");
			//vertx session is implemented by cookie, if you use an HTTP/REST tool other than a browser, 
			//remember check the request header "Cookie", by default they may always launched with a 
			//new cookie, and therefore vertx session is always a new one. 
			System.out.println(ctx.session().id());
			ctx.response().end("Key1 added.");
		});
		router.route("/sessioned/bar").handler(ctx -> {
			System.out.println("/sessioned/bar start...");
			System.out.println(ctx.session().get("sessionKey1"));
			System.out.println(ctx.session().id());
			ctx.response().end("Key1 got.");
		});
		//this should not be sessioned
		router.route("/nosession").handler(ctx -> {
			System.out.println("Want to get key1? Sorry. ");
			//will hit NPE because ctx.session() == null
			System.out.println(ctx.session().get("sessionKey1"));
			ctx.response().end();
		});
		
		
		
		
		//without the static handler, those html/images/css resources will not be served
		//and this should be put at the last route setting, otherwise all the below route will not work
		router.route().handler(StaticHandler.create());
		
		vertx.createHttpServer().requestHandler(router::accept).listen(8989);
	}
	
	

}
