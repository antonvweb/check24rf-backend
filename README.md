# Check24RF API - Backend

Мультисервисное Java/Spring Boot приложение для интеграции с МЧО (Мои Чеки Онлайн).

## 🏗 Архитектура

```
                                    Internet
                                       │
                                       ▼
                              ┌─────────────────┐
                              │   Nginx (80)    │
                              │  HTTP → HTTPS   │
                              └─────────────────┘
                                       │
                              ┌─────────────────┐
                              │  Nginx (443)    │
                              │  SSL Terminate  │
                              └─────────────────┘
                                       │
              ┌────────────────────────┼────────────────────────┐
              │                        │                        │
              ▼                        ▼                        ▼
    ┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
    │  чек24.рф        │   │  api.xn--24-...  │   │  host.docker.    │
    │  (Next.js:3000)  │   │  (API Gateway)   │   │  internal:16542  │
    │  Фронтенд        │   │                  │   │  adminPanel      │
    └──────────────────┘   └──────────────────┘   └──────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│ authService     │   │ userService     │   │ mcoService      │
│ Docker :18273   │   │ Docker :19384   │   │ Docker :17456   │
│ +Bucket4j       │   │                 │   │ +WebSocket      │
└─────────────────┘   └─────────────────┘   └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                                 ▼
                    ┌────────────────────────┐
                    │  check24_internal      │
                    │  Network (172.28.0.0)  │
                    └────────────────────────┘
                                 │
                    ┌────────────┴────────────┐
                    ▼                         ▼
          ┌─────────────────┐       ┌─────────────────┐
          │  PostgreSQL     │       │  Redis          │
          │  :15433         │       │  :16380         │
          │  +Backup        │       │  +Blacklist     │
          └─────────────────┘       └─────────────────┘
```

## 📦 Сервисы

| Сервис | Порт | RAM | Описание | Особенности |
|--------|------|-----|----------|-------------|
| authService | 18273 | 1 GB | Аутентификация, JWT, SMS/Email коды | Bucket4j rate limiting |
| userService | 19384 | 1 GB | Управление пользователями | |
| mcoService | 17456 | 4 GB | Интеграция с МЧО (SOAP, WebSocket) | WebSocket поддержка |
| adminPanelService | 16542 | 512 MB | Админ-панель (без Docker) | На хосте |
| PostgreSQL | 15433 | - | База данных | Auto backup 3:00 |
| Redis | 16380 | 256 MB | Кэш, сессии, blacklist токенов | AOF+RDB персистентность |

**Итого:** ~7 GB RAM на сервисы (на сервере 32 GB)

## 🔒 Безопасность

### Уровень приложения:
- **JWT в httpOnly cookies** — токены недоступны через JavaScript
- **CSRF protection** — включена для state-changing endpoints
- **Ротация refresh токенов** — каждый refresh token используется 1 раз
- **Token blacklist** — отозванные токены хранятся в Redis
- **Лимит попыток** — 5 попыток ввода кода верификации
- **Bucket4j rate limiting** — 10 запросов/сек на IP
- **Валидация phone/email** — строгая валидация форматов
- **Audit logging** — логирование всех событий безопасности

