import re, subprocess
sql = open('/home/claude/FarmJM/sql/farmjm.sql', encoding='utf-8').read()
tablas = {}
for m in re.finditer(r"CREATE TABLE (\w+) \((.*?)\n\);", sql, re.S):
    nombre, cuerpo = m.group(1), m.group(2)
    cols, fks = [], []
    for linea in cuerpo.split("\n"):
        l = linea.strip().rstrip(',')
        if not l or l.startswith('--'): continue
        f = re.match(r"FOREIGN KEY \((\w+)\) REFERENCES (\w+)", l)
        if f: fks.append((f.group(1), f.group(2))); continue
        if l.startswith('INDEX'): continue
        c = re.match(r"(\w+)\s+([A-Z]+(?:\(\d+(?:,\d+)?\))?)", l)
        if c: cols.append((c.group(1), c.group(2), 'PRIMARY KEY' in l))
    tablas[nombre] = (cols, fks)
grupos = {
 'Seguridad (NUEVO v2.0)': ['rol','usuario','bitacora_acceso','log_actividad'],
 'Catalogo': ['cliente','proveedor','categoria','producto','medicamento','lote'],
 'Inventario': ['inventario','ajuste_inventario','alerta_vencimiento'],
 'Caja y facturacion': ['caja','arqueo_caja','comprobante_fiscal','descuento','factura','detalle_factura','pago','devolucion'],
 'Recetas y compras': ['medico','receta','detalle_receta','orden_compra','detalle_orden_compra'],
}
color = {'Seguridad (NUEVO v2.0)':'#0F766E','Catalogo':'#475569','Inventario':'#B45309','Caja y facturacion':'#1E3A8A','Recetas y compras':'#6B21A8'}
nuevos = {'clave_hash','totp_secret','totp_activo','intentos_fallidos','bloqueado_hasta','email','ncf','activo'}
def nodo(t, g):
    cols, fks = tablas[t]
    fkc = {a for a, _ in fks}
    filas = f'<TR><TD BGCOLOR="{color[g]}" COLSPAN="2"><FONT COLOR="white"><B>{t}</B></FONT></TD></TR>'
    for n, tipo, pk in cols:
        marca = 'PK ' if pk else ('FK ' if n in fkc else '')
        txt = f'{marca}{n}'
        if n in nuevos and g=='Seguridad (NUEVO v2.0)' or (t=='factura' and n=='ncf') or (n=='activo' and t in ('producto','cliente')):
            txt = f'<B>{txt}</B>'
        filas += f'<TR><TD ALIGN="LEFT"><FONT POINT-SIZE="9">{txt}</FONT></TD><TD ALIGN="LEFT"><FONT POINT-SIZE="8" COLOR="#475569">{tipo}</FONT></TD></TR>'
    return f'  {t} [label=<<TABLE BORDER="1" CELLBORDER="0" CELLSPACING="0" CELLPADDING="2" BGCOLOR="white">{filas}</TABLE>>];\n'
dot = 'digraph ER {\n  rankdir=LR; nodesep=0.25; ranksep=0.9; splines=true; node [shape=plaintext, fontname="Helvetica"]; edge [color="#64748B", arrowsize=0.6];\n'
for g, ts in grupos.items():
    dot += f'  subgraph cluster_{abs(hash(g))%9999} {{ label="{g}"; fontname="Helvetica-Bold"; fontcolor="{color[g]}"; color="{color[g]}"; style=rounded;\n'
    for t in ts: dot += nodo(t, g)
    dot += '  }\n'
for t, (cols, fks) in tablas.items():
    for a, ref in fks: dot += f'  {t} -> {ref};\n'
dot += '}\n'
open('er.dot','w').write(dot)
subprocess.run(['dot','-Tpng','-Gdpi=110','er.dot','-o','img/er.png'], check=True)
print(len(tablas),'tablas', sum(len(f) for _,f in tablas.values()),'relaciones')
