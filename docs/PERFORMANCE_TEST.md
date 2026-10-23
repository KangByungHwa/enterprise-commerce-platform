# Performance Test Guide

Enterprise Commerce & Logistics Platform의 처리량, 응답 시간, 오류율, 재고 정합성과 비동기 이벤트 지연을 재현 가능한 방식으로 측정하기 위한 가이드다.

## 1. 목표

성능 테스트는 단순 최대 TPS 경쟁이 아니라 다음 질문에 답해야 한다.

1. 주문 API가 목표 부하에서 허용 가능한 응답 시간을 유지하는가?
2. 동시 주문에서도 재고가 음수가 되거나 초과 판매되지 않는가?
3. Kafka consumer가 유입량을 따라가며 배송 projection을 적시에 만드는가?
4. MySQL 잠금, connection pool, Kafka, Redis, MongoDB 중 실제 병목은 어디인가?
5. 장애와 복구 과정에서 Outbox와 멱등성 보장이 유지되는가?

## 2. 테스트 범위와 제외 범위

### 포함

- `POST /api/orders`
- `GET /api/orders/{orderId}`
- `GET /api/shipments/{orderId}`
- MySQL 주문·재고·Outbox 처리
- Kafka 발행과 소비
- Redis 멱등성 확인
- MongoDB 배송 projection 생성

### 기본 테스트에서 제외

- 인터넷 구간과 CDN
- 실제 결제 시스템
- OAuth/OIDC 인증 서버
- 외부 managed service의 SLA 검증
- 사용자 브라우저 렌더링 성능

제외 항목을 결과 보고서에 명시해 수치의 적용 범위를 분명하게 한다.

## 3. 성공 기준 예시

아래 값은 샘플 목표다. 실제 목표는 포트폴리오에서 가정한 트래픽과 실행 장비를 기준으로 확정한다.

| 지표 | 예시 기준 |
|---|---:|
| 주문 생성 성공률 | 99.5% 이상 |
| 주문 생성 p95 | 500ms 이하 |
| 주문 생성 p99 | 1,000ms 이하 |
| 주문 조회 p95 | 200ms 이하 |
| 배송 projection p95 지연 | 3초 이하 |
| 예상 외 HTTP 5xx | 0.1% 이하 |
| 재고 음수/초과 판매 | 0건 |
| 중복 배송 projection | 0건 |
| 테스트 종료 후 미발행 Outbox | 0건 |

재고 부족으로 발생하는 HTTP 409는 시나리오에 따라 정상 도메인 응답이다. 시스템 오류와 분리해 집계한다.

## 4. 테스트 환경 기록

결과 비교를 위해 매 실행마다 다음 정보를 기록한다.

- 날짜, 시작 시각, 시간대
- Git commit과 이미지 tag
- JDK, Spring Boot, MySQL, Kafka, Redis, MongoDB 버전
- 실행 장비 CPU, 메모리, 디스크 종류
- Docker 또는 Kubernetes 자원 제한
- 애플리케이션 replica 수
- DB connection pool 크기
- Kafka partition 수와 consumer 수
- 초기 SKU별 재고량
- 테스트 도구 버전과 스크립트 commit
- 다른 프로세스 또는 공유 클러스터 부하 여부

개발 노트북 결과와 클라우드 결과를 같은 기준선으로 직접 비교하지 않는다.

## 5. 사전 준비

### 기능 검증

부하를 주기 전에 정상 흐름을 먼저 확인한다.

```bash
docker compose up --build -d
curl http://localhost:8080/actuator/health
./scripts/smoke-test.sh
```

### 데이터 준비

성능 테스트 전용 SKU를 준비한다. 동시성 테스트를 제외한 일반 부하에서는 충분한 재고를 제공해 HTTP 409가 결과를 왜곡하지 않도록 한다.

권장 데이터 구분:

- `SKU-PERF-UNIQUE-*`: 경합이 낮은 일반 처리량 측정
- `SKU-PERF-HOT`: 동일 행 잠금 경합 측정
- `SKU-PERF-LIMITED`: 초과 판매 방지 검증

운영 데이터베이스나 공용 개발 데이터베이스에서 부하 테스트를 실행하지 않는다.

### 모니터링

최소한 다음 지표를 함께 수집한다.

