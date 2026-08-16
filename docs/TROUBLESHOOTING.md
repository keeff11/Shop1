
# 🔧 Troubleshooting & Performance Optimization

Shop1 프로젝트 개발 및 운영 과정에서 발생한 주요 기술적 이슈 해결과 성능 개선 과정을 기록합니다.

---

## 📌 Issue 1: Docker 배포 시 데이터 초기화 로직 미작동 및 데이터 정합성 문제

### 1. 문제 상황 (Problem)

* **현상:** CI/CD 파이프라인을 통해 배포를 진행했음에도 불구하고, `ddl-auto: create` 설정과 새로운 초기 데이터(`items_v2.json`)가 반영되지 않음.
* **영향:** 초기 데이터가 갱신되지 않아 신규 기능 테스트가 불가능하고, 기존 더미 데이터가 잔존하여 무결성 오류 발생 가능성 존재.

### 2. 원인 분석 (Root Cause Analysis)

* **Docker Volume 영속성:** `docker compose down` 명령어는 컨테이너만 삭제할 뿐, 데이터베이스의 데이터가 저장된 Volume(`shop1-db-data`)은 유지함. 이로 인해 DB가 초기화되지 않고 기존 데이터를 유지한 채 재시작됨.
* **방어 로직의 역설:** `DataInitializer` 클래스 내 `if (userRepository.count() > 0) return;` 로직이 잔존 데이터 때문에 항상 `true`가 되어 초기화 코드가 실행되지 않음.
* **이미지 캐싱:** 단순 리소스 파일(`json`) 변경 시 Docker 빌드 과정에서 기존 레이어를 캐시로 사용하여 변경 사항이 이미지에 포함되지 않는 경우가 발생.

### 3. 해결 과정 (Solution)

* **배포 스크립 고도화:** 데이터 초기화가 필요한 배포 시점에는 Volume까지 명시적으로 삭제하도록 스크립트 수정.
```bash
# 기존: sudo docker compose down
# 변경: sudo docker compose down -v  (Volume 삭제 옵션 추가)

```


* **강제 재빌드 적용:** `--build` 및 `--no-cache` 옵션을 활용하여 최신 소스코드와 리소스 파일이 확실하게 이미지에 포함되도록 조치.
* **운영 전략 수립:** 초기화 이후 운영 단계에서는 데이터 보존을 위해 `update` 전략으로 전환하고, `application.properties` 주입 방식을 GitHub Secrets와 연동하여 자동화함.

### 4. 결과 (Result)

* 배포 시 데이터베이스의 완전한 초기화와 신규 데이터 적재를 제어할 수 있게 됨.
* 개발 및 테스트 환경의 신뢰성 확보.

---

## 📌 Issue 2: [DB] JPA N+1 문제 해결을 통한 1차 조회 성능 개선

### 1. 문제 상황

* **현상:** 상품 목록 조회 API 호출 시, 상품 개수(N)만큼 연관된 이미지 정보를 조회하는 추가 쿼리가 발생.
* **영향:** 대량의 트래픽 발생 시 불필요한 DB 접근이 급증하여 시스템 병목과 응답 속도 저하를 유발.

### 2. 해결 과정

* **Fetch Join 적용:** QueryDSL 및 JPQL을 사용하여 연관된 엔티티(`ItemImage`)를 한 번의 쿼리로 함께 묶어서(Join) 조회하도록 영속성 컨텍스트 최적화.
* **Batch Size 설정:** 페이징이 필요한 컬렉션 조회의 경우, `default_batch_fetch_size`를 설정하여 `IN` 절을 통해 N개의 쿼리를 1개로 병합함.

### 3. 결과

* 상품 목록 조회 시 발생하는 쿼리 수를 N+1회에서 1회로 극적으로 감소시킴.

---

## 📌 Issue 3: [Cache] Redis 도입을 통한 상품 목록 API 2차 성능 개선 (응답시간 70% 단축)

### 1. 문제 상황

* **현상:** 메인 페이지 진입 시 가장 빈번하게 호출되는 '전체 상품 목록 조회' API가 매번 RDBMS(MySQL)를 거쳐 디스크 I/O를 발생시킴.
* **영향:** N+1 문제를 해결했음에도 불구하고 DB 접근 자체의 오버헤드로 인해 API 응답 시간이 평균 **30ms**로 측정됨. 대규모 이벤트 시 DB 부하가 우려됨.

