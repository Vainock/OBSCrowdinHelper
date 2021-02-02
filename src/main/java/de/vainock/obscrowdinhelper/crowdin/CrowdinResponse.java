package de.vainock.obscrowdinhelper.crowdin;

import java.util.ArrayList;
import java.util.List;
import okhttp3.ResponseBody;
import org.json.simple.JSONObject;

public class CrowdinResponse {

  private static final List<CrowdinResponse> responses = new ArrayList<>();
  public int code;
  public JSONObject body;
  public ResponseBody rawBody;

  CrowdinResponse() {

  }

  static void addResponse(CrowdinResponse response) {
    responses.add(response);
  }

  public static List<CrowdinResponse> getResponses(boolean clear) {
    if (clear) {
      List<CrowdinResponse> res = new ArrayList<>(responses);
      responses.clear();
      return res;
    } else {
      return responses;
    }
  }

  CrowdinResponse setCode(int code) {
    this.code = code;
    return this;
  }

  CrowdinResponse setBody(JSONObject body) {
    this.body = body;
    return this;
  }

  CrowdinResponse setRawBody(ResponseBody rawBody) {
    this.rawBody = rawBody;
    return this;
  }
}
