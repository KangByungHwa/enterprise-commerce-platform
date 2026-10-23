# Enterprise Commerce & Logistics Platform 개발환경 구축 가이드

이 문서는 프로젝트를 처음 내려받은 개발자가 로컬 테스트, 디버깅, Docker Compose 실행 및 Kubernetes 예제 검증까지 진행하기 위한 절차를 설명한다.

## 1. 권장 환경

| 도구 | 권장 버전 | 용도 |
|---|---:|---|
| Git | 2.40 이상 | 소스 관리 |
| JDK | 21 | Spring Boot 빌드·실행 |
| Maven | 3.9 이상 | 의존성 관리·테스트 |
| Docker Desktop 또는 Docker Engine | 최신 안정 버전 | 애플리케이션과 데이터 인프라 실행 |
| Docker Compose | v2 | 전체 컨테이너 구성 |
| kubectl | 1.29 이상 | Kubernetes 예제 검증, 선택 사항 |
| kind 또는 minikube | 최신 안정 버전 | 로컬 Kubernetes 실습, 선택 사항 |

가장 간단한 실행에는 Docker와 Docker Compose만 필요하다. IDE 디버깅과 로컬 코드 실행까지 하려면 JDK 21과 Maven 3.9 이상을 추가로 설치한다.

## 2. 운영체제별 준비

설치 명령과 패키지 이름은 변경될 수 있으므로 회사 장비에서는 사내 표준 정책을 우선한다.

### Windows 10/11

1. BIOS/UEFI에서 가상화를 활성화한다.
2. WSL 2와 Docker Desktop을 설치한다.
3. Docker Desktop 설정에서 WSL 2 기반 엔진을 사용한다.
4. 로컬 실행이 필요하면 Temurin JDK 21과 Maven 3.9 이상을 설치한다.
5. 새 PowerShell 창에서 설치 상태를 확인한다.

```powershell
git --version
java -version
mvn -version
docker version
docker compose version
```

Kafka, MySQL, Redis, MongoDB를 함께 실행하므로 Docker Desktop에는 CPU 4개와 메모리 8GB 이상 할당을 권장한다.

### macOS

Docker Desktop을 설치한다. 로컬 소스 실행 도구는 Homebrew로 설치할 수 있다.

```bash
brew install openjdk@21 maven git
java -version
mvn -version
docker version
docker compose version
```

### Ubuntu/Debian 계열 Linux

Docker 공식 저장소를 통해 Docker Engine과 Compose plugin을 설치하고 JDK 21, Maven 3.9 이상을 준비한다.

```bash
java -version
mvn -version
docker version
docker compose version
```

현재 사용자가 Docker daemon을 사용할 수 있어야 한다. 관리자 권한 없이 Docker를 사용하도록 설정할 때는 조직의 보안 정책을 먼저 확인한다.

## 3. 프로젝트 준비

ZIP을 원하는 작업 폴더에 압축 해제하고 프로젝트 루트로 이동한다.

```bash
cd enterprise-commerce-logistics-platform
```

다음 항목이 보이면 올바른 위치다.

```text
pom.xml
compose.yml
Dockerfile
src/
k8s/
```

Windows PowerShell에서는 구조 점검 스크립트를 실행할 수 있다.

```powershell
./scripts/verify-structure.ps1
```

## 4. 방법 A: Docker로 전체 환경 실행

로컬 JDK와 Maven 없이 실행하는 가장 간단한 방법이다. 첫 실행에서는 Maven 의존성과 컨테이너 이미지를 내려받으므로 시간이 걸릴 수 있다.

```bash
docker compose up --build -d
docker compose ps
```

구성되는 서비스는 다음과 같다.

| 서비스 | 로컬 포트 | 역할 |
|---|---:|---|
| app | 8080 | Spring Boot API |
| mysql | 3306 | 주문, 재고, Outbox 저장 |
| kafka | 9092 | 주문 이벤트 전달 |
| redis | 6379 | 이벤트 소비 멱등성 키 |
| mongo | 27017 | 배송 조회 projection |

애플리케이션 로그를 확인한다.

```bash
docker compose logs -f app
```

`Started PlatformApplication`이 나타나면 별도 터미널에서 상태를 확인한다.

```bash
curl http://localhost:8080/actuator/health
```

Windows PowerShell:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

전체 주문 흐름은 운영체제에 맞는 smoke test로 검증한다.

```powershell
./scripts/smoke-test.ps1
```

```bash
./scripts/smoke-test.sh
```

다음 개발용 화면도 확인할 수 있다.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Prometheus 지표: `http://localhost:8080/actuator/prometheus`

## 5. 방법 B: 인프라는 Docker, 애플리케이션은 로컬 실행

IDE 디버깅과 빠른 코드 변경에 적합하다.

### 5.1 인프라 실행

```bash
docker compose up -d mysql redis mongo kafka
docker compose ps
```

### 5.2 테스트와 애플리케이션 실행

```bash
mvn verify
mvn spring-boot:run
```

기본 연결 주소는 다음과 같다.

- MySQL: `jdbc:mysql://localhost:3306/commerce`
- Kafka: `localhost:9092`
- Redis: `localhost:6379`
- MongoDB: `localhost:27017`

IntelliJ IDEA에서는 Project SDK를 JDK 21로 지정하고 `PlatformApplication`을 실행한다. 기본 계정은 로컬 학습 환경에서만 사용한다.

