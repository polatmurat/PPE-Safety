# PPE Safety API - VDS Deployment Guide

## Senin VDS'indeki Mevcut Setup

Görüyorum ki şu konteynerler çalışıyor:
- `jipflix-postgres-1` (PostgreSQL 16) - port 5432
- `jipflix-redis-1` (Redis 7) - port 6379
- Gateway port 8080'de

Bu servisleri yeniden kullanacağız! 🎉

---

## Adım 1: Projeyi VDS'e Yükle

```bash
# Lokalden VDS'e kopyala
scp -r backend-spring/ root@srv788657:~/ppe-safety/

# VEYA git ile
ssh root@srv788657
cd ~
git clone <your-repo-url> ppe-safety
```

---

## Adım 2: PostgreSQL'de Database Oluştur

```bash
# VDS'e bağlan
ssh root@srv788657

# Mevcut postgres konteynerine bağlan ve database oluştur
docker exec -it jipflix-postgres-1 psql -U postgres -c "CREATE DATABASE ppesafety;"

# Kontrol et
docker exec -it jipflix-postgres-1 psql -U postgres -c "\l" | grep ppesafety
```

---

## Adım 3: Network'ü Kontrol Et

```bash
# jipflix network'ünü bul
docker network ls | grep jipflix

# Muhtemelen "jipflix_default" olarak görünecek
# Eğer yoksa, oluştur:
docker network create jipflix_default
```

---

## Adım 4: .env Dosyası Oluştur

```bash
cd ~/ppe-safety

# .env dosyası oluştur
cat > .env << 'EOF'
POSTGRES_PASSWORD=senin_jipflix_postgres_sifresi
JWT_SECRET=mur4th4z4r
LOG_LEVEL=INFO
EOF
```

---

## Adım 5: Build ve Deploy

```bash
cd ~/ppe-safety

# Basit deployment (mevcut postgres/redis kullan)
docker-compose -f docker-compose.prod.yml up -d --build

# Logları izle
docker logs -f ppe-safety-api
```

---

## Adım 6: Kontrol Et

```bash
# Health check
curl http://localhost:8090/actuator/health

# Swagger UI aç (tarayıcıda)
# http://YOUR_VDS_IP:8090/swagger-ui.html

# Login test
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

---

## Hızlı Komutlar

```bash
# Durumu kontrol et
docker ps | grep ppe

# Logları gör
docker logs ppe-safety-api

# Yeniden başlat
docker-compose -f docker-compose.prod.yml restart

# Durdur
docker-compose -f docker-compose.prod.yml down

# Rebuild (kod değişikliğinde)
docker-compose -f docker-compose.prod.yml up -d --build
```

---

## Port Yapılandırması

| Servis | Port | Açıklama |
|--------|------|----------|
| PPE Safety API | 8090 | Yeni API |
| jipflix-gateway | 8080 | Mevcut |
| PostgreSQL | 5432 (internal) | jipflix-postgres-1 |
| Redis | 6379 (internal) | jipflix-redis-1 |

---

## Sorun Giderme

### Database bağlantı hatası
```bash
# Network'ü kontrol et
docker network inspect jipflix_default | grep ppe-safety-api

# Postgres'e ping at
docker exec ppe-safety-api ping jipflix-postgres-1
```

### Redis bağlantı hatası
```bash
# Redis'e bağlan
docker exec -it jipflix-redis-1 redis-cli ping
```

### Uygulama başlamıyor
```bash
# Detaylı log
docker logs ppe-safety-api 2>&1 | tail -100
```
