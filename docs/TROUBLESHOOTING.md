# Troubleshooting Guide

Enterprise Commerce & Logistics Platform의 로컬 개발, Docker Compose 실행, 메시지 처리 및 Kubernetes 배포 과정에서 발생할 수 있는 문제를 진단하고 복구하기 위한 가이드다.

## 1. 기본 진단 순서

문제가 발생하면 구성 요소를 무작정 재시작하기 전에 다음 순서로 상태와 증거를 수집한다.

```bash
docker compose ps
docker compose logs --tail=200 app
docker compose logs --tail=200 mysql kafka redis mongo
curl -i http://localhost:8080/actuator/health
```

확인할 항목:

1. 문제가 애플리케이션 시작, API 요청, 이벤트 전달, 조회 중 어느 단계에서 발생했는가?
2. 마지막으로 성공한 단계는 무엇인가?
3. 반복 가능한가, 간헐적인가?
4. 최초 발생 시각과 관련 로그의 시각이 일치하는가?
5. 재시작 전에 로그, 요청, 응답, 컨테이너 상태를 보존했는가?

## 2. Docker와 시작 문제

### `docker` 또는 `docker compose` 명령을 찾을 수 없음

Docker Desktop 또는 Docker Engine과 Compose v2 설치 여부를 확인한다.

```bash
docker version
docker compose version
```

Windows에서는 Docker Desktop이 실행 중인지, WSL 2 엔진이 활성화됐는지 확인한다. 새로 설치했다면 터미널을 다시 연다.

### 컨테이너가 `unhealthy` 또는 재시작 상태

```bash
docker compose ps
docker inspect --format='{{json .State.Health}}' <container-name>
docker compose logs --tail=300 <service-name>
```

주요 원인은 초기 이미지 다운로드 실패, 메모리 부족, 포트 충돌, 데이터 volume에 남은 이전 설정이다. 로그 없이 `down -v`부터 실행하지 않는다.

### 이미지 다운로드 실패

오류에 `timeout`, `TLS`, `proxy`, `unauthorized`, `rate limit`가 있는지 확인한다.

- 회사 프록시와 Docker daemon 프록시 설정을 확인한다.
- 사내 registry mirror 사용 여부를 확인한다.
- registry 로그인이나 VPN이 필요한지 확인한다.
- 잠시 후 `docker compose pull`로 다시 시도한다.

### 포트 충돌

기본 포트는 8080, 3306, 6379, 9092, 27017이다. 기존 프로세스나 컨테이너가 사용 중인지 확인한다.

Windows:

```powershell
Get-NetTCPConnection -State Listen | Where-Object LocalPort -In 8080,3306,6379,9092,27017
```

