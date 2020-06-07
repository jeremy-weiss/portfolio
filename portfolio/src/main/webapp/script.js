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

function addContentToId(id, url, updateUrl) {
  if (!this.requested) {
    this.requested = new Set();
  }

  if (this.requested.has(updateUrl)) {
    document.getElementById(updateUrl).style.display = 'block';
    return;
  }

  const okResponse = 200;
  var xhttp = new XMLHttpRequest();
  xhttp.onreadystatechange = function() {
    if (this.readyState === XMLHttpRequest.DONE) {
      var node = document.createElement('div');
      node.classList.add('adv-desc')
      node.setAttribute('id', updateUrl);
      if (this.status === okResponse) {
        node.innerHTML = this.responseText;
      } else {
        node.innerHTML = 'Project could not be found.'
      }

      document.getElementById(id).appendChild(node);
      window.history.pushState('', '', updateUrl);
    }
  };

  xhttp.open('GET', url, true);
  xhttp.send();

  this.requested.add(updateUrl);
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
    } else if (
        topY > desiredDist - scrollMagnitude + offset &&
        topY < desiredDist + scrollMagnitude + offset) {
      window.scroll(x, y + desiredDist - topY - offset);
      return;
    }
    window.scroll(x, y + direction);
    setTimeout(function() {
      scrollTo(x, y + direction, topY, direction)
    }, 5);
  }

  var direction = scrollMagnitude;
  if (ele.getBoundingClientRect().top < desiredDist) {
    direction = -scrollMagnitude;
  }
  scrollTo(window.scrollX, window.scrollY, 0, direction);
}

// Highlight on mouseover
function highlightProjects() {
  var projects = document.getElementsByClassName('project');
  for (var i = 0; i < projects.length; i++) {
    var node = document.createElement('span');
    node.classList.add('tint');
    var project = projects[i].children[0].children[0];
    project.appendChild(node);
  }
}

// Have links update the url bar and page content without redirecting
function noRedir() {
  var links = document.getElementsByTagName('a');
  for (var i = 0; i < links.length; i++) {
    links[i].onclick = function(e) {
      var node = e.target;
      while (!('href' in node)) {
        node = node.parentNode;
      }

      var commentsContainer = document.getElementById('comments-container');
      var commentsNode = document.getElementById('comments');
      var contentNode = document.getElementById('content');
      for (var i = 0; i < contentNode.children.length; i++) {
        var child = contentNode.children[i];
        if (child.classList.contains('adv-desc')) {
          child.style.display = 'none';
        }
      }

      var homeNode = document.getElementById('home');
      // Reload contents into page
      if (node.href.includes('#')) {
        window.history.pushState('', '', '#');
        homeNode.style.display = 'block';
        commentsContainer.style.display = 'block';
        commentsNode.style.display = 'block';
      } else if (
          node.href.includes('javascript:') || node.href.includes('mailto:')) {
        homeNode.style.display = 'block';
        commentsContainer.style.display = 'block';
        commentsNode.style.display = 'block';
        return true;  // Want the javascript the activate
      } else {
        homeNode.style.display = 'none';
        commentsNode.style.display = 'none';
        commentsContainer.style.display = 'none';
        addContentToId('content', node.href + '.html', node.href);
      }
      return false;
    }
  }
}

function fetchAndReplace(url, id, replaceFunc) {
  fetch(url).then(response => response.text()).then(text => {
    replaceFunc(document.getElementById(id), text);
  });
}

function parseComments(node, text) {
  node.innerHTML = '';
  var commentEntity = JSON.parse(text);
  var comments = commentEntity.comments;
  for (var i = 0; i < comments.length; i++) {
    var comment = comments[i];
    var stylized = stylizeComment(comment)
    node.appendChild(stylized);
  }
}

