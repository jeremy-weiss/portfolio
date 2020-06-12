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

import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Servlet that adds comments to the Datastore db
@WebServlet("/comment")
public class DataServlet extends HttpServlet {
  private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
  private MemoryDataStoreFactory memDataStore = MemoryDataStoreFactory.getDefaultInstance();
  private DataStore<String> emailDataStore;
  private Gson gson = new Gson();

  public DataServlet() {
    try {
      emailDataStore = memDataStore.getDataStore("Login");
    } catch (Exception e) {
      System.out.println(e);
      return;
    }
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    CommentList commentList;

    String sessionId = request.getSession().getId();
    String email = emailDataStore.get(sessionId);
    System.out.println(email);
    if (email == null || !email.equals(System.getenv("email"))) {
      commentList = new CommentList();
    } else {
      System.out.println("Sending comment");
      Query query = new Query("Comment").addSort("time", SortDirection.DESCENDING);
      PreparedQuery results = datastore.prepare(query);

      // Limit comments per "comment page"
      int limit = Integer.parseInt(request.getParameter("num"));
      int page;
      try {
        page = Integer.parseInt(request.getParameter("page"));
      } catch (Exception e) {
        page = 1;
      }

      commentList = new CommentList(results, limit, page);
    }

    response.setContentType("application/json;");
    String json = gson.toJson(commentList);
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String nameText = request.getParameter("name");
    String commentText = request.getParameter("newComment");

    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("name", nameText);
    commentEntity.setProperty("comment", commentText);
    commentEntity.setProperty("time", System.currentTimeMillis());

    datastore.put(commentEntity);

    response.setContentType("text/html");
    response.getWriter().println(commentEntity);
    response.sendRedirect("/#comment");
  }

  class CommentList {
    public int num;
    public List<Entity> comments;

    public CommentList() {
      this.num = 0;
      comments = null;
    }

    public CommentList(PreparedQuery results, int limit, int page) {
      this.num = results.asList(FetchOptions.Builder.withDefaults()).size();
      List<Entity> allComments = results.asList(FetchOptions.Builder.withLimit(page * limit));
      this.comments =
          allComments.subList((page - 1) * limit, Math.min(page * limit, allComments.size()));
    }
  }
}
