package de.vainock.obscrowdinhelper.crowdin;

import de.vainock.obscrowdinhelper.OBSCrowdinHelper;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class CrowdinRequest implements Runnable {

  private static final OkHttpClient client = new OkHttpClient();
  private static ExecutorService executor = null;
  private static String token;
  private String url;
  private JSONObject body;
  private CrowdinRequestMethod method;
  private boolean isMultiple, auth = true, noResponse = false;
  private CrowdinResponse response;

  public CrowdinRequest() {

  }

  public static void setToken(String token) {
    CrowdinRequest.token = token;
  }

  public static synchronized List<CrowdinResponse> send(List<CrowdinRequest> requests,
      boolean clear)
      throws Exception {
    if (executor == null || executor.isTerminated()) {
      executor = Executors.newFixedThreadPool(20);
    }
    for (CrowdinRequest request : requests) {
      request.setMultiple();
      executor.execute(request);
    }
    executor.shutdown();
    while (!executor.isTerminated()) {
      Thread.sleep(100);
    }
    return CrowdinResponse.getResponses(clear);
  }

  public CrowdinRequest setPath(String path) {
    url = "https://" + OBSCrowdinHelper.PROJECT_DOMAIN + "/api/v2/" + path;
    return this;
  }

  public CrowdinRequest setUrl(String url) {
    this.url = url;
    auth = false;
    return this;
  }

  public CrowdinRequest removeAuth() {
    auth = false;
    return this;
  }

  public CrowdinRequest noResponse() {
    noResponse = true;
    return this;
  }

  public CrowdinRequest setBody(JSONObject body) {
    this.body = body;
    return this;
  }

  public CrowdinRequest setRequestMethod(CrowdinRequestMethod method) {
    this.method = method;
    return this;
  }

  private boolean isMultiple() {
    return isMultiple;
  }

  private void setMultiple() {
    this.isMultiple = true;
  }

  void setResponse(CrowdinResponse response) {
    this.response = response;
  }

  public CrowdinResponse send() {
    new Thread(this).run();
    return response;
  }

  @Override
  public void run() {
    try {
      Request.Builder reqBuilder = new Request.Builder();
      if (auth) {
        reqBuilder.header("Authorization", "Bearer " + token);
      }
      reqBuilder.url(url);
      switch (method) {
        case POST:
          reqBuilder
              .post(RequestBody.create(body.toString(), MediaType.parse("application/json")));
          break;
        case GET:
          reqBuilder.get();
          break;
      }
      Response requestRes = client.newCall(reqBuilder.build()).execute();
      if (!noResponse) {
        CrowdinResponse crowdinResponse = new CrowdinResponse().setCode(requestRes.code()).setBody(
            (JSONObject) new JSONParser()
                .parse(Objects.requireNonNull(requestRes.body()).string()));
        if (isMultiple()) {
          CrowdinResponse.addResponse(crowdinResponse);
        } else {
          setResponse(crowdinResponse);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
