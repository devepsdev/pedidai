"""Pasa los documentos de ./docs por la API local (perfil local, IA real) y compara con esperado.json.

Necesita la API en http://localhost:8085 y el usuario de la demo (marketing/demo/acceso.txt).
    python leer.py                 todos
    python leer.py 01 05           solo los casos que empiezan así
"""
import json
import os
import re
import sys
import unicodedata
import urllib.request
import uuid

AQUI = os.path.dirname(os.path.abspath(__file__))
API = 'http://localhost:8085/api'
ACCESO = os.path.join(AQUI, '..', '..', 'marketing', 'demo', 'acceso.txt')
UNIDADES = {'kg': 'kg', 'kgs': 'kg', 'kilo': 'kg', 'kilos': 'kg', 'ud': 'ud', 'uds': 'ud', 'u': 'ud', 'un': 'ud',
            'unidad': 'ud', 'unidades': 'ud', 'pz': 'ud', 'pieza': 'ud', 'piezas': 'ud', 'doc': 'docena',
            'docena': 'docena', 'docenas': 'docena', 'caja': 'caja', 'cajas': 'caja', 'lata': 'lata', 'latas': 'lata',
            'paquete': 'paquete', 'bolsa': 'bolsa', 'bandeja': 'bandeja'}
LOTE = re.compile(r'\d{6,}|\b\d{1,2}/\d{1,2}/\d{2,4}\b|\blote\b|\bfao\b', re.I)


def canon(s):
    s = unicodedata.normalize('NFD', s or '').encode('ascii', 'ignore').decode().lower()
    return re.sub(r'[^a-z0-9]+', ' ', s).strip()


def token():
    acc = open(ACCESO, encoding='utf-8').read()
    datos = {'email': re.search(r'Email: (.+)', acc).group(1).strip(),
             'password': re.search(r'Contraseña: (.+)', acc).group(1).strip()}
    req = urllib.request.Request(API + '/auth/login', method='POST', headers={'Content-Type': 'application/json'},
                                 data=json.dumps(datos).encode())
    return json.loads(urllib.request.urlopen(req).read())['data']['token']


def escanear(ruta, tok):
    nombre = os.path.basename(ruta)
    tipo = 'application/pdf' if nombre.endswith('.pdf') else 'image/jpeg'
    b = '----' + uuid.uuid4().hex
    cuerpo = (f'--{b}\r\nContent-Disposition: form-data; name="image"; filename="{nombre}"\r\n'
              f'Content-Type: {tipo}\r\n\r\n').encode() + open(ruta, 'rb').read() + \
        f'\r\n--{b}\r\nContent-Disposition: form-data; name="kind"\r\n\r\nAUTO\r\n--{b}--\r\n'.encode()
    req = urllib.request.Request(API + '/invoices/scan', method='POST', data=cuerpo,
                                 headers={'Content-Type': f'multipart/form-data; boundary={b}',
                                          'Authorization': 'Bearer ' + tok})
    return json.loads(urllib.request.urlopen(req, timeout=300).read())['data']


def comprobar(esperado, leido):
    fallos = []
    productos = leido['products']
    if len(productos) != len(esperado['lineas']):
        fallos.append(f"{len(productos)} productos leídos, se esperaban {len(esperado['lineas'])}")
    usados = set()
    for e in esperado['lineas']:
        cand = [i for i, p in enumerate(productos) if i not in usados and
                e['generico'] in canon(p.get('genericName'))]
        if not cand:
            fallos.append(f"ningún producto con nombre genérico «{e['generico']}…»")
            continue
        i = cand[0]
        usados.add(i)
        p = productos[i]
        unidad = UNIDADES.get(canon(p.get('unit')), canon(p.get('unit')))
        q, precio = p.get('quantity'), p.get('unitPrice')
        etiqueta = f"«{p.get('name')}»"
        if q is None or abs(q - e['cantidad']) > 0.005:
            fallos.append(f"{etiqueta}: cantidad {q}, debía ser {e['cantidad']}")
        if unidad not in e['unidades']:
            fallos.append(f"{etiqueta}: unidad {p.get('unit')}, debía ser {'/'.join(e['unidades'])}")
        if precio is None or abs(precio - e['precio']) > 0.02:
            fallos.append(f"{etiqueta}: precio {precio}, debía ser {e['precio']}")
        if LOTE.search(p.get('name') or ''):
            fallos.append(f"{etiqueta}: el nombre lleva lote, fecha o zona")
        final = re.search(r'\s(\d+(?:[.,]\d+)?)\s*[a-zA-Z]*\.?$', p.get('name') or '')
        if final and q is not None and abs(float(final.group(1).replace(',', '.')) - q) < 0.001                 and not re.search(r'(pack|caja|x|cl|ml|g|kg|l)\s*\d', (p.get('name') or '').lower()[-12:]):
            fallos.append(f"{etiqueta}: el nombre lleva la cantidad de la línea")
    return fallos


def main():
    esperado = json.load(open(os.path.join(AQUI, 'esperado.json'), encoding='utf-8'))
    filtro = sys.argv[1:]
    tok = token()
    total_lineas = total_fallos = 0
    for archivo, e in esperado.items():
        if filtro and not any(archivo.startswith(f) for f in filtro):
            continue
        leido = escanear(os.path.join(AQUI, 'docs', archivo), tok)
        fallos = comprobar(e, leido)
        total_lineas += len(e['lineas'])
        total_fallos += len(fallos)
        print(f"{'OK  ' if not fallos else 'MAL '} {archivo} — {e['nota']}")
        for p in leido['products']:
            print(f"       {p.get('name')!r}: {p.get('quantity')} {p.get('unit')} x {p.get('unitPrice')}  ({p.get('genericName')})")
        for f in fallos:
            print(f"     ✗ {f}")
    print(f"\n{total_lineas} líneas esperadas, {total_fallos} fallos")


if __name__ == '__main__':
    main()
