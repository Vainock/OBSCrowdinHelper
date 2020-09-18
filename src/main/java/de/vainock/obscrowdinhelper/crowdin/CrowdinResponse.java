package de.vainock.obscrowdinhelper.crowdin;

import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONObject;

public class CrowdinResponse {

  private static final List<CrowdinResponse> responses = new ArrayList<>();
  private int code;
  private JSONObject body;

  public CrowdinResponse() {

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

  public int getCode() {
    return code;
  }

  CrowdinResponse setCode(int code) {
    this.code = code;
    return this;
  }

  public JSONObject getBody() {
    return body;
  }

  CrowdinResponse setBody(JSONObject body) {
    this.body = body;
    return this;
  }
}
