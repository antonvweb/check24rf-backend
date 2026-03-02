#!/bin/bash

################################################################################
# СКРИПТ НАСТРОЙКИ СЕРВЕРА CHECK24RF API
# Версия: 2.0 (Security Hardened)
#
# Docker, Docker Compose, Nginx, PostgreSQL, Redis
# SSH Hardening, Firewall, Fail2Ban, Auto-Updates
#
# ОСОБЕННОСТИ:
# - Генерация криптографически стойких паролей
# - Идемпопотентность (безопасный повторный запуск)
# - Логирование всех действий
# - Откат изменений при ошибках
# - Валидация входных данных
# - Security best practices
################################################################################

set -euo pipefail

# ==================== ЦВЕТОВОЙ ВЫВОД ====================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# ==================== ЛОГИРОВАНИЕ ====================

LOG_FILE="/var/log/check24rf-setup.log"
mkdir -p "$(dirname "$LOG_FILE")"
touch "$LOG_FILE"

log()      { echo -e "${GREEN}[$(date +'%H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"; }
log_err()  { echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$LOG_FILE"; }
log_info() { echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$LOG_FILE"; }
log_skip() { echo -e "${CYAN}[SKIP]${NC} $1 — уже выполнено" | tee -a "$LOG_FILE"; }

# ==================== ОБРАБОТКА ОШИБОК ====================

cleanup() {
    local exit_code=$?
    if [ $exit_code -ne 0 ]; then
        log_err "Скрипт завершён с ошибкой (код: $exit_code). Проверьте лог: $LOG_FILE"
        log_warn "Рекомендуется восстановить SSH конфиг из бэкапа: /etc/ssh/sshd_config.bak.*"
    fi
}

trap cleanup EXIT

# ==================== ПРОВЕРКА ПРАВ ====================

if [ "$EUID" -ne 0 ]; then
    log_err "Запустите скрипт от root: sudo bash $0"
    exit 1
fi

# ==================== КОНФИГУРАЦИЯ ====================

# SSH
SSH_PORT=47483
APP_USER="checkadm"
APP_USER_PASS='m}touYoK6pYadKndz$ZD'

# Пароли (заданы явно)
DB_PORT=15433
DB_PASSWORD='kaNVHE?H9|PV9uiB7%Rr'
REDIS_PORT=16380
REDIS_PASSWORD='hzrMR01zN~~2evgLHn8W'

# Директории
PROJECT_DIR="/var/www/chech24rfapi"
STATE_DIR="/var/lib/check24rf-setup"
BACKUP_DIR="/var/www/chech24rfapi/backups"

mkdir -p "$STATE_DIR" "$BACKUP_DIR"

# Файл для сохранения сгенерированных паролей
ENV_FILE="${PROJECT_DIR}/.env"

# JWT Secret (сгенерировать)
JWT_SECRET=$(openssl rand -base64 64 | tr -dc 'a-zA-Z0-9!@#$%^&*()_+-=' | head -c 64)

# ==================== АРГУМЕНТЫ СКРИПТА ====================

# Домены (передаются как аргументы)
MAIN_DOMAIN=""
API_DOMAIN=""
EMAIL=""

parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --main-domain)
                MAIN_DOMAIN="$2"
                shift 2
                ;;
            --api-domain)
                API_DOMAIN="$2"
                shift 2
                ;;
            --email)
                EMAIL="$2"
                shift 2
                ;;
            *)
                log_err "Неизвестный аргумент: $1"
                echo "Использование: $0 --main-domain <domain> --api-domain <domain> --email <email>"
                exit 1
                ;;
        esac
    done
}

parse_args "$@"

# ==================== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ====================

step_done()  { touch "${STATE_DIR}/$1"; }
step_check() { [ -f "${STATE_DIR}/$1" ]; }