- API 요청 수, 오류율, p50/p95/p99
- JVM heap, GC pause, thread 수
- CPU, memory, disk I/O, network
- HikariCP active/pending connection
- MySQL query latency, lock wait, deadlock
- Outbox 미발행 레코드 수와 최고 대기 시간
- Kafka producer 오류, consumer lag, 처리율
- Redis latency, memory, eviction
- MongoDB write latency와 connection 수

부하 도구 수치만으로 병목 원인을 판단하지 않는다.

## 6. 권장 테스트 단계

### 단계 1: Baseline

- VU 1~5
- 5분 실행
- 기능 오류가 없는지 확인
- 무부하 대비 기본 응답 시간을 기록

### 단계 2: Load test

- 예상 정상 트래픽까지 점진 증가
- 목표 부하에서 15~30분 유지
- 성공 기준 충족 여부 확인

### 단계 3: Stress test

- 오류율 또는 지연이 급격히 증가할 때까지 부하 증가
- 첫 병목과 포화 지점을 찾음
- 테스트 종료 후 정상 상태 회복 여부 확인

### 단계 4: Spike test

- 짧은 시간에 트래픽을 급증시킴
- connection pool, Kafka backlog, HPA 반응 관찰

### 단계 5: Soak test

- 정상 부하의 50~80%를 2시간 이상 유지
- 메모리 누수, connection 누수, lag 누적, 디스크 증가 확인

### 단계 6: Contention test

- 모든 사용자가 동일 SKU를 주문
- 비관적 잠금 대기, throughput 감소, 재고 정합성 확인

### 단계 7: Recovery test

- 부하 중 Kafka를 중단했다가 복구
- 주문 DB 트랜잭션은 유지되는지 확인
- Outbox backlog가 쌓였다가 모두 발행되는지 측정
- 중복 이벤트에도 projection이 한 번만 반영되는지 확인

## 7. k6 주문 부하 예제

아래 내용을 별도 `performance/order-load.js`로 저장해 사용할 수 있다.

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const unexpectedErrors = new Counter('unexpected_errors');
const orderLatency = new Trend('order_create_latency', true);

