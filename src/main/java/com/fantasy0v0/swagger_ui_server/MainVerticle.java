package com.fantasy0v0.swagger_ui_server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.io.*;
import java.util.Set;

public class MainVerticle extends AbstractVerticle {

  private final String FileName = "swagger.json";

  @Override
  public void start(Promise<Void> startPromise) throws IOException {
    InputStream resourceAsStream = MainVerticle.class.getClassLoader().getResourceAsStream(FileName);
    if (null == resourceAsStream) {
      startPromise.fail("Please check if " + FileName + " is in classpath");
      return;
    }
    byte[] bytes = resourceAsStream.readAllBytes();
    this.writeSwaggerJson(vertx, Buffer.buffer(bytes));
    Router router = Router.router(vertx);
    router.get("/" + FileName).handler(this::getSwaggerJson);
    router.post("/" + FileName).handler(BodyHandler.create()).handler(this::updateSwaggerJson);
    router.route().handler(StaticHandler.create());

    vertx.createHttpServer().requestHandler(router).listen(3000, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 3000");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    FileSystem fileSystem = vertx.fileSystem();
    fileSystem.delete(FileName).onComplete(ar -> {
      if (ar.succeeded()) {
        stopPromise.complete();
      } else {
        stopPromise.fail(ar.cause());
      }
    });
  }

  /**
   * 获取当前的json
   * @param ctx ctx
   */
  private void getSwaggerJson(RoutingContext ctx) {
    Vertx vertx = ctx.vertx();
    FileSystem fileSystem = vertx.fileSystem();
    fileSystem.readFile(FileName).onComplete(ar -> {
      HttpServerResponse response = ctx.response();
      if (ar.succeeded()) {
        response.putHeader("Content-Type", "application/json");
        response.end(ar.result());
      } else {
        response.setStatusCode(500);
        response.end(printError(ar.cause()));
      }
    });
  }

  private Future<Void> writeSwaggerJson(Vertx vertx, Buffer buffer) {
    FileSystem fileSystem = vertx.fileSystem();
    return fileSystem.writeFile("swagger.json", buffer);
  }

  /**
   * 更新SwaggerJson
   * @param ctx ctx
   */
  private void updateSwaggerJson(RoutingContext ctx) {
    Set<FileUpload> uploads = ctx.fileUploads();
    FileUpload fileUpload = uploads.iterator().next();
    FileSystem fileSystem = vertx.fileSystem();
    fileSystem
      .readFile(fileUpload.uploadedFileName())
      .compose(buffer -> fileSystem.writeFile(FileName, buffer)).onComplete(ar -> {
      HttpServerResponse response = ctx.response();
        if (ar.succeeded()) {
          response.end();
        } else {
          response.setStatusCode(500);
          response.end(printError(ar.cause()));
        }
      });
  }

  private String printError(Throwable throwable) {
    OutputStream outputStream = new ByteArrayOutputStream();
    PrintWriter pw = new PrintWriter(outputStream);
    throwable.printStackTrace(pw);
    return pw.toString();
  }
}

