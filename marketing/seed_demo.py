"""Datos de la cuenta demo «Bar La Plaza» para grabar los vídeos promocionales (solo en el MySQL local).

Borra la demo anterior y la vuelve a crear, así cada toma empieza igual:
    python seed_demo.py [--pedido]   > demo/seed.sql
    docker exec -i pedidai-mysql-local sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" pedidai_db' < demo/seed.sql

--pedido añade un pedido enviado a Distribuciones López hace 2 días (para el vídeo «¿Te han servido lo que pediste?»).
El hash de la contraseña se lee de demo/hash.txt (fuera del repositorio).
"""
import sys
import uuid
from pathlib import Path

EMAIL = 'demo@pedidai.test'
HASH = (Path(__file__).parent / 'demo' / 'hash.txt').read_text().strip()

# (proveedor, email, [(clave, nombre, nombre genérico, unidad, [(días atrás, precio, cantidad), ...])])
PROVEEDORES = [
    ('Distribuciones López S.L.', 'pedidos@dlopez.test', [
        ('tomate', 'Tomate pera cat. 1', 'tomate pera', 'kg', [(55, 1.60, 30), (40, 1.60, 30), (25, 1.72, 30), (10, 1.85, 30)]),
        ('limon', 'Limón primera', 'limón', 'kg', [(40, 1.75, 10), (10, 1.90, 10)]),
        ('cebolla', 'Cebolla blanca saco 10 kg', 'cebolla', 'saco', [(40, 9.20, 2), (10, 9.60, 2)]),
        ('patata', 'Patata agria', 'patata', 'kg', [(40, 0.72, 40), (10, 0.79, 40)]),
        ('aceite', 'Aceite oliva virgen extra 5 L', 'aceite de oliva virgen extra', 'garrafa', [(40, 30.50, 3), (10, 32.50, 3)]),
        ('agua', 'Agua mineral 1,5 L caja 6 ud', 'agua mineral', 'caja', [(25, 2.90, 8)]),
    ]),
    ('Frutas Martínez', 'pedidos@frutasmartinez.test', [
        ('m_tomate', 'Tomate pera', 'tomate pera', 'kg', [(20, 1.55, 15)]),
        ('m_limon', 'Limón', 'limón', 'kg', [(20, 1.70, 5)]),
        ('m_patata', 'Patata agria', 'patata', 'kg', [(20, 0.68, 20)]),
        ('m_pimiento', 'Pimiento rojo', 'pimiento rojo', 'kg', [(20, 2.10, 5)]),
        ('m_lechuga', 'Lechuga iceberg', 'lechuga', 'ud', [(20, 0.85, 12)]),
    ]),
    ('Bebidas Costa', 'ventas@bebidascosta.test', [
        ('c_agua', 'Agua mineral 1,5 L pack 6', 'agua mineral', 'caja', [(15, 2.60, 6)]),
        ('c_aceite', 'Aceite de oliva virgen extra 5 L', 'aceite de oliva virgen extra', 'garrafa', [(15, 29.90, 1)]),
        ('c_cola', 'Refresco cola lata 24 ud', 'refresco de cola', 'caja', [(15, 13.20, 2)]),
        ('c_cafe', 'Café en grano 1 kg', 'café en grano', 'kg', [(15, 15.90, 2)]),
    ]),
]

# Pedido enviado a López hace 2 días: lo compara el albarán del vídeo
PEDIDO = [('tomate', 20, 1.85), ('limon', 5, 1.90), ('aceite', 2, 32.50), ('agua', 4, 2.90)]


def q(s):
    return "'" + str(s).replace("\\", "\\\\").replace("'", "''") + "'"


def main():
    pedido = '--pedido' in sys.argv
    out = ['SET NAMES utf8mb4;', 'START TRANSACTION;',
           f"SET @c := (SELECT company_id FROM users WHERE email = {q(EMAIL)});",
           'DELETE FROM price_history WHERE company_id = @c;',
           'DELETE oi FROM order_items oi JOIN orders o ON o.id = oi.order_id WHERE o.company_id = @c;',
           'DELETE FROM orders WHERE company_id = @c;',
           'DELETE p FROM products p JOIN suppliers s ON s.id = p.supplier_id WHERE s.company_id = @c;',
           'DELETE FROM suppliers WHERE company_id = @c;',
           'DELETE FROM users WHERE company_id = @c;',
           'DELETE FROM companies WHERE id = @c;',
           f"INSERT INTO companies (uuid, name, tax_id, email, city, status, trial_ends_at) VALUES "
           f"({q(uuid.uuid4())}, 'Bar La Plaza', NULL, {q(EMAIL)}, 'Barcelona', 'ACTIVE', NULL);",
           'SET @c := LAST_INSERT_ID();',
           f"INSERT INTO users (uuid, company_id, email, password, first_name, last_name, role, email_verified, is_active, language) VALUES "
           f"({q(uuid.uuid4())}, @c, {q(EMAIL)}, {q(HASH)}, 'Laura', 'García', 'ADMIN', 1, 1, 'es');",
           'SET @u := LAST_INSERT_ID();']
    for i, (nombre, email, productos) in enumerate(PROVEEDORES):
        out.append(f"INSERT INTO suppliers (uuid, company_id, name, email, is_active) VALUES ({q(uuid.uuid4())}, @c, {q(nombre)}, {q(email)}, 1);")
        out.append(f'SET @s{i} := LAST_INSERT_ID();')
        for clave, nombre_p, generico, unidad, historial in productos:
            actual = historial[-1][1]
            out.append(f"INSERT INTO products (uuid, supplier_id, name, canonical_name, price, unit, is_active) VALUES "
                       f"({q(uuid.uuid4())}, @s{i}, {q(nombre_p)}, {q(generico)}, {actual:.2f}, {q(unidad)}, 1);")
            out.append(f'SET @p_{clave} := LAST_INSERT_ID();')
            for dias, precio, cantidad in historial:
                out.append(f"INSERT INTO price_history (company_id, supplier_id, product_id, unit_price, unit, quantity, document_date, source, document_ref) VALUES "
                           f"(@c, @s{i}, @p_{clave}, {precio:.4f}, {q(unidad)}, {cantidad}, DATE_SUB(CURDATE(), INTERVAL {dias} DAY), 'INVOICE', "
                           f"{q('A-2026-%04d' % (900 - dias))});")
    if pedido:
        total = sum(c * p for _, c, p in PEDIDO)
        out.append(f"INSERT INTO orders (uuid, company_id, supplier_id, user_id, name, status, total_amount, created_at) VALUES "
                   f"({q(uuid.uuid4())}, @c, @s0, @u, 'Pedido semanal', 'SENT', {total:.2f}, NOW() - INTERVAL 2 DAY);")
        out.append('SET @o := LAST_INSERT_ID();')
        for clave, cantidad, precio in PEDIDO:
            out.append(f"INSERT INTO order_items (uuid, order_id, product_id, quantity, unit_price, subtotal) VALUES "
                       f"({q(uuid.uuid4())}, @o, @p_{clave}, {cantidad}, {precio:.2f}, {cantidad * precio:.2f});")
    out.append('COMMIT;')
    sys.stdout.reconfigure(encoding='utf-8')
    print('\n'.join(out))


if __name__ == '__main__':
    main()
