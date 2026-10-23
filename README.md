# Enterprise Commerce & Logistics Platform

Java/Spring/Oracle 기반 물류 시스템 경력을 Kafka·Redis·MongoDB·Docker·Kubernetes로 확장해 보여주는 실행형 포트폴리오다. 마이크로서비스 수를 늘리기보다 주문 정합성, 이벤트 전달, 중복 처리, 조회 모델이라는 면접에서 설명 가능한 문제에 집중했다.

기준 버전은 Java 21과 Spring Boot 3.5.16이다. 3.5 계열을 선택해 Spring Boot 3 경험을 유지하면서 장기 포트폴리오의 호환성을 우선했다.

## 빠른 실행

필수 도구는 Docker Desktop 4.x와 Docker Compose v2뿐이다.

```bash
docker compose up --build -d
curl http://localhost:8080/actuator/health
```

PowerShell에서는 `./scripts/smoke-test.ps1`, macOS/Linux에서는 `./scripts/smoke-test.sh`로 주문→Kafka→배송 조회 전체 흐름을 확인한다. Swagger UI는 `http://localhost:8080/swagger-ui.html`, Prometheus 지표는 `http://localhost:8080/actuator/prometheus`다.

종료는 `docker compose down`, 데이터까지 초기화하려면 의도적으로 `docker compose down -v`를 실행한다.

## API 예시

```bash
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"customer-1","sku":"SKU-001","quantity":2}'
curl http://localhost:8080/api/orders/{orderId}
curl http://localhost:8080/api/shipments/{orderId}
```

배송 조회는 비동기 projection이므로 주문 직후 잠시 404일 수 있다.

## 로컬 개발

JDK 21과 Maven 3.9+ 설치 후 인프라만 `docker compose up -d mysql redis mongo kafka`로 띄우고 `mvn spring-boot:run`을 실행한다. 테스트는 `mvn verify`다.

## Kubernetes

`k8s/base/configmap.yaml`의 외부 서비스 주소, 이미지 주소와 Secret 값을 환경에 맞게 바꾼 후 다음을 실행한다.

```bash
kubectl apply -f k8s/base/secret.example.yaml
kubectl apply -k k8s/base
kubectl rollout status deployment/commerce-logistics
```

Kubernetes 예제는 애플리케이션 배포를 다루며 데이터 계층은 managed service 또는 별도 operator 사용을 전제로 한다. 예제 Secret은 절대 저장소에 실제 비밀로 커밋하지 않는다.

## 문서

- `docs/ROADMAP.md`: 12주 주차별 개발·포트폴리오 로드맵
- `docs/ARCHITECTURE.md`: 데이터 흐름, 일관성, 핵심 설계 결정
- `docs/SCOPE.md`: 지금 실행되는 범위와 운영급 확장 범위

## 면접 데모 순서(5분)

1. 주문을 생성하고 MySQL 주문/재고/Outbox의 단일 트랜잭션을 설명한다.
2. Kafka 이벤트가 배송 projection을 만들고 Redis가 중복을 막는 흐름을 시연한다.
3. Kafka를 잠시 중단해 Outbox가 남고 재시작 후 회복되는 모습을 보인다.
4. Kubernetes probes/HPA/PDB와 CI를 보여준다.
5. `docs/SCOPE.md`의 한계를 먼저 말하고 다음 개선 실험을 제시한다.

