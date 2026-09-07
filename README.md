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
│ product-svc  │◀────▶│ order-service │ (via OpenFeign)
│     :8081    │      │     :8082      │
└──────────────┘      └───────────────┘
┌──────────────┐      ┌───────────────┐
│  book-svc    │◀────▶│  loan-service │ (lecture + écriture via Feign)
│     :8091    │      │     :8092      │
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
| `book-service`  | 8091  | Catalogue de livres (JPA + H2 `bookdb`)         |
| `loan-service`  | 8092  | Emprunts (lit **et** écrit** chez `book-service` via Feign) |

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

## Étape 10 — 🎓 Exercice final : système de gestion de bibliothèque

Deux nouveaux microservices métier **`book-service`** (catalogue de livres) et
**`loan-service`** (emprunts), en réutilisant `eureka-server`, `config-server`,
`api-gateway` déjà construits.

### Ce qui change par rapport au projet guidé

Dans le projet guidé, `order-service` ne faisait que **lire** chez `product-service`
(`GET`), jamais modifier. Ici, `loan-service` **lit et écrit** chez `book-service` :

- créer un emprunt → **décrémente** le nombre d'exemplaires disponibles ;
- rendre un emprunt → **réincrémente** le nombre d'exemplaires disponibles.

### Points clés de l'implémentation

1. **Endpoints d'écriture internes** : `PATCH /api/books/{id}/decrement-stock` et
   `PATCH /api/books/{id}/increment-stock` sont appelés **par `loan-service` via
   Feign** et non par un client externe (situation fréquente en microservices).
2. **Règle métier bloquante** : refuser un emprunt si aucun exemplaire n'est
   disponible → **`409 Conflict`** (nouveau code HTTP par rapport au projet guidé,
   qui n'utilisait que 400/404/502).
3. **Problème TOCTOU (*Time-Of-Check to Time-Of-Use*)** : entre la vérification de
   disponibilité (étape 1) et l'appel de décrément (étape 2), un **emprunt
   concurrent** peut avoir consommé le dernier exemplaire. C'est pourquoi
   `book-service` **revérifie** la condition au moment de décrémenter (défense en
   profondeur), plutôt que de faire confiance à la vérification déjà faite par
   `loan-service`.

### Modèle

- **`Book`** : `totalCopies` (≥ 1) et `availableCopies` (≥ 0, jamais > `totalCopies`).
  À la création, `availableCopies` est initialisé à `totalCopies`.
- **`Loan`** : `memberName`, `bookId`, `bookTitle` (snapshot copié depuis
  `book-service`), `loanDate`, `dueDate` (= `loanDate + 14 jours`), `returnDate`
  (null tant que non rendu), `status` (`ACTIVE` / `RETURNED`).

### API

| Méthode | Endpoint | Rôle |
|---------|----------|------|
| `GET` | `/api/books` | Liste les livres |
| `GET` | `/api/books/{id}` | Récupère un livre |
| `POST` | `/api/books` | Ajoute un livre (`availableCopies = totalCopies`) |
| `PUT` | `/api/books/{id}` | Modifie un livre |
| `DELETE` | `/api/books/{id}` | Supprime un livre |
| `PATCH` | `/api/books/{id}/decrement-stock` | (interne) décrémente le stock — `409` si déjà à 0 |
| `PATCH` | `/api/books/{id}/increment-stock` | (interne) réincrémente le stock, sans dépasser `totalCopies` — `409` sinon |
| `GET` | `/api/loans` | Liste les emprunts |
| `GET` | `/api/loans/{id}` | Récupère un emprunt |
| `POST` | `/api/loans` | Crée un emprunt (vérifie, décrémente, fixe `dueDate = +14 j`) — `409` si aucun exemplaire |
| `PATCH` | `/api/loans/{id}/return` | Rend un emprunt (réincrémente le stock) — `409` si déjà rendu |

### Règle métier — création d'un emprunt (`POST /api/loans`)

```
Requête { "memberName": "Bob", "bookId": 3 }
  │
  ▼
1. loan-service → GET /api/books/3
  │ ├── livre inexistant ────────────────▶ 400
  │ ├── availableCopies == 0 ────────────▶ 409 (pas d'appel decrement-stock)
  │ └── availableCopies > 0
  │        ▼
2. loan-service → PATCH /api/books/3/decrement-stock
  │ ├── book-service refuse (409, concurrence) ─────▶ 409 au client
  │ └── book-service décrémente (200)
  │        ▼
3. crée l'emprunt : ACTIVE, dueDate = loanDate + 14 jours → 201
```

Les erreurs sont renvoyées sous un corps uniforme `ApiError`
(`timestamp`, `status`, `error`, `message`, `details`).

### Construit dans cette étape

- `book-service/` et `loan-service/` (sources + `pom.xml`) + leurs tests
  (`BookServiceTest`, `BookControllerIntegrationTest`, `LoanServiceTest`,
  `LoanControllerIntegrationTest`).
- `config-repo/book-service.yml` et `config-repo/loan-service.yml` (+ copies dans
  `config-server/config-repo/`) : ports `8091` / `8092`, bases H2 `bookdb` / `loandb`.
- 2 nouvelles routes dans `api-gateway.yml` : `/api/books/**` → `book-service`,
  `/api/loans/**` → `loan-service`.
- Modules ajoutés au parent `pom.xml` ; `book-service/Dockerfile`,
  `loan-service/Dockerfile` ; services ajoutés à `docker-compose.yml`.

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

### 1.b) Couverture de tests (JaCoCo)

Le plugin **JaCoCo** est déclaré dans le `pom.xml` parent : il instrumente le code lors du
`mvn test` et génère automatiquement un rapport de couverture pour **chaque module**.

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64   # ou tout JDK >= 17
mvn test                                              # tests + génération rapport coverage
```

Rapports HTML générés (à ouvrir dans un navigateur) :

- `product-service/target/site/jacoco/index.html`
- `order-service/target/site/jacoco/index.html`
- `book-service/target/site/jacoco/index.html`
- `loan-service/target/site/jacoco/index.html`

Le rapport présente la couverture par **instruction / branche / ligne / méthode**, classe par
classe (vert = couvert, rouge = non couvert). C'est l'outil idéal pour repérer les endpoints
ou branches de code qui restent à tester.

### 2) Lancer les services (sans Docker)

Depuis la racine, un terminal par service :

```bash
mvn -pl eureka-server spring-boot:run
mvn -pl config-server spring-boot:run
mvn -pl api-gateway   spring-boot:run
mvn -pl product-service spring-boot:run
mvn -pl order-service  spring-boot:run
mvn -pl book-service   spring-boot:run
mvn -pl loan-service   spring-boot:run
```

Ordre de démarrage conseillé : `eureka-server` → `config-server` → `api-gateway` →
`product-service` → `order-service` → `book-service` → `loan-service`.

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

# Livres
curl http://localhost:8080/api/books                  # GET all
curl http://localhost:8080/api/books/1                # GET one

# Emprunter un livre (décrémente le stock)
curl -X POST http://localhost:8080/api/loans \
  -H "Content-Type: application/json" \
  -d '{"bookId":1,"memberName":"Alice"}'

# Rendre un livre (réincrémente le stock)
curl -X PATCH http://localhost:8080/api/loans/1/return
```

### 5) Swagger / OpenAPI

- `product-service` : `http://localhost:8081/swagger-ui.html`
- `order-service` : `http://localhost:8082/swagger-ui.html`
- `book-service` : `http://localhost:8091/swagger-ui.html`
- `loan-service` : `http://localhost:8092/swagger-ui.html`

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
