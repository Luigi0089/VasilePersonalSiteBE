# 📌 Vasile Personal Site – Backend

Backend Java Spring Boot che gestisce le funzionalità del sito personale
di **Luigi Vasile**, incluse la prenotazione delle lezioni private,
l’invio di email, la gestione delle skill e dei progetti, 
l’integrazione con GitHub e l'esposizione di API REST sicure.

---

## 🚀 Tech Stack

- **Java 21+**
- **Spring Boot 3**
    - Spring Web
    - Spring Data JPA
    - Spring Validation
    - Spring Boot Mail (SendGrid/Mailjet)
- **PostgreSQL / NeonDB**
- **Hibernate ORM**
- **Feign Client** (integrazione GitHub API)
- **Docker**
- **Render Deploy**

---

## 📂 Struttura del progetto

```text
VasilePersonalSiteBe
│
├── .gitattributes
├── .gitignore
├── Dockerfile
├── HELP.md
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── it.vasilepersonalsite
│   │   │       ├── client/        # Client esterni (es. GitHub Feign Client)
│   │   │       ├── config/        # Configurazioni (CORS, Feign, Mail, ecc.)
│   │   │       ├── constans/      # Costanti applicative
│   │   │       ├── controller/    # Controller REST
│   │   │       ├── DAO/           # Accesso al database (Repository/DAO)
│   │   │       ├── DTO/           # Data Transfer Objects
│   │   │       ├── entity/        # Entity JPA
│   │   │       ├── exception/     # Gestione custom delle eccezioni
│   │   │       ├── service/       # Interfacce dei servizi applicativi
│   │   │       │   └── impl/      # Implementazioni della business logic
│   │   │       ├── util/          # Utility e helper
│   │   │       ├── validation/    # Validator personalizzati
│   │   │       └── VasilePersonalSiteApplication  # Main Spring Boot
│   │   │
│   │   └── resources
│   │       ├── static/
│   │       │   └── Logo.png       # Asset statici
│   │       ├── templates/         # Template email (HTML)
│   │       ├── application.properties
│   │       └── application-dev.properties
│   │
│   └── test/                      # Test JUnit
│
└── target/                         # Output build

```
---

## 🛠 Configurazione

### 🔐 Variabili d’ambiente (Render / locale)

| Variabile | Descrizione |
|----------|-------------|
| `GITHUB_TOKEN` | Token personale per leggere i repository GitHub pubblici/privati |
| `JDBC_URL` | URL PostgreSQL/Neon |
| `DB_USERNAME` | Username del database |
| `DB_PASSWORD` | Password del database |
| `MAIL_API_KEY` | API Key del provider email |
| `MAIL_FROM` | Mittente delle email |
| `GOOGLE_CLIENT_ID` | Client ID OAuth Google (link Meet alla conferma) |
| `GOOGLE_CLIENT_SECRET` | Client secret OAuth Google |
| `GOOGLE_REFRESH_TOKEN` | Refresh token dell'account Google che ospita le lezioni |
| `GOOGLE_CALENDAR_ID` | (opzionale) calendario su cui creare gli eventi, default `primary` |

#### 🎥 Link Google Meet alla conferma

Quando una lezione viene confermata il backend crea un evento su Google Calendar con una
videochiamata Meet, salva il link in `lezione_prenotata.link_meet`, lo inserisce nella mail
di conferma allo studente (e nel file `.ics` allegato) e lo espone nella dashboard.
Se le variabili `GOOGLE_*` sono vuote o Google non risponde, la lezione viene confermata
comunque, solo senza link.

Setup una tantum:

1. Google Cloud Console → crea un progetto e abilita **Google Calendar API**.
2. *Schermata consenso OAuth*: tipo "Esterno", aggiungi il tuo account come utente di test
   e **pubblica l'app** (in modalità "Testing" il refresh token scade dopo 7 giorni).
3. *Credenziali* → *ID client OAuth* → tipo **Applicazione web**, con redirect URI
   `https://developers.google.com/oauthplayground`. Copia Client ID e Client secret.
