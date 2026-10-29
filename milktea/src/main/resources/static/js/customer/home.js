// A failed image falls back once to a local asset; no external image dependency.
document.querySelectorAll('[data-home-image]').forEach((image) => {
    const useFallback = () => {
        if (image.dataset.fallbackUsed) return;
        image.dataset.fallbackUsed = 'true';
        image.src = image.dataset.fallback;
    };
    image.addEventListener('error', useFallback);
    if (image.complete && image.naturalWidth === 0) useFallback();
});
