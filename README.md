# 🛒 Shop1 - 대규모 트래픽을 고려한 이커머스 플랫폼

**Shop1**은 실제 상용 서비스를 목표로 개발된 풀스택 이커머스 플랫폼입니다.  
상품 관리, 주문/결제, 배송 시스템을 마이크로서비스 아키텍처(MSA)를 고려한 모듈형 모놀리식 구조로 설계하였으며, Docker와 CI/CD를 통한 자동화된 배포 파이프라인을 구축했습니다.

🔗 **배포 링크:** [http://shop1.cloud](http://shop1.cloud)  
📚 **상세 포트폴리오(Notion):** [Notion](https://quasar-carnation-faa.notion.site/8b6636b4e262432bae9359e4948b8826?pvs=74)

---

## 📚 프로젝트 문서 (Documentation)
프로젝트의 기획, 설계, 그리고 기술 의사결정을 기록한 문서들입니다.

* 📝 **[요구사항 정의서 (REQUIREMENTS.md)](./docs/REQUIREMENTS.md)**: 서비스 주요 기능 및 세부 기획 문서
* 🔌 **[API 명세서 (API_GUIDE.md)](./docs/API_GUIDE.md)**: 프론트엔드와 백엔드 연동을 위한 REST API 가이드
* 💡 **[기술 의사결정 (TECH_DECISIONS.md)](./docs/TECH_DECISIONS.md)**: 핵심 기술 도입 배경
* 🔥 **[트러블슈팅 (TROUBLESHOOTING.md)](./docs/TROUBLESHOOTING.md)**: 배포/개발 과정에서 마주친 문제와 해결 과정

---

## 🛠 Tech Stack

### Backend
* **Java 17, Spring Boot 3.x**: 최신 LTS 버전을 활용한 안정적인 서버 구축
* **JPA (Hibernate), QueryDSL**: 복잡한 동적 쿼리 처리 및 타입 안전성 확보
* **Redis**: 캐싱을 통한 조회 성능 개선 및 분산 환경 세션 관리
* **Elasticsearch**: Nori 형태소 분석기를 활용한 빠르고 정확한 텍스트 검색
* **MySQL 8.0**: 대용량 데이터 저장을 위한 관계형 데이터베이스

### Frontend & Mobile
* **Next.js (React), TypeScript**: SSR 기반의 SEO 최적화 및 빠른 렌더링
* **Flutter**: 크로스 플랫폼 모바일 앱 지원

### Infra & DevOps
* **Docker, Docker Compose**: 개발 및 프로덕션 환경의 일치화
* **GitHub Actions**: 테스트 및 배포 자동화 (CI/CD)
* **AWS EC2**: 클라우드 서버 호스팅

---

## ✨ 주요 기능 (Key Features)

* **🔍 고도화된 검색 경험 (Elasticsearch)**
  * 기존 RDBMS의 `LIKE` 검색 한계를 극복하기 위해 **Elasticsearch**와 **Nori 형태소 분석기** 도입
  * 사용자 편의성을 위한 **초성 검색(예: ㄴㅇㅋ -> 나이키)** 및 **실시간 자동완성(Debounce 적용)** 기능 구현
* **💳 다양한 간편 결제 연동**
  * 카카오페이, 토스페이, 네이버페이 API 연동을 통한 편리한 결제 시스템 구축
* **🔐 보안 및 인증**
  * JWT 기반의 안전한 자체 로그인 및 OAuth 2.0 (카카오, 네이버) 소셜 로그인 지원
* **🛒 이커머스 핵심 비즈니스 로직**
  * 장바구니, 다양한 타입의 쿠폰 발급/사용, 주문 상태 관리, 리뷰 및 평점 시스템 구현
* **🚀 성능 최적화**
  * Redis를 활용한 세션 관리 및 데이터 캐싱으로 병목 현상 완화

---

## 🚀 Key Achievements & Troubleshooting

1.  **[Infra] EC2 프리티어 메모리 부족(OOM)으로 인한 서버 강제 종료(Killed) 해결**
    * **문제:** AWS EC2(1GB RAM)에 다수의 인프라(MySQL, Redis, Spring Boot, Elasticsearch)를 동시 배포 시 메모리 부족으로 인한 OOM Killer 발생.
    * **해결:** 4GB의 Swap 가상 메모리 할당 및 `JAVA_TOOL_OPTIONS`를 통한 JVM 힙 메모리 최적화로 안정적인 구동 환경 확보.

2.  **[Search] RDBMS(MySQL) 대신 Elasticsearch를 활용한 검색 기능 고도화**
    * **문제:** 데이터 증가에 따른 MySQL 풀 스캔(Full Scan) 성능 저하 및 초성 검색 구현의 한계.
    * **해결:** Elasticsearch 역인덱스 구조와 Nori 플러그인을 도입하여 검색 속도를 개선하고, 프론트엔드 Debouncing 처리로 서버 API 호출 부하를 최소화함.

3.  **Docker 배포 시 데이터 초기화 및 영속성 관리 전략 개선**
    * **문제:** CI/CD 배포 시 기존 데이터가 남아있어 초기화 로직이 동작하지 않거나 데이터가 유실되는 문제.
    * **해결:** Docker Volume 생명주기를 고려한 `ddl-auto` 전략 연계 및 맞춤형 배포 스크립트 작성.

---

## 🏃 How to Run

이 프로젝트는 Docker Compose를 통해 한 번의 명령어로 실행할 수 있습니다.

```bash
# 1. 저장소 클론
git clone [https://github.com/keeff11/shop1.git](https://github.com/keeff11/shop1.git)
cd shop1

# 2. 환경 변수 설정 (.env 파일 생성 필요)
# (비밀 키 등은 제외되어 있습니다.)

# 3. 빌드 및 실행 (캐시 없이 재빌드)
docker compose up -d --build --force-recreate