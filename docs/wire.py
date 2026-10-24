import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.patches import Rectangle, FancyBboxPatch

LINE = '#475569'; FILL = '#F1F5F9'; DARK = '#CBD5E1'; ACC = '#99D6CF'

SIZES = {'Escritorio 1366x768': (1366, 768, 'esc'), 'Tableta 1024x768': (1024, 768, 'tab'), 'Telefono 412x915': (412, 915, 'tel')}

def box(ax, x, y, w, h, label='', fill=FILL, fs=8, bold=False, align='center'):
    ax.add_patch(Rectangle((x, y), w, h, fc=fill, ec=LINE, lw=0.8))
    if label:
        ha = 'center' if align == 'center' else 'left'
        tx = x + w/2 if align == 'center' else x + 6
        ax.text(tx, y + h/2, label, ha=ha, va='center', fontsize=fs, color='white' if fill in ('#14233A','#0F766E','#0B1729') else '#0F172A', fontweight='bold' if bold else 'normal')

def frame(ax, W, H, title):
    ax.set_xlim(0, W); ax.set_ylim(H, 0); ax.set_aspect('equal'); ax.axis('off')
    ax.add_patch(Rectangle((0, 0), W, H, fc='white', ec='#0F172A', lw=1.6))
    ax.set_title(title, fontsize=9, fontweight='bold', color='#0F172A')

def shell(ax, W, H, modo, nombre):
    """Barra superior + menu segun el modo. Devuelve el area de contenido (x, y, w, h)."""
    top = 46 if modo != 'tel' else 40
    box(ax, 0, 0, W, top, '', fill='white')
    ax.text(10, top/2, nombre, fontsize=8, va='center', fontweight='bold')
    if modo != 'tel': ax.text(W-10, top/2, 'MySQL conectado | Rol', fontsize=7, va='center', ha='right')
    if modo == 'esc':
        box(ax, 0, top, 190, H-top, '', fill='#0B1729')
        for i, t in enumerate(['Dashboard', 'POS', 'Inventario', 'Clientes', 'Caja', 'Reportes', 'Usuarios', 'Auditoria']):
            box(ax, 12, top+70+i*34, 166, 26, t, fill='#14233A' if i else '#0F766E', fs=7)
        ax.text(14, top+24, 'FARMACIA JM', color='white', fontsize=8, fontweight='bold', va='center')
        return (200, top+10, W-210, H-top-20)
    if modo == 'tab':
        box(ax, 0, top, 56, H-top, '', fill='#0B1729')
        for i in range(8): box(ax, 9, top+60+i*34, 38, 26, '', fill='#14233A' if i else '#0F766E')
        return (66, top+10, W-76, H-top-20)
    bar = 46
    box(ax, 0, H-bar, W, bar, '', fill='#0B1729')
    for i in range(6): box(ax, 10+i*66, H-bar+9, 52, 28, '', fill='#14233A' if i else '#0F766E')
    return (8, top+8, W-16, H-top-bar-16)

def login(ax, W, H, modo):
    box(ax, 0, 0, W, H, '', fill='#0B1729')
    cw = min(400, W-30); ch = 330 if modo != 'tel' else 360
    x = (W-cw)/2; y = (H-ch)/2
    box(ax, x, y, cw, ch, '', fill='white')
    ax.text(W/2, y+34, 'FARMACIA JM', ha='center', fontsize=10, fontweight='bold')
    ax.text(W/2, y+54, 'Sistema Integral - Release 2.0', ha='center', fontsize=7, color=LINE)
    box(ax, x+22, y+86, cw-44, 34, 'Usuario', align='left', fs=7)
    box(ax, x+22, y+134, cw-44, 34, 'Contrasena', align='left', fs=7)
    box(ax, x+22, y+190, cw-44, 38, 'Iniciar sesion', fill='#0F766E', fs=8, bold=True)
    ax.text(W/2, y+262, 'Paso 2: codigo 2FA de 6 digitos (pantalla aparte)', ha='center', fontsize=6.5, color=LINE)