export const options = {
  scenarios: {
    orders: {
      executor: 'ramping-arrival-rate',
      startRate: 5,
      timeUnit: '1s',
      preAllocatedVUs: 30,
      maxVUs: 200,
      stages: [
        { target: 20, duration: '2m' },
        { target: 20, duration: '10m' },
        { target: 50, duration: '3m' },
        { target: 0, duration: '1m' },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.005'],
    order_create_latency: ['p(95)<500', 'p(99)<1000'],
    unexpected_errors: ['count==0'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const sku = __ENV.SKU || 'SKU-001';

export default function () {
  const payload = JSON.stringify({
    customerId: `perf-${__VU}-${__ITER}`,
    sku,
    quantity: 1,
  });

  const response = http.post(`${baseUrl}/api/orders`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { endpoint: 'create-order' },
  });

  orderLatency.add(response.timings.duration);
  const expected = check(response, {
    'order created': (r) => r.status === 201,
    'response contains id': (r) => {
      try { return Boolean(r.json('id')); } catch (_) { return false; }
    },
  });

  if (!expected && response.status !== 409) unexpectedErrors.add(1);
  sleep(0.1);
}
```

실행 예:

```bash
k6 run -e BASE_URL=http://localhost:8080 -e SKU=SKU-PERF-UNIQUE-1 performance/order-load.js
```

arrival-rate executor는 서버 응답이 느려져도 목표 요청률을 유지하려고 시도하므로 closed model보다 포화 현상을 관찰하기 쉽다. `dropped_iterations`가 발생하면 k6 자체 VU 부족 여부도 확인한다.

## 8. 배송 projection 지연 측정

주문 API 응답 시간만으로 비동기 흐름의 성능을 판단할 수 없다. 주문 생성 시각부터 `GET /api/shipments/{orderId}`가 200을 반환할 때까지 측정한다.

측정 시 주의 사항:

- polling 간격을 고정하고 결과에 기록한다.
- HTTP 404는 projection 생성 전의 예상 상태로 별도 집계한다.
- p50/p95/p99와 최대값을 기록한다.
- Kafka consumer lag와 MongoDB write latency를 같은 시간축으로 비교한다.
- 부하 종료 후 backlog가 0으로 복구되는 데 걸린 시간도 기록한다.

## 9. 재고 정합성 검증

초기 재고가 `N`이고 성공한 주문 수량 합계가 `S`라면 다음 조건을 만족해야 한다.

```text
최종 재고 = N - S
최종 재고 >= 0
성공 주문 수 = Outbox 이벤트 수
```

HTTP 409 요청은 성공 주문에 포함하지 않는다. 테스트 도구의 성공 수치만 믿지 말고 MySQL의 주문, 재고, Outbox를 함께 대조한다.

동일 SKU 경합 테스트에서는 다음을 기록한다.

- 성공 201 수
- 재고 부족 409 수
- 예상 외 5xx 수
- DB lock wait 시간
- API p95/p99
- deadlock 수

## 10. 장애 복구 시나리오

### Kafka 중단

1. 일정 부하를 시작한다.
2. Kafka를 중단한다.
3. 주문 성공률과 Outbox backlog를 관찰한다.
4. Kafka를 복구한다.
5. backlog가 0이 되는 시간과 중복 projection 여부를 확인한다.

```bash
docker compose stop kafka
docker compose start kafka
```

이 명령은 개인 로컬 환경에서만 실행한다.

### Consumer 처리 지연

MongoDB 또는 Redis에 인위적 지연을 주는 대신 별도 테스트 설정이나 fault injection 도구를 사용한다. 공용 환경의 네트워크를 임의로 차단하지 않는다.

### 애플리케이션 재시작

부하 중 app을 재시작하고 다음을 확인한다.

- 진행 중 요청의 실패율
- readiness 전환 시간
- 미발행 Outbox 보존 여부
- 복구 후 Kafka 재전송과 중복 차단

## 11. 결과 분석

### 응답 시간이 증가하지만 CPU가 낮음

DB lock, connection pool 대기, 외부 I/O, thread blocking을 확인한다. CPU가 낮다고 여유가 있다고 단정하지 않는다.

### p50은 안정적이지만 p99가 급증

GC pause, lock 경합, connection 생성, Kafka retry, 디스크 flush와 같은 간헐적 이벤트를 시간축으로 대조한다.

### API는 빠르지만 배송 projection이 느림

Outbox polling 간격, Kafka lag, Redis latency, MongoDB write 처리량을 확인한다. 동기 API 수치만으로 전체 사용자 경험을 평가하지 않는다.

### 처리량이 늘지 않고 lock wait가 증가

동일 SKU에 대한 비관적 잠금이 병목일 수 있다. 다음 대안을 별도 브랜치에서 비교한다.

- optimistic locking과 제한된 retry
- 재고 reservation 분할
- SKU 기준 partition과 queue 직렬화
- hot SKU 재고 bucket

정합성 검증 없이 처리량 개선만 채택하지 않는다.

## 12. 결과 보고서 템플릿

```markdown
# 성능 테스트 결과

## 요약
- 테스트 일시/시간대:
- Git commit / 이미지 tag:
- 테스트 목적:
- 최종 판정: PASS / FAIL

## 환경
- 부하 발생기:
- 애플리케이션 자원과 replica:
- MySQL/Kafka/Redis/MongoDB 구성:
- 주요 설정:

## 시나리오
- executor와 요청률:
- ramp-up / steady / ramp-down:
- 총 실행 시간:
- 초기 데이터:

## 결과
| 지표 | 목표 | 결과 | 판정 |
|---|---:|---:|---|
| 주문 성공률 | 99.5% | | |
| 주문 p95 | 500ms | | |
| 주문 p99 | 1,000ms | | |
| projection p95 | 3초 | | |
| 예상 외 5xx | 0.1% | | |

## 자원 및 병목
- CPU/메모리/GC:
- DB connection/lock:
- Kafka lag:
- Redis/MongoDB latency:

## 정합성 검증
- 초기/최종 재고:
- 성공 주문 수:
- Outbox 이벤트 수:
- 중복 projection 수:

## 결론과 다음 실험
- 확인된 병목:
- 변경 후보:
- 다음 테스트:
```

## 13. 포트폴리오에 남길 증거

- 실행 환경과 commit이 포함된 결과 문서
- 부하 단계가 표시된 p95/p99 그래프
- JVM, DB lock, Kafka lag의 동일 시간축 그래프
- 개선 전후 동일 시나리오 비교표
- 재고 정합성 검증 SQL 결과
- Kafka 중단과 복구 시 Outbox backlog 그래프
- 실패한 실험과 채택하지 않은 대안의 이유

절대 TPS 숫자보다 병목을 어떻게 측정하고, 정합성을 보존하면서 개선했는지 설명하는 것이 포트폴리오 가치가 크다.