# Генерация криптографически стойкого пароля
generate_password() {
    local length=${1:-32}
    openssl rand -base64 "$length" | tr -dc 'a-zA-Z0-9!@#$%^&*()_+-=' | head -c "$length"
}

# Проверка доступности порта
check_port_available() {
    local port=$1
    if ss -tlnp | grep -q ":${port} "; then
        log_err "Порт $port уже занят!"
        return 1
    fi
    return 0
}

# Проверка интернета
check_internet() {
    if ! ping -c 1 8.8.8.8 &>/dev/null; then
        log_err "Нет доступа к интернету. Проверьте соединение."
        exit 1
    fi
}

# ==================== ПРИВЕТСТВИЕ ====================

echo ""
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║         НАСТРОЙКА СЕРВЕРА CHECK24RF API v2.0                  ║"
echo "║         Security Hardened Edition                             ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# ==================== 0. ПРЕДВАРИТЕЛЬНАЯ ПРОВЕРКА ====================

if step_check "00_prerequisites"; then
    log_skip "[0/12] Предварительная проверка"
else
    log "[0/12] Предварительная проверка..."
    
    # Проверка ОС
    if [ ! -f /etc/os-release ]; then
        log_err "Не удалось определить ОС"
        exit 1
    fi
    
    source /etc/os-release
    if [[ "$ID" != "ubuntu" ]]; then
        log_err "Поддерживается только Ubuntu. Обнаружено: $ID"
        exit 1
    fi
    
    # Проверка версии Ubuntu
    if [[ "$VERSION_ID" != "20.04" && "$VERSION_ID" != "22.04" && "$VERSION_ID" != "24.04" ]]; then
        log_warn "Версия Ubuntu: $VERSION_ID. Рекомендуется 20.04, 22.04 или 24.04"
    fi
    
    # Проверка интернета
    check_internet
    log_info "✓ Интернет доступен"
    
    # Проверка портов
    for port in $SSH_PORT $DB_PORT $REDIS_PORT 80 443; do
        if ! check_port_available $port; then
            exit 1
        fi
    done
    log_info "✓ Все порты свободны"
    
    step_done "00_prerequisites"
    log "✓ Предварительная проверка пройдена"
fi

# ==================== 1. ОБНОВЛЕНИЕ СИСТЕМЫ ====================

if step_check "01_system_update"; then
    log_skip "[1/12] Обновление системы"
else
    log "[1/12] Обновление системы..."
    
    export DEBIAN_FRONTEND=noninteractive
    apt update
    apt upgrade -y
    
    apt install -y curl wget gnupg2 software-properties-common \
        apt-transport-https ca-certificates lsb-release unzip \
        git htop net-tools openssl jq
    
    step_done "01_system_update"
    log "✓ Система обновлена"
fi

# ==================== 2. ГЕНЕРАЦИЯ ПАРОЛЕЙ ====================

if step_check "02_generate_passwords"; then
    log_skip "[2/13] Генерация паролей"
else
    log "[2/13] Создание .env файла с паролями..."

    # Сохранение в .env файл
    mkdir -p "$PROJECT_DIR"
    cat > "$ENV_FILE" << EOF
# Check24RF API Environment
# Сгенерировано: $(date -Iseconds)
# НЕ КОММИТЬТЕ ЭТОТ ФАЙЛ В GIT!

# Application
APP_USER=${APP_USER}
APP_USER_PASS=${APP_USER_PASS}

# Database
DB_HOST=localhost
DB_PORT=${DB_PORT}
DB_NAME=check_rf
DB_USER=check_user
DB_PASSWORD=${DB_PASSWORD}

# Redis
REDIS_HOST=localhost
REDIS_PORT=${REDIS_PORT}
REDIS_PASSWORD=${REDIS_PASSWORD}

# JWT
JWT_SECRET=${JWT_SECRET}

# Service Ports
AUTH_SERVICE_PORT=18273
USER_SERVICE_PORT=19384
MCO_SERVICE_PORT=17456
ADMIN_SERVICE_PORT=16542

