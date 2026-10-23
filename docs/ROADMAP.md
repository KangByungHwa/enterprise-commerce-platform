# 12주 개발 로드맵

| 주차 | 구현 목표 | 증빙 산출물 |
|---|---|---|
| 1 | 도메인 이벤트·주문/재고 경계와 ADR 정의 | ERD, API 명세, ADR-001 |
| 2 | Spring Boot, MySQL, Flyway 기반 주문 API | CRUD, 마이그레이션, 단위 테스트 |
| 3 | 동시 재고 차감과 트랜잭션 경계 | 잠금 테스트, 실패 시 롤백 시나리오 |
| 4 | Transactional Outbox와 Kafka 발행 | 장애 재처리 데모, 이벤트 스키마 |
| 5 | Kafka 소비자와 Redis 멱등성 | 중복 이벤트 테스트, 소비자 그룹 실험 |
| 6 | MongoDB 배송 조회 모델(CQRS) | eventual consistency 데모 |
| 7 | Docker Compose와 헬스체크 | 원클릭 실행, smoke test |
| 8 | 관측성 | Actuator/Prometheus 지표, 대시보드 캡처 |
| 9 | 성능 시험 | k6/JMeter, p95와 병목 개선 전후 비교 |
| 10 | Kubernetes | probes, requests/limits, HPA, PDB |
| 11 | CI/CD·보안 | GitHub Actions, 이미지 스캔/SBOM 확장 |
| 12 | 장애 훈련·발표 자료 | Kafka/MySQL 장애 Runbook, 5분 데모 영상 |

## 기업별 강조점

- 쿠팡: 재고 정합성, 대량 트래픽, 이벤트 기반 fulfillment, 장애 격리.
- 토스: 멱등성, 정확한 트랜잭션, 관측성, 실패 복구와 테스트 근거.
- 현대자동차 ICT: 물류 도메인 경험, 시스템 연계, Kubernetes 운영, 추적 가능성.

매주 README에 문제·선택지·결정·측정 결과를 남긴다. 단순 기술 나열보다 기존 Java/Spring/Oracle 물류 경력에서 무엇을 개선했는지를 연결해 설명한다.

