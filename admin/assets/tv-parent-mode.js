(function () {
  'use strict';
  if (new URLSearchParams(location.search).get('tvParentMode') !== '1') return;
  document.documentElement.classList.add('tv-parent-mode');
  var css = document.createElement('link');
  css.rel = 'stylesheet'; css.href = './assets/tv-parent-mode.css';
  document.head.appendChild(css);
  var originalFetch = window.fetch;
  window.fetch = function (url, options) {
    return originalFetch.apply(this, arguments).then(function (response) {
      var path = typeof url === 'string' ? url : url.url;
      if (options && options.method === 'POST' && /\/api\/parent-mode$/.test(path) && response.ok) {
        response.clone().json().then(function (data) {
          if (typeof data.active === 'boolean' && window.SOTVParent) SOTVParent.modeChanged();
        });
      }
      return response;
    });
  };
  function targets() {
    return Array.prototype.filter.call(document.querySelectorAll('input,button,a[href],[tabindex="0"]'), function (el) {
      return !el.disabled && el.getClientRects().length && getComputedStyle(el).visibility !== 'hidden';
    });
  }
  function move(direction) {
    var list = targets(), current = list.indexOf(document.activeElement);
    if (!list.length) return;
    var next = Math.max(0, Math.min(list.length - 1, current + direction));
    list[next].focus(); list[next].scrollIntoView({block: 'nearest'});
  }
  window.TV = {
    move: move,
    horizontal: function (direction) {
      var el = document.activeElement;
      if (el && el.tagName === 'INPUT') {
        var position = Math.max(0, Math.min(el.value.length, (el.selectionStart || 0) + direction));
        try { el.setSelectionRange(position, position); } catch (ignore) {}
      } else move(direction);
    },
    submit: function () {
      var el = document.activeElement;
      var submit = el && el.form && el.form.querySelector('button[type="submit"]');
      if (submit && !submit.disabled) submit.click(); else window.TV.select();
    },
    select: function () {
      var el = document.activeElement;
      if (!el || el === document.body) { move(1); return; }
      if (el.tagName === 'INPUT') {
        el.focus(); if (window.SOTVParent) SOTVParent.showKeyboard();
      } else el.click();
    }
  };
  var focusedLogin = false, focusedCard = false;
  function adapt() {
    var host = document.getElementById('device-host');
    if (host && !host.readOnly) {
      // The dashboard already defaults to this device; keep its real form and auth.
      host.readOnly = true; host.tabIndex = -1;
      host.closest('.space-y-2').classList.add('tv-local-host');
    }
    if (!focusedLogin && document.getElementById('management-secret')) {
      var submit = document.querySelector('.login-card button[type="submit"]');
      if (submit) { submit.focus(); focusedLogin = true; }
    }
    var titles = document.querySelectorAll('[data-slot="card-title"]');
    for (var i = 0; i < titles.length; i++) {
      if (titles[i].textContent.trim() !== '家长免答题') continue;
      var card = titles[i].closest('[data-slot="card"]');
      card.classList.add('sotv-focus-card');
      var node = card.parentElement;
      while (node && node.id !== 'root') { node.classList.add('sotv-parent-path'); node = node.parentElement; }
      document.documentElement.classList.add('tv-parent-controls');
      if (!focusedCard) {
        var first = card.querySelector('button:not(:disabled)');
        if (first) { first.focus(); focusedCard = true; }
      }
      break;
    }
  }
  function boot() {
    adapt();
    new MutationObserver(adapt).observe(document.getElementById('root'), {childList: true, subtree: true});
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot); else boot();
})();
