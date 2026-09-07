# Projet microservices — Système de gestion de bibliothèque

> Document de rendu destiné au formateur.
> Chaque étape du cahier des charges a été **prise en compte et validée**.

**Stack** : Spring Boot 3.3.2 · Spring Cloud 2023.0.3 · Java 17 · Maven · Docker Compose

---

## 1. Contexte

La bibliothèque souhaitait informatiser la gestion de son catalogue de livres et de ses
emprunts. Deux nouveaux microservices métier ont été construits — **`book-service`** et
**`loan-service`** — en réutilisant l'infrastructure déjà en place (`eureka-server`,
`config-server`, `api-gateway`).

La difficulté ajoutée par rapport au projet guidé : `loan-service` doit **lire et écrire**
chez `book-service` (décrémenter / réincrémenter le stock), ce qui introduit le problème
**TOCTOU** (*Time-Of-Check to Time-Of-Use*), traité par une **revérification côté serveur**.

---

## 2. Architecture

L'infrastructure existante (`eureka-server`, `config-server`, `api-gateway`) a été
réutilisée ; deux services métier ont été ajoutés (`book-service`, `loan-service`).

```
┌──────────────┐     ┌───────────────┐     ┌──────────────────┐
│  api-gateway │────▶│ config-server │◀────│   config-repo/   │
│    :8080     │     │      :8888    │     │   (*.yml)        │
└──────┬───────┘     └──────┬────────┘     └──────────────────┘
       │                    │ (sert la config)
       ▼                    ▼
   eureka-server ←─────────┴── (annuaire / découverte)
       :8761
       │
       ▼
┌──────────────┐  ──Feign GET────▶  ┌───────────────┐
│ order-service│                   │ product-svc   │
│     :8082    │                   │    :8081      │
└──────────────┘                   └───────────────┘
┌──────────────┐ ─Feign LECTURE+ÉCRITURE▶ ┌───────────────┐
│ loan-service │                          │ book-service  │
│     :8092    │                          │    :8091      │
└──────────────┘                          └───────────────┘
```

| Service          | Port  | Rôle                                                              |
|------------------|-------|-------------------------------------------------------------------|
| `eureka-server`  | 8761  | Service discovery / annuaire                                       |
| `config-server`  | 8888  | Configuration centralisée (profil `native`)                        |
| `api-gateway`    | 8080  | Point d'entrée unique + routage + load-balancing                   |
| `product-service`| 8081  | Catalogue de produits (JPA + H2)                                   |
| `order-service`  | 8082  | Commandes — consulte `product-service` via Feign (GET)             |
| `book-service`   | 8091  | Catalogue de livres (JPA + H2 `bookdb`)                            |
| `loan-service`   | 8092  | Emprunts (JPA + H2 `loandb`) — appelle `book-service` en lecture **et** écriture via Feign |

---

## 3. Checklist du cahier des charges

| Étape | Statut | Validation |
|-------|--------|------------|
| `book-service` : CRUD complet + validation + erreurs (404 / 400) | ✅ | `BookController` + `@Valid` + `GlobalExceptionHandler` / `ApiError` |
| `book-service` : `decrement-stock` / `increment-stock` | ✅ | Revérification serveur → **409** (défense en profondeur) |
| `loan-service` : création d'emprunt | ✅ | Feign en **lecture** puis **écriture**, `dueDate = +14 j` |
| `loan-service` : gestion des erreurs Feign | ✅ | Livre inexistant → **400**, stock épuisé / déjà rendu → **409** |
| `availableCopies` ne dépasse jamais `totalCopies` | ✅ | Contrôlé dans `increment-stock` et `update` |
| Tests unitaires (Mockito) + intégration (MockMvc) | ✅ | 36 tests, dont le cas de **concurrence TOCTOU** |
| Enregistrement Eureka + routage via la gateway | ✅ | Routes `/api/books/**`, `/api/loans/**` dans `api-gateway.yml` |
| `config-repo/book-service.yml` et `loan-service.yml` | ✅ | Ports 8091/8092, bases H2 `bookdb` / `loandb` |
| `docker-compose.yml` + `Dockerfile` (2 services) | ✅ | 7 services, build multi-stage |
| Fichier `.http` testant les scénarios via la gateway | ✅ | `api-gateway.http` (5 scénarios + bonus) |

