package com.pedidai.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${app.frontend.url:https://pedidai.es}")
    private String frontendUrl;

    @Bean
    public OpenAPI pedidaiAPI() {
        // Configurar l'esquema de seguretat JWT
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("Introdueix el token JWT (sense 'Bearer ')");

        // Requeriment de seguretat
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("Bearer Authentication");

        // Servidor local
        Server localServer = new Server()
                .url("http://localhost:8085")
                .description("Servidor de desenvolupament local");

        // Servidor de producció
        Server productionServer = new Server()
                .url(frontendUrl)
                .description("Servidor de producció");

        // Informació de l'API
        Info info = new Info()
                .title("PedidAI API")
                .version("1.0")
                .description("""
                    API REST de PedidAI - Plataforma inteligente de pedidos.

                    ## Característiques principals:
                    - 🏢 Gestió completa d'empreses
                    - 👥 Gestió d'usuaris amb rols (ADMIN, USER)
                    - 🚚 Gestió integral de proveïdors
                    - 📦 Catàleg de productes amb comparativa de preus entre proveïdors
                    - 🛒 Sistema de comandes amb notificacions i anàlisi de consum
                    - 🤖 Escaneig intel·ligent de factures amb IA (DeepSeek Vision)
                    - 📊 Sistema de reports i estadístiques
                    - 🔐 Autenticació JWT
                    - ✉️ Verificació d'email
                    - 🔑 Recuperació de contrasenya
                    - 🔍 Cerca avançada amb filtres
                    - 📄 Paginació i ordenació
                    - 📑 Generació de PDF per reports

                    ## Autenticació:
                    1. Fes login a `/api/auth/login` amb email i contrasenya
                    2. Copia el token JWT de la resposta
                    3. Fes clic al botó "Authorize" (🔓) a dalt a la dreta
                    4. Enganxa el token (sense 'Bearer ') i fes clic a "Authorize"
                    5. Ara pots provar tots els endpoints protegits

                    ## Flux típic:
                    1. **Autenticació:**
                       - Registre: `POST /api/companies/register`
                       - Verificació email: `POST /api/auth/verify-email`
                       - Reenviar verificació: `POST /api/auth/resend-verification`
                       - Login: `POST /api/auth/login`
                       - Oblidar contrasenya: `POST /api/auth/forgot-password`
                       - Restablir contrasenya: `POST /api/auth/reset-password`
                    2. **Gestió empresa:**
                       - Consultar: `GET /api/companies`
                       - Actualitzar: `PUT /api/companies`
                    3. **Gestió usuaris (només ADMIN):**
                       - Llistar tots: `GET /api/users`
                       - Consultar: `GET /api/users/{uuid}`
                       - Crear: `POST /api/users`
                       - Actualitzar: `PUT /api/users/{uuid}`
                       - Canviar estat: `PATCH /api/users/{uuid}/status`
                       - Canviar contrasenya: `PATCH /api/users/{uuid}/change-password`
                       - Eliminar: `DELETE /api/users/{uuid}`
                       - Cerca bàsica: `GET /api/users/search?searchText=text`
                       - Cerca avançada: `GET /api/users/filter?email=...&firstName=...`
                    4. **Gestió proveïdors:**
                       - Llistar: `GET /api/suppliers`
                       - Crear: `POST /api/suppliers`
                       - Consultar: `GET /api/suppliers/{uuid}`
                       - Actualitzar: `PUT /api/suppliers/{uuid}`
                       - Canviar estat: `PATCH /api/suppliers/{uuid}/status`
                       - Cerca bàsica: `GET /api/suppliers/search?searchText=text`
                       - Cerca avançada: `GET /api/suppliers/filter?name=...&email=...`
                    5. **Gestió productes:**
                       - Llistar per empresa: `GET /api/products`
                       - Crear: `POST /api/products/create`
                       - Consultar: `GET /api/products/{uuid}`
                       - Actualitzar: `PUT /api/products/{uuid}`
                       - Desactivar: `PATCH /api/products/deactivate/{uuid}`
                       - Cerca: `GET /api/products/search?supplierUuid=...`
                       - Filtre avançat: `GET /api/products/filter?category=...`
                       - Pujar imatge: `POST /api/products/upload/{productUuid}`
                       - Imatge temporal: `POST /api/products/upload-temp`
                       - **[NOU] Comparativa de preus:** `GET /api/products/compare-prices?productName=...&days=90`
                    6. **Gestió comandes:**
                       - Crear: `POST /api/orders/create`
                       - Consultar: `GET /api/orders/{uuid}`
                       - Llistar/Filtrar: `GET /api/orders/filter?name=...&createdAfter=...`
                       - Actualitzar: `PUT /api/orders/{uuid}`
                       - Enviar al proveïdor: `POST /api/orders/{uuid}/send`
                       - Eliminar: `PATCH /api/orders/delete/{uuid}`
                       - **[NOU] Anàlisi de consum:** `GET /api/orders/consumption-analysis?days=90`
                    7. **[NOU] Escaneig de factures amb IA:**
                       - Escanejar factura: `POST /api/invoices/scan` (multipart: image + supplierUuid opcional)
                       - Confirmar factura: `POST /api/invoices/confirm`
                    8. **Reports i estadístiques:**
                       - Dashboard: `GET /api/reports/dashboard`
                       - Report global: `GET /api/reports/global?startDate=...&endDate=...`
                       - Descarregar PDF: `GET /api/reports/global/pdf?startDate=...&endDate=...`

                    ## Paginació i ordenació:
                    Tots els endpoints de llistat suporten:
                    - `page`: número de pàgina (default: 0)
                    - `size`: elements per pàgina (default: 10)
                    - `sortBy`: camp d'ordenació (default: "name")
                    - `sortDir`: direcció (asc/desc, default: "asc")

                    ## Cerca i filtres:
                    - **Cerca bàsica:** cerca en múltiples camps simultàniament
                    - **Cerca avançada:** filtres específics per cada camp
                    - Tots els filtres de text són case-insensitive i accepten coincidències parcials
                    """)
                .contact(new Contact()
                        .name("Equip PedidAI")
                        .email("devepsdev@gmail.com")
                        .url(frontendUrl))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0.html"));

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer, productionServer))
                .addSecurityItem(securityRequirement)
                .schemaRequirement("Bearer Authentication", securityScheme);
    }
}