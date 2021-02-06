package de.vainock.obscrowdinhelper.crowdin;

import org.json.simple.JSONObject;

public class CrowdinResponse {

  public int code;
  public JSONObject body;

  CrowdinResponse() {

  }

  CrowdinResponse setCode(int code) {
    this.code = code;
    return this;
  }

  CrowdinResponse setBody(JSONObject body) {
    this.body = body;
    return this;
  }
}
