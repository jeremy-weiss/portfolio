// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.api.client.util.store.DataStore;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.GenericUrl;

import java.util.Scanner;
import java.util.HashMap;
import java.io.InputStream;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

  private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
  private MemoryDataStoreFactory DATA_STORE_FACTORY = MemoryDataStoreFactory.getDefaultInstance();
  private DataStore<String> emailDataStore;
  private HttpTransport httpTransport;
  private JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private List<String> SCOPES = Arrays.asList("https://www.googleapis.com/auth/userinfo.email");
  private String emailUrl = "https://openidconnect.googleapis.com/v1/userinfo";
  private GoogleClientSecrets clientSecrets;
  private String APPLICATION_NAME = "Portfolio/1.0";
  private GoogleAuthorizationCodeFlow flow;
  private String redirectUri;
  private Gson gson = new Gson();

  public LoginServlet() {
    try {
          httpTransport = GoogleNetHttpTransport.newTrustedTransport();
          clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                           new InputStreamReader(LoginServlet.class.getResourceAsStream("/client.json")));
          flow = new GoogleAuthorizationCodeFlow.Builder(
                           httpTransport, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(
                           DATA_STORE_FACTORY).build();
          emailDataStore = DATA_STORE_FACTORY.getDataStore("Login");
        } catch (Exception e) {
          System.out.println(e);
          return;
        }
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String code = request.getParameter("code"); // Todo unique 'value' parameter as protection
    if (code != null) {
      // Successful login
      TokenResponse token = flow.newTokenRequest(code).setRedirectUri("http://localhost:8080/login").execute();
      Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod()).setFromTokenResponse(token);

      GenericUrl authUrl = new GenericUrl(emailUrl);
      HttpRequestFactory requestFactory = httpTransport.createRequestFactory(credential);
      HttpResponse authResponse = requestFactory.buildGetRequest(authUrl).execute();
      HashMap<String, String> authContent = gson.fromJson(streamToString(authResponse.getContent()), HashMap.class);
      String email = authContent.get("email");

      String sessionId = request.getSession().getId();
      emailDataStore.set(sessionId, email);
      response.sendRedirect("/");

    } else {
      // Redirect to Google oauth
      String redirectUri = flow.newAuthorizationUrl().setRedirectUri("http://localhost:8080/login").build();
      response.sendRedirect(redirectUri);
    }
  }

  private String streamToString(InputStream stream) {
    Scanner s = new Scanner(stream).useDelimiter("\\A");
    String result = s.hasNext() ? s.next() : "";
    return result;
  }
}