### 2. 해결 과정

* **Redis 글로벌 캐시 도입:** 인메모리(In-Memory) 데이터 저장소인 Redis를 도입하여 조회된 상품 목록 데이터를 캐싱.
* **캐시 전략 (Cache Aside):** 클라이언트 요청 시 우선 Redis에서 데이터를 찾고(Cache Hit), 없을 경우에만 DB를 조회(Cache Miss)한 뒤 Redis에 적재하도록 아키텍처 구성.
* **데이터 정합성 유지:** 상품의 추가/수정/삭제가 일어날 때 `@CacheEvict`를 통해 기존 캐시를 만료시켜 실시간 데이터 정합성 보장.

### 3. 결과

* 매 요청마다 발생하던 DB 접근을 차단하고 인메모리에서 즉시 반환.
* API 평균 응답 속도가 **30ms → 9ms**로 약 **70% 이상 대폭 단축**되며 사용자 경험과 서버 처리량(Throughput)을 크게 향상시킴.

---

## 📌 Issue 4: [Infra] EC2 프리티어 메모리 부족(OOM)으로 인한 서버 강제 종료(Killed) 해결

### 1. 문제 상황

* **현상:** AWS EC2 t2.micro (RAM 1GB) 환경에서 MySQL, Redis, Spring Boot에 이어 검색 고도화를 위해 Elasticsearch 컨테이너를 추가 배포하자, 서버가 멈추거나 Spring Boot 프로세스가 예고 없이 강제 종료(Killed)되는 현상 발생.

### 2. 원인 분석

* Elasticsearch의 기본 최소 요구 메모리가 높아 1GB의 물리 RAM으로는 기존 인프라와 함께 동시 구동이 불가능함.
* Spring Boot 구동 시 JVM이 남은 시스템 메모리를 확보하려다 임계치를 넘어 리눅스 커널의 OOM(Out Of Memory) 킬러에 의해 암살당함.

### 3. 해결 과정

1. **가상 메모리(Swap) 할당:** 하드 디스크 용량을 램처럼 활용하는 Swap 메모리를 4GB 넉넉하게 생성(`fallocate`, `mkswap`, `swapon`)하여 1차적인 메모리 부족 현상 방어.
2. **JVM 메모리 다이어트:** `docker-compose.yml` 파일 내 백엔드 서비스에 `JAVA_TOOL_OPTIONS: "-Xms256m -Xmx512m"` 환경 변수를 주입하여 Spring Boot의 최대 힙 메모리 점유율을 제한.
3. **Elasticsearch 메모리 제한:** 검색 엔진 컨테이너에도 `"ES_JAVA_OPTS=-Xms512m -Xmx512m"` 옵션을 부여하여 한정된 자원 내에서 구동되도록 튜닝.

### 4. 결과

* OOM Killer에 의한 강제 종료 현상이 완벽히 사라짐.
* 프리티어(1GB RAM)라는 극도로 제한된 인프라 환경에서도 다수의 컨테이너(RDBMS, Cache, Search Engine, Server)를 안정적으로 동시 구동하는 데 성공함.

---

## 📌 Issue 5: [Concurrency] 분산락 환경의 커넥션 풀 데드락(Deadlock) 해결 및 동시성 처리량 100% 개선

### 1. 문제 상황 (Problem)

* **현상:** JMeter를 활용하여 100명의 유저가 동시에 주문 및 결제를 진행하는 동시성 스트레스 테스트를 수행한 결과, 요청의 10% 남짓만 성공하고 나머지는 실패함.
* **에러 로그:** `HikariPool-1 - Connection is not available, request timed out` (DB 커넥션 풀 고갈) 및 Redis 분산락 획득 타임아웃 예외 발생.

### 2. 원인 분석 (Root Cause Analysis)

