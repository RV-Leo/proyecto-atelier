/* Interacciones de la interfaz web de ATELIER. */

(function () {
  'use strict';

  // ---- Menú móvil ----
  var botonMenu = document.querySelector('[data-menu-boton]');
  var navegacion = document.querySelector('[data-menu]');
  if (botonMenu && navegacion) {
    botonMenu.addEventListener('click', function () {
      navegacion.classList.toggle('abierta');
      document.body.style.overflow = navegacion.classList.contains('abierta') ? 'hidden' : '';
    });
    navegacion.addEventListener('click', function (evento) {
      if (evento.target.closest('a')) {
        navegacion.classList.remove('abierta');
        document.body.style.overflow = '';
      }
    });
  }

  // ---- Revelado suave de secciones ----
  var reveladores = document.querySelectorAll('.revelar');
  if ('IntersectionObserver' in window && reveladores.length) {
    var observador = new IntersectionObserver(function (entradas) {
      entradas.forEach(function (entrada) {
        if (entrada.isIntersecting) {
          entrada.target.classList.add('visible');
          observador.unobserve(entrada.target);
        }
      });
    }, { threshold: 0.12 });
    reveladores.forEach(function (elemento) { observador.observe(elemento); });
  } else {
    reveladores.forEach(function (elemento) { elemento.classList.add('visible'); });
  }

  // ---- Selección visual de la paleta de colores ----
  document.querySelectorAll('[data-paleta] input[type="checkbox"]').forEach(function (casilla) {
    casilla.addEventListener('change', function () {
      casilla.closest('.opcion-paleta').classList.toggle('seleccionada', casilla.checked);
    });
  });

  // ---- Confirmación para acciones destructivas ----
  document.querySelectorAll('[data-confirmar]').forEach(function (boton) {
    boton.addEventListener('click', function (evento) {
      var mensaje = boton.getAttribute('data-confirmar') || '¿Continuar?';
      if (!window.confirm(mensaje)) {
        evento.preventDefault();
      }
    });
  });

  // ---- Contadores de métricas (solo lectura visual) ----
  document.querySelectorAll('[data-contador]').forEach(function (elemento) {
    var objetivo = parseInt(elemento.getAttribute('data-contador'), 10) || 0;
    if (isNaN(objetivo)) { return; }
    var duracion = 700;
    var inicio = null;
    function paso(marca) {
      if (!inicio) { inicio = marca; }
      var progreso = Math.min((marca - inicio) / duracion, 1);
      elemento.textContent = Math.floor(progreso * objetivo).toLocaleString('es-EC');
      if (progreso < 1) { window.requestAnimationFrame(paso); }
    }
    window.requestAnimationFrame(paso);
  });
})();