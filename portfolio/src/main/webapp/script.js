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

/**
 * Adds a random greeting to the page.
 */
function addRandomGreeting() {
  const greetings =
      ['Hello world!', '¡Hola Mundo!', '你好，世界！', 'Bonjour le monde!'];

  // Pick a random greeting.
  const greeting = greetings[Math.floor(Math.random() * greetings.length)];

  // Add it to the page.
  const greetingContainer = document.getElementById('greeting-container');
  greetingContainer.innerText = greeting;
}

function addNav() {
  const navbar="navbar.html";
  let xhttp = new XMLHttpRequest();
  xhttp.open("GET", navbar, true);
  xhttp.send();
  
  xhttp.onreadystatechange = function() {
    if (this.readyState == 4 && this.status == 200) {
      document.getElementsByTagName("navbar")[0].innerHTML = this.responseText;
    }
  };
}

// Todo: update with math function to change speed based on position
function scrollToId(id) {
  var ele = document.getElementById(id);
  const desiredDist = 10;

  function scrollTo(x, y, lastTop, direction) {
      var topY = ele.getBoundingClientRect().top;
            console.log(y, topY);
      if (topY == lastTop) {
        return;
      } else if (topY > desiredDist - 5 && topY < desiredDist) {
        window.scroll(x, y + desiredDist - topY);
        return;
      }

      window.scroll(x, y + direction);
      setTimeout(function() {scrollTo(x, y + direction, topY, direction)}, 1);
  }

  var direction = 5;
  if (ele.getBoundingClientRect().top < desiredDist) {
    direction = -5;
  }
  scrollTo(window.scrollX, window.scrollY, 0, direction);
}

function highlightProjects() {
  var projects = document.getElementsByClassName("project");
    var node = document.createElement("span");
    node.classList.add("tint");
    for (var i = 0; i < projects.length; i++) {
      var project = projects[i];
      project.insertBefore(node);
      project.appendChild(node);
  }
}
