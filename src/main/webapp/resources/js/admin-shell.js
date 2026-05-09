/**
 * Admin shell: lg+ sidebar collapse (localStorage). Below lg, class is cleared so offcanvas works.
 */
(function () {
    'use strict';

    var STORAGE_KEY = 'admin-sidebar-collapsed';

    function shell() {
        return document.getElementById('adminShell');
    }

    function isLarge() {
        return window.matchMedia('(min-width: 992px)').matches;
    }

    function setCollapsed(collapsed) {
        var el = shell();
        if (!el) {
            return;
        }
        if (collapsed) {
            el.classList.add('admin-shell--sidebar-collapsed');
        } else {
            el.classList.remove('admin-shell--sidebar-collapsed');
        }
        try {
            window.localStorage.setItem(STORAGE_KEY, collapsed ? '1' : '0');
        } catch (ignore) {
            /* private mode */
        }
        syncAria();
    }

    function applyFromStorage() {
        var el = shell();
        if (!el) {
            return;
        }
        if (!isLarge()) {
            el.classList.remove('admin-shell--sidebar-collapsed');
            syncAria();
            return;
        }
        try {
            if (window.localStorage.getItem(STORAGE_KEY) === '1') {
                el.classList.add('admin-shell--sidebar-collapsed');
            } else {
                el.classList.remove('admin-shell--sidebar-collapsed');
            }
        } catch (ignore) {
            el.classList.remove('admin-shell--sidebar-collapsed');
        }
        syncAria();
    }

    function syncAria() {
        var el = shell();
        var collapsed = el && el.classList.contains('admin-shell--sidebar-collapsed');
        var large = isLarge();
        document.querySelectorAll('[data-admin-sidebar-action="collapse"]').forEach(function (btn) {
            btn.setAttribute('aria-expanded', large && !collapsed ? 'true' : 'false');
        });
        document.querySelectorAll('[data-admin-sidebar-action="open"]').forEach(function (btn) {
            btn.setAttribute('aria-expanded', large && collapsed ? 'true' : 'false');
        });
    }

    function onResize() {
        if (!isLarge()) {
            var el = shell();
            if (el) {
                el.classList.remove('admin-shell--sidebar-collapsed');
            }
        } else {
            applyFromStorage();
        }
        syncAria();
    }

    document.addEventListener('DOMContentLoaded', function () {
        applyFromStorage();

        document.querySelectorAll('[data-admin-sidebar-action="collapse"]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                if (!isLarge()) {
                    return;
                }
                setCollapsed(true);
            });
        });
        document.querySelectorAll('[data-admin-sidebar-action="open"]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                if (!isLarge()) {
                    return;
                }
                setCollapsed(false);
            });
        });

        window.addEventListener('resize', onResize);
    });
}());
