(function (factory) {
    // Support three module loading scenarios
    if (typeof require === 'function' && typeof exports === 'object' && typeof module === 'object') {
        // [1] CommonJS/Node.js
        var target = module['exports'] || exports; // module.exports is for Node.js
        factory(target, require);
    } else if (typeof define === 'function' && define['amd']) {
        // [2] AMD anonymous module
        define(['exports', 'Promise'], factory);
    } else {
        // [3] No module loader (plain <script> tag) - put directly in global namespace
        window['Cask'] = window['Cask'] || {};
        factory(window['Cask']);
    }
}(function (target, require) {
    target['Promise'] = target['Promise'] || function () {
        var success_handlers_stack = [],
            error_handlers_stack = [],
            notification_handlers_stack = [],

            resolve_value = null,
            reject_reason = null,
            notify_value_stack = [];

        var fireResolve = function () {
                if(null != resolve_value && success_handlers_stack.length) {
                    while(success_handlers_stack.length) {
                        success_handlers_stack.shift()(resolve_value);
                    }

                    error_handlers_stack = [];
                }
            },
            fireReject = function () {
                if(null != reject_reason && error_handlers_stack.length) {
                    while(error_handlers_stack.length) {
                        error_handlers_stack.shift()(reject_reason);
                    }

                    success_handlers_stack = [];
                }
            },
            fireNotify = function () {
                if(notify_value_stack.length && notification_handlers_stack.length) {
                    var message = null;

                    while(notify_value_stack.length > 0) {
                        message = notify_value_stack.shift();

                        while(notification_handlers_stack.length) {
                            notification_handlers_stack.shift()(message);
                        }
                    }
                }
            },

            /**
             * Sets up event handlers.
             *
             * @param {function} [success=null] - fired in case of successful promise resolving.
             * @param {function} [error=null]   - fired in case of promise resolving with error
             * @param {function} [notify=null]  - used to notify about promise working progress.
             */
            thenImpl = function (success, error, notify) {
                if (null != success) {
                    if ('function' === typeof success) {
                        success_handlers_stack.push(success);
                    } else {
                        throw TypeError('"success" parameter have to be a function.');
                    }
                }

                if (null != error) {
                    if ('function' === typeof error) {
                        error_handlers_stack.push(error);
                    } else {
                        throw TypeError('"error" parameter have to be a function.');
                    }
                }

                if (null != notify) {
                    if ('function' === typeof notify) {
                        notification_handlers_stack.push(notify);
                    } else {
                        throw TypeError('"notify" parameter have to be a function.');
                    }
                }

                fireResolve();
                fireReject();
                fireNotify();

                return this;
            },

            /**
             * Syntax sugar for 'then(null, errror_handler)'
             *
             * @param {function} handler
             */
            catchImpl = function (handler) {
                return thenImpl.apply(this, [null, handler]);
            },

            /**
             * Resolves the derived promise with the value.
             *
             * @param {any} value
             */
            resolveImpl = function (value) {
                resolve_value = value;
                fireResolve();
            },

            /**
             * Rejects the derived promise with the reason.
             *
             * @param {any} reason
             */
            rejectImpl = function (reason) {
                reject_reason = reason;
                fireReject();
            },

            /**
             * Notifies the derived promise with the value.
             *
             * @param {any} value
             */
            notifyImpl = function (value) {
                notify_value_stack.push(value);
                fireNotify();
            };

        return {
            then: thenImpl,
            catch: catchImpl,
            resolve: resolveImpl,
            reject: rejectImpl,
            notify: notifyImpl
        };
    };
}));
