/*
 * Triggers a handler when the element enters the viewport, so that a list
 * loads its next page as the user reaches its end, with nothing to click.
 *
 * Usage: v-auto-load="{handler: loadMore, reobserve: false}"
 *  - handler: called each time the element becomes visible
 *  - reobserve: when true, the handler is called again after each update of the
 *    component while the element is still visible (a list that does not yet
 *    fill its container); when false, only when the element comes back in view
 */
function options(value) {
  return typeof value === 'function' ? {handler: value, reobserve: false} : (value || {});
}

export default {
  inserted(el, binding) {
    if (typeof window.IntersectionObserver !== 'function') {
      return;
    }
    const state = {opts: options(binding.value)};
    state.observer = new window.IntersectionObserver(entries => {
      if (entries.some(entry => entry.isIntersecting) && typeof state.opts.handler === 'function') {
        state.opts.handler();
      }
    });
    state.observer.observe(el);
    el.__autoLoad = state;
  },
  componentUpdated(el, binding) {
    const state = el.__autoLoad;
    if (!state) {
      return;
    }
    state.opts = options(binding.value);
    if (state.opts.reobserve) {
      // observing again reports the current visibility
      state.observer.unobserve(el);
      state.observer.observe(el);
    }
  },
  unbind(el) {
    if (el.__autoLoad) {
      el.__autoLoad.observer.disconnect();
      delete el.__autoLoad;
    }
  },
};
