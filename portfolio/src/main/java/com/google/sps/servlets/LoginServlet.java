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

import com.google.api.client.extensions.appengine.datastore.AppEngineDataStoreFactory;

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

  private static AppEngineDataStoreFactory DATA_STORE_FACTORY = AppEngineDataStoreFactory.getDefaultInstance();
  private static HttpTransport httpTransport;
  private static JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static List<String> SCOPES = Arrays.asList("https://www.googleapis.com/auth/userinfo.email");
  private static GoogleClientSecrets clientSecrets;
  private static String APPLICATION_NAME = "Portfolio/1.0";
  private static GoogleAuthorizationCodeFlow flow;
  private static String redirectUri;
  private static Gson gson = new Gson();

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    try {
      httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                       new InputStreamReader(LoginServlet.class.getResourceAsStream("/client.json")));
      flow = new GoogleAuthorizationCodeFlow.Builder(
                       httpTransport, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(
                       DATA_STORE_FACTORY).build();
    } catch (Exception e) {
      System.out.println(e);
      return;
    }

    String code = request.getParameter("code"); // Todo unique 'value' parameter as protection
    if (code != null) {
      // Successful login
      TokenResponse token = flow.newTokenRequest(code).setRedirectUri("http://localhost:8080/login").execute();
      System.out.println(token);
      // Credential credential = flow.createAndStoreCredential(token, <unique id>);
      Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod()).setFromTokenResponse(token);
      System.out.println(credential);

      String emailUrl = "https://openidconnect.googleapis.com/v1/userinfo";
      GenericUrl authUrl = new GenericUrl(emailUrl);
      HttpRequestFactory requestFactory = httpTransport.createRequestFactory(credential);
      HttpResponse authResponse = requestFactory.buildGetRequest(authUrl).execute();

      HashMap<String, String> authContent = gson.fromJson(streamToString(authResponse.getContent()), HashMap.class);
      String email = authContent.get("email");

      if (email.equals(System.getenv("email"))) {
        System.out.println("Welcome, Jeremy!");
      } else {
        System.out.println("Who're you???");
      }

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
