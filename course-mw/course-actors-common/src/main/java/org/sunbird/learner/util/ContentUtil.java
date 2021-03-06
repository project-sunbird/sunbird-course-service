/** */
package org.sunbird.learner.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;

import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.HttpUtilResponse;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * This class will make the call to EkStep content search
 *
 * @author Manzarul
 */
public final class ContentUtil {

  private static ObjectMapper mapper = new ObjectMapper();
  private static String EKSTEP_COURSE_SEARCH_QUERY =
          "{\"request\": {\"filters\":{\"contentType\": [\"Course\"], \"identifier\": \"COURSE_ID_PLACEHOLDER\", \"status\": \"Live\", \"mimeType\": \"application/vnd.ekstep.content-collection\", \"trackable.enabled\": \"Yes\"},\"limit\": 1}}";


  private ContentUtil() {}

  /**
   * @param params String
   * @param headers Map<String, String>
   * @return Map<String,Object>
   */
  public static Map<String, Object> searchContent(String params, Map<String, String> headers) {
    Map<String, Object> resMap = new HashMap<>();
    try {
      String baseSearchUrl = ProjectUtil.getConfigValue(JsonKey.SEARCH_SERVICE_API_BASE_URL);
      headers.put(
          JsonKey.AUTHORIZATION, JsonKey.BEARER + System.getenv(JsonKey.EKSTEP_AUTHORIZATION));
      headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
      headers.remove(HttpHeaders.ACCEPT_ENCODING.toLowerCase());
      headers.put(HttpHeaders.ACCEPT_ENCODING.toLowerCase(), "UTF-8");
      if (StringUtils.isBlank(headers.get(JsonKey.AUTHORIZATION))) {
        headers.put(
            JsonKey.AUTHORIZATION,
            PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_AUTHORIZATION));
      }
      ProjectLogger.log("making call for content search ==" + params, LoggerEnum.INFO.name());
      String response =
          HttpUtil.sendPostRequest(
              baseSearchUrl
                  + PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_CONTENT_SEARCH_URL),
              params,
              headers);
      ProjectLogger.log("Content serach response is ==" + response, LoggerEnum.INFO.name());
      Map<String, Object> data = mapper.readValue(response, Map.class);
      if (MapUtils.isNotEmpty(data)) {
        String resmsgId = (String) ((Map<String, Object>) data.get("params")).get("resmsgid");
        String apiId = (String) data.get("id");
        data = (Map<String, Object>) data.get(JsonKey.RESULT);
        ProjectLogger.log(
            "Total number of content fetched from Ekstep while assembling page data : "
                + data.get("count"),
            LoggerEnum.INFO.name());
        if (MapUtils.isNotEmpty(data)) {
          Object contentList = data.get(JsonKey.CONTENT);
          Map<String, Object> param = new HashMap<>();
          param.put(JsonKey.RES_MSG_ID, resmsgId);
          param.put(JsonKey.API_ID, apiId);
          resMap.put(JsonKey.PARAMS, param);
          resMap.put(JsonKey.CONTENTS, contentList);
          Iterator<Map.Entry<String, Object>> itr = data.entrySet().iterator();
          while (itr.hasNext()) {
            Map.Entry<String, Object> entry = itr.next();
            if (!JsonKey.CONTENT.equals(entry.getKey())) {
              resMap.put(entry.getKey(), entry.getValue());
            }
          }
        }
      } else {
        ProjectLogger.log("EkStepRequestUtil:searchContent No data found", LoggerEnum.INFO.name());
      }
    } catch (Exception e) {
      ProjectLogger.log("Error found during contnet search parse==" + e.getMessage(), e);
    }
    return resMap;
  }

  public static String contentCall(String baseURL, String apiURL, String authKey, String body)
      throws IOException {
    String url = baseURL + PropertiesCache.getInstance().getProperty(apiURL);
    ProjectLogger.log(
        "BaseMetricsActor:makePostRequest completed requested url :" + url + " data : " + body,
        LoggerEnum.INFO.name());
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json; charset=utf-8");
    headers.put(JsonKey.AUTHORIZATION, authKey);
    HttpUtilResponse response = HttpUtil.doPostRequest(url, body, headers);
    if (response == null || response.getStatusCode() != 200) {
      ProjectLogger.log(
          "BaseMetricsActor:makePostRequest: Status code from analytics is not 200 ",
          LoggerEnum.INFO.name());
      throw new ProjectCommonException(
          ResponseCode.unableToConnect.getErrorCode(),
          ResponseCode.unableToConnect.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }

    String result = response.getBody();
    ProjectLogger.log(
        "BaseMetricsActor:makePostRequest: Response from analytics store for metrics = " + result,
        LoggerEnum.INFO.name());
    return result;
  }
  public static Map<String, Object> getContent(String courseId) {
    Map<String, Object> resMap = new HashMap<>();
    Map<String, String> headers = new HashMap<>();
    try {
      String baseContentreadUrl = ProjectUtil.getConfigValue(JsonKey.EKSTEP_BASE_URL) + "/content/v3/read/" + courseId + "?fields=status";
      headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
      headers.put(JsonKey.AUTHORIZATION, PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_AUTHORIZATION));

      ProjectLogger.log("making call for content read ==" + courseId, LoggerEnum.INFO.name());
      String response = HttpUtil.sendGetRequest(baseContentreadUrl, headers);

      ProjectLogger.log("Content read response is == " + response, LoggerEnum.INFO.name());
      Map<String, Object> data = mapper.readValue(response, Map.class);
      if (MapUtils.isNotEmpty(data)) {
        data = (Map<String, Object>) data.get(JsonKey.RESULT);
        if (MapUtils.isNotEmpty(data)) {
          Object content = data.get(JsonKey.CONTENT);
          resMap.put(JsonKey.CONTENT, content);
        }else {
          ProjectLogger.log("EkStepRequestUtil:searchContent No data found", LoggerEnum.INFO.name());
        }
      } else {
        ProjectLogger.log("EkStepRequestUtil:searchContent No data found", LoggerEnum.INFO.name());
      }
    } catch (IOException e) {
      ProjectLogger.log("Error found during content search parse==" + e.getMessage(), e);
    } catch (UnirestException e) {
      ProjectLogger.log("Error found during content search parse==" + e.getMessage(), e);
    }
    return resMap;
  }


  public static Map<String, Object> getCourseObjectFromEkStep(
          String courseId, Map<String, String> headers) {
    ProjectLogger.log("Requested course id is ==" + courseId, LoggerEnum.INFO.name());
    if (!StringUtils.isBlank(courseId)) {
      try {
        String query = EKSTEP_COURSE_SEARCH_QUERY.replaceAll("COURSE_ID_PLACEHOLDER", courseId);
        Map<String, Object> result = ContentUtil.searchContent(query, headers);
        if (null != result && !result.isEmpty() && result.get(JsonKey.CONTENTS) != null) {
          return ((List<Map<String, Object>>) result.get(JsonKey.CONTENTS)).get(0);
          // return (Map<String, Object>) contentObject;
        } else {
          ProjectLogger.log(
                  "CourseEnrollmentActor:getCourseObjectFromEkStep: Content not found for requested courseId "
                          + courseId,
                  LoggerEnum.INFO.name());
        }
      } catch (Exception e) {
        ProjectLogger.log(e.getMessage(), e);
      }
    }
    return null;
  }
}