macOS/Linux:

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
```

기존 서비스를 종료하거나 `compose.yml`의 호스트 포트를 변경한다. 애플리케이션을 Docker 외부에서 실행한다면 연결 주소도 같은 포트로 변경한다.

## 3. 애플리케이션 시작 실패

### `Connection refused` 또는 데이터 서비스 연결 실패

```bash
docker compose ps
docker compose logs --tail=200 mysql kafka redis mongo
```

연결 주소는 애플리케이션 실행 위치에 따라 달라진다.

| 실행 위치 | MySQL host | Kafka | Redis host | Mongo host |
|---|---|---|---|---|
| 로컬 JVM | `localhost` | `localhost:9092` | `localhost` | `localhost` |
| Compose 내부 | `mysql` | `kafka:29092` | `redis` | `mongo` |

Compose 내부에서 `localhost`는 다른 컨테이너가 아니라 현재 컨테이너 자신을 의미한다.

### Flyway migration 실패

로그에서 실패한 migration과 SQL 오류를 먼저 확인한다.

- 이미 적용된 migration 파일은 수정하지 않는다.
- 변경이 필요하면 다음 버전의 migration을 추가한다.
- 로컬 실습 데이터만 있는 경우에 한해 volume 초기화를 고려한다.
- 운영 데이터베이스에서는 `repair`, 수동 SQL, 스키마 삭제를 임의로 실행하지 않는다.

로컬 데이터를 완전히 버려도 되는 경우:

```bash
docker compose down -v
docker compose up --build -d
```

`-v`는 MySQL, Redis, MongoDB volume 데이터를 삭제한다.

### 애플리케이션 health가 `DOWN`

```bash
curl -s http://localhost:8080/actuator/health
docker compose logs --tail=200 app
```

health 응답의 하위 구성 요소를 확인한다. 운영 환경에서 상세 health 정보는 외부에 공개하지 않는다.

## 4. 주문 API 문제

### 주문 생성이 HTTP 400

요청 형식과 validation 조건을 확인한다.

```json
{
  "customerId": "local-user",
  "sku": "SKU-001",
  "quantity": 2
}
```

- `customerId`, `sku`는 빈 문자열일 수 없다.
- `quantity`는 1 이상 1000 이하여야 한다.
- `Content-Type: application/json` 헤더를 전송한다.

### 주문 생성이 HTTP 409

응답의 `detail`을 확인한다.

- `Unknown sku`: 초기 데이터에 없는 SKU다.
- `Insufficient stock`: 요청 수량보다 재고가 적다.

재고 부족은 정상적인 도메인 거절이며 시스템 장애로 분류하지 않는다.

### 같은 재고에 대한 요청이 느림

현재 구현은 재고 행에 비관적 잠금을 사용한다. 동일 SKU에 요청이 집중되면 잠금 대기가 발생할 수 있다.

확인 항목:

- 특정 SKU에만 지연이 집중되는가?
- MySQL lock wait 또는 deadlock 로그가 있는가?
- API p95/p99 지연과 DB connection pool 대기가 함께 증가하는가?

해결 후보는 재고 예약 분할, optimistic retry, queue 기반 직렬화다. 측정 없이 잠금 전략을 변경하지 않는다.

## 5. Kafka와 Outbox 문제

### 주문은 생성되지만 배송 조회가 계속 404

데이터 흐름을 단계별로 확인한다.

```text
MySQL 주문/Outbox → Outbox publisher → Kafka → Consumer → Redis → MongoDB
```

```bash
docker compose logs --tail=300 app kafka redis mongo
```

확인 순서:

1. 주문 POST가 201을 반환했는가?
2. MySQL에 Outbox 이벤트가 생성됐는가?
3. Outbox의 `published_at`이 채워졌는가?
4. Kafka broker가 healthy 상태인가?
5. consumer 역직렬화 또는 처리 예외가 있는가?
6. MongoDB에 해당 `orderId` projection이 저장됐는가?

### Outbox 이벤트가 미발행 상태로 남음

Kafka 연결과 advertised listener 설정을 확인한다.

- 로컬 JVM: `KAFKA_BOOTSTRAP_SERVERS=localhost:9092`
- Compose app: `KAFKA_BOOTSTRAP_SERVERS=kafka:29092`

Kafka가 복구되면 polling publisher가 다시 시도한다. 레코드를 수동으로 published 처리하지 않는다.

### 이벤트가 중복 처리됨

Outbox 방식은 at-least-once 전달이므로 중복 자체는 발생할 수 있다. 소비자는 Redis의 `event:processed:{eventId}` 키로 중복을 차단한다.

확인 항목:

- 동일한 논리 이벤트가 같은 `eventId`를 사용하는가?
- Redis가 재시작되거나 키가 만료됐는가?
- 처리 실패 시 멱등성 키가 삭제됐는가?
- 서로 다른 consumer가 동일 Redis를 사용하는가?

현재 Redis와 MongoDB 쓰기는 하나의 트랜잭션이 아니다. 더 강한 보장이 필요하면 inbox table, Kafka EOS 또는 영속 소비 이력을 확장 과제로 검토한다.

### Consumer lag 증가

원인 후보:

- MongoDB 쓰기 지연
- Redis 연결 지연
- consumer 처리 예외와 반복 재시도
- partition 수보다 적은 유효 consumer
- producer 유입량이 소비 처리량보다 큼

lag, 처리 시간, 오류율을 함께 확인한다. partition만 늘리기 전에 consumer 병목을 측정한다.

## 6. Redis 문제

### Redis 연결 실패

```bash
docker compose ps redis
docker compose logs --tail=200 redis
docker compose exec redis redis-cli ping
```

정상 응답은 `PONG`이다. 로컬 JVM은 `localhost`, Compose app은 `redis`를 host로 사용한다.

### 멱등성 키가 예상보다 빨리 사라짐

기본 보관 기간은 7일이다. Redis eviction 정책과 메모리 사용량을 함께 확인한다. 이벤트 재처리 최대 기간보다 TTL이 짧으면 중복 projection이 발생할 수 있다.

## 7. MongoDB 문제

### 인증 실패

기본 URI의 `authSource=admin` 포함 여부와 계정을 확인한다.

```text
mongodb://commerce:commerce@localhost:27017/logistics?authSource=admin
```

기존 volume이 있는 상태에서 Compose 환경 변수만 바꿔도 최초 생성 계정은 자동 변경되지 않는다.

### projection이 저장되지 않음

consumer 로그에서 MongoDB 쓰기 예외를 확인한다. Redis 멱등성 키가 남았지만 projection이 없다면 처리 실패 경계일 수 있다. 현재 구현은 저장 실패 시 키 삭제를 시도하지만 네트워크 단절 경계에서는 수동 재처리 또는 운영 확장이 필요할 수 있다.

## 8. 테스트와 빌드 문제

### Java 또는 Maven 버전 오류

```bash
java -version
mvn -version
```

JDK 21 이상과 Maven 3.9 이상을 사용한다. `mvn -version`이 표시하는 Java runtime도 21인지 확인한다.

### 의존성 다운로드 실패

- Maven proxy와 `settings.xml`을 확인한다.
- 사내 artifact repository URL과 인증을 확인한다.
- 일시적 네트워크 문제인지 `mvn -U verify`로 다시 확인한다.
- 임의로 인증서 검증을 비활성화하지 않는다.

### 테스트는 성공하지만 Docker build가 실패

Docker build는 깨끗한 환경에서 전체 의존성을 다시 해결한다. 다음을 확인한다.

- private repository 자격 증명 필요 여부
- Docker daemon 네트워크 및 프록시
- 디스크 여유 공간
- 멀티스테이지 build의 JDK/아키텍처 호환성

## 9. Kubernetes 문제

### manifest 렌더링 실패

```bash
kubectl kustomize k8s/base
```

`kustomization.yaml`의 경로와 참조 파일 존재 여부를 확인한다.

### Pod가 `ImagePullBackOff`

- 이미지 주소와 tag가 실제 registry에 존재하는지 확인한다.
- private registry라면 image pull secret을 구성한다.
- 클러스터 node에서 registry에 접근 가능한지 확인한다.

### Pod가 `CrashLoopBackOff`

```bash
kubectl describe pod <pod-name>
kubectl logs <pod-name> --previous
kubectl get events --sort-by=.lastTimestamp
```

이전 컨테이너 로그, 환경 변수, Secret/ConfigMap 참조, 데이터 서비스 연결을 확인한다.

### Readiness probe 실패

```bash
kubectl port-forward deployment/commerce-logistics 8080:8080
curl -i http://localhost:8080/actuator/health/readiness
```

애플리케이션 시작 시간이 probe의 초기 대기 시간을 초과하는지, 의존 서비스 실패가 readiness에 반영됐는지 확인한다. 원인을 숨기기 위해 probe를 제거하지 않는다.

### HPA가 확장되지 않음

```bash
kubectl get hpa
kubectl describe hpa commerce-logistics
kubectl top pods
```

Metrics Server 설치 여부와 Pod의 CPU request 설정을 확인한다.

## 10. 장애 보고 템플릿

```text
제목:
최초 발생 시각/시간대:
환경과 버전:
영향 범위:
재현 절차:
기대 결과:
실제 결과:
HTTP 상태/응답:
관련 로그와 trace ID:
컨테이너 또는 Pod 상태:
마지막 정상 시점:
최근 변경 사항:
시도한 조치와 결과:
```

비밀번호, 토큰, 개인정보, 전체 접속 문자열은 보고서와 로그에 포함하지 않는다.

## 11. 복구 후 확인

- 주문 생성과 조회가 성공한다.
- 재고가 정확히 한 번 감소한다.
- Outbox 이벤트가 Kafka로 발행된다.
- 동일 이벤트 재전달이 중복 projection을 만들지 않는다.
- 배송 조회가 최종적으로 `READY_TO_SHIP`을 반환한다.
- health, 오류율, p95/p99 지연, consumer lag가 정상 범위로 돌아온다.
- 원인, 임시 조치, 영구 개선 과제를 기록한다.

