"""Albaranes de pescado inventados para probar la lectura con la IA real.

Genera un HTML por caso en ./docs y esperado.json con el resultado correcto de cada línea.
    python casos.py      →  después  node render.mjs  (JPG/PDF)  y  python leer.py  (con la API local)
"""
import json
import os

AQUI = os.path.dirname(os.path.abspath(__file__))
DOCS = os.path.join(AQUI, 'docs')


def linea(nombre, cantidad, unidad, precio, *alternativas):
    """Lo que debe salir: nombre genérico (castellano), cantidad, unidades aceptadas y precio unitario sin IVA."""
    return {'generico': nombre, 'cantidad': cantidad, 'unidades': [unidad, *alternativas], 'precio': precio}


CASOS = [
    {
        'id': '01-lonja-cajas-kilos',
        'nota': 'Columnas Cajas y Kg: la cantidad son los kilos',
        'proveedor': 'PESCADOS DEL CANTÁBRICO S.L.', 'pie': 'Lonja de Santoña · Puesto 14',
        'columnas': ['Artículo', 'Cajas', 'Kg', '€/Kg', 'Importe'],
        'filas': [
            ['MERLUZA PINCHO 2-3 KG', '2', '18,500', '12,40', '229,40'],
            ['RAPE BLANCO S/C', '1', '7,250', '15,90', '115,28'],
            ['SARDINA', '3', '21,000', '3,20', '67,20'],
            ['BOQUERON', '1', '6,800', '5,10', '34,68'],
        ],
        'esperado': [linea('merluza', 18.5, 'kg', 12.40), linea('rape', 7.25, 'kg', 15.90),
                     linea('sardina', 21, 'kg', 3.20), linea('boquer', 6.8, 'kg', 5.10)],
    },
    {
        'id': '02-sin-unidad',
        'nota': 'Sin columna de unidad, cantidades enteras y con decimales',
        'proveedor': 'MARISCOS ES PORTITXOL', 'pie': 'Mercapalma, nave 3',
        'columnas': ['Descripción', 'Cantidad', 'Precio', 'Importe'],
        'filas': [
            ['CALAMAR NACIONAL', '10', '14,50', '145,00'],
            ['RODABALLO 1/2', '3,450', '16,80', '57,96'],
            ['GAMBA ROJA M', '2', '38,00', '76,00'],
            ['PULPO GALLEGO 2/3', '4', '13,90', '55,60'],
            ['PEREJIL MANOJO', '5', '0,60', '3,00'],
        ],
        'esperado': [linea('calamar', 10, 'kg', 14.50), linea('rodaballo', 3.45, 'kg', 16.80),
                     linea('gamba', 2, 'kg', 38.00), linea('pulpo', 4, 'kg', 13.90),
                     linea('perejil', 5, 'ud', 0.60, 'paquete')],
    },
    {
        'id': '03-unidad-escrita',
        'nota': 'Columna de unidad con kg, ud, docena y caja',
        'proveedor': 'OSTRAS Y MARISCOS RÍAS BAIXAS', 'pie': 'Vilanova de Arousa',
        'columnas': ['Artículo', 'Cant.', 'Ud.', 'Precio', 'Importe'],
        'filas': [
            ['OSTRA GALLEGA Nº3', '2', 'DOC', '14,40', '28,80'],
            ['LUBINA 600/800', '5,200', 'KG', '11,20', '58,24'],
            ['NAVAJA', '3', 'KG', '19,50', '58,50'],
            ['ERIZO DE MAR', '30', 'UD', '0,90', '27,00'],
            ['GAMBA BLANCA CONGELADA 2 KG', '4', 'CAJA', '21,00', '84,00'],
        ],
        'esperado': [linea('ostra', 2, 'docena', 14.40), linea('lubina', 5.2, 'kg', 11.20),
                     linea('navaja', 3, 'kg', 19.50), linea('erizo', 30, 'ud', 0.90),
                     linea('gamba', 4, 'caja', 21.00)],
    },
    {
        'id': '04-raciones',
        'nota': 'Pescado de ración por piezas, con la unidad dentro de la cantidad',
        'proveedor': 'ACUICULTURA MEDITERRÁNEA S.A.', 'pie': 'Burriana (Castellón)',
        'columnas': ['Producto', 'Unid.', 'Precio', 'Total'],
        'filas': [
            ['DORADA RACION 400/600 GR', '20 uds', '4,20', '84,00'],
            ['LUBINA RACION 400/600 GR', '15 uds', '4,60', '69,00'],
            ['SALMON NORUEGO ENTERO 5/6', '2 uds', '52,00', '104,00'],
            ['LIMÓN', '2 kg', '1,80', '3,60'],
        ],
        'esperado': [linea('dorada', 20, 'ud', 4.20), linea('lubina', 15, 'ud', 4.60),
                     linea('salm', 2, 'ud', 52.00), linea('lim', 2, 'kg', 1.80)],
    },
    {
        'id': '05-trazabilidad',
        'nota': 'Debajo de cada línea, nombre científico, zona FAO y lote: no son productos',
        'proveedor': 'FRIGORÍFICOS DEL NORTE S.L.', 'pie': 'Mercabilbao · Pabellón B',
        'columnas': ['Concepto', 'Kilos', 'Precio', 'Importe'],
        'filas': [
            ['MERLUZA DE PINCHO', '6,350', '13,80', '87,63',
             'Merluccius merluccius · FAO 27.8.c · Arrastre de fondo · Lote 26R1043-77 · Fresco'],
            ['BESUGO', '2,100', '24,50', '51,45', 'Pagellus bogaraveo · FAO 27.9.a · Palangre · Lote 26B0981-12'],
            ['CABALLA', '8,000', '2,90', '23,20', 'Scomber scombrus · FAO 27.8.b · Cerco · Lote 26C2210-03'],
            ['MEJILLÓN GALLEGO', '10,000', '1,95', '19,50', 'Mytilus galloprovincialis · Cultivo · Lote M-2610-445'],
        ],
        'esperado': [linea('merluza', 6.35, 'kg', 13.80), linea('besugo', 2.1, 'kg', 24.50),
                     linea('caballa', 8, 'kg', 2.90), linea('mejill', 10, 'kg', 1.95)],
    },
    {
        'id': '06-catalan',
        'nota': 'Albarán en catalán',
        'proveedor': 'PEIX FRESC DE LA BARCELONETA', 'pie': 'Mercabarna · Mercat Central del Peix',
        'titulo': 'ALBARÀ', 'columnas': ['Article', 'Quantitat', 'Unitat', 'Preu', 'Import'],
        'filas': [
            ['LLOBARRO 600/800', '3,400', 'kg', '11,90', '40,46'],
            ['ORADA 400/600', '2,750', 'kg', '9,40', '25,85'],
            ['SÍPIA NETA', '4,000', 'kg', '12,60', '50,40'],
            ['MUSCLO DEL DELTA', '8,000', 'kg', '2,40', '19,20'],
        ],
        'esperado': [linea('lubina', 3.4, 'kg', 11.90), linea('dorada', 2.75, 'kg', 9.40),
                     linea('sepia', 4, 'kg', 12.60), linea('mejill', 8, 'kg', 2.40)],
    },
    {
        'id': '07-piezas-y-peso',
        'nota': 'Piezas y peso en columnas distintas: se cobra por kilo',
        'proveedor': 'COCEDERO DE MARISCOS O GROVE', 'pie': 'O Grove (Pontevedra)',
        'columnas': ['Artículo', 'Piezas', 'Peso (kg)', '€/kg', 'Importe'],
        'filas': [
            ['PULPO COCIDO', '3', '4,250', '18,90', '80,33'],
            ['BOGAVANTE AZUL', '2', '1,180', '42,00', '49,56'],
            ['CENTOLLO', '4', '3,600', '16,50', '59,40'],
            ['LANGOSTA', '1', '0,850', '58,00', '49,30'],
        ],
        'esperado': [linea('pulpo', 4.25, 'kg', 18.90), linea('bogavante', 1.18, 'kg', 42.00),
                     linea('centollo', 3.6, 'kg', 16.50), linea('langosta', 0.85, 'kg', 58.00)],
    },
    {
        'id': '08-congelado-conservas',
        'nota': 'Congelado y conservas: se cuentan por envase, con código de artículo',
        'proveedor': 'CONGELADOS LEVANTE S.L.', 'pie': 'Polígono El Oliveral · Riba-roja',
        'columnas': ['Código', 'Descripción', 'Cant.', 'Precio', 'Importe'],
        'filas': [
            ['4471', 'GAMBÓN ARGENTINO 10/20 CAJA 2 KG', '3', '24,90', '74,70'],
            ['2210', 'ATÚN CLARO ACEITE LATA 1 KG', '6', '9,80', '58,80'],
            ['2302', 'ANCHOAS CANTÁBRICO 50 G', '24', '3,10', '74,40'],
            ['5120', 'LANGOSTINO COCIDO BOLSA 1 KG', '5', '11,40', '57,00'],
            ['5133', 'CALAMAR TROCEADO CONGELADO 1 KG', '8', '6,90', '55,20'],
        ],
        'esperado': [linea('gamb', 3, 'caja', 24.90, 'paquete', 'ud'),
                     linea('at', 6, 'lata', 9.80, 'ud', 'kg'),
                     linea('anchoa', 24, 'lata', 3.10, 'ud'),
                     linea('langostino', 5, 'paquete', 11.40, 'ud', 'kg', 'bolsa'),
                     linea('calamar', 8, 'paquete', 6.90, 'ud', 'kg', 'bolsa')],
    },
    {
        'id': '09-tique-foto',
        'nota': 'Tique estrecho de báscula, girado y algo borroso',
        'proveedor': 'PESCADERÍA LA RIBERA', 'estilo': 'tique',
        'filas': [
            ['SEPIA', '2,300', '10,50', '24,15'],
            ['GALLO', '1,850', '19,90', '36,82'],
            ['ALMEJA FINA', '1,000', '28,00', '28,00'],
            ['PESCADILLA', '3,400', '6,40', '21,76'],
        ],
        # «IVA 10% incluido»: el precio que se guarda es sin IVA
        'esperado': [linea('sepia', 2.3, 'kg', 9.55), linea('gallo', 1.85, 'kg', 18.09),
                     linea('almeja', 1, 'kg', 25.45), linea('pescadilla', 3.4, 'kg', 5.82)],
    },
    {
        'id': '10-factura-descuento',
        'nota': 'Factura en PDF con descuento por línea e IVA',
        'proveedor': 'DISTRIBUCIONES PESQUERAS DEL SUR S.L.', 'pie': 'Mercamálaga · CIF B29876543',
        'titulo': 'FACTURA', 'pdf': True,
        'columnas': ['Descripción', 'Cantidad', 'Precio', 'Dto %', 'Importe'],
        'filas': [
            ['MERO', '1,950', '26,00', '5', '48,17'],
            ['CORVINA', '4,300', '14,20', '0', '61,06'],
            ['CIGALA Nº2', '1,500', '48,00', '10', '64,80'],
            ['ATÚN ROJO LOMO', '2,600', '32,50', '0', '84,50'],
        ],
        'esperado': [linea('mero', 1.95, 'kg', 24.70), linea('corvina', 4.3, 'kg', 14.20),
                     linea('cigala', 1.5, 'kg', 43.20), linea('at', 2.6, 'kg', 32.50)],
    },
    {
        'id': '11-carniceria',
        'nota': 'Carne con piezas y kilos; hamburguesas por unidad',
        'proveedor': 'CÁRNICAS HERMANOS GIL S.L.', 'pie': 'Mercamadrid · Nave de carnes 4',
        'columnas': ['Artículo', 'Piezas', 'Kg', '€/Kg', 'Importe'],
        'filas': [
            ['SOLOMILLO TERNERA', '2', '3,150', '32,00', '100,80'],
            ['CHULETÓN VACA MADURADA', '4', '4,800', '38,50', '184,80'],
            ['POLLO ENTERO', '6', '11,400', '3,40', '38,76'],
            ['HAMBURGUESA VACUNO 150 G', '40', '—', '0,95', '38,00'],
        ],
        'esperado': [linea('solomillo', 3.15, 'kg', 32.00), linea('chulet', 4.8, 'kg', 38.50),
                     linea('pollo', 11.4, 'kg', 3.40), linea('hamburguesa', 40, 'ud', 0.95)],
    },
    {
        'id': '12-fruteria',
        'nota': 'Frutería sin columna de unidad y comas perdidas por el OCR (en verdura sin unidad, kg o ud valen)',
        'proveedor': 'FRUTAS Y VERDURAS LA HUERTA', 'pie': 'Mercavalència · Puesto 87',
        'columnas': ['Descripción', 'Cantidad', 'Precio', 'Importe'],
        'filas': [
            ['TOMATE PERA CAT.1', '15', '1,95', '29,25'],
            ['LECHUGA ICEBERG', '12', '0,85', '10,20'],
            ['PATATA AGRIA SACO 25 KG', '2', '14,00', '28,00'],
            ['LIMÓN', '5,500', '1,60', '8,80'],
        ],
        'esperado': [linea('tomate', 15, 'kg', 1.95, 'ud'), linea('lechuga', 12, 'ud', 0.85, 'kg'),
                     linea('patata', 2, 'saco', 14.00), linea('lim', 5.5, 'kg', 1.60)],
    },
    {
        'id': '13-mayorista-mixto',
        'nota': 'Mayorista con bebida, aceite, ahumado, congelado y huevos, con columna de unidad',
        'proveedor': 'CASH HOSTELERÍA MEDITERRÁNEO', 'pie': 'Polígono Can Valero · Palma',
        'columnas': ['Código', 'Artículo', 'Ud.', 'Cant.', 'Precio', 'Importe'],
        'filas': [
            ['1201', 'CERVEZA ESTRELLA DAMM 33CL PACK 24', 'CJ', '5', '18,90', '94,50'],
            ['3310', 'ACEITE OLIVA VIRGEN EXTRA 5 L', 'UD', '3', '31,50', '94,50'],
            ['5540', 'SALMÓN AHUMADO LONCHAS 200 G', 'UD', '10', '4,90', '49,00'],
            ['5612', 'MERLUZA FILETE CONGELADA', 'KG', '4,000', '7,20', '28,80'],
            ['7001', 'HUEVOS CAMPEROS L', 'DOC', '6', '2,80', '16,80'],
        ],
        'esperado': [linea('cerveza', 5, 'caja', 18.90), linea('aceite', 3, 'garrafa', 31.50, 'botella', 'ud'),
                     linea('salm', 10, 'ud', 4.90, 'paquete'), linea('merluza', 4, 'kg', 7.20),
                     linea('huevo', 6, 'docena', 2.80)],
    },
    {
        'id': '14-como-iscapalma',
        'nota': 'Columna de unidad casi vacía (solo el mejillón dice Kg), como el albarán real de La mejillonera',
        'proveedor': 'PESCADOS SA DRAGONERA', 'pie': 'Mercapalma, nave 9',
        'columnas': ['Descripción', 'Cantidad', 'Ud.', 'Precio', 'Importe'],
        'filas': [
            ['MEJILLON SELECCION', '6,000', 'Kg', '3,15', '18,90'],
            ['SALMON NORUEGO 4/5', '21,300', '', '9,80', '208,74'],
            ['GALLO SAN PEDRO', '10', '', '21,90', '219,00'],
            ['DORADA 04/06', '1,000', '', '9,25', '9,25'],
            ['LUBINA 04/06', '1,100', '', '10,30', '11,33'],
        ],
        'esperado': [linea('mejill', 6, 'kg', 3.15), linea('salm', 21.3, 'kg', 9.80),
                     linea('gallo', 10, 'kg', 21.90), linea('dorada', 1, 'kg', 9.25),
                     linea('lubina', 1.1, 'kg', 10.30)],
    },
]

