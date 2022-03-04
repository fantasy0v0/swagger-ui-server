package com.fantasy0v0.swagger_ui_server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.io.*;
import java.util.Set;

public class MainVerticle extends AbstractVerticle {

  private final String FileName = "swagger.json";

  private Buffer defaultSwaggerJson;

  @Override
  public void start(Promise<Void> startPromise) throws IOException {
    InputStream resourceAsStream = MainVerticle.class.getClassLoader().getResourceAsStream(FileName);
    if (null == resourceAsStream) {
      startPromise.fail("Please check if " + FileName + " is in classpath");
      return;
    }
    byte[] bytes = resourceAsStream.readAllBytes();
    defaultSwaggerJson = Buffer.buffer(bytes);
    Router router = Router.router(vertx);
    // 静态文件
    router.route().handler(StaticHandler.create());
    // 获取当前的服务列表
    router.get("/services").handler(this::getServices);
    // 新增服务
    router.post("/services").handler(this::createService);
    // 删除服务
    router.delete("/services").handler(this::deleteService);
    // 获取对应服务的json文件
    router.getWithRegex("/(?<serviceName>.+)\\.json")
      .handler(this::getSwaggerJson);
    // 更新对应的JSON文件
    router.postWithRegex("/(?<serviceName>.+)\\.json")
      .handler(BodyHandler.create())
      .handler(this::updateSwaggerJson);
    vertx.createHttpServer()
      .requestHandler(router)
      .listen(3001)
      .onSuccess(http -> {
        System.out.println("HTTP server started on port " + http.actualPort());
        startPromise.complete();
      }).onFailure(startPromise::fail);
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
   * 获取服务列表
   *
   * @param ctx ctx
   */
  private void getServices(RoutingContext ctx) {
    FileSystem fileSystem = vertx.fileSystem();
    fileSystem.readDir(".", "[\\u2E80-\\u9FFF]+\\.json")
      .onSuccess(files -> {
        JsonArray array = new JsonArray();
        for (String file : files) {
          file = new File(file).getName();
          array.add(file.substring(0, file.length() - ".json".length()));
        }
        ctx.response().end(array.encode());
      }).onFailure(e -> ctx.response().setStatusCode(500).end(e.getMessage()));
  }

  /**
   * 创建服务
   *
   * @param ctx ctx
   */
  private void createService(RoutingContext ctx) {
    String name = ctx.request().getParam("name");
    if (null == name || 0 == name.length()) {
      ctx.response().setStatusCode(500).end();
      return;
    }
    FileSystem fileSystem = vertx.fileSystem();
    String fileName = name + ".json";
    fileSystem.createFile(fileName)
      .compose(Void -> fileSystem.writeFile(fileName, defaultSwaggerJson))
      .onSuccess(Void -> ctx.end())
      .onFailure(Void -> ctx.response().setStatusCode(500).end());
  }

  /**
   * 删除服务
   *
   * @param ctx ctx
   */
  private void deleteService(RoutingContext ctx) {
    String name = ctx.request().getParam("name");
    if (null == name || 0 == name.length()) {
      ctx.response().setStatusCode(500).end();
      return;
    }
    vertx.fileSystem().delete(name + ".json")
      .onSuccess(Void -> ctx.end())
      .onFailure(Void -> ctx.response().setStatusCode(500).end());
  }

  /**
   * 获取当前的json
   *
   * @param ctx ctx
   */
  private void getSwaggerJson(RoutingContext ctx) {
    String serviceName = ctx.pathParam("serviceName");
    HttpServerResponse response = ctx.response();
    FileSystem fileSystem = vertx.fileSystem();
    fileSystem.readFile(serviceName + ".json").onComplete(ar -> {

      if (ar.succeeded()) {
        response.putHeader("Content-Type", "application/json");
        response.end(ar.result());
      } else {
        response.setStatusCode(500);
        response.end(printError(ar.cause()));
      }
    });
  }

  /**
   * 更新SwaggerJson
   *
   * @param ctx ctx
   */
  private void updateSwaggerJson(RoutingContext ctx) {
    String serviceName = ctx.pathParam("serviceName");
    Set<FileUpload> uploads = ctx.fileUploads();
    FileUpload fileUpload = uploads.iterator().next();
    FileSystem fileSystem = vertx.fileSystem();

    fileSystem
      .readFile(fileUpload.uploadedFileName())
      // 将上传的文件写入
      .compose(buffer -> fileSystem.writeFile(serviceName + ".json", buffer))
      // 删除上传的文件
      .compose(Void -> fileSystem.delete(fileUpload.uploadedFileName()))
      .onComplete(ar -> {
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