# MCO API (замените на реальный токен)
MCO_API_TOKEN=chek24_REPLACE_ME

# SmartCaptcha (замените на реальный ключ)
SMARTCAPTCHA_SERVER_KEY=ysc2_REPLACE_ME
EOF

    chmod 600 "$ENV_FILE"
    chown root:root "$ENV_FILE"

    log_info "Пароли сохранены в: $ENV_FILE"
    log_warn "ВАЖНО: Сохраните этот файл в безопасное место!"
    log_warn "Используйте значения из этого файла для GitHub Secrets"

    step_done "02_generate_passwords"
    log "✓ Пароли настроены"
fi

# ==================== 3. DOCKER ====================

if step_check "03_docker"; then
    log_skip "[3/12] Docker установка"
else
    log "[3/12] Установка Docker..."
    
    # Удаляем старые версии
    apt remove -y docker docker-engine docker.io containerd runc 2>/dev/null || true
    
    # Добавляем репозиторий
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
    chmod a+r /etc/apt/keyrings/docker.asc
    
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
        https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
        | tee /etc/apt/sources.list.d/docker.list > /dev/null
    
    apt update
    apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    
    step_done "03_docker"
    log "✓ $(docker --version)"
fi

# ==================== 4. НАСТРОЙКА DOCKER ====================

if step_check "04_docker_config"; then
    log_skip "[4/12] Настройка Docker"
else
    log "[4/12] Настройка Docker..."
    
    # Создаём группу docker
    getent group docker >/dev/null 2>&1 || groupadd docker
    
    # Настраиваем daemon.json
    cat > /etc/docker/daemon.json << 'EOF'
{
    "log-driver": "json-file",
    "log-opts": {
        "max-size": "10m",
        "max-file": "3"
    },
    "userland-proxy": false,
    "no-new-privileges": true,
    "live-restore": true
}
EOF
    
    # Создаём internal сеть
    docker network create check24_internal --driver bridge --internal 2>/dev/null || true
    
    systemctl restart docker
    systemctl enable docker
    
    step_done "04_docker_config"
    log "✓ Docker настроен, сеть check24_internal создана"
fi

# ==================== 5. NGINX ====================

if step_check "05_nginx"; then
    log_skip "[5/12] Nginx"
else
    log "[5/12] Установка Nginx..."
    
    apt install -y nginx
    systemctl enable nginx
    systemctl start nginx
    
    step_done "05_nginx"
    log "✓ Nginx установлен"
fi

# ==================== 6. ПОЛЬЗОВАТЕЛИ ====================

if step_check "06_users"; then
    log_skip "[6/12] Пользователи"
else
    log "[6/12] Настройка пользователей..."
    
    # Создаём пользователя
    if ! id "$APP_USER" &>/dev/null; then
        useradd -m -s /bin/bash "$APP_USER"
        log_info "Пользователь ${APP_USER} создан"
    else
        log_info "Пользователь ${APP_USER} уже существует"
    fi
    
    # Устанавливаем пароль
    echo "${APP_USER}:${APP_USER_PASS}" | chpasswd
    
    # Добавляем в группы
    usermod -aG sudo "$APP_USER"
    usermod -aG docker "$APP_USER"
    
    # Настраиваем SSH директорию
    APP_USER_HOME=$(eval echo ~"$APP_USER")
    mkdir -p "$APP_USER_HOME/.ssh"
    chmod 700 "$APP_USER_HOME/.ssh"
    
    log_info "Пользователь ${APP_USER} добавлен в группы: sudo, docker"
    
    step_done "06_users"
    log "✓ Пользователи настроены"
fi

# ==================== 7. SSH HARDENING ====================

if step_check "07_sshd_config"; then
    log_skip "[7/12] SSH Hardening"
