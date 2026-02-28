# 🔧 Troubleshooting Log

Shop1 프로젝트 개발 및 운영 과정에서 발생한 주요 기술적 이슈와 해결 과정을 기록합니다.

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
* **배포 스크립트 고도화:** 데이터 초기화가 필요한 배포 시점에는 Volume까지 명시적으로 삭제하도록 스크립트 수정.
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

## 📌 Issue 2: N+1 문제 해결 및 조회 성능 개선

### 1. 문제 상황
* **현상:** 상품 목록 조회 API 호출 시, 상품 개수(N)만큼 연관된 이미지 정보를 조회하는 추가 쿼리가 발생.
* **영향:** 대량의 트래픽 발생 시 DB 부하가 급증하고 응답 속도가 저하됨.

### 2. 해결 과정
* **Fetch Join 적용:** JPQL 및 QueryDSL을 사용하여 연관된 엔티티(`ItemImage`)를 한 번의 쿼리로 함께 조회하도록 변경.
* **Batch Size 설정:** 컬렉션 조회의 경우 `default_batch_fetch_size`를 설정하여 `IN` 절을 통해 쿼리 수를 최적화함.

### 3. 결과
* 상품 목록 조회 쿼리 수: N+1회 → 1회로 감소.
* API 응답 속도 약 60% 개선.


### 📌 Issue 3: [Infra] EC2 프리티어 메모리 부족(OOM)으로 인한 서버 강제 종료(Killed) 해결
* **문제 상황**: 
  * AWS EC2 t2.micro (RAM 1GB) 환경에서 MySQL, Redis, Spring Boot에 이어 Elasticsearch 컨테이너를 추가 배포하자, 메모리 부족으로 인해 Linux OOM(Out Of Memory) Killer가 Spring Boot 프로세스를 예고 없이 강제 종료시키는 현상 발생.
  * `docker-compose up` 실행 시 시스템이 멈추거나 컨테이너가 튕기는 불안정한 상태 지속.
* **원인 분석**: 
  * Elasticsearch 자체의 최소 요구 메모리가 높아 1GB 물리 램으로는 기존 인프라와 함께 구동이 불가능함.
  * Spring Boot 구동 시 JVM이 남은 메모리를 확보하려다 임계치를 넘어 리눅스 커널에 의해 Kill 됨.
* **해결 방법**:
  1. **가상 메모리(Swap) 할당**: 하드 디스크 용량을 활용하여 4GB의 Swap 메모리를 생성 (`fallocate`, `mkswap`, `swapon`)하여 물리적 램 부족 현상 1차 방어.
  2. **JVM 메모리 제한 (다이어트)**: `docker-compose.yml`에 `JAVA_TOOL_OPTIONS: "-Xms256m -Xmx512m"` 환경변수를 추가하여 Spring Boot가 사용할 최대 힙 메모리를 제한.
  3. **ES 메모리 제한**: Elasticsearch 컨테이너에도 `"ES_JAVA_OPTS=-Xms512m -Xmx512m"` 옵션을 주어 자원 점유율 최적화.
* **결과**: OOM 강제 종료 현상이 완벽히 사라졌으며, 배포 환경에서도 검색 엔진과 백엔드 서버가 안정적으로 동시 구동됨.