def dashboard(ax, W, H, modo):
    cx, cy, cw, ch = shell(ax, W, H, modo, 'Dashboard')
    cols = {'esc': 4, 'tab': 2, 'tel': 1}[modo]
    kh = 54; gap = 10; n = 6 if modo != 'tel' else 4
    kw = (cw - gap*(cols-1))/cols
    for i in range(n):
        box(ax, cx+(i % cols)*(kw+gap), cy+(i//cols)*(kh+gap), kw, kh, f'KPI {i+1}', fs=7, fill='white')
    rows = (n + cols - 1)//cols
    y = cy + rows*(kh+gap) + 4
    if modo == 'tel':
        box(ax, cx, y, cw, 150, 'Grafico de barras (ventas 7 dias)', fs=7, fill=DARK)
        box(ax, cx, y+160, cw, 150, 'Grafico de pastel (categorias)', fs=7, fill=DARK)
        box(ax, cx, y+320, cw, 110, 'Actividad reciente (columnas principales)', fs=7, fill='white')
    else:
        gw = (cw-gap)/2
        gh = 170 if modo == 'esc' else 150
        box(ax, cx, y, gw, gh, 'BarChart: ventas 7 dias', fs=7, fill=DARK)
        box(ax, cx+gw+gap, y, gw, gh, 'PieChart: unidades por categoria', fs=7, fill=DARK)
        box(ax, cx, y+gh+gap, cw, max(60, cy+ch-(y+gh+gap)), 'Tabla: actividad reciente', fs=7, fill='white')

def crud(ax, W, H, modo):
    cx, cy, cw, ch = shell(ax, W, H, modo, 'Inventario & Lotes')
    ax.text(cx, cy+12, 'Inventario & Lotes', fontsize=9, fontweight='bold', va='center')
    if modo == 'tel':
        box(ax, cx, cy+30, cw, 34, '+ Nuevo', fill='#0F766E', fs=7)
        box(ax, cx, cy+70, (cw-8)/2, 30, 'Editar', fs=7); box(ax, cx+(cw+8)/2, cy+70, (cw-8)/2, 30, 'Eliminar', fs=7, fill='#FCA5A5')
        ty = cy+112
        headers = ['Medicamento', 'Precio', 'Stock']
    else:
        box(ax, cx, cy+30, 130, 32, '+ Nuevo', fill='#0F766E', fs=7); box(ax, cx+140, cy+30, 80, 32, 'Editar', fs=7); box(ax, cx+230, cy+30, 80, 32, 'Eliminar', fs=7, fill='#FCA5A5')
        ty = cy+76
        headers = ['Codigo', 'Medicamento', 'Precio', 'Stock', 'Lote', 'Vence', 'Estado'] if modo == 'esc' else ['Medicamento', 'Precio', 'Stock', 'Vence', 'Estado']
    cwid = cw/len(headers)
    for j, h in enumerate(headers): box(ax, cx+j*cwid, ty, cwid, 24, h, fill=DARK, fs=7, bold=True)
    for r in range(1, 8 if modo != 'tel' else 9):
        for j in range(len(headers)): box(ax, cx+j*cwid, ty+r*24, cwid, 24, '', fill='white' if r % 2 else FILL)
    # modal (solo escritorio/tableta)
    if modo != 'tel':
        mw, mh = 250, 150
        mx, my = cx+cw-mw-20, ty+60
        box(ax, mx, my, mw, mh, '', fill='white'); box(ax, mx, my, mw, 22, 'Modal: Nuevo / Editar', fill='#0F766E', fs=7)
        for i in range(3): box(ax, mx+12, my+34+i*30, mw-24, 22, 'campo', fs=6, align='left')
        box(ax, mx+mw-84, my+mh-30, 72, 22, 'Guardar', fill='#0F766E', fs=6)
    else:
        box(ax, cx+8, ty+120, cw-16, 120, 'Modal a pantalla completa:\nformulario + Guardar / Cancelar', fill='white', fs=7)

def figura(fn, titulo, nombre):
    fig, axes = plt.subplots(1, 3, figsize=(16, 5.6), gridspec_kw={'width_ratios': [1366, 1024, 412]})
    for ax, (t, (W, H, modo)) in zip(axes, SIZES.items()):
        frame(ax, W, H, t)
        fn(ax, W, H, modo)
    fig.suptitle(titulo, fontsize=12, fontweight='bold', color='#0F766E')
    fig.tight_layout()
    fig.savefig(f'img/{nombre}.png', dpi=130)
    plt.close(fig)

figura(login, 'Wireframe 1 - Inicio de sesion (1.er factor)', 'wf_login')
figura(dashboard, 'Wireframe 2 - Dashboard', 'wf_dashboard')
figura(crud, 'Wireframe 3 - Modulo CRUD (Inventario) con modal', 'wf_crud')
print('ok')