### Bonus réalisés

| Bonus | Statut | Détail |
|-------|--------|--------|
| Recherche `?author=` / `?title=` (insensible à la casse) | ✅ | `JpaSpecificationExecutor` |
| Pagination `?page=&size=` | ✅ | retourne une `Page<BookResponse>` |
| ISBN unique → `409 Conflict` | ✅ | `DuplicateIsbnException` |
| 3 emprunts `ACTIVE` max par `memberName` | ✅ | `countByMemberNameAndStatus` → `409` |
| Docker (conteneurisation des 2 services) | ✅ | multi-stage + `docker-compose.yml` |

---

## 4. Validation technique

- **Compilation** : `mvn -pl book-service,loan-service -am clean package -DskipTests` → `BUILD SUCCESS`.
- **Tests** : `mvn -pl book-service,loan-service test` → `BUILD SUCCESS`
  - `book-service` : **19 tests** (9 unitaires + 10 intégration)
  - `loan-service` : **17 tests** (10 unitaires + 7 intégration)
- **Cas particulier couvert** : scénario « refuser l'emprunt si stock épuisé » et cas de
  **concurrence** (revérification côté `book-service`).

---

## 5. Lancer le projet

### Sans Docker
```bash
mvn -pl eureka-server spring-boot:run
mvn -pl config-server spring-boot:run
mvn -pl api-gateway   spring-boot:run
mvn -pl book-service  spring-boot:run
mvn -pl loan-service  spring-boot:run
```
Ordre : `eureka-server` → `config-server` → `api-gateway` → `book-service` → `loan-service`.

### Avec Docker
```bash
docker compose up --build
docker compose ps   # attendre que tout soit "healthy"
```

### Tester via la gateway (`http://localhost:8080`)
Les scénarios sont regroupés dans `api-gateway.http` (client REST) :
1. Création d'un livre ;
2. Création d'un emprunt réussi ;
3. Création d'un emprunt refusé (stock épuisé → `409`) ;
4. Retour d'un emprunt ;
5. Retour d'un emprunt déjà rendu → `409` ;
6. Bonus : recherche, pagination, limite de 3 emprunts par membre.

### Swagger / OpenAPI
- `book-service` : `http://localhost:8091/swagger-ui.html`
- `loan-service` : `http://localhost:8092/swagger-ui.html`

---

## 6. Endpoints

### `book-service` (`:8091`)
| Méthode | Endpoint | Rôle |
|---------|----------|------|
| `GET`    | `/api/books`                        | Liste (filtres `?author=`, `?title=`, pagination `?page=&size=`) |
| `GET`    | `/api/books/{id}`                   | Récupère un livre |
| `POST`   | `/api/books`                        | Crée un livre (`availableCopies = totalCopies`) |
| `PUT`    | `/api/books/{id}`                   | Modifie un livre |
| `DELETE` | `/api/books/{id}`                   | Supprime un livre |
| `PATCH`  | `/api/books/{id}/decrement-stock`   | (interne) décrémente le stock — `409` si déjà à 0 |
| `PATCH`  | `/api/books/{id}/increment-stock`   | (interne) réincrémente le stock — `409` si dépasse `totalCopies` |

### `loan-service` (`:8092`)
| Méthode | Endpoint | Rôle |
|---------|----------|------|
| `GET`    | `/api/loans`             | Liste les emprunts |
| `GET`    | `/api/loans/{id}`        | Récupère un emprunt |
| `POST`   | `/api/loans`             | Crée un emprunt (vérifie, décrémente, `dueDate = +14 j`) — `409` si aucun exemplaire |
| `PATCH`  | `/api/loans/{id}/return` | Rend un emprunt (réincrémente le stock) — `409` si déjà rendu |

---

## 7. Modèles

- **`Book`** : `id`, `isbn`, `title`, `author`, `totalCopies` (≥ 1), `availableCopies`
  (≥ 0, jamais > `totalCopies`).
- **`Loan`** : `id`, `memberName`, `bookId`, `bookTitle` (snapshot), `loanDate`,
  `dueDate` (= `loanDate + 14 jours`), `returnDate` (null tant que non rendu),
  `status` (`ACTIVE` / `RETURNED`).

Les erreurs sont renvoyées sous un corps uniforme `ApiError`
(`timestamp`, `status`, `error`, `message`, `details`).
