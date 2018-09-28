package com.hpe.autoframework;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.path.json.JsonPath;
import io.restassured.path.xml.XmlPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.http.client.params.ClientPNames;
import org.apache.http.params.CoreConnectionPNames;
import org.testng.TestException;

@SuppressWarnings("deprecation")
public class RestClient {
	
	private static final String RESPONSE_DUMP_FILENAME = "rest_resp_%05d.log";
	
	protected int respDumpIndex_ = 1;
	
	private Response resp_;
	
	private RequestSpecification request_;
	
	private int timeoutms_ = 2000;
	
	public RestClient() {
		RestAssured.given().relaxedHTTPSValidation(); // for HTTPS
	}
	
	public void setProxy(String host, int port) {
		RestAssured.proxy(host, port);
	}
	
	public void setBaseUrlPort(String baseUrl, int port) {
		RestAssured.baseURI = baseUrl;
		RestAssured.port = port;
	}
	
	public void get(String url) {
		get(url, 200);
	}

	public void get(String url, int expectStatusCode) {
		if (request_ == null) {
			resp_ = getRequestSpecification().get(url);
		} else {
			resp_ = request_.get(url);
		}
		request_ = null;
		dumpResponse();
	}
	
	public void get(String url, String bodyJson) {
		get(url, bodyJson, 200);
	}
	
	public void get(String url, String bodyJson, int expectStatusCode) {
		if (request_ == null) {
			resp_ = getRequestSpecification().body(bodyJson).get(url);
		} else {
			resp_ = request_.body(bodyJson).post(url);
		}
		request_ = null;
		dumpResponse();
	}
	
	public void getWithFile(String url, String bodyJsonFile) {
		getWithFile(url, bodyJsonFile, 200);
	}
	
	public void getWithFile(String url, String bodyJsonFile, int expectStatusCode) {
		if (request_ == null) {
			resp_ = getRequestSpecification().body(new File(bodyJsonFile)).get(url);
		} else {
			resp_ = request_.body(new File(bodyJsonFile)).post(url);
		}
		request_ = null;
		dumpResponse();
	}
	
	public void head(String url) {
		head(url, 200);
	}

	public void head(String url, int expectStatusCode) {
		if (request_ == null) {
			resp_ = getRequestSpecification().head(url);
		} else {
			resp_ = request_.head(url);
		}
		request_ = null;
		dumpResponse();
	}
	
	public void options(String url) {
		options(url, 200);
	}

	public void options(String url, int expectStatusCode) {
		if (request_ == null) {
			resp_ = getRequestSpecification().options(url);
		} else {
			resp_ = request_.options(url);
		}
		request_ = null;
		dumpResponse();
	}
	
	public String getBody() {
		assert resp_ != null;
		return resp_.getBody().asString();
	}
	
	public JsonPath getJsonPath() {
		assert resp_ != null;
		return resp_.getBody().jsonPath();
	}
	
	public XmlPath getXmlPath() {
		assert resp_ != null;
		return resp_.getBody().xmlPath();
	}
	
	public void assertJsonPathFile(JsonPath jsonpath, String expectJsonFile) {
		JsonPath expectJsonPath = new JsonPath(new File(expectJsonFile));
		assert jsonpath.get("").equals(expectJsonPath.get(""));
	}
	
	public void assertJsonPath(JsonPath jsonpath, String expectJson) {
		JsonPath expectJsonPath = new JsonPath(expectJson);
		assert jsonpath.get("").equals(expectJsonPath.get(""));
	}
	