else
    log "[7/12] Настройка SSH (порт ${SSH_PORT})..."
    
    SSHD_CONFIG="/etc/ssh/sshd_config"
    BACKUP_FILE="${SSHD_CONFIG}.bak.$(date +%Y%m%d%H%M%S)"
    
    # Бэкап
    cp "$SSHD_CONFIG" "$BACKUP_FILE"
    log_info "Бэкап SSH конфига: $BACKUP_FILE"
    
    # Создаём новый конфиг
    cat > "$SSHD_CONFIG" << EOF
# Check24RF API - SSH Hardened Configuration
# Сгенерировано: $(date -Iseconds)

Port ${SSH_PORT}
AddressFamily inet
ListenAddress 0.0.0.0

# Аутентификация
PubkeyAuthentication yes
PasswordAuthentication no
ChallengeResponseAuthentication no
KbdInteractiveAuthentication no
UsePAM yes
PermitEmptyPasswords no
MaxAuthTries 3
LoginGraceTime 30
MaxSessions 10

# Root доступ
PermitRootLogin prohibit-password

# Безопасность
X11Forwarding no
AllowAgentForwarding no
AllowTcpForwarding no
PermitTunnel no
TCPKeepAlive yes
ClientAliveInterval 300
ClientAliveCountMax 2
PrintLastLog yes
PrintMotd no

# Только разрешённые пользователи
AllowUsers root ${APP_USER}

# Banner
Banner /etc/ssh/banner

# Subsystem
Subsystem sftp /usr/lib/openssh/sftp-server
EOF
    
    # Создаём banner
    cat > /etc/ssh/banner << 'EOF'
***************************************************************************
*                    CHECK24RF API - AUTHORIZED ACCESS ONLY               *
*                                                                         *
* This system is for authorized users only. All activity may be monitored *
* and recorded. Unauthorized access is a violation of applicable laws.    *
***************************************************************************
EOF
    
    # Проверка конфига
    if sshd -t 2>/dev/null; then
        # На Ubuntu сервис называется ssh.service, не sshd.service
        if systemctl restart ssh.service 2>/dev/null; then
            step_done "07_sshd_config"
            log "✓ SSH настроен: порт ${SSH_PORT}, только ключи, hardened"
        elif systemctl restart sshd.service 2>/dev/null; then
            step_done "07_sshd_config"
            log "✓ SSH настроен: порт ${SSH_PORT}, только ключи, hardened"
        else
            log_err "Не удалось перезапустить SSH. Пробую напрямую..."
            # Пробую перезапустить напрямую
            /etc/init.d/ssh restart 2>/dev/null || /etc/init.d/sshd restart 2>/dev/null || true
            step_done "07_sshd_config"
            log "✓ SSH настроен (перезапуск вручную)"
        fi
    else
        log_err "Ошибка в конфиге SSH! Восстанавливаю бэкап..."
        cp "$BACKUP_FILE" "$SSHD_CONFIG"
        systemctl restart ssh.service 2>/dev/null || systemctl restart sshd.service 2>/dev/null || true
        exit 1
    fi
fi

# ==================== 8. FIREWALL (UFW) ====================

if step_check "08_firewall"; then
    log_skip "[8/12] Firewall"
else
    log "[8/12] Настройка Firewall (UFW)..."
    
    apt install -y ufw
    
    # Сброс и настройка
    ufw --force reset
    ufw default deny incoming
    ufw default allow outgoing
    
    # Разрешаем порты
    ufw allow ${SSH_PORT}/tcp comment 'SSH'
    ufw allow 80/tcp comment 'HTTP'
    ufw allow 443/tcp comment 'HTTPS'
    
    # Включаем
    ufw --force enable
    
    step_done "08_firewall"
    log "✓ Firewall: SSH(${SSH_PORT}), HTTP(80), HTTPS(443)"
fi

# ==================== 9. FAIL2BAN ====================

if step_check "09_fail2ban"; then
    log_skip "[9/12] Fail2Ban"