* **네트워크 I/O와 트랜잭션 혼재:** 외부 PG사 결제 API를 호출하는 무거운 로직이 `@Transactional` 내부에 포함되어 있어, 네트워크 응답을 기다리는 동안 쓰레드가 DB 커넥션을 반납하지 못하고 장시간 점유함.
* **락 획득 순서에 따른 교착 상태(Deadlock):** 주문 트랜잭션을 시작해 1차 DB 커넥션을 얻은 상태에서, 상품 재고 차감을 위해 Redis 분산락 대기줄에 진입하는 구조로 설계됨.
* 게다가 락 내부에서 `REQUIRES_NEW` 전파 속성을 사용해 쓰레드당 총 2개의 DB 커넥션을 요구함. 동시 접속자가 몰릴 경우 남은 커넥션이 없어 모든 쓰레드가 서로 2번째 커넥션을 무한 대기하는 교착 상태(Deadlock)에 빠짐.

### 3. 해결 과정 (Solution)

1. **Facade 패턴을 통한 트랜잭션 분리:** `@Transactional` 영역을 순수 DB 업데이트용(주문 생성/재고 차감) 핸들러 객체(`OrderTxHandler`)로 분리. 트랜잭션이 종료된 홀가분한 상태에서 외부 PG사 API를 호출하도록 재설계하여 커넥션 점유 시간을 수 밀리초 단위로 단축시킴.
2. **Redisson MultiLock 도입 및 순서 교정:** 다수의 상품을 동시에 주문할 때 발생할 수 있는 순환 참조 데드락을 방지하기 위해, 상품 ID를 정렬하여 `MultiLock`으로 한 번에 락을 획득하도록 개선.
3. **DB 접근 전 문지기 역할 부여:** DB 커넥션을 물고 Redis 락을 기다리던 기존 구조를 뒤집어, **트랜잭션 진입 이전(DB 접근 전)에 Redis 락을 먼저 획득**하도록 변경. 이를 통해 Redis Pub/Sub 시스템이 1명씩만 DB 커넥션을 사용하도록 완벽하게 교통정리를 수행함.

### 4. 결과 (Result)

* JMeter 100명 동시 접속(100 Threads) 테스트 환경에서 커넥션 에러 및 병목 현상을 원천 차단하여 **성공률 100% 달성**.
* `Spin Lock`으로 인한 Redis 서버 부하 없이, `Pub/Sub` 메커니즘을 통한 효율적인 동시성 제어 및 완벽한 재고 데이터 정합성(Lost Update 방지) 보장.

---

## 📌 Issue 6: [Consistency] PG 승인 이후 상태 미반영 위험 해결 (Outbox 패턴 + 정합성 배치) *(2026-08-15)*

### 1. 문제 상황 (Problem)

* Issue 5에서 동시 트래픽 상황의 안정성은 확보했지만, 별도로 다음 두 가지 정합성 위험이 남아있음을 검토 과정에서 확인함.
  1. **PG 승인 성공 + 서버 장애:** `OrderService.approveOrder()`에서 PG사 `approve()` 호출은 트랜잭션 밖(순서상 먼저) 실행되고, 그 다음에야 `OrderTxHandler.completeOrderPayment()`(`@Transactional`)가 상태를 `PAID`로 커밋함. PG 승인이 성공한 직후, 이 트랜잭션이 커밋되기 전에 서버가 죽으면 PG에는 결제가 남지만 우리 DB는 `PAYMENT_PENDING`으로 영구히 방치됨.
  2. **커밋 이후 후속 처리(알림 등) 유실:** 상태 변경(DB 커밋)과 그 결과를 알리는 부수 작업(알림 발송 등)이 분리되어 있을 경우, DB 커밋은 성공했는데 그 직후 서버가 죽으면 알림 발행 자체가 유실될 수 있음.

### 2. 원인 분석 (Root Cause Analysis)

* 두 문제는 **서로 다른 실패 시점**을 가리키므로 하나의 장치로 동시에 해결할 수 없음을 확인함.
  * (1)은 "DB 커밋 자체가 안 된 경우" — 로컬 트랜잭션만으로는 원천적으로 막을 수 없고, 외부 시스템(PG)과 우리 DB의 상태를 사후에 대조하는 절차가 필요함.
  * (2)는 "DB 커밋은 됐지만 그 결과를 알리는 절차가 유실된 경우" — 상태 변경과 이벤트 기록을 같은 트랜잭션으로 묶어야 해결됨(Outbox 패턴의 전형적인 적용 범위).

### 3. 해결 과정 (Solution)

