# 범위 구분

## 현재 ZIP에 구현된 실행 범위

- 주문 생성/조회, MySQL 재고 차감, 비관적 잠금과 낙관적 버전 필드
- 같은 DB 트랜잭션에 주문과 Outbox 이벤트 저장
- Kafka 발행 및 Redis 기반 소비 멱등성
- MongoDB 배송 조회 모델과 조회 API
- Bean Validation, RFC 9457 ProblemDetail 오류, Flyway 초기 데이터
- 단위 테스트, Docker Compose, smoke test, Actuator/Prometheus
- GitHub Actions 빌드, Kubernetes 배포·서비스·HPA·PDB 예제

## 의도적으로 확장 과제로 남긴 운영 범위

- 인증/인가(OAuth2/OIDC), 개인정보 암호화 및 감사 로그
- 결제·취소 Saga, 보상 트랜잭션, DLQ와 운영자 재처리 UI
- Debezium CDC 기반 Outbox relay(현재는 polling publisher)
- Schema Registry/Avro 호환성, Kafka ACL/TLS, Redis/Mongo/MySQL 고가용성
- 분산 추적(OpenTelemetry), Grafana 대시보드, SLO/알람
- 실제 클라우드 registry 배포, Helm/Argo CD, External Secrets
- 부하 시험 수치와 장애 복구 RTO/RPO 증거

이 구분은 “샘플에서 실행됨”과 “운영 환경에서 검증됨”을 혼동하지 않기 위한 것이다.

