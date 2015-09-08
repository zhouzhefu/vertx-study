package com.jeff.study.vertx.example1;

import java.util.Set;

import com.jeff.study.vertx.util.ExampleRunner;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.shiro.ShiroAuth;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.FormLoginHandler;
import io.vertx.ext.web.handler.RedirectAuthHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.UserSessionHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;

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
		router.route("/sessioned/*").handler(SessionHandler.create(store));
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
			System.out.println(ctx.session().get("sessionKey1").toString());
			System.out.println(ctx.session().id());
			ctx.response().end("Key1 got.");
		});
		//this path should not be sessioned
		router.route("/nosession").handler(ctx -> {
			System.out.println("Want to get key1? Sorry. ");
			//will hit NPE because ctx.session() == null
			System.out.println(ctx.session().get("sessionKey1").toString());
			ctx.response().end();
		});
		
		
		//auth. Here exampled is a quite basic one, for more advanced/secured auth, please refer to JWTAuth
		AuthProvider authProvider = ShiroAuth.create(vertx, ShiroAuthRealmType.PROPERTIES, new JsonObject().put("properties_path", "classpath:test_auth.properties"));
		router.route("/auth/*").handler(CookieHandler.create());
		router.route("/auth/*").handler(SessionHandler.create(LocalSessionStore.create(vertx)));
		//UserSessionHandler should be added to root route of the module("/auth/*" in this case), 
		//if added to "/auth/authed/*", log out will not work as expected. 
		router.route("/auth/*").handler(UserSessionHandler.create(authProvider));
		//RedirectAuthHandler will by default redirect to ${webRoot}/loginpage
		//router.route("/auth/authed/*").handler(RedirectAuthHandler.create(authProvider));
		router.route("/auth/authed/*").handler(RedirectAuthHandler.create(authProvider, "/auth/authlogin"));
		router.route("/auth/authed/sample1").handler(ctx -> {
			//with effect of the RedirectAuthHandler, the result of "Sorry not today" will not be seen by user.
			String result = (ctx.user() != null)?"Thank you for access sample1, " + ctx.user() : "Sorry not today";
			StringBuilder html = new StringBuilder()
					.append("<html><head></head><body>")
					.append("<div>").append(result).append("</div>")
					.append("<a href=\"/auth/authed/logout\">").append("Log Out</a>")
					.append("</body></html>");
			ctx.response().end(html.toString());
		});
		router.route("/auth/authed/logout").handler(ctx -> {
			ctx.clearUser();
			ctx.response().putHeader("location", "/auth/authlogin").setStatusCode(302).end();
		});
		//handle login page & login action
		router.route("/auth/authlogin").handler(ctx -> {
			StringBuilder html = new StringBuilder();
			html.append("<html><head></head><body>")
				.append("<form action=\"loginhandler\" method=\"post\">")
				.append("UserName: <input type=\"text\" name=\"username\" /><br/>")
				.append("Password: <input type=\"text\" name=\"password\" /><br/>")
				.append("<button type=\"submit\">Submit</button>")
				.append("</form")
				.append("</body></html>");
			ctx.response().end(html.toString());
		});
		//Default FormLoginHandler has no redirection, simply display "Successful"
		//router.post("/auth/loginhandler").handler(FormLoginHandler.create(authProvider));
		router.post("/auth/loginhandler").handler(FormLoginHandler.create(
				authProvider, "username", "password", "/auth/authed/homes", "/auth/authed/homes"));
		/** below should be the actual implementation of above out of box "FormLoginHandler", 
		 *  guess in the login form the "username" and "password" fields must have.
		router.post("/auth/loginhandler").handler(ctx -> {
			JsonObject principal = new JsonObject();
			principal.put("username", ctx.request().getParam("username"));
			principal.put("password", ctx.request().getParam("password"));
			
			authProvider.authenticate(principal, res -> {
				String result = null;
				if (res.succeeded()) {
					User user = res.result();
					result = "Login successful: " + user.principal();
					ctx.setUser(user);
				} else {
					result = "Login failed";
				}
				
				ctx.response().end(result);
			});
		});
		*/
		router.route("/auth/authed/homes").handler(ctx -> {
			ctx.response().putHeader("content-type", "text/html").setChunked(true)
						.write("<div><a href=\"/auth/authed/jeffhome\">Jeff's Home</a></div>")
						.write("<div><a href=\"/auth/authed/winniehome\">Winnie's Home</a></div>")
						.write("<div><a href=\"/auth/authed/sample1\">Sample 1</a></div>")
						.write("<br>")
						.write("<div><a href=\"/auth/authed/logout\">Log Out</a></div>")
						.end();
		});
		
		
		//authorities partition
		router.route("/auth/authed/jeffhome").handler(
				RedirectAuthHandler.create(authProvider, "/auth/authlogin")
									.addAuthority("do_actual_work"));
		router.route("/auth/authed/jeffhome").handler(ctx -> {
			ctx.response().putHeader("content-type", "text/html")
						  .setChunked(true)
							.write("<div>Welcome home, Jeff</div>")
							.end("<div><a href=\"/auth/authed/logout\">Log Out</a></div>");
		});
		
		router.route("/auth/authed/winniehome").handler(
				RedirectAuthHandler.create(authProvider, "/auth/authlogin")
									.addAuthority("place_order"));
		router.route("/auth/authed/winniehome").handler(ctx -> {
			StringBuilder output = new StringBuilder();
			output.append("<div>Welcome home, Winnie</div>")
				  .append("<div><a href=\"/auth/authed/logout\">Log Out</a></div>");
			
			ctx.response().putHeader("content-type", "text/html")
						  /*.setChunked(false)*/.putHeader("content-length", "" + output.length())
							.write("<div>Welcome home, Winnie</div>")
							.end("<div><a href=\"/auth/authed/logout\">Log Out</a></div>");
		});
		
		
		// SockJS
		SockJSHandlerOptions options = new SockJSHandlerOptions().setHeartbeatInterval(2000);
		SockJSHandler sockJSHandler = SockJSHandler.create(vertx, options);
		sockJSHandler.socketHandler(socket -> {
			System.out.println("In Socket...");
			socket.handler(socket::write);
		});
		router.route("/sockjs").handler(sockJSHandler);
		router.route("/sockjs/info").handler(ctx -> {ctx.response().end("{}");});
		
		//without the static handler, those  nhggvfg/images/css resources will not be served
		//and this should be put at the last route setting, otherwise all the below route will not work
		router.route().handler(StaticHandler.create().setCachingEnabled(false));
		
		vertx.createHttpServer().requestHandler(router::accept).listen(8989);
	}
	
	

}
