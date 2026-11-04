/* Pantry Recipes service worker: precache the app so it works offline. MIT License. */
'use strict';
importScripts('js/version.js');                 // sets self.PR_VERSION, the one version source
var CORE = 'pr-core-' + self.PR_VERSION, IMAGES = 'pr-images';
var MAX_IMAGES = 300;
var PRECACHE = [
  './', 'index.html', 'manifest.json',
  'css/app.css',
  'js/version.js', 'js/core-data.js', 'js/icons.js', 'js/core.js', 'js/core15.js', 'js/core16.js', 'js/data16.js', 'js/core17.js', 'js/data17.js', 'js/import15.js', 'js/data20.js', 'js/helpers20.js', 'js/core20.js', 'js/stage3-data.js', 'js/core30.js', 'js/data201.js', 'js/core201.js', 'js/data21.js', 'js/core21.js', 'js/app.js',
  'data/recipes-0.json', 'data/recipes-1.json', 'data/recipes-2.json', 'data/recipes-3.json', 'data/recipes-4.json', 'data/substitutions.txt',
  'icons/apple-touch-icon.png', 'icons/icon-192.png', 'icons/icon-512.png', 'icons/icon-maskable-512.png', 'icons/favicon-32.png'
];
self.addEventListener('install', function (e) {
  e.waitUntil(caches.open(CORE).then(function (c) {
    return c.addAll(PRECACHE.map(function (u) { return new Request(u, { cache: 'reload' }); }));
  }).then(function () { return self.skipWaiting(); }));
});
self.addEventListener('activate', function (e) {
  e.waitUntil(caches.keys().then(function (keys) {
    return Promise.all(keys.filter(function (k) { return k.indexOf('pr-core-') === 0 && k !== CORE; }).map(function (k) { return caches.delete(k); }));
  }).then(function () { return self.clients.claim(); }));
});
function trimImages() {
  return caches.open(IMAGES).then(function (c) {
    return c.keys().then(function (keys) {
      var extra = keys.length - MAX_IMAGES;
      return Promise.all(keys.slice(0, Math.max(0, extra)).map(function (k) { return c.delete(k); }));
    });
  });
}
self.addEventListener('fetch', function (e) {
  var req = e.request;
  if (req.method !== 'GET') return;
  var url = new URL(req.url);
  if (url.origin === self.location.origin) {
    // cache-first for the app shell; navigations fall back to index.html
    e.respondWith(caches.match(req, { ignoreSearch: true }).then(function (hit) {
      if (hit) return hit;
      return fetch(req).catch(function () {
        if (req.mode === 'navigate') return caches.match('index.html');
        return Response.error();
      });
    }));
    return;
  }
  if (url.hostname === 'www.themealdb.com' && url.pathname.indexOf('/images/') === 0) {
    // recipe photos: cache-first, bounded
    e.respondWith(caches.open(IMAGES).then(function (c) {
      return c.match(req).then(function (hit) {
        if (hit) return hit;
        return fetch(req).then(function (res) {
          if (res && (res.ok || res.type === 'opaque')) { c.put(req, res.clone()).then(trimImages); }
          return res;
        });
      });
    }));
  }
  // TheMealDB API calls and everything else: network only (not intercepted)
});