1. **Outbox 패턴 적용 ((2) 해결):** `outbox_event` 테이블을 추가하고, 결제 완료 트랜잭션(`OrderTxHandler.completeOrderPayment`) 안에서 상태 변경과 이벤트 기록을 함께 커밋. `@TransactionalEventListener(phase = AFTER_COMMIT)`으로 커밋 직후 즉시 발행을 시도하고, 즉시 발행이 실패하거나 애플리케이션 재시작으로 시도 자체가 누락된 경우를 대비해 `@Scheduled` 폴링 배치를 이중 안전장치로 추가함. 발행 실패는 재시도 횟수를 기록하고 일정 횟수 이상이면 `FAILED`로 확정.
2. **PG 재조회 기반 정합성 배치 추가 ((1) 해결):** `PaymentService`에 `inquire(Order)`를 추가해 PG사(Kakao/Toss/Naver) 기준의 실제 결제 상태를 조회할 수 있게 함. `PAYMENT_PENDING` 상태로 10분 이상 방치된 주문을 5분 주기로 스캔해, PG가 결제완료 상태면 `completeOrderPayment`로 복구(자연스럽게 Outbox 이벤트도 함께 기록됨), PG도 미결제 상태면 재고를 복구하고 주문을 취소 처리함. Issue 5의 교훈을 그대로 적용해, PG 상태 조회(외부 I/O)는 트랜잭션 밖에서 수행하고 DB 반영만 별도의 짧은 트랜잭션으로 처리함.

### 4. 결과 (Result)

* "PG 승인 성공 후 서버 장애"와 "커밋 후 알림 유실"이라는, 서로 다른 두 실패 시나리오를 각각 정확히 대응하는 장치로 분리해 해결함.
* 두 기능 모두 단위 테스트로 검증(Outbox 9건, 멱등성 5건, 정합성 배치/PG 조회 14건 — 총 28건, 전부 통과 확인).
* 다만 정합성 배치는 폴링 주기(최대 15분)만큼 지연이 있고, Outbox 발행은 "최소 한 번(at-least-once)" 보장이라 수신 측 멱등 처리가 별도로 필요함 — 완전한 실시간 강한 일관성은 아니라는 한계는 명확히 인지하고 있음.

### 5. 검증 중 추가로 발견한 버그: AFTER_COMMIT 리스너와 트랜잭션 전파

* **현상:** 단위 테스트(Mock 기반)는 모두 통과했지만, 실제 DB + 실제 SMTP로 전체 흐름(`completeOrderPayment` → outbox 기록 → 커밋 → 즉시 발행)을 종단 간(end-to-end)으로 검증하는 과정에서, 이메일은 실제로 정상 발송되는데도 `outbox_event`의 상태가 DB에는 계속 `PENDING`으로 남는 현상을 발견함. `event.markPublished()` 호출은 물론, `outboxEventRepository.save(event)`를 명시적으로 추가해도 커밋되지 않음.
* **원인 분석:** `OutboxEventService.tryPublish()`가 `@Transactional`(기본 propagation `REQUIRED`)이었는데, 이 메서드가 `@TransactionalEventListener(phase = AFTER_COMMIT)` 콜백 안에서 호출되는 구조였음. AFTER_COMMIT 시점엔 원본 트랜잭션이 물리적으로는 이미 커밋됐지만, 스프링의 트랜잭션 동기화 컨텍스트(`TransactionSynchronizationManager`)는 `afterCompletion` 단계 전까지 아직 유지됨. 기본 `REQUIRED` 전파는 이 상태를 "기존(이미 끝난) 트랜잭션에 참여"하는 것으로 처리해, 여기서 변경한 상태가 실제로는 물리적으로 커밋되지 않고 유실됨.
* **해결:** `tryPublish()`를 `@Transactional(propagation = Propagation.REQUIRES_NEW)`로 변경해, AFTER_COMMIT 콜백 안에서도 완전히 독립된 물리 트랜잭션이 열리도록 강제함.
* **파급 효과:** 이 버그가 그대로 있었다면, 폴링 스케줄러(`OutboxPollingScheduler`)가 이미 발송된 이벤트를 계속 `PENDING`으로 잘못 인식해 5분마다 같은 알림을 반복 발송했을 것 — 단위 테스트만으로는 발견하지 못하고, 실제 트랜잭션 커밋/AFTER_COMMIT 타이밍이 개입되는 종단 간 검증에서만 드러난 문제였음.