## 6. 환경 변수

외부 데이터 서비스나 별도 계정을 사용할 때 다음 값을 설정한다.

| 변수 | 기본값 |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/commerce?useSSL=false&allowPublicKeyRetrieval=true` |
| `DB_USER` | `commerce` |
| `DB_PASSWORD` | `commerce` |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `REDIS_HOST` | `localhost` |
| `REDIS_PORT` | `6379` |
| `MONGODB_URI` | `mongodb://commerce:commerce@localhost:27017/logistics?authSource=admin` |

실제 비밀번호나 운영 접속 문자열을 Git에 커밋하지 않는다. 운영 환경에서는 Secret Manager, Kubernetes Secret, External Secrets 등의 비밀 관리 방식을 사용한다.

## 7. API 수동 확인

주문을 생성한다.

```bash
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"local-user","sku":"SKU-001","quantity":2}'
```

응답의 `id`를 사용해 주문과 배송 조회 모델을 확인한다.

```bash
curl http://localhost:8080/api/orders/{orderId}
curl http://localhost:8080/api/shipments/{orderId}
```

배송 데이터는 Kafka 소비자가 비동기로 생성하므로 주문 직후 수 초 동안 404가 반환될 수 있다.

## 8. 테스트

```bash
mvn test
mvn verify
```

- `OrderServiceTest`: 재고 차감, Outbox 저장, 재고 부족 처리
- `OrderEventConsumerTest`: Redis 키를 이용한 중복 이벤트 차단
- `mvn verify`: 컴파일과 전체 테스트 수행
- smoke test: MySQL → Outbox → Kafka → Redis → MongoDB 연결 확인

Pull Request에서는 `.github/workflows/ci.yml`이 테스트와 Docker 이미지 빌드를 수행한다.

## 9. Kubernetes 실습

Kubernetes 예제는 애플리케이션 계층만 배포한다. MySQL, Kafka, Redis, MongoDB는 외부 managed service 또는 별도 operator가 제공한다고 가정한다.

먼저 manifest 생성을 확인한다.

```bash
kubectl version --client
kubectl kustomize k8s/base
```

배포 전에 다음 값을 변경한다.

1. `k8s/base/deployment.yaml`의 컨테이너 이미지 주소
2. `k8s/base/configmap.yaml`의 데이터 서비스 주소
3. `k8s/base/secret.example.yaml`의 예제 계정

```bash
kubectl apply -f k8s/base/secret.example.yaml
kubectl apply -k k8s/base
kubectl rollout status deployment/commerce-logistics
kubectl get pods,svc,hpa,pdb
```

예제 Secret 파일에 실제 운영 비밀번호를 저장하거나 커밋하지 않는다.

## 10. 종료와 데이터 초기화

컨테이너를 종료하되 데이터를 보존한다.

```bash
docker compose down
```

MySQL, Redis, MongoDB 데이터를 포함해 완전히 초기화할 때만 다음 명령을 사용한다.

```bash
docker compose down -v
```

`-v`는 프로젝트 Docker volume의 데이터를 삭제한다. 필요한 데이터가 없는지 먼저 확인한다.

## 11. 문제 해결

### 포트가 이미 사용 중인 경우

8080, 3306, 6379, 9092, 27017 포트를 사용하는 기존 프로세스나 컨테이너를 종료한다. 또는 `compose.yml`의 호스트 쪽 포트를 변경하고 로컬 애플리케이션 연결 주소도 함께 수정한다.

### 애플리케이션 컨테이너가 계속 재시작되는 경우

```bash
docker compose ps
docker compose logs --tail=200 app mysql kafka redis mongo
```

데이터 서비스가 아직 준비되지 않았거나 Docker 메모리가 부족할 수 있다. 각 서비스의 health 상태를 확인한다.

### Kafka 연결 실패

로컬에서 실행한 애플리케이션은 `localhost:9092`, Compose 내부 애플리케이션은 `kafka:29092`를 사용한다. 실행 위치와 `KAFKA_BOOTSTRAP_SERVERS`가 일치하는지 확인한다.

### MySQL 인증 또는 Flyway 실패

기존 volume에 예전 계정이나 스키마가 남아 있을 수 있다. 필요한 데이터를 백업한 후에만 `docker compose down -v`로 초기화한다.

### 배송 조회가 계속 404인 경우

Kafka와 애플리케이션 로그를 확인한다.

```bash
docker compose logs --tail=200 kafka app
```

주문은 생성됐지만 Kafka 발행이 실패한 경우 Outbox 레코드는 미발행 상태로 남는다. 연결이 복구되면 publisher가 다시 시도한다.

## 12. 최초 구축 완료 체크리스트

- [ ] Docker와 Compose 버전 확인 명령이 성공한다.
- [ ] `docker compose up --build -d`가 성공한다.
- [ ] 모든 컨테이너가 실행 또는 healthy 상태다.
- [ ] `/actuator/health` 응답이 `UP`이다.
- [ ] smoke test가 주문 ID와 `READY_TO_SHIP` 상태를 출력한다.
- [ ] 로컬 개발 방식에서는 `mvn verify`가 성공한다.
- [ ] Kubernetes 실습 시 `kubectl kustomize k8s/base`가 성공한다.

