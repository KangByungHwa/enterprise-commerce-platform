# 아키텍처와 핵심 결정

```text
Client -> REST API -> MySQL(order + inventory + outbox)
                         |
                    Outbox publisher
                         v
                       Kafka -> Consumer -> Redis(idempotency)
                                             |
                                             v
                                      MongoDB shipment view
```

## 결정

1. 주문·재고·Outbox는 한 MySQL 트랜잭션으로 처리한다. DB 저장과 Kafka 전송의 dual-write 불일치를 피한다.
2. Outbox 발행은 at-least-once다. 발행 직후 프로세스가 죽으면 중복될 수 있으므로 소비자가 eventId를 Redis에 7일 보관한다.
3. 재고 행은 비관적 잠금으로 직렬화한다. 포트폴리오 단계에서는 명확성을 우선하며, 부하 시험 후 optimistic retry나 reservation 분할을 비교한다.
4. 배송 조회는 MongoDB projection으로 분리해 eventual consistency와 CQRS의 trade-off를 보여준다.

## 알려진 한계

Redis 키 설정과 Mongo 저장은 원자적이지 않다. 저장 실패 시 키를 삭제하지만 네트워크 단절 경계에는 빈틈이 있다. 운영 확장에서는 소비 이력 DB, Kafka EOS, 또는 inbox pattern을 검토한다.

