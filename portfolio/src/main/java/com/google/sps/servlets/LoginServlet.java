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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Tokeninfo;
import com.google.api.services.oauth2.model.Userinfoplus;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

   /**
   * Global instance of the {@link DataStoreFactory}. The best practice is to make it a single
   * globally shared instance across your application.
   */
  //  private static FileDataStoreFactory dataStoreFactory;
  // private DataStoreFactory datastoreFactory = DatastoreServiceFactory.getDatastoreService();
  private static final AppEngineDataStoreFactory DATA_STORE_FACTORY =
        AppEngineDataStoreFactory.getDefaultInstance();


  /** Global instance of the HTTP transport. */
  private static HttpTransport httpTransport;

  /** Global instance of the JSON factory. */
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

  /** OAuth 2.0 scopes. */
    private static final List<String> SCOPES = Arrays.asList(
        "https://www.googleapis.com/auth/userinfo.email");

  private static Oauth2 oauth2;
  private static GoogleClientSecrets clientSecrets;

  private static final String APPLICATION_NAME = "Portfolio/1.0";

  /** Directory to store user credentials. */
  private static final java.io.File DATA_STORE_DIR =
      new java.io.File("store/oauth2_sample");

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    try {
      httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      // dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
      // authorization
      Credential credential = authorize();
      // set up global Oauth2 instance
      oauth2 = new Oauth2.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(
          APPLICATION_NAME).build();
      // run commands
      System.out.println("Validating a token");
      Tokeninfo tokeninfo = oauth2.tokeninfo().setAccessToken(credential.getAccessToken()).execute();
      System.out.println("BEFORE");
      System.out.println(tokeninfo.toPrettyString());
      if (!tokeninfo.getAudience().equals(clientSecrets.getDetails().getClientId())) {
        System.err.println("ERROR: audience does not match our client ID!");
      }
      System.out.println("AFTER");

      System.out.println("Obtaining User Profile Information");
      Userinfoplus userinfo = oauth2.userinfo().get().execute();
      System.out.println(userinfo.toPrettyString());
      // success!
      System.out.println("END");
      return;
    } catch (IOException e) {
    System.out.println("ERROR1");
      System.err.println(e.getMessage());
    } catch (Throwable t) {
    System.out.println("ERROR2");
      t.printStackTrace();
    }
  }

  private static Credential authorize() throws Exception {
      // load client secrets
      clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
          new InputStreamReader(LoginServlet.class.getResourceAsStream("/client.json")));
      // set up authorization code flow
      GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
          httpTransport, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(
          DATA_STORE_FACTORY).build();
      // authorize
      return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
  }
}