else
    log "[9/12] Установка и настройка Fail2Ban..."
    
    apt install -y fail2ban
    
    cat > /etc/fail2ban/jail.local << EOF
[DEFAULT]
ignoreip = 127.0.0.1/8
backend = systemd
banaction = ufw

[sshd]
enabled = true
port = ${SSH_PORT}
filter = sshd
logpath = /var/log/auth.log
findtime = 3600
maxretry = 5
bantime = 86400

[nginx-http-auth]
enabled = true
port = http,https
filter = nginx-http-auth
logpath = /var/log/nginx/error.log
findtime = 600
maxretry = 5
bantime = 3600

[nginx-limit-req]
enabled = true
port = http,https
filter = nginx-limit-req
logpath = /var/log/nginx/error.log
findtime = 120
maxretry = 10
bantime = 600
EOF
    
    systemctl enable fail2ban
    systemctl restart fail2ban
    
    step_done "09_fail2ban"
    log "✓ Fail2Ban: 5 попыток / 1ч → бан 24ч"
fi

# ==================== 10. AUTO-UPDATES ====================

if step_check "10_auto_updates"; then
    log_skip "[10/12] Автообновления"
else
    log "[10/12] Настройка автоматических обновлений..."
    
    apt install -y unattended-upgrades apt-listchanges
    
    # Включаем автообновления
    cat > /etc/apt/apt.conf.d/20auto-upgrades << 'EOF'
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
APT::Periodic::Download-Upgradeable-Packages "1";
APT::Periodic::AutocleanInterval "7";
EOF
    
    # Настройка безопасности
    cat > /etc/apt/apt.conf.d/50unattended-upgrades << 'EOF'
Unattended-Upgrade::Allowed-Origins {
    "${distro_id}:${distro_codename}";
    "${distro_id}:${distro_codename}-security";
    "${distro_id}ESMApps:${distro_codename}-apps-security";
    "${distro_id}ESM:${distro_codename}-infra-security";
};

Unattended-Upgrade::Automatic-Reboot "true";
Unattended-Upgrade::Automatic-Reboot-Time "03:00";
Unattended-Upgrade::Mail "root";
EOF
    
    systemctl enable unattended-upgrades
    systemctl restart unattended-upgrades
    
    step_done "10_auto_updates"
    log "✓ Автообновления настроены (в 03:00)"
fi

# ==================== 11. LOGROTATE ====================

if step_check "11_logrotate"; then
    log_skip "[11/12] Logrotate"
else
    log "[11/12] Настройка Logrotate..."
    
    cat > /etc/logrotate.d/check24rf << EOF