# Documentos de la demo de marketing: no son de pescado, pero no deben empeorar
DEMO = {
    '../../../marketing/demo/factura-bebidas-costa.pdf': {
        'nota': 'Demo: factura de bebidas (cajas, garrafa y kg)',
        'lineas': [linea('agua', 6, 'caja', 2.75, 'paquete'), linea('refresco', 2, 'caja', 13.20, 'paquete'),
                   linea('caf', 2, 'kg', 16.50, 'paquete'), linea('aceite', 1, 'garrafa', 29.90, 'botella')],
    },
    '../../../marketing/demo/albaran-lopez.jpg': {
        'nota': 'Demo: albarán de verdura con la cantidad junto a la descripción',
        'lineas': [linea('tomate', 15, 'kg', 1.95), linea('lim', 5, 'kg', 1.90), linea('cebolla', 1, 'saco', 9.60),
                   linea('patata', 20, 'kg', 0.79), linea('aceite', 2, 'garrafa', 33.90, 'botella')],
    },
}

ESTILO = """<style>
body{font-family:Arial,Helvetica,sans-serif;width:820px;padding:28px;font-size:15px;color:#111;background:#fff}
h1{font-size:21px;margin:0}.pie{color:#444;font-size:13px}
table{width:100%;border-collapse:collapse;margin-top:18px}td,th{border:1px solid #999;padding:6px 8px;text-align:left}
th{background:#eee}.r{text-align:right}.sub{color:#666;font-size:11px;border-top:none;padding-top:0}
.tot{margin-top:12px;text-align:right}
</style>"""

