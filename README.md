# Distributed E-Commerce Platform

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen.svg" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2021.0.5.0-blue.svg" alt="Spring Cloud Alibaba"/>
  <img src="https://img.shields.io/badge/Java-11-orange.svg" alt="Java"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-blue.svg" alt="MySQL"/>
  <img src="https://img.shields.io/badge/Redis-7.0-red.svg" alt="Redis"/>
  <img src="https://img.shields.io/badge/RabbitMQ-3.12-orange.svg" alt="RabbitMQ"/>
</p>

A microservices-based e-commerce platform covering user management, product catalog, and order processing.

## Features

- **Microservices architecture**: user, product, order services independently deployed
- **Atomic stock deduction**: Redis Lua script eliminates overselling without blocking concurrent requests
- **Idempotent order creation**: Redis token mechanism prevents duplicate submissions
- **Order timeout cancellation**: RabbitMQ dead-letter queue auto-cancels unpaid orders after 30 minutes
- **Service communication**: OpenFeign for inter-service calls with eventual consistency
- **API Gateway**: Spring Cloud Gateway with unified JWT authentication and IP-based rate limiting
- **Cache strategy**: Cache-Aside pattern for product data (30-min TTL)
- **Full-stack frontend**: Vue 3 + Element Plus SPA

## Tech Stack

| Technology | Version | Purpose |
|------|------|------|
| Spring Boot | 2.7.18 | Application framework |
| Spring Cloud | 2021.0.8 | Microservices framework |
| Spring Cloud Alibaba | 2021.0.5.0 | Nacos integration |
| Nacos | 2.2.3 | Service discovery & config center |
| Spring Cloud Gateway | - | API gateway |
| OpenFeign | - | Declarative HTTP client |
| MyBatis Plus | 3.5.3.1 | ORM |
| MySQL | 8.0 | Relational database |
| Redis | 7.0 | Cache & atomic operations |
| RabbitMQ | 3.12 | Message queue |
| Docker | - | Containerized deployment |
| Vue 3 | - | Frontend framework |

## Architecture

```
┌─────────────┐
│   Browser   │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────┐
│          API Gateway (8080)             │
│  Routing / JWT Auth / Rate Limiting     │
└────┬────────────┬────────────┬─────────┘
     │            │            │
     ▼            ▼            ▼
┌─────────┐  ┌─────────┐  ┌─────────┐
│  User   │  │ Product │  │  Order  │
│ (8081)  │  │ (8082)  │  │ (8083)  │
└────┬────┘  └────┬────┘  └────┬────┘
     │            │            │
     └────────────┴────────────┘
                  │
     ┌────────────┼────────────┐
     ▼            ▼            ▼
┌────────┐   ┌────────┐   ┌────────┐
│  MySQL │   │  Redis │   │RabbitMQ│
└────────┘   └────────┘   └────────┘
```

Each service owns an independent database (`ecommerce_user`, `ecommerce_product`, `ecommerce_order`).

## Project Structure

```
distributed-ecommerce/
├── ecommerce-common/          # Shared module
│   ├── exception/            # Global exception handling
│   ├── result/               # Unified response wrapper
│   ├── utils/                # JWT, Redis lock utilities
│   └── constant/             # Redis key constants
├── ecommerce-gateway/         # API gateway (8080)
│   ├── filter/               # JWT auth filter
│   └── config/               # Rate limiter, CORS
├── ecommerce-user/            # User service (8081)
│   ├── controller/           # User & address APIs
│   ├── service/              # Auth, address logic
│   ├── mapper/               # Data access
│   └── entity/               # Entities
├── ecommerce-product/         # Product service (8082)
│   ├── controller/           # Product APIs
│   ├── service/              # Stock management, caching
│   └── mapper/               # Data access
├── ecommerce-order/           # Order service (8083)
│   ├── controller/           # Order APIs
│   ├── service/              # Order lifecycle, idempotency
│   ├── feign/                # Feign clients
│   └── mq/                   # RabbitMQ producer/consumer
├── ecommerce-frontend/        # Vue 3 SPA (3000)
│   └── src/
│       ├── api/              # API wrappers
│       ├── views/            # Page components
│       ├── router/           # Route guards
│       └── utils/            # Axios interceptors
├── docker/mysql/init/         # Database init scripts
├── docker-compose.yml         # Container orchestration
├── Dockerfile                 # Multi-stage build
└── pom.xml                    # Maven parent
```

## Quick Start

### Prerequisites

- JDK 11+
- Maven 3.6+
- Docker & Docker Compose

### 1. Start infrastructure & services

```bash
docker-compose up -d
```

This starts all containers: MySQL, Redis, RabbitMQ, Nacos, and the four Spring Boot services.

### 2. Verify services registered

Open http://localhost:8848/nacos (nacos/nacos) and confirm all four services are registered.

### 3. Start frontend (optional)

```bash
cd ecommerce-frontend
npm install
npm run dev
```

Open http://localhost:3000. Test account: `testuser` / `123456`.

### 4. API testing

All requests go through the gateway at `http://localhost:8080/api`. See [QUICKSTART.md](QUICKSTART.md) for detailed API examples.

## Key Implementations

### Atomic stock deduction (Redis Lua)

**Location**: `ProductServiceImpl.java`

```java
String lua =
    "local v = redis.call('GET', KEYS[1])\n" +
    "if v == false then return -1 end\n" +
    "if tonumber(v) >= tonumber(ARGV[1]) then\n" +
    "    redis.call('DECRBY', KEYS[1], ARGV[1])\n" +
    "    return 1\n" +
    "end\n" +
    "return 0";
```

Check-and-deduct executes atomically inside Redis, serialized by its single-threaded event loop. MySQL `UPDATE ... WHERE stock >= ?` acts as a second line of defense, with `INCRBY` rollback if the DB write fails.

### Idempotent order creation

**Location**: `OrderServiceImpl.java`

```java
Boolean deleted = redisTemplate.delete(tokenKey);
if (!Boolean.TRUE.equals(deleted)) {
    throw new BusinessException(ResultCode.DUPLICATE_REQUEST);
}
```

A UUID token is issued before checkout and consumed (deleted) at order creation. The atomic DELETE acts as both validation and consumption: first request succeeds, retries are rejected.

### Order timeout auto-cancellation

**Location**: `RabbitMQConfig.java`, `OrderMessageConsumer.java`

```java
// Queue with dead-letter routing, no consumer
QueueBuilder.durable(ORDER_TIMEOUT_QUEUE)
    .withArgument("x-dead-letter-exchange", ORDER_TIMEOUT_DLX_EXCHANGE)
    .withArgument("x-dead-letter-routing-key", ORDER_TIMEOUT_DLX_ROUTING_KEY)
    .build();

// Consumer on the dead-letter queue checks status
if (order.getStatus() == 0) {
    orderService.cancelOrder(orderNo);
}
```

Messages expire after 30 minutes, are routed to the dead-letter queue, and the consumer cancels unpaid orders with stock restoration via Feign. Manual ACK ensures no message loss.

### JWT authentication

**Location**: `AuthFilter.java`, `JwtUtil.java`

HS256-signed tokens (7-day expiry) issued at login, validated at the gateway. Whitelisted paths (login/register/product listing) bypass auth.

## Roadmap

- [ ] Elasticsearch product search
- [ ] Shopping cart
- [ ] Coupon system
- [ ] Payment gateway integration
- [ ] Distributed tracing

## License

MIT
