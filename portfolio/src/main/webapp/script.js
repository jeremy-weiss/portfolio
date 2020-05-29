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

// Injects the navbar into the page
function addNav() {
  replaceIdWithQuery("nav", "navbar.html");
}

function replaceIdWithQuery(id, url) {
  var xhttp = new XMLHttpRequest();

  var success = false;
  xhttp.onreadystatechange = function() {
    if (this.readyState === 4 && this.status === 200) {
      document.getElementById(id).innerHTML = this.responseText;
      success = true;
    }
  };

  xhttp.open("GET", url, false);
  xhttp.send();

  return success;

}

// Todo: update with math function to change speed based on distance to target
// Scroll animation
function scrollToId(id) {
  var ele = document.getElementById(id);
  const desiredDist = 10;
  var scrollMagnitude = 5;
  var offset = 15;

  function scrollTo(x, y, lastTop, direction) {
    var topY = ele.getBoundingClientRect().top;
    // Target is at the bottom of the page
    if (topY === lastTop) {
      return;
    } else if (topY > desiredDist - scrollMagnitude + offset && topY < desiredDist + scrollMagnitude + offset) {
      window.scroll(x, y + desiredDist - topY - offset);
      return;
    }
    window.scroll(x, y + direction);
    setTimeout(function() {scrollTo(x, y + direction, topY, direction)}, 5);
  }

  var direction = scrollMagnitude;
  if (ele.getBoundingClientRect().top < desiredDist) {
    direction = -scrollMagnitude;
  }
  scrollTo(window.scrollX, window.scrollY, 0, direction);
}

// Highlight on mouseover
function highlightProjects() {
  var projects = document.getElementsByClassName("project");
    for (var i = 0; i < projects.length; i++) {
      var node = document.createElement("span");
      node.classList.add("tint");
      var project = projects[i].children[0].children[0];
      project.appendChild(node);
  }
}

// Have links update the url bar and page content without redirecting
// var homeNode = undefined;
function noRedir() {
  if (!homeNode) {
    homeNode = document.getElementById("content").cloneNode(true);
  }

  var links = document.getElementsByTagName("a");
  for (var i = 0; i < links.length; i++) {
    links[i].onclick = function(e) {
      var node = e.target;
      while (!("href" in node)) {
        node = node.parentNode;
      }

      console.log(node.href);
      // Reload contents into page
      if (node.href === "#") {
        window.history.replaceState("", "", "#");
        var contentNode = document.getElementById("content");
        contentNode.parentNode.replaceChild(homeNode, contentNode);
        init();
      } else if (!node.href.includes("mailto")) {
        var success = replaceIdWithQuery("content", node.href + '.html');
        if (success) {
          window.history.replaceState("", "", node.href);
        }
      }
      return false;
    }
  }
}

function init() {
  highlightProjects();
  // noRedir();
}