ESTILO_TIQUE = """<style>
body{margin:0;background:#cfcac0;width:640px;height:760px;display:flex;align-items:center;justify-content:center}
.t{background:#fbfaf6;width:330px;padding:18px 16px;font-family:'Courier New',monospace;font-size:14px;color:#222;
transform:rotate(-2.5deg);filter:blur(.45px);box-shadow:0 4px 18px rgba(0,0,0,.25)}
.c{text-align:center}.l{display:flex;justify-content:space-between}hr{border:0;border-top:1px dashed #555}
</style>"""


def num(s):
    return float(s.replace('.', '').replace(',', '.'))


def html(caso):
    if caso.get('estilo') == 'tique':
        lineas = ''.join(f"<div>{f[0]}</div><div class='l'><span>{f[1]} kg x {f[2]}</span><span>{f[3]}</span></div>"
                         for f in caso['filas'])
        total = sum(num(f[3]) for f in caso['filas'])
        return (f"<!doctype html><html><head><meta charset='utf-8'>{ESTILO_TIQUE}</head><body><div class='t'>"
                f"<div class='c'><b>{caso['proveedor']}</b><br>C/ Mayor 12 · Tel. 961 234 567</div><hr>"
                f"<div>TIQUE 0004417 · 21/10/2026 10:42</div><hr>{lineas}<hr>"
                f"<div class='l'><b>TOTAL</b><b>{f'{total:.2f}'.replace('.', ',')} €</b></div><div>IVA 10% incluido</div></div></body></html>"
                ).replace('.', ',', 0)
    cab = ''.join(f"<th{' class=r' if i > 0 and c not in ('Ud.', 'Unitat', 'Descripción') else ''}>{c}</th>"
                  for i, c in enumerate(caso['columnas']))
    filas = ''
    n = len(caso['columnas'])
    for f in caso['filas']:
        celdas = f[:n]
        filas += '<tr>' + ''.join(
            f"<td{' class=r' if i > 0 and caso['columnas'][i] not in ('Ud.', 'Unitat', 'Descripción') else ''}>{c}</td>"
            for i, c in enumerate(celdas)) + '</tr>'
        if len(f) > n:
            filas += f"<tr><td class='sub' colspan='{n}'>{f[n]}</td></tr>"
    base = sum(num(f[n - 1]) for f in caso['filas'])
    iva = round(base * 0.10, 2)
    return (f"<!doctype html><html><head><meta charset='utf-8'>{ESTILO}</head><body>"
            f"<h1>{caso['proveedor']}</h1><div class='pie'>{caso.get('pie', '')}</div>"
            f"<div style='margin-top:12px'><b>{caso.get('titulo', 'ALBARÁN')} Nº</b> 26-{abs(hash(caso['id'])) % 90000 + 10000}"
            f" &nbsp; <b>Fecha:</b> 21/10/2026</div><div>Cliente: RESTAURANTE DE PRUEBA</div>"
            f"<table><tr>{cab}</tr>{filas}</table>"
            f"<div class='tot'>Base imponible {base:.2f} · IVA 10% {iva:.2f} · <b>TOTAL {base + iva:.2f} €</b></div>"
            "</body></html>").replace(f"{base:.2f}", f"{base:.2f}".replace('.', ','))


def main():
    os.makedirs(DOCS, exist_ok=True)
    esperado = {}
    for caso in CASOS:
        ext = 'pdf' if caso.get('pdf') else 'jpg'
        with open(os.path.join(DOCS, caso['id'] + '.html'), 'w', encoding='utf-8') as f:
            f.write(html(caso))
        esperado[f"{caso['id']}.{ext}"] = {'nota': caso['nota'], 'lineas': caso['esperado']}
    esperado.update(DEMO)
    with open(os.path.join(AQUI, 'esperado.json'), 'w', encoding='utf-8') as f:
        json.dump(esperado, f, ensure_ascii=False, indent=1)
    print(f'{len(CASOS)} casos en {DOCS}')


if __name__ == '__main__':
    main()
