'use strict';
/* FarmJM Web: interfaz de una página. Todo el texto dinámico pasa por esc() para evitar XSS. */
const $ = (s, e = document) => e.querySelector(s);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const dinero = n => 'RD$ ' + Number(n || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const COLORES = ['#0F766E', '#14B8A6', '#B45309', '#14233A', '#047857', '#B91C1C'];
let token = sessionStorage.getItem('fjm_token');
let yo = null;
let actual = null;

// ---------------------------------------------------------------- API
async function api(ruta, { metodo, cuerpo, blob } = {}) {
  const r = await fetch('/api' + ruta, {
    method: metodo || (cuerpo !== undefined ? 'POST' : 'GET'),
    headers: { ...(token ? { Authorization: 'Bearer ' + token } : {}), ...(cuerpo !== undefined ? { 'Content-Type': 'application/json' } : {}) },
    body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined
  });
  if (r.ok && blob) return r.blob();
  let d = {};
  try { d = await r.json(); } catch (e) { /* sin cuerpo */ }
  if (!r.ok) {
    if (d.sesion) { cerrarLocal(); }
    const err = new Error(d.error || 'Error de conexión con el servidor.');
    err.volver = d.volver;
    throw err;
  }
  return d;
}
const puede = (mod, acc) => !!(yo && yo.permisos[mod] && yo.permisos[mod].includes(acc));

// ---------------------------------------------------------------- acceso (login + 2FA)
function vistaLogin(aviso) {
  $('#app').innerHTML = `<div class="login"><form class="card-login" id="f">
    <div class="logo">✚</div><h1>FARMACIA JM</h1><p class="muted">Sistema Integral de Farmacia · versión web</p>
    <label for="u">USUARIO</label><input id="u" autocomplete="username" required>
    <label for="c">CONTRASEÑA</label><input id="c" type="password" autocomplete="current-password" required>
    <p class="err" id="e" role="alert">${esc(aviso || '')}</p>
    <button class="btn p" style="width:100%">Iniciar sesión</button>
    <p class="muted">Acceso protegido con verificación en dos pasos (2FA).</p></form></div>`;
  $('#f').onsubmit = async ev => {
    ev.preventDefault();
    const b = $('#f button'); b.disabled = true; $('#e').textContent = '';
    try { vistaDosFactores(await api('/login', { cuerpo: { usuario: $('#u').value, clave: $('#c').value } })); }
    catch (e) { $('#e').textContent = e.message; $('#c').value = ''; b.disabled = false; }
  };
  $('#u').focus();
}

function vistaDosFactores(r) {
  const qr = r.activar ? `<p>1. Instale Google Authenticator o Microsoft Authenticator.</p><p>2. Escanee este código QR (o escriba la clave).</p>
    <img src="${esc(r.qr)}" alt="Código QR para el 2FA"><label>CLAVE MANUAL</label><div class="secret">${esc(r.secreto.replace(/(.{4})/g, '$1 ').trim())}</div>
    <p>3. Escriba el código de 6 dígitos que muestra la aplicación.</p>` : '<p>Escriba el código de 6 dígitos de su aplicación Authenticator.</p>';
  $('#app').innerHTML = `<div class="login"><form class="card-login" id="f"><h1>${r.activar ? 'Active la verificación en dos pasos' : 'Verificación en dos pasos'}</h1>
    ${qr}<label for="k">CÓDIGO</label><input id="k" class="codigo" inputmode="numeric" maxlength="6" pattern="\\d{6}" autocomplete="one-time-code" placeholder="000000" required>
    <p class="err" id="e" role="alert"></p><button class="btn p" style="width:100%">Verificar</button>
    <p class="muted">Máximo 3 intentos; luego la cuenta se bloquea 5 minutos.</p><button type="button" class="link" id="v">Volver al inicio de sesión</button></form></div>`;
  $('#v').onclick = () => vistaLogin();
  $('#k').oninput = e => { e.target.value = e.target.value.replace(/\D/g, ''); };
  $('#f').onsubmit = async ev => {
    ev.preventDefault();
    const b = $('#f button'); b.disabled = true; $('#e').textContent = '';
    try {
      const d = await api('/2fa', { cuerpo: { pendiente: r.pendiente, codigo: $('#k').value } });
      token = d.token; sessionStorage.setItem('fjm_token', token); yo = d; vistaApp();
    } catch (e) {
      if (e.volver) return vistaLogin(e.message);
      $('#e').textContent = e.message; $('#k').value = ''; b.disabled = false;
    }
  };
  $('#k').focus();
}

function cerrarLocal() { token = null; yo = null; sessionStorage.removeItem('fjm_token'); vistaLogin('Su sesión terminó. Inicie sesión de nuevo.'); }
async function salir() { try { await api('/salir', { cuerpo: {} }); } catch (e) { /* ya expiró */ } token = null; yo = null; sessionStorage.removeItem('fjm_token'); vistaLogin(); }

// ---------------------------------------------------------------- estructura
const MODULOS = [
  ['DASHBOARD', 'Dashboard', '🏠', vistaDashboard], ['POS', 'Punto de Venta', '🛒', vistaPos], ['INVENTARIO', 'Inventario & Lotes', '📦', vistaInventario],
  ['CLIENTES', 'Clientes', '👥', vistaClientes], ['CAJA', 'Caja', '🏛', vistaCaja], ['REPORTES', 'Reportes', '📊', vistaReportes],
  ['USUARIOS', 'Usuarios y Roles', '🔑', vistaUsuarios], ['AUDITORIA', 'Auditoría & Logs', '🛡', vistaAuditoria]
];

function vistaApp() {
  const lista = MODULOS.filter(m => puede(m[0], 'VER'));
  $('#app').innerHTML = `<div class="shell"><header class="top"><span class="t" id="tit"></span><span class="sp"></span>
    <span class="chip">${esc(yo.rol)}</span><span class="quien muted">${esc(yo.nombre)}</span></header>
    <nav class="side" aria-label="Principal"><div class="marca"><div class="logo">✚</div><span>FARMACIA JM</span></div>
    ${lista.map(m => `<button class="nav" data-m="${m[0]}" title="${esc(m[1])}"><span class="ico">${m[2]}</span><span class="tx">${esc(m[1])}</span></button>`).join('')}
    <div class="pie"><b>${esc(yo.nombre)}</b>${esc(yo.rol)}</div><button class="salir" id="s" title="Cerrar sesión">⎋ <span class="tx">Cerrar sesión</span></button></nav>
    <main id="main"></main></div>`;
  document.querySelectorAll('.nav').forEach(b => b.onclick = () => ir(b.dataset.m));
  $('#s').onclick = salir;
  ir(lista[0][0]);
}

async function ir(id) {
  const m = MODULOS.find(x => x[0] === id);
  actual = id;
  document.querySelectorAll('.nav').forEach(b => b.classList.toggle('on', b.dataset.m === id));
  $('#tit').textContent = m[1];
  $('#main').innerHTML = '<p class="muted">Cargando...</p>';
  try { await m[3]($('#main')); } catch (e) { $('#main').innerHTML = `<p class="err">${esc(e.message)}</p>`; }
}

// ---------------------------------------------------------------- utilidades de UI
function modal(titulo, html, guardar) {
  const d = $('#modal');
  d.innerHTML = `<form method="dialog" id="mf"><h1>${esc(titulo)}</h1>${html}<p class="err" id="me" role="alert"></p>
    <div class="fila" style="justify-content:flex-end"><button type="button" class="btn" id="mc">Cancelar</button><button class="btn p" id="mg">Guardar</button></div></form>`;
  $('#mc').onclick = () => d.close();
  $('#mf').onsubmit = async ev => {
    ev.preventDefault();
    $('#mg').disabled = true; $('#me').textContent = '';
    try { await guardar(); d.close(); } catch (e) { $('#me').textContent = e.message; $('#mg').disabled = false; }
  };
  d.showModal();
}
const campo = (id, et, v = '', tipo = 'text', extra = '') => `<label for="${id}">${et}</label><input id="${id}" type="${tipo}" value="${esc(v)}" ${extra}>`;
const val = id => $('#' + id).value.trim();

/** Tabla con una fila seleccionable. columnas: [titulo, fn(fila), claseOpcional]. */
function tabla(cols, filas, vacio = 'Sin datos') {
  return `<div class="tabla"><table><thead><tr>${cols.map(c => `<th class="${c[2] || ''}">${esc(c[0])}</th>`).join('')}</tr></thead><tbody>${
    filas.length ? filas.map((f, i) => `<tr data-i="${i}">${cols.map(c => `<td class="${c[2] || ''}">${c[1](f)}</td>`).join('')}</tr>`).join('') : `<tr><td colspan="${cols.length}" class="muted">${vacio}</td></tr>`
  }</tbody></table></div>`;
}
function seleccion(cont, filas, alCambiar) {
  cont.querySelectorAll('tbody tr[data-i]').forEach(tr => tr.onclick = () => {
    cont.querySelectorAll('tr.sel').forEach(x => x.classList.remove('sel')); tr.classList.add('sel'); alCambiar(filas[+tr.dataset.i]);
  });
}
const badge = (t, c) => `<span class="badge b-${c}">${esc(t)}</span>`;

// ---------------------------------------------------------------- Dashboard
async function vistaDashboard(el) {
  const d = await api('/dashboard');
  const max = Math.max(1, ...d.barras.map(b => b.valor));
  const total = d.pastel.reduce((a, p) => a + p.valor, 0) || 1;
  let acum = 0;
  const gradiente = d.pastel.map((p, i) => { const a = acum; acum += p.valor / total * 100; return `${COLORES[i % 6]} ${a}% ${acum}%`; }).join(',');
  el.innerHTML = `<h1>Dashboard</h1><p class="muted">${esc(d.subtitulo)}</p>
    <div class="grid k4" style="margin:.9rem 0">${d.kpis.map(k => `<div class="kpi ${esc(k.nivel)}"><small>${esc(k.etiqueta)}</small><b>${esc(k.valor)}</b></div>`).join('')}</div>
    <div class="grid g2"><div class="panel"><b>${esc(d.tituloBarras)}</b><div class="barras">${d.barras.map(b =>
      `<div><span>${b.valor ? Math.round(b.valor).toLocaleString('en-US') : ''}</span><i style="height:${Math.max(1, b.valor / max * 140)}px"></i>${esc(b.etiqueta)}</div>`).join('')}</div></div>
    <div class="panel"><b>${esc(d.tituloPastel)}</b><div class="pastel"><div class="disco" style="background:conic-gradient(${d.pastel.length ? gradiente : '#E2E8F0 0 100%'})" role="img" aria-label="Gráfico de pastel"></div>
    <ul class="leyenda">${d.pastel.map((p, i) => `<li><i style="background:${COLORES[i % 6]}"></i>${esc(p.etiqueta)} (${p.valor})</li>`).join('')}</ul></div></div></div>
    <h2 style="margin:1rem 0 .4rem;font-size:1rem">Actividad reciente</h2>${tabla([['Fecha y hora', m => esc(m.fecha), 'sec'], ['Usuario', m => esc(m.usuario), 'sec'], ['Módulo', m => esc(m.modulo)], ['Acción', m => esc(m.accion)]], d.movimientos, 'Sin actividad reciente')}`;
}

// ---------------------------------------------------------------- Inventario
async function vistaInventario(el) {
  const meds = await api('/medicamentos');
  let sel = null;
  const estado = m => m.dias < 0 ? badge('Vencido', 'bad') : m.dias <= 30 ? badge('Vence en ' + m.dias + ' d', 'bad') : m.dias <= 90 ? badge('Por vencer', 'warn') : m.stock <= 10 ? badge('Stock bajo', 'warn') : badge('Vigente', 'ok');
  el.innerHTML = `<h1>Inventario & Lotes</h1><p class="muted">Existencias por medicamento y lote más próximo a vencer (regla FEFO).</p>
    <div class="fila">${puede('INVENTARIO', 'CREAR') ? '<button class="btn p" id="n">＋ Nuevo medicamento</button>' : ''}${puede('INVENTARIO', 'EDITAR') ? '<button class="btn" id="e" disabled>Editar</button>' : ''}
    ${puede('INVENTARIO', 'ELIMINAR') ? '<button class="btn d" id="x" disabled>Eliminar</button>' : ''}</div><p class="err" id="er"></p>
    <div id="t">${tabla([['Código', m => esc(m.codigo), 'sec'], ['Medicamento', m => esc(m.nombre)], ['Principio activo', m => esc(m.principioActivo), 'sec'], ['Categoría', m => esc(m.categoria), 'sec'],
      ['Precio', m => dinero(m.precio)], ['Stock', m => m.stock], ['Lote', m => esc(m.lote), 'sec'], ['Vence', m => esc(m.vence), 'sec'], ['Estado', estado]], meds)}</div>`;
  const hab = () => { ['e', 'x'].forEach(i => { const b = $('#' + i); if (b) b.disabled = !sel; }); };
  seleccion($('#t'), meds, f => { sel = f; hab(); });
  const form = async m => {
    const cats = await api('/categorias');
    m = m || { id: 0, codigo: '', nombre: '', principioActivo: '', presentacion: '', categoriaId: 0, precio: '', stock: '', requiereReceta: false, controlado: false, idLote: 0, lote: '', vence: '' };
    modal(m.id ? 'Editar medicamento' : 'Nuevo medicamento', campo('f1', 'CÓDIGO DE BARRAS *', m.codigo) + campo('f2', 'NOMBRE COMERCIAL *', m.nombre) + campo('f3', 'PRINCIPIO ACTIVO *', m.principioActivo) +
      campo('f4', 'PRESENTACIÓN', m.presentacion) + `<label for="f5">CATEGORÍA *</label><select id="f5"><option value="0">Seleccione...</option>${cats.map(c => `<option value="${c.id}" ${c.id === m.categoriaId ? 'selected' : ''}>${esc(c.nombre)}</option>`).join('')}</select>` +
      campo('f6', 'PRECIO (RD$) *', m.precio, 'number', 'step="0.01" min="0"') + campo('f7', 'STOCK (UNIDADES) *', m.stock, 'number', 'min="0" step="1"') +
      `<div class="chk"><label class="chk"><input type="checkbox" id="f8" ${m.requiereReceta ? 'checked' : ''}> Requiere receta</label><label class="chk"><input type="checkbox" id="f9" ${m.controlado ? 'checked' : ''}> Controlado</label></div>` +
      campo('fa', 'LOTE *', m.lote) + campo('fb', 'FECHA DE VENCIMIENTO *', m.vence, 'date'),
      async () => {
        await api('/medicamentos', { cuerpo: { id: m.id, codigo: val('f1'), nombre: val('f2'), principioActivo: val('f3'), presentacion: val('f4'), categoriaId: +val('f5'), precio: val('f6'),
          stock: val('f7'), requiereReceta: $('#f8').checked, controlado: $('#f9').checked, idLote: m.idLote, lote: val('fa'), vence: val('fb') } });
        ir('INVENTARIO');
      });
  };
  if ($('#n')) $('#n').onclick = () => form();
  if ($('#e')) $('#e').onclick = () => sel && form(sel);
  if ($('#x')) $('#x').onclick = async () => {
    if (!sel || !confirm('¿Eliminar ' + sel.nombre + ' del inventario? Sus ventas anteriores se conservan.')) return;
    try { await api('/medicamentos/' + sel.id, { metodo: 'DELETE' }); ir('INVENTARIO'); } catch (e) { $('#er').textContent = e.message; }
  };
}

// ---------------------------------------------------------------- Clientes
async function vistaClientes(el) {
  const cl = await api('/clientes');
  let sel = null;
  el.innerHTML = `<h1>Clientes</h1><p class="muted">Registro de clientes para facturación y recetas.</p>
    <div class="fila">${puede('CLIENTES', 'CREAR') ? '<button class="btn p" id="n">＋ Nuevo cliente</button>' : ''}${puede('CLIENTES', 'EDITAR') ? '<button class="btn" id="e" disabled>Editar</button>' : ''}
    ${puede('CLIENTES', 'ELIMINAR') ? '<button class="btn d" id="x" disabled>Eliminar</button>' : ''}</div><p class="err" id="er"></p>
    <div id="t">${tabla([['Nombre', c => esc(c.nombre)], ['Cédula / RNC', c => esc(c.doc), 'sec'], ['Teléfono', c => esc(c.telefono)], ['Correo', c => esc(c.email), 'sec']], cl)}</div>`;
  seleccion($('#t'), cl, f => { sel = f; ['e', 'x'].forEach(i => { if ($('#' + i)) $('#' + i).disabled = false; }); });
  const form = c => {
    c = c || { id: 0, nombre: '', doc: '', telefono: '', email: '' };
    modal(c.id ? 'Editar cliente' : 'Nuevo cliente', campo('c1', 'NOMBRE *', c.nombre) + campo('c2', 'CÉDULA / RNC', c.doc) + campo('c3', 'TELÉFONO', c.telefono) + campo('c4', 'CORREO ELECTRÓNICO', c.email),
      async () => {
        if (!val('c1')) throw new Error('El nombre del cliente es obligatorio.');
        await api('/clientes', { cuerpo: { id: c.id, nombre: val('c1'), doc: val('c2'), telefono: val('c3'), email: val('c4') } });
        ir('CLIENTES');
      });
  };
  if ($('#n')) $('#n').onclick = () => form();
  if ($('#e')) $('#e').onclick = () => sel && form(sel);
  if ($('#x')) $('#x').onclick = async () => {
    if (!sel || !confirm('¿Eliminar a ' + sel.nombre + '?')) return;
    try { await api('/clientes/' + sel.id, { metodo: 'DELETE' }); ir('CLIENTES'); } catch (e) { $('#er').textContent = e.message; }
  };
}

// ---------------------------------------------------------------- Usuarios
async function vistaUsuarios(el) {
  const [us, roles] = await Promise.all([api('/usuarios'), api('/roles')]);
  let sel = null;
  el.innerHTML = `<h1>Usuarios y Roles</h1><p class="muted">Solo el Administrador gestiona cuentas. Las contraseñas se guardan con hash BCrypt.</p>
    <div class="fila"><button class="btn p" id="n">＋ Nuevo usuario</button><button class="btn" id="e" disabled>Editar rol / estado</button><button class="btn" id="r" disabled>Restablecer 2FA</button></div><p class="err" id="er"></p>
    <div id="t">${tabla([['Usuario', u => esc(u.username)], ['Nombre', u => esc(u.nombre + ' ' + u.apellido), 'sec'], ['Rol', u => esc(u.rol)], ['Estado', u => u.activo ? badge('Activo', 'ok') : badge('Desactivado', 'bad')],
      ['2FA', u => u.totp ? badge('Activo', 'ok') : badge('Pendiente', 'warn'), 'sec'], ['Bloqueo', u => u.bloqueado ? badge('Bloqueado', 'bad') : '-', 'sec']], us)}</div>`;
  seleccion($('#t'), us, f => { sel = f; $('#e').disabled = false; $('#r').disabled = false; });
  const form = u => {
    const nuevo = !u;
    u = u || { id: 0, nombre: '', apellido: '', username: '', email: '', licencia: '', rolId: 0, activo: true };
    modal(nuevo ? 'Nuevo usuario' : 'Editar usuario', campo('u1', 'NOMBRE *', u.nombre) + campo('u2', 'APELLIDO *', u.apellido) + campo('u3', 'USUARIO *', u.username, 'text', nuevo ? '' : 'disabled') +
      campo('u4', 'CORREO ELECTRÓNICO', u.email) + campo('u5', 'LICENCIA / MATRÍCULA', u.licencia) +
      `<label for="u6">ROL *</label><select id="u6"><option value="0">Seleccione...</option>${roles.map(r => `<option value="${r.id}" ${r.id === u.rolId ? 'selected' : ''}>${esc(r.nombre)}</option>`).join('')}</select>` +
      campo('u7', nuevo ? 'CONTRASEÑA *' : 'NUEVA CONTRASEÑA (opcional)', '', 'password', 'autocomplete="new-password"') +
      `<label class="chk"><input type="checkbox" id="u8" ${u.activo ? 'checked' : ''}> Cuenta activa</label>`,
      async () => {
        if (!+val('u6')) throw new Error('Seleccione un rol.');
        await api('/usuarios', { cuerpo: { id: u.id, nombre: val('u1'), apellido: val('u2'), username: val('u3') || u.username, email: val('u4'), licencia: val('u5'), rolId: +val('u6'), clave: $('#u7').value, activo: $('#u8').checked } });
        ir('USUARIOS');
      });
  };
  $('#n').onclick = () => form();
  $('#e').onclick = () => sel && form(sel);
  $('#r').onclick = async () => {
    if (!sel || !confirm('Se borrará la clave 2FA de ' + sel.username + '. Deberá escanear un QR nuevo en su próximo ingreso. ¿Continuar?')) return;
    try { await api('/usuarios/' + sel.id + '/reset2fa', { cuerpo: {} }); ir('USUARIOS'); } catch (e) { $('#er').textContent = e.message; }
  };
}

// ---------------------------------------------------------------- Reportes
async function vistaReportes(el) {
  const hoy = new Date().toISOString().slice(0, 10), mes = hoy.slice(0, 8) + '01';
  const contenido = { Administrador: 'Ventas de todos los usuarios, inventario crítico y bitácora de accesos', Supervisor: 'Ventas de todos los usuarios e inventario crítico' }[yo.rol] || 'Sus propias ventas';
  el.innerHTML = `<h1>Reportes</h1><p class="muted">Genere un PDF con encabezado (sistema, fecha y usuario) y filtro por fechas.</p>
    <div class="panel" style="max-width:520px;margin-top:.8rem"><label>CONTENIDO SEGÚN SU ROL</label><p>${esc(contenido)}.</p>
    <div class="fila"><div><label for="d1">DESDE</label><input id="d1" type="date" value="${mes}"></div><div><label for="d2">HASTA</label><input id="d2" type="date" value="${hoy}"></div></div>
    <button class="btn p" id="g">📄 Generar reporte PDF</button><p id="m" class="muted"></p></div>`;
  $('#g').onclick = async () => {
    $('#g').disabled = true; $('#m').className = 'muted'; $('#m').textContent = 'Generando el reporte...';
    try {
      const b = await api('/reporte?desde=' + $('#d1').value + '&hasta=' + $('#d2').value, { blob: true });
      const a = document.createElement('a'); a.href = URL.createObjectURL(b); a.download = 'Reporte_FarmJM_' + hoy + '.pdf'; a.click();
      $('#m').textContent = 'Reporte generado y descargado.';
    } catch (e) { $('#m').className = 'err'; $('#m').textContent = e.message; }
    $('#g').disabled = false;
  };
}

// ---------------------------------------------------------------- Auditoría
async function vistaAuditoria(el) {
  const a = await api('/auditoria');
  const act = tabla([['Fecha y hora', x => esc(x.fecha), 'sec'], ['Usuario', x => esc(x.usuario), 'sec'], ['Módulo', x => esc(x.modulo)], ['Acción', x => esc(x.accion)]], a.actividad);
  const acc = tabla([['Fecha y hora', x => esc(x.fecha), 'sec'], ['Usuario', x => esc(x.usuario)], ['Resultado', x => x.resultado === 'EXITOSO' ? badge('Exitoso', 'ok') : badge('Fallido', 'bad')], ['Detalle', x => esc(x.detalle)]], a.accesos);
  el.innerHTML = `<h1>Auditoría & Logs</h1><p class="muted">Cada acción importante y cada inicio de sesión (exitoso o fallido) queda registrado.</p>
    <div class="tabs"><button class="btn on" id="t1">Actividad del sistema</button><button class="btn" id="t2">Bitácora de accesos</button></div><div id="c">${act}</div>`;
  const cambiar = (n, html) => { $('#t1').classList.toggle('on', n === 1); $('#t2').classList.toggle('on', n === 2); $('#c').innerHTML = html; };
  $('#t1').onclick = () => cambiar(1, act); $('#t2').onclick = () => cambiar(2, acc);
}

// ---------------------------------------------------------------- Caja
async function vistaCaja(el) {
  const c = await api('/caja');
  el.innerHTML = `<h1>Arqueo & Cierre de Caja</h1><div class="panel" style="max-width:480px;margin-top:.8rem">
    <p><b>${c.id ? 'Caja #' + String(c.id).padStart(3, '0') + ' — ' + (c.abierta ? 'Abierta' : 'Cerrada') : 'Sin caja abierta'}</b></p>
    <p class="muted">Monto base: ${dinero(c.base)} · Efectivo de ventas: ${dinero(c.efectivo)}</p>
    ${c.abierta ? `<p>Esperado en caja: <b>${dinero(c.base + c.efectivo)}</b></p>${campo('mc', 'EFECTIVO CONTADO (RD$)', '', 'number', 'min="0" step="0.01"')}<p class="err" id="er"></p><button class="btn p" id="ci">Cerrar caja</button>`
      : (puede('CAJA', 'CREAR') ? '<button class="btn p" id="ab">Abrir nueva caja</button><p class="err" id="er"></p>' : '')}</div><div id="r"></div>`;
  if ($('#ab')) $('#ab').onclick = async () => { try { await api('/caja/abrir', { cuerpo: {} }); ir('CAJA'); } catch (e) { $('#er').textContent = e.message; } };
  if ($('#ci')) $('#ci').onclick = async () => {
    try {
      const r = await api('/caja/cerrar', { cuerpo: { contado: +$('#mc').value } });
      el.innerHTML = `<h1>Caja cerrada</h1><div class="panel" style="max-width:480px;margin-top:.8rem"><p>Esperado: ${dinero(r.esperado)}</p><p>Contado: ${dinero(r.contado)}</p><p><b>Diferencia: ${dinero(r.diferencia)}</b></p></div>`;
    } catch (e) { $('#er').textContent = e.message; }
  };
}

// ---------------------------------------------------------------- Punto de venta
async function vistaPos(el) {
  const [meds, cfg] = await Promise.all([api('/medicamentos'), api('/pos')]);
  const carrito = new Map();
  let metodo = 'Efectivo';
  el.innerHTML = `<h1>Punto de Venta</h1>${cfg.caja.abierta ? '' : '<p class="err">La caja está cerrada. Ábrala en el módulo Caja para poder facturar.</p>'}
    <div class="pos"><div><input id="q" placeholder="Buscar medicamento por nombre, código o principio activo" aria-label="Buscar"><div class="cat" id="cat"></div></div>
    <div class="orden"><h2>Orden de venta</h2><div class="cuerpo"><label for="tc">TIPO DE COMPROBANTE (NCF)</label><select id="tc">${cfg.comprobantes.map(c => `<option value="${c.id}">${esc(c.nombre)}</option>`).join('')}</select>
    <div id="lin"></div><label for="ds">DESCUENTO</label><select id="ds"><option value="0">Sin descuento</option>${cfg.descuentos.map(d => `<option value="${d.id}" data-p="${d.porcentaje}">${esc(d.nombre)}</option>`).join('')}</select>
    <div id="tot"></div><div class="fila" id="pg">${['Efectivo', 'Tarjeta', 'Mixto'].map(m => `<button class="btn ${m === metodo ? 'p' : ''}" data-m="${m}">${m}</button>`).join('')}</div>
    <p class="err" id="er"></p><button class="btn p" id="pr" style="width:100%" disabled>PROCESAR FACTURA</button></div></div></div>`;
  const pintarCat = () => {
    const q = $('#q').value.toLowerCase();
    const f = meds.filter(m => !q || (m.nombre + m.codigo + m.principioActivo).toLowerCase().includes(q));
    $('#cat').innerHTML = f.map(m => `<button class="item" data-id="${m.id}"><span class="muted">${esc(m.codigo)}</span>${m.requiereReceta ? ' ' + badge('RECETA', 'warn') : ''}<b>${esc(m.nombre)}</b>
      <span class="muted">${esc(m.principioActivo)}</span><br><b>${dinero(m.precio)}</b> ${m.dias <= 30 ? badge(m.lote, 'bad') : m.dias <= 90 ? badge(m.lote, 'warn') : badge(m.lote, 'ok')}<br><span class="muted">Stock: ${m.stock}</span></button>`).join('') || '<p class="muted">Sin resultados</p>';
    $('#cat').querySelectorAll('.item').forEach(b => b.onclick = () => agregar(meds.find(m => m.id === +b.dataset.id)));
  };
  const agregar = m => {
    const n = carrito.get(m.id) || 0;
    if (n + 1 > m.stock) { $('#er').textContent = 'Stock insuficiente de ' + m.nombre + '.'; return; }
    if (n === 0 && m.requiereReceta && !confirm(m.nombre + ' requiere receta. ¿Verificó la receta del paciente?')) return;
    $('#er').textContent = ''; carrito.set(m.id, n + 1); pintarOrden();
  };
  const pintarOrden = () => {
    const filas = [...carrito].map(([id, n]) => ({ m: meds.find(x => x.id === id), n }));
    $('#lin').innerHTML = filas.length ? filas.map(f => `<div class="lin"><span><b>${esc(f.m.nombre)}</b><br><small class="muted">${dinero(f.m.precio)}</small></span>
      <button data-a="-" data-id="${f.m.id}" aria-label="Quitar uno">−</button><b>${f.n}</b><button data-a="+" data-id="${f.m.id}" aria-label="Agregar uno">+</button></div>`).join('') : '<p class="muted">Carrito vacío. Seleccione productos.</p>';
    $('#lin').querySelectorAll('button').forEach(b => b.onclick = () => {
      const id = +b.dataset.id, n = carrito.get(id) + (b.dataset.a === '+' ? 1 : -1), m = meds.find(x => x.id === id);
      if (n > m.stock) { $('#er').textContent = 'Stock insuficiente de ' + m.nombre + '.'; return; }
      if (n <= 0) carrito.delete(id); else carrito.set(id, n); pintarOrden();
    });
    const sub = filas.reduce((a, f) => a + f.m.precio * f.n, 0), p = +($('#ds').selectedOptions[0].dataset.p || 0), desc = sub * p / 100, itbis = (sub - desc) * 0.18;
    $('#tot').innerHTML = `<div class="tot"><span>Subtotal</span><span>${dinero(sub)}</span></div><div class="tot"><span>Descuento</span><span>- ${dinero(desc)}</span></div>
      <div class="tot"><span>ITBIS (18%)</span><span>${dinero(itbis)}</span></div><div class="tot g"><span>TOTAL</span><span>${dinero(sub - desc + itbis)}</span></div>`;
    $('#pr').disabled = !filas.length || !cfg.caja.abierta;
  };
  $('#q').oninput = pintarCat; $('#ds').onchange = pintarOrden;
  $('#pg').querySelectorAll('button').forEach(b => b.onclick = () => { metodo = b.dataset.m; $('#pg').querySelectorAll('button').forEach(x => x.classList.toggle('p', x === b)); });
  $('#pr').onclick = async () => {
    $('#pr').disabled = true; $('#er').textContent = '';
    try {
      const r = await api('/ventas', { cuerpo: { items: [...carrito].map(([id, cantidad]) => ({ id, cantidad })), comprobanteId: +$('#tc').value, descuentoId: +$('#ds').value, metodo } });
      alert('Factura procesada\nFactura: ' + r.numero + '\nNCF: ' + r.ncf + '\nTotal: ' + dinero(r.total));
      ir('POS');
    } catch (e) { $('#er').textContent = e.message; $('#pr').disabled = false; }
  };
  pintarCat(); pintarOrden();
}

// ---------------------------------------------------------------- arranque
(async () => {
  if (token) {
    try { yo = await api('/yo'); return vistaApp(); } catch (e) { token = null; sessionStorage.removeItem('fjm_token'); }
  }
  vistaLogin();
})();