### Уровень инфраструктуры:
- **HTTPS** — SSL/TLS (Let's Encrypt)
- **Security headers** — X-Frame-Options, X-Content-Type-Options, CSP
- **Rate limiting (Nginx)** — 5 r/s для auth, 20 r/s для API
- **Firewall (UFW)** — только необходимые порты
- **Fail2Ban** — бан после 5 попыток
- **SSH hardening** — только ключи, порт 47483
- **Auto-updates** — автоматические обновления безопасности

## 🚀 Быстрый старт

### ⚡️ Автоматический деплой через GitHub Actions

Все секреты хранятся в GitHub, `.env` файл создаётся автоматически при деплое.

**Инструкция:**
1. Настройте сервер: `sudo bash server_setup.sh --main-domain <domain> --api-domain <domain> --email <email>`
2. Добавьте 23 GitHub Secrets (см. **GITHUB_SECRETS.md**)
3. Сделайте push в ветку `main`
4. GitHub Actions автоматически создаст `.env` и развернёт проект

**Подробная инструкция:** см. **DEPLOYMENT_GUIDE.md**

---

### Предварительные требования

- Docker 24+
- Docker Compose 2.20+
- Java 17 (для adminPanelService)
- Maven 3.9+

### 1. Клонирование репозитория

```bash
git clone <repository-url>
cd chech24rfapi
```

### 2. Настройка переменных окружения

```bash
cp .env.example .env
```

**Заполните `.env` значениями:**

```bash
# JWT Secret (64+ символа)
JWT_SECRET=$(openssl rand -base64 48)

# Пароли (16+ символов)
DB_PASSWORD=<сгенерируйте-случайный-пароль>
REDIS_PASSWORD=<сгенерируйте-случайный-пароль>

# MCO API Token (из письма ФНС)
MCO_API_TOKEN=<ваш-токен>

# SmartCaptcha Key (Yandex Cloud)
SMARTCAPTCHA_SERVER_KEY=<ваш-ключ>
```

### 3. Запуск Docker сервисов

```bash
docker compose up -d
```

Проверка статуса:

```bash
docker compose ps
```

Просмотр логов:

```bash
docker compose logs -f authService
docker compose logs -f userService
docker compose logs -f mcoService
```

### 4. Запуск adminPanelService (на хосте)

```bash
cd adminPanelService
mvn clean package
java -jar target/adminPanelService.jar --spring.profiles.active=dev
```

### 5. Проверка работы

Health check:

```bash
curl http://localhost:80/health
```

Получение CSRF токена:

```bash
curl http://localhost:80/api/auth/csrf-token
```

Отправка кода верификации:

```bash
curl -X POST http://localhost:80/api/auth/send-code \
  -H "Content-Type: application/json" \
  -d '{"identifier": "79054455906"}'
```

## 🔒 Безопасность

### JWT Tokens в Cookies

- **Access Token**: httpOnly, secure, sameSite=Strict
- **Refresh Token**: httpOnly, secure, sameSite=Strict
- **Ротация**: Refresh token используется один раз
- **Blacklist**: Отозванные токены хранятся в Redis

### CSRF Protection

Получите CSRF токен перед запросами:

```bash
curl http://localhost:80/api/auth/csrf-token
```

Используйте в заголовке: `X-CSRF-TOKEN: <token>`

### Rate Limiting (Nginx)

| Endpoint | Limit |
|----------|-------|
| /api/auth/* | 5 req/s |
| /api/users/* | 20 req/s |
| /api/mco/* | 20 req/s |
| Остальные | 50 req/s |

## 🛠 Разработка

### Сборка всех сервисов

```bash
mvn clean package
```

### Запуск конкретного сервиса

```bash
# authService
java -jar authService.jar --spring.profiles.active=dev

# userService
java -jar userService.jar --spring.profiles.active=dev

# mcoService
java -jar mcoService.jar --spring.profiles.active=dev
```

### Docker сборка

```bash
# Пересборка образов
docker compose build --no-cache

# Запуск с пересборкой
docker compose up -d --build
```

## 📊 Мониторинг

### Health Checks

```bash
# Nginx
curl http://localhost:80/health

# authService
curl http://localhost:18273/actuator/health

# userService
curl http://localhost:19384/actuator/health

# mcoService
curl http://localhost:17456/actuator/health
```

### Prometheus Metrics

Метрики доступны на `/actuator/prometheus`:

```bash
# authService
curl http://localhost:18273/actuator/prometheus

# userService
curl http://localhost:19384/actuator/prometheus

# mcoService
curl http://localhost:17456/actuator/prometheus
```

### Audit Logs

Логи безопасности в `/var/www/chech24rfapi/logs/audit.log`:

```bash
tail -f /var/www/chech24rfapi/logs/audit.log
```

### Backup PostgreSQL

```bash
# Ручной backup
bash /var/www/chech24rfapi/scripts/backup-db.sh

# Восстановление из backup
bash /var/www/chech24rfapi/scripts/restore-db.sh /var/www/chech24rfapi/backups/postgresql/check_rf_20260302_120000.sql.gz

# Проверка cron
crontab -l | grep backup-db
```

### Логи

```bash
# Docker сервисы
docker compose logs -f

# PostgreSQL
docker compose logs -f postgres

# Redis
docker compose logs -f redis
```

## 🔧 Конфигурация

### Переменные окружения

| Переменная | Описание | Пример |
|------------|----------|--------|
| JWT_SECRET | Секрет для JWT (64+ символов) | `abc123...` |
| DB_PASSWORD | Пароль PostgreSQL | `secure123...` |
| REDIS_PASSWORD | Пароль Redis | `redis123...` |
| MCO_API_TOKEN | Токен ФНС для МЧО | `chek24_...` |
| SMARTCAPTCHA_SERVER_KEY | Ключ Yandex SmartCaptcha | `ysc2_...` |

### Порты

| Сервис | Переменная | По умолчанию |
|--------|------------|--------------|
| authService | AUTH_SERVICE_PORT | 18273 |
| userService | USER_SERVICE_PORT | 19384 |
| mcoService | MCO_SERVICE_PORT | 17456 |
| adminPanelService | ADMIN_SERVICE_PORT | 16542 |
| PostgreSQL | DB_PORT | 15433 |
| Redis | REDIS_PORT | 16380 |
| Next.js (фронтенд) | - | 3000 |

### Домены и URLs

| Сервис | URL |
|--------|-----|
| Фронтенд (Next.js) | `https://чек24.рф` |
| API | `https://api.xn--24-mlcu7d.xn--p1ai` |
| Admin API | `https://api.xn--24-mlcu7d.xn--p1ai/admin-api/` |

## 🧪 Тестирование

```bash
# Запуск тестов
mvn test

# Запуск тестов с покрытием
mvn clean test jacoco:report
```

## 📝 API Документация

Swagger UI доступен для каждого сервиса:

- authService: http://localhost:18273/swagger-ui.html
- userService: http://localhost:19384/swagger-ui.html
- mcoService: http://localhost:17456/swagger-ui.html
- adminPanelService: http://localhost:16542/swagger-ui.html

## 🔄 CI/CD

Автоматический deploy настроен через GitHub Actions.

### Настройка GitHub Secrets:

| Secret | Описание | Пример |
|--------|----------|--------|
| `PROD_SERVER_HOST` | IP адрес сервера | `95.213.143.142` |
| `PROD_SERVER_USER` | пользователь SSH | `checkadm` |
| `PROD_SSH_PRIVATE_KEY` | приватный SSH ключ | `-----BEGIN OPENSSH PRIVATE KEY-----...` |
| `PROD_SSH_PORT` | порт SSH | `47483` |
| `PROD_DB_PORT` | порт PostgreSQL | `15433` |
| `PROD_DB_NAME` | имя БД | `check_rf` |
| `PROD_DB_USER` | пользователь БД | `check_user` |
| `PROD_DB_PASSWORD` | пароль БД | `secure123...` |
| `PROD_REDIS_PORT` | порт Redis | `16380` |
| `PROD_REDIS_PASSWORD` | пароль Redis | `redis123...` |
| `PROD_JWT_SECRET` | JWT секрет (64+ символов) | `abc123...` |
| `PROD_AUTH_SERVICE_PORT` | порт authService | `18273` |
| `PROD_USER_SERVICE_PORT` | порт userService | `19384` |
| `PROD_MCO_SERVICE_PORT` | порт mcoService | `17456` |
| `PROD_ADMIN_SERVICE_PORT` | порт adminPanel | `16542` |
| `PROD_MCO_API_TOKEN` | токен ФНС МЧО | `chek24_...` |
| `PROD_SMARTCAPTCHA_SERVER_KEY` | ключ SmartCaptcha | `ysc2_...` |

### Workflow:

- **Push в main/develop:** сборка → build Docker образов → deploy на сервер

### Ручной deploy:

```bash
# На сервере
cd /var/www/chech24rfapi
git pull origin main
docker compose down
docker compose build --no-cache
docker compose up -d
docker system prune -f
```

## ⚠️ Важные замечания

1. **Не коммитьте `.env` файл** - он содержит секреты
2. **Смените пароли по умолчанию** перед production
3. **Используйте HTTPS** в production (настройте SSL в nginx)
4. **Регулярно обновляйте токены MCO** и ключи капчи

## 🐛 Troubleshooting

### Сервис не запускается

```bash
# Проверьте логи
docker compose logs <service-name>

# Проверьте переменные окружения
docker compose config
```

### Ошибка подключения к БД

```bash
# Проверьте что PostgreSQL запущен
docker compose ps postgres

# Проверьте логи
docker compose logs postgres
```

### Проблемы с Redis

```bash
# Подключитесь к Redis CLI
docker compose exec redis redis-cli -a ${REDIS_PASSWORD}

# Проверьте ключи
docker compose exec redis redis-cli -a ${REDIS_PASSWORD} KEYS '*'
```

## 📄 Лицензия

Proprietary. Все права защищены.
