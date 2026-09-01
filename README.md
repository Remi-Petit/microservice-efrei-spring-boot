# microservice-parent — Projet microservices Spring Boot / Spring Cloud

Projet pédagogique d'architecture microservices avec **Spring Boot 3** et **Spring Cloud 2023.0.3** (Java 17).

## Architecture

```
┌──────────────┐     ┌───────────────┐     ┌──────────────────┐
│  Api Gateway │────▶│  config-server│◀────│   config-repo/   │
│    :8080     │     │     :8888     │     │   (*.yml)        │
└──────┬───────┘     └──────┬────────┘     └──────────────────┘
       │                    │ (sert la config)
       ▼                    ▼
   eureka-server ←─────────┴── (annuaire/découverte)
       :8761
       │
       ▼
┌──────────────┐      ┌───────────────┐
│ product-svc  │◀────▶│  order-service │ (via OpenFeign)
│     :8081    │      │     :8082      │
└──────────────┘      └───────────────┘
```

### Modules

| Module          | Port  | Rôle                                            |
|-----------------|-------|-------------------------------------------------|
| `eureka-server` | 8761  | Service discovery / annuaire (Eureka)           |
| `config-server` | 8888  | Configuration centralisée (profil `native`)     |
| `api-gateway`   | 8080  | Point d'entrée unique + routage + load-balancing |
| `product-service`| 8081 | Catalogue de produits (JPA + H2)                |
| `order-service` | 8082  | Commandes (consomme `product-service` via Feign) |

---

## Étape 8 — Volet Docker (déjà mis en place)

Fichiers ajoutés :

- `eureka-server/Dockerfile`, `config-server/Dockerfile`, `api-gateway/Dockerfile`,
  `product-service/Dockerfile`, `order-service/Dockerfile` — tous en **multi-stage build**
  (`maven:3.9-eclipse-temurin-17` → `eclipse-temurin:17-jre-jammy`).
- `docker-compose.yml` à la racine — orchestre les 5 services sur le réseau `microservice-net`.
- `.dockerignore` — exclut `target/`, `.git/`, etc. du contexte de build.

### Points clés retenus du module 8

- **Nom de service = nom DNS interne** : dans Docker, on joint les services par leur
  nom (`http://eureka-server:8761`), pas `localhost`.
- **`depends_on` + `condition: service_healthy`** : évite de démarrer un service avant
  que ses dépendances soient prêtes.
- **`config-server`** : `WORKDIR /app` fait que `spring.cloud.config.server.native.search-locations=file:./config-repo`
  nécessite `COPY config-server/config-repo config-repo` dans l'image.
- **Priorité de config** : variables d'environnement > config-server > valeurs locales.
  `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` dans `docker-compose.yml` écrase la valeur
  `localhost` de `config-repo/application.yml`.

---

## Étapes restantes (à exécuter par toi)

### 1) Vérifier la compilation + les tests

Dans `mon-projet-microservice/` :

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64   # ou tout JDK >= 17
mvn clean test
```

Résultat attendu : `BUILD SUCCESS` (tous les tests passent).

> ⚠️ Les tests unitaires (`ProductServiceTest`, `OrderServiceTest`) et d'intégration
> (`ProductControllerIntegrationTest`, `OrderControllerIntegrationTest`) sont situés dans
> chaque module. Ils utilisent une base H2 dédiée et `@MockBean ProductClient` pour isoler
> `order-service` de `product-service`.

### 2) Lancer les services (sans Docker)

Depuis la racine, un terminal par service :

```bash
mvn -pl eureka-server spring-boot:run
mvn -pl config-server spring-boot:run
mvn -pl api-gateway   spring-boot:run
mvn -pl product-service spring-boot:run
mvn -pl order-service  spring-boot:run
```

Ordre de démarrage conseillé : `eureka-server` → `config-server` → `api-gateway` →
`product-service` → `order-service`.

### 3) Lancer via Docker Compose

```bash
cd /home/admin/Rust/mon-projet-microservice
docker compose up --build
```

Puis vérifier que tous les conteneurs sont `healthy` :

```bash
docker compose ps
```

Attendre 20–30 s (rafraîchissement du cache Eureka côté gateway) avant d'appeler la gateway.

### 4) Tester via la gateway (`http://localhost:8080`)

Quelques requêtes de contrôle :

```bash
# Catalogue
curl http://localhost:8080/api/products            # GET all
curl http://localhost:8080/api/products/1          # GET one

# Créer une commande
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerName":"Alice","items":[{"productId":1,"quantity":2}]}'

# Changer le statut
curl -X PATCH http://localhost:8080/api/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"CONFIRMED"}'
```

### 5) Swagger / OpenAPI

- `product-service` : `http://localhost:8081/swagger-ui.html`
- `order-service` : `http://localhost:8082/swagger-ui.html`

---

## Rappels — pièges du module 8

1. **Architecture processeur** : sur Apple Silicon, éviter `eclipse-temurin:17-jre-alpine`
   (amd64 only) → utiliser `eclipse-temurin:17-jre-jammy` (multi-arch).
2. **Jar non exécutable** : `no main manifest attribute` ⇔ `spring-boot-maven-plugin`
   non activé dans le `<build><plugins>` du module (il ne suffit pas de le déclarer dans
   `pluginManagement` du parent). Chaque module exécutable doit le redéclarer.

## Notes

- `order-service` (module 7) stocke dans `OrderItem` une **copie** du nom/prix produit
  (pattern snapshot) et traduit les erreurs Feign : `NotFound` → **400**, autres
  `FeignException` → **502**.
- `product-service` (module 6) applique le pattern **DTO** (jamais l'entité renvoyée
  directement) et utilise `@RestControllerAdvice` pour centraliser les erreurs.