4. Apri [OAuth Playground](https://developers.google.com/oauthplayground), ingranaggio →
   "Use your own OAuth credentials" → incolla ID e secret. Scope:
   `https://www.googleapis.com/auth/calendar.events` → *Authorize APIs* con l'account
   che deve ospitare le lezioni → *Exchange authorization code for tokens*.
5. Copia il **Refresh token** in `GOOGLE_REFRESH_TOKEN` (e le altre due variabili).

---

### 📄 `application.properties`

```properties
spring.application.name=VasilePersonalSite

app.cors.allowed-origins=https://vasile-luigi.onrender.com

github.token=${GITHUB_TOKEN}

spring.datasource.url=${JDBC_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

chiave.SimpleAES=${CHIAVE_AES}

universal.password=${UNIVERSAL_PASSWORD}

lezioni.notification.to=${MIA_MAIL}

personal.domain=${PERSONAL_DOMAIN}

mailjet.api.key=${MAILJET_API_KEY}
mailjet.api.secret=${MAILJET_SECRET_KEY}
mailjet.sender.email=${MAIL_SENDER}
mailjet.sender.name=${MAIL_NAME}

spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.default-encoding=UTF-8
```
---

## ✉️ Funzionalità principali

### 1️⃣ Prenotazione lezioni private
- Validazione dei campi
- Salvataggio in DB
- Email di conferma allo studente
- Email di notifica a Luigi
- Stato della prenotazione inviato al FE

### 2️⃣ Integrazione GitHub
- Lettura repository personali
- Integrazione tramite **FeignClient**
- Restituisce al FE dati già mappati (nome, descrizione, URL, tech stack)

### 3️⃣ Gestione skill e categorie
- Relazione molti-a-molti
- Seed dati iniziali
- Endpoint pubblici di sola lettura

### 4️⃣ Endpoint di salute
Usato in Render e UptimeRobot:

---

# 📡 API Endpoint

Tutti gli endpoint espongono il prefisso: /luigi/vasile/personal/api

## 🧩 STACK & PROGETTI

### 📘 Progetti (GitHub)
| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/stack/progetti` | Restituisce la lista dei progetti GitHub |
| GET | `/stack/readme?repoName={nome}` | Restituisce il README markdown di un repository |

## 🧩 SKILL — PUBLIC

### 📘 Skill
| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/stack/skills` | Restituisce tutte le skill (con categorie e keyword) |
| GET | `/stack/skills/{id}` | Restituisce una singola skill |

### 📘 Categorie (public)
| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/stack/categories` | Restituisce tutte le categorie disponibili |

### 📘 Keyword (public)
| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/stack/keywords` | Restituisce tutte le keyword |

---

# 🧩 LEZIONI

### 📘 Prenotazioni lezioni

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| POST | `/lezioni` | Crea una nuova lezione |
| PUT | `/lezioni/modifica` | Modifica una prenotazione |
| PUT | `/lezioni/annulla` | Annulla una prenotazione |
| GET | `/lezioni/settimana?data=yyyy-MM-dd` | Restituisce le lezioni della settimana contenente la data indicata |

---

# 🔐 API ADMIN

Prefisso: /luigi/vasile/personal/api/admin


### 📘 Lezioni – Admin

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/admin/conferma?id={id}` | Conferma una lezione |
| GET | `/admin/rifiuta?id={id}` | Rifiuta una lezione |
| GET | `/admin/ping` | Endpoint di salute (usato da Render/UptimeRobot) |

---

### 📘 Skill – Admin (richiede `?password=...`)

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| POST | `/admin/skills?password=` | Crea una nuova skill |
| PUT | `/admin/skills/{id}?password=` | Aggiorna una skill |
| DELETE | `/admin/skills/{id}?password=` | Elimina una skill |

---

### 📘 Category – Admin (richiede `?password=...`)

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| POST | `/admin/skills/categories?password=` | Crea una nuova categoria |
| PUT | `/admin/skills/categories/{id}?password=` | Aggiorna una categoria |
| DELETE | `/admin/skills/categories/{id}?password=` | Elimina una categoria |

---

### 📘 Keyword – Admin (richiede `?password=...`)

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| POST | `/admin/skills/keywords?password=` | Crea una nuova keyword |
| PUT | `/admin/skills/keywords/{id}?password=` | Aggiorna una keyword |
| DELETE | `/admin/skills/keywords/{id}?password=` | Elimina una keyword |

---

## 🐳 Docker

### Dockerfile

```dockerfile
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]

```

---

## 📄 Licenza

Questo repository è pubblicato per **visione del codice**, ma la proprietà rimane di **Luigi Vasile**.  
L’uso non autorizzato non è consentito.

---

## 👤 Autore

**Luigi Francesco Vasile**  
Full Stack Developer  
📧 Email: Luigifravasile@gmail.com
🌐 Sito: https://vasile-luigi.onrender.com

