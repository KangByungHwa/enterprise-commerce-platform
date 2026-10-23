# 검증 기록

생성일: 2026-10-23 (Asia/Seoul)

## 이 환경에서 완료한 검증

- 프로젝트 필수 파일 및 21개 Java 소스 존재 확인
- `pom.xml` XML 파싱 확인
- YAML 11개 파일 존재와 Kubernetes Kustomization 필수 `apiVersion`/`kind`, 참조 리소스 확인
- ZIP 생성 후 파일 목록 확인과 외부 SHA-256 산출

## 환경 제약으로 수행하지 못한 검증

현재 생성 환경에는 JDK, Maven, Docker, kubectl이 설치되어 있지 않아 `mvn verify`, `docker compose config/up`, 실제 Kafka 왕복, `kubectl apply --dry-run`은 실행하지 못했다. 이를 성공했다고 주장하지 않는다.

다음 명령이 수신 환경 및 GitHub Actions에서 수행하는 최종 검증 기준이다.

```bash
mvn verify
docker compose config
docker compose up --build -d
./scripts/smoke-test.sh
kubectl kustomize k8s/base
```