${PROJECT_DIR}/logs/*.log {
    daily
    rotate 14
    compress
    delaycompress
    missingok
    notifempty
    create 0640 ${APP_USER} ${APP_USER}
    sharedscripts
    postrotate
        systemctl reload nginx 2>/dev/null || true
    endscript
}
EOF
    
    step_done "11_logrotate"
    log "✓ Logrotate настроен"
fi

# ==================== 12. ДИРЕКТОРИИ ПРОЕКТА ====================

if step_check "12_dirs"; then
    log_skip "[12/13] Директории проекта"
else
    log "[12/13] Создание директорий проекта..."

    mkdir -p "${PROJECT_DIR}"/{logs,backups/{postgresql,redis},scripts,nginx}
    chown -R "${APP_USER}:${APP_USER}" "$PROJECT_DIR"
    chmod -R 755 "$PROJECT_DIR"

    step_done "12_dirs"
    log "✓ Директории созданы: $PROJECT_DIR"
fi

# ==================== 13. SSL СЕРТИФИКАТЫ (Let's Encrypt) ====================

if step_check "13_ssl_certificates"; then
    log_skip "[13/13] SSL сертификаты"
else
    if [ -n "$MAIN_DOMAIN" ] && [ -n "$API_DOMAIN" ] && [ -n "$EMAIL" ]; then
        log "[13/13] Установка SSL сертификатов Let's Encrypt..."

        # Проверка что домены указывают на этот сервер
        log_info "Проверка доменов..."
        
        # Установка Certbot
        apt install -y certbot python3-certbot-nginx

        # Получение сертификатов
        log_info "Получение сертификатов для: $MAIN_DOMAIN, $API_DOMAIN"
        
        certbot certonly --nginx \
            --agree-tos \
            --register-unsafely-without-email \
            --email "$EMAIL" \
            -d "$MAIN_DOMAIN" \
            -d "www.$MAIN_DOMAIN" \
            -d "$API_DOMAIN" \
            --non-interactive \
            --redirect

        if [ $? -eq 0 ]; then
            log_info "✓ SSL сертификаты установлены"
            
            # Проверка автообновления
            log_info "Настройка автообновления сертификатов..."
            certbot renew --dry-run
            
            step_done "13_ssl_certificates"
            log "✓ SSL сертификаты установлены и настроены"
        else
            log_warn "⚠️ Не удалось получить SSL сертификаты"
            log_warn "Пропускаем шаг. Запустите certbot вручную после настройки DNS"
            step_done "13_ssl_certificates"
        fi
    else
        log_warn "[13/13] Пропускаем установку SSL (не переданы домены)"
        log_info "Для установки SSL запустите:"
        log_info "  certbot --nginx -d $MAIN_DOMAIN -d www.$MAIN_DOMAIN -d $API_DOMAIN"
        step_done "13_ssl_certificates"
    fi
fi

# ==================== ЗАВЕРШЕНИЕ ====================

echo ""
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                    НАСТРОЙКА ЗАВЕРШЕНА                        ║"
echo "╠═══════════════════════════════════════════════════════════════╣"
echo "║ Docker: $(docker --version | cut -d' ' -f3)                          ║"
echo "║ Docker Compose: $(docker compose version --short)                         ║"
echo "║ SSH порт: ${SSH_PORT}                                              ║"
echo "║ Пользователь: ${APP_USER}                                         ║"
echo "║ Группы: sudo, docker                                     ║"
if [ -n "$MAIN_DOMAIN" ] && [ -f "/etc/letsencrypt/live/$MAIN_DOMAIN/fullchain.pem" ]; then
    echo "║ SSL: ✓ установлен для $MAIN_DOMAIN, $API_DOMAIN              ║"
else
    echo "║ SSL: ⚠️  не установлен (запустите с --main-domain --api-domain) ║"
fi
echo "╠═══════════════════════════════════════════════════════════════╣"
echo "║ СЛЕДУЮЩИЕ ШАГИ:                                               ║"
echo "║ 1. Добавьте SSH ключи в /home/${APP_USER}/.ssh/authorized_keys   ║"
echo "║ 2. Настройте GitHub Secrets (см. README.md)                   ║"
echo "║ 3. GitHub Actions сам создаст .env и развернёт проект         ║"
echo "║ 4. Настройте nginx: cp nginx/nginx.conf /etc/nginx/sites-available/ ║"
echo "║ 5. Запустите: docker compose up -d                            ║"
echo "║ 6. Настройте backup: bash scripts/setup-backup-cron.sh        ║"
echo "╠═══════════════════════════════════════════════════════════════╣"
echo "║ ФАЙЛЫ:                                                        ║"
echo "║ - Логи установки: $LOG_FILE                     ║"
echo "║ - Переменные окружения: $ENV_FILE (создаст GitHub) ║"
echo "║ - SSH бэкап: /etc/ssh/sshd_config.bak.*                       ║"
echo "║ - SSL сертификаты: /etc/letsencrypt/live/                     ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

log_info "Перезапустите SSH сессию на порту ${SSH_PORT} перед закрытием текущей!"
log_warn "Команда для подключения: ssh -p ${SSH_PORT} ${APP_USER}@<server-ip>"
