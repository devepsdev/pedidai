// Las páginas públicas llegan prerenderizadas en castellano. Si la visita es en catalán (?lang=ca, elegido
// antes en este navegador o catalán como idioma principal: las mismas reglas que LanguageService), la página
// se oculta hasta que llegan los textos en catalán, para que no se vea el castellano un instante.
// LanguageService quita la clase; el CSS de index.html la muestra igualmente a los 2,5 s si algo falla.
(function () {
  try {
    var lang = new URLSearchParams(location.search).get('lang');
    if (lang !== 'es' && lang !== 'ca') {
      var stored = null;
      try { stored = localStorage.getItem('lang'); } catch (e) { /* almacenamiento no disponible */ }
      if (stored === 'es' || stored === 'ca') {
        lang = stored;
      } else {
        var main = (navigator.languages && navigator.languages[0]) || navigator.language || '';
        lang = main.toLowerCase().indexOf('ca') === 0 ? 'ca' : 'es';
      }
    }
    if (lang === 'ca') document.documentElement.classList.add('lang-pending');
  } catch (e) { /* ante cualquier fallo, se muestra en castellano como hasta ahora */ }
})();