function stylizeComment(commentEntity) {
  var name = commentEntity.propertyMap.name;
  var comment = commentEntity.propertyMap.comment;
  var time = new Date(commentEntity.propertyMap.time);

  var div = document.createElement('div');
  div.classList.add('comment');
  div.innerHTML = `
                   <div class="container border">
                     <div class="row">
                       <div class="col text-left">${name}</div>
                       <div class="col text-right">${time}</div>
                     </div>
                     <div class="row">${comment}</div>
                   </div>
                  `
  return div;
}

function enterNoSubmit(cls) {
  var nodes = document.getElementsByClassName(cls);

  // Function must only be called once
  this.numToForm = nodes;
  this.formToNum = {};

  const RET = 13
  for (var i = 0; i < nodes.length; i++) {
    var node = nodes[i];
    formToNum[node.name] = i;
    node.addEventListener('keydown', e => {
      // Return key
      if (e.keyCode === RET) {
        e.preventDefault();
        var num = formToNum[e.target.name];
        if (num + 1 < Object.keys(this.formToNum).length) {
          this.numToForm[num + 1].focus();
        }
      }
    });
  }
}

function deleteComments() {
  fetch('delete-comment', {
    method: 'POST'
  }).then(fetchAndReplace('/comment?num=10', 'comments', parseComments))
}

function formatComments() {
  this.page = 1;
  this.limit = 5;
  // Changes number of comments based on the select box
  document.getElementById('select-limit').addEventListener('change', e => {
    this.limit = parseInt(e.target.value);
    fetchAndReplace(
        `/comment?num=${this.limit}&page=${this.page}`, 'comments',
        parseComments);
    // Calculates number of pages needed
    fetchAndReplace('comment?num=0', 'pagination', paginate);
  });

  // Initial call
  fetchAndReplace('comment?num=0', 'pagination', paginate);
}

function paginate(pageNode, comments) {
  var commentEntity = JSON.parse(comments);
  var numComments = commentEntity.num;
  var numPages = Math.floor((numComments + this.limit - 1) / this.limit);

  var pageHTML =
      '<li class="page-item"><a class="page-link" id="prev" href="#">Previous</a></li>';
  for (var i = 1; i <= numPages; i++) {
    pageHTML += `<li class="page-item"><a class="page-link" id="page-${
        i}" href="#">${i}</a></li>`;
  }
  pageHTML +=
      '<li class="page-item"><a class="page-link" id="next" href="#">Next</a></li>';
  pageNode.innerHTML = pageHTML;

  var pages = pageNode.children;
  var prev = pages[0];
  var next = pages[pages.length - 1];

  function pageOnclick(e, newValue) {
    e.preventDefault();
    if (e.target.classList.contains('disabled')) {
      return;
    }

    pages[this.page].classList.remove('active');
    this.page = newValue;
    fetchAndReplace(
        `comment?num=${this.limit}&page=${this.page}`, 'comments',
        parseComments);
    stylePages(pageNode, numPages);
  }

  for (i = 1; i < pages.length - 1; i++) {
    var page = pages[i];
    // Adds closure for page
    (function(tmpPage) {
      page.onclick = e => {
        pageOnclick(e, parseInt(tmpPage.innerText));
      }
    }(page));
  }
  prev.onclick = e => {
    pageOnclick(e, this.page - 1)
  };
  next.onclick = e => {
    pageOnclick(e, this.page + 1)
  };

  stylePages(pageNode, numPages);
}

function stylePages(pageNode, numPages) {
  var pages = pageNode.children;
  var prev = pages[0];
  var next = pages[pages.length - 1];
  prev.classList.remove('disabled');
  next.classList.remove('disabled');

  if (this.page === 1) {
    prev.classList.add('disabled');
    prev.innerHTML = '<span class="page-link">Previous</span>';
  }
  if (this.page === numPages) {
    next.classList.add('disabled');
    next.innerHTML = '<span class="page-link">Next</span>';
  }

  var activePage = pages[this.page];
  activePage.classList.add('active');
}

function init() {
  highlightProjects();
  noRedir();
  enterNoSubmit('noSubmit');
  fetchAndReplace('/comment?num=5', 'comments', parseComments);
  formatComments();
}