# 💡 Technical Decisions

Shop1 프로젝트의 아키텍처 설계 및 기술 스택 선정 이유를 정리합니다.

---

## 1. QueryDSL 도입
* **Decision:** 복잡한 검색 및 필터링 기능을 구현하기 위해 JPA Criteria 대신 **QueryDSL**을 채택.
* **Reasoning:**
    * **타입 안정성(Type Safety):** 컴파일 시점에 쿼리 문법 오류를 잡을 수 있어 유지보수성이 뛰어남.
    * **동적 쿼리:** `BooleanBuilder`를 활용하여 카테고리, 가격대, 검색어 등 다양한 조건 조합을 직관적인 자바 코드로 구현 가능.
    * **가독성:** SQL과 유사한 문법으로 작성되어 복잡한 조인이나 서브쿼리 작성 시 가독성이 높음.

## 2. Redis (In-Memory DB) 활용
* **Decision:** 데이터 캐싱 및 세션 저장소로 **Redis**를 도입.
* **Reasoning:**
    * **조회 성능 최적화:** 변경 빈도가 낮지만 조회 빈도가 높은 '상품 상세 정보', '카테고리 목록' 등을 캐싱하여 DB 부하를 분산시킴.
    * **분산 세션 관리:** 향후 Scale-out(서버 다중화) 상황을 대비하여, 세션 정보를 각 서버 메모리가 아닌 외부 저장소(Redis)에서 통합 관리하도록 설계.

## 3. Docker & Docker Compose 기반 인프라
* **Decision:** 개발 및 배포 환경을 **Docker Container**로 표준화.
* **Reasoning:**
    * **환경 일치성:** 로컬 개발 환경(Mac/Windows)과 배포 서버(Linux) 간의 환경 차이로 인한 버그("It works on my machine")를 원천 차단.
    * **관리 편의성:** `Backend`, `Frontend`, `DB`, `Redis` 등 다수의 서비스를 `docker-compose.yml` 파일 하나로 정의하고 실행할 수 있어 협업 및 인수인계에 유리.

## 4. 데이터 초기화 전략 개선 (JSON 기반)
* **Decision:** 무작위 데이터 생성 라이브러리(Datafaker) 대신 **Custom JSON 데이터 주입 방식**으로 변경.
* **Reasoning:**
    * 기존 Faker 사용 시 상품명과 이미지가 매칭되지 않는 등 데이터의 개연성이 떨어져 사용자 경험(UX) 테스트에 한계가 있었음.
    * 실제 커머스 데이터와 유사한 고품질 JSON 데이터셋을 구축하고, 서버 구동 시 이를 파싱하여 적재함으로써 포트폴리오의 완성도를 높임.