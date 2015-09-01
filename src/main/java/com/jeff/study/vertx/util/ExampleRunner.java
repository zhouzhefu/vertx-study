package com.jeff.study.vertx.util;

import java.util.function.Consumer;

import io.vertx.core.Vertx;

public class ExampleRunner {

	public ExampleRunner() {
	}
	
	public static void run(Class clazz) {
		Vertx theVertx = Vertx.vertx();
		String deploymentID = clazz.getName();
		Consumer<Vertx> runner = vertx -> {
			vertx.deployVerticle(deploymentID);
		};
		runner.accept(theVertx);
	}

}