	private void dumpResponse() {
		assert resp_ != null;
		
		BufferedWriter bw = null;
		FileWriter fw = null;
		String filename = null;
		try {
			filename = ExProgressFormatter.getEvidenceDirName().resolve(String.format(RESPONSE_DUMP_FILENAME, respDumpIndex_ ++)).toString();
			fw = new FileWriter(new File(filename));
			bw = new BufferedWriter(fw);
			bw.write(resp_.getStatusLine() + "\n");
			bw.write(resp_.getHeaders().toString() + "\n");
			bw.write("\n");
			bw.write(resp_.getBody().asString());
		} catch (IOException e) {
			throw new TestException("Output RESTful response dump file failed:" + filename, e);
		} finally {
			if (bw != null){
				try {
					bw.close();
				} catch (IOException e) {
					// ignore
				}
			}
			if (fw != null){
				try {
					fw.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
	
	public void delete(String url) {
		delete(url, 200);
	}

	public void assertStatusCode(int expectStatusCode) {
		assert resp_.statusCode() == expectStatusCode;
	}

	public void assertStatusCode(int expectMinStatusCode, int expectMaxStatusCode) {
		assert resp_.statusCode() >= expectMinStatusCode && resp_.statusCode() <= expectMaxStatusCode;
	}

	public void delete(String url, int expectStatusCode) {
		if (request_ == null) {
			resp_ = getRequestSpecification().delete(url);
		} else {
			resp_ = request_.delete(url);
		}
		request_ = null;
		dumpResponse();
	}
	
	public void putWithFile(String url, String bodyJsonFile) {
		putWithFile(url, bodyJsonFile, 0);
	}
	
	public void putWithFile(String url, String bodyJsonFile, int expectStatusCode) {
		if (request_ == null) {
			resp_ = getRequestSpecification().body(new File(bodyJsonFile)).put(url);
		} else {
			resp_ = request_.body(new File(bodyJsonFile)).put(url);
		}
		request_ = null;
		dumpResponse();
		if (expectStatusCode > 0) {
			assert resp_.statusCode() == expectStatusCode;
		}
	}
	
	public void put(String url, String bodyJson) {
		put(url, bodyJson, 0);
	}
	
	public void put(String url, String bodyJson, int expectStatusCode) {
		if (request_ == null) {
			resp_ = getRequestSpecification().body(bodyJson).put(url);
		} else {
			resp_ = request_.body(bodyJson).put(url);
		}
		request_ = null;
		dumpResponse();
		if (expectStatusCode > 0) {
			assert resp_.statusCode() == expectStatusCode;
		}
	}
	
	public void postWithFile(String url, String bodyJsonFile) {
		postWithFile(url, bodyJsonFile, 0);
	}

	public void postWithString(String url, String bodyJson, int expectStatusCode) {
		if (request_ == null) {
			resp_ = getRequestSpecification().body(bodyJson).post(url);
		} else {
			resp_ = request_.body(bodyJson).post(url);
		}
		request_ = null;
		dumpResponse();
		if (expectStatusCode > 0) {
			assert resp_.statusCode() == expectStatusCode;
		}
	}
	
	public void postWithFile(String url, String bodyJsonFile, int expectStatusCode) {
		if (request_ == null) {
			resp_ = getRequestSpecification().body(new File(bodyJsonFile)).post(url);
		} else {
			resp_ = request_.body(new File(bodyJsonFile)).post(url);
		}
		request_ = null;
		dumpResponse();
		if (expectStatusCode > 0) {
			assert resp_.statusCode() == expectStatusCode;
		}
	}
	
	public void post(String url, String bodyJson) {
		post(url, bodyJson, 0);
	}
	
	public void post(String url, String bodyJson, int expectStatusCode) {
		if (request_ == null) {
			resp_ = getRequestSpecification().body(bodyJson).post(url);
		} else {
			resp_ = request_.body(bodyJson).post(url);
		}
		request_ = null;
		dumpResponse();
		if (expectStatusCode > 0) {
			assert resp_.statusCode() == expectStatusCode;
		}
	}
	
	public void patchWithFile(String url, String bodyJsonFile) {
		patchWithFile(url, bodyJsonFile, 0);
	}
	
	public void patchWithFile(String url, String bodyJsonFile, int expectStatusCode) {
		if (request_ == null) {
			resp_ = getRequestSpecification().body(new File(bodyJsonFile)).patch(url);
		} else {
			resp_ = request_.body(new File(bodyJsonFile)).patch(url);
		}
		request_ = null;
		dumpResponse();
		if (expectStatusCode > 0) {
			assert resp_.statusCode() == expectStatusCode;
		}
	}
	
	public void patch(String url, String bodyJson) {
		patch(url, bodyJson, 0);
	}
	
	public void patch(String url, String bodyJson, int expectStatusCode) {
		if (request_ == null) {
			resp_ = getRequestSpecification().body(bodyJson).patch(url);
		} else {
			resp_ = request_.body(bodyJson).patch(url);
		}
		request_ = null;
		dumpResponse();
		if (expectStatusCode > 0) {
			assert resp_.statusCode() == expectStatusCode;
		}
	}
	
	public String getHeader(String header) {
		assert resp_ != null;
		return resp_.getHeader(header);
	}
	
	public void addHeader(String name, String value) {
		if (request_ == null) {
			request_ = getRequestSpecification().header(name, value);
		} else {
			request_ = request_.header(name, value);
		}
	}
	
	public void setTimeout(int milisecond) {
		timeoutms_ = milisecond;
	}
	
	private RequestSpecification getRequestSpecification() {
		RestAssuredConfig restAssuredConfig = RestAssured.config().httpClient(HttpClientConfig.httpClientConfig().
				setParam(ClientPNames.CONN_MANAGER_TIMEOUT, (long)timeoutms_).
				setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, timeoutms_).
				setParam(CoreConnectionPNames.SO_TIMEOUT, timeoutms_).
				setParam(CoreConnectionPNames.STALE_CONNECTION_CHECK, true));
		request_ = RestAssured.given().config(restAssuredConfig);
		return request_;
	}
}
